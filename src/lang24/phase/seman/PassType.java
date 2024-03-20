package lang24.phase.seman;

public enum PassType {
    /**
     * For putting names in the symbol table.
     */
    FIRST_PASS,

    /**
     * For connecting each node of an abstract syntax tree where a name is used with the node where it is defined.
     */
    SECOND_PASS;
}
