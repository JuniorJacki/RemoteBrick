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

public class ColorSensor extends ConnectedDevice{

    private int reflection = 0;
    private int color = 0;
    private int red = 0;
    private int green = 0;
    private int blue = 0;

    public ColorControl getControl() {
        return control;
    }

    public int getBlue() {
        return blue;
    }

    public int getGreen() {
        return green;
    }

    public int getRed() {
        return red;
    }

    public int getColor() {
        return color;
    }

    public int getReflection() {
        return reflection;
    }

    private final ColorControl control = new ColorControl();

    public ColorSensor(Hub deviceRoot, Port port) {
        super(deviceRoot, port, 61);
    }


    public class ColorControl {
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
            reflection = data.optInt(0);
            color = data.optInt(1);
            red = data.optInt(2);
            green = data.optInt(3);
            blue = data.optInt(4);
        }
    }
}
