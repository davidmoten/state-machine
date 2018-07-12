package com.github.davidmoten.fsm.model;

import static com.github.davidmoten.fsm.Util.camelCaseToSpaced;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import com.github.davidmoten.bean.annotation.GenerateImmutable;
import com.github.davidmoten.fsm.graph.Graph;
import com.github.davidmoten.fsm.graph.GraphEdge;
import com.github.davidmoten.fsm.graph.GraphNode;
import com.github.davidmoten.fsm.graph.GraphmlWriter;
import com.github.davidmoten.fsm.graph.NodeOptions;
import com.github.davidmoten.fsm.runtime.Event;
import com.github.davidmoten.fsm.runtime.EventVoid;
import com.github.davidmoten.guavamini.Preconditions;

public final class StateMachineDefinition<T> {

    private final Class<T> cls;
    private final List<Transition<T, ? extends Event<? super T>, ? extends Event<? super T>>> transitions = new ArrayList<>();
    private final Set<State<T, ? extends Event<? super T>>> states = new HashSet<>();
    private final State<T, EventVoid> initialState;

    private StateMachineDefinition(Class<T> cls) {
        Preconditions.checkArgument(!cls.isAnnotationPresent(GenerateImmutable.class),
                "cannot base a state machine definition on a class that is annotated with @GenerateImmutable, use the generated immutable class instead");
        this.cls = cls;
        this.initialState = new State<T, EventVoid>(this, "Initial", EventVoid.class);
    }

    public static <T> StateMachineDefinition<T> create(Class<T> cls) {
        return new StateMachineDefinition<T>(cls);
    }

    public Class<T> cls() {
        return cls;
    }

    public <R extends Event<? super T>> State<T, R> createState(String name, Class<R> eventClass) {
        Preconditions.checkArgument(!eventClass.isAnnotationPresent(GenerateImmutable.class),
                "cannot base a state on an event that is annotated with @GenerateImmutable, use the generated immutable class instead");
        Preconditions.checkNotNull(name);
        if (name.equals("Initial")) {
            name = name.concat("_1");
        }
        State<T, R> state = new State<T, R>(this, name, eventClass);
        states.add(state);
        return state;
    }
    
    public StateBuilder createState(String name) {
         return new StateBuilder(name);
    }
    
    public final class StateBuilder {
        
        final String name;

        StateBuilder(String name) {
            this.name = name;
        }
        
        /**
         * Sets the event type used to transition <i>to</i> this state.
         * 
         * @param cls the type of event
         * @return the {@code State}
         */
        public <R extends Event<? super T>> State<T, R> event(Class<R> cls) {
            return createState(name, cls);
        }
    }

    public <R extends Event<? super T>, S extends Event<? super T>> StateMachineDefinition<T> addTransition(
            State<T, R> state, State<T, S> other) {
        Transition<T, R, S> transition = new Transition<T, R, S>(state, other);
        System.out.println("adding " + transition);
        for (Transition<T, ?, ?> t : transitions) {
            if (t.from() == state && t.to() == other) {
                throw new IllegalArgumentException(
                        "the transition already exists: " + state.name() + " -> " + other.name());
            }
        }
        transitions.add(transition);
        return this;
    }

    <S extends Event<? super T>> StateMachineDefinition<T> addInitialTransition(State<T, S> other) {
        Transition<T, EventVoid, S> transition = new Transition<T, EventVoid, S>(initialState, other);
        System.out.println("adding " + transition);
        transitions.add(transition);
        states.add(initialState);
        states.add(other);
        return this;
    }

    public void generateClasses(File directory, String pkg) {
        new Generator<T>(this, directory, pkg).generate();
    }

    public List<Transition<T, ? extends Event<? super T>, ? extends Event<? super T>>> transitions() {
        return transitions;
    }

    public String documentationHtml() {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        PrintWriter out = new PrintWriter(bytes);
        out.println("<html/>");
        out.println("<head>");
        out.println("<style>");
        out.println("table {border-collapse: collapse;}\n" + "table, th, td {border: 1px solid black;}");
        out.println(".transition {background-color: #ADE2A7}");
        out.println("</style>");
        out.println("</head>");
        out.println("<body>");
        // states
        out.println("<h2>States</h2>");
        Comparator<State<T, ?>> comparator = (a, b) -> a.name().compareTo(b.name());
        states.stream().sorted(comparator).forEach(state -> out.println("<p class=\"state\"><b>" + state.name()
                + "</b> [" + state.eventClass().getSimpleName() + "]</p>" + state.documentation().orElse("")));

        // events
        out.println("<h2>Events</h2>");
        states.stream().filter(state -> !state.isInitial()).map(state -> state.eventClass().getSimpleName()).distinct()
                .sorted()
                .forEach(event -> out.println("<p class=\"event\"><i>" + camelCaseToSpaced(event) + "</i></p>"));

        // transition table
        // state onEntry template

        out.println("<h2>Transitions</h2>");
        out.println("<table>");
        out.print("<tr><th/>");
        states.stream().sorted(comparator).forEach(state -> {
            out.print("<th>" + state.name() + "</th>");
        });
        out.println("</tr>");
        states.stream().sorted(comparator).forEach(state -> {
            out.print("<tr><th>" + state.name() + "</th>");
            states.stream().sorted(comparator).forEach(st -> {
                boolean hasTransition = transitions.stream()
                        .anyMatch(t -> t.from().name().equals(state.name()) && t.to().name().equals(st.name()));
                if (hasTransition) {
                    out.print(
                            "<td class=\"transition\">" + camelCaseToSpaced(st.eventClass().getSimpleName()) + "</td>");
                } else {
                    out.print("<td></td>");
                }
            });
            out.println("</tr>");
        });
        out.println("</table>");

        out.println("</body>");
        out.println("</html>");
        out.close();
        return new String(bytes.toByteArray(), StandardCharsets.UTF_8);
    }

    public String graphml(Function<GraphNode, NodeOptions> options, boolean includeDocumentation) {
        final Graph graph = this.getGraph();
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        PrintWriter out = new PrintWriter(bytes);
        new GraphmlWriter().printGraphml(out, graph, options, includeDocumentation);
        return new String(bytes.toByteArray(), StandardCharsets.UTF_8);
    }

    public boolean hasCreationTransition() {
        return transitions().stream().filter(t -> t.from().isCreationDestination()).findAny().isPresent();
    }

    /**
     * Get the Graph representation of this state machine definition
     * @return A Graph generated from this StateMachineDefinition
     */
    public final Graph getGraph() {
        List<GraphNode> nodes = states.stream().map(GraphNode::new).collect(Collectors.toList());
        Map<String, GraphNode> map = nodes.stream()
            .collect(Collectors.toMap(node -> node.state().name(), node -> node));
        List<GraphEdge> edges = transitions.stream().map(t -> {
            GraphNode from = map.get(t.from().name());
            GraphNode to = map.get(t.to().name());
            return new GraphEdge(from, to, t.to().eventClass().getSimpleName());
        }).collect(Collectors.toList());
        return new Graph(nodes, edges);
    }

}
