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
			
			/* when the light intensity is extremely low, it's due to a
			 * black line being crossed.
			 */
			if (lsData[0] <= 0.1) {
				
				SquareDriver.lineCount++;
				
				double[] curPosition = odometer.getXYT();
				
//				System.out.println("POSITION: " + curPosition[0] + ", " + curPosition[1] + ", " + curPosition[2]);
				
				// The first time the robot crosses a line:
				if (pastPosition == null) {
					pastPosition = curPosition;
					
					/* Since a black line was detected, there will not be another for a while.
					 * The thread can be safely put to sleep for about two seconds, at which point
					 * it will be a few inches from another line.
					 */
					try {
						Thread.sleep(LINE_SLEEP);
					} catch (InterruptedException e) {
						e.printStackTrace();
					}
//					System.out.println("first line");
				}
				/* When the robot turns 90-degrees
				 * -------------------------------
				 * The modular distance between two angles (d(270,0) = 90, NOT 270!)
				 * A change in direction is only handled when 80 < angle < 100
				 * These bounds prevent situations where small heading changes result in
				 * a large angle distances. (E.g. 0.01 - 0.02 mod 360 = -0.01 mod 360 = 359.99 mod 360,
				 * clearly the robot didn't rotate 359.99 degrees)
				 */
				else if (80 < ((curPosition[2] - pastPosition[2] + 360) % 360) && ((curPosition[2] - pastPosition[2] + 360) % 360) < 100) {
//					System.out.println("turned " + pastPosition[2] + " to " + curPosition[2]);
					
					pastPosition = curPosition;
					
					/* Since a black line was detected, there will not be another for a while.
					 * The thread can be safely put to sleep for about two seconds, at which point
					 * it will be a few inches from another line.
					 */
					try {
						Thread.sleep(LINE_SLEEP);
					} catch (InterruptedException e) {
						e.printStackTrace();
					}
				}
				// When the robot crosses a black line (no turning)
				else {
					/* Since the direction of the robot is tracked in it's angle and the angles
					 * are in multiples of approximately 90-degrees, multiply the tile length by
					 *  sin(angle) or cos(angle). This eliminates the need to keep track of the robot's heading
					 *  by other means.
					 */					
					double newX = pastPosition[0] + SquareDriver.TILE_SIZE * Math.sin(pastPosition[2] / Odometer.RAD_TO_DEG);
					double newY = pastPosition[1] + SquareDriver.TILE_SIZE * Math.cos(pastPosition[2] / Odometer.RAD_TO_DEG);
					odometer.setXYT(newX, newY, curPosition[2]);
					
					// keep the current position for later use
					pastPosition[0] = newX;
					pastPosition[1] = newY;
					
					/* Since a black line was detected, there will not be another for a while.
					 * The thread can be safely put to sleep for about two seconds, at which point
					 * it will be a few inches from another line.
					 */
					try {
						Thread.sleep(LINE_SLEEP);
					} catch (InterruptedException e) {
						e.printStackTrace();
					}
//					System.out.println("CORRECTION:    X: " + newX + "    Y: " + newY);
				}
				
			}

			// this ensures the odometer's correction occurs only once every period
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
