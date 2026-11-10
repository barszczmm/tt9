package io.github.sspanak.tt9.preferences.settings;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.HashMap;

import io.github.sspanak.tt9.commands.CmdAddWord;
import io.github.sspanak.tt9.commands.CmdEditText;
import io.github.sspanak.tt9.commands.CmdEditWord;
import io.github.sspanak.tt9.commands.CmdVoiceInput;
import io.github.sspanak.tt9.commands.NullCommand;

public class SettingsCustomKeyActions extends SettingsUI {
	public static final String CUSTOM_ACTION_KEY_1 = "_1";
	public static final String CUSTOM_ACTION_KEY_2 = "_2";
	public static final String CUSTOM_ACTION_KEY_3 = "_3";
	public static final String CUSTOM_ACTION_KEY_4 = "_4";
	public static final String CUSTOM_ACTION_KEY_5 = "_5";
	public static final String CUSTOM_ACTION_KEY_6 = "_6";
	public static final String CUSTOM_ACTION_KEY_7 = "_7";
	public static final String CUSTOM_ACTION_KEY_8 = "_8";
	public static final String CUSTOM_ACTION_KEY_9 = "_9";
	public static final String[] CUSTOM_ACTION_KEYS = {
		CUSTOM_ACTION_KEY_1, CUSTOM_ACTION_KEY_2, CUSTOM_ACTION_KEY_3,
		CUSTOM_ACTION_KEY_4, CUSTOM_ACTION_KEY_5, CUSTOM_ACTION_KEY_6,
		CUSTOM_ACTION_KEY_7, CUSTOM_ACTION_KEY_8, CUSTOM_ACTION_KEY_9
	};

	// number keys swipe left/right
	private static final String PREF_SWIPE_LEFT_COMMAND = "pref_swipe_left_command";
	private static final String PREF_SWIPE_RIGHT_COMMAND = "pref_swipe_right_command";

	// space key
	public static final String PREF_MOVE_CURSOR_WITH_SPACE = "pref_move_cursor_with_space";
	private static final float MOVE_CURSOR_WITH_SPACE_THRESHOLD_DP = 12;

	// custom text keys ("!" and "?")
	public static final String TEXT_KEY_LEFT = "left";
	public static final String TEXT_KEY_RIGHT = "right";
	public static final String TEXT_KEY_ACTION_PRESS = "press";
	public static final String TEXT_KEY_ACTION_SWIPE_UP = "swipe_up";
	public static final String TEXT_KEY_ACTION_SWIPE_DOWN = "swipe_down";
	public static final String TEXT_KEY_ACTION_SWIPE_LEFT = "swipe_left";
	public static final String TEXT_KEY_ACTION_SWIPE_RIGHT = "swipe_right";
	public static final String[] TEXT_KEY_ACTIONS = {
		TEXT_KEY_ACTION_PRESS, TEXT_KEY_ACTION_SWIPE_UP, TEXT_KEY_ACTION_SWIPE_DOWN, TEXT_KEY_ACTION_SWIPE_LEFT, TEXT_KEY_ACTION_SWIPE_RIGHT
	};
	private static final String PREF_TEXT_KEY = "pref_text_key";
	private static final int TEXT_KEY_AUTO_LABEL_MAX_LENGTH = 3;

	protected static final HashMap<String, String> classicLayoutDefaultsSwipeLeft = new HashMap<>() {{
		put(CUSTOM_ACTION_KEY_1, CmdAddWord.ID);
		put(CUSTOM_ACTION_KEY_2, CmdEditWord.ID);
		put(CUSTOM_ACTION_KEY_5, CmdEditText.ID);
	}};

	protected static final HashMap<String, String> classicLayoutDefaultsSwipeRight = new HashMap<>() {{
		put(CUSTOM_ACTION_KEY_3, CmdVoiceInput.ID);
	}};


	protected SettingsCustomKeyActions(Context context) {
		super(context);
	}


	/******** SPACE KEY ********/

	public boolean getMoveCursorWithSpace() {
		return prefs.getBoolean(PREF_MOVE_CURSOR_WITH_SPACE, true);
	}


	/**
	 * The distance in pixels the finger must travel for the text cursor to move by one character.
	 */
	public float getMoveCursorWithSpaceThreshold() {
		return MOVE_CURSOR_WITH_SPACE_THRESHOLD_DP * context.getResources().getDisplayMetrics().density;
	}


	/******** NUMBER KEYS SWIPE LEFT/RIGHT ********/

	private static boolean isValidActionKey(@Nullable String keySuffix) {
		if (keySuffix == null || keySuffix.isEmpty()) {
			return false;
		}

		for (String key : CUSTOM_ACTION_KEYS) {
			if (key.equals(keySuffix)) {
				return true;
			}
		}

		return false;
	}


	@NonNull
	public static String getSwipeCommandPreferenceName(boolean left, @NonNull String keySuffix) {
		return (left ? PREF_SWIPE_LEFT_COMMAND : PREF_SWIPE_RIGHT_COMMAND) + keySuffix;
	}


	/**
	 * The function that runs, when no other function was selected by the user. Only the Retro layout
	 * has some default swipe functions. The Modern layout has dedicated keys for them.
	 */
	@NonNull
	public String getDefaultSwipeCommand(boolean left, @Nullable String keySuffix) {
		if (!isValidActionKey(keySuffix) || !isMainLayoutClassic()) {
			return NullCommand.ID;
		}

		String defaultCmd = left ? classicLayoutDefaultsSwipeLeft.get(keySuffix) : classicLayoutDefaultsSwipeRight.get(keySuffix);
		return defaultCmd != null ? defaultCmd : NullCommand.ID;
	}


	@NonNull
	private String getSwipeCommand(boolean left, @Nullable String keySuffix) {
		if (!isValidActionKey(keySuffix)) {
			return NullCommand.ID;
		}

		final String saved = prefs.getString(getSwipeCommandPreferenceName(left, keySuffix), null);
		if (saved == null) {
			return getDefaultSwipeCommand(left, keySuffix);
		}

		return saved.isEmpty() ? NullCommand.ID : saved;
	}


	@NonNull
	public String getSwipeRightCommand(String keySuffix) {
		return getSwipeCommand(false, keySuffix);
	}


	@NonNull
	public String getSwipeLeftCommand(String keySuffix) {
		return getSwipeCommand(true, keySuffix);
	}


	public void setSwipeCommand(boolean left, @NonNull String keySuffix, @Nullable String commandId) {
		if (isValidActionKey(keySuffix)) {
			getPrefsEditor().putString(getSwipeCommandPreferenceName(left, keySuffix), commandId == null ? NullCommand.ID : commandId).apply();
		}
	}


	public void resetSwipeCommands() {
		for (String key : CUSTOM_ACTION_KEYS) {
			getPrefsEditor().remove(getSwipeCommandPreferenceName(true, key));
			getPrefsEditor().remove(getSwipeCommandPreferenceName(false, key));
		}
		getPrefsEditor().apply();
	}


	/******** CUSTOM TEXT KEYS ********/

	@NonNull
	public static String getTextKeyPreferenceName(@NonNull String textKey, @NonNull String action, boolean label) {
		return PREF_TEXT_KEY + "_" + textKey + "_" + action + (label ? "_label" : "_text");
	}


	/**
	 * Returns the text that the given "!" or "?" key action must type, or an empty string, when the
	 * user has not set anything.
	 */
	@NonNull
	public String getTextKeyText(@NonNull String textKey, @NonNull String action) {
		final String text = prefs.getString(getTextKeyPreferenceName(textKey, action, false), "");
		return text != null ? text : "";
	}


	/**
	 * Returns the short label to display on the "!" or "?" key for the given action. When the user
	 * has not provided a label, the text itself is used, if it is short enough. Otherwise, it is
	 * abbreviated. When there is no text for this action, an empty string is returned.
	 */
	@NonNull
	public String getTextKeyLabel(@NonNull String textKey, @NonNull String action) {
		final String text = getTextKeyText(textKey, action);
		if (text.isEmpty()) {
			return "";
		}

		final String label = prefs.getString(getTextKeyPreferenceName(textKey, action, true), "");
		if (label != null && !label.trim().isEmpty()) {
			return label.trim();
		}

		final String trimmed = text.trim();
		if (trimmed.isEmpty()) {
			return text;
		}

		if (trimmed.codePointCount(0, trimmed.length()) <= TEXT_KEY_AUTO_LABEL_MAX_LENGTH) {
			return trimmed;
		}

		return trimmed.substring(0, trimmed.offsetByCodePoints(0, TEXT_KEY_AUTO_LABEL_MAX_LENGTH - 1)) + "…";
	}


	public boolean hasTextKeySwipes(@NonNull String textKey) {
		return
			!getTextKeyText(textKey, TEXT_KEY_ACTION_SWIPE_UP).isEmpty()
			|| !getTextKeyText(textKey, TEXT_KEY_ACTION_SWIPE_DOWN).isEmpty()
			|| !getTextKeyText(textKey, TEXT_KEY_ACTION_SWIPE_LEFT).isEmpty()
			|| !getTextKeyText(textKey, TEXT_KEY_ACTION_SWIPE_RIGHT).isEmpty();
	}


	public void resetTextKey(@NonNull String textKey) {
		for (String action : TEXT_KEY_ACTIONS) {
			getPrefsEditor().remove(getTextKeyPreferenceName(textKey, action, false));
			getPrefsEditor().remove(getTextKeyPreferenceName(textKey, action, true));
		}
		getPrefsEditor().apply();
	}
}
