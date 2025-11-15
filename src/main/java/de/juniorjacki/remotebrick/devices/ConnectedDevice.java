/*
 * Copyright (c) 2025 JuniorJacki
 * All Rights Reserved
 */

package de.juniorjacki.remotebrick.devices;

import de.juniorjacki.remotebrick.Hub;
import de.juniorjacki.remotebrick.types.Port;
import de.juniorjacki.remotebrick.utils.SimpleJsonArray;


public abstract class ConnectedDevice {

    /**
     * Hub the Device is connected to
     */
    final Hub deviceRoot;

    /**
     * Port the Device is connected to
     */
    final Port port;

    /**
     * @return The Devicetype
     */
    public int getType() {
        return type;
    }

    /**
     * @return The Port teh Device is connected to
     */
    public Port getPort() {
        return port;
    }

    /**
     * Device Type
     */
    final int type;

    protected ConnectedDevice(Hub deviceRoot,Port port,int type) {
        this.deviceRoot = deviceRoot;
        this.port = port;
        this.type = type;
    }

    /**
     * Updates Dynamic Device Values
     * @param data Device Values
     */
    public void update(SimpleJsonArray data) {}

    /**
     * @return True if the Device is still connected to the Hub, else False
     */
    public boolean isFunctional() {
        if (deviceRoot.getDevice(port) == null) return false;
        return deviceRoot.getDevice(port).type == this.type;
    };



}
