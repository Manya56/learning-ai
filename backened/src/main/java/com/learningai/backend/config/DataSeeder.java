package com.learningai.backend.config;

import com.learningai.backend.entity.Concept;
import com.learningai.backend.entity.Topic;
import com.learningai.backend.repository.ConceptRepository;
import com.learningai.backend.repository.TopicRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class DataSeeder implements CommandLineRunner {

    private final TopicRepository topicRepository;
    private final ConceptRepository conceptRepository;

    @Override
    public void run(String... args) {
        if (topicRepository.count() > 0) {
            log.info("Content already seeded — skipping");
            return;
        }

        log.info("Seeding DSA content...");
        seedDSA();
        log.info("Seeding complete");
    }

    private void seedDSA() {

        // ── Topic 1: Arrays ───────────────────────────────────────────────
        Topic arrays = save(Topic.builder()
                .name("Arrays")
                .category("DSA")
                .description("Foundation of DSA — indexing, traversal, manipulation")
                .orderIndex(1)
                .build());

        saveConcepts(arrays, List.of(
            concept("Array Basics", "Indexing, traversal, insertion, deletion",
                    "EASY", 1, 20, "array,basics"),
            concept("Two Pointer Technique", "Solve problems with two moving pointers",
                    "EASY", 2, 25, "array,pointer,optimization"),
            concept("Sliding Window", "Fixed and variable size window problems",
                    "MEDIUM", 3, 30, "array,window,optimization"),
            concept("Prefix Sum", "Precompute sums for range queries",
                    "MEDIUM", 4, 25, "array,sum,range"),
            concept("Kadane's Algorithm", "Maximum subarray sum in O(n)",
                    "MEDIUM", 5, 20, "array,dp,subarray"),
            concept("Binary Search on Arrays", "Search in sorted arrays",
                    "MEDIUM", 6, 30, "array,search,binary"),
            concept("Dutch National Flag", "3-way partition algorithm",
                    "HARD", 7, 25, "array,partition,sorting")
        ));

        // ── Topic 2: Strings ──────────────────────────────────────────────
        Topic strings = save(Topic.builder()
                .name("Strings")
                .category("DSA")
                .description("String manipulation, pattern matching, parsing")
                .orderIndex(2)
                .build());

        saveConcepts(strings, List.of(
            concept("String Basics", "Immutability, charAt, substring operations",
                    "EASY", 1, 15, "string,basics"),
            concept("String Reversal and Palindrome", "Reverse and check palindromes",
                    "EASY", 2, 20, "string,palindrome,reverse"),
            concept("Anagram Detection", "Check if two strings are anagrams",
                    "EASY", 3, 20, "string,anagram,hashmap"),
            concept("Sliding Window on Strings", "Longest substring problems",
                    "MEDIUM", 4, 30, "string,window,optimization"),
            concept("KMP Algorithm", "Pattern matching in O(n+m)",
                    "HARD", 5, 40, "string,pattern,matching")
        ));

        // ── Topic 3: Linked Lists ─────────────────────────────────────────
        Topic linkedList = save(Topic.builder()
                .name("Linked Lists")
                .category("DSA")
                .description("Pointer manipulation, traversal, cycle detection")
                .orderIndex(3)
                .build());

        saveConcepts(linkedList, List.of(
            concept("Singly Linked List", "Node structure, insert, delete, traverse",
                    "EASY", 1, 25, "linkedlist,basics,pointer"),
            concept("Fast and Slow Pointer", "Detect cycle, find middle",
                    "MEDIUM", 2, 30, "linkedlist,pointer,cycle"),
            concept("Reverse Linked List", "Iterative and recursive reversal",
                    "MEDIUM", 3, 25, "linkedlist,reverse,pointer"),
            concept("Merge Two Sorted Lists", "Merge lists maintaining order",
                    "MEDIUM", 4, 25, "linkedlist,merge,sort"),
            concept("LRU Cache", "Doubly linked list + HashMap implementation",
                    "HARD", 5, 45, "linkedlist,hashmap,cache,design")
        ));

        // ── Topic 4: Stacks and Queues ────────────────────────────────────
        Topic stackQueue = save(Topic.builder()
                .name("Stacks and Queues")
                .category("DSA")
                .description("LIFO and FIFO structures and their applications")
                .orderIndex(4)
                .build());

        saveConcepts(stackQueue, List.of(
            concept("Stack Basics", "Push, pop, peek using arrays and linked lists",
                    "EASY", 1, 20, "stack,basics"),
            concept("Valid Parentheses", "Bracket matching using stack",
                    "EASY", 2, 20, "stack,parentheses,string"),
            concept("Monotonic Stack", "Next greater/smaller element problems",
                    "MEDIUM", 3, 35, "stack,monotonic,array"),
            concept("Queue Using Stacks", "Implement queue with two stacks",
                    "MEDIUM", 4, 25, "queue,stack,design"),
            concept("Sliding Window Maximum", "Deque based maximum in window",
                    "HARD", 5, 35, "queue,deque,window,array")
        ));

        // ── Topic 5: Trees ────────────────────────────────────────────────
        Topic trees = save(Topic.builder()
                .name("Binary Trees")
                .category("DSA")
                .description("Tree traversals, BST operations, tree problems")
                .orderIndex(5)
                .build());

        saveConcepts(trees, List.of(
            concept("Tree Traversals", "Inorder, preorder, postorder, BFS",
                    "EASY", 1, 30, "tree,traversal,recursion"),
            concept("Binary Search Tree", "Insert, delete, search in BST",
                    "MEDIUM", 2, 35, "tree,bst,search"),
            concept("Height and Diameter", "Calculate tree height and diameter",
                    "MEDIUM", 3, 25, "tree,height,recursion"),
            concept("Lowest Common Ancestor", "Find LCA of two nodes",
                    "MEDIUM", 4, 30, "tree,lca,recursion"),
            concept("Tree from Traversals", "Construct tree from pre+inorder",
                    "HARD", 5, 40, "tree,construction,traversal"),
            concept("Balanced BST", "AVL rotation concepts",
                    "HARD", 6, 45, "tree,avl,rotation,balanced")
        ));

        // ── Topic 6: Graphs ───────────────────────────────────────────────
        Topic graphs = save(Topic.builder()
                .name("Graphs")
                .category("DSA")
                .description("BFS, DFS, shortest path, topological sort")
                .orderIndex(6)
                .build());

        saveConcepts(graphs, List.of(
            concept("Graph Representation", "Adjacency list vs matrix",
                    "EASY", 1, 20, "graph,basics,representation"),
            concept("BFS", "Breadth first search and shortest path",
                    "MEDIUM", 2, 30, "graph,bfs,traversal"),
            concept("DFS", "Depth first search, connected components",
                    "MEDIUM", 3, 30, "graph,dfs,traversal"),
            concept("Cycle Detection", "Detect cycle in directed and undirected graphs",
                    "MEDIUM", 4, 35, "graph,cycle,detection"),
            concept("Topological Sort", "Kahn's algorithm and DFS based sort",
                    "MEDIUM", 5, 35, "graph,topological,sort,dag"),
            concept("Dijkstra's Algorithm", "Single source shortest path",
                    "HARD", 6, 45, "graph,dijkstra,shortest,path"),
            concept("Union Find", "Disjoint set for cycle detection",
                    "HARD", 7, 40, "graph,unionfind,disjoint,set")
        ));

        // ── Topic 7: Dynamic Programming ─────────────────────────────────
        Topic dp = save(Topic.builder()
                .name("Dynamic Programming")
                .category("DSA")
                .description("Memoization, tabulation, classic DP patterns")
                .orderIndex(7)
                .build());

        saveConcepts(dp, List.of(
            concept("DP Introduction", "Overlapping subproblems, optimal substructure",
                    "MEDIUM", 1, 30, "dp,basics,memoization"),
            concept("1D DP", "Fibonacci, climbing stairs, house robber",
                    "MEDIUM", 2, 35, "dp,1d,tabulation"),
            concept("0/1 Knapsack", "Classic inclusion/exclusion DP",
                    "MEDIUM", 3, 40, "dp,knapsack,2d"),
            concept("Longest Common Subsequence", "LCS and edit distance",
                    "HARD", 4, 45, "dp,lcs,string,2d"),
            concept("Longest Increasing Subsequence", "LIS with patience sort",
                    "HARD", 5, 40, "dp,lis,binary,search"),
            concept("DP on Trees", "Tree DP patterns",
                    "HARD", 6, 50, "dp,tree,recursion")
        ));

        // ── Topic 8: Recursion and Backtracking ───────────────────────────
        Topic backtracking = save(Topic.builder()
                .name("Recursion and Backtracking")
                .category("DSA")
                .description("Recursive thinking, backtracking, pruning")
                .orderIndex(8)
                .build());

        saveConcepts(backtracking, List.of(
            concept("Recursion Basics", "Base case, recursive case, call stack",
                    "EASY", 1, 25, "recursion,basics,stack"),
            concept("Subsets and Permutations", "Generate all subsets and permutations",
                    "MEDIUM", 2, 35, "recursion,backtracking,subset"),
            concept("N-Queens Problem", "Classic backtracking problem",
                    "HARD", 3, 40, "backtracking,queens,pruning"),
            concept("Word Search", "DFS + backtracking on 2D grid",
                    "HARD", 4, 35, "backtracking,dfs,grid,string")
        ));
    }

    // ─── Helpers ─────────────────────────────────────────────────────────

    private Topic save(Topic topic) {
        return topicRepository.save(topic);
    }

    private void saveConcepts(Topic topic, List<Concept> concepts) {
        concepts.forEach(c -> {
            c.setTopic(topic);
            conceptRepository.save(c);
        });
    }

    private Concept concept(String name, String description,
                             String difficulty, int order,
                             int minutes, String tags) {
        return Concept.builder()
                .name(name)
                .description(description)
                .difficultyLevel(difficulty)
                .orderIndex(order)
                .estimatedMinutes(minutes)
                .tags(tags)
                .build();
    }
}