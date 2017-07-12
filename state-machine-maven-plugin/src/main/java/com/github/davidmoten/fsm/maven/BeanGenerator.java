package com.github.davidmoten.fsm.maven;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintStream;
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
import com.fasterxml.jackson.annotation.JsonProperty;;

public final class BeanGenerator {

    public void generate(Class<?> cls, File generatedSource) {
        String pkg = cls.getPackage().getName();
        String pkg2 = pkg + ".bean";
        String className = cls.getSimpleName();
        String path = pkg2.replace(".", File.separator);
        File directory = new File(generatedSource, path);
        directory.mkdirs();
        File file = new File(directory, className + ".java");
        Map<Class<?>, String> imports = new HashMap<>();
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        PrintStream out = new PrintStream(bytes);
        out.format("public class %s {\n", className);
        out.println();

        List<Field> fields = Arrays.stream(cls.getDeclaredFields()).filter(c -> !c.getName().startsWith("$"))
                .collect(Collectors.toList());

        for (Field field : fields) {
            Class<?> type = field.getType();
            String name = field.getName();
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
        for (Field field : fields) {
            String name = field.getName();
            out.format("        this.%s = %s;\n", name, name);
        }
        out.format("    }\n");

        // getters
        for (Field field : fields) {
            Class<?> type = field.getType();
            String name = field.getName();
            out.println();
            out.format("    public %s %s() {\n", resolve(imports, type), name);
            out.format("        return %s;\n", name);
            out.format("    }\n");
        }

        // hashCode
        StringBuffer flds = new StringBuffer();
        for (Field field : cls.getDeclaredFields()) {
            String name = field.getName();
            if (flds.length() > 0) {
                flds.append(", ");
            }
            flds.append(name);
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

        out.println();
        out.format("    @%s\n", resolve(imports, Override.class));
        out.format("    public int hashCode() {\n");
        out.format("        return %s.hashCode(%s);\n", resolve(imports, Objects.class), flds);
        out.format("    }\n");

        out.println();
        out.format("    @%s\n", resolve(imports, Override.class));
        out.format("    public boolean equals(Object o) {\n");
        out.format("        return %s.equals(this, o);\n", resolve(imports, Objects.class));
        out.format("    }\n");

        out.println("}\n");
        out.close();

        StringBuffer w = new StringBuffer();
        w.append("package " + pkg2 + ";\n");
        w.append("\n");

        for (Entry<Class<?>, String> entry : imports.entrySet()) {
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
    }

    private static String capFirst(String name) {
        if (name.length() <= 1) {
            return name.toUpperCase();
        } else {
            return name.substring(0, 1).toUpperCase() + name.substring(1);
        }
    }

    public String resolve(Map<Class<?>, String> map, Class<?> cls) {
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
