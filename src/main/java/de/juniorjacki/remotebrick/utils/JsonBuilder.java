package de.juniorjacki.remotebrick.utils;

import de.juniorjacki.remotebrick.types.Port;
import java.util.*;

public class JsonBuilder {

    // Kann entweder ein Objekt (Map) oder ein Array (List) sein
    private final Object root;
    private final boolean isArray;

    // --- Konstruktoren ---
    private JsonBuilder(boolean isArray) {
        this.isArray = isArray;
        this.root = isArray ? new ArrayList<>() : new LinkedHashMap<String, Object>();
    }

    // --- Factory-Methoden ---
    public static JsonBuilder object() {
        return new JsonBuilder(false);
    }

    public static JsonBuilder array() {
        return new JsonBuilder(true);
    }

    public static JsonBuilder array(Object... values) {
        JsonBuilder jb = new JsonBuilder(true);
        if (values != null) {
            jb.addAll(Arrays.asList(values));
        }
        return jb;
    }

    // --- Hinzufügen ---
    public JsonBuilder add(String key, Object value) {
        if (isArray) throw new IllegalStateException("Cannot add key to array");
        ((Map<String, Object>) root).put(key, value);
        return this;
    }

    public JsonBuilder add(Object value) {
        if (!isArray) throw new IllegalStateException("Cannot add value to object without key");
        ((List<Object>) root).add(value);
        return this;
    }

    public JsonBuilder addAll(Collection<?> values) {
        if (!isArray) throw new IllegalStateException("Cannot add collection to object");
        ((List<Object>) root).addAll(values);
        return this;
    }

    public static JsonBuilder arrayOfStrings(String... strings) {
        JsonBuilder arr = JsonBuilder.array();
        for (String s : strings) {
            arr.add(s);
        }
        return arr;
    }

    // === NEUE METHODE ===
    public JsonBuilder addAll(JsonBuilder other) {
        if (other == null || other.root == null) return this;

        if (this.isArray && other.isArray) {
            List<Object> thisList = (List<Object>) this.root;
            List<?> otherList = (List<?>) other.root;
            thisList.addAll(deepCopyList(otherList));
        }
        else if (!this.isArray && !other.isArray) {
            Map<String, Object> thisMap = (Map<String, Object>) this.root;
            Map<String, ?> otherMap = (Map<String, ?>) other.root;
            for (Map.Entry<String, ?> e : otherMap.entrySet()) {
                thisMap.put(e.getKey(), deepCopy(e.getValue()));
            }
        }
        else {
            throw new IllegalStateException("Cannot merge array with object");
        }

        return this;
    }

    // === HILFSMETHODEN ===
    private static Object deepCopy(Object value) {
        if (value == null) return null;
        if (value instanceof Number || value instanceof Boolean || value instanceof String) {
            return value;
        }
        if (value instanceof JsonBuilder jb) {
            return jb.isArray
                    ? JsonBuilder.array().addAll(jb)
                    : JsonBuilder.object().addAll(jb);
        }
        if (value instanceof Map) {
            JsonBuilder obj = JsonBuilder.object();
            for (Map.Entry<?, ?> e : ((Map<?, ?>) value).entrySet()) {
                obj.add(e.getKey().toString(), deepCopy(e.getValue()));
            }
            return obj;
        }
        if (value instanceof List) {
            return deepCopyList((List<?>) value);
        }
        if (value.getClass().isArray()) {
            JsonBuilder arr = JsonBuilder.array();
            int len = java.lang.reflect.Array.getLength(value);
            for (int i = 0; i < len; i++) {
                arr.add(deepCopy(java.lang.reflect.Array.get(value, i)));
            }
            return arr;
        }
        return value.toString();
    }

    private static List<Object> deepCopyList(List<?> list) {
        JsonBuilder arr = JsonBuilder.array();
        for (Object item : list) {
            arr.add(deepCopy(item));
        }
        return (List<Object>) arr.root;
    }

    public JsonBuilder addObject(String key, JsonBuilder builder) {
        return add(key, builder.buildMap());
    }

    public JsonBuilder addArray(String key, Object... values) {
        return add(key, Arrays.asList(values));
    }



    // --- Build ---
    private Map<String, Object> buildMap() {
        if (isArray) throw new IllegalStateException("Array cannot be used as map");
        return (Map<String, Object>) root;
    }

    // --- toString() ---
    @Override
    public String toString() {
        return isArray ? arrayToJson((List<?>) root) : mapToJson((Map<?, ?>) root);
    }

    // === JSON SERIALISIERUNG ===
    private static String mapToJson(Map<?, ?> map) {
        if (map.isEmpty()) return "{}";
        StringBuilder sb = new StringBuilder("{");
        boolean first = true;
        for (var e : map.entrySet()) {
            if (!first) sb.append(",");
            sb.append('"').append(escape(e.getKey().toString())).append("\":")
                    .append(valueToJson(e.getValue()));
            first = false;
        }
        return sb.append("}").toString();
    }

    private static String arrayToJson(List<?> list) {
        if (list.isEmpty()) return "[]";
        StringBuilder sb = new StringBuilder("[");
        boolean first = true;
        for (Object v : list) {
            if (!first) sb.append(",");
            sb.append(valueToJson(v));
            first = false;
        }
        return sb.append("]").toString();
    }

    private static String valueToJson(Object value) {
        if (value == null) return "null";
        if (value instanceof Port p) return "\"" + p.name() + "\"";
        if (value instanceof String s) return "\"" + escape(s) + "\"";
        if (value instanceof Number || value instanceof Boolean) return value.toString();
        if (value instanceof Map) return mapToJson((Map<?, ?>) value);
        if (value instanceof List) return arrayToJson((List<?>) value);
        if (value.getClass().isArray()) return arrayToJson(toList(value));
        if (value instanceof JsonBuilder jb) return jb.toString();
        return "\"" + escape(value.toString()) + "\"";
    }

    private static List<Object> toList(Object array) {
        List<Object> list = new ArrayList<>();
        int len = java.lang.reflect.Array.getLength(array);
        for (int i = 0; i < len; i++) {
            list.add(java.lang.reflect.Array.get(array, i));
        }
        return list;
    }

    private static String escape(String s) {
        return s.replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\b", "\\b")
                .replace("\f", "\\f")
                .replace("\n", "\\n")
                .replace("\r", "\\r")
                .replace("\t", "\\t");
    }

    // === TEST ===
    public static void main(String[] args) {
        String json = JsonBuilder.object()
                .add("name", "Anna")
                .add("alter", 30)
                .add("aktiv", true)
                .add("nullWert", null)
                .addArray("hobbies", "Lesen", "Sport", 123)
                .add("sprachen", JsonBuilder.array("DE", "EN"))
                .add("adresse", JsonBuilder.object()
                        .add("strasse", "Musterweg 1")
                        .add("plz", 12345)
                )
                .add("kind", JsonBuilder.object()
                        .add("name", "Tom")
                        .add("alter", 5)
                )
                .toString();

        System.out.println(json);
    }
}