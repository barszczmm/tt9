package io.github.sspanak.tt9.preferences.screens.textKeys;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.preference.Preference;
import androidx.preference.PreferenceCategory;
import androidx.preference.PreferenceScreen;

import java.util.ArrayList;

import io.github.sspanak.tt9.R;
import io.github.sspanak.tt9.preferences.PreferencesActivity;
import io.github.sspanak.tt9.preferences.screens.BaseScreenFragment;
import io.github.sspanak.tt9.preferences.settings.SettingsStore;
import io.github.sspanak.tt9.util.Logger;

/**
 * Allows setting what the "!" or "?" key types when pressed or swiped in each direction, and the
 * short labels displayed on the key for each action.
 */
abstract class AbstractTextKeyScreen extends BaseScreenFragment {
	private static final String RESET_ITEM = "pref_reset_text_key";

	@NonNull private final ArrayList<PreferenceTextKeyValue> fields = new ArrayList<>();


	AbstractTextKeyScreen() { super(); }
	AbstractTextKeyScreen(@Nullable PreferencesActivity activity) { super(activity); }

	@Override protected int getXml() { return R.xml.prefs_screen_text_key; }

	/**
	 * SettingsStore.TEXT_KEY_LEFT or SettingsStore.TEXT_KEY_RIGHT
	 */
	@NonNull abstract protected String getTextKeyId();


	@Override
	public int getPreferenceCount() {
		return -1; // prevent scrolling and item selection using the number keys, because there are text fields
	}


	@Override
	protected void onCreate() {
		createFields();
		createResetItem();
		resetFontSize(true);
	}


	private void createFields() {
		final PreferenceScreen screen = getPreferenceScreen();
		if (activity == null || screen == null) {
			Logger.w(getName(), "Cannot create the text key options without an activity or a screen.");
			return;
		}

		if (!fields.isEmpty() && screen.findPreference(fields.get(0).getKey()) != null) {
			reloadFields();
			return;
		}

		fields.clear();

		for (String action : SettingsStore.TEXT_KEY_ACTIONS) {
			PreferenceCategory category = new PreferenceCategory(activity);
			category.setKey("category_text_key_" + getTextKeyId() + "_" + action);
			category.setTitle(getActionTitle(action));
			category.setIconSpaceReserved(false);
			screen.addPreference(category);

			PreferenceTextKeyValue text = new PreferenceTextKeyValue(activity);
			text.setKey(SettingsStore.getTextKeyPreferenceName(getTextKeyId(), action, false));
			text.setTitle(R.string.pref_text_key_text);
			text.setSummary(action.equals(SettingsStore.TEXT_KEY_ACTION_PRESS) ? R.string.pref_text_key_text_press_summary : R.string.pref_text_key_text_swipe_summary);
			category.addPreference(text);
			fields.add(text);

			PreferenceTextKeyValue label = new PreferenceTextKeyValue(activity);
			label.setKey(SettingsStore.getTextKeyPreferenceName(getTextKeyId(), action, true));
			label.setTitle(R.string.pref_text_key_label);
			label.setSummary(R.string.pref_text_key_label_summary);
			category.addPreference(label);
			fields.add(label);
		}
	}


	private int getActionTitle(@NonNull String action) {
		return switch (action) {
			case SettingsStore.TEXT_KEY_ACTION_SWIPE_UP -> R.string.pref_swipe_up;
			case SettingsStore.TEXT_KEY_ACTION_SWIPE_DOWN -> R.string.pref_swipe_down;
			case SettingsStore.TEXT_KEY_ACTION_SWIPE_LEFT -> R.string.pref_swipe_left;
			case SettingsStore.TEXT_KEY_ACTION_SWIPE_RIGHT -> R.string.pref_swipe_right;
			default -> R.string.pref_press;
		};
	}


	private void reloadFields() {
		for (PreferenceTextKeyValue field : fields) {
			field.reload();
		}
	}


	private void createResetItem() {
		Preference reset = findPreference(RESET_ITEM);
		if (reset == null) {
			return;
		}

		reset.setOnPreferenceClickListener(p -> {
			if (activity != null) {
				activity.getSettings().resetTextKey(getTextKeyId());
				reloadFields();
			}
			return true;
		});
	}
}
