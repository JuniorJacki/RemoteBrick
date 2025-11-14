package de.juniorjacki.remotebrick.types;

import de.juniorjacki.remotebrick.utils.JsonBuilder;

public class Image {
    private final int[][] pixels = new int[5][5];

    public Image setPixel(int x, int y, int brightness) {
        if (x < 0 || x > 4 || y < 0 || y > 4) {
            throw new IllegalArgumentException("x und y müssen 0-4 sein!");
        }
        if (brightness < 0 || brightness > 9) {
            throw new IllegalArgumentException("Helligkeit muss 0-9 sein!");
        }
        pixels[y][x] = brightness; // y = Zeile, x = Spalte
        return this; // für Chaining
    }

    public JsonBuilder toJson() {
        StringBuilder data = new StringBuilder();
        for (int y = 0; y < 5; y++) {
            for (int x = 0; x < 5; x++) {
                data.append(pixels[y][x]);
            }
            if (y < 4) data.append(":");
        }
        return new JsonBuilder().add("image", data.toString());
    }
}