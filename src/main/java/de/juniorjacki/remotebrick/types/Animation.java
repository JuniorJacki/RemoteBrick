/*
 * Copyright (c) 2025 JuniorJacki
 * All Rights Reserved
 */

package de.juniorjacki.remotebrick.types;

import de.juniorjacki.remotebrick.utils.JsonBuilder;

public class Animation {
    private final java.util.List<String> frames = new java.util.ArrayList<>();

    // Füge ein Image als Frame hinzu
    public Animation addImage(Image image) {
        // Konvertiere Image → "99099:99099:..."
        StringBuilder frameData = new StringBuilder();
        int[][] pixels = getPixelsFromImage(image);
        for (int y = 0; y < 5; y++) {
            for (int x = 0; x < 5; x++) {
                frameData.append(pixels[y][x]);
            }
            if (y < 4) frameData.append(":");
        }
        frames.add(frameData.toString());
        return this;
    }

    // Füge einen Frame direkt als String hinzu (muss 5x5 sein: "00000:00000:...")
    public Animation addFrame(String frameData) {
        validateFrame(frameData);
        frames.add(frameData);
        return this;
    }

    public JsonBuilder toJson() {
        return new JsonBuilder().addArray("frames", new java.util.ArrayList<>(frames));
    }


    @Override
    public String toString() {
        return toJson().toString();
    }

    // --- Hilfsmethode: Image → pixels (Reflection oder direkt) ---
    private int[][] getPixelsFromImage(Image image) {
        // Da Image private pixels hat → wir nutzen toString() und parsen!
        // Alternativ: Freundschaft mit Package, aber wir bleiben sauber
        String data = extractImageData(image.toString());
        if (data == null) return new int[5][5];

        String[] rows = data.split(":");
        int[][] pixels = new int[5][5];
        for (int y = 0; y < 5; y++) {
            String row = rows[y];
            for (int x = 0; x < 5; x++) {
                pixels[y][x] = row.charAt(x) - '0';
            }
        }
        return pixels;
    }

    private String extractImageData(String json) {
        int start = json.indexOf("\"image\":\"");
        if (start == -1) return null;
        start += 9;
        int end = json.indexOf("\"", start);
        return end != -1 ? json.substring(start, end) : null;
    }

    // --- Validierung ---
    private void validateFrame(String frame) {
        if (frame == null) throw new IllegalArgumentException("Frame darf nicht null sein");
        String[] rows = frame.split(":");
        if (rows.length != 5) throw new IllegalArgumentException("Frame muss 5 Zeilen haben");
        for (String row : rows) {
            if (row.length() != 5) throw new IllegalArgumentException("Jede Zeile muss 5 Zeichen haben");
            for (char c : row.toCharArray()) {
                if (c < '0' || c > '9') throw new IllegalArgumentException("Nur Ziffern 0-9 erlaubt");
            }
        }
    }
}