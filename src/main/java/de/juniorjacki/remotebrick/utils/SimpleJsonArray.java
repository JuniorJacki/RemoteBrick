package de.juniorjacki.remotebrick.utils;

import java.util.ArrayList;
import java.util.List;

public class SimpleJsonArray {
    final List<Object> list = new ArrayList<>();

    // --- Konstruktor nur für Parser ---
    SimpleJsonArray() {}

    public int length() { return list.size(); }

    // ===================== SICHERE OPT-METHODEN =====================

    /** Gibt Object oder null zurück – nie Exception */
    public Object opt(int index) {
        return (index >= 0 && index < list.size()) ? list.get(index) : null;
    }

    public String optString(int index) { return optString(index, ""); }
    public String optString(int index, String fallback) {
        Object val = opt(index);
        if (val == null) return fallback;
        if (val instanceof String s) return s;
        return val.toString(); // sicher: alles hat toString()
    }

    public int optInt(int index) { return optInt(index, 0); }
    public int optInt(int index, int fallback) {
        Object val = opt(index);
        if (val == null) return fallback;
        if (val instanceof Number n) return n.intValue();
        if (val instanceof String s) {
            try { return Integer.parseInt(s.trim()); }
            catch (NumberFormatException e) { return fallback; }
        }
        if (val instanceof Boolean b) return b ? 1 : 0;
        return fallback;
    }

    public long optLong(int index) { return optLong(index, 0L); }
    public long optLong(int index, long fallback) {
        Object val = opt(index);
        if (val == null) return fallback;
        if (val instanceof Number n) return n.longValue();
        if (val instanceof String s) {
            try { return Long.parseLong(s.trim()); }
            catch (NumberFormatException e) { return fallback; }
        }
        if (val instanceof Boolean b) return b ? 1L : 0L;
        return fallback;
    }

    public double optDouble(int index) { return optDouble(index, 0.0); }
    public double optDouble(int index, double fallback) {
        Object val = opt(index);
        if (val == null) return fallback;
        if (val instanceof Number n) return n.doubleValue();
        if (val instanceof String s) {
            try { return Double.parseDouble(s.trim()); }
            catch (NumberFormatException e) { return fallback; }
        }
        if (val instanceof Boolean b) return b ? 1.0 : 0.0;
        return fallback;
    }

    public boolean optBoolean(int index) { return optBoolean(index, false); }
    public boolean optBoolean(int index, boolean fallback) {
        Object val = opt(index);
        if (val == null) return fallback;
        if (val instanceof Boolean b) return b;
        if (val instanceof Number n) return n.doubleValue() != 0.0;
        if (val instanceof String s) {
            String lower = s.trim().toLowerCase();
            if ("true".equals(lower) || "1".equals(lower)) return true;
            if ("false".equals(lower) || "0".equals(lower)) return false;
        }
        return fallback;
    }

    public SimpleJson optJSONObject(int index) {
        Object val = opt(index);
        return val instanceof SimpleJson ? (SimpleJson) val : null;
    }

    public SimpleJsonArray optJSONArray(int index) {
        Object val = opt(index);
        return val instanceof SimpleJsonArray ? (SimpleJsonArray) val : null;
    }

    // ===================== UNSICHERE GET-METHODEN (nur für sichere Fälle) =====================

    public String getString(int index) {
        Object val = get(index);
        return val.toString();
    }

    public int getInt(int index) {
        return toInt(get(index));
    }

    public long getLong(int index) {
        return toLong(get(index));
    }

    public double getDouble(int index) {
        return toDouble(get(index));
    }

    public boolean getBoolean(int index) {
        return toBoolean(get(index));
    }

    public SimpleJson getJSONObject(int index) {
        Object val = get(index);
        if (!(val instanceof SimpleJson))
            throw new IllegalArgumentException("Not a JSON object at index " + index);
        return (SimpleJson) val;
    }

    public SimpleJsonArray getJSONArray(int index) {
        Object val = get(index);
        if (!(val instanceof SimpleJsonArray))
            throw new IllegalArgumentException("Not a JSON array at index " + index);
        return (SimpleJsonArray) val;
    }

    private Object get(int index) {
        if (index < 0 || index >= list.size())
            throw new IndexOutOfBoundsException("Index: " + index + ", Size: " + list.size());
        return list.get(index);
    }

    // ===================== INTERNE KONVERTIERUNGEN (nur für getX()) =====================

    private int toInt(Object val) {
        if (val instanceof Number n) return n.intValue();
        return Integer.parseInt(val.toString());
    }

    private long toLong(Object val) {
        if (val instanceof Number n) return n.longValue();
        return Long.parseLong(val.toString());
    }

    private double toDouble(Object val) {
        if (val instanceof Number n) return n.doubleValue();
        return Double.parseDouble(val.toString());
    }

    private boolean toBoolean(Object val) {
        if (val instanceof Boolean b) return b;
        String s = val.toString().toLowerCase();
        return "true".equals(s) || "1".equals(s);
    }

    @Override
    public String toString() {
        return list.toString();
    }
}