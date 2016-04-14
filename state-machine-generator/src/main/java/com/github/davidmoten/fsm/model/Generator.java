package com.github.davidmoten.fsm.model;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.github.davidmoten.fsm.runtime.Event;
import com.github.davidmoten.guavamini.Preconditions;

public final class Generator<T> {

    private final Class<T> cls;
    private final File directory;
    private final String pkg;
    private final StateMachine<T> machine;

    public Generator(StateMachine<T> machine, File directory, String pkg) {
        this.machine = machine;
        this.cls = machine.cls();
        this.directory = directory;
        this.pkg = pkg;
    }

    private File packageDirectory() {
        return new File(
                directory.getAbsolutePath() + File.separator + pkg.replace(".", File.separator));
    }

    private File stateMachineClassFile() {
        return new File(packageDirectory(), stateMachineClassSimpleName() + ".java");
    }

    private String stateMachineClassSimpleName() {
        return cls.getSimpleName() + "StateMachine";
    }

    private String behaviourClassSimpleName() {
        return Util.toClassSimpleName(cls.getSimpleName()) + "Behaviour";
    }

    private String behaviourBaseClassSimpleName() {
        return Util.toClassSimpleName(cls.getSimpleName()) + "BehaviourBase";
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

    private String stateConstant(State<?> state) {
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

    private String instanceName() {
        return Util.lowerFirst(classSimpleName());
    }

    private String onEntryMethodName(State<?> state) {
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

    private Stream<State<? extends Object>> states() {
        return machine.transitions().stream().flatMap(t -> Stream.of(t.from(), t.to())).distinct();
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
            out.format("public interface %s {\n", behaviourClassSimpleName());
            out.println();
            indent.right();
            states().filter(state -> !state.name().equals("Initial")).forEach(state -> {
                if (state.isCreationDestination()) {
                    out.format("%s%s %s(%s event);\n", indent, imports.add(cls),
                            onEntryMethodName(state), imports.add(state.eventClass()));
                } else {
                    out.format("%s%s %s(%s %s, %s event);\n", indent, imports.add(cls),
                            onEntryMethodName(state), imports.add(cls), instanceName(),
                            imports.add(state.eventClass()));
                }
                out.println();
            });
            indent.left();
            out.format("}\n");
        }
        try (PrintStream out = new PrintStream(behaviourClassFile())) {
            out.print(new String(bytes.toByteArray()).replace("<IMPORTS>",
                    imports.importsAsString()));
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

            out.format("public %sclass %s implements %s {\n",
                    hasCreationTransition() ? "abstract " : "", behaviourBaseClassSimpleName(),
                    imports.add(behaviourClassName()));
            out.println();
            indent.right();
            states().filter(state -> !state.name().equals("Initial")).forEach(state -> {
                if (!state.isCreationDestination()) {
                    out.format("%s@%s\n", indent, imports.add(Override.class));
                    out.format("%spublic %s %s(%s %s, %s event) {\n", indent, imports.add(cls),
                            onEntryMethodName(state), imports.add(cls), instanceName(),
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
            out.print(new String(bytes.toByteArray()).replace("<IMPORTS>",
                    imports.importsAsString()));
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
            out.format("public final class %s {\n", stateMachineClassSimpleName());
            indent.right();
            out.println();
            out.format("%sprivate final %s %s;\n", indent, imports.add(cls), instanceName());
            out.format("%sprivate final %s behaviour;\n", indent,
                    imports.add(behaviourClassName()));
            out.format("%sprivate final State state;\n", indent);
            out.format("%sprivate final boolean transitionOccurred;\n", indent);
            out.println();
            out.format(
                    "%sprivate %s(%s %s, %s behaviour, State state, boolean transitionOccurred) {\n",
                    indent, stateMachineClassSimpleName(), imports.add(cls), instanceName(),
                    imports.add(behaviourClassName()));
            out.format("%s%s.checkNotNull(behaviour, \"behaviour cannot be null\");\n",
                    indent.right(), imports.add(Preconditions.class));
            out.format("%s%s.checkNotNull(state, \"state cannot be null\");\n", indent,
                    imports.add(Preconditions.class));
            out.format("%sthis.%s = %s;\n", indent, instanceName(), instanceName());
            out.format("%sthis.behaviour = behaviour;\n", indent);
            out.format("%sthis.state = state;\n", indent);
            out.format("%sthis.transitionOccurred = transitionOccurred;\n", indent);
            out.format("%s}\n", indent.left());
            out.println();
            out.format("%spublic static %s create(%s %s, %s behaviour, State state) {\n", indent,
                    stateMachineClassSimpleName(), imports.add(cls), instanceName(),
                    imports.add(behaviourClassName()));
            indent.right();
            out.format("%s%s.checkNotNull(%s, \"%s cannot be null\");\n", indent,
                    imports.add(Preconditions.class), instanceName(), instanceName());
            out.format("%sreturn new %s(%s, behaviour, state, false);\n", indent,
                    stateMachineClassSimpleName(), instanceName());
            out.format("%s}\n", indent.left());
            out.println();
            if (hasCreationTransition()) {
                out.format("%spublic static %s create(%s behaviour) {\n", indent,
                        stateMachineClassSimpleName(), imports.add(behaviourClassName()));
                out.format("%sreturn new %s(null, behaviour, State.INITIAL, false);\n",
                        indent.right(), stateMachineClassSimpleName(), instanceName());
                out.format("%s}\n", indent.left());
                out.println();
            }
            out.format("%spublic static enum State {\n", indent);
            indent.right();
            String states = states().map(state -> stateConstant(state)).distinct()
                    .collect(Collectors.joining(",\n" + indent));
            out.format("%s%s;\n", indent, states);
            indent.left();
            out.format("%s}\n", indent);
            out.println();

            Stream.concat(
                    states().filter(state -> state.isCreationDestination()).map(state -> state.eventClass()),
                    machine.transitions().stream().map(t -> t.to().eventClass())).distinct()
                    .forEach(eventClass -> {
                        out.format("%spublic %s event(%s event) {\n", indent,
                                stateMachineClassSimpleName(), imports.add(eventClass));
                        out.format("%sreturn _event(event);\n", indent.right());
                        out.format("%s}\n", indent.left());
                        out.println();
                    });

            out.format("%sprivate %s _event(%s<?> event) {\n", indent,
                    stateMachineClassSimpleName(), imports.add(Event.class));
            out.format("%s%s.checkNotNull(event);\n", indent.right(),
                    imports.add(Preconditions.class));

            boolean first = true;
            for (Transition<?, ?> t : machine.transitions()) {
                if (first) {
                    out.format("%s", indent);
                } else {
                    out.format("%s} else ", indent);
                }
                first = false;
                out.format("if (state == State.%s && event instanceof %s) {\n",
                        stateConstant(t.from()), imports.add(t.to().eventClass()));
                out.format("%sState nextState = State.%s;\n", indent.right(),
                        stateConstant(t.to()));
                if (t.from().name().equals("Initial")) {
                    out.format("%s%s nextObject = behaviour.%s((%s) event);\n", indent,
                            imports.add(cls), onEntryMethodName(t.to()),
                            imports.add(t.to().eventClass()));
                } else {
                    out.format("%s%s nextObject = behaviour.%s(%s, (%s) event);\n", indent,
                            imports.add(cls), onEntryMethodName(t.to()), instanceName(),
                            imports.add(t.to().eventClass()));
                }
                out.format("%sreturn new %s(nextObject, behaviour, nextState, true);\n", indent,
                        stateMachineClassSimpleName());
                indent.left();
            }
            if (!first) {
                // transitions exist
                out.format("%s} else {\n", indent);
                out.format("%sreturn new %s(%s, behaviour, state, false);\n", indent.right(),
                        stateMachineClassSimpleName(), instanceName());
                out.format("%s}\n", indent.left());
            } else {
                out.format("%sreturn new %s(%s, behaviour, state, false);\n", indent,
                        stateMachineClassSimpleName(), instanceName());
            }
            out.format("%s}\n", indent.left());
            out.println();
            out.format("%spublic State state() {\n", indent);
            out.format("%sreturn state;\n", indent.right());
            out.format("%s}\n", indent.left());
            out.println();
            out.format("%spublic %s<%s> get() {\n", indent, imports.add(Optional.class),
                    imports.add(cls));
            out.format("%sreturn %s.ofNullable(%s);\n", indent.right(), imports.add(Optional.class),
                    instanceName());
            out.format("%s}\n", indent.left());
            out.println();
            out.format("%spublic boolean transitionOccurred() {\n", indent);
            out.format("%sreturn transitionOccurred;\n", indent.right(),
                    imports.add(Optional.class), instanceName());
            out.format("%s}\n", indent.left());
            out.format("%s}", indent.left());
        }
        try (PrintStream out = new PrintStream(stateMachineClassFile())) {
            out.print(new String(bytes.toByteArray()).replace("<IMPORTS>",
                    imports.importsAsString()));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private String classSimpleName() {
        return cls.getSimpleName();
    }

}
