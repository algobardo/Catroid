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
package org.catrobat.catroid.content.bricks;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.ArrayAdapter;
import android.widget.BaseAdapter;
import android.widget.LinearLayout;
import android.widget.Spinner;

import com.badlogic.gdx.scenes.scene2d.actions.SequenceAction;

import org.catrobat.catroid.R;
import org.catrobat.catroid.content.Script;
import org.catrobat.catroid.content.Sprite;
import org.catrobat.catroid.content.WhenVirtualButtonScript;

import java.util.List;

public class WhenVirtualButtonBrick extends ScriptBrick {
	protected WhenVirtualButtonScript whenVirtualButtonScript;
	private static final long serialVersionUID = 1L;

	public static enum Action {
		TOUCH(0), HOLD(1), WIPE_UP(2), WIPE_DOWN(3), WIPE_LEFT(4), WIPE_RIGHT(5), TOUCH_RIGHT(11),TOUCH_LEFT(12),TOUCH_TOP(13),TOUCH_BOTTOM(14);
        // NEW

		private int id;

		private Action(int id) {
			this.id = id;
		}

		public int getId() {
			return this.id;
		}

		public static Action getActionById(int id) {
			for (int i = 0; i < Action.values().length; i++) {
				Action action = Action.values()[i];
				if (action.getId() == id) {
					return action;
				}
			}
			return null;
		}

		public static int getIdByAction(Action action) {
			for (int i = 0; i < Action.values().length; i++) {
				Action tmpAction = Action.values()[i];
				if (tmpAction.getId() == action.getId()) {
					return i;
				}
			}
			return 0;
		}
	}

	private transient Action action;

	public WhenVirtualButtonBrick(Sprite sprite, WhenVirtualButtonScript whenVirtualButtonScript, Action action) {
		this.whenVirtualButtonScript = whenVirtualButtonScript;
		this.sprite = sprite;
		this.action = action;

		if (this.action == null) {
			this.action = Action.TOUCH;
		}

		if (this.whenVirtualButtonScript == null) {
			this.whenVirtualButtonScript = new WhenVirtualButtonScript(this.sprite, this.action.getId());
		} else {
			this.whenVirtualButtonScript.setId(this.action.getId());
		}

	}

	public WhenVirtualButtonBrick() {
		if (this.action == null) {
			this.action = Action.TOUCH;
		}
	}

	@Override
	public int getRequiredResources() {
		return NO_RESOURCES;
	}

	@Override
	public Brick copyBrickForSprite(Sprite sprite, Script script) {
		WhenVirtualButtonBrick copyBrick = (WhenVirtualButtonBrick) clone();
		copyBrick.sprite = sprite;
		copyBrick.whenVirtualButtonScript = (WhenVirtualButtonScript) script;
		return copyBrick;
	}

	@Override
	public View getView(final Context context, int brickId, final BaseAdapter baseAdapter) {
		if (whenVirtualButtonScript == null) {
			whenVirtualButtonScript = new WhenVirtualButtonScript(sprite, action.getId());
		}

		view = View.inflate(context, R.layout.brick_when_virtual_button, null);

		final Spinner actionSpinner = (Spinner) view.findViewById(R.id.brick_when_virtual_button_spinner);

		ArrayAdapter<CharSequence> arrayAdapter = ArrayAdapter.createFromResource(context,
				R.array.virtual_button_chooser, android.R.layout.simple_spinner_item);
		arrayAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);

		actionSpinner.setAdapter(arrayAdapter);
		actionSpinner.setClickable(true);
		actionSpinner.setFocusable(true);

		actionSpinner.setOnItemSelectedListener(new OnItemSelectedListener() {

			@Override
			public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
				action = Action.values()[position];
				if (whenVirtualButtonScript == null) {
					whenVirtualButtonScript = new WhenVirtualButtonScript(sprite, action.getId());
				} else {
					whenVirtualButtonScript.setId(action.getId());
				}
			}

			@Override
			public void onNothingSelected(AdapterView<?> parent) {
			}
		});

		//actionSpinner.setSelection(action.ordinal());
		actionSpinner.setSelection(Action.getIdByAction(action));
		actionSpinner.setFocusable(false);

		return view;
	}

	@Override
	public View getViewWithAlpha(int alphaValue) {
		LinearLayout layout = (LinearLayout) view.findViewById(R.id.brick_when_virtual_button_layout);
		Drawable background = layout.getBackground();
		background.setAlpha(alphaValue);
		this.alphaValue = (alphaValue);
		return view;
	}

	@Override
	public View getPrototypeView(Context context) {
		return getView(context, 0, null);
	}

	@Override
	public Brick clone() {
		return new WhenVirtualButtonBrick(getSprite(), whenVirtualButtonScript, action);
	}

	@Override
	public Script initScript(Sprite sprite) {
		if (whenVirtualButtonScript == null) {
			whenVirtualButtonScript = new WhenVirtualButtonScript(sprite, action.getId());
		}

		return whenVirtualButtonScript;
	}

	@Override
	public List<SequenceAction> addActionToSequence(SequenceAction sequence) {
		return null;

	}
}