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
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.badlogic.gdx.scenes.scene2d.actions.SequenceAction;

import org.catrobat.catroid.R;
import org.catrobat.catroid.content.Script;
import org.catrobat.catroid.content.Sprite;
import org.catrobat.catroid.content.actions.ExtendedActions;
import org.catrobat.catroid.ui.ScriptActivity;
import org.catrobat.catroid.ui.dialogs.BrickTextDialog;

import java.util.List;

public class AskBrick extends BrickBaseType {
	private static final long serialVersionUID = 1L;
	private String question = "";

	private transient View prototypeView;

	public AskBrick(Sprite sprite) {
		this.sprite = sprite;
	}

	public AskBrick() {

	}

	@Override
	public int getRequiredResources() {
		return NETWORK_CONNECTION;
	}

	public AskBrick(Sprite sprite, String question) {
		this.sprite = sprite;
		this.question = question;
	}

	@Override
	public Brick copyBrickForSprite(Sprite sprite, Script script) {
		AskBrick copyBrick = (AskBrick) clone();
		copyBrick.sprite = sprite;
		return copyBrick;
	}

	@Override
	public View getView(final Context context, int brickId, BaseAdapter baseAdapter) {
		if (animationState) {
			return view;
		}

		view = View.inflate(context, R.layout.brick_ask, null);
		view = getViewWithAlpha(alphaValue);

		setCheckboxView(R.id.brick_ask_checkbox);

		final Brick brickInstance = this;
		checkbox.setOnCheckedChangeListener(new OnCheckedChangeListener() {
			@Override
			public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
				checked = isChecked;
				adapter.handleCheck(brickInstance, isChecked);
			}
		});

		TextView textHolder = (TextView) view.findViewById(R.id.brick_ask_prototype_text_view);
		EditText editText = (EditText) view.findViewById(R.id.brick_ask_edit_text);
		editText.setText(question);

		textHolder.setVisibility(View.GONE);
		editText.setVisibility(View.VISIBLE);

		editText.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View view) {
				if (checkbox.getVisibility() == View.VISIBLE) {
					return;
				}
				ScriptActivity activity = (ScriptActivity) view.getContext();

				BrickTextDialog editDialog = new BrickTextDialog() {
					@Override
					protected void initialize() {
						input.setText(question);
						input.setSelectAllOnFocus(true);
					}

					@Override
					protected boolean getPositiveButtonEnabled() {
						return true;
					}

					@Override
					protected TextWatcher getInputTextChangedListener(Button buttonPositive) {
						return new TextWatcher() {
							@Override
							public void onTextChanged(CharSequence s, int start, int before, int count) {
							}

							@Override
							public void beforeTextChanged(CharSequence s, int start, int count, int after) {
							}

							@Override
							public void afterTextChanged(Editable s) {
							}
						};
					}

					@Override
					protected boolean handleOkButton() {
						question = (input.getText().toString()).trim();
						return true;
					}
				};

				editDialog.show(activity.getSupportFragmentManager(), "dialog_ask_brick");
			}
		});

		return view;
	}

	@Override
	public View getViewWithAlpha(int alphaValue) {
		LinearLayout layout = (LinearLayout) view.findViewById(R.id.brick_ask_layout);
		Drawable background = layout.getBackground();
		background.setAlpha(alphaValue);

		TextView askPreLabel = (TextView) view.findViewById(R.id.brick_ask_pre_text_view);
		EditText noteEditText = (EditText) view.findViewById(R.id.brick_ask_edit_text);
		TextView askPostLabel = (TextView) view.findViewById(R.id.brick_ask_post_text_view);
		askPreLabel.setTextColor(askPreLabel.getTextColors().withAlpha(alphaValue));
		askPostLabel.setTextColor(askPostLabel.getTextColors().withAlpha(alphaValue));
		noteEditText.setTextColor(noteEditText.getTextColors().withAlpha(alphaValue));
		noteEditText.getBackground().setAlpha(alphaValue);

		this.alphaValue = (alphaValue);
		return view;
	}

	@Override
	public View getPrototypeView(Context context) {
		prototypeView = View.inflate(context, R.layout.brick_ask, null);
		TextView textSpeak = (TextView) prototypeView.findViewById(R.id.brick_ask_prototype_text_view);
		textSpeak.setText(question);
		return prototypeView;
	}

	@Override
	public Brick clone() {
		return new AskBrick(this.sprite, this.question);
	}

	@Override
	public List<SequenceAction> addActionToSequence(SequenceAction sequence) {
		sequence.addAction(ExtendedActions.ask(sprite, question));
		return null;
	}
}