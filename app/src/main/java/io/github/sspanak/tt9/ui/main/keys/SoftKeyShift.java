package io.github.sspanak.tt9.ui.main.keys;

import android.content.Context;
import android.util.AttributeSet;

import io.github.sspanak.tt9.commands.CmdEditText;
import io.github.sspanak.tt9.commands.CmdShift;
import io.github.sspanak.tt9.commands.CmdSpaceKorean;
import io.github.sspanak.tt9.commands.CmdSwitchInputMode;
import io.github.sspanak.tt9.ime.modes.InputMode;
import io.github.sspanak.tt9.ime.modes.InputModeKind;
import io.github.sspanak.tt9.ui.StatusIcon;
import io.github.sspanak.tt9.util.chars.Characters;

public class SoftKeyShift extends BaseSwipeableKey {
	private final CmdShift shift = new CmdShift();
	private final CmdSpaceKorean space = new CmdSpaceKorean();
	private final CmdEditText editText = new CmdEditText();

	public SoftKeyShift(Context context) {
		super(context);
	}

	public SoftKeyShift(Context context, AttributeSet attrs) {
		super(context, attrs);
	}

	public SoftKeyShift(Context context, AttributeSet attrs, int defStyleAttr) {
		super(context, attrs, defStyleAttr);
	}


	@Override
	protected boolean allowTwoStepInAccessibility() {
		return false;
	}


	private boolean isShiftEnabled() {
		return tt9 != null
			&& tt9.getLanguage() != null && tt9.getLanguage().hasUpperCase()
			&& !tt9.isVoiceInputActive()
			&& !tt9.isInputModeNumeric()
			&& !tt9.isFnPanelVisible();
	}


	private boolean areSwipesEnabled() {
		return
			tt9 != null
			&& tt9.getSettings().getShiftKeySwipes()
			&& !hasLettersOnAllKeys()
			&& !tt9.isVoiceInputActive();
	}


	/**
	 * Swiping down toggles between 123 and the last used text mode.
	 */
	private boolean isSwipeDownEnabled() {
		return
			areSwipesEnabled()
			&& !tt9.isFnPanelVisible()
			&& CmdSwitchInputMode.canToggle123(tt9);
	}


	/**
	 * Swiping up shows or hides the text editing palette (Clipboard Tools).
	 */
	private boolean isSwipeUpEnabled() {
		return
			areSwipesEnabled()
			&& (tt9.isTextEditingActive() || (!tt9.isFnPanelVisible() && editText.isAvailable(tt9)));
	}


	private boolean setVisibility() {
		if (tt9 != null && tt9.getSettings().isMainLayoutClassic()) { // no change for other layouts
			final boolean isVisible = !tt9.isFnPanelVisible();

			getOverlayWrapper();
			overlay.setVisibility(isVisible ? VISIBLE : GONE);

			return isVisible;
		}

		return true;
	}


	@Override public boolean isDynamic() { return true; }
	@Override protected String getAccessibilityText() { return hasLettersOnAllKeys() ? space.getName(tt9) : shift.getName(tt9); }
	@Override protected String getTitle() { return hasLettersOnAllKeys() ? Characters.SPACE : ""; }
	@Override protected float getTitleScale() { return hasLettersOnAllKeys() ? 1.3f * Math.min(1, getTT9Height()) * getScreenSizeScale() : super.getTitleScale(); }


	@Override
	protected int getCentralIcon() {
		if (hasLettersOnAllKeys()) {
			return 0;
		}

		final int textCase = tt9 != null ? tt9.getDisplayTextCase() : InputMode.CASE_UNDEFINED;
		return switch (textCase) {
			case InputMode.CASE_CAPITALIZE -> shift.getIconCaps();
			case InputMode.CASE_UPPER -> shift.getIconUp();
			default -> shift.getIcon();
		};
	}


	/**
	 * Indicates the swipe functions: Clipboard Tools at the top (swipe up), and the mode that swiping
	 * down will switch to at the bottom.
	 */
	@Override
	protected int getCornerIcon(int position) {
		if (position == ICON_POSITION_TOP_LEFT && isSwipeUpEnabled()) {
			return editText.getIcon();
		}

		if (position == ICON_POSITION_BOTTOM_LEFT && isSwipeDownEnabled()) {
			final int targetMode = InputModeKind.is123(tt9.getInputMode()) ? CmdSwitchInputMode.getTextModeToRestore(tt9) : InputMode.MODE_123;
			return StatusIcon.getModeIcon(tt9.getLanguage(), targetMode);
		}

		return super.getCornerIcon(position);
	}


	@Override
	protected float getCornerElementScale(int position) {
		return super.getCornerElementScale(position) * 0.75f;
	}


	@Override
	public boolean isHoldEnabled() {
		return false;
	}


	/**
	 * Holding the key does nothing. Prevent the repeated calls and the press action on release.
	 */
	@Override
	protected void handleHold() {
		preventRepeat();
	}


	@Override
	protected void handleEndSwipeY(float position, float delta) {
		if (delta > 0 && isSwipeDownEnabled()) {
			CmdSwitchInputMode.toggle123(tt9);
		} else if (delta < 0 && isSwipeUpEnabled()) {
			editText.run(tt9);
		}
	}


	@Override
	protected boolean handleRelease() {
		if (isSwipeable && !notSwiped()) {
			return true;
		}

		return hasLettersOnAllKeys() ? space.run(tt9) : shift.run(tt9);
	}


	@Override
	public void render() {
		if (!setVisibility()) {
			return;
		}

		isSwipeable = areSwipesEnabled();
		resetIconCache();

		// In 123 mode and in text editing mode, Shift does nothing when pressed, but it must remain
		// enabled for swiping.
		setEnabled(isShiftEnabled() || hasLettersOnAllKeys() || isSwipeDownEnabled() || isSwipeUpEnabled());
		super.render();
	}
}
