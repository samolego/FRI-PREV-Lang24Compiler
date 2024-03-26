package lang24.data.type;

import java.util.*;
import lang24.data.type.visitor.*;

/**
 * A union type.
 * 
 * @author bostjan.slivnik@fri.uni-lj.si
 */
public class SemUnionType extends SemRecordType {

	/**
	 * Constructs a new union type.
	 * 
	 * @param compTypes Types of components.
	 */
	public SemUnionType(final List<SemType> compTypes) {
		super(compTypes);
	}

	@Override
	public String toString() {
		final StringBuilder str = new StringBuilder();
		str.append("union(");
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