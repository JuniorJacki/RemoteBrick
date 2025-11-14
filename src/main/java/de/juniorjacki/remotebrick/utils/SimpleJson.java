/*
 * Copyright (c) 2025 JuniorJacki
 * All Rights Reserved
 */

package de.juniorjacki.remotebrick.utils;

import java.util.LinkedHashMap;
import java.util.Map;

public class SimpleJson {
    final Map<String, Object> map = new LinkedHashMap<>();

    SimpleJson() {}

    public String getString(String key) {
        Object val = map.get(key);
        if (val == null) throw new IllegalArgumentException("Key not found: " + key);
        return val.toString();
    }

    public String optString(String key) { return optString(key, ""); }
    public String optString(String key, String fallback) {
        Object val = map.get(key);
        return val != null ? val.toString() : fallback;
    }

    public int getInt(String key) {
        Object val = map.get(key);
        if (val == null) throw new IllegalArgumentException("Key not found: " + key);
        return toInt(val);
    }

    public int optInt(String key) { return optInt(key, 0); }
    public int optInt(String key, int fallback) {
        try {
            Object val = map.get(key);
            return val != null ? toInt(val) : fallback;
        } catch (Exception e) {
            return fallback;
        }
    }

    public long getLong(String key) {
        Object val = map.get(key);
        if (val == null) throw new IllegalArgumentException("Key not found: " + key);
        return toLong(val);
    }

    public long optLong(String key) { return optLong(key, 0L); }
    public long optLong(String key, long fallback) {
        Object val = map.get(key);
        return val != null ? toLong(val) : fallback;
    }

    public double getDouble(String key) {
        Object val = map.get(key);
        if (val == null) throw new IllegalArgumentException("Key not found: " + key);
        return toDouble(val);
    }

    public double optDouble(String key) { return optDouble(key, 0.0); }
    public double optDouble(String key, double fallback) {
        Object val = map.get(key);
        return val != null ? toDouble(val) : fallback;
    }

    public boolean getBoolean(String key) {
        Object val = map.get(key);
        if (val == null) throw new IllegalArgumentException("Key not found: " + key);
        return toBoolean(val);
    }

    public boolean optBoolean(String key) { return optBoolean(key, false); }
    public boolean optBoolean(String key, boolean fallback) {
        Object val = map.get(key);
        return val != null ? toBoolean(val) : fallback;
    }

    public SimpleJson getJSONObject(String key) {
        Object val = map.get(key);
        if (!(val instanceof SimpleJson))
            throw new IllegalArgumentException("Not a JSON object: " + key);
        return (SimpleJson) val;
    }

    public SimpleJson optJSONObject(String key) {
        Object val = map.get(key);
        return val instanceof SimpleJson ? (SimpleJson) val : null;
    }

    public SimpleJsonArray  getJSONArray(String key) {
        Object val = map.get(key);
        if (!(val instanceof SimpleJsonArray))
            throw new IllegalArgumentException("Not a JSON array: " + key);
        return (SimpleJsonArray) val;
    }

    public SimpleJsonArray optJSONArray(String key) {
        Object val = map.get(key);
        return val instanceof SimpleJsonArray ? (SimpleJsonArray) val : null;
    }

    public boolean has(String key) {
        return map.containsKey(key);
    }

    public Object get(String key) {
        return map.get(key);
    }

    // --- Hilfsmethoden ---
    private int toInt(Object val) {
        if (val instanceof Number) return ((Number) val).intValue();
        return Integer.parseInt(val.toString());
    }

    private long toLong(Object val) {
        if (val instanceof Number) return ((Number) val).longValue();
        return Long.parseLong(val.toString());
    }

    private double toDouble(Object val) {
        if (val instanceof Number) return ((Number) val).doubleValue();
        return Double.parseDouble(val.toString());
    }

    private boolean toBoolean(Object val) {
        if (val instanceof Boolean) return (Boolean) val;
        String s = val.toString().toLowerCase();
        return "true".equals(s) || "1".equals(s);
    }

    @Override
    public String toString() {
        return map.toString();
    }
}