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

import com.badlogic.gdx.scenes.scene2d.actions.SequenceAction;

import org.catrobat.catroid.R;
import org.catrobat.catroid.content.Sprite;
import org.catrobat.catroid.content.actions.ExtendedActions;
import org.catrobat.catroid.formulaeditor.Formula;

import java.util.List;

public class DroneMoveDownBrick extends DroneMoveBrick {

	private static final long serialVersionUID = 1L;

	public DroneMoveDownBrick(Sprite sprite, int timeInMillisecondsValue, int powerInPercent) {
		super(sprite, timeInMillisecondsValue, powerInPercent);
	}

	public DroneMoveDownBrick(Sprite sprite, Formula time, Formula powerInPercent) {
		super(sprite, time, powerInPercent);

	}

	public DroneMoveDownBrick() {
		super();
	}

	@Override
	protected String getBrickLabel() {
		return super.view.getResources().getString(R.string.brick_drone_move_down);
	}

	@Override
	public Brick clone() {
		return new DroneMoveDownBrick(getSprite(), timeToFlyInSeconds.clone(), powerInPercent.clone());
	}

	@Override
	public List<SequenceAction> addActionToSequence(SequenceAction sequence) {
		sequence.addAction(ExtendedActions.droneMoveDown(sprite, timeToFlyInSeconds, powerInPercent));
		return null;
	}
}
