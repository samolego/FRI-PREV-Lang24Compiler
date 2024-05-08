package lang24.data.type;

import java.util.*;

/**
 * A record type.
 * 
 * @author bostjan.slivnik@fri.uni-lj.si
 */
public abstract class SemRecordType extends SemValueType {

	/** Types of components. */
	public final SemTypes<SemType> cmpTypes;

	/**
	 * Constructs a new record type.
	 * 
	 * @param compTypes Types of components.
	 */
	public SemRecordType(final List<SemType> compTypes) {
		this.cmpTypes = compTypes == null ? null : new SemTypes<SemType>(compTypes);
	}


	@Override
	public String getKind() {
		var sb = new StringBuilder();
		char lparen = this instanceof SemStructType ? '(' : '{';
		char rparen = this instanceof SemStructType ? ')' : '}';
		sb.append(lparen);
		for (var cmpType : cmpTypes) {
			sb.append(cmpType.getKind());
			sb.append(", ");
		}
		sb.append(rparen);

		return sb.toString();
	}
}