package de.juniorjacki.remotebrick.devices;

import de.juniorjacki.remotebrick.Hub;
import de.juniorjacki.remotebrick.types.Port;

public class UltrasonicSensor extends ConnectedDevice{
    public UltrasonicSensor(Hub deviceRoot, Port port) {
        super(deviceRoot, port, 62);
    }
}
