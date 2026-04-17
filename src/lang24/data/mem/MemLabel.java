package lang24.data.mem;

/**
 * A label.
 *
 * @author bostjan.slivnik@fri.uni-lj.si
 * @param name  The name of a label.
 */
public record MemLabel(String name) implements Comparable<MemLabel> {

	/** Counter of anonymous labels. */
	private static long count = 0;

	/** Creates a new anonymous label. */
	public MemLabel() {
		this("L" + count);
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
	public boolean equals(Object obj) {
		return obj instanceof MemLabel(String othrName) && name.equals(othrName);
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