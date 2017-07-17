package com.github.davidmoten.fsm.model;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.github.davidmoten.bean.annotation.ImmutableBean;
import com.github.davidmoten.fsm.Util;
import com.github.davidmoten.fsm.runtime.Action3;
import com.github.davidmoten.fsm.runtime.CancelTimedSignal;
import com.github.davidmoten.fsm.runtime.Clock;
import com.github.davidmoten.fsm.runtime.ClockDefault;
import com.github.davidmoten.fsm.runtime.EntityBehaviour;
import com.github.davidmoten.fsm.runtime.EntityState;
import com.github.davidmoten.fsm.runtime.EntityStateMachine;
import com.github.davidmoten.fsm.runtime.Event;
import com.github.davidmoten.fsm.runtime.Search;
import com.github.davidmoten.fsm.runtime.SearchUnsupported;
import com.github.davidmoten.fsm.runtime.Signal;
import com.github.davidmoten.fsm.runtime.Signaller;
import com.github.davidmoten.guavamini.Preconditions;
import com.github.davidmoten.guavamini.Sets;

public final class Generator<T> {

    private final File directory;
    private final String pkg;
    private final StateMachineDefinition<T> machine;
    private final String clsName;
    private String clsSimpleName;

    public Generator(StateMachineDefinition<T> machine, File directory, String pkg) {
        this.machine = machine;
        this.clsName = getClassName(machine);
        this.clsSimpleName = getSimpleName(clsName);
        this.directory = directory;
        this.pkg = pkg;
    }

    private static String getClassName(StateMachineDefinition<?> machine) {
        String clsName = machine.cls().getName().replace("$", ".");
        if (machine.cls().isAnnotationPresent(ImmutableBean.class)) {
            int i = clsName.lastIndexOf(".");
            if (i == -1) {
                return "immutable." + clsName;
            } else {
                return clsName.substring(0, i) + ".immutable" + clsName.substring(i);
            }
        } else {
            return clsName;
        }
    }

    private static String getSimpleName(String clsName) {
        int i = clsName.lastIndexOf(".");
        if (i == -1) {
            return clsName;
        } else {
            return clsName.substring(i + 1, clsName.length());
        }
    }

    private File packageDirectory() {
        return new File(directory.getAbsolutePath() + File.separator + pkg.replace(".", File.separator));
    }

    private File stateMachineClassFile() {
        return new File(packageDirectory(), stateMachineClassSimpleName() + ".java");
    }

    private String stateMachineClassSimpleName() {
        return clsSimpleName + "StateMachine";
    }

    private String behaviourClassSimpleName() {
        return Util.toClassSimpleName(clsSimpleName) + "Behaviour";
    }

    private String behaviourBaseClassSimpleName() {
        return Util.toClassSimpleName(clsSimpleName) + "BehaviourBase";
    }

    private String behaviourClassName() {
        return pkg + "." + behaviourClassSimpleName();
    }

    private File behaviourClassFile() {
        return new File(packageDirectory(), behaviourClassSimpleName() + ".java");
    }

    private File behaviourBaseClassFile() {
        return new File(packageDirectory(), behaviourBaseClassSimpleName() + ".java");
    }

    private String stateConstant(State<?, ?> state) {
        return Util.toJavaConstantIdentifier(state.name());
    }

    private static class Indent {
        int n;

        @Override
        public String toString() {
            StringBuilder s = new StringBuilder();
            for (int i = 0; i < n; i++) {
                s.append(' ');
            }
            return s.toString();
        }

        Indent right() {
            n += 4;
            return this;
        }

        Indent left() {
            n = Math.max(0, n - 4);
            return this;
        }

    }

    private static Set<String> reservedWords = Sets.newHashSet("behaviour", "state", "clock", "id", "previousState",
            "transitionOccurred", "signalsToSelf", "signalsToOther", "search", "_event", "repaying", "preTransition");

    private static Set<String> javaReservedWords = Sets.newHashSet("abstract", "assert", "boolean", "break", "byte",
            "case", "catch", "char", "class", "const", "continue", "default", "do", "double", "else", "extends",
            "false", "final", "finally", "float", "for", "goto", "if", "implements", "import", "instanceof", "int",
            "interface", "long", "native", "new", "null", "package", "private", "protected", "public", "return",
            "short", "static", "strictfp", "super", "switch", "synchronized", "this", "throw", "throws", "transient",
            "true", "try", "void", "volatile", "while");

    private String instanceName() {
        String candidate = Util.lowerFirst(classSimpleName());
        while (reservedWords.contains(candidate) || javaReservedWords.contains(candidate)) {
            candidate = "_" + candidate;
        }
        return candidate;
    }

    private String onEntryMethodName(State<?, ?> state) {
        return "onEntry_" + Util.upperFirst(Util.toJavaIdentifier(state.name()));
    }

    private boolean hasCreationTransition() {
        return machine.hasCreationTransition();
    }

    public void generate() {
        generateStateMachine();
        System.out.println("generated " + stateMachineClassFile());
        generateBehaviourInterface();
        System.out.println("generated " + behaviourClassFile());
        generateBehaviourBase();
        System.out.println("generated " + behaviourBaseClassFile());
    }

    private Stream<State<T, ? extends Event<? super T>>> states() {
        Stream<State<T, ? extends Event<? super T>>> o = machine.transitions().stream()
                .flatMap(t -> Stream.<State<T, ? extends Event<? super T>>>of(t.from(), t.to())).distinct();
        return o;
    }

    private void generateBehaviourInterface() {
        behaviourClassFile().getParentFile().mkdirs();
        Imports imports = new Imports();
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        try (PrintStream out = new PrintStream(bytes)) {
            out.format("package %s;\n", pkg);
            out.println();
            out.println("<IMPORTS>");
            out.println();
            Indent indent = new Indent();
            out.format("public interface %s<Id> extends %s<%s, Id>{\n", behaviourClassSimpleName(),
                    imports.add(EntityBehaviour.class), imports.add(clsName));
            out.println();
            indent.right();
            out.format("%s@%s\n", indent, imports.add(Override.class));
            out.format("%s%s<Id> create(Id id);\n", indent, this.stateMachineClassSimpleName());
            out.println();

            out.format("%s@%s\n", indent, imports.add(Override.class));
            out.format("%sdefault %s<Id> create(Id id, %s entity, %s<%s> state) {\n", indent,
                    this.stateMachineClassSimpleName(), imports.add(clsName), imports.add(EntityState.class),
                    imports.add(clsName));
            indent.right();
            out.format("%sreturn %s.create(entity, id, this, (%s.State) state);\n", indent,
                    this.stateMachineClassSimpleName(), this.stateMachineClassSimpleName());
            indent.left();
            out.format("%s}\n", indent);
            out.println();

            out.format("%s@%s\n", indent, imports.add(Override.class));
            out.format("%sdefault %s.State from(String name) {\n", indent, this.stateMachineClassSimpleName(),
                    imports.add(clsName));
            indent.right();
            out.format("%sreturn %s.State.valueOf(name);\n", indent, this.stateMachineClassSimpleName());
            indent.left();
            out.format("%s}\n", indent);
            out.println();

            states().filter(state -> !state.name().equals("Initial")).forEach(state -> {
                if (state.isCreationDestination()) {
                    out.format("%s%s %s(%s<%s, Id> signaller, Id id, %s event, boolean replaying);\n", indent,
                            imports.add(clsName), onEntryMethodName(state), imports.add(Signaller.class),
                            imports.add(clsName), imports.add(state.eventClass()));
                } else {
                    out.format("%s%s %s(%s<%s, Id> signaller, %s %s, Id id, %s event, boolean replaying);\n", indent,
                            imports.add(clsName), onEntryMethodName(state), imports.add(Signaller.class),
                            imports.add(clsName), imports.add(clsName), instanceName(),
                            imports.add(state.eventClass()));
                }
                out.println();
            });

            indent.left();
            out.format("}\n");
        }
        try (PrintStream out = new PrintStream(behaviourClassFile())) {
            out.print(new String(bytes.toByteArray()).replace("<IMPORTS>", imports.importsAsString()));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private void generateBehaviourBase() {
        behaviourBaseClassFile().getParentFile().mkdirs();
        Imports imports = new Imports();
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        try (PrintStream out = new PrintStream(bytes)) {
            out.format("package %s;\n", pkg);
            out.println();
            out.println("<IMPORTS>");
            out.println();
            Indent indent = new Indent();

            out.format("public abstract class %s<Id> implements %s<Id> {\n", behaviourBaseClassSimpleName(),
                    imports.add(behaviourClassName()));
            out.println();
            indent.right();
            states().filter(state -> !state.name().equals("Initial")).forEach(state -> {
                if (!state.isCreationDestination()) {
                    out.format("%s@%s\n", indent, imports.add(Override.class));
                    out.format("%spublic %s %s(%s<%s, Id> signaller, %s %s, Id id, %s event, boolean replaying) {\n",
                            indent, imports.add(clsName), onEntryMethodName(state), imports.add(Signaller.class),
                            imports.add(clsName), imports.add(clsName), instanceName(),
                            imports.add(state.eventClass()));
                    out.format("%sreturn %s;\n", indent.right(), instanceName());
                    out.format("%s}\n", indent.left());
                    out.println();
                }
            });
            indent.left();
            out.format("}\n");
        }
        try (PrintStream out = new PrintStream(behaviourBaseClassFile())) {
            out.print(new String(bytes.toByteArray()).replace("<IMPORTS>", imports.importsAsString()));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private void generateStateMachine() {
        Imports imports = new Imports();
        stateMachineClassFile().getParentFile().mkdirs();
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        try (PrintStream out = new PrintStream(bytes)) {
            out.format("package %s;\n", pkg);
            out.println();
            out.println("<IMPORTS>");
            out.println();
            Indent indent = new Indent();

            // Class Declaration
            out.format("public final class %s<T> implements %s<%s, T>, %s<%s, T> {\n", stateMachineClassSimpleName(),
                    imports.add(EntityStateMachine.class), imports.add(clsName), imports.add(Signaller.class),
                    imports.add(clsName));
            indent.right();
            out.println();

            // Fields
            out.format("%sprivate final %s %s;\n", indent, imports.add(clsName), instanceName());
            out.format("%sprivate final T id;\n", indent);
            out.format("%sprivate final %s<T> behaviour;\n", indent, imports.add(behaviourClassName()));
            out.format("%sprivate final State state;\n", indent);
            out.format("%sprivate final %s<State> previousState;\n", indent, imports.add(Optional.class));
            out.format("%sprivate final boolean transitionOccurred;\n", indent);
            out.format("%sprivate final %s<%s<? super %s>> signalsToSelf;\n", indent, imports.add(List.class),
                    imports.add(Event.class), imports.add(clsName));
            out.format("%sprivate final %s<%s<?, T>> signalsToOther;\n", indent, imports.add(List.class),
                    imports.add(Signal.class));
            out.format("%sprivate final %s clock;\n", indent, imports.add(Clock.class));
            out.format("%sprivate final %s<T> search;\n", indent, imports.add(Search.class));
            out.format("%sprivate final %s<%s<? super %s>> _event;\n", indent, imports.add(Optional.class),
                    imports.add(Event.class), imports.add(clsName));
            out.format("%sprivate final boolean replaying;\n", indent);
            out.format("%sprivate final %s<? super %s<%s, T>, ? super %s<? super %s>, ? super %s<%s>> preTransition;\n",
                    indent, imports.add(Action3.class), imports.add(EntityStateMachine.class), imports.add(clsName),
                    imports.add(Event.class), imports.add(clsName), imports.add(EntityState.class),
                    imports.add(clsName));

            out.println();

            // Constructor
            out.format(
                    "%sprivate %s(%s %s, T id, %s<T> behaviour, %s<State> previousState, State state, boolean transitionOccurred, %s<%s<? super %s>> signalsToSelf, %s<%s<?, T>> signalsToOther, %s<T> search, %s clock, %s<%s<? super %s>> _event, boolean replaying, %s<? super %s<%s, T>, ? super %s<? super %s>, ? super %s<%s>> preTransition) {\n",
                    indent, stateMachineClassSimpleName(), imports.add(clsName), instanceName(),
                    imports.add(behaviourClassName()), imports.add(Optional.class), imports.add(List.class),
                    imports.add(Event.class), imports.add(clsName), imports.add(List.class), imports.add(Signal.class),
                    imports.add(Search.class), imports.add(Clock.class), imports.add(Optional.class),
                    imports.add(Event.class), imports.add(clsName), imports.add(Action3.class),
                    imports.add(EntityStateMachine.class), imports.add(clsName), imports.add(Event.class),
                    imports.add(clsName), imports.add(EntityState.class), imports.add(clsName));
            out.format("%s%s.checkNotNull(behaviour, \"behaviour cannot be null\");\n", indent.right(),
                    imports.add(Preconditions.class));
            out.format("%s%s.checkNotNull(id, \"id cannot be null\");\n", indent, imports.add(Preconditions.class));
            out.format("%s%s.checkNotNull(previousState, \"previousState cannot be null\");\n", indent,
                    imports.add(Preconditions.class));
            out.format("%s%s.checkNotNull(state, \"state cannot be null\");\n", indent,
                    imports.add(Preconditions.class));
            out.format("%s%s.checkNotNull(signalsToSelf, \"signalsToSelf cannot be null\");\n", indent,
                    imports.add(Preconditions.class));
            out.format("%s%s.checkNotNull(signalsToOther, \"signalsToOther cannot be null\");\n", indent,
                    imports.add(Preconditions.class));
            out.format("%s%s.checkNotNull(search, \"search cannot be null\");\n", indent,
                    imports.add(Preconditions.class));
            out.format("%s%s.checkNotNull(clock, \"clock cannot be null\");\n", indent,
                    imports.add(Preconditions.class));
            out.format("%s%s.checkNotNull(_event, \"event cannot be null\");\n", indent,
                    imports.add(Preconditions.class));
            out.format("%s%s.checkNotNull(_event, \"preTransition cannot be null\");\n", indent,
                    imports.add(Preconditions.class));
            out.format("%sthis.%s = %s;\n", indent, instanceName(), instanceName());
            out.format("%sthis.id = id;\n", indent);
            out.format("%sthis.behaviour = behaviour;\n", indent);
            out.format("%sthis.previousState = previousState;\n", indent);
            out.format("%sthis.state = state;\n", indent);
            out.format("%sthis.transitionOccurred = transitionOccurred;\n", indent);
            out.format("%sthis.signalsToSelf = signalsToSelf;\n", indent);
            out.format("%sthis.signalsToOther = signalsToOther;\n", indent);
            out.format("%sthis.search = search;\n", indent);
            out.format("%sthis.clock = clock;\n", indent);
            out.format("%sthis._event = _event;\n", indent);
            out.format("%sthis.replaying = replaying;\n", indent);
            out.format("%sthis.preTransition = preTransition;\n", indent);
            out.format("%s}\n", indent.left());
            out.println();

            out.format("%spublic static <T> %s<T> create(%s %s, T id, %s<T> behaviour, State state) {\n", indent,
                    stateMachineClassSimpleName(), imports.add(clsName), instanceName(),
                    imports.add(behaviourClassName()), imports.add(Clock.class));
            indent.right();
            out.format(
                    "%sreturn new %s<T>(%s, id, behaviour, %s.empty(), state, false, new %s<%s<? super %s>>(), new %s<%s<?, T>>(), %s.instance(), %s.instance(), %s.empty(), false, %s.<%s, %s, %s>doNothing());\n",
                    indent, stateMachineClassSimpleName(), instanceName(), imports.add(Optional.class),
                    imports.add(ArrayList.class), imports.add(Event.class), imports.add(clsName),
                    imports.add(ArrayList.class), imports.add(Signal.class), imports.add(SearchUnsupported.class),
                    imports.add(ClockDefault.class), imports.add(Optional.class), imports.add(Action3.class),
                    imports.add(Object.class), imports.add(Object.class), imports.add(Object.class));
            out.format("%s}\n", indent.left());
            out.println();

            if (hasCreationTransition()) {
                out.format("%spublic static <T> %s<T> create(T id, %s<T> behaviour) {\n", indent,
                        stateMachineClassSimpleName(), imports.add(behaviourClassName()));
                out.format(
                        "%sreturn new %s<T>(null, id, behaviour, %s.empty(), State.INITIAL, false, new %s<%s<? super %s>>(), new %s<%s<?, T>>(), %s.instance(), %s.instance(), %s.empty(), false, %s.<%s, %s, %s>doNothing());\n",
                        indent.right(), stateMachineClassSimpleName(), imports.add(Optional.class),
                        imports.add(ArrayList.class), imports.add(Event.class), imports.add(clsName),
                        imports.add(ArrayList.class), imports.add(Signal.class), imports.add(SearchUnsupported.class),
                        imports.add(ClockDefault.class), imports.add(Optional.class), imports.add(Action3.class),
                        imports.add(Object.class), imports.add(Object.class), imports.add(Object.class));
                out.format("%s}\n", indent.left());
                out.println();
            }

            // withSearch()
            out.format("%s@%s\n", indent, imports.add(Override.class));
            out.format("%spublic %s<T> withSearch(%s<T> search) {\n", indent, stateMachineClassSimpleName(),
                    imports.add(Search.class));
            out.format(
                    "%sreturn new %s<T>(%s, id, behaviour, previousState, state, transitionOccurred, signalsToSelf, signalsToOther, search, clock, _event, replaying, preTransition);\n",
                    indent.right(), stateMachineClassSimpleName(), instanceName());
            out.format("%s}\n", indent.left());
            out.println();

            // withClock()
            out.format("%s@%s\n", indent, imports.add(Override.class));
            out.format("%spublic %s<T> withClock(%s clock) {\n", indent, stateMachineClassSimpleName(),
                    imports.add(Clock.class));
            out.format(
                    "%sreturn new %s<T>(%s, id, behaviour, previousState, state, transitionOccurred, signalsToSelf, signalsToOther, search, clock, _event, replaying, preTransition);\n",
                    indent.right(), stateMachineClassSimpleName(), instanceName());
            out.format("%s}\n", indent.left());
            out.println();

            // clock()
            out.format("%s@%s\n", indent, imports.add(Override.class));
            out.format("%spublic %s clock() {\n", indent, imports.add(Clock.class));
            out.format("%sreturn clock;\n", indent.right(), stateMachineClassSimpleName(), instanceName());
            out.format("%s}\n", indent.left());
            out.println();

            // replaying()
            out.format("%s@%s\n", indent, imports.add(Override.class));
            out.format("%spublic %s<T> replaying() {\n", indent, stateMachineClassSimpleName());
            out.format(
                    "%sreturn new %s<T>(%s, id, behaviour, previousState, state, transitionOccurred, signalsToSelf, signalsToOther, search, clock, _event, true, preTransition);\n",
                    indent.right(), stateMachineClassSimpleName(), instanceName());
            out.format("%s}\n", indent.left());
            out.println();

            // withPreTransition()
            out.format("%s@%s\n", indent, imports.add(Override.class));
            out.format(
                    "%spublic %s<T> withPreTransition(%s<? super %s<%s, T>, ? super %s<? super %s>, ? super %s<%s>> preTransition) {\n",
                    indent, stateMachineClassSimpleName(), imports.add(Action3.class),
                    imports.add(EntityStateMachine.class), imports.add(clsName), imports.add(Event.class),
                    imports.add(clsName), imports.add(EntityState.class), imports.add(clsName));
            out.format(
                    "%sreturn new %s<T>(%s, id, behaviour, previousState, state, transitionOccurred, signalsToSelf, signalsToOther, search, clock, _event, replaying, preTransition);\n",
                    indent.right(), stateMachineClassSimpleName(), instanceName());
            out.format("%s}\n", indent.left());
            out.println();

            // States
            out.format("%spublic static enum State implements %s<%s> {\n", indent, imports.add(EntityState.class),
                    imports.add(clsName));
            indent.right();
            String states = states().map(state -> stateConstant(state)).distinct()
                    .collect(Collectors.joining(",\n" + indent));
            out.format("%s%s;\n", indent, states);
            indent.left();
            out.format("%s}\n", indent);
            out.println();

            Stream.concat(states().filter(state -> state.isCreationDestination()).map(state -> state.eventClass()),
                    machine.transitions().stream().map(t -> t.to().eventClass())).distinct().forEach(eventClass -> {
                        out.format("%spublic %s<T> signal(%s event) {\n", indent, stateMachineClassSimpleName(),
                                imports.add(eventClass));
                        out.format("%sreturn signal((%s<? super %s>) event);\n", indent.right(),
                                imports.add(Event.class), imports.add(clsName));
                        out.format("%s}\n", indent.left());
                        out.println();
                    });

            out.format("%s@%s\n", indent, imports.add(Override.class));
            out.format("%spublic %s<T> signal(%s<? super %s> event) {\n", indent, stateMachineClassSimpleName(),
                    imports.add(Event.class), imports.add(clsName));
            out.format("%s%s.checkNotNull(event);\n", indent.right(), imports.add(Preconditions.class));
            out.format("%ssignalsToSelf.clear();\n", indent);
            out.format("%ssignalsToOther.clear();\n", indent);
            boolean first = true;
            for (Transition<?, ?, ?> t : machine.transitions()) {
                if (first) {
                    out.format("%s", indent);
                } else {
                    out.format("%s} else ", indent);
                }
                first = false;
                out.format("if (state == State.%s && event instanceof %s) {\n", stateConstant(t.from()),
                        imports.add(t.to().eventClass()));
                out.format("%sState nextState = State.%s;\n", indent.right(), stateConstant(t.to()));
                out.format("%spreTransition.call(this, event, nextState);\n", indent);
                if (t.from().name().equals("Initial")) {
                    out.format("%s%s nextObject = behaviour.%s(this, this.id, (%s) event, replaying);\n", indent,
                            imports.add(clsName), onEntryMethodName(t.to()), imports.add(t.to().eventClass()));
                } else {
                    out.format("%s%s nextObject = behaviour.%s(this, %s, this.id, (%s) event, replaying);\n", indent,
                            imports.add(clsName), onEntryMethodName(t.to()), instanceName(),
                            imports.add(t.to().eventClass()));
                }
                out.format(
                        "%sreturn new %s<T>(nextObject, this.id, behaviour, %s.of(state), nextState, true, signalsToSelf, signalsToOther, search, clock, %s.of(event), replaying, preTransition);\n",
                        indent, stateMachineClassSimpleName(), imports.add(Optional.class),
                        imports.add(Optional.class));
                indent.left();
            }
            if (!first) {
                // transitions exist
                out.format("%s} else {\n", indent);
                out.format(
                        "%sreturn new %s<T>(%s, this.id, behaviour, previousState, state, false, new %s<%s<? super %s>>(), new %s<%s<?, T>>(), search, clock, %s.of(event), replaying, preTransition);\n",
                        indent.right(), stateMachineClassSimpleName(), instanceName(), imports.add(ArrayList.class),
                        imports.add(Event.class), imports.add(clsName), imports.add(ArrayList.class),
                        imports.add(Signal.class), imports.add(Optional.class));
                out.format("%s}\n", indent.left());
            } else {
                out.format(
                        "%sreturn new %s<T>(%s, this.id, behaviour, previousState, state, false, signalsToSelf, signalsToOther, search, clock, %s.of(event), replaying, preTransition);\n",
                        indent, stateMachineClassSimpleName(), instanceName(), imports.add(Optional.class));
            }
            out.format("%s}\n", indent.left());
            out.println();
            out.format("%s@%s\n", indent, imports.add(Override.class));
            out.format("%spublic State state() {\n", indent);
            out.format("%sreturn state;\n", indent.right());
            out.format("%s}\n", indent.left());
            out.println();
            out.format("%s@%s\n", indent, imports.add(Override.class));
            out.format("%spublic %s<%s> get() {\n", indent, imports.add(Optional.class), imports.add(clsName));
            out.format("%sreturn %s.ofNullable(%s);\n", indent.right(), imports.add(Optional.class), instanceName());
            out.format("%s}\n", indent.left());
            out.println();
            out.format("%s@%s\n", indent, imports.add(Override.class));
            out.format("%spublic boolean transitionOccurred() {\n", indent);
            out.format("%sreturn transitionOccurred;\n", indent.right(), imports.add(Optional.class), instanceName());
            out.format("%s}\n", indent.left());

            out.println();
            // List<Event<?>> signalsToSelf();
            out.format("%spublic %s<%s<? super %s>> signalsToSelf() {\n", indent, imports.add(List.class),
                    imports.add(Event.class), imports.add(clsName));
            out.format("%sreturn %s.unmodifiableList(signalsToSelf);\n", indent.right(),
                    imports.add(Collections.class));
            out.format("%s}\n", indent.left());
            out.println();

            // List<Signal<?, ?>> signalsToOther();
            out.format("%spublic %s<%s<?, ?>> signalsToOther() {\n", indent, imports.add(List.class),
                    imports.add(Signal.class));
            out.format("%sreturn %s.unmodifiableList(signalsToOther);\n", indent.right(),
                    imports.add(Collections.class));
            out.format("%s}\n", indent.left());

            out.println();
            // void signalToSelf(event);
            out.format("%s@%s\n", indent, imports.add(Override.class));
            out.format("%spublic void signalToSelf(%s<? super %s> event) {\n", indent, imports.add(Event.class),
                    imports.add(clsName));
            addIfReplayReturn(out, indent.right());
            out.format("%ssignalsToSelf.add(event);\n", indent, imports.add(ArrayList.class));
            out.format("%s}\n", indent.left());
            out.println();

            // void signal(Class<?> cls, Object id, Event<?> event);
            out.format("%s@%s\n", indent, imports.add(Override.class));
            out.format("%spublic <R> void signal(%s<R> cls, T id, %s<? super R> event) {\n", indent,
                    imports.add(Class.class), imports.add(Event.class));
            addIfReplayReturn(out, indent.right());
            out.format("%ssignalsToOther.add(%s.create(cls, id, event));\n", indent, imports.add(Signal.class));
            out.format("%s}\n", indent.left());
            out.println();

            // void signal(Class<?> cls, Object id, Event<?> event, long
            // delay, TimeUnit
            // unit);
            out.format("%s@%s\n", indent, imports.add(Override.class));
            out.format("%spublic <R> void signal(%s<R> cls, T id, %s<? super R> event, long delay, %s unit) {\n",
                    indent, imports.add(Class.class), imports.add(Event.class), imports.add(TimeUnit.class));
            out.format("%s%s.checkNotNull(unit, \"unit cannot be null\");\n", indent.right(),
                    imports.add(Preconditions.class));
            addIfReplayReturn(out, indent);
            out.format("%slong time = clock.now() + unit.toMillis(delay);\n", indent);
            out.format("%ssignalsToOther.add(%s.create(cls, id, event, time));\n", indent, imports.add(Signal.class));
            out.format("%s}\n", indent.left());
            out.println();

            // <T> void signalToSelf(Event<?> event, long delay, TimeUnit unit);
            out.format("%s@%s\n", indent, imports.add(Override.class));
            out.format("%spublic void signalToSelf(%s<? super %s> event, long delay, %s unit) {\n", indent,
                    imports.add(Event.class), imports.add(clsName), imports.add(TimeUnit.class));
            out.format("%s%s.checkNotNull(unit, \"unit cannot be null\");\n", indent.right(),
                    imports.add(Preconditions.class));
            addIfReplayReturn(out, indent);
            out.format("%sif (delay <= 0) {\n", indent);
            out.format("%ssignalToSelf(event);\n", indent.right());
            out.format("%s} else {\n", indent.left());
            out.format("%slong time = clock.now() + unit.toMillis(delay);\n", indent.right());
            out.format("%ssignalsToOther.add(%s.create(%s.class, this.id, event, time));\n", indent,
                    imports.add(Signal.class), imports.add(clsName));
            out.format("%s}\n", indent.left());
            out.format("%s}\n", indent.left());
            out.println();

            // void cancelSignal(Class<?> fromClass, Object fromId, Class<?>
            // toClass, Object toId);
            out.format("%s@%s\n", indent, imports.add(Override.class));
            out.format("%spublic void cancelSignal(%s<?> fromClass, T fromId, %s<?> toClass, T toId) {\n", indent,
                    imports.add(Class.class), imports.add(Class.class));
            addIfReplayReturn(out, indent.right());
            out.format("%ssignalsToOther.add(%s.create(toClass, toId, new %s<T>(fromClass, fromId)));\n", indent,
                    imports.add(Signal.class), imports.add(CancelTimedSignal.class));
            out.format("%s}\n", indent.left());
            out.println();

            // void cancelSignalToSelf();
            out.format("%s@%s\n", indent, imports.add(Override.class));
            out.format("%spublic void cancelSignalToSelf() {\n", indent, imports.add(Class.class),
                    imports.add(Class.class));
            addIfReplayReturn(out, indent.right());
            out.format("%ssignalsToSelf.add(new %s<T>(cls(), id));\n", indent, imports.add(CancelTimedSignal.class));
            out.format("%s}\n", indent.left());
            out.println();

            // Class<T> cls()
            out.format("%s@%s\n", indent, imports.add(Override.class));
            out.format("%spublic %s<%s> cls() {\n", indent, imports.add(Class.class), imports.add(clsName));
            out.format("%sreturn %s.class;\n", indent.right(), imports.add(clsName));
            out.format("%s}\n", indent.left());
            out.println();

            // long now()
            out.format("%s@%s\n", indent, imports.add(Override.class));
            out.format("%spublic long now() {\n", indent);
            out.format("%sreturn clock.now();\n", indent.right());
            out.format("%s}\n", indent.left());
            out.println();

            // Optional<R> search()
            out.format("%s@%s\n", indent, imports.add(Override.class));
            out.format("%spublic <R> %s<R> search(%s<R> cls, T id) {\n", indent, imports.add(Optional.class),
                    imports.add(Class.class));
            out.format("%sreturn search.search(cls, id);\n", indent.right());
            out.format("%s}\n", indent.left());
            out.println();

            // Optional<State> previousState()
            out.format("%s@%s\n", indent, imports.add(Override.class));
            out.format("%spublic %s<State> previousState() {\n", indent, imports.add(Optional.class));
            out.format("%sreturn previousState;\n", indent.right());
            out.format("%s}\n", indent.left());
            out.println();

            // Optional<Event<Microwave>>> event()
            out.format("%s@%s\n", indent, imports.add(Override.class));
            out.format("%spublic %s<%s<? super %s>> event() {\n", indent, imports.add(Optional.class),
                    imports.add(Event.class), imports.add(clsName));
            out.format("%sreturn _event;\n", indent.right());
            out.format("%s}\n", indent.left());
            out.println();

            // T id()
            out.format("%s@%s\n", indent, imports.add(Override.class));
            out.format("%spublic T id() {\n", indent);
            out.format("%sreturn id;\n", indent.right());
            out.format("%s}\n", indent.left());
            out.println();

            out.format("%s}", indent.left());
        }
        try (PrintStream out = new PrintStream(stateMachineClassFile())) {
            out.print(new String(bytes.toByteArray()).replace("<IMPORTS>", imports.importsAsString()));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private void addIfReplayReturn(PrintStream out, Indent indent) {
        out.format("%sif (replaying) {\n", indent);
        out.format("%sreturn;\n", indent.right());
        out.format("%s}\n", indent.left());
    }

    private String classSimpleName() {
        return clsSimpleName;
    }

}