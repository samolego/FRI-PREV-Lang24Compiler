package lang24.phase.seman;

import lang24.data.ast.attribute.*;
import lang24.data.ast.tree.*;
import lang24.data.ast.tree.defn.*;
import lang24.phase.*;

/**
 * Semantic analysis phase.
 * 
 * @author bostjan.slivnik@fri.uni-lj.si
 */
public class SemAn extends Phase {

	/**
	 * Mapping of names to definitions.
	 */
	public static final Attribute<AstNode, AstDefn> definedAt = new Attribute<AstNode, AstDefn>();

	/**
	 * Phase construction.
	 */
	public SemAn() {
		super("seman");
	}

}