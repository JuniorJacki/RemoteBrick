package de.juniorjacki.remotebrick;

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

                hub.getHubControl().display.text("Verbunden").sendAsync();
                hub.getHubControl().display.buttonLight(4).send();

                hub.getDevices().forEach(connectedDevice ->
                {
                    if (connectedDevice instanceof Motor motor) {
                        System.out.println(connectedDevice.getPort());
                        motor.getControl().pwm(100,false,100).sendAsync();
                    }
                });



                hub.getListener().addListener(new Hub.Listener.HubEventListener() {
                    boolean on = true;
                    @Override
                    public void newDeviceConnected(ConnectedDevice device) {
                        System.out.println(hub.getMacAddress() +" New Device Connected type " + device.getType() + " port "+ device.getPort());

                        if (device instanceof Motor motor) {
                            motor.getControl().pwm(100,false,100).sendAsync();
                        }
                        if (device instanceof UltrasonicSensor usensor) {
                            usensor.getControl().lightUp(100,10,50,100).sendAsync();
                        }
                    }

                    @Override
                    public void deviceDisconnected(ConnectedDevice device) {
                        System.out.println(hub.getMacAddress() +" Device Disconnected type " + device.getType() + " port "+ device.getPort());
                    }

                    @Override
                    public void hubWasKnocked() {
                        if (on) {
                            System.out.println(hub.getMacAddress() + " Was Knocked");
                            hub.getHubControl().display.text("STOP").sendAsync();
                            hub.getHubControl().display.buttonLight(2).send();
                            hub.getDevices().forEach(connectedDevice ->
                            {
                                if (connectedDevice instanceof Motor motor) {
                                    System.out.println(connectedDevice.getPort());
                                    motor.getControl().stop(StopType.BRAKE,10).sendAsync();
                                }
                            });
                            on = false;
                        } else {
                            hub.getHubControl().display.text("START").sendAsync();
                            hub.getHubControl().display.buttonLight(5).send();

                            hub.getDevices().forEach(connectedDevice ->
                            {
                                if (connectedDevice instanceof Motor motor) {
                                    System.out.println(connectedDevice.getPort());
                                    motor.getControl().pwm(100,false,10).sendAsync();
                                }
                            });
                            on =  true;
                        }

                    }

                    @Override
                    public void hubChangedState(HubState newState) {
                        System.out.println(hub.getMacAddress() +" State: " + newState.name());
                    }

                    @Override
                    public void hubButtonPressed(HubButton button) {
                        System.out.println(hub.getMacAddress() +" Pressed: " + button.name());
                    }

                    @Override
                    public void hubButtonReleased(HubButton button, long duration) {
                        System.out.println(hub.getMacAddress() +" Released: " + button.name() + " duration: " + duration);
                    }
                });
            }

            @Override
            public void hubDisconnected(Hub hub) {
                System.out.println(hub.getMacAddress() + " Disconnected");
            }
        });

        new Thread(() -> {
            Hub.connect("A8:E2:C1:9C:91:02");
            }).start();
        new Thread(() -> {
            Hub.connect("A8:E2:C1:9C:96:DF");
            }).start();

    }


}
