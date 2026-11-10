package io.github.sspanak.tt9.ui.main.keys;

import android.content.Context;
import android.util.AttributeSet;

import androidx.annotation.Nullable;

import io.github.sspanak.tt9.R;
import io.github.sspanak.tt9.commands.CmdFilterSuggestions;
import io.github.sspanak.tt9.commands.CmdMoveCursor;
import io.github.sspanak.tt9.commands.CmdNextInputMode;
import io.github.sspanak.tt9.commands.CmdTogglePredictiveMode;
import io.github.sspanak.tt9.commands.CmdVoiceInput;
import io.github.sspanak.tt9.commands.Command;
import io.github.sspanak.tt9.commands.CommandCollection;
import io.github.sspanak.tt9.commands.NullCommand;
import io.github.sspanak.tt9.ime.TraditionalT9;
import io.github.sspanak.tt9.languages.LanguageKind;

public class SoftKeyNumberSwipeable extends SoftKeyNumber {
	private float previousX;
	private String lastSwipeCommand = null;
	private final Command[] swipeCommand = { null, null };
	private boolean swipeCommandRan = false;

	public SoftKeyNumberSwipeable(Context context) { super(context); }
	public SoftKeyNumberSwipeable(Context context, AttributeSet attrs) { super(context, attrs); }
	public SoftKeyNumberSwipeable(Context context, AttributeSet attrs, int defStyleAttr) { super(context, attrs, defStyleAttr); }


	@Override
	public void setTT9(TraditionalT9 tt9) {
		super.setTT9(tt9);
		updateSwipeable();
	}


	@Override
	public void render() {
		// the swipe functions may have been changed from the Settings, so we must recheck them
		updateSwipeable();
		super.render();
	}


	private void updateSwipeable() {
		isSwipeable = false;

		if (tt9 != null) {
			final int number = getNumber();
			final String actionKey = ActionKey.numberToActionKey(number);

			isSwipeable =
				tt9.getSettings().getSwipeNumberKeysUpDown()
				|| hasCustomSwipeUpText()
				|| (number == 0 && tt9.getSettings().getMoveCursorWithSpace())
				|| (number > 0 && !(NullCommand.ID.equals(tt9.getSettings().getSwipeLeftCommand(actionKey)) && NullCommand.ID.equals(tt9.getSettings().getSwipeRightCommand(actionKey))));
		}
	}


	@Override
	protected String getRightText() { return is0Swipeable() && getNumber() == 0 ? getContext().getString(R.string.key_dpad_right) : super.getRightText(); }


	@Override
	protected String getLeftText() { return is0Swipeable() && getNumber() == 0 ? getContext().getString(R.string.key_dpad_left) : super.getLeftText(); }


	@Override
	protected int getCornerIcon(int position) {
		final Command cmd = switch (position) {
			case ICON_POSITION_BOTTOM_LEFT -> getSwipeCommand(true);
			case ICON_POSITION_BOTTOM_RIGHT -> getSwipeCommand(false);
			default -> null;
		};

		if (cmd instanceof CmdNextInputMode) {
			((CmdNextInputMode) cmd).invalidateIcon(tt9);
		}
		if (cmd instanceof CmdTogglePredictiveMode) {
			((CmdTogglePredictiveMode) cmd).invalidateIcon(tt9);
		}

		return cmd != null ? cmd.getIcon() : super.getCornerIcon(position);
	}


	@Override
	protected float getCornerElementScale(int position) {
		Command cornerCommand = getSwipeCommand(position == ICON_POSITION_BOTTOM_LEFT);

		if ((position == ICON_POSITION_BOTTOM_LEFT || position == ICON_POSITION_BOTTOM_RIGHT)
			&& !(cornerCommand instanceof CmdNextInputMode)
			&& !(cornerCommand instanceof CmdTogglePredictiveMode)
		) {
			return super.getCornerElementScale(position) * 0.75f;
		}

		return super.getCornerElementScale(position);
	}


	@Override
	protected boolean handlePress() {
		swipeCommandRan = false;
		return super.handlePress();
	}


	@Override
	protected void handleStartSwipeX(float screenX, float d) {
		previousX = screenX;
	}


	/**
	 * 0-key swipe function. Move the text cursor left or right.
	 */
	@Override
	protected void handleSwipeX(float screenX, float delta) {
		if (is0Swipeable()) {
			moveCursor(screenX);
		}
	}


	/**
	 * All other number keys swipe function. Execute the assigned command, and set the swipeCommandRan
	 * flag to avoid triggering the default press-release method. Or keep the flag off, if no action.
	 */
	@Override
	protected void handleEndSwipeX(float position, float delta) {
		Command cmd = getSwipeCommand(delta < 0);

		// avoid executing commands during voice input, to prevent undesired actions
		if (tt9 != null && tt9.isVoiceInputActive() && !(cmd instanceof CmdVoiceInput)) {
			return;
		}

		if (cmd instanceof CmdFilterSuggestions) {
			swipeCommandRan = ((CmdFilterSuggestions) cmd).run(tt9, cmd.getId().equals(lastSwipeCommand));
		} else if (cmd != null) {
			swipeCommandRan = cmd.run(tt9);
		}

		lastSwipeCommand = cmd != null && swipeCommandRan ? cmd.getId() : null;
	}


	/**
	 * Swiping up types the key letter in the opposite text case. Swiping down types the key digit.
	 * When neither is possible, the key behaves as if it was pressed normally.
	 */
	@Override
	protected void handleEndSwipeY(float position, float delta) {
		if (tt9 == null) {
			return;
		}

		final String customText = delta < 0 ? getCustomSwipeUpText() : null;
		if (customText != null && !customText.isEmpty()) {
			swipeCommandRan = tt9.onText(customText, false);
		} else if (delta < 0 && isSwipeUpEnabled()) {
			swipeCommandRan = tt9.onNumberInvertedTextCase(getNumber());
		} else if (delta > 0 && isSwipeDownEnabled()) {
			swipeCommandRan = tt9.onNumberDigit(getNumber());
		}

		if (swipeCommandRan) {
			lastSwipeCommand = null;
		}
	}


	/**
	 * Keys that allow typing a user-defined text when swiped up, return it here. When it is null or
	 * empty, swiping up falls back to the default function.
	 */
	@Nullable
	protected String getCustomSwipeUpText() {
		return null;
	}


	private boolean hasCustomSwipeUpText() {
		final String text = getCustomSwipeUpText();
		return text != null && !text.isEmpty();
	}


	/**
	 * Custom texts are allowed only when typing regular text, not in numeric fields, nor when
	 * a special panel, such as the clipboard tools, is active.
	 */
	protected boolean isCustomTextAllowed() {
		return
			tt9 != null
			&& !isFnPanelOn()
			&& !tt9.isInputModeNumeric()
			&& !tt9.isInputTypePhone()
			&& !tt9.isVoiceInputActive()
			&& !tt9.isTextEditingActive();
	}


	private boolean isSwipeYEnabled() {
		return
			tt9 != null
			&& isSwipeable
			&& tt9.getSettings().getSwipeNumberKeysUpDown()
			&& !isFnPanelOn()
			&& !tt9.isInputModeNumeric()
			&& !tt9.isVoiceInputActive()
			&& !tt9.isTextEditingActive();
	}


	private boolean isSwipeUpEnabled() {
		return
			isSwipeYEnabled()
			&& tt9.getLanguage() != null
			&& tt9.getLanguage().hasUpperCase()
			&& (getNumber() >= 2 || hasLettersOnAllKeys());
	}


	private boolean isSwipeDownEnabled() {
		return isSwipeYEnabled();
	}


	@Override
	protected boolean handleRelease() {
		if (!swipeCommandRan) {
			lastSwipeCommand = null;
		}

		return swipeCommandRan || super.handleRelease();
	}


	@Nullable
	private Command getSwipeCommand(boolean left) {
		if (tt9 == null || getNumber() == 0 || isFnPanelOn()) {
			return null;
		}

		final String aKey = ActionKey.numberToActionKey(getNumber());

		String currentCommandId = left ? tt9.getSettings().getSwipeLeftCommand(aKey) : tt9.getSettings().getSwipeRightCommand(aKey);
		Command cachedCommand = left ? swipeCommand[0] : swipeCommand[1];

		if (cachedCommand == null || !cachedCommand.getId().equals(currentCommandId)) {
			cachedCommand = CommandCollection.getById(CommandCollection.COLLECTION_SWIPE, currentCommandId);
			swipeCommand[left ? 0 : 1] = cachedCommand;
			resetIconCache();
		}

		return cachedCommand != null && cachedCommand.isAvailable(tt9) ? cachedCommand : null;
	}


	@Override
	public boolean isDynamic() {
		return
			getSwipeCommand(false) instanceof CmdNextInputMode || getSwipeCommand(true) instanceof CmdNextInputMode
			|| getSwipeCommand(false) instanceof CmdTogglePredictiveMode || getSwipeCommand(true) instanceof CmdTogglePredictiveMode;
	}


	private boolean is0Swipeable() {
		boolean fnPanelOn = isFnPanelOn();

		return
			isSwipeable
			&& getNumber() == 0
			&& tt9 != null && tt9.getSettings().getMoveCursorWithSpace()
			&& !LanguageKind.isKorean(tt9 != null ? tt9.getLanguage() : null)
			&& (!fnPanelOn || (tt9 != null && tt9.isTextEditingActive()));
	}


	/**
	 * Swipe function for the 0-key. Move the text cursor left or right, and set the swipeCommandRan
	 * flag to avoid triggering the default press-release method. Or keep the flag off, if no action.
	 */
	private void moveCursor(float screenX) {
		float delta = previousX - screenX;
		if (tt9 == null || Math.abs(delta) <= tt9.getSettings().getMoveCursorWithSpaceThreshold()) {
			return;
		}

		previousX = screenX;
		new CmdMoveCursor().run(tt9, delta > 0 ? CmdMoveCursor.CURSOR_MOVE_LEFT : CmdMoveCursor.CURSOR_MOVE_RIGHT);
		swipeCommandRan = true;
	}
}
