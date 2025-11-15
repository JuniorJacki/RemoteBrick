/*
 * Copyright (c) 2025 JuniorJacki
 * All Rights Reserved
 */

package de.juniorjacki.remotebrick.devices;

import de.juniorjacki.remotebrick.Hub;
import de.juniorjacki.remotebrick.types.ColorSensorMode;
import de.juniorjacki.remotebrick.types.Command;
import de.juniorjacki.remotebrick.types.CommandContext;
import de.juniorjacki.remotebrick.types.Port;
import de.juniorjacki.remotebrick.utils.JsonBuilder;
import de.juniorjacki.remotebrick.utils.SimpleJsonArray;

/**
 * Represents a LEGO Inventor Hub Color Sensor.
 * <p>
 * Detects colors, ambient light, and reflected light intensity using RGB LED illumination.
 * Supports multiple operating modes via {@link ColorSensorMode}.
 * </p>
 *
 * <p><strong>Sensor Specifications (SPIKE Prime / Inventor Hub):</strong></p>
 * <ul>
 *   <li><strong>Color Detection:</strong> 10 distinct colors (including none/black)</li>
 *   <li><strong>Reflection:</strong> 0–100% (proximity to surface under red LED)</li>
 *   <li><strong>RGB Values:</strong> 0–1023 per channel (10-bit ADC)</li>
 *   <li><strong>Illumination:</strong> Red LED for reflection, white LED for color detection</li>
 *   <li><strong>Update Rate:</strong> ~30 Hz per mode</li>
 *   <li><strong>Range (Reflection Mode):</strong> 1–10 cm optimal</li>
 *   <li><strong>Out of Range / No Data:</strong> Returns {@code -1}</li>
 * </ul>
 *
 * <p>
 * Use {@link #getControl()} to change sensor mode.
 * </p>
 *
 * @see ColorSensorMode
 * @see ColorControl
 */
public class ColorSensor extends ConnectedDevice{

    /**
     * Reflected light intensity in percent.
     * <p>
     * Measures how much red light is reflected back from a surface.
     * Higher values = brighter/whiter surface.
     * </p>
     * <p><strong>Range:</strong> 0 to 100 (% reflection)<br>
     * <strong>Invalid/No Data:</strong> {@code -1}</p>
     */
    private int reflection = 0;

    /**
     * Detected color as an index.
     * <p>
     * Maps to LEGO's predefined color palette (0 = none, 1 = black, ..., 10 = white).
     * </p>
     * <p><strong>Range:</strong> 0 to 10<br>
     * <strong>Invalid/No Data:</strong> {@code -1}</p>
     */
    private int color = 0;

    /**
     * Raw red channel value from RGB measurement.
     * <p><strong>Range:</strong> 0 to 1023<br>
     * <strong>Invalid/No Data:</strong> {@code -1}</p>
     */
    private int red = 0;

    /**
     * Raw green channel value from RGB measurement.
     * <p><strong>Range:</strong> 0 to 1023<br>
     * <strong>Invalid/No Data:</strong> {@code -1}</p>
     */
    private int green = 0;

    /**
     * Raw blue channel value from RGB measurement.
     * <p>
     * Measured under white LED illumination.
     * </p>
     * <p><strong>Range:</strong> 0 to 1023<br>
     * <strong>Invalid/No Data:</strong> {@code -1}</p>
     */
    private int blue = 0;

    /**
     * Returns the control interface for the color sensor.
     *
     * @return The {@link ColorControl} instance.
     */
    public ColorControl getControl() {
        return control;
    }

    /**
     * Returns the raw blue channel value.
     *
     * @return Blue value (0–1023), or {@code -1} if not available.
     */
    public int getBlue() {
        return blue;
    }

    /**
     * Returns the raw green channel value.
     *
     * @return Green value (0–1023), or {@code -1} if not available.
     */
    public int getGreen() {
        return green;
    }

    /**
     * Returns the raw red channel value.
     *
     * @return Red value (0–1023), or {@code -1} if not available.
     */
    public int getRed() {
        return red;
    }

    /**
     * Returns the detected color index.
     *
     * @return Color index (0–10), or {@code -1} if not available.
     */
    public int getColor() {
        return color;
    }

    /**
     * Returns the current reflected light intensity.
     *
     * @return Reflection in percent (0–100), or {@code -1} if not available.
     */
    public int getReflection() {
        return reflection;
    }

    private final ColorControl control = new ColorControl();
    public ColorSensor(Hub deviceRoot, Port port) {
        super(deviceRoot, port, 61);
    }

    /**
     * Controls the operating mode of the color sensor.
     * <p>
     * Use {@link #setDeviceMode(ColorSensorMode)} to switch between:
     * </p>
     *
     * @see ColorSensorMode
     * @see Command
     */
    public class ColorControl {

        /**
         * Changes the operating mode of the color sensor.
         * <p>
         * The sensor must be in a compatible mode to return valid data via getters.
         * </p>
         * <p>
         * Mode change is asynchronous – allow ~100ms before reading new values.
         * </p>
         *
         * @param mode The new {@link ColorSensorMode} to activate.
         * @return An executable {@link Command}, or {@code null} if sensor is not functional.
         * @see ColorSensorMode
         */
        public Command setDeviceMode(ColorSensorMode mode) {
            if (isFunctional()) {
                return new CommandContext("scratch.set_device_mode", JsonBuilder.object().add("port",port.name()).add("modetype",mode.name().toLowerCase()).addAll(mode.getModeJson())).generateCommand(deviceRoot);
            }
            return null;
        }
    }

    @Override
    public void update(SimpleJsonArray data) {
        if (data != null) {
            reflection = data.optInt(0,-1);
            color = data.optInt(1,-1);
            red = data.optInt(2,-1);
            green = data.optInt(3,-1);
            blue = data.optInt(4,-1);
        }
    }
}
