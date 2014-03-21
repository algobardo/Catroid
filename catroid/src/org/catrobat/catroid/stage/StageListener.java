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
package org.catrobat.catroid.stage;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Bitmap.Config;
import android.os.SystemClock;

import com.badlogic.gdx.ApplicationListener;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.InputMultiplexer;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.FPSLogger;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Pixmap.Format;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.BitmapFont.TextBounds;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.utils.ScreenUtils;

import org.catrobat.catroid.ProjectManager;
import org.catrobat.catroid.common.Constants;
import org.catrobat.catroid.common.LookData;
import org.catrobat.catroid.common.ScreenModes;
import org.catrobat.catroid.common.ScreenValues;
import org.catrobat.catroid.content.Look;
import org.catrobat.catroid.content.Project;
import org.catrobat.catroid.content.Sprite;
import org.catrobat.catroid.io.SoundManager;
import org.catrobat.catroid.ui.dialogs.StageDialog;
import org.catrobat.catroid.utils.Utils;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

public class StageListener implements ApplicationListener {

	private static final int AXIS_WIDTH = 4;
	private static final float DELTA_ACTIONS_DIVIDER_MAXIMUM = 50f;
	private static final int ACTIONS_COMPUTATION_TIME_MAXIMUM = 8;
	private static final boolean DEBUG = false;

	// needed for UiTests - is disabled to fix crashes with EMMA coverage
	// CHECKSTYLE DISABLE StaticVariableNameCheck FOR 1 LINES
	private static boolean DYNAMIC_SAMPLING_RATE_FOR_ACTIONS = true;

	private float deltaActionTimeDivisor = 10f;
	public static final String SCREENSHOT_AUTOMATIC_FILE_NAME = "automatic_screenshot"
			+ Constants.IMAGE_STANDARD_EXTENTION;
	public static final String SCREENSHOT_MANUAL_FILE_NAME = "manual_screenshot" + Constants.IMAGE_STANDARD_EXTENTION;
	private FPSLogger fpsLogger;

	private Stage stage;
	private boolean paused = false;
	private boolean finished = false;
	private boolean firstStart = true;
	private boolean reloadProject = false;

	private static boolean checkIfAutomaticScreenshotShouldBeTaken = true;
	private boolean makeAutomaticScreenshot = false;
	private boolean makeScreenshot = false;
	private String pathForScreenshot;
	private int screenshotWidth;
	private int screenshotHeight;
	private int screenshotX;
	private int screenshotY;
	private byte[] screenshot = null;
	// in first frame, framebuffer could be empty and screenshot
	// would be white
	private boolean skipFirstFrameForAutomaticScreenshot;

	private Project project;

	private OrthographicCamera camera;
	private SpriteBatch batch;
	private BitmapFont font;
	private Passepartout passepartout;

	private List<Sprite> sprites;

	private float virtualWidthHalf;
	private float virtualHeightHalf;
	private float virtualWidth;
	private float virtualHeight;

	private Texture axes;

	private boolean makeTestPixels = false;
	private byte[] testPixels;
	private int testX = 0;
	private int testY = 0;
	private int testWidth = 0;
	private int testHeight = 0;

	private StageDialog stageDialog;

	public int maximizeViewPortX = 0;
	public int maximizeViewPortY = 0;
	public int maximizeViewPortHeight = 0;
	public int maximizeViewPortWidth = 0;

	public boolean axesOn = false;
	public static Map<Look, Pixmap> bubble = new HashMap<Look, Pixmap>();

	private byte[] thumbnail;

	StageListener() {
	}

	@Override
	public void create() {
		font = new BitmapFont();
		font.setColor(1f, 0f, 0.05f, 1f);
		font.setScale(1.2f);

		project = ProjectManager.getInstance().getCurrentProject();
		pathForScreenshot = Utils.buildProjectPath(project.getName()) + "/";

		virtualWidth = project.getXmlHeader().virtualScreenWidth;
		virtualHeight = project.getXmlHeader().virtualScreenHeight;

		virtualWidthHalf = virtualWidth / 2;
		virtualHeightHalf = virtualHeight / 2;

		stage = new Stage(virtualWidth, virtualHeight, true);
		batch = stage.getSpriteBatch();

		Gdx.gl.glViewport(0, 0, ScreenValues.SCREEN_WIDTH, ScreenValues.SCREEN_HEIGHT);
		initScreenMode();

		sprites = project.getSpriteList();
		for (Sprite sprite : sprites) {
			sprite.resetSprite();
			sprite.look.createBrightnessContrastShader();
			stage.addActor(sprite.look);
			sprite.resume();
		}

		passepartout = new Passepartout(ScreenValues.SCREEN_WIDTH, ScreenValues.SCREEN_HEIGHT, maximizeViewPortWidth,
				maximizeViewPortHeight, virtualWidth, virtualHeight);
		stage.addActor(passepartout);

		if (DEBUG) {
			OrthoCamController camController = new OrthoCamController(camera);
			InputMultiplexer multiplexer = new InputMultiplexer();
			multiplexer.addProcessor(camController);
			multiplexer.addProcessor(stage);
			Gdx.input.setInputProcessor(multiplexer);
			fpsLogger = new FPSLogger();
		} else {
			Gdx.input.setInputProcessor(stage);
		}

		axes = new Texture(Gdx.files.internal("stage/red_pixel.bmp"));
		skipFirstFrameForAutomaticScreenshot = true;
		if (checkIfAutomaticScreenshotShouldBeTaken) {
			makeAutomaticScreenshot = project.manualScreenshotExists(SCREENSHOT_MANUAL_FILE_NAME);
		}

	}

	void menuResume() {
		if (reloadProject) {
			return;
		}
		paused = false;
		SoundManager.getInstance().resume();
		for (Sprite sprite : sprites) {
			sprite.resume();
		}
	}

	void menuPause() {
		if (finished || reloadProject) {
			return;
		}
		paused = true;
		SoundManager.getInstance().pause();
		for (Sprite sprite : sprites) {
			sprite.pause();
		}
	}

	public void reloadProject(Context context, StageDialog stageDialog) {
		if (reloadProject) {
			return;
		}
		this.stageDialog = stageDialog;

		project.getUserVariables().resetAllUserVariables();

		reloadProject = true;
	}

	@Override
	public void resume() {
		if (!paused) {
			SoundManager.getInstance().resume();
			for (Sprite sprite : sprites) {
				sprite.resume();
			}
		}

		for (Sprite sprite : sprites) {
			sprite.look.refreshTextures();
		}

	}

	@Override
	public void pause() {
		if (finished) {
			return;
		}
		if (!paused) {
			SoundManager.getInstance().pause();
			for (Sprite sprite : sprites) {
				sprite.pause();
			}
		}
	}

	public void finish() {
		finished = true;
		SoundManager.getInstance().clear();
		if (thumbnail != null && !makeAutomaticScreenshot) {
			saveScreenshot(thumbnail, SCREENSHOT_AUTOMATIC_FILE_NAME);
		}
	}

	@Override
	public void render() {
		Gdx.gl.glClearColor(1f, 1f, 1f, 1f);
		Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);
		if (reloadProject) {
			int spriteSize = sprites.size();
			for (int i = 0; i < spriteSize; i++) {
				sprites.get(i).pause();
			}
			stage.clear();
			SoundManager.getInstance().clear();

			Sprite sprite;
			if (spriteSize > 0) {
				sprites.get(0).look.setLookData(createWhiteBackgroundLookData());
			}
			for (int i = 0; i < spriteSize; i++) {
				sprite = sprites.get(i);
				sprite.resetSprite();
				sprite.look.createBrightnessContrastShader();
				stage.addActor(sprite.look);
				sprite.pause();
			}
			stage.addActor(passepartout);

			paused = true;
			firstStart = true;
			reloadProject = false;
			synchronized (stageDialog) {
				stageDialog.notify();
			}
		}

		batch.setProjectionMatrix(camera.combined);

		if (firstStart) {
			int spriteSize = sprites.size();
			if (spriteSize > 0) {
				sprites.get(0).look.setLookData(createWhiteBackgroundLookData());
			}
			Sprite sprite;
			for (int i = 0; i < spriteSize; i++) {
				sprite = sprites.get(i);
				sprite.createStartScriptActionSequence();
				if (!sprite.getLookDataList().isEmpty()) {
					sprite.look.setLookData(sprite.getLookDataList().get(0));
				}
			}
			firstStart = false;
		}
		if (!paused) {
			float deltaTime = Gdx.graphics.getDeltaTime();

			/*
			 * Necessary for UiTests, when EMMA - code coverage is enabled.
			 * 
			 * Without setting DYNAMIC_SAMPLING_RATE_FOR_ACTIONS to false(via reflection), before
			 * the UiTest enters the stage, random segmentation faults(triggered by EMMA) will occur.
			 * 
			 * Can be removed, when EMMA is replaced by an other code coverage tool, or when a
			 * future EMMA - update will fix the bugs.
			 */
			if (DYNAMIC_SAMPLING_RATE_FOR_ACTIONS == false) {
				stage.act(deltaTime);
			} else {
				float optimizedDeltaTime = deltaTime / deltaActionTimeDivisor;
				long timeBeforeActionsUpdate = SystemClock.uptimeMillis();
				while (deltaTime > 0f) {
					stage.act(optimizedDeltaTime);
					deltaTime -= optimizedDeltaTime;
				}
				long executionTimeOfActionsUpdate = SystemClock.uptimeMillis() - timeBeforeActionsUpdate;
				if (executionTimeOfActionsUpdate <= ACTIONS_COMPUTATION_TIME_MAXIMUM) {
					deltaActionTimeDivisor += 1f;
					deltaActionTimeDivisor = Math.min(DELTA_ACTIONS_DIVIDER_MAXIMUM, deltaActionTimeDivisor);
				} else {
					deltaActionTimeDivisor -= 1f;
					deltaActionTimeDivisor = Math.max(1f, deltaActionTimeDivisor);
				}
			}
		}

		if (!finished) {
			stage.draw();
		}

		if (makeAutomaticScreenshot) {
			if (skipFirstFrameForAutomaticScreenshot) {
				skipFirstFrameForAutomaticScreenshot = false;
			} else {
				thumbnail = ScreenUtils.getFrameBufferPixels(screenshotX, screenshotY, screenshotWidth,
						screenshotHeight, true);
				makeAutomaticScreenshot = false;
			}
		}

		if (makeScreenshot) {
			screenshot = ScreenUtils.getFrameBufferPixels(screenshotX, screenshotY, screenshotWidth, screenshotHeight,
					true);
			makeScreenshot = false;
		}

		if (axesOn && !finished) {
			drawAxes();
		}

		if (bubble.size() > 0 && !finished) {
			drawBubbleOnStage();
		}

		if (DEBUG) {
			fpsLogger.log();
		}

		if (makeTestPixels) {
			testPixels = ScreenUtils.getFrameBufferPixels(testX, testY, testWidth, testHeight, false);
			makeTestPixels = false;
		}
	}

	private void drawAxes() {
		batch.setProjectionMatrix(camera.combined);
		batch.begin();
		batch.draw(axes, -virtualWidthHalf, -AXIS_WIDTH / 2, virtualWidth, AXIS_WIDTH);
		batch.draw(axes, -AXIS_WIDTH / 2, -virtualHeightHalf, AXIS_WIDTH, virtualHeight);

		TextBounds bounds = font.getBounds(String.valueOf((int) virtualHeightHalf));
		font.draw(batch, "-" + (int) virtualWidthHalf, -virtualWidthHalf + 3, -bounds.height / 2);
		font.draw(batch, String.valueOf((int) virtualWidthHalf), virtualWidthHalf - bounds.width, -bounds.height / 2);

		font.draw(batch, "-" + (int) virtualHeightHalf, bounds.height / 2, -virtualHeightHalf + bounds.height + 3);
		font.draw(batch, String.valueOf((int) virtualHeightHalf), bounds.height / 2, virtualHeightHalf - 3);
		font.draw(batch, "0", bounds.height / 2, -bounds.height / 2);
		batch.end();
	}

	@Override
	public void resize(int width, int height) {
	}

	@Override
	public void dispose() {
		if (!finished) {
			this.finish();
		}
		stage.dispose();
		font.dispose();
		axes.dispose();
		disposeTextures();
	}

	public boolean makeManualScreenshot() {
		makeScreenshot = true;
		while (makeScreenshot) {
			Thread.yield();
		}
		return saveScreenshot(this.screenshot, SCREENSHOT_MANUAL_FILE_NAME);
	}

	private boolean saveScreenshot(byte[] screenshot, String fileName) {
		int length = screenshot.length;
		Bitmap fullScreenBitmap;
		Bitmap centerSquareBitmap;
		int[] colors = new int[length / 4];

		if (colors.length != screenshotWidth * screenshotHeight || colors.length == 0) {
			return false;
		}

		for (int i = 0; i < length; i += 4) {
			colors[i / 4] = android.graphics.Color.argb(255, screenshot[i + 0] & 0xFF, screenshot[i + 1] & 0xFF,
					screenshot[i + 2] & 0xFF);
		}
		fullScreenBitmap = Bitmap.createBitmap(colors, 0, screenshotWidth, screenshotWidth, screenshotHeight,
				Config.ARGB_8888);

		if (screenshotWidth < screenshotHeight) {
			int verticalMargin = (screenshotHeight - screenshotWidth) / 2;
			centerSquareBitmap = Bitmap.createBitmap(fullScreenBitmap, 0, verticalMargin, screenshotWidth,
					screenshotWidth);
		} else if (screenshotWidth > screenshotHeight) {
			int horizontalMargin = (screenshotWidth - screenshotHeight) / 2;
			centerSquareBitmap = Bitmap.createBitmap(fullScreenBitmap, horizontalMargin, 0, screenshotHeight,
					screenshotHeight);
		} else {
			centerSquareBitmap = Bitmap.createBitmap(fullScreenBitmap, 0, 0, screenshotWidth, screenshotHeight);
		}

		FileHandle image = Gdx.files.absolute(pathForScreenshot + fileName);
		OutputStream stream = image.write(false);
		try {
			new File(pathForScreenshot + Constants.NO_MEDIA_FILE).createNewFile();
			centerSquareBitmap.compress(Bitmap.CompressFormat.PNG, 100, stream);
			stream.close();
		} catch (IOException e) {
			return false;
		}
		return true;
	}

	public byte[] getPixels(int x, int y, int width, int height) {
		testX = x;
		testY = y;
		testWidth = width;
		testHeight = height;
		makeTestPixels = true;
		while (makeTestPixels) {
			Thread.yield();
		}
		byte[] copyOfTestPixels = new byte[testPixels.length];
		System.arraycopy(testPixels,0,copyOfTestPixels,0,testPixels.length);
		return copyOfTestPixels;
	}

	public void toggleScreenMode() {
		switch (project.getScreenMode()) {
			case MAXIMIZE:
				project.setScreenMode(ScreenModes.STRETCH);
				break;
			case STRETCH:
				project.setScreenMode(ScreenModes.MAXIMIZE);
				break;
		}

		initScreenMode();

		if (checkIfAutomaticScreenshotShouldBeTaken) {
			makeAutomaticScreenshot = project.manualScreenshotExists(SCREENSHOT_MANUAL_FILE_NAME);
		}
	}

	private void initScreenMode() {
		switch (project.getScreenMode()) {
			case STRETCH:
				stage.setViewport(virtualWidth, virtualHeight, false);
				screenshotWidth = ScreenValues.SCREEN_WIDTH;
				screenshotHeight = ScreenValues.SCREEN_HEIGHT;
				screenshotX = 0;
				screenshotY = 0;
				break;

			case MAXIMIZE:
				stage.setViewport(virtualWidth, virtualHeight, true);
				screenshotWidth = maximizeViewPortWidth;
				screenshotHeight = maximizeViewPortHeight;
				screenshotX = maximizeViewPortX;
				screenshotY = maximizeViewPortY;
				break;

			default:
				break;

		}
		camera = (OrthographicCamera) stage.getCamera();
		camera.position.set(0, 0, 0);
		camera.update();
	}

	public void drawBubbleOnStage() {

		batch.setProjectionMatrix(camera.combined);
		batch.begin();

		Iterator<Look> iterator = bubble.keySet().iterator();

		while (iterator.hasNext()) {
			final Look currentLook = iterator.next();
			final Texture bubbleTexture = new Texture(bubble.get(currentLook));

			final float lookScaleX = currentLook.getScaleX();
			final float lookScaleY = currentLook.getScaleY();
			float scaleOffsetX = 1;
			float scaleOffsetY = 1;

			if (lookScaleX != 1) {
				scaleOffsetX = lookScaleX > 1 ? (lookScaleX - 1) * 2 + 1 : (1 - lookScaleX) / 2;
			}

			if (lookScaleY != 1) {
				scaleOffsetY = lookScaleY > 1 ? (lookScaleY - 1) * 2 + 1 : (1 - lookScaleY) / 2;
			}

			final float zeroX = -(currentLook.getImageWidth() / 2);
			final float zeroY = -(currentLook.getImageHeight() / 2);

			final float rightTopX = zeroX + currentLook.getWidth() * (scaleOffsetX + lookScaleX);
			final float rightTopY = zeroY + currentLook.getHeight() * (scaleOffsetY + lookScaleY);
			final float rightBottomX = zeroX + currentLook.getWidth() * (scaleOffsetX + lookScaleX);
			final float rightBottomY = zeroY + currentLook.getHeight() * (scaleOffsetY);
			final float leftBottomX = zeroX + currentLook.getWidth() * (scaleOffsetX);
			final float leftBottomY = zeroY + currentLook.getHeight() * (scaleOffsetY);
			final float leftTopX = zeroX + currentLook.getWidth() * (scaleOffsetX);
			final float leftTopY = zeroY + currentLook.getHeight() * (scaleOffsetY + lookScaleY);

			float rotatedRightTopX = rightTopX;
			float rotatedRightTopY = rightTopY;
			float rotatedRightBottomX = rightBottomX;
			float rotatedRightBottomY = rightBottomY;
			float rotatedLeftBottomX = leftBottomX;
			float rotatedLeftBottomY = leftBottomY;
			float rotatedLeftTopX = leftTopX;
			float rotatedLeftTopY = leftTopY;

			float bubbleX = rightTopX;
			float bubbleY = rightTopY;

			final float lookRotation = currentLook.getRotation();

			//			if (!(lookRotation == this.oldRotation)) {
			//				Log.i("info", "ImageWidth: " + currentLook.getImageWidth());
			//				Log.i("info", "ImageHeight: " + currentLook.getImageHeight());
			//				Log.i("info", "ImageX: " + currentLook.getImageX());
			//				Log.i("info", "ImageY: " + currentLook.getImageY());
			//				Log.i("info", "X: " + currentLook.getX());
			//				Log.i("info", "Y: " + currentLook.getY());
			//				Log.i("info", "lookScaleY: " + lookScaleY);
			//				Log.i("info", "lookScaleX: " + lookScaleX);
			//				Log.i("info", "lookRotation: " + lookRotation);
			//				Log.i("info", "rightTopX:" + rightTopX + " rightTopY:" + rightTopY + " rightBottomX:" + rightBottomX
			//						+ " rightBottomY:" + rightBottomY + " leftBottomX:" + leftBottomX + " leftBottomY:"
			//						+ leftBottomY + " leftTopX:" + leftTopX + " leftTopY:" + leftTopY);
			//
			//				this.oldRotation = lookRotation;
			//			}

			if (lookRotation != 0) {
				final float cos = MathUtils.cosDeg(lookRotation);
				final float sin = MathUtils.sinDeg(lookRotation);

				rotatedRightTopX = cos * rightTopX - sin * rightTopY;
				rotatedRightTopY = sin * rightTopX + cos * rightTopY;
				rotatedRightBottomX = cos * rightBottomX - sin * rightBottomY;
				rotatedRightBottomY = sin * rightBottomX + cos * rightBottomY;
				rotatedLeftBottomX = cos * leftBottomX - sin * leftBottomY;
				rotatedLeftBottomY = sin * leftBottomX + cos * leftBottomY;
				rotatedLeftTopX = cos * leftTopX - sin * leftTopY;
				rotatedLeftTopY = sin * leftTopX + cos * leftTopY;

				if (lookRotation > 0 && lookRotation <= 90) {
					bubbleX = rotatedRightTopX - zeroX + currentLook.getX();
					bubbleY = rotatedRightTopY - zeroY + currentLook.getY();
				}
				if (lookRotation > 90 && lookRotation <= 180) {
					bubbleX = rotatedRightBottomX - zeroX + currentLook.getX();
					bubbleY = rotatedRightBottomY - zeroY + currentLook.getY();
				}
				if (lookRotation > 180 && lookRotation <= 270) {
					bubbleX = rotatedLeftBottomX - zeroX + currentLook.getX();
					bubbleY = rotatedLeftBottomY - zeroY + currentLook.getY();
				}
				if (lookRotation > 270 && lookRotation <= 360) {
					bubbleX = rotatedLeftTopX - zeroX + currentLook.getX();
					bubbleY = rotatedLeftTopY - zeroY + currentLook.getY();
				}
			}
			//TODO: Bubble outside of screen

			final int bubbleHeight = bubbleTexture.getHeight();
			final int bubbleWidth = bubbleTexture.getWidth();

			bubbleX = (bubbleX + bubbleWidth) > virtualWidthHalf ? virtualWidthHalf - bubbleWidth : bubbleX;
			bubbleY = (bubbleY + bubbleHeight) > virtualHeightHalf ? virtualHeightHalf - bubbleHeight : bubbleY;
			bubbleX = bubbleX < -virtualWidthHalf ? -virtualWidthHalf : bubbleX;
			bubbleY = bubbleY < -virtualHeightHalf ? -virtualHeightHalf : bubbleY;

			//TODO: Bubble bigger then screen
			batch.draw(bubbleTexture, bubbleX, bubbleY);

			//			batch.draw(bubbleTexture, rotatedRightTopX - zeroX + currentLook.getX(), rotatedRightTopY - zeroY
			//					+ currentLook.getY());
			//			batch.draw(bubbleTexture, rotatedRightBottomX - zeroX + currentLook.getX(), rotatedRightBottomY - zeroY
			//					+ currentLook.getY());
			//			batch.draw(bubbleTexture, rotatedLeftBottomX - zeroX + currentLook.getX(), rotatedLeftBottomY - zeroY
			//					+ currentLook.getY());
			//			batch.draw(bubbleTexture, rotatedLeftTopX - zeroX + currentLook.getX(), rotatedLeftTopY - zeroY
			//					+ currentLook.getY());
		}
		batch.end();
	}

	private LookData createWhiteBackgroundLookData() {
		LookData whiteBackground = new LookData();
		Pixmap whiteBackgroundPixmap = new Pixmap((int) virtualWidth, (int) virtualHeight, Format.RGBA8888);
		whiteBackgroundPixmap.setColor(Color.WHITE);
		whiteBackgroundPixmap.fill();
		whiteBackground.setPixmap(whiteBackgroundPixmap);
		whiteBackground.refreshTextureRegion();
		return whiteBackground;
	}

	private void disposeTextures() {
		List<Sprite> sprites = project.getSpriteList();
		int spriteSize = sprites.size();
		for (int i = 0; i > spriteSize; i++) {
			List<LookData> data = sprites.get(i).getLookDataList();
			int dataSize = data.size();
			for (int j = 0; j < dataSize; j++) {
				LookData lookData = data.get(j);
				lookData.getPixmap().dispose();
				lookData.getTextureRegion().getTexture().dispose();
			}
		}
	}
}
