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
package org.catrobat.catroid.livewallpaper;

import android.annotation.SuppressLint;
import android.app.WallpaperManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.speech.tts.TextToSpeech;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.WindowManager;
import android.widget.Toast;

import com.badlogic.gdx.ApplicationListener;
import com.badlogic.gdx.backends.android.AndroidApplicationConfiguration;
import com.badlogic.gdx.backends.android.AndroidLiveWallpaperService;

import org.catrobat.catroid.common.Constants;
import org.catrobat.catroid.common.ScreenValues;
import org.catrobat.catroid.formulaeditor.SensorHandler;
import org.catrobat.catroid.io.SoundManager;
import org.catrobat.catroid.stage.PreStageActivity;
import org.catrobat.catroid.stage.StageListener;
import org.catrobat.catroid.utils.Utils;

@SuppressLint("NewApi")
public class LiveWallpaper extends AndroidLiveWallpaperService {

	private static LiveWallpaper INSTANCE;
	private AndroidApplicationConfiguration cfg;
	private LiveWallpaperEngine lastCreatedWallpaperEngine;
	private Context context;
	private LiveWallpaperEngine previewEngine;
	private LiveWallpaperEngine homeEngine;
	private StageListener LWPStageListener = null;

	/**
	 * @return the previewEngine
	 */
	public LiveWallpaperEngine getPreviewEngine() {
		return previewEngine;
	}

	/**
	 * @param previewEngine
	 *            the previewEngine to set
	 */
	public void setPreviewEngine(LiveWallpaperEngine previewEngine) {
		this.previewEngine = previewEngine;
		this.previewEngine.name = "Preview";
	}

	@Override
	public void onCreate() {
		super.onCreate();
		//android.os.Debug.waitForDebugger();
		INSTANCE = this;
		SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
		SoundManager.getInstance().soundDisabledByLwp = sharedPreferences.getBoolean(Constants.PREF_SOUND_DISABLED,
				false);
		context = this;
	}

	public static LiveWallpaper getInstance() {
		return INSTANCE;
	}

	@Override
	public void onDestroy() {
		super.onDestroy();
		PreStageActivity.shutDownTextToSpeechForLiveWallpaper();
	}

	@Override
	public ApplicationListener createListener(boolean isPreview) {
		setScreenSize(isPreview);
		LWPStageListener = new StageListener(true);
		return LWPStageListener;
	}

	public void changeWallpaperProgram() {
		previewEngine.changeWallpaperProgram();

		//homeEngine.changeWallpaperProgram();
	}

	@Override
	public AndroidApplicationConfiguration createConfig() {
		if (cfg == null) {
			cfg = new AndroidApplicationConfiguration();
			cfg.useGL20 = true;
		}
		return cfg;
	}

	@Override
	public Engine onCreateEngine() {
		Utils.loadProjectIfNeeded(getApplicationContext());
		lastCreatedWallpaperEngine = new LiveWallpaperEngine();
		return lastCreatedWallpaperEngine;
	}

	private void setScreenSize(boolean isPreview) {
		DisplayMetrics displayMetrics = new DisplayMetrics();
		int currentApiVersion = android.os.Build.VERSION.SDK_INT;
		if (!isPreview && currentApiVersion >= 19) {
			((WindowManager) getSystemService(WINDOW_SERVICE)).getDefaultDisplay().getRealMetrics(displayMetrics);
		} else {
			((WindowManager) getSystemService(WINDOW_SERVICE)).getDefaultDisplay().getMetrics(displayMetrics);
		}
		ScreenValues.SCREEN_WIDTH = displayMetrics.widthPixels;
		ScreenValues.SCREEN_HEIGHT = displayMetrics.heightPixels;
	}

	@Override
	public void offsetChange(ApplicationListener listener, float xOffset, float yOffset, float xOffsetStep,
			float yOffsetStep, int xPixelOffset, int yPixelOffset) {
		// TODO Auto-generated method stub

	}

	/**
	 * @return the homeEngine
	 */
	public LiveWallpaperEngine getHomeEngine() {
		return homeEngine;
	}

	/**
	 * @param homeEngine
	 *            the homeEngine to set
	 */
	public void setHomeEngine(LiveWallpaperEngine homeEngine) {
		this.homeEngine = homeEngine;
		this.homeEngine.name = "Home";
	}

	public class LiveWallpaperEngine extends AndroidWallpaperEngine {

		public String name = "";
		private static final int REFRESH_RATE = 300;
		private boolean mVisible = false;
		private final Handler mHandler = new Handler();
		private final Runnable mUpdateDisplay = new Runnable() {
			@Override
			public void run() {
				if (mVisible) {
					//mHandler.post(mUpdateDisplay);
					mHandler.postDelayed(mUpdateDisplay, REFRESH_RATE);
				}
			}
		};

		public LiveWallpaperEngine() {
			super();
			activateTextToSpeechIfNeeded();
			SensorHandler.startSensorListener(getApplicationContext());
		}

		private StageListener getLocalStageListener() {
			//if (this.isPreview()) {
			return LWPStageListener;
			//} else {
			//return homeScreenStageListener;
			//}
		}

		/*
		 * (non-Javadoc)
		 * 
		 * @see
		 * com.badlogic.gdx.backends.android.AndroidLiveWallpaperService.AndroidWallpaperEngine#onCreate(android.view
		 * .SurfaceHolder)
		 */
		@Override
		public void onCreate(SurfaceHolder surfaceHolder) {
			if (isPreview()) {
				setPreviewEngine(this);
			} else {
				setHomeEngine(this);
			}
			super.onCreate(surfaceHolder);
		}

		@Override
		public void onVisibilityChanged(boolean visible) {
			Log.d("LWP", "Engine: " + name + " the engine is visible: " + visible);
			mVisible = visible;
			super.onVisibilityChanged(visible);
		}

		@Override
		public void onResume() {
			if (!mVisible) {
				return;
			}

			if (getLocalStageListener().isFinished()) {
				return;
			}
			Log.d("LWP", "Resuming  " + name + ": " + " SL-" + getLocalStageListener().hashCode());
			SensorHandler.startSensorListener(getApplicationContext());
			getLocalStageListener().menuResume();
			mHandler.postDelayed(mUpdateDisplay, REFRESH_RATE);

		}

		@Override
		public void onPause() {
			mHandler.removeCallbacks(mUpdateDisplay);

			if (getLocalStageListener().isFinished()) {
				return;
			}

			SensorHandler.stopSensorListeners();
			getLocalStageListener().menuPause();
			Log.d("LWP", "Pausing " + name + ": " + " SL-" + getLocalStageListener().hashCode());
		}

		@Override
		public void onSurfaceChanged(SurfaceHolder holder, int format, int width, int height) {
			super.onSurfaceChanged(holder, format, width, height);

			if (width > height) {
				Toast.makeText(context, context.getResources().getString(R.string.lwp_no_landscape_support),
						Toast.LENGTH_SHORT).show();
				getLocalStageListener().finish();
				mHandler.removeCallbacks(mUpdateDisplay);
				return;
			}

			if (mVisible) {
				mHandler.postDelayed(mUpdateDisplay, REFRESH_RATE);
			} else {
				mHandler.removeCallbacks(mUpdateDisplay);
			}
		}

		@Override
		public void onSurfaceDestroyed(SurfaceHolder holder) {
			Log.d("LWP", "destroying surface");
			mVisible = false;
			mHandler.removeCallbacks(mUpdateDisplay);
			super.onSurfaceDestroyed(holder);
		}

		@Override
		public void onDestroy() {
			mVisible = false;
			Log.d("LWP", "destroying engine");
			mHandler.removeCallbacks(mUpdateDisplay);
			super.onDestroy();
		}

		public void changeWallpaperProgram() {
			//			try {
			//				WallpaperManager.getInstance(getApplicationContext()).clear();
			//			} catch (IOException e) {
			//				// TODO Auto-generated catch block
			//				e.printStackTrace();
			//			}

			WallpaperManager.getInstance(getApplicationContext()).forgetLoadedWallpaper();

			LWPStageListener.reloadProject(getApplicationContext());
			//homeEngine.activateTextToSpeechIfNeeded();
		}

		private void activateTextToSpeechIfNeeded() {
			if (PreStageActivity.initTextToSpeechForLiveWallpaper(context) != 0) {
				Intent installIntent = new Intent();
				installIntent.setAction(TextToSpeech.Engine.ACTION_INSTALL_TTS_DATA);
				startActivity(installIntent);
			}
		}
	}
}
