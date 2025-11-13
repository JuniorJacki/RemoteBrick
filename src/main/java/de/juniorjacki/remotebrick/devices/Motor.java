package de.juniorjacki.remotebrick.devices;

import de.juniorjacki.remotebrick.Hub;

public class Motor extends ConnectedDevice{


    public Motor(Hub deviceRoot, byte port) {
        super(deviceRoot, port,75);
    }
}
