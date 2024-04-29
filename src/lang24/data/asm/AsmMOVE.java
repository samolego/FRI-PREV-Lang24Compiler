package lang24.data.asm;

import lang24.common.report.Report;
import lang24.data.mem.MemTemp;

import java.util.Vector;

/**
 * An assembly move.
 */
public class AsmMOVE extends AsmOPER {

	public AsmMOVE(String instr, Vector<MemTemp> uses, Vector<MemTemp> defs) {
		super(instr, uses, defs, null);
		if (this.uses().size() != 1 || this.defs().size() != 1)
			throw new Report.InternalError();
	}

}
