package de.juniorjacki.remotebrick.devices;

import de.juniorjacki.remotebrick.Hub;
import de.juniorjacki.remotebrick.types.Port;
import de.juniorjacki.remotebrick.utils.SimpleJsonArray;


public abstract class ConnectedDevice {

    final Hub deviceRoot;
    final Port port;

    public int getType() {
        return type;
    }

    final int type;

    public ConnectedDevice(Hub deviceRoot,Port port,int type) {
        this.deviceRoot = deviceRoot;
        this.port = port;
        this.type = type;
    }

    public void update(SimpleJsonArray data) {}


    public Port getPort() {
       return port;
    }

    public boolean isFunctional() {
        if (deviceRoot.getDevice(port) == null) return false;
        return deviceRoot.getDevice(port).type == this.type;
    };



}
