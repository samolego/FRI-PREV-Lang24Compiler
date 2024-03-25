package lang24.data.ast.tree.expr;

import lang24.common.report.*;
import lang24.data.ast.Nameable;
import lang24.data.ast.visitor.*;

/**
 * Variable use.
 * 
 * @author bostjan.slivnik@fri.uni-lj.si
 */
public class AstNameExpr extends AstExpr implements Nameable {

	/** The name. */
	public String name;

	/**
	 * Constructs a function call.
	 * 
	 * @param location The location.
	 * @param name     The name.
	 */
	public AstNameExpr(Locatable location, String name) {
		super(location);
		this.name = name;
	}

	@Override
	public <Result, Argument> Result accept(AstVisitor<Result, Argument> visitor, Argument arg) {
		return visitor.visit(this, arg);
	}

	@Override
	public String name() {
		return name;
	}
}