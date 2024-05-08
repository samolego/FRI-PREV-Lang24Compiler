package lang24.data.asm;

import lang24.data.mem.MemLabel;
import lang24.data.mem.MemTemp;

import java.util.HashMap;

/**
 * An assembly label.
 */
public class AsmLABEL extends AsmOPER {

	/** The label. */
	public final MemLabel label;

	public AsmLABEL(MemLabel label) {
		super("", null, null, null);
		this.label = label;
	}

	@Override
	public String toString() {
		return label.name;
	}

	@Override
	public String toString(HashMap<MemTemp, Integer> regs) {
		return label.name;
	}

}
