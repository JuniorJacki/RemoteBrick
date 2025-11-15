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
                hub.getControl().display.animation(new Animation().addImage(new Image().setPixel(0,1,6)).addImage(new Image().setPixel(1,2,9)),false,1000,5,true).sendAsync();


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


                hub.getListenerService().addListener(new Hub.Listener.HubEventListener() {
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

                        if (device instanceof ColorSensor csensor) {
                           csensor.getControl().setDeviceMode(ColorSensorMode.RAW).send();
                            try {
                                Thread.sleep(5000);
                            } catch (InterruptedException e) {
                                throw new RuntimeException(e);
                            }
                            csensor.getControl().setDeviceMode(ColorSensorMode.TUPLES).send();
                            System.out.println(csensor.getRed() + " " + csensor.getGreen() + " " + csensor.getBlue());
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
                            hub.getControl().display.text("STOP").sendAsync();
                            hub.getControl().display.buttonLight(2).send();
                            hub.getDevices().forEach(connectedDevice ->
                            {
                                if (connectedDevice instanceof Motor motor) {
                                    System.out.println(connectedDevice.getPort());
                                    motor.getControl().stop(StopType.BRAKE,10).sendAsync();
                                }
                            });
                            on = false;
                        } else {
                            hub.getControl().display.text("START").sendAsync();
                            hub.getControl().display.buttonLight(5).send();

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
                        if (button == HubButton.LEFT) {
                            hub.getControl().broadcastSignal(4260241449L,"Ente").sendAsync();
                        }
                        System.out.println(hub.getMacAddress() +" Pressed: " + button.name());
                    }

                    @Override
                    public void hubButtonReleased(HubButton button, long duration) {
                        System.out.println(hub.getMacAddress() +" Released: " + button.name() + " duration: " + duration);
                    }

                    @Override
                    public void receivedBroadcastMessage(long hash, String message) {
                        System.out.println(hub.getMacAddress() +" Received broadcast message: " + message + " with Hash" + hash);
                    }
                });
            }

            @Override
            public void hubDisconnected(Hub hub) {
                System.out.println(hub.getMacAddress() + " Disconnected");
            }
        });
        new Thread(() -> {Hub.connect("A8:E2:C1:9C:91:02");}).start();
        //new Thread(() -> {Hub.connect("A8:E2:C1:9C:96:DF");}).start();

    }


}
