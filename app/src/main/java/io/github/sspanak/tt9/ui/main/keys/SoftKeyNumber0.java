package io.github.sspanak.tt9.ui.main.keys;

import android.content.Context;
import android.util.AttributeSet;
import android.view.KeyEvent;

import io.github.sspanak.tt9.commands.CmdSpaceKorean;
import io.github.sspanak.tt9.ime.helpers.Key;
import io.github.sspanak.tt9.ime.modes.InputModeKind;
import io.github.sspanak.tt9.ui.Vibration;
import io.github.sspanak.tt9.util.TextTools;
import io.github.sspanak.tt9.util.chars.Characters;

public class SoftKeyNumber0 extends SoftKeyNumberSwipeable {
	private static final String CHARS_NUMERIC_MODE = "+%$";
	private static final int SPACE_KEY_HOLD_TEXT_MAX_CHARS = 3;

	private final CmdSpaceKorean spaceCommand = new CmdSpaceKorean();

	public SoftKeyNumber0(Context context) { super(context); }
	public SoftKeyNumber0(Context context, AttributeSet attrs) { super(context, attrs); }
	public SoftKeyNumber0(Context context, AttributeSet attrs, int defStyleAttr) { super(context, attrs, defStyleAttr); }


	@Override
	protected boolean allowTwoStepInAccessibility() {
		return tt9 == null || !tt9.isTouchExplorationEnabled();
	}


	private boolean shouldBeTransparent() {
		return tt9 != null && tt9.isTextEditingActive() && hasLettersOnAllKeys();
	}


	protected boolean shouldHide() {
		if (tt9 == null || tt9.isInputModeNumeric()) {
			return false;
		}

		final boolean isKeypadLong = tt9.getSettings().isNumpadShapeLongSpace();
		final boolean isKeyLong = this instanceof SoftKeyNumber0Long;

		if (hasLettersOnAllKeys() || isFnPanelOn()) {
			return isKeyLong;
		}

		return isKeypadLong != isKeyLong;
	}


	/**
	 * The space key can be very wide, so the default threshold, which depends on the key width, would
	 * require too much finger movement before the text cursor starts moving. Use the smaller of
	 * the width-based and the height-based thresholds.
	 */
	@Override
	protected float getSwipeXThreshold() {
		return Math.min(super.getSwipeXThreshold(), getSwipeYThreshold());
	}


	@Override
	protected int getNumber(int keyId) {
		return 0;
	}


	/**
	 * When the key is displayed as a space bar, pressing it types a space without displaying the
	 * list of whitespace and special characters, and holding it displays that list.
	 */
	private boolean isSimpleSpaceKey() {
		return
			tt9 != null
			&& tt9.getSettings().getSimpleSpaceKey()
			&& !isFnPanelOn()
			&& !tt9.isInputModeNumeric()
			&& !tt9.isTextEditingActive()
			&& !tt9.isVoiceInputActive()
			&& !InputModeKind.isRecomposing(tt9.getInputMode())
			&& !hasLettersOnAllKeys()
			&& getHoldCommand() == null;
	}


	@Override
	protected boolean handleRelease() {
		if (!isSimpleSpaceKey() || !notSwiped()) {
			return super.handleRelease();
		}

		return validateTT9Handler() && spaceCommand.run(tt9);
	}


	/**
	 * Holding the space bar is the same as pressing the 0-key on a hardware keypad. It shows all
	 * whitespace and special characters, with the space preselected.
	 */
	@Override
	protected void handleHold() {
		if (!isSimpleSpaceKey()) {
			super.handleHold();
			return;
		}

		preventRepeat();

		final int keyCode = Key.numberToCode(tt9.getSettings(), 0);
		if (keyCode < 0) {
			return;
		}

		vibrate(Vibration.getHoldVibration());
		tt9.onKeyDown(keyCode, new KeyEvent(KeyEvent.ACTION_DOWN, keyCode));
		tt9.onKeyUp(keyCode, new KeyEvent(KeyEvent.ACTION_UP, keyCode));
	}


	@Override
	public boolean isHoldEnabled() {
		return tt9 != null && (tt9.getSettings().getHoldToType() || isSimpleSpaceKey()) && !shouldHide();
	}


	@Override
	protected String getHoldText() {
		// The hold text causes screen readers to announce too much unnecessary information, so we
		// disable it in accessibility mode. The user can still use the hold functionality, but it
		// won't be announced.
		if (!allowTwoStepInAccessibility()) {
			return null;
		}

		if (isFnPanelOn()) {
			return super.getHoldText();
		}

		if (tt9 == null || !isHoldEnabled()) {
			return null;
		}

		if (isSimpleSpaceKey()) {
			return getSpaceKeyHoldText();
		}

		if (tt9.isTextEditingActive()) {
			return "";
		} else if (tt9.isInputTypeSigned()) {
			return "+/-";
		} else if (tt9.isInputTypePhone()) {
			return "+";
		} else if (tt9.isInputTypeNumeric() && !tt9.isInputTypeSigned() && !tt9.isInputTypePhone()) {
			return "";
		} else if (tt9.isInputModeNumeric() || hasLettersOnAllKeys()) {
			return CHARS_NUMERIC_MODE;
		}

		return super.getLocalizedNumber(getNumber());
	}


	/**
	 * Preview the first few printable characters available when holding the space bar.
	 */
	private String getSpaceKeyHoldText() {
		StringBuilder sb = new StringBuilder();
		for (String c : tt9.getSettings().getOrderedKeyChars(tt9.getLanguage(), 0)) {
			if (sb.length() >= SPACE_KEY_HOLD_TEXT_MAX_CHARS) {
				break;
			}
			if (!c.codePoints().allMatch(cp -> Character.isWhitespace(cp) || Character.isSpaceChar(cp)) && !Characters.PLACEHOLDER.equals(c)) {
				sb.append(c);
			}
		}

		return sb.toString();
	}


	@Override
	protected String getTitle() {
		if (isFnPanelOn()) {
			return super.getTitle();
		}

		if (tt9 == null || tt9.isInputModeNumeric()) {
			return "0";
		}

		if (hasLettersOnAllKeys() && tt9.getLanguage() != null) {
			return TextTools.removeNonLettersFromListAndJoin(tt9.getLanguage().getKeyCharacters(0));
		}

		return Characters.SPACE;
	}


	@Override
	protected float getTitleScale() {
		if (isBopomofo()) {
			return super.getTitleScale() * TITLE_SCALE_BOPOMOFO;
		}

		// scale up the space character, because it is too small
		if (Characters.SPACE.equals(getTitle())) {
			return 1.3f * Math.min(1, getTT9Height()) * getScreenSizeScale();
		}

		return super.getTitleScale();
	}


	private void setVisibility() {
		getOverlayWrapper();
		if (shouldHide()) {
			overlay.setVisibility(GONE);
		} else if (shouldBeTransparent()) {
			overlay.setVisibility(INVISIBLE);
		} else {
			overlay.setVisibility(VISIBLE);
		}
	}


	private void setEnabled() {
		setEnabled(tt9 != null && !(tt9.isTextEditingActive() && hasLettersOnAllKeys()));
	}

	@Override
	public void render() {
		setVisibility();
		setEnabled();
		super.render();
	}
}
