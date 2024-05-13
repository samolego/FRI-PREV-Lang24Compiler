package lang24.phase.regall;

import lang24.data.asm.Code;
import lang24.data.mem.MemTemp;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Stack;

public class RegAlloc {
    public static final int MAX_REGISTERS = 8;
    private final Code code;
    private int currentColor = 0;
    private Map<MemTemp, Integer> currentColors;

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

        colorGraph(graph);

        // Print the results
        System.out.println("Register allocation for code: " + this.code.entryLabel.name);
        System.out.println("Total used registers: " + this.currentColors.values().stream().distinct().count());
        System.out.println("Registers used: " + this.currentColors);
    }

    public Map<MemTemp, Set<MemTemp>> buildGraph() {
        var graph = new HashMap<MemTemp, Set<MemTemp>>();
        // Graph from instructions
        for (var instr : this.code.instrs) {
            for (var temp : instr.in()) {
                if (!graph.containsKey(temp)) {
                    graph.put(temp, new HashSet<>());
                }
                graph.get(temp).addAll(instr.in());
                graph.get(temp).addAll(instr.out());

                // Remove self
                graph.get(temp).remove(temp);
            }

            for (var temp : instr.out()) {
                if (!graph.containsKey(temp)) {
                    graph.put(temp, new HashSet<>());
                }
                graph.get(temp).addAll(instr.in());
                graph.get(temp).addAll(instr.out());

                // Remove self
                graph.get(temp).remove(temp);
            }
        }
        
        return graph;
    }

    public void colorGraph(Map<MemTemp, Set<MemTemp>> graph) {
        // Clone the graph
        final var clonedGraph = new HashMap<>(graph);
        clonedGraph.replaceAll((n, v) -> new HashSet<>(clonedGraph.get(n)));

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

        var spills = new HashSet<MemTemp>();
        while (!stack.isEmpty()) {
            var node = stack.pop();
            System.out.println("Coloring node: " + node);
            // Get the colors of the neighbors
            var neighborColors = new HashSet<Integer>();
            var neighbours =  clonedGraph.get(node);
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
                spills.add(node);
            } else {
                // Assign the color
                this.currentColors.put(node, color);
            }
        }

        if (!spills.isEmpty()) {
            // Spill the variables
            System.err.println("Spilling variables: " + spills);
        }
    }


    /**
     * Returns the nodes of the graph sorted by number of edges, descending.
     * @param graph The graph to sort.
     * @return List of nodes sorted by number of edges.
     */
    private List<MemTemp> sortGraph(Map<MemTemp, Set<MemTemp>> graph) {
        // Sort the graph by number of edges, descending
        return graph
                .keySet()
                .stream()
                .sorted((a, b) -> graph.get(b).size() - graph.get(a).size())
                .toList();
    }
}
