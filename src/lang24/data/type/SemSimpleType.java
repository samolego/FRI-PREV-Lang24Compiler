package lang24.data.type;

/**
 * A data type with simple values.
 * 
 * @author bostjan.slivnik@fri.uni-lj.si
 */
public abstract class SemSimpleType extends SemValueType implements WatType {

	/** Constructs a new data type with simple values. */
	public SemSimpleType() {
	}

	@Override
	public Type watType() {
		return Type.I64;
	}
}