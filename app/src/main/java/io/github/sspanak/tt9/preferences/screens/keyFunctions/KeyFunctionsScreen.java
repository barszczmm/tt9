package io.github.sspanak.tt9.preferences.screens.keyFunctions;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.preference.DropDownPreference;
import androidx.preference.ListPreference;
import androidx.preference.Preference;
import androidx.preference.PreferenceCategory;
import androidx.preference.PreferenceScreen;

import java.util.ArrayList;
import java.util.LinkedHashMap;

import io.github.sspanak.tt9.R;
import io.github.sspanak.tt9.commands.Command;
import io.github.sspanak.tt9.commands.CommandCollection;
import io.github.sspanak.tt9.commands.NullCommand;
import io.github.sspanak.tt9.preferences.PreferencesActivity;
import io.github.sspanak.tt9.preferences.screens.BaseScreenFragment;
import io.github.sspanak.tt9.preferences.settings.SettingsStore;
import io.github.sspanak.tt9.util.Logger;

public class KeyFunctionsScreen extends BaseScreenFragment {
	public static final String NAME = "KeyFunctions";
	private static final String RESET_SWIPE_COMMANDS = "pref_reset_swipe_commands";

	@NonNull private final ArrayList<DropDownPreference> swipeDropDowns = new ArrayList<>();


	public KeyFunctionsScreen() { super(); }
	public KeyFunctionsScreen(@Nullable PreferencesActivity activity) { super(activity); }

	@Override public String getName() { return NAME; }
	@Override protected int getTitle() { return R.string.pref_category_key_functions; }
	@Override protected int getXml() { return R.xml.prefs_screen_key_functions; }


	@Override
	protected void onCreate() {
		createNumberKeySwipeOptions();
		createResetItem();
		resetFontSize(true);
	}


	private void createNumberKeySwipeOptions() {
		final PreferenceScreen screen = getPreferenceScreen();
		if (activity == null || screen == null) {
			Logger.w(NAME, "Cannot create the number key swipe options without an activity or a screen.");
			return;
		}

		// When restarting the screen, the options are already there. We only need to update them.
		if (!swipeDropDowns.isEmpty() && screen.findPreference(swipeDropDowns.get(0).getKey()) != null) {
			refreshSwipeOptions();
			return;
		}

		swipeDropDowns.clear();

		final LinkedHashMap<String, String> options = getCommandOptions(activity);
		final CharSequence[] entries = options.values().toArray(new CharSequence[0]);
		final CharSequence[] entryValues = options.keySet().toArray(new CharSequence[0]);

		for (int i = 0; i < SettingsStore.CUSTOM_ACTION_KEYS.length; i++) {
			final String actionKey = SettingsStore.CUSTOM_ACTION_KEYS[i];

			PreferenceCategory category = new PreferenceCategory(activity);
			category.setKey("category_number_key_swipes" + actionKey);
			category.setTitle(getString(R.string.pref_number_key_swipes_category, String.valueOf(i + 1)));
			category.setIconSpaceReserved(false);
			screen.addPreference(category);

			category.addPreference(createSwipeDropDown(activity, actionKey, true, entries, entryValues));
			category.addPreference(createSwipeDropDown(activity, actionKey, false, entries, entryValues));
		}

		refreshSwipeOptions();
	}


	@NonNull
	private DropDownPreference createSwipeDropDown(@NonNull Context context, @NonNull String actionKey, boolean left, CharSequence[] entries, CharSequence[] entryValues) {
		DropDownPreference dropDown = new DropDownPreference(context);
		dropDown.setKey(SettingsStore.getSwipeCommandPreferenceName(left, actionKey));
		dropDown.setTitle(left ? R.string.pref_swipe_left : R.string.pref_swipe_right);
		dropDown.setIconSpaceReserved(false);
		dropDown.setEntries(entries);
		dropDown.setEntryValues(entryValues);
		dropDown.setSummaryProvider(ListPreference.SimpleSummaryProvider.getInstance());

		// We store the value manually, to avoid persisting the default values. This way, the Retro
		// layout defaults will keep working, unless the user explicitly changes them.
		dropDown.setPersistent(false);
		dropDown.setOnPreferenceChangeListener((preference, newValue) -> {
			if (activity != null) {
				activity.getSettings().setSwipeCommand(left, actionKey, newValue != null ? newValue.toString() : null);
			}
			return true;
		});

		swipeDropDowns.add(dropDown);
		return dropDown;
	}


	/**
	 * Generates the list of all functions that can be assigned to swiping. The first option is
	 * always "None".
	 */
	@NonNull
	private static LinkedHashMap<String, String> getCommandOptions(@NonNull Context context) {
		ArrayList<Command> commands = new ArrayList<>(CommandCollection.getSwipeCommands());
		commands.sort((a, b) -> {
			String nameA = a.getName(context);
			String nameB = b.getName(context);
			return (nameA != null ? nameA : "").compareToIgnoreCase(nameB != null ? nameB : "");
		});

		LinkedHashMap<String, String> options = new LinkedHashMap<>();
		options.put(NullCommand.ID, context.getString(R.string.pref_key_function_none));
		for (Command cmd : commands) {
			String name = cmd.getName(context);
			if (name != null && !name.isEmpty()) {
				options.put(cmd.getId(), name);
			}
		}

		return options;
	}


	/**
	 * Selects the current swipe function in each dropdown.
	 */
	private void refreshSwipeOptions() {
		if (activity == null) {
			return;
		}

		final SettingsStore settings = activity.getSettings();
		for (DropDownPreference dropDown : swipeDropDowns) {
			final String prefKey = dropDown.getKey();
			for (String actionKey : SettingsStore.CUSTOM_ACTION_KEYS) {
				if (prefKey.equals(SettingsStore.getSwipeCommandPreferenceName(true, actionKey))) {
					setDropDownValue(dropDown, settings.getSwipeLeftCommand(actionKey));
				} else if (prefKey.equals(SettingsStore.getSwipeCommandPreferenceName(false, actionKey))) {
					setDropDownValue(dropDown, settings.getSwipeRightCommand(actionKey));
				}
			}
		}
	}


	private void setDropDownValue(@NonNull DropDownPreference dropDown, @NonNull String commandId) {
		// fall back to "None" for unknown commands, for example, ones that were removed in a newer version
		dropDown.setValue(dropDown.findIndexOfValue(commandId) >= 0 ? commandId : NullCommand.ID);
	}


	private void createResetItem() {
		Preference reset = findPreference(RESET_SWIPE_COMMANDS);
		if (reset == null) {
			return;
		}

		reset.setOnPreferenceClickListener(p -> {
			if (activity != null) {
				activity.getSettings().resetSwipeCommands();
				refreshSwipeOptions();
			}
			return true;
		});
	}
}
