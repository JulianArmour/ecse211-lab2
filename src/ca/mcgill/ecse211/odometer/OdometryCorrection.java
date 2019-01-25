/*
 * OdometryCorrection.java
 */
package ca.mcgill.ecse211.odometer;

import ca.mcgill.ecse211.lab2.Lab2;
import ca.mcgill.ecse211.lab2.SquareDriver;
import lejos.hardware.Sound;
import lejos.robotics.Color;
import lejos.robotics.SampleProvider;

public class OdometryCorrection implements Runnable {
	private static final long CORRECTION_PERIOD = 10;
	private Odometer odometer;
	private SampleProvider ls;
	private float[] lsData;
	private double[] pastPosition;

	/**
	 * This is the default class constructor. An existing instance of the odometer
	 * is used. This is to ensure thread safety.
	 * @param sampleLS 
	 * @param colorProvider 
	 * 
	 * @throws OdometerExceptions
	 */
	public OdometryCorrection(SampleProvider colorProvider, float[] sampleLS) throws OdometerExceptions {
		this.odometer = Odometer.getOdometer();
		this.ls = colorProvider;
		this.lsData = sampleLS;
		this.pastPosition = null;
	}

	/**
	 * Here is where the odometer correction code should be run.
	 * 
	 * @throws OdometerExceptions
	 */
	// run method (required for Thread)
	public void run() {
		long correctionStart, correctionEnd;

		while (true) {
			correctionStart = System.currentTimeMillis();
			
			ls.fetchSample(lsData, 0);
			
			if ((int) lsData[0] == Color.BLACK) {
				Sound.beep();
				
				double[] curPosition = odometer.getXYT();
				
				// the first time the robot crosses the line
				if (pastPosition == null) {
					pastPosition = curPosition;
				}
				// the robot turned 90-degrees. We need to reset the correction.
				// since the distance is measured in degrees, we need to use
				// cyclic distance instead of Euclidean distance
				else if ((curPosition[2] - pastPosition[2] + 360) % 360 > 85) {
					pastPosition = curPosition;
				}
				else {
//					double dx = curPosition[0] - pastPosition[0];
//					double dy = curPosition[1] - pastPosition[1];
//					double odometerDistance = Math.sqrt(Math.pow(dx, 2) + Math.pow(dy, 2));
//					double error = SquareDriver.TILE_SIZE - odometerDistance;
//					double errorX = error * Math.sin(curPosition[2] / Odometer.RAD_TO_DEG);
//					double errorY = error * Math.cos(curPosition[2] / Odometer.RAD_TO_DEG);
					
					double newX = SquareDriver.TILE_SIZE * Math.sin(pastPosition[2] / Odometer.RAD_TO_DEG);
					double newY = SquareDriver.TILE_SIZE * Math.cos(pastPosition[2] / Odometer.RAD_TO_DEG);
					
					odometer.setX(newX);
					odometer.setY(newY);
				}
			}

			// this ensures the odometry correction occurs only once every period
			correctionEnd = System.currentTimeMillis();
			if (correctionEnd - correctionStart < CORRECTION_PERIOD) {
				try {
					Thread.sleep(CORRECTION_PERIOD - (correctionEnd - correctionStart));
				} catch (InterruptedException e) {
					// there is nothing to be done here
				}
			}
		}
	}
}
