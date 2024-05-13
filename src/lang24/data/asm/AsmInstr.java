package lang24.data.asm;

import lang24.data.mem.MemLabel;
import lang24.data.mem.MemTemp;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.Vector;

/**
 * An assembly instruction (operation or label).
 */
public abstract class AsmInstr {

	/**
	 * A list of temporaries used by this instruction.
	 * 
	 * @return The list of temporaries used by this instruction.
	 */
	public abstract Vector<MemTemp> uses();

	/**
	 * Returns the list of temporaries defined by this instruction.
	 * 
	 * @return The list of temporaries defined by this instruction.
	 */
	public abstract Vector<MemTemp> defs();

	/**
	 * Returns the list of labels this instruction can jump to.
	 * 
	 * @returnThe list of labels this instruction can jump to.
	 */
	public abstract Vector<MemLabel> jumps();

	/**
	 * Returns a string representing this instruction with temporaries.
	 * 
	 * @return A string representing this instruction with temporaries.
	 */
	public abstract String toString();

	/**
	 * Returns the set of temporaries that are live in the control flow graph edges
	 * leading to this instruction.
	 * 
	 * @return The set of temporaries that are live in the control flow graph edges
	 *         leading to this instruction.
	 */
	public abstract HashSet<MemTemp> in();

	/**
	 * Returns the set of temporaries that are live in the control flow graph edges
	 * leading from this instruction.
	 * 
	 * @return The set of temporaries that are live in the control flow graph edges
	 *         leading from this instruction.
	 */
	public abstract HashSet<MemTemp> out();

	/**
	 * Adds a set of temporaries to the set of temporaries that are live in the
	 * control flow graph edges leading to this instruction.
	 * 
	 * @param in A set of temporaries to be added.
	 */
	public abstract void addInTemps(Set<MemTemp> in);

	/**
	 * Adds a set of temporaries to the set of temporaries that are live in the
	 * control flow graph edges leading from this instruction.
	 * 
	 * @param out A set of temporaries to be added.
	 */
	public abstract void addOutTemp(Set<MemTemp> out);

	/**
	 * Returns a string representing this instruction with registers.
	 * 
	 * @param regs A mapping of temporaries to registers.
	 * @return A a string representing this instruction with registers.
	 */
	public abstract String toString(Map<MemTemp, Integer> regs);

}
