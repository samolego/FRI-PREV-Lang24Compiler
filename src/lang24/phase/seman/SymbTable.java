package lang24.phase.seman;

import lang24.common.report.Report;
import lang24.data.ast.tree.defn.AstDefn;

import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Objects;

/**
 * A symbol table.
 * 
 * @author bostjan.slivnik@fri.uni-lj.si
 */
public class SymbTable implements Iterable<String> {

	@Override
	public Iterator<String> iterator() {
		return allDefnsOfAllNames.keySet().iterator();
	}

	/**
	 * A symbol table record denoting a definition of a name within a certain scope.
	 */
	private record ScopedDefn(int depth, AstDefn defn) {
		/**
		 * Constructs a new record denoting a definition of a name within a certain
		 * scope.
		 *
		 * @param depth The depth of the scope the definition belongs to.
		 * @param defn  The definition.
		 */
		private ScopedDefn {
		}

		@Override
		public boolean equals(Object obj) {
			if (obj == this) {
                return true;
            }

			if ((obj instanceof ScopedDefn that)) {
				return this.depth == that.depth &&
						Objects.equals(this.defn, that.defn);
            }
			return false;
		}

		@Override
		public String toString() {
			return "ScopedDefn[" +
					"depth=" + depth + ", " +
					"defn=" + defn + ']';
		}


	}

	/**
	 * A mapping of names into lists of records denoting definitions at different
	 * scopes. At each moment during the lifetime of a symbol table, the definition
	 * list corresponding to a particular name contains all definitions that name
	 * within currently active scopes: the definition at the inner most scope is the
	 * first in the list and is visible, the other definitions are hidden.
	 */
	private final HashMap<String, LinkedList<ScopedDefn>> allDefnsOfAllNames;

	/**
	 * The list of scopes. Each scope is represented by a list of names defined
	 * within it.
	 */
	private final LinkedList<LinkedList<String>> scopes;

	/** The depth of the currently active scope. */
	private int currDepth;

	/** Whether the symbol table can no longer be modified or not. */
	private boolean lock;

	/**
	 * Constructs a new symbol table.
	 */
	public SymbTable() {
		allDefnsOfAllNames = new HashMap<String, LinkedList<ScopedDefn>>();
		scopes = new LinkedList<LinkedList<String>>();
		currDepth = 0;
		lock = false;
		pushScope();
	}

	/**
	 * Returns the depth of the currently active scope.
	 * 
	 * @return The depth of the currently active scope.
	 */
	public int currDepth() {
		return currDepth;
	}

	/**
	 * Inserts a new definition of a name within the currently active scope or
	 * throws an exception if this name has already been defined within this scope.
	 * Once the symbol table is locked, any attempt to insert further definitions
	 * results in an internal error.
	 * 
	 * @param name The name.
	 * @param defn The definition.
	 * @throws CannotInsNameException Thrown if this name has already been defined
	 *                                within the currently active scope.
	 */
	public void ins(String name, AstDefn defn) throws CannotInsNameException {
		if (lock) {
            throw new Report.InternalError();
        }

        LinkedList<ScopedDefn> allDefnsOfName = allDefnsOfAllNames.computeIfAbsent(name, k -> new LinkedList<ScopedDefn>());

        if (!allDefnsOfName.isEmpty()) {
			ScopedDefn defnOfName = allDefnsOfName.getFirst();

			if (defnOfName.depth == currDepth) {
                throw new CannotInsNameException();
            }
		}

		allDefnsOfName.addFirst(new ScopedDefn(currDepth, defn));
		scopes.getFirst().addFirst(name);
	}

	/**
	 * Returns the currently visible definition of the specified name. If no
	 * definition of the name exists within these scopes, an exception is thrown.
	 * 
	 * @param name The name.
	 * @return The definition.
	 * @throws CannotFndNameException Thrown if the name is not defined within the
	 *                                currently active scope or any scope enclosing
	 *                                it.
	 */
	public AstDefn fnd(String name) throws CannotFndNameException {
		LinkedList<ScopedDefn> allDefnsOfName = allDefnsOfAllNames.get(name);

		if (allDefnsOfName == null) {
            throw new CannotFndNameException();
        }

		if (allDefnsOfName.isEmpty()) {
            throw new CannotFndNameException();
        }

		return allDefnsOfName.getFirst().defn;
	}

	/** Used for selecting the range of scopes. */
	public enum XScopeSelector {
		/** All live scopes. */
		ALL,
		/** Currently active scope. */
		ACT,
	}

	/**
	 * Constructs a new scope within the currently active scope. The newly
	 * constructed scope becomes the currently active scope.
	 */
	public void pushScope() {
		if (lock) {
            throw new Report.InternalError();
        }

		currDepth++;
		scopes.addFirst(new LinkedList<String>());
	}

	/**
	 * Destroys the currently active scope by removing all definitions belonging to
	 * it from the symbol table. Makes the enclosing scope the currently active
	 * scope.
	 */
	public void popScope() {
		if (lock) {
            throw new Report.InternalError();
        }

		if (currDepth == 0) {
            throw new Report.InternalError();
        }

		for (String name : scopes.getFirst()) {
			allDefnsOfAllNames.get(name).removeFirst();
		}

		scopes.removeFirst();
		currDepth--;
	}

	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder();
		sb.append("LookupTable:\n");

		for (String name : allDefnsOfAllNames.keySet()) {
			sb.append(name).append(":\n");
			for (ScopedDefn defn : allDefnsOfAllNames.get(name)) {
				sb.append("  ").append(defn.defn).append(" at depth ").append(defn.depth).append("\n");
			}
		}

		return sb.toString();
	}

	/**
	 * Prevents further modifications of this symbol table.
	 */
	public void lock() {
		lock = true;
	}

	/**
	 * An exception thrown when the name cannot be inserted into a symbol table.
	 */
	public static class CannotInsNameException extends Exception {

		/**
		 * Constructs a new exception.
		 */
		private CannotInsNameException() {
		}

	}

	/**
	 * An exception thrown when the name cannot be found in the symbol table.
	 */
	public static class CannotFndNameException extends Exception {

		/**
		 * Constructs a new exception.
		 */
		private CannotFndNameException() {
		}

	}

}