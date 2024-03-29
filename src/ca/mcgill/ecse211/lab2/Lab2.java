// Lab2.java
package ca.mcgill.ecse211.lab2;

import ca.mcgill.ecse211.odometer.*;
import lejos.hardware.Button;
import lejos.hardware.ev3.LocalEV3;
import lejos.hardware.lcd.TextLCD;
import lejos.hardware.motor.EV3LargeRegulatedMotor;
import lejos.hardware.port.Port;
import lejos.hardware.sensor.EV3ColorSensor;
import lejos.hardware.sensor.SensorModes;
import lejos.robotics.SampleProvider;

public class Lab2 {

  // Motor Objects, and Robot related parameters
  private static final EV3LargeRegulatedMotor leftMotor =
      new EV3LargeRegulatedMotor(LocalEV3.get().getPort("A"));
  private static final EV3LargeRegulatedMotor rightMotor =
      new EV3LargeRegulatedMotor(LocalEV3.get().getPort("B"));
  private static final TextLCD lcd = LocalEV3.get().getTextLCD();
  // The radius of the wheels in cm
  public static final double WHEEL_RAD = 2.1; //Radius increase = distance decrease
  // The distance between both wheels in cm
  public static final double TRACK = 10.8;	//Width decrease = turn angle increase
  
  private static Port portLS = LocalEV3.get().getPort("S1");
  private static SensorModes ls = new EV3ColorSensor(portLS);
  private static SampleProvider colorProvider = ls.getMode("RGB");
  private static float[] sampleLS = new float[colorProvider.sampleSize()];

  public static void main(String[] args) throws OdometerExceptions {

    int buttonChoice;

    // Odometer related objects
    Odometer odometer = Odometer.getOdometer(leftMotor, rightMotor, TRACK, WHEEL_RAD);
    OdometryCorrection odometryCorrection = new OdometryCorrection(colorProvider, sampleLS);
                                                                      // implementation
    Display odometryDisplay = new Display(lcd); // No need to change


    do {
      // clear the display
      lcd.clear();

      // ask the user whether the motors should drive in a square or float
      lcd.drawString("< Left | Right >", 0, 0);
      lcd.drawString("       |        ", 0, 1);
      lcd.drawString(" Float | Drive  ", 0, 2);
      lcd.drawString("motors | in a   ", 0, 3);
      lcd.drawString("       | square ", 0, 4);

      buttonChoice = Button.waitForAnyPress(); // Record choice (left or right press)
    } while (buttonChoice != Button.ID_LEFT && buttonChoice != Button.ID_RIGHT);

    if (buttonChoice == Button.ID_LEFT) {
      // Float the motors
      leftMotor.forward();
      leftMotor.flt();
      rightMotor.forward();
      rightMotor.flt();

      // Display changes in position as wheels are (manually) moved
      
      Thread odoThread = new Thread(odometer);
      odoThread.start();
      Thread odoDisplayThread = new Thread(odometryDisplay);
      odoDisplayThread.start();

    } else {
      // clear the display
      lcd.clear();

      // ask the user whether odometery correction should be run or not
      lcd.drawString("< Left | Right >", 0, 0);
      lcd.drawString("  No   | with   ", 0, 1);
      lcd.drawString(" corr- | corr-  ", 0, 2);
      lcd.drawString(" ection| ection ", 0, 3);
      lcd.drawString("       |        ", 0, 4);

      buttonChoice = Button.waitForAnyPress(); // Record choice (left or right press)

      // Start odometer and display threads
      Thread odoThread = new Thread(odometer);
      odoThread.start();
      Thread odoDisplayThread = new Thread(odometryDisplay);
      odoDisplayThread.start();

      // Start correction if right button was pressed
      if (buttonChoice == Button.ID_RIGHT) {
        Thread odoCorrectionThread = new Thread(odometryCorrection);
        odoCorrectionThread.start();
      }

      // spawn a new Thread to avoid SquareDriver.drive() from blocking
      (new Thread() {
        public void run() {
          SquareDriver.drive(leftMotor, rightMotor, WHEEL_RAD, WHEEL_RAD, TRACK);
        }
      }).start();
    }

    while (Button.waitForAnyPress() != Button.ID_ESCAPE);
    System.exit(0);
  }
}
