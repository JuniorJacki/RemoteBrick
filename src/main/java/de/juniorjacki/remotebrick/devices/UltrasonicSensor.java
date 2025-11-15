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

public class UltrasonicSensor extends ConnectedDevice{
    public int getDistance() {
        return distance;
    }

    private int distance = 0;

    public UltrasonicControl getControl() {
        return control;
    }

    private final UltrasonicControl control = new UltrasonicControl();
    public UltrasonicSensor(Hub deviceRoot, Port port) {
        super(deviceRoot, port, 62);
    }

    @Override
    public void update(SimpleJsonArray data) {
        if (data != null) {
            distance = data.optInt(0);
        }
    }


    public class UltrasonicControl {
        public Command lightUp(int l1,int l2,int l3,int l4) {
            if (isFunctional()) {
                return new CommandContext("scratch.ultrasonic_light_up",JsonBuilder.object().add("port",port.name()).add("lights",JsonBuilder.array(l1,l2,l3,l4))).generateCommand(deviceRoot);
            }
            return null;
        }
    }
}
