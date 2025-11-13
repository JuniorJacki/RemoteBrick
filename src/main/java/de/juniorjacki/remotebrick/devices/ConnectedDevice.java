package de.juniorjacki.remotebrick.devices;

import de.juniorjacki.remotebrick.Hub;

public abstract class ConnectedDevice {

    final Hub deviceRoot;
    final byte port;
    final int type;

    public ConnectedDevice(Hub deviceRoot,byte port,int type) {
        this.deviceRoot = deviceRoot;
        this.port = port;
        this.type = type;
    }

    public byte getPortIndex() {
        return port;
    }

    public char getPort() {
        return switch (port) {
            case 0 -> 'A';
            case 1 -> 'B';
            case 2 -> 'C';
            case 3 -> 'D';
            case 4 -> 'E';
            default -> 'F';
        };
    }

    public boolean isFunctional() {
        return true;
    };


}
