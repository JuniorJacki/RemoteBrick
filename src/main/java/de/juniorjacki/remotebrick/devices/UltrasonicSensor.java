/*
 * Copyright (c) 2025 JuniorJacki
 * All Rights Reserved
 */

package de.juniorjacki.remotebrick.devices;

import de.juniorjacki.remotebrick.Hub;
import de.juniorjacki.remotebrick.types.Command;
import de.juniorjacki.remotebrick.types.CommandContext;
import de.juniorjacki.remotebrick.types.Port;
import de.juniorjacki.remotebrick.utils.JsonBuilder;
import de.juniorjacki.remotebrick.utils.SimpleJsonArray;

import java.util.ArrayList;
import java.util.List;

/**
 * Represents LEGO Inventor Hub Ultrasonic Sensor.
 * <p>
 * Measures distance using ultrasonic pulses and supports 4 independently controllable RGB LEDs.
 * </p>
 *
 * <p><strong>Sensor Specifications (SPIKE Prime / Inventor Hub):</strong></p>
 * <ul>
 *   <li><strong>Measurement Range:</strong> 0 cm to 200 cm </li>
 *   <li><strong>Accuracy:</strong> ±1 cm (typical within 100 cm)</li>
 *   <li><strong>Update Rate:</strong> ~30 Hz (approximately 33 ms per measurement)</li>
 *   <li><strong>LEDs:</strong> 4 RGB LEDs (top, bottom, left, right), each 0–100% brightness per channel</li>
 *   <li><strong>Power:</strong> 5V from hub</li>
 *   <li><strong>Out of Range:</strong> Returns 201 cm if object is too close or too far</li>
 * </ul>
 *
 * <p>
 * Distance is reported in <strong>centimeters</strong> and updated asynchronously via {@link #update(SimpleJsonArray)}.
 * Use {@link #getControl()} to control the sensor's LEDs.
 * </p>
 *
 * @see UltrasonicControl
 */
public class UltrasonicSensor extends ConnectedDevice<UltrasonicSensor.UltrasonicSensorDataType> {

    public enum UltrasonicSensorDataType implements DataType {
        Distance
    }

    /**
     * Current distance measured by the sensor in <strong>centimeters</strong>.
     * <p>
     * Updated in real-time.
     * </p>
     * <p><strong>Valid Range:</strong> 1 to 200 cm<br>
     * <strong>Out of Range / No Object:</strong> Returns {@code 201}</p>
     */
    private int distance = 0;

    /**
     * Returns the current measured distance.
     *
     * @return Distance in <strong>centimeters</strong> (1–200), or {@code 201} if out of range or not detected.
     */
    public int getDistance() {
        return distance;
    }

    /**
     * Returns the control interface for the sensor's LEDs.
     *
     * @return The {@link UltrasonicControl} instance.
     */
    public UltrasonicControl getControl() {
        return control;
    }

    private final UltrasonicControl control = new UltrasonicControl();
    public UltrasonicSensor(Hub deviceRoot, Port port) {
        super(deviceRoot, port, 62);
    }

    @Override
    public Object parseData(SimpleJsonArray data, UltrasonicSensorDataType type) {
        return data.optInt(0,201);
    }

    @Override
    public List<UltrasonicSensorDataType> update(SimpleJsonArray data) {
        List<UltrasonicSensorDataType> dataTypes = new ArrayList<>();
        if (data != null) {
            int newData = data.optInt(0,201);
            if (newData !=  getDistance()) {
                distance = newData;
                dataTypes.add(UltrasonicSensorDataType.Distance);
            }
        }
        return dataTypes;
    }


    /**
     * Provides control over the ultrasonic sensor's 4 RGB LEDs.
     * <p>
     * Each LED can be set independently with brightness from 0 (off) to 100 (full).
     * </p>
     * <p>
     * LED positions (viewed from front):
     * <ul>
     *   <li>{@code l1} – Top LED</li>
     *   <li>{@code l2} – Bottom LED</li>
     *   <li>{@code l3} – Left LED</li>
     *   <li>{@code l4} – Right LED</li>
     * </ul>
     * </p>
     *
     * @see Command
     * @see CommandContext
     */
    public class UltrasonicControl {

        /**
         * Sets the brightness of all 4 LEDs on the ultrasonic sensor.
         * <p>
         * Each parameter controls one LED's brightness in percent.
         * </p>
         * <p><strong>Parameter Ranges:</strong></p>
         * <ul>
         *   <li>{@code l1, l2, l3, l4}: 0 to 100 (0 = off, 100 = full brightness)</li>
         * </ul>
         *
         * @param l1 Top LED brightness (0–100)
         * @param l2 Bottom LED brightness (0–100)
         * @param l3 Left LED brightness (0–100)
         * @param l4 Right LED brightness (0–100)
         * @return An executable {@link Command}, or {@code null} if sensor is not functional.
         */
        public Command lightUp(int l1,int l2,int l3,int l4) {
            if (isFunctional()) {
                return new CommandContext("scratch.ultrasonic_light_up",JsonBuilder.object().add("port",port.name()).add("lights",JsonBuilder.array(l1,l2,l3,l4))).generateCommand(deviceRoot);
            }
            return null;
        }
    }
}
