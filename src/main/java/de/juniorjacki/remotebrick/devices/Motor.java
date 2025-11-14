package de.juniorjacki.remotebrick.devices;

import de.juniorjacki.remotebrick.Hub;
import de.juniorjacki.remotebrick.types.Port;
import de.juniorjacki.remotebrick.utils.JsonParser;
import de.juniorjacki.remotebrick.utils.SimpleJsonArray;

public class Motor extends ConnectedDevice{

    public int getSpeed() {
        return speed;
    }

    public int getRelativePosition() {
        return relativePosition;
    }

    public int getPosition() {
        return position;
    }

    public int getPower() {
        return power;
    }

    int speed = 0;
    int relativePosition = 0;
    int position = 0;
    int power = 0;

    public Motor(Hub deviceRoot, Port port) {
        super(deviceRoot, port,75);
    }

    @Override
    public void update(SimpleJsonArray array) {
        if (array != null) {
            speed = array.getInt(0);
            relativePosition = array.getInt(1);
            position = array.getInt(2);
            power = array.getInt(3);
        }
    }
}
