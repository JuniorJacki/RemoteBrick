/*
 * Copyright (c) 2025 JuniorJacki
 * All Rights Reserved
 */

package de.juniorjacki.remotebrick.utils;

public class JsonParser {

    public static SimpleJson parseObject(String json) {
        return new Parser(json.trim()).parseObject();
    }

    public static SimpleJsonArray parseArray(String json) {
        return new Parser(json.trim()).parseArray();
    }

    private static class Parser {
        String s;
        int i = 0;

        Parser(String s) { this.s = s; }

        SimpleJson parseObject() {
            expect('{');
            SimpleJson obj = new SimpleJson();
            while (i < s.length()) {
                skip();
                if (s.charAt(i) == '}') { i++; break; }
                String key = parseString();
                skip(); expect(':'); skip();
                Object val = parseValue();
                obj.map.put(key, val);
                skip();
                if (s.charAt(i) == ',') i++;
                else if (s.charAt(i) == '}') { i++; break; }
            }
            return obj;
        }

        SimpleJsonArray parseArray() {
            expect('[');
            SimpleJsonArray arr = new SimpleJsonArray();
            while (i < s.length()) {
                skip();
                if (s.charAt(i) == ']') { i++; break; }
                arr.list.add(parseValue());
                skip();
                if (s.charAt(i) == ',') i++;
                else if (s.charAt(i) == ']') { i++; break; }
            }
            return arr;
        }

        Object parseValue() {
            skip();
            char c = s.charAt(i);
            if (c == '{') return parseObject();
            if (c == '[') return parseArray();
            if (c == '"') return parseString();
            if (c == 't' || c == 'f') return parseBoolean();
            if (c == 'n') return parseNull();
            return parseNumber();
        }

        String parseString() {
            expect('"');
            StringBuilder sb = new StringBuilder();
            while (i < s.length()) {
                char c = s.charAt(i++);
                if (c == '\\') {
                    c = s.charAt(i++);
                    sb.append(switch (c) {
                        case 'n' -> '\n'; case 't' -> '\t'; case 'r' -> '\r';
                        case 'b' -> '\b'; case 'f' -> '\f'; default -> c;
                    });
                } else if (c == '"') return sb.toString();
                else sb.append(c);
            }
            throw new RuntimeException("Unterminated string");
        }

        Number parseNumber() {
            int start = i;
            if (s.charAt(i) == '-') i++;
            while (i < s.length() && Character.isDigit(s.charAt(i))) i++;
            if (i < s.length() && s.charAt(i) == '.') { i++; while (i < s.length() && Character.isDigit(s.charAt(i))) i++; }
            if (i < s.length() && (s.charAt(i) == 'e' || s.charAt(i) == 'E')) {
                i++; if (i < s.length() && (s.charAt(i) == '+' || s.charAt(i) == '-')) i++;
                while (i < s.length() && Character.isDigit(s.charAt(i))) i++;
            }
            String num = s.substring(start, i);
            if (num.contains(".") || num.contains("e") || num.contains("E"))
                return Double.parseDouble(num);
            else
                return Long.parseLong(num);
        }

        Boolean parseBoolean() {
            if (s.startsWith("true", i)) { i += 4; return true; }
            if (s.startsWith("false", i)) { i += 5; return false; }
            throw new RuntimeException("Invalid boolean");
        }

        Object parseNull() {
            if (s.startsWith("null", i)) { i += 4; return null; }
            throw new RuntimeException("Invalid null");
        }

        void expect(char c) {
            if (i >= s.length() || s.charAt(i) != c)
                throw new RuntimeException("Expected '" + c + "' at " + i);
            i++;
        }

        void skip() {
            while (i < s.length() && Character.isWhitespace(s.charAt(i))) i++;
        }
    }
}