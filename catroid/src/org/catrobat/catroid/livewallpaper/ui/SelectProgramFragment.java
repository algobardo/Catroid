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
package org.catrobat.catroid.livewallpaper.ui;

import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.os.AsyncTask;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.LinearLayout;
import android.widget.ListView;

import com.actionbarsherlock.app.SherlockListFragment;
import com.actionbarsherlock.view.ActionMode;
import com.actionbarsherlock.view.Menu;

import org.catrobat.catroid.ProjectManager;
import org.catrobat.catroid.common.Constants;
import org.catrobat.catroid.common.ProjectData;
import org.catrobat.catroid.content.Project;
import org.catrobat.catroid.io.SoundManager;
import org.catrobat.catroid.io.StorageHandler;
import org.catrobat.catroid.livewallpaper.LiveWallpaper;
import org.catrobat.catroid.livewallpaper.R;
import org.catrobat.catroid.ui.adapter.ProjectAdapter;
import org.catrobat.catroid.ui.adapter.ProjectAdapter.OnProjectClickedListener;
import org.catrobat.catroid.ui.dialogs.CustomAlertDialogBuilder;
import org.catrobat.catroid.utils.UtilFile;
import org.catrobat.catroid.utils.Utils;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

public class SelectProgramFragment extends SherlockListFragment implements OnProjectClickedListener {
	private String selectedProject;
	private SelectProgramFragment selectProgramFragment;

	private List<ProjectData> projectList;
	private ProjectAdapter adapter;

	private ActionMode actionMode;
	private static String deleteActionModeTitle;
	private ProjectData projectToEdit;

	private ProjectManager projectManager = ProjectManager.getInstance();

	private View selectAllActionModeButton;

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		selectProgramFragment = this;
		return inflater.inflate(R.layout.fragment_lwp_select_program, container, false);
	}

	@Override
	public void onActivityCreated(Bundle savedInstanceState) {
		super.onActivityCreated(savedInstanceState);
		initListeners();
	}

	private void initListeners() {
		File rootDirectory = new File(Constants.DEFAULT_ROOT);
		File projectCodeFile;
		projectList = new ArrayList<ProjectData>();
		for (String projectName : UtilFile.getProjectNames(rootDirectory)) {
			projectCodeFile = new File(Utils.buildPath(Utils.buildProjectPath(projectName), Constants.PROJECTCODE_NAME));
			projectList.add(new ProjectData(projectName, projectCodeFile.lastModified()));
		}

		Collections.sort(projectList, new SortIgnoreCase());

		adapter = new ProjectAdapter(getActivity(), R.layout.activity_my_projects_list_item,
				R.id.my_projects_activity_project_title, projectList);
		setListAdapter(adapter);
		initClickListener();
	}

	private void initClickListener() {
		adapter.setOnProjectEditListener(this);
	}

	public void startDeleteActionMode() {
		if (actionMode == null) {
			actionMode = getSherlockActivity().startActionMode(deleteModeCallBack);
		}
	}

	private class SortIgnoreCase implements Comparator<ProjectData> {
		@Override
		public int compare(ProjectData o1, ProjectData o2) {
			String s1 = o1.projectName;
			String s2 = o2.projectName;
			return s1.toLowerCase(Locale.getDefault()).compareTo(s2.toLowerCase(Locale.getDefault()));
		}
	}

	private class LoadProject extends AsyncTask<Void, Void, Void> {
		private ProgressDialog progress;

		public LoadProject() {
			progress = new ProgressDialog(getActivity());
			progress.setTitle(getActivity().getString(R.string.please_wait));
			progress.setMessage(getActivity().getString(R.string.loading));
			progress.setCancelable(false);
		}

		@Override
		protected void onPreExecute() {
			progress.show();
			super.onPreExecute();
		}

		@Override
		protected Void doInBackground(Void... params) {
			Project project = StorageHandler.getInstance().loadProject(selectedProject);
			if (project != null) {
				if (projectManager.getCurrentProject() != null
						&& projectManager.getCurrentProject().getName().equals(selectedProject)) {
					getFragmentManager().beginTransaction().remove(selectProgramFragment).commit();
					getFragmentManager().popBackStack();
					return null;
				}
				projectManager.setProject(project);
				SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(getActivity());
				Editor editor = sharedPreferences.edit();
				editor.putString(Constants.PREF_PROJECTNAME_KEY, selectedProject);
				editor.commit();
			}
			return null;
		}

		@Override
		protected void onPostExecute(Void result) {
			LiveWallpaper.getInstance().changeWallpaperProgram();
			//			getFragmentManager().beginTransaction().remove(selectProgramFragment).commit();
			//			getFragmentManager().popBackStack();
			if (progress.isShowing()) {
				progress.dismiss();
			}

			//			Intent intent = new Intent(WallpaperManager.ACTION_CHANGE_LIVE_WALLPAPER);
			//			intent.putExtra(WallpaperManager.EXTRA_LIVE_WALLPAPER_COMPONENT, new ComponentName(LiveWallpaper
			//					.getInstance().getLWPContext(), LiveWallpaper.class));
			//			startActivity(intent);

			super.onPostExecute(result);
		}
	}

	@Override
	public void onProjectChecked() {
	}

	@Override
	public void onProjectClicked(int position) {
		selectedProject = projectList.get(position).projectName;
		CheckBox checkBox = new CheckBox(getActivity());
		checkBox.setText(R.string.lwp_enable_sound);

		final SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(getActivity());
		checkBox.setChecked(!sharedPreferences.getBoolean(Constants.PREF_SOUND_DISABLED, false));

		checkBox.setOnCheckedChangeListener(new OnCheckedChangeListener() {
			@Override
			public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {

				//SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(getActivity());
				Editor editor = sharedPreferences.edit();

				if (isChecked) {
					SoundManager.getInstance().soundDisabledByLwp = false;
					editor.putBoolean(Constants.PREF_SOUND_DISABLED, false);
				} else {
					SoundManager.getInstance().soundDisabledByLwp = true;
					editor.putBoolean(Constants.PREF_SOUND_DISABLED, true);
				}

				editor.commit();

			}

		});

		LinearLayout linearLayout = new LinearLayout(getActivity());
		linearLayout.setLayoutParams(new LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT,
				LinearLayout.LayoutParams.WRAP_CONTENT));
		linearLayout.addView(checkBox);

		AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
		builder.setView(linearLayout);
		builder.setTitle(selectedProject);
		builder.setMessage(R.string.lwp_confirm_set_program_message);
		builder.setNegativeButton(R.string.no, new DialogInterface.OnClickListener() {
			@Override
			public void onClick(DialogInterface dialog, int which) {
				dialog.cancel();

			}
		});

		builder.setPositiveButton(R.string.yes, new DialogInterface.OnClickListener() {
			@Override
			public void onClick(DialogInterface dialog, int which) {
				LoadProject Loader = new LoadProject();
				Loader.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
			}
		});
		AlertDialog alertDialog = builder.create();
		alertDialog.show();
	}

	private ActionMode.Callback deleteModeCallBack = new ActionMode.Callback() {
		@Override
		public boolean onPrepareActionMode(ActionMode mode, Menu menu) {
			return false;
		}

		@Override
		public boolean onCreateActionMode(ActionMode mode, Menu menu) {
			setSelectMode(ListView.CHOICE_MODE_MULTIPLE);

			deleteActionModeTitle = getString(R.string.delete);

			mode.setTitle(deleteActionModeTitle);
			addSelectAllActionModeButton(mode, menu);

			return true;
		}

		@Override
		public boolean onActionItemClicked(ActionMode mode, com.actionbarsherlock.view.MenuItem item) {
			return false;
		}

		@Override
		public void onDestroyActionMode(ActionMode mode) {
			if (adapter.getAmountOfCheckedProjects() == 0) {
				clearCheckedProjectsAndEnableButtons();
			} else {
				checkIfCurrentProgramSelectedForDeletion();
			}
		}

		public void setSelectMode(int selectMode) {
			adapter.setSelectMode(selectMode);
			adapter.notifyDataSetChanged();
		}

	};

	private void addSelectAllActionModeButton(ActionMode mode, Menu menu) {
		selectAllActionModeButton = Utils.addSelectAllActionModeButton(getLayoutInflater(null), mode, menu);
		selectAllActionModeButton.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View view) {
				for (int position = 0; position < projectList.size(); position++) {
					adapter.addCheckedProject(position);
				}
				adapter.notifyDataSetChanged();
				onProjectChecked();
			}

		});
	}

	private void clearCheckedProjectsAndEnableButtons() {
		setSelectMode(ListView.CHOICE_MODE_NONE);
		adapter.clearCheckedProjects();

		actionMode = null;
	}

	private void checkIfCurrentProgramSelectedForDeletion() {

		boolean currentProgramSelected = false;
		Project currentProject = projectManager.getCurrentProject();
		for (int position : adapter.getCheckedProjects()) {
			ProjectData tempProjectData = (ProjectData) getListView().getItemAtPosition(position);
			if (currentProject.getName().equalsIgnoreCase(tempProjectData.projectName)) {
				currentProgramSelected = true;
				break;
			}
		}

		if (!currentProgramSelected) {
			showConfirmDeleteDialog();
			return;
		}

		AlertDialog.Builder builder = new CustomAlertDialogBuilder(getActivity());
		builder.setTitle(R.string.error);

		if (adapter.getAmountOfCheckedProjects() == 1) {
			builder.setMessage(R.string.lwp_error_delete_current_program);
			builder.setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
				@Override
				public void onClick(DialogInterface dialog, int id) {
					clearCheckedProjectsAndEnableButtons();
				}
			});

		} else {
			builder.setMessage(R.string.lwp_error_delete_multiple_program);
			builder.setPositiveButton(R.string.yes, new DialogInterface.OnClickListener() {
				@Override
				public void onClick(DialogInterface dialog, int id) {
					showConfirmDeleteDialog();
				}
			});
			builder.setNegativeButton(R.string.no, new DialogInterface.OnClickListener() {
				@Override
				public void onClick(DialogInterface dialog, int id) {
					clearCheckedProjectsAndEnableButtons();
				}
			});
		}

		AlertDialog alertDialog = builder.create();
		alertDialog.show();

	}

	private void showConfirmDeleteDialog() {
		int titleId;
		if (adapter.getAmountOfCheckedProjects() == 1) {
			titleId = R.string.dialog_confirm_delete_program_title;
		} else {
			titleId = R.string.dialog_confirm_delete_multiple_programs_title;
		}

		AlertDialog.Builder builder = new CustomAlertDialogBuilder(getActivity());
		builder.setTitle(titleId);
		builder.setMessage(R.string.dialog_confirm_delete_program_message);
		builder.setPositiveButton(R.string.yes, new DialogInterface.OnClickListener() {
			@Override
			public void onClick(DialogInterface dialog, int id) {
				deleteCheckedProjects();
				clearCheckedProjectsAndEnableButtons();
			}
		});
		builder.setNegativeButton(R.string.no, new DialogInterface.OnClickListener() {
			@Override
			public void onClick(DialogInterface dialog, int id) {
				clearCheckedProjectsAndEnableButtons();
				dialog.cancel();
			}
		});

		AlertDialog alertDialog = builder.create();
		alertDialog.show();
	}

	private void deleteCheckedProjects() {
		int numDeleted = 0;
		for (int position : adapter.getCheckedProjects()) {
			projectToEdit = (ProjectData) getListView().getItemAtPosition(position - numDeleted);
			if (projectToEdit.projectName.equalsIgnoreCase(projectManager.getCurrentProject().getName())) {
				continue;
			}
			deleteProject();
			numDeleted++;
		}

		if (projectList.isEmpty()) {
			projectManager.initializeDefaultProject(getActivity());
		} else if (projectManager.getCurrentProject() == null) {
			Utils.saveToPreferences(getActivity().getApplicationContext(), Constants.PREF_PROJECTNAME_KEY,
					projectList.get(0).projectName);
		}

		initAdapter();
	}

	private void deleteProject() {
		StorageHandler.getInstance().deleteProject(projectToEdit);
		projectList.remove(projectToEdit);
	}

	private void initAdapter() {
		File rootDirectory = new File(Constants.DEFAULT_ROOT);
		File projectCodeFile;
		projectList = new ArrayList<ProjectData>();
		for (String projectName : UtilFile.getProjectNames(rootDirectory)) {
			projectCodeFile = new File(Utils.buildPath(Utils.buildProjectPath(projectName), Constants.PROJECTCODE_NAME));
			projectList.add(new ProjectData(projectName, projectCodeFile.lastModified()));
		}
		Collections.sort(projectList, new SortIgnoreCase());

		adapter = new ProjectAdapter(getActivity(), R.layout.activity_my_projects_list_item,
				R.id.my_projects_activity_project_title, projectList);
		setListAdapter(adapter);
		initClickListener();
	}

	public void setSelectMode(int selectMode) {
		adapter.setSelectMode(selectMode);
		adapter.notifyDataSetChanged();
	}

	public List<ProjectData> getProjectList() {
		return projectList;
	}
}
