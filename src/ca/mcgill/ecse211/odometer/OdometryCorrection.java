/*
 * OdometryCorrection.java
 */
package ca.mcgill.ecse211.odometer;



import ca.mcgill.ecse211.lab2.SquareDriver;
import lejos.robotics.SampleProvider;

public class OdometryCorrection implements Runnable {
	private static final long CORRECTION_PERIOD = 10;
	private static final long LINE_SLEEP = 2000;
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
	
	// ((curPosition[2] - pastPosition[2] + 360) % 360 > 80)
	public void run() {
		long correctionStart, correctionEnd;

		while (true) {
			correctionStart = System.currentTimeMillis();
			
			ls.fetchSample(lsData, 0);
			
			if (lsData[0] <= 0.1) {
//				Sound.beep();
				
				SquareDriver.lineCount++;
				
				double[] curPosition = odometer.getXYT();
				
//				System.out.println("POSITION: " + curPosition[0] + ", " + curPosition[1] + ", " + curPosition[2]);
				
				if (pastPosition == null) {
					pastPosition = curPosition;
					try {
						Thread.sleep(LINE_SLEEP);
					} catch (InterruptedException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
//					System.out.println("first line");
				}
				else if (Math.abs(curPosition[2] - pastPosition[2]) > 80 && Math.abs(curPosition[2] - pastPosition[2]) < 100) {
//					System.out.println("turned " + pastPosition[2] + " to " + curPosition[2]);
					
					pastPosition = curPosition;
					
					try {
						Thread.sleep(LINE_SLEEP);
					} catch (InterruptedException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
				}
				else {
					double newX = pastPosition[0] + SquareDriver.TILE_SIZE * Math.sin(pastPosition[2] / Odometer.RAD_TO_DEG);
					double newY = pastPosition[1] + SquareDriver.TILE_SIZE * Math.cos(pastPosition[2] / Odometer.RAD_TO_DEG);
					odometer.setXYT(newX, newY, curPosition[2]);
					
					pastPosition[0] = newX;
					pastPosition[1] = newY;
					try {
						Thread.sleep(LINE_SLEEP);
					} catch (InterruptedException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
//					System.out.println("CORRECTION:    X: " + newX + "    Y: " + newY);
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
