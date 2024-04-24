package lang24.phase.imclin;

import lang24.data.imc.code.expr.ImcBINOP;
import lang24.data.imc.code.expr.ImcBINOP.Oper;
import lang24.data.imc.code.expr.ImcUNOP;
import lang24.data.imc.code.stmt.ImcCJUMP;
import lang24.data.imc.code.stmt.ImcJUMP;
import lang24.data.imc.code.stmt.ImcLABEL;
import lang24.data.imc.code.stmt.ImcStmt;
import lang24.data.mem.MemLabel;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

public class BlockPermuter {
    private final List<ImcStmt> stmts;

    public BlockPermuter(List<ImcStmt> stmts) {
        this.stmts = stmts;
    }


    /**
     * Ugly method to fix the blocks of code.
     * @return Fixed list of statements.
     */
    public LinkedList<ImcStmt> permute_bulic_tepe() {
        var newList = new LinkedList<ImcStmt>();

        // Just traverse the list and add jumps after cjumps
        for (var stmt : this.stmts) {
            newList.add(stmt);

            if (stmt instanceof ImcCJUMP cjump) {
                // Add new dummy label & jump
                var newNegLabel = new MemLabel();
                newList.add(new ImcLABEL(newNegLabel));

                var negJump = new ImcJUMP(cjump.negLabel);
                newList.add(negJump);

                // Update the cjump
                cjump.negLabel = newNegLabel;
            }
        }

        return newList;
    }


    /**
     * Permutes the blocks of code. Tries to put the jumps and labels in the right order.
     * @return Permuted list of statements.
     */
    public LinkedList<ImcStmt> permute() {
        // Create blocks of code, starting with label and ending with jump
        var blocks = this.createCodeBlocks();

        // Greedy permute the blocks
        var newStmts = new LinkedList<ImcStmt>();

        // Add all statements until first label
        MemLabel nextLabel = null;
        for (var stmt : this.stmts) {
            if (stmt instanceof ImcLABEL lbl) {
                nextLabel = lbl.label;
                break;
            }
            newStmts.add(stmt);
        }

        // We have the first label, now permute the blocks
        while (nextLabel != null) {
            var block = blocks.remove(nextLabel);
            if (block != null) {
                newStmts.addAll(block);

                // Find next label
                nextLabel = null;
                for (var stmt : block.reversed()) {
                    if (stmt instanceof ImcJUMP jmp) {
                        nextLabel = jmp.label;
                        break;
                    } else if (stmt instanceof ImcCJUMP cjmp) {
                        // Check the labels
                        if (blocks.containsKey(cjmp.negLabel)) {
                            // Great, just assign as the next label
                            nextLabel = cjmp.negLabel;
                        } else if (blocks.containsKey(cjmp.posLabel)) {
                            // Negate the condition
                            negateCondition(cjmp);
                            nextLabel = cjmp.posLabel;
                        } else {
                            // Tough luck, we have to break the chain
                            // We're forced to jump

                            // Create new label
                            var newNegLabel = new MemLabel();
                            newStmts.add(new ImcLABEL(newNegLabel));

                            // Insert jump to the new label
                            var negJump = new ImcJUMP(cjmp.negLabel);
                            newStmts.add(negJump);

                            // Update the cjump
                            cjmp.negLabel = newNegLabel;

                            // Find next label
                            nextLabel = blocks.keySet().stream().findFirst().orElse(null);

                        }

                        break;
                    }
                }
            } else {
                // Chain is broken, add next block if it exists
                nextLabel = blocks.keySet().stream().findFirst().orElse(null);
            }
        }

        // Add any leftover blocks
        for (var block : blocks.values()) {
            newStmts.addAll(block);
        }

        return newStmts;
    }

    /**
     * Negates the condition of the CJUMP statement.
     *
     * @param cjmp CJUMP statement to negate.
     */
    private void negateCondition(ImcCJUMP cjmp) {
        var tmpLabel = cjmp.posLabel;
        cjmp.posLabel = cjmp.negLabel;
        cjmp.negLabel = tmpLabel;

        if (cjmp.cond instanceof ImcBINOP binop) {
            var switched = switch (binop.oper) {
                case Oper.EQU -> Oper.NEQ;
                case Oper.NEQ -> Oper.EQU;
                case Oper.LTH -> Oper.GEQ;
                case Oper.GEQ -> Oper.LTH;
                case Oper.LEQ -> Oper.GTH;
                case Oper.GTH -> Oper.LEQ;
                case Oper.AND -> Oper.OR;
                case Oper.OR -> Oper.AND;
                default -> null;
            };

            if (switched != null) {
                cjmp.cond = new ImcBINOP(switched, binop.fstExpr, binop.sndExpr);
            }
        } else if (cjmp.cond instanceof ImcUNOP unop && unop.oper == ImcUNOP.Oper.NOT) {
            cjmp.cond = unop.subExpr;
        } else {
            // Need to wrap the condition with NOT
            cjmp.cond = new ImcUNOP(ImcUNOP.Oper.NOT, cjmp.cond);
        }
    }

    private Map<MemLabel, List<ImcStmt>> createCodeBlocks() {
        // Traverse and fill the blocks map
        boolean expectsJump = false;
        var lastBlock = new LinkedList<ImcStmt>();

        var blocks = new HashMap<MemLabel, List<ImcStmt>>();
        for (var stmt : this.stmts) {
            if (stmt instanceof ImcLABEL label) {
                if (!expectsJump) {
                    // .....J ..... L <- we are here
                    blocks.put(label.label, lastBlock);

                    expectsJump = true;
                } else {
                    // .....J ..... L ..... L <- we are here
                    // Found two labels in a row, insert dummy jump to this label
                    lastBlock.add(new ImcJUMP(label.label));

                    lastBlock = new LinkedList<>();
                    // Put new block
                    blocks.put(label.label, lastBlock);

                    expectsJump = false;
                }
            } else if (stmt instanceof ImcJUMP || stmt instanceof ImcCJUMP) {
                // .....J ..... L ..... J <- we are here
                lastBlock.add(stmt);
                lastBlock = new LinkedList<>();
                expectsJump = false;
            }

            // Also remove dangling jumps
            if (expectsJump) {
                lastBlock.add(stmt);
            }
        }

        return blocks;
    }
}
