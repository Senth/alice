/*
    Copyright (c) 2005, Corey Goldberg

    StopWatch.java is free software; you can redistribute it and/or modify
    it under the terms of the GNU General Public License as published by
    the Free Software Foundation; either version 2 of the License, or
    (at your option) any later version.
 */

package alice;

/**
 * Debugging class, will not be used later
 * 
 * @author Corey Goldberg
 */
public class StopWatch {

	/**
	 * The start time
	 */
	private long startTime = 0;
	/**
	 * The stop time
	 */
	private long stopTime = 0;
	/**
	 * if stop-watch is running
	 */
	private boolean running = false;

	/**
	 * Start the stop-watch
	 */
	public void start() {
		this.startTime = System.currentTimeMillis();
		this.running = true;
	}

	/**
	 * Stop the stop-watch
	 */
	public void stop() {
		this.stopTime = System.currentTimeMillis();
		this.running = false;
	}

	/**
	 * @return elapsed time in milliseconds
	 */
	public long getElapsedTime() {
		long elapsed;
		if (running) {
			elapsed = (System.currentTimeMillis() - startTime);
		} else {
			elapsed = (stopTime - startTime);
		}
		return elapsed;
	}

	/**
	 * @return elapsed time in seconds
	 */
	public long getElapsedTimeSecs() {
		long elapsed;
		if (running) {
			elapsed = ((System.currentTimeMillis() - startTime) / 1000);
		} else {
			elapsed = ((stopTime - startTime) / 1000);
		}
		return elapsed;
	}
}
