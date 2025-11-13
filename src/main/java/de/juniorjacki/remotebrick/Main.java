package de.juniorjacki.remotebrick;

import de.juniorjacki.remotebrick.devices.Motor;

public class Main {

    public static void main(String[] args) throws InterruptedException {
        Hub hub = Hub.connect("A8:E2:C1:9C:91:02");

        if (hub != null) {
            System.out.println("Successfully connected to Hub");

            hub.getHubControl().sound.beep(80,200,1000).sendAsync();
            hub.getHubControl().display.image(new Hub.Control.Display.Image().setPixel(0,0,8).setPixel(0,1,5)).send();
            setSpeed(hub,500);
            setSpeed(hub,450);
            setSpeed(hub,400);
            setSpeed(hub,350);
            setSpeed(hub,300);
            setSpeed(hub,250);
            setSpeed(hub,200);
            setSpeed(hub,150);
            setSpeed(hub,100);
            setSpeed(hub,90);
            setSpeed(hub,80);
            setSpeed(hub,70);
            setSpeed(hub,60);
            setSpeed(hub,50);
            setSpeed(hub,40);
            setSpeed(hub,30);
            setSpeed(hub,20);
            setSpeed(hub,10);
            setSpeed(hub,0);
            hub.getHubControl().move.stop(new Motor(hub, (byte) 0),new Motor(hub, (byte) 1), Hub.Control.StopType.BRAKE).send();
            hub.getHubControl().display.clear().send();
            hub.disconnect();
        }
    }

    private static void setSpeed(Hub hub, int speed) throws InterruptedException {
        hub.getHubControl().move.startPowers(new Motor(hub, (byte) 0),new Motor(hub, (byte) 1),speed,speed,100).sendAsync();
        hub.getHubControl().display.text(String.valueOf(speed)).send();
        Thread.sleep(1000);
    }
}
