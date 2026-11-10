package io.github.sspanak.tt9.preferences.screens.textKeys;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import io.github.sspanak.tt9.R;
import io.github.sspanak.tt9.preferences.PreferencesActivity;
import io.github.sspanak.tt9.preferences.settings.SettingsStore;

public class TextKeyLeftScreen extends AbstractTextKeyScreen {
	public static final String NAME = "TextKeyLeft";

	public TextKeyLeftScreen() { super(); }
	public TextKeyLeftScreen(@Nullable PreferencesActivity activity) { super(activity); }

	@Override public String getName() { return NAME; }
	@Override protected int getTitle() { return R.string.pref_text_key_left; }
	@NonNull @Override protected String getTextKeyId() { return SettingsStore.TEXT_KEY_LEFT; }
}
