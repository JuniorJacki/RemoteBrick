/*
 * Copyright (c) 2025 JuniorJacki
 * All Rights Reserved
 */

package de.juniorjacki.remotebrick.devices;

import de.juniorjacki.remotebrick.Hub;
import de.juniorjacki.remotebrick.types.Port;

public class ColorSensor extends ConnectedDevice{
    public ColorSensor(Hub deviceRoot, Port port) {
        super(deviceRoot, port, 61);
    }
}
