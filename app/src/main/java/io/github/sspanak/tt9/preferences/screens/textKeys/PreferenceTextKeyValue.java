package io.github.sspanak.tt9.preferences.screens.textKeys;

import android.content.Context;
import android.util.AttributeSet;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import io.github.sspanak.tt9.preferences.items.TextInputPreference;

/**
 * A text field that saves its value under its own preference key, as soon as it changes. Used for
 * the texts and labels of the customizable "!" and "?" keys.
 */
public class PreferenceTextKeyValue extends TextInputPreference {
	public PreferenceTextKeyValue(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr, int defStyleRes) { super(context, attrs, defStyleAttr, defStyleRes); }
	public PreferenceTextKeyValue(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr) { super(context, attrs, defStyleAttr); }
	public PreferenceTextKeyValue(@NonNull Context context, @Nullable AttributeSet attrs) { super(context, attrs); }
	public PreferenceTextKeyValue(@NonNull Context context) { super(context); }


	@Override
	public void onAttached() {
		super.onAttached();
		reload();
	}


	/**
	 * Reloads the value from the storage, for example, after resetting it.
	 */
	public void reload() {
		final String saved = getPersistedString("");
		setText(saved != null ? saved : "");
	}


	@Override
	protected void onTextChange() {
		persistString(getText().toString());
	}
}
