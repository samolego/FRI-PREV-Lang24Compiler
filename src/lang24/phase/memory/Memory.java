package lang24.phase.memory;

import lang24.data.ast.attribute.*;
import lang24.data.ast.tree.defn.*;
import lang24.data.ast.tree.expr.*;
import lang24.data.ast.tree.type.*;
import lang24.data.mem.*;
import lang24.phase.*;

/**
 * Memory layout phase: stack frames and variable accesses.
 * 
 * @author bostjan.slivnik@fri.uni-lj.si
 */
public class Memory extends Phase {

	/** Maps function declarations to frames. */
	public static final Attribute<AstFunDefn, MemFrame> frames = new Attribute<>();

	/** Maps variable declarations to accesses. */
	public static final Attribute<AstVarDefn, MemAccess> varAccesses = new Attribute<>();

	/** Maps parameter declarations to accesses. */
	public static final Attribute<AstFunDefn.AstParDefn, MemRelAccess> parAccesses = new Attribute<>();

	/** Maps component declarations to accesses. */
	public static final Attribute<AstRecType.AstCmpDefn, MemRelAccess> cmpAccesses = new Attribute<>();

	/** Maps string constants to accesses. */
	public static final Attribute<AstAtomExpr, MemAbsAccess> strings = new Attribute<>();

	/**
	 * Phase construction.
	 */
	public Memory() {
		super("memory");
	}

}