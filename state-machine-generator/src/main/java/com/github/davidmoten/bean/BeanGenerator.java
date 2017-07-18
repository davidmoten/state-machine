package com.github.davidmoten.bean;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.stream.Collectors;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
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
import com.github.javaparser.ast.type.ClassOrInterfaceType;

public final class BeanGenerator {

    private static final String NL = "\n";

    public static void generate(String code, String newPkg, File generatedSource) {
        CompilationUnit cu = JavaParser.parse(code);
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        PrintStream s = new PrintStream(bytes);

        cu.getPackageDeclaration() //
                .ifPresent(p -> s.format(p.toString()));
        {
            NodeList<ImportDeclaration> n = cu.getImports();
            if (n != null) {
                for (ImportDeclaration node : n) {
                    s.append(node.toString());
                }
            }
        }
        String indent = "    ";

        // add placeholder for more imports
        // s.append("<IMPORTS>\n");
        {
            for (Node n : cu.getChildNodes()) {
                if (n instanceof ClassOrInterfaceDeclaration) {
                    ClassOrInterfaceDeclaration c = (ClassOrInterfaceDeclaration) n;
                    s.format("\npublic class %s", c.getName());
                    if (c.getImplementedTypes() != null) {
                        s.format(" implements");
                        for (ClassOrInterfaceType iface : c.getImplementedTypes()) {
                            s.append(" " + iface);
                        }
                    }
                    s.append(" {\n");
                    Preconditions.checkArgument(c.getExtendedTypes().size() == 0);
                    List<FieldDeclaration> fields = c.getChildNodes() //
                            .stream() //
                            .filter(x -> x instanceof FieldDeclaration) //
                            .map(x -> (FieldDeclaration) x) //
                            .collect(Collectors.toList());

                    // fields
                    String flds = fields.stream() //
                            .map(x -> variableDeclarator(x)) //
                            .map(x -> NL + indent + "private final " + x.getType() + " " + x.getName() + ";") //
                            .collect(Collectors.joining());
                    s.append(flds);

                    // constructor
                    String params = fields.stream() //
                            .map(x -> declaration(x)) //
                            .collect(Collectors.joining(", "));
                    s.format("\n\n%s%s(%s) {", indent, c.getName(), params);
                    fields.stream() //
                            .map(x -> variableDeclarator(x)) //
                            .forEach(x -> s.format("\n%s%sthis.%s = %s;", indent, indent, x.getName(), x.getName()));
                    s.format("\n%s}\n", indent);

                    // getters
                    fields.stream() //
                            .map(x -> variableDeclarator(x)) //
                            .forEach(x -> {
                                s.format("\n\n%spublic %s %s() {", indent, x.getType(), x.getName());
                                s.format("\n%s%sreturn %s;", indent, indent, x.getName());
                                s.format("\n%s}", indent);
                            });

                    s.append("\n}");

                    break;
                }
            }
        }
        //
        //
        System.out.println(new String(bytes.toByteArray(), StandardCharsets.UTF_8));
    }

    private static VariableDeclarator variableDeclarator(FieldDeclaration f) {
        for (Node node : f.getChildNodes()) {
            if (node instanceof VariableDeclarator) {
                return (VariableDeclarator) node;
            }
        }
        throw new RuntimeException("declaration not found!");
    }

    private static String declaration(FieldDeclaration f) {
        VariableDeclarator v = variableDeclarator(f);
        return v.getType() + " " + v.getName();
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
