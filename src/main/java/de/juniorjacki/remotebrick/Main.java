package de.juniorjacki.remotebrick;

import de.juniorjacki.remotebrick.devices.ColorSensor;
import de.juniorjacki.remotebrick.devices.ConnectedDevice;
import de.juniorjacki.remotebrick.devices.Motor;
import de.juniorjacki.remotebrick.devices.UltrasonicSensor;
import de.juniorjacki.remotebrick.types.*;

public class Main {

    public static void main(String[] args) throws InterruptedException {
        Hub.addListener(new Hub.BrickListener() {
            @Override
            public void newHubConnected(Hub hub) {
                System.out.println("Successfully connected to Hub " + hub.getMacAddress() + " battery:" + hub.getBatteryPercentage());

                hub.getControl().display.text("Verbunden").sendAsync();
                hub.getControl().display.buttonLight(4).send();
                hub.getControl().display.animation(Animation.BLINK,false,100,2,true).sendAsync();

                hub.getListenerService().addListener(new Hub.Listener.HubEventListener() {
                    @Override
                    public void newDeviceConnected(ConnectedDevice device) {
                        System.out.println("New Device Connected Port " + device.getPort() + " Type:" + device.getType());
                    }

                    @Override
                    public void deviceDisconnected(ConnectedDevice device) {
                        System.out.println("Device Disconnected Port " + device.getPort() + " Type:" + device.getType());
                    }

                    @Override
                    public void hubWasKnocked() {
                        System.out.println("KNOCK! Hub was tapped!");
                    }

                    @Override
                    public void hubChangedState(HubState newState) {
                        System.out.println("New Hubstate " + newState.name());
                    }

                    @Override
                    public void hubButtonPressed(HubButton button) {
                        System.out.println("Button " + button.name() +" pressed");
                    }

                    @Override
                    public void hubButtonReleased(HubButton button, long duration) {
                        System.out.println("Button " + button.name() +" held: " + duration + "ms");
                    }

                    @Override
                    public void receivedBroadcastMessage(long hash, String message) {
                        System.out.println("Received broadcast message: " + message);
                    }
                });
            }

            @Override
            public void hubDisconnected(Hub hub) {
                System.out.println(hub.getMacAddress() + " Disconnected");
            }
        });
        new Thread(() -> {Hub.connect("A8:E2:C1:9C:91:02");}).start();
    }


}
