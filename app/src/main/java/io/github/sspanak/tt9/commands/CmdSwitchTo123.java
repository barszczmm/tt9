package io.github.sspanak.tt9.commands;

import io.github.sspanak.tt9.R;
import io.github.sspanak.tt9.ime.modes.InputMode;

public class CmdSwitchTo123 extends CmdSwitchInputMode {
	public static final String ID = "key_switch_to_123";
	@Override public String getId() { return ID; }
	@Override public int getName() { return R.string.function_switch_to_123; }
	@Override protected int getTargetMode() { return InputMode.MODE_123; }
}
