package de.juniorjacki.remotebrick.utils;

import java.util.SortedMap;

public class JsonBuilder {
    private final java.util.Map<String, Object> data = new  java.util.LinkedHashMap<>();

    public JsonBuilder add(String key, Object value) {
        data.put(key, value);
        return this;
    }

    public JsonBuilder addObject(String key, JsonBuilder builder) {
        data.put(key, builder.data);
        return this;
    }

    public JsonBuilder addArray(String key, Object... values) {
        data.put(key, java.util.Arrays.asList(values));
        return this;
    }

    public JsonBuilder addList(String key, java.util.List<?> list) {
        data.put(key, list);
        return this;
    }

    public JsonBuilder addMap(String key, java.util.Map<String, ?> map) {
        data.put(key, map);
        return this;
    }

    public JsonBuilder add(JsonBuilder builder) {
        data.putAll(builder.data);
        return this;
    }

    public static JsonBuilder array(Object... values) {
        JsonBuilder jb = new JsonBuilder();
        jb.data.put("__array__", java.util.Arrays.asList(values));
        return jb;
    }


    @Override
    public String toString() {
        if (data.containsKey("__array__")) {
            return arrayToJson(data.get("__array__"));
        }
        return mapToJson(data);
    }

    private String mapToJson(java.util.Map<?, ?> map) {
        if (map.isEmpty()) return "{}";
        StringBuilder sb = new StringBuilder("{");
        boolean first = true;
        for (var e : map.entrySet()) {
            if (!first) sb.append(",");
            sb.append("\"").append(escape(e.getKey().toString())).append("\":")
              .append(valueToJson(e.getValue()));
            first = false;
        }
        return sb.append("}").toString();
    }

    private String arrayToJson(Object arrayOrList) {
        java.util.List<Object> list = new java.util.ArrayList<>();

        if (arrayOrList instanceof java.util.List) {
            for (Object item : (java.util.List<?>) arrayOrList) {
                list.add(item);
            }
        } else if (arrayOrList.getClass().isArray()) {
            int len = java.lang.reflect.Array.getLength(arrayOrList);
            for (int i = 0; i < len; i++) {
                list.add(java.lang.reflect.Array.get(arrayOrList, i));
            }
        } else {
            return "[]";
        }

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

    private String valueToJson(Object value) {
        if (value == null) return "null";
        if (value instanceof String) return "\"" + escape((String) value) + "\"";
        if (value instanceof Number || value instanceof Boolean) return value.toString();
        if (value instanceof java.util.Map) return mapToJson((java.util.Map<?, ?>) value);
        if (value instanceof java.util.List) return arrayToJson(value);
        if (value.getClass().isArray()) return arrayToJson(value);
        if (value instanceof JsonBuilder) return ((JsonBuilder) value).data.toString(); // Rekursion
        return "\"" + escape(value.toString()) + "\""; // Fallback
    }

    private String escape(String s) {
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
        String json = new JsonBuilder().add("name", "Anna")
            .add("alter", 30)
            .add("aktiv", true)
            .add("nullWert", null)
            .addArray("hobbies", "Lesen", "Sport", 123)
            .addList("sprachen", java.util.Arrays.asList("DE", "EN"))
            .addMap("adresse", new java.util.HashMap<String, Object>() {{
                put("strasse", "Musterweg 1");
                put("plz", 12345);
            }})
            .addObject("kind", new JsonBuilder()
                .add("name", "Tom")
                .add("alter", 5)
            )
            .toString();

        System.out.println(json);
    }
}