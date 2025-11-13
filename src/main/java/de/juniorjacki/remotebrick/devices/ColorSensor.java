package de.juniorjacki.remotebrick.devices;

import de.juniorjacki.remotebrick.Hub;

public class ColorSensor extends ConnectedDevice{
    public ColorSensor(Hub deviceRoot, byte port) {
        super(deviceRoot, port, 61);
    }
}
