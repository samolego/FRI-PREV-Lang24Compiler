package lang24.data.type;

import lang24.data.type.visitor.SemVisitor;

import java.util.List;

/**
 * A struct type.
 * 
 * @author bostjan.slivnik@fri.uni-lj.si
 */
public class SemStructType extends SemRecordType {

	/**
	 * Constructs a new struct type.
	 * 
	 * @param compTypes Types of components.
	 */
	public SemStructType(final List<SemType> compTypes) {
		super(compTypes);
	}

	@Override
	public String toString() {
		final StringBuilder str = new StringBuilder();
		str.append("struct(");

		if (cmpTypes != null) {
            str.append(cmpTypes);
        }
		str.append(")");

		return str.toString();
	}



	@Override
	public <Result, Argument> Result accept(SemVisitor<Result, Argument> visitor, Argument arg) {
		return visitor.visit(this, arg);
	}

}