package lang24.data.mem;

/**
 * A temporary variable.
 *
 * @author bostjan.slivnik@fri.uni-lj.si
 */
public class MemTemp implements Comparable<MemTemp> {

	/** The name of a temporary variable. */
	public final long temp;

	/** Counter of temporary variables. */
	private static long count = 0;

	/** Creates a new temporary variable. */
	public MemTemp() {
		this.temp = count;
		count++;
	}

	@Override
	public String toString() {
		return "T" + temp;
	}

	@Override
	public int compareTo(MemTemp memTemp) {
		return Long.compare(this.temp, memTemp.temp);
	}
}