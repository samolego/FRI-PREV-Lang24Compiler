package lang24.common.report;

/**
 * Implemented by classes describing parts of the source file.
 * 
 * @author bostjan.slivnik@fri.uni-lj.si
 */
public interface Locatable {

	/**
	 * Returns the location of a part of the source file.
	 * 
	 * @return The location of a part of the source file.
	 */
	public Location location();

	/**
	 * Modifies the existing location.
	 * 
	 * @param location The new location.
	 */
	public default void relocate(final Locatable location) {
		throw new Report.InternalError();
	}

	/**
	 * Gets text of this node, as it's written in the source file.
	 * @return The text of this node.
	 * @author samolego
	 */
	default String getText() {
		return location().getText();
	}

}