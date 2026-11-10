package io.github.sspanak.tt9.ui.main.keys;

import android.content.Context;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewParent;
import android.widget.RelativeLayout;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import io.github.sspanak.tt9.R;
import io.github.sspanak.tt9.preferences.settings.SettingsStore;
import io.github.sspanak.tt9.util.chars.Characters;

abstract public class SoftKeyText extends BaseSwipeableKey {
	private boolean swipeTextTyped = false;

	public SoftKeyText(Context context) { super(context); }
	public SoftKeyText(Context context, AttributeSet attrs) { super(context, attrs); }
	public SoftKeyText(Context context, AttributeSet attrs, int defStyleAttr) { super(context, attrs, defStyleAttr); }


	@Override
	protected void initColors(@NonNull SettingsStore settings) {
		backgroundColor = settings.getKeyBackgroundColor();
		borderColor = settings.getKeyBorderColor();
		cornerElementColor = settings.getKeyCornerElementColor();
		rippleColor = settings.getKeyRippleColor();
		centralIconColor = textColor = settings.getKeyTextColor();
	}


	abstract protected String getKeyChar();


	/**
	 * Returns SettingsStore.TEXT_KEY_LEFT or SettingsStore.TEXT_KEY_RIGHT, depending on which one
	 * of the customizable text keys this is.
	 */
	@NonNull abstract protected String getTextKeyId();


	/**
	 * The user-defined texts are only allowed when typing regular text. In numeric or phone fields,
	 * and when the text editing palette is active, the keys have fixed functions.
	 */
	protected boolean isCustomizable() {
		return
			tt9 != null
			&& !tt9.isInputTypePhone()
			&& !tt9.isInputModeNumeric()
			&& !tt9.isTextEditingActive();
	}


	/**
	 * Returns the user-defined text for the given action, or an empty string, if there is none.
	 */
	@NonNull
	protected String getCustomText(@NonNull String action) {
		return isCustomizable() ? tt9.getSettings().getTextKeyText(getTextKeyId(), action) : "";
	}


	/**
	 * Returns the short label for the given action, or null, if there is nothing to display.
	 */
	@Nullable
	protected String getCustomLabel(@NonNull String action) {
		if (!isCustomizable()) {
			return null;
		}

		final String label = tt9.getSettings().getTextKeyLabel(getTextKeyId(), action);
		return label.isEmpty() ? null : label;
	}


	private boolean hasSwipes() {
		return isCustomizable() && !shouldHide() && tt9.getSettings().hasTextKeySwipes(getTextKeyId());
	}


	@Override
	public boolean onTouch(View v, MotionEvent event) {
		// The available swipes depend on the input field and on the Settings, so we re-check them
		// at the beginning of each gesture.
		if ((event.getAction() & MotionEvent.ACTION_MASK) == MotionEvent.ACTION_DOWN) {
			isSwipeable = hasSwipes();
		}

		return super.onTouch(v, event);
	}


	@Override
	protected boolean handlePress() {
		swipeTextTyped = false;
		return super.handlePress();
	}


	@Override
	protected void handleEndSwipeX(float position, float delta) {
		swipeTextTyped = typeCustomText(delta < 0 ? SettingsStore.TEXT_KEY_ACTION_SWIPE_LEFT : SettingsStore.TEXT_KEY_ACTION_SWIPE_RIGHT);
	}


	@Override
	protected void handleEndSwipeY(float position, float delta) {
		swipeTextTyped = typeCustomText(delta < 0 ? SettingsStore.TEXT_KEY_ACTION_SWIPE_UP : SettingsStore.TEXT_KEY_ACTION_SWIPE_DOWN);
	}


	private boolean typeCustomText(@NonNull String action) {
		final String text = getCustomText(action);
		return !text.isEmpty() && tt9.onText(text, false);
	}


	@Override protected String getTopText() { return getCustomLabel(SettingsStore.TEXT_KEY_ACTION_SWIPE_UP); }
	@Override protected String getBottomText() { return getCustomLabel(SettingsStore.TEXT_KEY_ACTION_SWIPE_DOWN); }
	@Override protected String getLeftText() { return getCustomLabel(SettingsStore.TEXT_KEY_ACTION_SWIPE_LEFT); }
	@Override protected String getRightText() { return getCustomLabel(SettingsStore.TEXT_KEY_ACTION_SWIPE_RIGHT); }


	protected boolean isTextEditingOn() {
		return tt9 != null && tt9.isTextEditingActive();
	}


	protected boolean shouldHide() {
		final boolean isLongSpaceKey = getId() == R.id.soft_key_text_201 || getId() == R.id.soft_key_text_202;
		final boolean isShapeLongSpace = tt9 != null && tt9.getSettings().isNumpadShapeLongSpace();
		final boolean isInputModeNumeric = tt9 != null && tt9.isInputModeNumeric();
		final boolean isFnPanelOn = tt9 != null && tt9.isFnPanelVisible();

		if (isInputModeNumeric || hasLettersOnAllKeys() || isFnPanelOn) {
			return isLongSpaceKey;
		}

		return isShapeLongSpace != isLongSpaceKey;
	}


	@Override
	protected boolean handleRelease() {
		if (swipeTextTyped) {
			swipeTextTyped = false;
			return true;
		}

		return tt9 != null && tt9.onText(getKeyChar(), false);
	}


	@Override
	protected String getAccessibilityText() {
		final String customText = getCustomText(SettingsStore.TEXT_KEY_ACTION_PRESS);
		return customText.isEmpty() ? super.getAccessibilityText() : customText;
	}


	@Override
	protected String getTitle() {
		final String customLabel = getCustomLabel(SettingsStore.TEXT_KEY_ACTION_PRESS);
		return customLabel != null ? customLabel : Characters.getCharReadable(getKeyChar());
	}


	@Override
	public void render() {
		isSwipeable = hasSwipes();
		final boolean isHidden = shouldHide();
		setVisibility(isHidden ? GONE : VISIBLE);

		final ViewParent parent = getParent();
		if (parent instanceof RelativeLayout) {
			((RelativeLayout) parent).setVisibility(isHidden ? RelativeLayout.GONE : RelativeLayout.VISIBLE);
		}

		super.render();
	}
}
