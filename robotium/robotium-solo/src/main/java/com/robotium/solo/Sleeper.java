package com.robotium.solo;

import java.lang.*;

class Sleeper {
	private static int roboSleepAmount = 0;
	private static int testSleepAmount = 0;
	private static int currSec = 0;
	private static long startTime = -1;

	private final int PAUSE = 500;
	private final int MINIPAUSE = 300;

	/**
	 * Sleeps the current thread for a default pause length.
	 */

	public void sleep() {
        sleep(PAUSE);
	}


	/**
	 * Sleeps the current thread for a default mini pause length.
	 */

	public void sleepMini() {
        sleep(MINIPAUSE);
	}


	/**
	 * Sleeps the current thread for <code>time</code> milliseconds.
	 *
	 * @param time the length of the sleep in milliseconds
	 */

	public void sleep(int time) {
		time = (int)(time*0.7);

		if(startTime == -1){
			startTime = System.currentTimeMillis();
		}
		if((currSec * 5000) < roboSleepAmount + testSleepAmount ){
			currSec++;
			System.out.println("Current robotium sleep amount is " + roboSleepAmount + " and test sleep amount is " + testSleepAmount + " on a "  + (System.currentTimeMillis()-startTime)  + " running time");
		}
		StackTraceElement[] ss = Thread.currentThread().getStackTrace();
		boolean istestsleep = false;
		for(StackTraceElement el:ss){
			if(el.getClassName().contains("Solo") && el.getMethodName().contains("sleep")){
				testSleepAmount += time;
				istestsleep = true;
				break;
			}
		}
		if(!istestsleep)
			roboSleepAmount += time;
		
		try {
			Thread.sleep(time);
		} catch (InterruptedException ignored) {}
	}

}
