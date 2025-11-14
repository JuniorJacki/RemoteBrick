package de.juniorjacki.remotebrick;

import de.juniorjacki.remotebrick.devices.ConnectedDevice;
import de.juniorjacki.remotebrick.devices.Motor;
import de.juniorjacki.remotebrick.types.Image;
import de.juniorjacki.remotebrick.types.Port;
import de.juniorjacki.remotebrick.types.StopType;

public class Main {

    public static void main(String[] args) throws InterruptedException {
        Hub hub = Hub.connect("A8:E2:C1:9C:91:02");

        if (hub != null) {
            System.out.println("Successfully connected to Hub");


            hub.getListener().addListener(new Hub.Listener.HubEventListener() {
                @Override
                public void newDeviceConnected(ConnectedDevice device) {
                    System.out.println("New Device Connected type " + device.getType() + " port "+ device.getPort());


                }

                @Override
                public void deviceDisconnected(ConnectedDevice device) {
                    System.out.println("Device Disconnected type " + device.getType() + " port "+ device.getPort());
                }
            });


        }
    }


}
