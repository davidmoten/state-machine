package com.github.davidmoten.bean;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
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
import com.github.davidmoten.bean.annotation.NonNull;
import com.github.davidmoten.guavamini.Preconditions;
import com.github.javaparser.JavaParser;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.ImportDeclaration;
import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.NodeList;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.body.FieldDeclaration;
import com.github.javaparser.ast.body.VariableDeclarator;
import com.github.javaparser.ast.expr.MarkerAnnotationExpr;
import com.github.javaparser.ast.type.ClassOrInterfaceType;

public final class BeanGenerator {

    private static final String NL = "\n";

    public static void generate(String code, String newPkg, File generatedSource) {
        CompilationUnit cu = JavaParser.parse(code);
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        PrintStream s = new PrintStream(bytes);

        s.format("package %s.immutable;\n\n",
                cu.getPackageDeclaration().map(p -> p.getName().toString()).orElse("immutable"));
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

        {
            for (Node n : cu.getChildNodes()) {
                if (n instanceof ClassOrInterfaceDeclaration) {
                    ClassOrInterfaceDeclaration c = (ClassOrInterfaceDeclaration) n;
                    writeClassDeclaration(s, c);

                    List<FieldDeclaration> fields = c.getChildNodes() //
                            .stream() //
                            .filter(x -> x instanceof FieldDeclaration) //
                            .map(x -> (FieldDeclaration) x) //
                            .collect(Collectors.toList());

                    List<VariableDeclarator> vars = fields.stream() //
                            .map(x -> variableDeclarator(x)) //
                            .collect(Collectors.toList());

                    // fields
                    writeFields(s, indent, fields);

                    // constructor
                    writeConstructor(s, indent, c, fields, vars, imports);

                    // getters
                    writeGetters(s, indent, vars);

                    // with
                    writeWiths(s, indent, c, vars);

                    // builder
                    if (!vars.isEmpty()) {
                        Iterator<VariableDeclarator> it = vars.iterator();
                        s.format("\n\n%s// Constructor synchronized builder pattern.", indent);
                        s.format("\n%s// Changing the parameter list in the source", indent);
                        s.format("\n%s// and regenerating will provoke compiler errors", indent);
                        s.format("\n%s// wherever the builder is used.", indent);

                        VariableDeclarator first = it.next();
                        if (vars.size() == 1) {
                            s.format("\n\n%spublic static %s %s(%s %s) {", indent, c.getName(), first.getName(),
                                    first.getType(), first.getName());
                            s.format("\n%s%sreturn new %s(%s);", indent, indent, c.getName(), first.getName());
                            s.format("\n%s}", indent);
                        } else {
                            s.format("\n\n%spublic static Builder2 %s(%s %s) {", indent, first.getName(),
                                    first.getType(), first.getName());
                            s.format("\n%s%sBuilder b = new Builder();", indent, indent);
                            s.format("\n%s%sb.%s = %s;", indent, indent, first.getName(), first.getName());
                            s.format("\n%s%sreturn new Builder2(b);", indent, indent, first.getName());
                            s.format("\n%s}", indent);
                            s.format("\n\n%sprivate static final class Builder {", indent);
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
                                s.format("\n\n%s%spublic Builder%s %s(%s %s) {", indent, indent, i + 1, v.getName(),
                                        v.getType(), v.getName());
                                s.format("\n%s%s%sb.%s = %s;", indent, indent, indent, v.getName(), v.getName());
                                s.format("\n%s%s%sreturn new Builder%s(b);", indent, indent, indent, i + 1);
                                s.format("\n%s%s}", indent, indent);
                                s.format("\n%s}", indent);
                            } else {
                                s.format("\n\n%s%spublic %s %s(%s %s) {", indent, indent, c.getName(), v.getName(),
                                        v.getType(), v.getName());
                                s.format("\n%s%s%sb.%s = %s;", indent, indent, indent, v.getName(), v.getName());
                                s.format("\n%s%s%sreturn new %s(%s);", indent, indent, indent, c.getName(), //
                                        vars.stream().map(x -> "b." + x.getName().toString())
                                                .collect(Collectors.joining(", ")));
                                s.format("\n%s%s}", indent, indent);
                                s.format("\n%s}", indent);
                            }
                            i++;
                        }
                    }

                    // hashCode
                    writeHashCode(s, imports, indent, vars);

                    // equals
                    writeEquals(s, imports, indent, c, vars);

                    // toString
                    writeToString(s, imports, indent, c, vars);

                    s.append("\n}");
                    break;
                }
            }
        }
        // imports
        System.out.println(insertImports(bytes, imports));
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
        s.format("\n\n%s@%s", indent, resolve2(imports, Override.class));
        s.format("\n%spublic %s toString() {", indent, resolve2(imports, String.class));
        s.format("\n%s%s%s b = new %s();", indent, indent, resolve2(imports, StringBuilder.class),
                resolve2(imports, StringBuilder.class));
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
        s.format("\n\n%s@%s", indent, resolve2(imports, Override.class));
        s.format("\n%spublic boolean equals(Object o) {", indent);
        s.format("\n%s%sif (o == null) {", indent, indent);
        s.format("\n%s%s%sreturn false;", indent, indent, indent);
        s.format("\n%s%s} else if (!(o instanceof %s)) {", indent, indent, c.getName());
        s.format("\n%s%s%sreturn false;", indent, indent, indent);
        s.format("\n%s%s} else {", indent, indent);
        s.format("\n%s%s%s%s other = (%s) o;", indent, indent, indent, c.getName(), c.getName());
        s.format("\n%s%s%sreturn", indent, indent, indent);
        String expression = vars.stream() ///
                .map(x -> String.format("%s.deepEquals(this.%s, other.%s)", //
                        resolve2(imports, Objects.class), x.getName(), x.getName())) //
                .collect(Collectors.joining(String.format("\n%s%s%s%s&& ", indent, indent, indent, indent)));
        s.format("\n%s%s%s%s%s;", indent, indent, indent, indent, expression);
        s.format("\n%s%s}", indent, indent);
        s.format("\n%s}", indent);
    }

    private static void writeHashCode(PrintStream s, Map<String, String> imports, String indent,
            List<VariableDeclarator> vars) {
        s.format("\n\n%s@%s", indent, resolve2(imports, Override.class));
        s.format("\n%spublic int hashCode() {", indent);
        s.format("\n%s%sreturn %s.hash(%s);", indent, indent, resolve2(imports, Objects.class), //
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
        s.format("\n\n%s@%s", indent, resolve2(imports, JsonCreator.class));
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
        s.format("\npublic class %s", c.getName());
        if (c.getImplementedTypes() != null) {
            s.format(" implements");
            for (ClassOrInterfaceType iface : c.getImplementedTypes()) {
                s.append(" " + iface);
            }
        }
        s.append(" {\n");
        Preconditions.checkArgument(c.getExtendedTypes().size() == 0);
    }

    private static String resolve2(Map<String, String> imports, Class<?> cls) {
        return resolve2(imports, cls.getName());
    }

    private static String resolve2(Map<String, String> imports, String className) {
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
        return String.format("@%s(\"%s\") %s %s", resolve2(imports, JsonProperty.class), v.getName(), v.getType(),
                v.getName());
    }

    /**
     * @param cls
     * @param generatedSource
     */
    public static void generate(Class<?> cls, String newPkg, File generatedSource) {
        System.out.format("generating immutable bean for %s, newPkg=%s, genSrc=%s", cls, newPkg, generatedSource);
        String className = cls.getSimpleName();
        String path = newPkg.replace(".", File.separator);
        File directory = new File(generatedSource, path);
        directory.mkdirs();
        File file = new File(directory, className + ".java");
        Map<Class<?>, String> imports = new HashMap<>();
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        PrintStream out = new PrintStream(bytes);
        out.format("/////////////////////////////////////////////////////\n" + //
                "// WARNING - Generated data class! \n" //
                + "/////////////////////////////////////////////////////\n");
        out.println();
        // out.format("@%s\n", resolve(imports, ImmutableBean.class));
        out.format("public class %s {\n", className);
        out.println();

        List<Field> fields = Arrays //
                .stream(cls.getDeclaredFields()) //
                .filter(c -> !c.getName().startsWith("$")) //
                .collect(Collectors.toList());

        for (Field field : fields) {
            String declaration = getDeclaration(field, imports);
            Class<?> type = field.getType();
            String name = field.getName();
            for (Annotation a : field.getAnnotations()) {
                System.out.println(a);
                if (a.annotationType().equals(NonNull.class)) {
                    out.format("    @%s\n", resolve(imports, NonNull.class));
                }
            }
            out.format("    private final %s %s;\n", resolve(imports, type), name);
        }

        // constructor params
        StringBuffer params = new StringBuffer();
        for (Field field : fields) {
            Class<?> type = field.getType();
            String name = field.getName();
            if (params.length() > 0) {
                params.append(", ");
            }
            params.append(String.format("\n          @%s(\"%s\") ", resolve(imports, JsonProperty.class), name));
            params.append(resolve(imports, type) + " " + name);
        }

        // constructor
        out.println();
        out.format("    @%s\n", resolve(imports, JsonCreator.class));
        out.format("    public %s(%s) {\n", cls.getSimpleName(), params);

        // TODO make checkForNull configurable
        boolean checkForNull = false;
        if (checkForNull) {
            for (Field field : fields) {
                String name = field.getName();
                Class<?> type = field.getType();
                if ((!type.isPrimitive() || type.isArray()) && isNonNull(type.getAnnotations())) {
                    out.format("        if (%s == null) {\n", name);
                    out.format("            throw new %s(\"'%s' parameter cannot be null\");\n",
                            resolve(imports, NullPointerException.class), name);
                    out.format("        }\n");
                }
            }
        }
        for (Field field : fields) {
            String name = field.getName();
            out.format("        this.%s = %s;\n", name, name);
        }
        out.format("    }\n");

        // build comma delimited params
        StringBuffer flds = new StringBuffer();
        for (Field field : fields) {
            String name = field.getName();
            if (flds.length() > 0) {
                flds.append(", ");
            }
            flds.append(name);
        }

        // static factory
        // build create method params
        StringBuffer params2 = new StringBuffer();
        for (Field field : fields) {
            Class<?> type = field.getType();
            String name = field.getName();
            if (params2.length() > 0) {
                params2.append(", ");
            }
            params2.append(String.format("%s %s", resolve(imports, type), name));
        }
        out.println();
        out.format("    public static %s create(%s) {\n", className, params2);
        out.format("        return new %s(%s);\n", className, flds);
        out.format("    }");
        out.println();

        // getters
        for (Field field : fields) {
            Class<?> type = field.getType();
            String name = field.getName();
            out.println();
            out.format("    public %s %s() {\n", resolve(imports, type), name);
            out.format("        return %s;\n", name);
            out.format("    }\n");
        }

        // with fields
        for (Field field : fields) {
            Class<?> type = field.getType();
            String name = field.getName();
            out.println();
            out.format("    public %s with%s(%s %s) {\n", className, capFirst(name), resolve(imports, type), name);
            out.format("        return new %s(%s);\n", className, flds);
            out.format("    }\n");
        }

        // builder

        if (!fields.isEmpty()) {
            out.println();
            Field f = fields.get(0);
            if (fields.size() > 1) {
                out.format("    public static Builder2 %s(%s %s) {\n", f.getName(), resolve(imports, f.getType()),
                        f.getName());
                out.format("        return new Builder1(new Builder()).%s(%s);\n", f.getName(), f.getName());
                out.format("    }\n\n");
            } else {
                out.format("    public static %s %s(%s %s) {\n", className, f.getName(), resolve(imports, f.getType()),
                        f.getName());
                out.format("        return new Builder1(new Builder()).%s(%s);\n", f.getName(), f.getName());
                out.format("    }\n\n");
            }
            out.format("    private static final class Builder {\n");
            for (Field field : fields) {
                Class<?> type = field.getType();
                String name = field.getName();
                out.format("        %s %s;\n", resolve(imports, type), name);
            }
            out.println();
            out.format("        %s create() {\n", className);
            out.format("            return new %s(%s);\n", className, flds);
            out.format("        }\n");
            out.format("    }\n");
        }

        for (int i = 1; i <= fields.size(); i++) {
            Field field = fields.get(i - 1);
            Class<?> type = field.getType();
            String name = field.getName();
            out.println();
            out.format("    public static final class Builder%s {\n", i);
            out.format("        final Builder b;\n\n");
            out.format("        Builder%s(Builder b) {\n", i);
            out.format("            this.b = b;\n");
            out.format("        }\n\n");
            if (i == fields.size()) {
                out.format("        public %s %s(%s %s) {\n", className, name, resolve(imports, type), name);
                out.format("            this.b.%s = %s;\n", name, name);

                out.format("            return b.create();\n");
                out.format("        }\n");
            } else {
                out.format("        public Builder%s %s(%s %s) {\n", //
                        i + 1, name, resolve(imports, type), name);
                out.format("            this.b.%s = %s;\n", name, name);
                out.format("            return new Builder%s(b);\n", i + 1);
                out.format("        }\n");
            }
            out.format("    }\n");
        }

        // TODO toString

        // hashCode
        out.println();
        out.format("    @%s\n", resolve(imports, Override.class));
        out.format("    public int hashCode() {\n");
        out.format("        return %s.hash(%s);\n", resolve(imports, Objects.class), flds);
        out.format("    }\n");

        // equals
        out.println();
        out.format("    @%s\n", resolve(imports, Override.class));
        out.format("    public boolean equals(Object o) {\n");
        out.format("        return %s.equals(this, o);\n", resolve(imports, Objects.class));
        out.format("    }\n");

        out.println("}\n");
        out.close();

        // package
        StringBuffer w = new StringBuffer();
        w.append("package " + newPkg + ";\n");
        w.append("\n");

        // imports
        for (Entry<Class<?>, String> entry : imports //
                .entrySet() //
                .stream() //
                .sorted((a, b) -> a.getKey().getName().compareTo(b.getKey().getName())) //
                .collect(Collectors.toList())) {
            Class<?> c = entry.getKey();
            if (!c.isPrimitive() && !c.getName().startsWith("java.lang.")) {
                w.append("import " + c.getName() + ";\n");
            }
        }
        w.append("\n");

        try (FileOutputStream fos = new FileOutputStream(file)) {
            fos.write(w.toString().getBytes(StandardCharsets.UTF_8));
            fos.write(bytes.toByteArray());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        System.out.println("written generated data class " + file);
    }

    private static String getDeclaration(Field field, Map<Class<?>, String> imports) {
        StringBuilder s = new StringBuilder();
        return null;
    }

    private static boolean isNonNull(Annotation[] annotations) {
        for (Annotation a : annotations) {
            if (a.annotationType().equals(NonNull.class)) {
                return true;
            }
        }
        return false;
    }

    private static String capFirst(String name) {
        if (name.length() <= 1) {
            return name.toUpperCase();
        } else {
            return name.substring(0, 1).toUpperCase() + name.substring(1);
        }
    }

    public static String resolve(Map<Class<?>, String> map, Class<?> cls) {
        Class<?> c;
        if (cls.isArray()) {
            c = cls.getComponentType();
        } else {
            c = cls;
        }
        final String name;
        if (map.containsKey(c)) {
            name = map.get(c);
        } else {
            if (map.values().contains(c.getSimpleName())) {
                map.put(c, c.getName());
                name = c.getName();
            } else {
                map.put(c, c.getSimpleName());
                name = c.getSimpleName();
            }
        }
        if (cls.isArray()) {
            return name + "[]";
        } else {
            return name;
        }
    }

}
