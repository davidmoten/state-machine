package com.github.davidmoten.fsm.model;

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
import java.util.stream.Collectors;

import com.github.davidmoten.fsm.graph.Graph;
import com.github.davidmoten.fsm.graph.GraphEdge;
import com.github.davidmoten.fsm.graph.GraphNode;
import com.github.davidmoten.fsm.graph.GraphmlWriter;
import com.github.davidmoten.fsm.runtime.Event;
import com.github.davidmoten.fsm.runtime.EventVoid;
import com.github.davidmoten.guavamini.Preconditions;

public final class StateMachine<T> {

    private final Class<T> cls;
    private final List<Transition<?, ?>> transitions = new ArrayList<>();
    private final Set<State<?>> states = new HashSet<State<?>>();
    private final State<Void> initialState;

    private StateMachine(Class<T> cls) {
        this.cls = cls;
        this.initialState = new State<Void>(this, "Initial", EventVoid.class);
    }

    public static <T> StateMachine<T> create(Class<T> cls) {
        return new StateMachine<T>(cls);
    }

    public Class<T> cls() {
        return cls;
    }

    public <R> State<R> createState(String name, Class<? extends Event<R>> eventClass) {
        Preconditions.checkNotNull(name);
        if (name.equals("Initial")) {
            name = name.concat("_1");
        }
        State<R> state = new State<R>(this, name, eventClass);
        states.add(state);
        return state;
    }

    public <R, S> StateMachine<T> addTransition(State<R> state, State<S> other) {
        Transition<R, S> transition = new Transition<R, S>(state, other);
        System.out.println("adding " + transition);
        for (Transition<?, ?> t : transitions) {
            if (t.from() == state && t.to() == other) {
                throw new IllegalArgumentException(
                        "the transition already exists: " + state.name() + " -> " + other.name());
            }
        }
        transitions.add(transition);
        return this;
    }

    <S> StateMachine<T> addInitialTransition(State<S> other) {
        Transition<Void, S> transition = new Transition<Void, S>(initialState, other);
        System.out.println("adding " + transition);
        transitions.add(transition);
        states.add(initialState);
        states.add(other);
        return this;
    }

    public void generateClasses(File directory, String pkg) {
        new Generator<T>(this, directory, pkg).generate();
    }

    public List<Transition<?, ?>> transitions() {
        return transitions;
    }

    public String documentationHtml() {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        PrintWriter out = new PrintWriter(bytes);
        out.println("<html/>");
        out.println("<head>");
        out.println("<style>");
        out.println("table {border-collapse: collapse;}\n"
                + "table, th, td {border: 1px solid black;}");
        out.println(".transition {background-color: #ADE2A7}");
        out.println("</style>");
        out.println("</head>");
        out.println("<body>");
        // states
        out.println("<h2>States</h2>");
        Comparator<State<?>> comparator = (a, b) -> a.name().compareTo(b.name());
        states.stream().sorted(comparator)
                .forEach(state -> out.println("<p class=\"state\"><b>" + state.name() + "</b> ["
                        + state.eventClass().getSimpleName() + "]</p>"
                        + state.documentation().orElse("")));

        // events
        out.println("<h2>Events</h2>");
        states.stream().filter(state -> !state.isInitial())
                .map(state -> state.eventClass().getSimpleName()).distinct().sorted()
                .forEach(event -> out
                        .println("<p class=\"event\"><i>" + camelCaseToSpaced(event) + "</i></p>"));

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
                        .anyMatch(t -> t.from().name().equals(state.name())
                                && t.to().name().equals(st.name()));
                if (hasTransition) {
                    out.print("<td class=\"transition\">"
                            + camelCaseToSpaced(st.eventClass().getSimpleName()) + "</td>");
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

    private static String camelCaseToSpaced(String s) {
        return s.chars().mapToObj(ch -> {
            if (ch >= 'A' && ch <= 'Z') {
                return " " + (char) ch;
            } else {
                return "" + (char) ch;
            }
        }).collect(Collectors.joining(""));
    }

    public String graphml() {
        List<GraphNode> nodes = states.stream()
                .map(state -> new GraphNode(state.name(), state.documentation().orElse("")))
                .collect(Collectors.toList());
        Map<String, GraphNode> map = nodes.stream()
                .collect(Collectors.toMap(node -> node.name(), node -> node));
        List<GraphEdge> edges = transitions.stream().map(t -> {
            GraphNode from = map.get(t.from().name());
            GraphNode to = map.get(t.to().name());
            return new GraphEdge(from, to, t.to().eventClass().getSimpleName());
        }).collect(Collectors.toList());
        Graph graph = new Graph(nodes, edges);
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        PrintWriter out = new PrintWriter(bytes);
        new GraphmlWriter().printGraphml(out, graph);
        return new String(bytes.toByteArray(), StandardCharsets.UTF_8);
    }

    public boolean hasCreationTransition() {
        return transitions().stream().filter(t -> t.from().isCreationDestination()).findAny()
                .isPresent();
    }

}
