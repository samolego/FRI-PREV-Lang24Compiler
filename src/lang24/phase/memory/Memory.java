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
	public static final Attribute<AstFunDefn, MemFrame> frames = new Attribute<AstFunDefn, MemFrame>();

	/** Maps variable declarations to accesses. */
	public static final Attribute<AstVarDefn, MemAccess> varAccesses = new Attribute<AstVarDefn, MemAccess>();

	/** Maps parameter declarations to accesses. */
	public static final Attribute<AstFunDefn.AstParDefn, MemAccess> parAccesses = new Attribute<AstFunDefn.AstParDefn, MemAccess>();

	/** Maps component declarations to accesses. */
	public static final Attribute<AstRecType.AstCmpDefn, MemAccess> cmpAccesses = new Attribute<AstRecType.AstCmpDefn, MemAccess>();

	/** Maps string constants to accesses. */
	public static final Attribute<AstAtomExpr, MemAbsAccess> strings = new Attribute<AstAtomExpr, MemAbsAccess>();

	/**
	 * Phase construction.
	 */
	public Memory() {
		super("memory");
	}

}