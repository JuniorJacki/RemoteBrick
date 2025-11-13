package de.juniorjacki.remotebrick.devices;

import de.juniorjacki.remotebrick.Hub;

public class UltrasonicSensor extends ConnectedDevice{
    public UltrasonicSensor(Hub deviceRoot, byte port) {
        super(deviceRoot, port, 62);
    }
}
