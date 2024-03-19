package lang24.data.ast.tree.defn;

import lang24.common.report.*;
import lang24.data.ast.tree.*;
import lang24.data.ast.tree.type.*;

/**
 * A definition of a name.
 * 
 * @author bostjan.slivnik@fri.uni-lj.si
 */
public abstract class AstDefn extends AstNode implements Comparable<AstDefn> {

	/** The defined name. */
	public final String name;

	/** The type of the defined name. */
	public final AstType type;

	/**
	 * Constructs a definition of a name.
	 * 
	 * @param location The location.
	 * @param name     The defined name.
	 * @param type     The type of the defined name.
	 */
	public AstDefn(final Locatable location, final String name, final AstType type) {
		super(location);
		this.name = name;
		this.type = type;
	}

	@Override
	public int compareTo(AstDefn astDefn) {
		if (this == astDefn) {
			return 0;
		}

		if (this instanceof AstTypDefn && astDefn instanceof AstVarDefn ||
				this instanceof AstTypDefn && astDefn instanceof AstFunDefn ||
				this instanceof AstVarDefn && astDefn instanceof AstFunDefn) {
			return -1;
		}

		if (this instanceof AstVarDefn && astDefn instanceof AstTypDefn ||
				this instanceof AstFunDefn && astDefn instanceof AstTypDefn ||
				this instanceof AstFunDefn && astDefn instanceof AstVarDefn) {
			return 1;
		}

		return this.name.compareTo(astDefn.name);
	}
}