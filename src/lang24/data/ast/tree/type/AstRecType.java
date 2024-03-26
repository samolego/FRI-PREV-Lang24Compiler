package lang24.data.ast.tree.type;

import lang24.common.report.*;
import lang24.data.ast.tree.*;
import lang24.data.ast.tree.defn.*;
import lang24.data.ast.visitor.*;
import lang24.data.type.SemType;

import java.util.HashMap;
import java.util.Map;

/**
 * A record type, i.e., either a struct or union.
 * 
 * @author bostjan.slivnik@fri.uni-lj.si
 */
public abstract class AstRecType extends AstType {

	/** The components. */
	public final AstNodes<AstCmpDefn> cmps;

	/**
	 * Maps the names of the components to their definitions.
	 */
	public final Map<String, AstCmpDefn> cmpTypes;

	/**
	 * Constructs a record type.
	 * 
	 * @param location The location.
	 * @param cmps     The components of this union.
	 */
	public AstRecType(final Locatable location, final AstNodes<AstCmpDefn> cmps) {
		super(location);
		this.cmps = cmps;
		this.cmpTypes = new HashMap<>();
	}

	/**
	 * A definition of a record component.
	 * 
	 * @author bostjan.slivnik@fri.uni-lj.si
	 */
	public static class AstCmpDefn extends AstDefn {

		/**
		 * Constructs a definition of a record component.
		 * 
		 * @param location The location.
		 * @param name     The name of this record component.
		 * @param type     The type of this record component.
		 */
		public AstCmpDefn(final Locatable location, final String name, final AstType type) {
			super(location, name, type);
		}

		@Override
		public <Result, Argument> Result accept(AstVisitor<Result, Argument> visitor, Argument arg) {
			return visitor.visit(this, arg);
		}

	}

}