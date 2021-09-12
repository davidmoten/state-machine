package com.github.davidmoten.bean;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.stream.Collectors;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.github.davidmoten.bean.annotation.GenerateImmutable;
import com.github.davidmoten.guavamini.Preconditions;
import com.github.javaparser.JavaParser;
import com.github.javaparser.ParseResult;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.ImportDeclaration;
import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.NodeList;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.body.FieldDeclaration;
import com.github.javaparser.ast.body.VariableDeclarator;
import com.github.javaparser.ast.expr.MarkerAnnotationExpr;
import com.github.javaparser.ast.type.ClassOrInterfaceType;

public final class ImmutableBeanGenerator {

    private static final String NL = "\n";

    private static final boolean GENERATE_CREATE_METHOD = false;

    public static void generate(String code, File generatedSource) {
        Generated g = generate(code);
        File file = new File(generatedSource, g.className().replace(".", File.separator) + ".java");
        file.getParentFile().mkdirs();
        try (FileOutputStream fos = new FileOutputStream(file)) {
            fos.write(g.generatedCode().getBytes(StandardCharsets.UTF_8));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public static final class Generated {
        private final String generatedCode;
        private final String className;

        public Generated(String generatedCode, String className) {
            this.generatedCode = generatedCode;
            this.className = className;
        }

        public String generatedCode() {
            return generatedCode;
        }

        public String className() {
            return className;
        }

        public String pkgName() {
            int i = className.lastIndexOf(".");
            if (i == -1) {
                return "";
            } else {
                return className.substring(0, i);
            }
        }
    }

    public static Generated generate(String code) {
        ParseResult<CompilationUnit> pr = new JavaParser().parse(code);
        CompilationUnit cu = pr.getResult().get();
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        PrintStream s = new PrintStream(bytes);

        String newPkg = cu //
                .getPackageDeclaration() //
                .map(p -> p.getName().toString()) //
                .orElse("") + ".immutable";
        s.format("package %s;\n\n", newPkg);
        s.format("<IMPORTS>\n");
        Map<String, String> imports;
        {
            NodeList<ImportDeclaration> n = cu.getImports();
            if (n != null) {
                imports = new HashMap<>(n.stream() //
                        .filter(x -> !x.getName().toString().equals(GenerateImmutable.class.getName())) //
                        .collect(Collectors.<ImportDeclaration, String, String>toMap(
                                x -> simpleName(x.getName().toString()), x -> x.getName().toString())));
            } else {
                imports = new HashMap<>();
            }
        }
        String indent = "    ";

        s.format("\n/////////////////////////////////////////////////////\n" + //
                "// WARNING - Generated data class! \n" //
                + "/////////////////////////////////////////////////////\n");

        {
            for (Node n : cu.getChildNodes()) {
                if (n instanceof ClassOrInterfaceDeclaration) {
                    ClassOrInterfaceDeclaration c = (ClassOrInterfaceDeclaration) n;
                    writeClassDeclaration(s, c);

                    List<FieldDeclaration> fields = getFields(c);

                    List<VariableDeclarator> vars = getVars(fields);

                    // fields
                    writeFields(s, indent, fields);

                    // constructor
                    writeConstructor(s, indent, c, fields, vars, imports);

                    // create
                    writeCreateMethod(s, indent, c, vars);

                    // getters
                    writeGetters(s, indent, vars);

                    // with
                    writeWiths(s, indent, c, vars);

                    // static methods
                    writeStaticMethods(s, indent, c);

                    // builder
                    writeBuilder(s, indent, c, vars);

                    // hashCode
                    writeHashCode(s, imports, indent, vars);

                    // equals
                    writeEquals(s, imports, indent, c, vars);

                    // toString
                    writeToString(s, imports, indent, c, vars);

                    s.append("\n}");

                    // imports
                    return new Generated(insertImports(bytes, imports), newPkg + "." + c.getName());
                }
            }
        }
        throw new RuntimeException("expected class structure not found");
    }

    private static List<VariableDeclarator> getVars(List<FieldDeclaration> fields) {
        List<VariableDeclarator> vars = fields.stream() //
                .map(x -> variableDeclarator(x)) //
                .collect(Collectors.toList());
        return vars;
    }

    private static List<FieldDeclaration> getFields(ClassOrInterfaceDeclaration c) {
        List<FieldDeclaration> fields = c.getChildNodes() //
                .stream() //
                .filter(x -> x instanceof FieldDeclaration) //
                .map(x -> (FieldDeclaration) x) //
                .collect(Collectors.toList());
        return fields;
    }

    private static void writeStaticMethods(PrintStream s, String indent, ClassOrInterfaceDeclaration c) {
        if (!c.getMethods().isEmpty()) {
            c //
                    .getMethods() //
                    .stream() //
                    .filter(x -> x.isStatic()) //
                    .forEach(x -> {
                        s.format("\n\n%s%s", indent, x.toString().replaceAll("\n", "\n" + indent));
                    });
        }
    }

    private static void writeBuilder(PrintStream s, String indent, ClassOrInterfaceDeclaration c,
            List<VariableDeclarator> vars) {
        if (!vars.isEmpty()) {
            Iterator<VariableDeclarator> it = vars.iterator();
            s.format("\n\n%s// Constructor synchronized builder pattern.", indent);
            s.format("\n%s// Changing the parameter list in the source", indent);
            s.format("\n%s// and regenerating will provoke compiler errors", indent);
            s.format("\n%s// wherever the builder is used.", indent);

            VariableDeclarator first = it.next();
            if (vars.size() == 1) {
                s.format("\n\n%spublic static %s createWith%s(%s %s) {", indent, c.getName(),
                        capFirst(first.getName().toString()), first.getType(), first.getName());
                s.format("\n%s%sreturn new %s(%s);", indent, indent, c.getName(), first.getName());
                s.format("\n%s}", indent);
            } else {
                s.format("\n\n%spublic static Builder2 createWith%s(%s %s) {", indent,
                        capFirst(first.getName().toString()), first.getType(), first.getName());
                s.format("\n%s%sBuilder b = new Builder();", indent, indent);
                s.format("\n%s%sb.%s = %s;", indent, indent, first.getName(), first.getName());
                s.format("\n%s%sreturn new Builder2(b);", indent, indent, first.getName());
                s.format("\n%s}", indent);
                // Builder
                s.format("\n\n%sstatic final class Builder {", indent);
                writeBuilderFields(s, indent, vars);
                s.format("\n%s}", indent);
            }
            int i = 2;
            while (it.hasNext()) {
                VariableDeclarator v = it.next();
                s.format("\n\n%spublic static final class Builder%s {", indent, i);
                s.format("\n\n%s%sprivate final Builder b;", indent, indent);
                s.format("\n\n%s%sBuilder%s(Builder b) {", indent, indent, i);
                s.format("\n%s%s%sthis.b = b;", indent, indent, indent);
                s.format("\n%s%s}", indent, indent);
                if (i < vars.size()) {
                    s.format("\n\n%s%spublic Builder%s %s(%s %s) {", indent, indent, i + 1, v.getName(), v.getType(),
                            v.getName());
                    s.format("\n%s%s%sb.%s = %s;", indent, indent, indent, v.getName(), v.getName());
                    s.format("\n%s%s%sreturn new Builder%s(b);", indent, indent, indent, i + 1);
                    s.format("\n%s%s}", indent, indent);
                    s.format("\n%s}", indent);
                } else {
                    s.format("\n\n%s%spublic %s %s(%s %s) {", indent, indent, c.getName(), v.getName(), v.getType(),
                            v.getName());
                    s.format("\n%s%s%sb.%s = %s;", indent, indent, indent, v.getName(), v.getName());
                    s.format("\n%s%s%sreturn new %s(%s);", indent, indent, indent, c.getName(), //
                            vars.stream().map(x -> "b." + x.getName().toString()).collect(Collectors.joining(", ")));
                    s.format("\n%s%s}", indent, indent);
                    s.format("\n%s}", indent);
                }
                i++;
            }
        }
    }

    private static void writeCreateMethod(PrintStream s, String indent, ClassOrInterfaceDeclaration c,
            List<VariableDeclarator> vars) {
        if (!GENERATE_CREATE_METHOD)
            return;
        s.format("\n\n%spublic static %s create(%s) {", indent, c.getName(), //
                vars.stream() //
                        .map(x -> x.getType() + " " + x.getName()) //
                        .collect(Collectors.joining(", ")));
        s.format("\n%s%sreturn new %s(%s);", indent, indent, c.getName(), //
                vars.stream() //
                        .map(x -> x.getName().toString()) //
                        .collect(Collectors.joining(", ")));
        s.format("\n%s}", indent);
    }

    private static void writeGetters(PrintStream s, String indent, List<VariableDeclarator> vars) {
        // getters
        vars.stream() //
                .forEach(x -> {
                    s.format("\n\n%spublic %s %s() {", indent, x.getType(), x.getName());
                    s.format("\n%s%sreturn %s;", indent, indent, x.getName());
                    s.format("\n%s}", indent);
                });
    }

    private static String insertImports(ByteArrayOutputStream bytes, Map<String, String> imports) {
        String s2 = new String(bytes.toByteArray(), StandardCharsets.UTF_8);
        List<Entry<String, String>> sorted = new ArrayList<Entry<String, String>>();
        sorted.addAll(imports.entrySet());
        Collections.sort(sorted, (a, b) -> a.getValue().compareTo(b.getValue()));
        s2 = s2.replace("<IMPORTS>", sorted.stream() //
                .filter(x -> !x.getKey().contains(".")) //
                .filter(x -> !x.getValue().startsWith("java.lang.")) //
                .map(x -> "import " + x.getValue() + ";") //
                .collect(Collectors.joining("\n")));
        return s2;
    }

    private static void writeToString(PrintStream s, Map<String, String> imports, String indent,
            ClassOrInterfaceDeclaration c, List<VariableDeclarator> vars) {
        s.format("\n\n%s@%s", indent, resolve(imports, Override.class));
        s.format("\n%spublic %s toString() {", indent, resolve(imports, String.class));
        s.format("\n%s%s%s b = new %s();", indent, indent, resolve(imports, StringBuilder.class),
                resolve(imports, StringBuilder.class));
        s.format("\n%s%sb.append(\"%s[\");", indent, indent, c.getName());
        String ex = vars.stream() //
                .map(x -> String.format("\n%s%sb.append(\"%s=\" + this.%s);", indent, indent, x.getName(), x.getName())) //
                .collect(Collectors.joining(String.format("\n%s%sb.append(\",\");", indent, indent)));
        s.format("%s", ex);
        s.format("\n%s%sb.append(\"]\");", indent, indent);
        s.format("\n%s%sreturn b.toString();", indent, indent);
        s.format("\n%s}", indent);
    }

    private static void writeEquals(PrintStream s, Map<String, String> imports, String indent,
            ClassOrInterfaceDeclaration c, List<VariableDeclarator> vars) {
        s.format("\n\n%s@%s", indent, resolve(imports, Override.class));
        s.format("\n%spublic boolean equals(Object o) {", indent);
        s.format("\n%s%sif (o == null) {", indent, indent);
        s.format("\n%s%s%sreturn false;", indent, indent, indent);
        s.format("\n%s%s} else if (!(o instanceof %s)) {", indent, indent, c.getName());
        s.format("\n%s%s%sreturn false;", indent, indent, indent);
        s.format("\n%s%s} else {", indent, indent);
        if (vars.isEmpty()) {
            s.format("\n%s%s%sreturn true;", indent, indent, indent);
        } else {
            s.format("\n%s%s%s%s other = (%s) o;", indent, indent, indent, c.getName(), c.getName());
            s.format("\n%s%s%sreturn", indent, indent, indent);
            String expression = vars.stream() ///
                    .map(x -> String.format("%s.deepEquals(this.%s, other.%s)", //
                            resolve(imports, Objects.class), x.getName(), x.getName())) //
                    .collect(Collectors.joining(String.format("\n%s%s%s%s&& ", indent, indent, indent, indent)));
            s.format("\n%s%s%s%s%s;", indent, indent, indent, indent, expression);
        }
        s.format("\n%s%s}", indent, indent);
        s.format("\n%s}", indent);
    }

    private static void writeHashCode(PrintStream s, Map<String, String> imports, String indent,
            List<VariableDeclarator> vars) {
        s.format("\n\n%s@%s", indent, resolve(imports, Override.class));
        s.format("\n%spublic int hashCode() {", indent);
        s.format("\n%s%sreturn %s.hash(%s);", indent, indent, resolve(imports, Objects.class), //
                vars.stream() //
                        .map(y -> y.getName().toString()) //
                        .collect(Collectors.joining(", ")));
        s.format("\n%s}", indent);
    }

    private static void writeWiths(PrintStream s, String indent, ClassOrInterfaceDeclaration c,
            List<VariableDeclarator> vars) {
        vars.stream() //
                .forEach(x -> {
                    s.format("\n\n%spublic %s with%s(%s %s) {", indent, c.getName(), capFirst(x.getName().toString()),
                            x.getType(), x.getName());
                    s.format("\n%s%sreturn new %s(%s);", indent, indent, c.getName(), //
                            vars.stream() //
                                    .map(y -> y.getName().toString()) //
                                    .collect(Collectors.joining(", ")));
                    s.format("\n%s}", indent);
                });
    }

    private static void writeConstructor(PrintStream s, String indent, ClassOrInterfaceDeclaration c,
            List<FieldDeclaration> fields, List<VariableDeclarator> vars, Map<String, String> imports) {
        String typedParams = fields.stream() //
                .map(x -> declaration(x, imports)) //
                .collect(Collectors.joining(String.format(",\n%s  ", indent)));
        s.format("\n\n%s@%s", indent, resolve(imports, JsonCreator.class));
        s.format("\n%s%s(\n%s%s%s) {", indent, c.getName(), indent, "  ", typedParams);
        vars.stream() //
                .forEach(x -> s.format("\n%s%sthis.%s = %s;", indent, indent, x.getName(), x.getName()));
        s.format("\n%s}", indent);
    }

    private static void writeFields(PrintStream s, String indent, List<FieldDeclaration> fields) {
        String flds = fields.stream() //
                .map(x -> fieldDeclaration(indent, x)) //
                .collect(Collectors.joining());
        s.append(flds);
    }

    private static String fieldDeclaration(String indent, FieldDeclaration x) {
        StringBuilder s = new StringBuilder();
        for (Node n : x.getChildNodes()) {
            if (n instanceof VariableDeclarator) {
                VariableDeclarator v = (VariableDeclarator) n;
                s.append(NL + indent + "private final " + v.getType() + " " + v.getName() + ";");
            } else if (n instanceof MarkerAnnotationExpr) {
                s.append(NL + indent + n);
            }
        }
        return s.toString();
    }

    private static void writeBuilderFields(PrintStream s, String indent, List<VariableDeclarator> vars) {
        String flds = vars.stream() //
                .map(x -> NL + indent + indent + x.getType() + " " + x.getName() + ";") //
                .collect(Collectors.joining());
        s.append(flds);
    }

    private static void writeClassDeclaration(PrintStream s, ClassOrInterfaceDeclaration c) {
        s.format("\npublic%s class %s", c.isFinal() ? " final" : "", c.getName());
        if (c.getImplementedTypes() != null && !c.getImplementedTypes().isEmpty()) {
            s.format(" implements");
            for (ClassOrInterfaceType iface : c.getImplementedTypes()) {
                s.append(" " + iface);
            }
        }
        s.append(" {\n");
        Preconditions.checkArgument(c.getExtendedTypes().size() == 0);
    }

    private static String resolve(Map<String, String> imports, Class<?> cls) {
        return resolve(imports, cls.getName());
    }

    private static String resolve(Map<String, String> imports, String className) {
        String simple = simpleName(className);
        for (Entry<String, String> entry : imports.entrySet()) {
            if (entry.getValue().equals(className)) {
                return entry.getKey();
            } else if (entry.getKey().equals(simple)) {
                return className;
            }
        }
        imports.put(simple, className);
        return simple;
    }

    private static String simpleName(String s) {
        int i = s.lastIndexOf('.');
        if (i == -1) {
            return s;
        } else {
            return s.substring(i + 1);
        }
    }

    private static VariableDeclarator variableDeclarator(FieldDeclaration f) {
        for (Node node : f.getChildNodes()) {
            if (node instanceof VariableDeclarator) {
                return (VariableDeclarator) node;
            }
        }
        throw new RuntimeException("declaration not found!");
    }

    private static String declaration(FieldDeclaration f, Map<String, String> imports) {
        VariableDeclarator v = variableDeclarator(f);
        return String.format("@%s(\"%s\") %s %s", resolve(imports, JsonProperty.class), v.getName(), v.getType(),
                v.getName());
    }

    private static String capFirst(String name) {
        if (name.length() <= 1) {
            return name.toUpperCase();
        } else {
            return name.substring(0, 1).toUpperCase() + name.substring(1);
        }
    }

    public static void scanAndGenerate(File directory, File generatedSourceDirectory) {
        for (File file : directory.listFiles()) {
            if (file.isDirectory()) {
                scanAndGenerate(file, generatedSourceDirectory);
            } else if (file.getName().endsWith(".java")) {
                try {
                    String code = new String(Files.readAllBytes(file.toPath()), StandardCharsets.UTF_8);
                    if (code.contains(GenerateImmutable.class.getName())) {
                        generate(code, generatedSourceDirectory);
                    }
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }
        }
    }

}
