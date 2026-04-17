package lang24.data.type;

/**
 * A data type with atom values.
 * 
 * @author bostjan.slivnik@fri.uni-lj.si
 */
public abstract class SemAtomType extends SemSimpleType implements WatType {

	/** Constructs a new data type with atom values. */
	public SemAtomType() {
	}

	@Override
	public Type watType() {
		return Type.I64;
	}
}