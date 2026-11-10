package io.github.sspanak.tt9.ui.main.keys;

import android.content.Context;
import android.util.AttributeSet;

import io.github.sspanak.tt9.commands.CmdShift;
import io.github.sspanak.tt9.commands.CmdSwitchInputMode;
import io.github.sspanak.tt9.commands.CmdSpaceKorean;
import io.github.sspanak.tt9.ime.modes.InputMode;
import io.github.sspanak.tt9.ime.modes.InputModeKind;
import io.github.sspanak.tt9.ui.StatusIcon;
import io.github.sspanak.tt9.ui.Vibration;
import io.github.sspanak.tt9.util.chars.Characters;

public class SoftKeyShift extends BaseSoftKeyWithIcons {
	private final CmdShift shift = new CmdShift();
	private final CmdSpaceKorean space = new CmdSpaceKorean();

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


	/**
	 * Holding Shift toggles between 123 and the last used text mode, when enabled in the Settings.
	 */
	private boolean isHoldFor123Enabled() {
		return
			tt9 != null
			&& tt9.getSettings().getHoldShiftFor123()
			&& !tt9.isFnPanelVisible()
			&& !hasLettersOnAllKeys()
			&& CmdSwitchInputMode.canToggle123(tt9);
	}


	@Override
	public boolean isHoldEnabled() {
		return isHoldFor123Enabled();
	}


	@Override
	protected void handleHold() {
		preventRepeat();
		if (isHoldFor123Enabled() && CmdSwitchInputMode.toggle123(tt9)) {
			vibrate(Vibration.getHoldVibration());
		}
	}


	/**
	 * Indicates the mode that holding the key will switch to.
	 */
	@Override
	protected int getCornerIcon(int position) {
		if (position != ICON_POSITION_TOP_RIGHT || !isHoldFor123Enabled()) {
			return super.getCornerIcon(position);
		}

		final int targetMode = InputModeKind.is123(tt9.getInputMode()) ? CmdSwitchInputMode.getTextModeToRestore(tt9) : InputMode.MODE_123;
		return StatusIcon.getModeIcon(tt9.getLanguage(), targetMode);
	}


	@Override
	protected float getCornerElementScale(int position) {
		return position == ICON_POSITION_TOP_RIGHT ? super.getCornerElementScale(position) * 0.75f : super.getCornerElementScale(position);
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


	@Override
	protected boolean handleRelease() {
		return hasLettersOnAllKeys() ? space.run(tt9) : shift.run(tt9);
	}


	@Override
	public void render() {
		if (!setVisibility()) {
			return;
		}

		resetIconCache();
		// In 123 mode, Shift does nothing when pressed, but it must remain enabled for switching back
		// to the text modes by holding it.
		setEnabled(isShiftEnabled() || hasLettersOnAllKeys() || isHoldFor123Enabled());
		super.render();
	}
}
