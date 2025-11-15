/*
 * Copyright (c) 2025 JuniorJacki
 * All Rights Reserved
 */

package de.juniorjacki.remotebrick.types;

import de.juniorjacki.remotebrick.utils.JsonBuilder;

/**
 * Represents a 5x5 monochrome image for the LEGO Inventor Hub display.
 * <p>
 * Each pixel has a brightness level from {@code 0} (off) to {@code 9} (full brightness).
 * Images are used for static display or as frames in {@link Animation}.
 * </p>
 *
 * <p><strong>Display Specifications (Inventor Hub):</strong></p>
 * <ul>
 *   <li><strong>Resolution:</strong> 5x5 LED matrix</li>
 *   <li><strong>Brightness Levels:</strong> 0–9 (10 levels)</li>
 *   <li><strong>Coordinate System:</strong> (0,0) = top-left, x = column, y = row</li>
 *   <li><strong>String Format:</strong> {@code "12345:00000:54321:00000:12345"} (rows separated by {@code :})</li>
 * </ul>
 *
 * <p><strong>Examples:</strong></p>
 * <pre>
 * // Create a smiley face
 * Image smiley = new Image()
 *     .setPixel(1, 0, 9).setPixel(3, 0, 9)  // eyes
 *     .setPixel(0, 1, 9).setPixel(4, 1, 9)
 *     .setPixel(2, 2, 0)                     // nose (off)
 *     .setPixel(1, 3, 9).setPixel(2, 3, 9).setPixel(3, 3, 9); // mouth
 *
 * // Show on hub display
 * hub.getDisplay().showImage(smiley);
 * </pre>
 *
 * @see Animation
 * @see JsonBuilder
 */
public class Image {
    /** 5x5 pixel grid: pixels[y][x] where y = row, x = column (0–4). */
    private final int[][] pixels = new int[5][5];

    /**
     * Sets the brightness of a single pixel.
     * <p>
     * Coordinates: (0,0) = top-left corner.
     * </p>
     *
     * @param x          Column index (0–4).
     * @param y          Row index (0–4).
     * @param brightness Brightness level (0 = off, 9 = full).
     * @return This {@link Image} instance for method chaining.
     * @throws IllegalArgumentException if coordinates are out of bounds or brightness is invalid.
     */
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

    /**
     * Returns the brightness of a pixel.
     *
     * @param x Column index (0–4).
     * @param y Row index (0–4).
     * @return Brightness level (0–9).
     * @throws IllegalArgumentException if coordinates are invalid.
     */
    public int getPixel(int x, int y) {
        if (x < 0 || x > 4 || y < 0 || y > 4) {
            throw new IllegalArgumentException("x und y müssen 0–4 sein!");
        }
        return pixels[y][x];
    }

    /**
     * Clears the image (sets all pixels to 0).
     *
     * @return This {@link Image} instance for chaining.
     */
    public Image clear() {
        for (int y = 0; y < 5; y++) {
            for (int x = 0; x < 5; x++) {
                pixels[y][x] = 0;
            }
        }
        return this;
    }

    /**
     * Inverts all pixel values ({@code 9 - current}).
     *
     * @return This {@link Image} instance for chaining.
     */
    public Image invert() {
        for (int y = 0; y < 5; y++) {
            for (int x = 0; x < 5; x++) {
                pixels[y][x] = 9 - pixels[y][x];
            }
        }
        return this;
    }

    /**
     * Fills the entire image with a single brightness level.
     *
     * @param brightness Brightness level (0–9).
     * @return This {@link Image} instance for chaining.
     * @throws IllegalArgumentException if brightness is invalid.
     */
    public Image fill(int brightness) {
        if (brightness < 0 || brightness > 9) {
            throw new IllegalArgumentException("Helligkeit muss 0–9 sein!");
        }
        for (int y = 0; y < 5; y++) {
            for (int x = 0; x < 5; x++) {
                pixels[y][x] = brightness;
            }
        }
        return this;
    }

    /**
     * Converts the image to a {@link JsonBuilder} for hub transmission.
     * <p>
     * Output format:
     * <pre>
     * {"image": "12345:00000:54321:00000:12345"}
     * </pre>
     * </p>
     *
     * @return A {@link JsonBuilder} with the image string.
     */
    public JsonBuilder toJson() {
        return JsonBuilder.object().add("image", this.toString());
    }

    /**
     * Returns the 5x5 image as a string in hub-compatible format.
     * <p>
     * Example: {@code "09090:90009:00000:90009:09990"}
     * </p>
     *
     * @return The image string with rows separated by colons.
     */
    @Override
    public String toString() {
        StringBuilder data = new StringBuilder();
        for (int y = 0; y < 5; y++) {
            for (int x = 0; x < 5; x++) {
                data.append(pixels[y][x]);
            }
            if (y < 4) data.append(":");
        }
        return data.toString();
    }

    // --- Predefined Images ---

    /** Happy smiley face */
    public static final Image SMILEY = new Image()
            .setPixel(1, 0, 9).setPixel(3, 0, 9)
            .setPixel(0, 1, 9).setPixel(4, 1, 9)
            .setPixel(1, 3, 9).setPixel(2, 3, 9).setPixel(3, 3, 9);

    /** Sad face */
    public static final Image SAD = new Image()
            .setPixel(1, 0, 9).setPixel(3, 0, 9)
            .setPixel(0, 1, 9).setPixel(4, 1, 9)
            .setPixel(1, 2, 9).setPixel(3, 2, 9)
            .setPixel(0, 3, 9).setPixel(4, 3, 9);

    /** Heart symbol (full) */
    public static final Image HEART = new Image()
            .setPixel(1, 0, 9).setPixel(3, 0, 9)
            .setPixel(0, 1, 9).setPixel(2, 1, 9).setPixel(4, 1, 9)
            .setPixel(0, 2, 9).setPixel(1, 2, 9).setPixel(3, 2, 9).setPixel(4, 2, 9)
            .setPixel(1, 3, 9).setPixel(3, 3, 9)
            .setPixel(2, 4, 9);

    /** Broken heart */
    public static final Image BROKEN_HEART = new Image()
            .setPixel(1, 0, 9).setPixel(3, 0, 9)
            .setPixel(0, 1, 9).setPixel(2, 1, 9).setPixel(4, 1, 9)
            .setPixel(0, 2, 9).setPixel(1, 2, 9).setPixel(3, 2, 9)
            .setPixel(1, 3, 9).setPixel(3, 3, 9)
            .setPixel(2, 4, 9);

    /** Star */
    public static final Image STAR = new Image()
            .setPixel(2, 0, 9)
            .setPixel(1, 1, 9).setPixel(2, 1, 9).setPixel(3, 1, 9)
            .setPixel(0, 2, 9).setPixel(4, 2, 9)
            .setPixel(1, 3, 9).setPixel(2, 3, 9).setPixel(3, 3, 9)
            .setPixel(2, 4, 9);

    /** Checkmark */
    public static final Image CHECK = new Image()
            .setPixel(4, 0, 9)
            .setPixel(3, 1, 9).setPixel(4, 1, 9)
            .setPixel(2, 2, 9)
            .setPixel(0, 3, 9).setPixel(1, 3, 9)
            .setPixel(0, 4, 9);

    /** Cross (X) */
    public static final Image CROSS = new Image()
            .setPixel(0, 0, 9).setPixel(4, 0, 9)
            .setPixel(1, 1, 9).setPixel(3, 1, 9)
            .setPixel(2, 2, 9)
            .setPixel(1, 3, 9).setPixel(3, 3, 9)
            .setPixel(0, 4, 9).setPixel(4, 4, 9);

    /** Arrow pointing left */
    public static final Image ARROW_LEFT = new Image()
            .setPixel(4, 0, 9)
            .setPixel(3, 1, 9).setPixel(4, 1, 9)
            .setPixel(2, 2, 9).setPixel(3, 2, 9).setPixel(4, 2, 9)
            .setPixel(3, 3, 9).setPixel(4, 3, 9)
            .setPixel(4, 4, 9);

    /** Arrow pointing right */
    public static final Image ARROW_RIGHT = new Image()
            .setPixel(0, 0, 9)
            .setPixel(0, 1, 9).setPixel(1, 1, 9)
            .setPixel(0, 2, 9).setPixel(1, 2, 9).setPixel(2, 2, 9)
            .setPixel(0, 3, 9).setPixel(1, 3, 9)
            .setPixel(0, 4, 9);

    /** Arrow pointing up */
    public static final Image ARROW_UP = new Image()
            .setPixel(2, 0, 9)
            .setPixel(1, 1, 9).setPixel(2, 1, 9).setPixel(3, 1, 9)
            .setPixel(0, 2, 9).setPixel(4, 2, 9)
            .setPixel(2, 3, 9)
            .setPixel(2, 4, 9);

    /** Arrow pointing down */
    public static final Image ARROW_DOWN = new Image()
            .setPixel(2, 0, 9)
            .setPixel(2, 1, 9)
            .setPixel(0, 2, 9).setPixel(4, 2, 9)
            .setPixel(1, 3, 9).setPixel(2, 3, 9).setPixel(3, 3, 9)
            .setPixel(2, 4, 9);

    /** Play symbol (triangle right) */
    public static final Image PLAY = new Image()
            .setPixel(0, 0, 9)
            .setPixel(0, 1, 9).setPixel(1, 1, 9)
            .setPixel(0, 2, 9).setPixel(1, 2, 9).setPixel(2, 2, 9)
            .setPixel(0, 3, 9).setPixel(1, 3, 9)
            .setPixel(0, 4, 9);

    /** Pause symbol (double bar) */
    public static final Image PAUSE = new Image()
            .setPixel(1, 0, 9).setPixel(3, 0, 9)
            .setPixel(1, 1, 9).setPixel(3, 1, 9)
            .setPixel(1, 2, 9).setPixel(3, 2, 9)
            .setPixel(1, 3, 9).setPixel(3, 3, 9)
            .setPixel(1, 4, 9).setPixel(3, 4, 9);

    /** Stop symbol (square) */
    public static final Image STOP = new Image()
            .fill(9)
            .setPixel(1, 1, 0).setPixel(1, 2, 0).setPixel(1, 3, 0)
            .setPixel(2, 1, 0).setPixel(2, 3, 0)
            .setPixel(3, 1, 0).setPixel(3, 2, 0).setPixel(3, 3, 0);

    /** Battery full */
    public static final Image BATTERY_FULL = new Image()
            .setPixel(0, 0, 9).setPixel(1, 0, 9).setPixel(2, 0, 9).setPixel(3, 0, 9)
            .setPixel(0, 1, 9).setPixel(3, 1, 9)
            .setPixel(0, 2, 9).setPixel(3, 2, 9)
            .setPixel(0, 3, 9).setPixel(3, 3, 9)
            .setPixel(0, 4, 9).setPixel(1, 4, 9).setPixel(2, 4, 9).setPixel(3, 4, 9);

    /** Battery low */
    public static final Image BATTERY_LOW = new Image()
            .setPixel(0, 0, 9).setPixel(1, 0, 9).setPixel(2, 0, 9).setPixel(3, 0, 9)
            .setPixel(0, 1, 9).setPixel(3, 1, 9)
            .setPixel(0, 2, 9)
            .setPixel(0, 3, 9).setPixel(3, 3, 9)
            .setPixel(0, 4, 9).setPixel(1, 4, 9).setPixel(2, 4, 9).setPixel(3, 4, 9);

    /** Bluetooth symbol */
    public static final Image BLUETOOTH = new Image()
            .setPixel(2, 0, 9)
            .setPixel(1, 1, 9).setPixel(2, 1, 9).setPixel(3, 1, 9)
            .setPixel(0, 2, 9).setPixel(4, 2, 9)
            .setPixel(1, 3, 9).setPixel(2, 3, 9).setPixel(3, 3, 9)
            .setPixel(2, 4, 9);

    /** WiFi symbol */
    public static final Image WIFI = new Image()
            .setPixel(2, 0, 9)
            .setPixel(1, 1, 9).setPixel(3, 1, 9)
            .setPixel(0, 2, 9).setPixel(4, 2, 9)
            .setPixel(1, 3, 9).setPixel(3, 3, 9)
            .setPixel(2, 4, 9);

    /** Music note */
    public static final Image MUSIC = new Image()
            .setPixel(0, 0, 9).setPixel(1, 0, 9)
            .setPixel(0, 1, 9)
            .setPixel(0, 2, 9).setPixel(1, 2, 9).setPixel(2, 2, 9).setPixel(3, 2, 9)
            .setPixel(3, 3, 9)
            .setPixel(3, 4, 9);

    /** Warning / Alert */
    public static final Image WARNING = new Image()
            .setPixel(2, 0, 9)
            .setPixel(1, 1, 9).setPixel(2, 1, 9).setPixel(3, 1, 9)
            .setPixel(0, 2, 9).setPixel(4, 2, 9)
            .setPixel(0, 3, 9).setPixel(4, 3, 9)
            .setPixel(0, 4, 9).setPixel(1, 4, 9).setPixel(3, 4, 9).setPixel(4, 4, 9);

    /** Question mark */
    public static final Image QUESTION = new Image()
            .setPixel(1, 0, 9).setPixel(2, 0, 9).setPixel(3, 0, 9)
            .setPixel(0, 1, 9).setPixel(4, 1, 9)
            .setPixel(3, 2, 9)
            .setPixel(2, 3, 9)
            .setPixel(2, 4, 9);

    /** Sun */
    public static final Image SUN = new Image()
            .setPixel(2, 0, 9)
            .setPixel(1, 1, 9).setPixel(2, 1, 9).setPixel(3, 1, 9)
            .setPixel(0, 2, 9).setPixel(2, 2, 9).setPixel(4, 2, 9)
            .setPixel(1, 3, 9).setPixel(2, 3, 9).setPixel(3, 3, 9)
            .setPixel(2, 4, 9);

    /** Cloud */
    public static final Image CLOUD = new Image()
            .setPixel(1, 0, 9).setPixel(2, 0, 9).setPixel(3, 0, 9)
            .setPixel(0, 1, 9).setPixel(1, 1, 9).setPixel(3, 1, 9).setPixel(4, 1, 9)
            .setPixel(0, 2, 9).setPixel(1, 2, 9).setPixel(2, 2, 9).setPixel(3, 2, 9).setPixel(4, 2, 9);

    /** Lightning */
    public static final Image LIGHTNING = new Image()
            .setPixel(2, 0, 9)
            .setPixel(1, 1, 9).setPixel(2, 1, 9)
            .setPixel(2, 2, 9).setPixel(3, 2, 9)
            .setPixel(1, 3, 9).setPixel(2, 3, 9)
            .setPixel(2, 4, 9);

    /** Robot face */
    public static final Image ROBOT = new Image()
            .setPixel(0, 0, 9).setPixel(4, 0, 9)
            .setPixel(0, 1, 9).setPixel(1, 1, 9).setPixel(3, 1, 9).setPixel(4, 1, 9)
            .setPixel(0, 2, 9).setPixel(4, 2, 9)
            .setPixel(1, 3, 9).setPixel(2, 3, 9).setPixel(3, 3, 9)
            .setPixel(2, 4, 9);

    /** Target / Bullseye */
    public static final Image TARGET = new Image()
            .setPixel(2, 0, 9)
            .setPixel(1, 1, 9).setPixel(2, 1, 9).setPixel(3, 1, 9)
            .setPixel(0, 2, 9).setPixel(4, 2, 9)
            .setPixel(1, 3, 9).setPixel(2, 3, 9).setPixel(3, 3, 9)
            .setPixel(2, 4, 9);

    /** Clock */
    public static final Image CLOCK = new Image()
            .setPixel(1, 0, 9).setPixel(2, 0, 9).setPixel(3, 0, 9)
            .setPixel(0, 1, 9).setPixel(4, 1, 9)
            .setPixel(0, 2, 9).setPixel(2, 2, 9).setPixel(4, 2, 9)
            .setPixel(0, 3, 9).setPixel(4, 3, 9)
            .setPixel(1, 4, 9).setPixel(2, 4, 9).setPixel(3, 4, 9);

    /** Gear */
    public static final Image GEAR = new Image()
            .setPixel(2, 0, 9)
            .setPixel(1, 1, 9).setPixel(3, 1, 9)
            .setPixel(0, 2, 9).setPixel(2, 2, 9).setPixel(4, 2, 9)
            .setPixel(1, 3, 9).setPixel(3, 3, 9)
            .setPixel(2, 4, 9);

    /** House */
    public static final Image HOUSE = new Image()
            .setPixel(2, 0, 9)
            .setPixel(1, 1, 9).setPixel(2, 1, 9).setPixel(3, 1, 9)
            .setPixel(0, 2, 9).setPixel(1, 2, 9).setPixel(3, 2, 9).setPixel(4, 2, 9)
            .setPixel(0, 3, 9).setPixel(4, 3, 9)
            .setPixel(0, 4, 9).setPixel(1, 4, 9).setPixel(3, 4, 9).setPixel(4, 4, 9);

    /** Thermometer (high) */
    public static final Image THERMOMETER_HIGH = new Image()
            .setPixel(1, 0, 9).setPixel(3, 0, 9)
            .setPixel(1, 1, 9).setPixel(3, 1, 9)
            .setPixel(1, 2, 9).setPixel(3, 2, 9)
            .setPixel(0, 3, 9).setPixel(1, 3, 9).setPixel(3, 3, 9).setPixel(4, 3, 9)
            .setPixel(1, 4, 9).setPixel(3, 4, 9);

    /** Thermometer (low) */
    public static final Image THERMOMETER_LOW = new Image()
            .setPixel(1, 0, 9).setPixel(3, 0, 9)
            .setPixel(1, 1, 9).setPixel(3, 1, 9)
            .setPixel(0, 3, 9).setPixel(1, 3, 9).setPixel(3, 3, 9).setPixel(4, 3, 9)
            .setPixel(1, 4, 9).setPixel(3, 4, 9);

    /** Empty (all off) */
    public static final Image EMPTY = new Image();

    /** Full (all on) */
    public static final Image FULL = new Image().fill(9);
}