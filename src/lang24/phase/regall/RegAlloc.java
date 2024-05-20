package lang24.phase.regall;

import lang24.data.asm.AsmInstr;
import lang24.data.asm.AsmOPER;
import lang24.data.asm.Code;
import lang24.data.mem.MemTemp;
import lang24.data.type.SemPointerType;
import lang24.phase.asmgen.Imc2AsmVisitor;
import lang24.phase.livean.LiveAnAlyser;
import lang24.phase.memory.MemEvaluator;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.Stack;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.Vector;

import static lang24.phase.asmgen.Imc2AsmVisitor.Vector_of;

public class RegAlloc {
    public static final int MAX_REGISTERS = 3;
    private final Code code;
    private final Map<MemTemp, Integer> currentColors;

    public RegAlloc(Code code) {
        this.code = code;
        this.currentColors = new HashMap<>();
    }

    /**
     * Allocate registers for the code.
     * @param tempToReg Mapping of temporary variables to registers.
     */
    public void allocate(Map<MemTemp, Integer> tempToReg) {
        var graph = buildGraph();
        System.out.println("Interference graph: " + graph);

        var spilled = colorGraph(graph);
        if (!spilled.isEmpty()) {
            // Oh no, we have to spill some variables
            System.out.println("Spilling variables: " + spilled);
            //this.findLongestTimeSpanVar().ifPresent(this::generateSpillCode);
            spilled.forEach(this::generateSpillCode);
            System.out.println("Done spilling!");
            var liveAnAlyser = new LiveAnAlyser(code.instrs);
            this.currentColors.clear();
            System.out.println("Reanalyzing liveness!");
			liveAnAlyser.analyzeAll();
            this.allocate(tempToReg);
            return;
        }

        // Print the results
        System.out.println("Register allocation for code: " + this.code.entryLabel.name);
        System.out.println("Total used registers: " + this.currentColors.values().stream().distinct().count());
        System.out.println("Registers used: " + this.currentColors);

        // Save the results
        tempToReg.putAll(this.currentColors);
    }

    private void generateSpillCode(MemTemp memTemp) {
        // automatic vars + old FP, return address + other temporaries
        long offset = this.code.frame.blockSize + MemEvaluator.getSizeInBytes(SemPointerType.type) * 2 + this.code.tempSize * 8;
        var storeInstr = new AsmOPER("STOU `s0,%s,%d".formatted(Imc2AsmVisitor.FP, offset), Vector_of(memTemp), null, null);

        ++this.code.tempSize;

        boolean added = false;
        // Find the instruction that uses the spilled variable
        Vector<AsmInstr> instrs = this.code.instrs;
        int i = 0;
        while (i < instrs.size()) {
            System.out.println("Checking instruction: " + instrs.get(i) + "(" + i + " of " + instrs.size());
            var instr = instrs.get(i);

            if (instr.uses().contains(memTemp) && added) {
                // Load variable from memory
                var tmp = new MemTemp();
                var popInstr = new AsmOPER("LDOU `d0, %s,%d".formatted(Imc2AsmVisitor.FP, offset), null, Vector_of(tmp), null);
                instrs.insertElementAt(popInstr, i);

                // Replace instruction with new one
                var uses = instr.uses();
                int ix = uses.indexOf(memTemp);
                do {
                    uses.set(ix, tmp);
                    ix = uses.indexOf(memTemp);
                } while (ix != -1);

                var defs = instr.defs();
                ix = defs.indexOf(memTemp);
                boolean needsStore = false;
                while (ix != -1) {
                    defs.set(ix, tmp);
                    ix = defs.indexOf(memTemp);
                    needsStore = true;
                }

                var newInstr = new AsmOPER(((AsmOPER) instr).instr(), uses, defs, instr.jumps());
                instrs.set(i + 1, newInstr);

                if (needsStore) {
                    ++i;
                    var storeIns = new AsmOPER("STOU `s0,%s,%d".formatted(Imc2AsmVisitor.FP, offset), Vector_of(tmp), null, null);
                    instrs.add(i + 1, storeIns);
                }

                ++i;
            } else if (instr.defs().contains(memTemp)) {
                // Save variable to memory
                instrs.add(i + 1, storeInstr);
                added = true;
                ++i;
            }
            ++i;
        }
        System.out.println("Done");
    }

    public Map<MemTemp, Set<MemTemp>> buildGraph() {
        var graph = new TreeMap<MemTemp, Set<MemTemp>>();
        // Graph from instructions
        for (var instr : this.code.instrs) {
            for (var inTemp : instr.in()) {
                graph.putIfAbsent(inTemp, new TreeSet<>());
                graph.get(inTemp).addAll(instr.in());

                // Remove self
                graph.get(inTemp).remove(inTemp);
            }
        }
        
        return graph;
    }

    public HashSet<MemTemp> colorGraph(Map<MemTemp, Set<MemTemp>> graph) {
        // Clone the graph
        final var originalGraph = new TreeMap<>(graph);
        originalGraph.replaceAll((n, v) -> new TreeSet<>(originalGraph.get(n)));

        // Color the graph
        // Take out the nodes, sorted by number of edges, descending
        var nodes = sortGraph(graph);
        System.out.println("Nodes: " + nodes);

        var stack = new Stack<MemTemp>();
        while (!nodes.isEmpty()) {
            // Get node with the least edges
            var node = nodes.getLast();
            System.out.println("Taking node: " + node);
            stack.push(node);

            // Get neighbors
            var neighbors = graph.remove(node);
            System.out.println("Neighbors: " + neighbors);

            // Remove current node from all neighbors
            for (var neigh : neighbors) {
                var includesNode = graph.get(neigh);
                if (includesNode != null) {
                    includesNode.remove(node);
                }
            }
            nodes = sortGraph(graph);

            System.out.println("Nodes: " + nodes);
            System.out.println("New graph: " + graph);
            System.out.println("Stack: " + stack);
        }

        System.out.println("-----------------------------------");
        System.out.println("Stack: " + stack);

        var spilled = new HashSet<MemTemp>();
        while (!stack.isEmpty()) {
            var node = stack.pop();
            System.out.println("Coloring node: " + node);
            // Get the colors of the neighbors
            var neighborColors = new HashSet<Integer>();
            var neighbours =  originalGraph.get(node);
            System.out.println("    Neighbours: " + neighbours);

            if (neighbours != null) {
                for (var neighbor : neighbours) {
                    if (this.currentColors.get(neighbor) != null) {
                        neighborColors.add(this.currentColors.get(neighbor));
                        System.out.println("        Neighbor " + neighbor + " color:" + this.currentColors.get(neighbor));
                    }
                }
            }

            // Find the first available color
            int color = 0;
            while (neighborColors.contains(color)) {
                color += 1;
            }
            System.out.println("    Node color: " + color);

            if (color >= MAX_REGISTERS) {
                // Cannot color graph
                spilled.add(node);
                System.out.println("!!!!  --->  Spilled node: " + node);
            } else {
                // Assign the color
                this.currentColors.put(node, color);
            }
        }

        return spilled;
    }

    private Optional<MemTemp> findLongestTimeSpanVar() {
        var defs = new HashMap<MemTemp, Integer>();
        var lastUses = new HashMap<MemTemp, Integer>();

        for (int i = 0; i < this.code.instrs.size(); i++) {
            var instr = this.code.instrs.get(i);
            int finalI = i;
            instr.defs().forEach(d -> defs.put(d, finalI));
            instr.uses().forEach(u -> lastUses.put(u, finalI));
        }

        MemTemp spill = null;
        int longest = Integer.MIN_VALUE;

        for (var defStart : defs.entrySet()) {
            int diff = lastUses.getOrDefault(defStart.getKey(), 0) - defStart.getValue();

            if (diff > longest) {
                longest = diff;
                spill = defStart.getKey();
            }
        }
        assert longest > 2 : "Cannot use register from one to other instruction??";

        return  Optional.ofNullable(spill);
    }


    /**
     * Returns the nodes of the graph sorted by number of edges, descending.
     * @param graph The graph to sort.
     * @return List of nodes sorted by number of edges.
     */
    private List<MemTemp> sortGraph(Map<MemTemp, Set<MemTemp>> graph) {
        // Sort the graph by number of edges, descending
        return graph.keySet()
                .stream()
                .sorted((a, b) -> graph.get(b).size() - graph.get(a).size())
                .toList();
    }
}
