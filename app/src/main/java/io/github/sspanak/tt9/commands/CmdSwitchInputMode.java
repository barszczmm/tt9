package io.github.sspanak.tt9.commands;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.ArrayList;

import io.github.sspanak.tt9.ime.TraditionalT9;
import io.github.sspanak.tt9.ime.modes.InputMode;
import io.github.sspanak.tt9.ime.modes.InputModeKind;
import io.github.sspanak.tt9.ui.StatusIcon;

/**
 * Base for the commands that switch directly to a given input mode, instead of cycling through all
 * of them. It also contains the common logic for toggling between 123 and the text input modes.
 */
abstract public class CmdSwitchInputMode implements Command {
	private static int lastTextModeId = -1;
	private int icon = -1;


	/**
	 * The ID of the input mode to switch to (InputMode.MODE_*).
	 */
	abstract protected int getTargetMode();


	@Override
	public int getIcon() {
		return icon > 0 ? icon : StatusIcon.getModeIcon(null, getTargetMode());
	}


	/**
	 * Updates the icon, because it depends on the current language.
	 */
	public void invalidateIcon(@Nullable TraditionalT9 tt9) {
		icon = StatusIcon.getModeIcon(tt9 != null ? tt9.getLanguage() : null, getTargetMode());
	}


	private static boolean isSwitchingAllowed(@Nullable TraditionalT9 tt9) {
		return
			tt9 != null
			&& tt9.getAllowedInputModes().size() > 1
			&& !tt9.shouldBeOff()
			&& !tt9.isVoiceInputActive()
			&& !InputModeKind.isPassthrough(tt9.getInputMode());
	}


	@Override
	public boolean isAvailable(@Nullable TraditionalT9 tt9) {
		return isSwitchingAllowed(tt9) && tt9.getAllowedInputModes().contains(getTargetMode());
	}


	@Override
	public boolean run(@Nullable TraditionalT9 tt9) {
		return isAvailable(tt9) && switchTo(tt9, getTargetMode());
	}


	/**
	 * Remembers the current mode, if it is a text mode, so that we can return to it after leaving
	 * the 123 mode.
	 */
	static void rememberTextMode(@Nullable InputMode mode) {
		if (mode != null && !InputModeKind.is123(mode) && !InputModeKind.isPassthrough(mode) && !InputModeKind.isRecomposing(mode)) {
			lastTextModeId = mode.getId();
		}
	}


	/**
	 * Returns the text mode to restore when leaving the 123 mode: the last used one, if possible,
	 * otherwise, Predictive or the first available text mode. Returns -1 if there is none.
	 */
	public static int getTextModeToRestore(@NonNull TraditionalT9 tt9) {
		final ArrayList<Integer> allowedModes = tt9.getAllowedInputModes();

		if (lastTextModeId != InputMode.MODE_123 && allowedModes.contains(lastTextModeId)) {
			return lastTextModeId;
		}

		if (allowedModes.contains(InputMode.MODE_PREDICTIVE)) {
			return InputMode.MODE_PREDICTIVE;
		}

		for (int mode : allowedModes) {
			if (mode != InputMode.MODE_123) {
				return mode;
			}
		}

		return -1;
	}


	/**
	 * Returns true if it is possible to switch between 123 and a text mode.
	 */
	public static boolean canToggle123(@Nullable TraditionalT9 tt9) {
		return
			isSwitchingAllowed(tt9)
			&& tt9.getAllowedInputModes().contains(InputMode.MODE_123)
			&& getTextModeToRestore(tt9) >= 0;
	}


	/**
	 * Switches to 123 mode, or back to the previous text mode, when already in 123 mode.
	 */
	public static boolean toggle123(@Nullable TraditionalT9 tt9) {
		if (!canToggle123(tt9)) {
			return false;
		}

		final int target = InputModeKind.is123(tt9.getInputMode()) ? getTextModeToRestore(tt9) : InputMode.MODE_123;
		return switchTo(tt9, target);
	}


	static boolean switchTo(@NonNull TraditionalT9 tt9, int modeId) {
		final InputMode currentMode = tt9.getInputMode();
		if (currentMode.getId() == modeId) {
			return true;
		}

		if (!tt9.getAllowedInputModes().contains(modeId)) {
			return false;
		}

		rememberTextMode(currentMode);
		tt9.getSuggestionOps().scheduleDelayedAccept(currentMode.getAutoAcceptTimeout()); // restart the timer
		tt9.setInputMode(modeId);
		tt9.forceShowWindow();
		return true;
	}
}
