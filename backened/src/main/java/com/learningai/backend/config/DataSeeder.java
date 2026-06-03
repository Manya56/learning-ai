package com.learningai.backend.config;

import com.learningai.backend.entity.Concept;
import com.learningai.backend.entity.Topic;
import com.learningai.backend.repository.ConceptRepository;
import com.learningai.backend.repository.TopicRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
@Order(1)
public class DataSeeder implements CommandLineRunner {

    private final TopicRepository topicRepository;
    private final ConceptRepository conceptRepository;

    @Override
    public void run(String... args) {
        seedCategoryIfAbsent("DSA", "DSA content", this::seedDSA);
        seedCategoryIfAbsent("Finance", "Finance content", this::seedFinance);
        seedCategoryIfAbsent("System Design", "System Design content", this::seedSystemDesign);
        seedCategoryIfAbsent("Machine Learning", "Machine Learning content", this::seedMachineLearning);
        log.info("Seeding complete");
    }

    private void seedCategoryIfAbsent(String category, String label, Runnable seeder) {
        if (topicRepository.findByCategoryOrderByOrderIndexAsc(category).isEmpty()) {
            log.info("Seeding {} ...", label);
            seeder.run();
        } else {
            log.info("{} already seeded — skipping", label);
        }
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

        private void seedFinance() {

        // ── Topic 1: Financial Statements ─────────────────────────────────
        Topic fs = save(Topic.builder()
                .name("Financial Statements")
                .category("Finance")
                .description("Income statement, balance sheet, cash flow analysis")
                .orderIndex(1)
                .build());

        saveConcepts(fs, List.of(
            concept("Income Statement", "Revenue, expenses, net income breakdown",
                    "EASY", 1, 20, "finance,income,statement"),
            concept("Balance Sheet", "Assets, liabilities, shareholders equity",
                    "EASY", 2, 20, "finance,balance,sheet"),
            concept("Cash Flow Statement", "Operating, investing, financing activities",
                    "MEDIUM", 3, 25, "finance,cashflow,statement"),
            concept("Financial Ratios", "Liquidity, profitability, leverage ratios",
                    "MEDIUM", 4, 30, "finance,ratios,analysis"),
            concept("Earnings Quality", "Accruals, one-time items, red flags",
                    "HARD", 5, 35, "finance,earnings,quality")
        ));

        // ── Topic 2: Valuation ────────────────────────────────────────────
        Topic valuation = save(Topic.builder()
                .name("Valuation")
                .category("Finance")
                .description("DCF, comparable companies, precedent transactions")
                .orderIndex(2)
                .build());

        saveConcepts(valuation, List.of(
            concept("Time Value of Money", "PV, FV, NPV, IRR concepts",
                    "EASY", 1, 20, "finance,tvm,npv"),
            concept("DCF Analysis", "Discounted cash flow valuation model",
                    "MEDIUM", 2, 40, "finance,dcf,valuation"),
            concept("Comparable Company Analysis", "EV/EBITDA, P/E multiples",
                    "MEDIUM", 3, 35, "finance,comps,multiples"),
            concept("Precedent Transactions", "M&A deal multiples and control premiums",
                    "HARD", 4, 35, "finance,ma,transactions"),
            concept("LBO Modeling", "Leveraged buyout returns analysis",
                    "HARD", 5, 50, "finance,lbo,modeling")
        ));

        // ── Topic 3: Stock Market ─────────────────────────────────────────
        Topic stock = save(Topic.builder()
                .name("Stock Market")
                .category("Finance")
                .description("Equity markets, indices, market microstructure")
                .orderIndex(3)
                .build());

        saveConcepts(stock, List.of(
            concept("Market Basics", "Exchanges, brokers, order types",
                    "EASY", 1, 15, "finance,stock,market"),
            concept("Technical Analysis", "Charts, moving averages, RSI, MACD",
                    "MEDIUM", 2, 35, "finance,technical,analysis"),
            concept("Fundamental Analysis", "Intrinsic value and margin of safety",
                    "MEDIUM", 3, 35, "finance,fundamental,analysis"),
            concept("Market Efficiency", "EMH, alpha, anomalies",
                    "MEDIUM", 4, 25, "finance,emh,efficiency"),
            concept("Behavioral Finance", "Biases, heuristics, market psychology",
                    "HARD", 5, 30, "finance,behavioral,psychology")
        ));

        // ── Topic 4: Options ──────────────────────────────────────────────
        Topic options = save(Topic.builder()
                .name("Options")
                .category("Finance")
                .description("Derivatives, pricing models, Greeks, strategies")
                .orderIndex(4)
                .build());

        saveConcepts(options, List.of(
            concept("Options Basics", "Calls, puts, strike, expiry, premium",
                    "EASY", 1, 20, "finance,options,basics"),
            concept("Option Payoff Diagrams", "Long/short call and put payoffs",
                    "EASY", 2, 20, "finance,options,payoff"),
            concept("Black-Scholes Model", "Option pricing formula and assumptions",
                    "HARD", 3, 45, "finance,blackscholes,pricing"),
            concept("The Greeks", "Delta, gamma, theta, vega, rho",
                    "HARD", 4, 40, "finance,greeks,delta,gamma"),
            concept("Options Strategies", "Spreads, straddles, iron condor",
                    "HARD", 5, 40, "finance,strategies,spreads")
        ));

        // ── Topic 5: Portfolio Management ─────────────────────────────────
        Topic portfolio = save(Topic.builder()
                .name("Portfolio Management")
                .category("Finance")
                .description("Modern portfolio theory, risk, asset allocation")
                .orderIndex(5)
                .build());

        saveConcepts(portfolio, List.of(
            concept("Risk and Return", "Expected return, variance, standard deviation",
                    "EASY", 1, 20, "finance,risk,return"),
            concept("Diversification", "Correlation, covariance, efficient frontier",
                    "MEDIUM", 2, 30, "finance,diversification,correlation"),
            concept("CAPM", "Beta, market risk premium, SML",
                    "MEDIUM", 3, 30, "finance,capm,beta"),
            concept("Factor Models", "Fama-French 3 and 5 factor models",
                    "HARD", 4, 40, "finance,factor,fama-french"),
            concept("Portfolio Optimization", "Mean-variance optimization, Sharpe ratio",
                    "HARD", 5, 45, "finance,optimization,sharpe")
        ));
    }

    private void seedSystemDesign() {

        // ── Topic 1: Scalability ──────────────────────────────────────────
        Topic scalability = save(Topic.builder()
                .name("Scalability")
                .category("System Design")
                .description("Horizontal vs vertical scaling, stateless design, CAP theorem")
                .orderIndex(1)
                .build());

        saveConcepts(scalability, List.of(
            concept("Horizontal vs Vertical Scaling", "Scale-out vs scale-up tradeoffs",
                    "EASY", 1, 20, "systemdesign,scaling,horizontal"),
            concept("CAP Theorem", "Consistency, availability, partition tolerance",
                    "MEDIUM", 2, 25, "systemdesign,cap,distributed"),
            concept("Stateless Architecture", "Designing stateless services for scale",
                    "MEDIUM", 3, 25, "systemdesign,stateless,scalability"),
            concept("Rate Limiting", "Token bucket, leaky bucket algorithms",
                    "MEDIUM", 4, 30, "systemdesign,ratelimit,throttle"),
            concept("Consistent Hashing", "Distributed key routing and node addition",
                    "HARD", 5, 40, "systemdesign,hashing,distributed")
        ));

        // ── Topic 2: Databases ────────────────────────────────────────────
        Topic databases = save(Topic.builder()
                .name("Databases")
                .category("System Design")
                .description("SQL vs NoSQL, replication, sharding, indexing")
                .orderIndex(2)
                .build());

        saveConcepts(databases, List.of(
            concept("SQL vs NoSQL", "Relational vs document, key-value, column stores",
                    "EASY", 1, 20, "systemdesign,sql,nosql"),
            concept("Indexing", "B-tree, hash indexes, composite indexes",
                    "MEDIUM", 2, 30, "systemdesign,indexing,btree"),
            concept("Database Replication", "Master-slave, multi-master, read replicas",
                    "MEDIUM", 3, 35, "systemdesign,replication,database"),
            concept("Database Sharding", "Horizontal partitioning strategies",
                    "HARD", 4, 40, "systemdesign,sharding,partition"),
            concept("ACID vs BASE", "Transaction guarantees and eventual consistency",
                    "HARD", 5, 35, "systemdesign,acid,base,consistency")
        ));

        // ── Topic 3: Caching ──────────────────────────────────────────────
        Topic caching = save(Topic.builder()
                .name("Caching")
                .category("System Design")
                .description("Cache strategies, eviction policies, distributed caches")
                .orderIndex(3)
                .build());

        saveConcepts(caching, List.of(
            concept("Cache Basics", "Cache hit, miss, TTL, warm-up",
                    "EASY", 1, 15, "systemdesign,cache,basics"),
            concept("Eviction Policies", "LRU, LFU, FIFO cache eviction",
                    "MEDIUM", 2, 25, "systemdesign,cache,lru,eviction"),
            concept("Cache Strategies", "Cache-aside, write-through, write-behind",
                    "MEDIUM", 3, 30, "systemdesign,cache,strategy"),
            concept("Distributed Caching", "Redis, Memcached, cluster mode",
                    "MEDIUM", 4, 35, "systemdesign,redis,distributed,cache"),
            concept("Cache Stampede and Thundering Herd", "Prevention with locking and jitter",
                    "HARD", 5, 35, "systemdesign,cache,stampede,thundering")
        ));

        // ── Topic 4: Load Balancing ───────────────────────────────────────
        Topic lb = save(Topic.builder()
                .name("Load Balancing")
                .category("System Design")
                .description("Traffic distribution, algorithms, health checks")
                .orderIndex(4)
                .build());

        saveConcepts(lb, List.of(
            concept("Load Balancer Basics", "L4 vs L7, reverse proxy, VIP",
                    "EASY", 1, 20, "systemdesign,loadbalancer,basics"),
            concept("Load Balancing Algorithms", "Round robin, least connections, IP hash",
                    "MEDIUM", 2, 25, "systemdesign,loadbalancer,roundrobin"),
            concept("Health Checks", "Active and passive health monitoring",
                    "MEDIUM", 3, 20, "systemdesign,healthcheck,loadbalancer"),
            concept("Global Load Balancing", "GeoDNS, anycast, CDN edge routing",
                    "HARD", 4, 35, "systemdesign,global,geodns,cdn"),
            concept("Service Mesh", "Sidecar proxy, Istio, mTLS",
                    "HARD", 5, 40, "systemdesign,servicemesh,istio,sidecar")
        ));

        // ── Topic 5: Message Queues ───────────────────────────────────────
        Topic mq = save(Topic.builder()
                .name("Message Queues")
                .category("System Design")
                .description("Async communication, Kafka, RabbitMQ, event-driven design")
                .orderIndex(5)
                .build());

        saveConcepts(mq, List.of(
            concept("Message Queue Basics", "Producer, consumer, broker, topics",
                    "EASY", 1, 20, "systemdesign,mq,basics"),
            concept("Kafka Architecture", "Partitions, offsets, consumer groups",
                    "MEDIUM", 2, 40, "systemdesign,kafka,partitions"),
            concept("At-Least-Once vs Exactly-Once", "Delivery guarantees and idempotency",
                    "MEDIUM", 3, 30, "systemdesign,delivery,idempotency"),
            concept("Dead Letter Queues", "Error handling and retry strategies",
                    "MEDIUM", 4, 25, "systemdesign,dlq,retry"),
            concept("Event-Driven Architecture", "CQRS, event sourcing, sagas",
                    "HARD", 5, 45, "systemdesign,eventdriven,cqrs,saga")
        ));
    }

    private void seedMachineLearning() {

        // ── Topic 1: Linear Regression ────────────────────────────────────
        Topic lr = save(Topic.builder()
                .name("Linear Regression")
                .category("Machine Learning")
                .description("Supervised learning, gradient descent, regularization")
                .orderIndex(1)
                .build());

        saveConcepts(lr, List.of(
            concept("Simple Linear Regression", "Slope, intercept, OLS estimation",
                    "EASY", 1, 20, "ml,regression,linear"),
            concept("Multiple Linear Regression", "Multiple features, multicollinearity",
                    "EASY", 2, 25, "ml,regression,multiple"),
            concept("Gradient Descent", "Batch, stochastic, mini-batch variants",
                    "MEDIUM", 3, 30, "ml,gradient,descent,optimization"),
            concept("Regularization", "L1 (Lasso), L2 (Ridge), ElasticNet",
                    "MEDIUM", 4, 30, "ml,regularization,lasso,ridge"),
            concept("Evaluation Metrics", "MSE, RMSE, MAE, R-squared",
                    "EASY", 5, 20, "ml,metrics,rmse,r2")
        ));

        // ── Topic 2: Neural Networks ──────────────────────────────────────
        Topic nn = save(Topic.builder()
                .name("Neural Networks")
                .category("Machine Learning")
                .description("Perceptrons, backpropagation, activation functions")
                .orderIndex(2)
                .build());

        saveConcepts(nn, List.of(
            concept("Perceptron and MLP", "Neurons, layers, forward pass",
                    "EASY", 1, 25, "ml,neural,perceptron,mlp"),
            concept("Activation Functions", "ReLU, sigmoid, tanh, softmax",
                    "EASY", 2, 20, "ml,activation,relu,sigmoid"),
            concept("Backpropagation", "Chain rule, weight update, vanishing gradient",
                    "MEDIUM", 3, 40, "ml,backprop,gradient,chainrule"),
            concept("Optimizers", "Adam, RMSProp, momentum, learning rate schedules",
                    "MEDIUM", 4, 35, "ml,optimizer,adam,rmsprop"),
            concept("Overfitting and Regularization", "Dropout, batch norm, early stopping",
                    "MEDIUM", 5, 35, "ml,overfitting,dropout,batchnorm")
        ));

        // ── Topic 3: CNN ──────────────────────────────────────────────────
        Topic cnn = save(Topic.builder()
                .name("CNN")
                .category("Machine Learning")
                .description("Convolutional networks for image recognition")
                .orderIndex(3)
                .build());

        saveConcepts(cnn, List.of(
            concept("Convolution Operation", "Filters, feature maps, stride, padding",
                    "EASY", 1, 25, "ml,cnn,convolution,filter"),
            concept("Pooling Layers", "Max pooling, average pooling, spatial reduction",
                    "EASY", 2, 20, "ml,cnn,pooling,maxpool"),
            concept("CNN Architectures", "LeNet, AlexNet, VGG, ResNet, Inception",
                    "MEDIUM", 3, 35, "ml,cnn,resnet,architecture"),
            concept("Transfer Learning", "Fine-tuning pretrained models",
                    "MEDIUM", 4, 35, "ml,transfer,pretrained,finetuning"),
            concept("Object Detection", "YOLO, SSD, Faster R-CNN overview",
                    "HARD", 5, 45, "ml,cnn,detection,yolo,rcnn")
        ));

        // ── Topic 4: NLP ──────────────────────────────────────────────────
        Topic nlp = save(Topic.builder()
                .name("NLP")
                .category("Machine Learning")
                .description("Text processing, embeddings, transformers, LLMs")
                .orderIndex(4)
                .build());

        saveConcepts(nlp, List.of(
            concept("Text Preprocessing", "Tokenization, stemming, lemmatization, TF-IDF",
                    "EASY", 1, 20, "ml,nlp,tokenization,tfidf"),
            concept("Word Embeddings", "Word2Vec, GloVe, FastText",
                    "MEDIUM", 2, 30, "ml,nlp,word2vec,embeddings"),
            concept("Recurrent Networks", "RNN, LSTM, GRU for sequence modeling",
                    "MEDIUM", 3, 40, "ml,nlp,rnn,lstm,gru"),
            concept("Attention Mechanism", "Self-attention, multi-head attention",
                    "HARD", 4, 40, "ml,nlp,attention,transformer"),
            concept("Transformers and LLMs", "BERT, GPT architecture and fine-tuning",
                    "HARD", 5, 50, "ml,nlp,transformer,bert,gpt")
        ));

        // ── Topic 5: Reinforcement Learning ──────────────────────────────
        Topic rl = save(Topic.builder()
                .name("Reinforcement Learning")
                .category("Machine Learning")
                .description("Agents, rewards, policies, Q-learning, policy gradients")
                .orderIndex(5)
                .build());

        saveConcepts(rl, List.of(
            concept("RL Basics", "Agent, environment, state, action, reward",
                    "EASY", 1, 20, "ml,rl,basics,markov"),
            concept("Markov Decision Process", "MDP formulation, Bellman equation",
                    "MEDIUM", 2, 30, "ml,rl,mdp,bellman"),
            concept("Q-Learning", "Tabular Q-table, epsilon-greedy exploration",
                    "MEDIUM", 3, 35, "ml,rl,qlearning,exploration"),
            concept("Deep Q-Network", "DQN, experience replay, target network",
                    "HARD", 4, 45, "ml,rl,dqn,deeprl"),
            concept("Policy Gradient Methods", "REINFORCE, PPO, actor-critic",
                    "HARD", 5, 50, "ml,rl,policy,ppo,actorcritic")
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
