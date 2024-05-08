package lang24.data.type;

import lang24.data.type.visitor.SemVisitor;

import java.util.HashSet;

/**
 * A type.
 * 
 * @author bostjan.slivnik@fri.uni-lj.si
 */
public abstract class SemType {

	/** The number of nodes constructed so far. */
	private static int numNodes = 0;

	/** The unique id of this node. */
	public final int id;

	/** Constructs a new type. */
	public SemType() {
		id = numNodes++;
	}

	/**
	 * Returns the actual type, i.e., not the renamed one.
	 * 
	 * @return The actual type, i.e., not the renamed one.
	 */
	public SemType actualType() {
		return this;
	}

	/**
	 * Returns string representation of the type.
	 * @return String representation of the type.
	 */
	public abstract String getKind();

	/**
	 * Returns the actual type, i.e., not the renamed one.
	 * 
	 * @param types The types encountered during the evaluation of the actual type.
	 * @return The actual type, i.e., not the renamed one.
	 */
	public SemType actualType(final HashSet<SemNameType> types) {
		return this;
	}

	/**
	 * The acceptor method.
	 * 
	 * @param <Result>   The result type.
	 * @param <Argument> The argument type.
	 * @param visitor    The visitor accepted by this acceptor.
	 * @param arg        The argument.
	 * @return The result.
	 */
	public abstract <Result, Argument> Result accept(SemVisitor<Result, Argument> visitor, Argument arg);

}