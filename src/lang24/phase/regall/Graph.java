package lang24.phase.regall;

import lang24.common.report.Report.InternalError;
import lang24.data.asm.Code;
import lang24.data.mem.MemTemp;

import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.Stack;
import java.util.TreeMap;
import java.util.TreeSet;

public class Graph {
    private final Map<MemTemp, Set<MemTemp>> nodes;
    private final Map<MemTemp, Integer> colors;
    private final MemTemp FP;

    public Graph(Code code) {
        this(new TreeMap<>(), new TreeMap<>(), code.frame.FP);

        for (var instr : code.instrs) {
            // Ensure all nodes end up in the graph
            instr.defs().forEach(this::addNode);
            instr.uses().forEach(this::addNode);

            // Add edges
            for (var reg1 : instr.in()) {
                for (var reg2 : instr.in()) {
                    if (reg1 != reg2 && reg1 != FP && reg2 != FP) {
                        this.addNode(reg1, Set.of(reg2));
                        this.addNode(reg2, Set.of(reg1));
                    }
                }
            }
        }
    }

    private Graph(Map<MemTemp, Set<MemTemp>> nodes, Map<MemTemp, Integer> colors, MemTemp FP) {
        this.nodes = nodes;
        this.nodes.replaceAll((n, v) -> new TreeSet<>(this.nodes.get(n)));
        this.colors = colors;
        this.FP = FP;
    }


    public void addNode(MemTemp node) {
        nodes.putIfAbsent(node, new TreeSet<>());
    }

    public void addNode(MemTemp node, Set<MemTemp> edges) {
        this.addNode(node);
        this.nodes.get(node).addAll(edges);

        // Remove self
        this.nodes.get(node).remove(node);
    }

    /**
     * Get a node with edge count less than max.
     * @param max Maximum edge count.
     * @return Node with less than max edges, if it exists.
     */
    public Optional<MemTemp> getLowConnectedNode(int max) {
        return this.nodes.entrySet().stream()
                .filter(e -> e.getValue().size() < max)
                .map(Map.Entry::getKey)
                .findAny();
        /*
        for (var node : this.nodes.keySet()) {
            if (this.nodes.get(node).size() < max) {
                return Optional.of(node);
            }
        }
        return Optional.empty();*/
    }

    public void removeNode(MemTemp node) {
        var neighbours = this.nodes.remove(node);
        for (var neighbour : neighbours) {
            if (this.nodes.containsKey(neighbour)) {
                this.nodes.get(neighbour).remove(node);
            }
        }

        for (var n : this.nodes.keySet()) {
            assert !this.nodes.get(n).contains(node) : "Node not removed from all neighbours";
        }
    }

    public Set<MemTemp> colorAll(int max) {
        final var stack = this.createNodeStack(max);
        final var spilled = new TreeSet<MemTemp>();
        final var colors = new TreeMap<MemTemp, Integer>();

        while (!stack.isEmpty()) {
            var node = stack.pop();

            // Try to color the node
            int color = 0;
            var neighbours = this.nodes.get(node);
            assert neighbours != null : "Node not found in graph";

            var unavailableColors = new TreeSet<Integer>();

            for (var neighbour : neighbours) {
                if (colors.containsKey(neighbour)) {
                    unavailableColors.add(colors.get(neighbour));
                }
            }

            while (unavailableColors.contains(color)) {
                color += 1;
            }

            if (color >= max) {
                spilled.add(node);
            } else {
                colors.put(node, color);
            }

            // Save color
            colors.put(node, color);
        }

        if (spilled.isEmpty()) {
            this.colors.putAll(colors);
        }

        return spilled;
    }

    public TreeMap<MemTemp, Integer> getColors() {
        return new TreeMap<>(this.colors);
    }

    private Stack<MemTemp> createNodeStack(int max) {
        // Clone nodes
        final var copiedGraph = this.copy();

        // Fetch nodes with the less than max edges if possible
        final var stack = new Stack<MemTemp>();

        while (!copiedGraph.nodes.isEmpty()) {
            // Get node with the least edges
            var node = copiedGraph.getLowConnectedNode(max)
                    // Potential spill
                    .orElse(copiedGraph.nodes.keySet()
                            .stream()
                            .findAny()
                            .orElseThrow(InternalError::new));

            copiedGraph.removeNode(node);
            if (node != FP) {
                stack.push(node);
            }
        }

        return stack;
    }

    @Override
    public String toString() {
        final var sb = new StringBuilder("Graph{\n");
        for (var node : this.nodes.keySet()) {
            sb.append("    ").append(node).append(" -> ").append(this.nodes.get(node)).append("\n");
        }
        sb.append("}");

        return sb.toString();
    }

    public Graph copy() {
        return new Graph(new TreeMap<>(this.nodes), new TreeMap<>(this.colors), this.FP);
    }
}
