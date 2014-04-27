/**
 *  Catroid: An on-device visual programming system for Android devices
 *  Copyright (C) 2010-2013 The Catrobat Team
 *  (<http://developer.catrobat.org/credits>)
 *  
 *  This program is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU Affero General Public License as
 *  published by the Free Software Foundation, either version 3 of the
 *  License, or (at your option) any later version.
 *  
 *  An additional term exception under section 7 of the GNU Affero
 *  General Public License, version 3, is available at
 *  http://developer.catrobat.org/license_additional_term
 *  
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 *  GNU Affero General Public License for more details.
 *  
 *  You should have received a copy of the GNU Affero General Public License
 *  along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.catrobat.catroid.test.drone;

import static java.lang.Thread.sleep;

import android.content.Intent;
import android.test.ActivityInstrumentationTestCase2;
import android.util.Log;

import com.parrot.freeflight.service.DroneControlService;

import org.catrobat.catroid.stage.PreStageActivity;
import org.catrobat.catroid.stage.StageActivity;
import org.catrobat.catroid.uitest.util.Reflection;

public class PreStageActivityDroneTest extends ActivityInstrumentationTestCase2<PreStageActivity> {

	private static final String TAG = PreStageActivityDroneTest.class.getSimpleName();

	PreStageActivity preStageActivity;
	DroneControlService droneControlService;
	StageActivity stageActivity;
	Intent stageActivityIntent;

	public PreStageActivityDroneTest() {
		super(PreStageActivity.class);
	}

	@Override
	protected void setUp() throws Exception {
		super.setUp();
		preStageActivity = null;
		droneControlService = null;
		stageActivity = null;
		stageActivityIntent = null;
		DroneTestUtils.createDroneProjectWithScriptAndAllDroneMoveBricks();
		System.setProperty("dexmaker.dexcache", getInstrumentation().getTargetContext().getCacheDir().getPath());
		preStageActivity = getActivity();
	}

	public void testDroneServiceStart() {
		getDroneControlServiceFromPrestage(preStageActivity);
		assertNull("DroneControlServce must not be started", droneControlService);

		preStageActivity.onDroneAvailabilityChanged(true);
		waitForDroneServiceToStart();
		assertNotNull("DroneControlService must be instanciated", droneControlService);

		Reflection.invokeMethod(preStageActivity, "resourceInitialized");
	}

	//Duplicate code frome here on:
	private void waitForDroneServiceToStart() {
		for (int i = 0; i < 10; i++) { //waiting for the service to start
			Log.d(TAG, "Spinning=" + i);
			try {
				sleep(500);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
			getDroneControlServiceFromPrestage(preStageActivity);
			if (droneControlService != null) {
				break;
			}
		}
	}

	private void getDroneControlServiceFromPrestage(PreStageActivity activity) {
		droneControlService = (DroneControlService) Reflection.getPrivateField(activity, "droneControlService");
	}
}
