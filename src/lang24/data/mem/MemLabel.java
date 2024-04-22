package lang24.data.mem;

/**
 * A label.
 * 
 * @author bostjan.slivnik@fri.uni-lj.si
 */
public class MemLabel implements Comparable<MemLabel> {

	/** The name of a label. */
	public final String name;

	/** Counter of anonymous labels. */
	private static long count = 0;

	/** Creates a new anonymous label. */
	public MemLabel() {
		this.name = "L" + count;
		count++;
	}

	/**
	 * Creates a new named label.
	 * 
	 * @param name The name of a label.
	 */
	public MemLabel(String name) {
		this.name = "_" + name;
	}

	@Override
	public int hashCode() {
		return name.hashCode();
	}

	@Override
	public boolean equals(Object obj) {
		return obj instanceof MemLabel mlb && name.equals(mlb.name);
	}

	@Override
	public String toString() {
		return name;
	}

	@Override
	public int compareTo(MemLabel memLabel) {
		return name.compareTo(memLabel.name);
	}
}