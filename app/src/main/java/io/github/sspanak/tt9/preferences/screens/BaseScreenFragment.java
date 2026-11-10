package io.github.sspanak.tt9.preferences.screens;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.preference.Preference;
import androidx.preference.PreferenceFragmentCompat;
import androidx.preference.PreferenceGroup;

import io.github.sspanak.tt9.languages.LanguageCollection;
import io.github.sspanak.tt9.preferences.PreferencesActivity;
import io.github.sspanak.tt9.preferences.custom.ScreenPreferencesList;
import io.github.sspanak.tt9.preferences.settings.SettingsUI;
import io.github.sspanak.tt9.util.Logger;
import io.github.sspanak.tt9.util.sys.DeviceInfo;

abstract public class BaseScreenFragment extends PreferenceFragmentCompat {
	@Nullable protected PreferencesActivity activity;
	private ScreenPreferencesList preferencesList;


	public BaseScreenFragment() { init(null); }
	public BaseScreenFragment(@Nullable PreferencesActivity activity) { init(activity); }


	protected void init(@Nullable PreferencesActivity activity) {
		this.activity = activity == null ? (PreferencesActivity) getActivity() : activity;
		setScreenTitle();
	}


	public void restart(@NonNull PreferencesActivity activity) {
		init(activity);
		onCreate();
	}


	private void setScreenTitle() {
		if (activity != null) {
			activity.setScreenTitle(getTitle());
		}
	}


	private void initPreferencesList() {
		if (preferencesList == null) {
			preferencesList = new ScreenPreferencesList(getPreferenceScreen());
		}
	}


	@Override
	public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
		LanguageCollection.init(getContext());
		setPreferencesFromResource(getXml(), rootKey);
		fixIntentPackages(getPreferenceScreen());

		if (activity == null) {
			activity = (PreferencesActivity) getActivity();
		}

		if (activity == null) {
			Logger.w(
				"MainSettingsScreen",
				"Starting up without an Activity. Preference Items will not be fully initialized."
			);
			return;
		}

		onCreate();
	}


	/**
	 * The preference XML files contain intents with a hardcoded "targetPackage". When the app is built
	 * with a different application ID (e.g. to be installed alongside the original), they would open
	 * the activities of the other app, or nothing at all. So, we point them to the current package.
	 */
	private void fixIntentPackages(@Nullable PreferenceGroup group) {
		final Context context = getContext();
		if (group == null || context == null) {
			return;
		}

		for (int i = 0; i < group.getPreferenceCount(); i++) {
			final Preference pref = group.getPreference(i);
			final Intent intent = pref.getIntent();
			if (intent != null && intent.getComponent() != null) {
				intent.setClassName(context.getPackageName(), intent.getComponent().getClassName());
			}

			if (pref instanceof PreferenceGroup) {
				fixIntentPackages((PreferenceGroup) pref);
			}
		}
	}


	@Override
	public void onResume() {
		super.onResume();
		setScreenTitle();
		setActivityOptionCounter();
	}


	public int getPreferenceCount() {
		initPreferencesList();
		return preferencesList.size();
	}


	private void setActivityOptionCounter() {
		if (activity != null) {
			activity.setOptionsCount(this::getPreferenceCount);
		}
	}


	public void resetFontSize(boolean reloadList) {
		if (activity == null) {
			return;
		}

		initPreferencesList();
		preferencesList.getAll(reloadList, true);
		if (DeviceInfo.AT_LEAST_ANDROID_12) {
			preferencesList.setFontSize(SettingsUI.FONT_SIZE_DEFAULT);
		} else {
			preferencesList.setFontSize(activity.getSettings().getSettingsFontSize());
		}
	}


	abstract public String getName();
	abstract protected int getTitle();
	abstract protected int getXml();
	abstract protected void onCreate();
}
