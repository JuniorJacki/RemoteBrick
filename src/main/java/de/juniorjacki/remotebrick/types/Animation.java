/*
 * Copyright (c) 2025 JuniorJacki
 * All Rights Reserved
 */

package de.juniorjacki.remotebrick.types;

import de.juniorjacki.remotebrick.utils.JsonBuilder;

import java.util.List;

/**
 * Represents a 5x5 pixel animation for Inventor Hub display.
 * <p>
 * Animations are composed of individual frames, where each frame is a 5x5 grid of brightness values.
 * Each pixel is represented by a digit from {@code 0} (off) to {@code 9} (full brightness).
 * Frames are stored as strings in the format: {@code "12345:00000:54321:00000:12345"}.
 * </p>
 *
 * <p><strong>Display Specifications (Inventor Hub):</strong></p>
 * <ul>
 *   <li><strong>Resolution:</strong> 5x5 monochrome LED matrix</li>
 *   <li><strong>Brightness Levels:</strong> 0–9 (10 levels, 0 = off, 9 = max)</li>
 *   <li><strong>Frame Format:</strong> 5 rows × 5 columns, separated by colons ({@code :})</li>
 *   <li><strong>Max Frames:</strong> No strict limit (practical: ≤100 for smooth playback)</li>
 *   <li><strong>Playback:</strong> Controlled via hub commands (not part of this class)</li>
 * </ul>
 *
 * <p>
 * Use {@link #addFrame(String)} for raw frame strings or {@link #addImage(Image)} for pre-built images.
 * </p>
 *
 * <p><strong>Example Frame (smiley):</strong></p>
 * <pre>
 * "09090:90009:00000:90009:09990"
 * </pre>
 *
 * @see Image
 */
public class Animation {

    /** List of frame strings in 5x5 format. */
    private final java.util.List<String> frames = new java.util.ArrayList<>();

    /**
     * Adds a pre-defined {@link Image} as a frame to the animation.
     * <p>
     * The image is converted to its 5x5 string representation before being added.
     * </p>
     *
     * @param image The {@link Image} to add as a frame. Must not be {@code null}.
     * @return This {@link Animation} instance for method chaining.
     * @throws NullPointerException if {@code image} is {@code null}.
     */
    public Animation addImage(Image image) {
        frames.add(image.toString());
        return this;
    }

    /**
     * Adds a raw frame string to the animation.
     * <p>
     * The frame must be exactly 5x5 and contain only digits 0–9.
     * Example valid frame: {@code "00000:00100:01110:00100:00000"}
     * </p>
     *
     * @param frameData The 5x5 frame as a string with colons separating rows.
     * @return This {@link Animation} instance for method chaining.
     * @throws IllegalArgumentException if the frame format is invalid.
     * @throws NullPointerException if {@code frameData} is {@code null}.
     * @see #validateFrame(String)
     */
    public Animation addFrame(String frameData) {
        validateFrame(frameData);
        frames.add(frameData);
        return this;
    }

    /**
     * Converts the animation to a {@link JsonBuilder} object.
     * <p>
     * The resulting JSON has the structure:
     * <pre>
     * {
     *   "frames": ["frame1", "frame2", ...]
     * }
     * </pre>
     * </p>
     *
     * @return A {@link JsonBuilder} containing the animation data.
     */
    public JsonBuilder toJson() {
        return JsonBuilder.object()
                .add("frames", JsonBuilder.arrayOfStrings(frames.toArray(new String[0])));
    }

    /**
     * Returns the JSON string representation of this animation.
     * <p>
     * Equivalent to {@code toJson().toString()}.
     * </p>
     *
     * @return The animation as a JSON string.
     */
    @Override
    public String toString() {
        return toJson().toString();
    }

    /**
     * Returns the number of frames in this animation.
     *
     * @return The frame count (≥0).
     */
    public int getFrameCount() {
        return frames.size();
    }

    /**
     * Returns an unmodifiable view of the frame list.
     *
     * @return A list of frame strings.
     */
    public List<String> getFrames() {
        return java.util.Collections.unmodifiableList(frames);
    }

// --- Internal Validation ---

    /**
     * Validates the format of a 5x5 frame string.
     * <p>
     * Checks:
     * <ul>
     *   <li>Not {@code null}</li>
     *   <li>Exactly 5 rows separated by {@code :}</li>
     *   <li>Each row has exactly 5 characters</li>
     *   <li>All characters are digits 0–9</li>
     * </ul>
     * </p>
     *
     * @param frame The frame string to validate.
     * @throws IllegalArgumentException if validation fails.
     * @throws NullPointerException if {@code frame} is {@code null}.
     */
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

    // --- Predefined Animations ---

    /** Heartbeat animation – heart pulses with inner glow (6 frames) */
    public static final Animation HEARTBEAT = new Animation()
            .addImage(Image.HEART)
            .addImage(new Image().fill(0).setPixel(1, 1, 9).setPixel(2, 1, 9).setPixel(3, 1, 9)
                    .setPixel(1, 2, 9).setPixel(2, 2, 9).setPixel(3, 2, 9))
            .addImage(new Image().fill(0).setPixel(2, 2, 9))
            .addImage(new Image().fill(0).setPixel(1, 1, 9).setPixel(2, 1, 9).setPixel(3, 1, 9)
                    .setPixel(1, 2, 9).setPixel(2, 2, 9).setPixel(3, 2, 9))
            .addImage(Image.HEART)
            .addImage(Image.EMPTY);

    /** Blinking smiley – happy to neutral to happy (8 frames) */
    public static final Animation BLINK = new Animation()
            .addImage(Image.SMILEY)
            .addImage(new Image().setPixel(1, 0, 9).setPixel(3, 0, 9)
                    .setPixel(0, 1, 9).setPixel(4, 1, 9)
                    .setPixel(1, 3, 9).setPixel(2, 3, 9).setPixel(3, 3, 9))
            .addImage(new Image().setPixel(1, 0, 9).setPixel(3, 0, 9)
                    .setPixel(0, 1, 9).setPixel(4, 1, 9)
                    .setPixel(2, 3, 9))
            .addImage(new Image().setPixel(1, 0, 9).setPixel(3, 0, 9)
                    .setPixel(0, 1, 9).setPixel(4, 1, 9)
                    .setPixel(1, 3, 9).setPixel(3, 3, 9))
            .addImage(Image.SMILEY)
            .addImage(Image.SMILEY)
            .addImage(Image.SMILEY)
            .addImage(Image.SMILEY);

    /** Loading spinner – rotating bar (8 frames) */
    public static final Animation LOADING_SPINNER = new Animation()
            .addFrame("99999:00000:00000:00000:00000")
            .addFrame("09999:09999:00000:00000:00000")
            .addFrame("00999:00999:00999:00000:00000")
            .addFrame("00099:00099:00999:00999:00000")
            .addFrame("00009:00009:00009:00999:00999")
            .addFrame("00000:00000:00009:09999:09999")
            .addFrame("00000:00000:00000:99999:99999")
            .addFrame("00000:00000:00000:09999:99999");

    /** Battery charging – fill up gradually (10 frames) */
    public static final Animation BATTERY_CHARGING = new Animation()
            .addImage(Image.BATTERY_LOW)
            .addFrame("99990:90009:90000:90009:99990")
            .addFrame("99990:90009:90009:90009:99990")
            .addFrame("99990:90009:99009:90009:99990")
            .addFrame("99990:90009:99099:90009:99990")
            .addFrame("99990:90009:99999:90009:99990")
            .addFrame("99990:90009:99999:99009:99990")
            .addFrame("99990:90009:99999:99909:99990")
            .addFrame("99990:90009:99999:99999:99990")
            .addImage(Image.BATTERY_FULL);

    /** WiFi scanning – waves expanding (7 frames) */
    public static final Animation WIFI_SCAN = new Animation()
            .addImage(new Image().setPixel(2, 4, 9))
            .addImage(new Image().setPixel(1, 3, 9).setPixel(3, 3, 9).setPixel(2, 4, 9))
            .addImage(new Image().setPixel(0, 2, 9).setPixel(4, 2, 9).setPixel(1, 3, 9).setPixel(3, 3, 9).setPixel(2, 4, 9))
            .addImage(new Image().setPixel(1, 1, 9).setPixel(3, 1, 9).setPixel(0, 2, 9).setPixel(4, 2, 9).setPixel(2, 4, 9))
            .addImage(new Image().setPixel(2, 0, 9).setPixel(1, 1, 9).setPixel(3, 1, 9).setPixel(0, 2, 9).setPixel(4, 2, 9))
            .addImage(new Image().setPixel(2, 0, 9).setPixel(1, 1, 9).setPixel(3, 1, 9).setPixel(0, 2, 9).setPixel(4, 2, 9).setPixel(2, 4, 9))
            .addImage(Image.WIFI);

    /** Rocket launch – takeoff with smoke (12 frames) */
    public static final Animation ROCKET_LAUNCH = new Animation()
            .addFrame("00000:00200:00200:00200:00000")
            .addFrame("00000:00200:00200:00200:00900")
            .addFrame("00000:00200:00200:00900:09090")
            .addFrame("00000:00200:00900:09090:90909")
            .addFrame("00000:00900:09090:90909:09090")
            .addFrame("00000:09090:90909:09090:00000")
            .addFrame("00000:90909:09090:00000:00000")
            .addFrame("00000:09090:00000:00000:00000")
            .addFrame("00000:00000:00000:00000:00000")
            .addFrame("00000:00000:00000:00000:00999")
            .addFrame("00000:00000:00000:09990:99999")
            .addFrame("00000:00000:09990:99999:99999");

    /** Checkmark appears – growing from center (9 frames) */
    public static final Animation YES = new Animation()
            .addImage(Image.EMPTY)
            .addFrame("00000:00000:00000:00000:00900")
            .addFrame("00000:00000:00000:00900:00900")
            .addFrame("00000:00000:00900:00900:00000")
            .addFrame("00000:00900:00900:00000:00000")
            .addFrame("00900:00900:00000:00000:00000")
            .addFrame("09000:09000:00900:00000:00000")
            .addFrame("90000:90000:09000:00900:00000")
            .addImage(Image.CHECK);

    /** Cross appears – diagonal growth (10 frames) */
    public static final Animation NO = new Animation()
            .addImage(Image.EMPTY)
            .addFrame("90000:00000:00000:00000:00900")
            .addFrame("09000:90000:00000:00009:00090")
            .addFrame("00900:09000:90000:09000:90000")
            .addFrame("00090:00900:09000:90000:09000")
            .addFrame("00009:00090:00900:09000:90000")
            .addFrame("00000:00009:00090:00900:09000")
            .addFrame("00000:00000:00009:00090:00900")
            .addFrame("00000:00000:00000:00009:00090")
            .addFrame("00000:00000:00000:00000:00009")
            .addImage(Image.CROSS);

    /** Pulsing star – brightness cycle (12 frames) */
    public static final Animation PULSE_STAR = new Animation()
            .addImage(new Image().fill(1).invert().setPixel(2, 2, 9))
            .addImage(new Image().fill(2).invert().setPixel(2, 2, 9))
            .addImage(new Image().fill(3).invert().setPixel(2, 2, 9))
            .addImage(new Image().fill(4).invert().setPixel(2, 2, 9))
            .addImage(new Image().fill(5).invert().setPixel(2, 2, 9))
            .addImage(Image.STAR)
            .addImage(new Image().fill(7).invert().setPixel(2, 2, 9))
            .addImage(new Image().fill(6).invert().setPixel(2, 2, 9))
            .addImage(new Image().fill(5).invert().setPixel(2, 2, 9))
            .addImage(new Image().fill(4).invert().setPixel(2, 2, 9))
            .addImage(new Image().fill(3).invert().setPixel(2, 2, 9))
            .addImage(new Image().fill(2).invert().setPixel(2, 2, 9));

    /** Bouncing ball – left to right and back (12 frames) */
    public static final Animation BOUNCE = new Animation()
            .addFrame("00000:00000:00000:00000:90000")
            .addFrame("00000:00000:00000:90000:00000")
            .addFrame("00000:00000:90000:00000:00000")
            .addFrame("00000:90000:00000:00000:00000")
            .addFrame("90000:00000:00000:00000:00000")
            .addFrame("00000:90000:00000:00000:00000")
            .addFrame("00000:00000:90000:00000:00000")
            .addFrame("00000:00000:00000:90000:00000")
            .addFrame("00000:00000:00000:00000:90000")
            .addFrame("00000:00000:00000:90000:00000")
            .addFrame("00000:00000:90000:00000:00000")
            .addFrame("00000:90000:00000:00000:00000");

    /** Running man – simple walk cycle (8 frames) */
    public static final Animation RUNNING = new Animation()
            .addFrame("00900:09000:00900:90000:90000")
            .addFrame("00900:09000:00900:09000:90000")
            .addFrame("00900:09000:90000:90000:09000")
            .addFrame("00900:09000:90000:09000:90000")
            .addFrame("00900:09000:00900:90000:90000")
            .addFrame("00900:09000:00900:09000:90000")
            .addFrame("00900:09000:90000:90000:09000")
            .addFrame("00900:09000:90000:09000:90000");

    /** Wave – left to right ripple (10 frames) */
    public static final Animation WAVE = new Animation()
            .addFrame("10000:00000:00000:00000:00000")
            .addFrame("01000:10000:00000:00000:00000")
            .addFrame("00100:01000:10000:00000:00000")
            .addFrame("00010:00100:01000:10000:00000")
            .addFrame("00001:00010:00100:01000:10000")
            .addFrame("00000:00001:00010:00100:01000")
            .addFrame("00000:00000:00001:00010:00100")
            .addFrame("00000:00000:00000:00001:00010")
            .addFrame("00000:00000:00000:00000:00001")
            .addFrame("00000:00000:00000:00000:00000");

    /** Clock ticking – hand moves (12 frames) */
    public static final Animation CLOCK_TICK = new Animation()
            .addFrame("00100:00000:00100:00000:00100")
            .addFrame("00010:00000:00100:00000:00100")
            .addFrame("00001:00000:00100:00000:00100")
            .addFrame("00000:00001:00100:00000:00100")
            .addFrame("00000:00010:00100:00000:00100")
            .addFrame("00000:00100:00100:00000:00100")
            .addFrame("00000:01000:00100:00000:00100")
            .addFrame("00000:10000:00100:00000:00100")
            .addFrame("00100:00000:00100:00000:00100")
            .addFrame("01000:00000:00100:00000:00100")
            .addFrame("10000:00000:00100:00000:00100")
            .addFrame("00000:10000:00100:00000:00100");

    /** Rain – drops falling (10 frames) */
    public static final Animation RAIN = new Animation()
            .addFrame("10001:00000:00000:00000:00000")
            .addFrame("01000:10001:00000:00000:00000")
            .addFrame("00100:01000:10001:00000:00000")
            .addFrame("00010:00100:01000:10001:00000")
            .addFrame("00001:00010:00100:01000:10001")
            .addFrame("00000:00001:00010:00100:01000")
            .addFrame("00000:00000:00001:00010:00100")
            .addFrame("00000:00000:00000:00001:00010")
            .addFrame("00000:00000:00000:00000:00001")
            .addFrame("00000:00000:00000:00000:00000");

    /** Fire – flickering flames (8 frames) */
    public static final Animation FIRE = new Animation()
            .addFrame("00000:00900:09090:90009:99999")
            .addFrame("00000:09000:90909:09090:99999")
            .addFrame("00000:90000:09090:90909:99999")
            .addFrame("00000:00090:90909:09090:99999")
            .addFrame("00000:00900:09090:90009:99999")
            .addFrame("00000:09000:90909:09090:99999")
            .addFrame("00000:90000:09090:90909:99999")
            .addFrame("00000:00090:90909:09090:99999");

    /** Scanner – line sweeps top to bottom (6 frames) */
    public static final Animation SCANNER = new Animation()
            .addFrame("99999:00000:00000:00000:00000")
            .addFrame("00000:99999:00000:00000:00000")
            .addFrame("00000:00000:99999:00000:00000")
            .addFrame("00000:00000:00000:99999:00000")
            .addFrame("00000:00000:00000:00000:99999")
            .addFrame("00000:00000:00000:00000:00000");

    /** Equalizer – bars bouncing (10 frames) */
    public static final Animation EQUALIZER = new Animation()
            .addFrame("90000:09000:00900:00090:00009")
            .addFrame("09000:00900:00090:00009:90000")
            .addFrame("00900:00090:00009:90000:09000")
            .addFrame("00090:00009:90000:09000:00900")
            .addFrame("00009:90000:09000:00900:00090")
            .addFrame("90000:09000:00900:00090:00009")
            .addFrame("09000:00900:00090:00009:90000")
            .addFrame("00900:00090:00009:90000:09000")
            .addFrame("00090:00009:90000:09000:00900")
            .addFrame("00009:90000:09000:00900:00090");

    /** Growing circle – expand from center (7 frames) */
    public static final Animation GROW_CIRCLE = new Animation()
            .addFrame("00000:00000:00900:00000:00000")
            .addFrame("00000:09990:99999:09990:00000")
            .addFrame("09990:90009:90009:90009:09990")
            .addFrame("90009:90009:90009:90009:90009")
            .addFrame("90009:90009:90009:90009:90009")
            .addFrame("09990:90009:90009:90009:09990")
            .addFrame("00000:09990:99999:09990:00000");

    /** Countdown – 3 to 2 to 1 to GO (12 frames) */
    public static final Animation COUNTDOWN = new Animation()
            .addFrame("09990:90009:90009:90009:09990") // 3
            .addFrame("09990:90009:90009:90009:09990")
            .addFrame("09990:90009:90009:90009:09990")
            .addFrame("99999:90009:99990:90009:99999") // 2
            .addFrame("99999:90009:99990:90009:99999")
            .addFrame("99999:90009:99990:90009:99999")
            .addFrame("09990:90009:90009:90009:09990") // 1
            .addFrame("09990:90009:90009:90009:09990")
            .addFrame("09990:90009:90009:90009:09990")
            .addFrame("09990:90009:99999:90009:09990") // GO
            .addFrame("90009:99099:90909:99099:90009")
            .addFrame("99999:99999:99999:99999:99999");
}