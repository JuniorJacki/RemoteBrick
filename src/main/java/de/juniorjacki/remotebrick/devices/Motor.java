/*
 * Copyright (c) 2025 JuniorJacki
 * All Rights Reserved
 */

package de.juniorjacki.remotebrick.devices;

import de.juniorjacki.remotebrick.Hub;
import de.juniorjacki.remotebrick.types.*;
import de.juniorjacki.remotebrick.utils.JsonBuilder;
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

    public MotorControl getControl() {
        return control;
    }

    private int speed = 0;
    private int relativePosition = 0;
    private int position = 0;
    private int power = 0;
    private final MotorControl control = new MotorControl();

    public Motor(Hub deviceRoot, Port port) {
        super(deviceRoot, port,75);
    }

    @Override
    public void update(SimpleJsonArray array) {
        if (array != null) {
            speed = array.optInt(0);
            relativePosition = array.optInt(1);
            position = array.optInt(2);
            power = array.optInt(3);
        }
    }

    public class MotorControl {
        public Command runForDegrees(int speed, int degrees, boolean stall, StopType stopType, int acceleration, int deceleration) {
            if (isFunctional()) {
                return new CommandContext("scratch.motor_run_for_degrees",JsonBuilder.object().add("port",port.name()).add("speed",speed).add("degrees",degrees).add("stall",stall).add("stop",stopType.ordinal()).add("acceleration",acceleration).add("deceleration",deceleration)).generateCommand(deviceRoot);
            }
            return null;
        }

        public Command runTimed(int speed, long time, boolean stall, StopType stopType, int acceleration, int deceleration) {
            if (isFunctional()) {
                return new CommandContext("scratch.motor_run_timed",JsonBuilder.object().add("port",port.name()).add("speed",speed).add("time",time).add("stall",stall).add("stop",stopType.ordinal()).add("acceleration",acceleration).add("deceleration",deceleration)).generateCommand(deviceRoot);
            }
            return null;
        }

        public Command start(int speed, boolean stall, int acceleration) {
            if (isFunctional()) {
                return new CommandContext("scratch.motor_start",JsonBuilder.object().add("port",port.name()).add("speed",speed).add("stall",stall).add("acceleration",acceleration)).generateCommand(deviceRoot);
            }
            return null;
        }

        public Command stop(StopType stopType,int deceleration) {
            if (isFunctional()) {
                return new CommandContext("scratch.motor_stop",JsonBuilder.object().add("port",port.name()).add("stop",stopType.ordinal()).add("deceleration",deceleration)).generateCommand(deviceRoot);
            }
            return null;
        }

        public Command pwm(int power, boolean stall, int acceleration) {
            if (isFunctional()) {
                return new CommandContext("scratch.motor_pwm",JsonBuilder.object().add("port",port.name()).add("power",power).add("stall",stall).add("acceleration",acceleration)).generateCommand(deviceRoot);
            }
            return null;
        }

        public Command setPosition(int offset) {
            if (isFunctional()) {
                return new CommandContext("scratch.motor_set_position",JsonBuilder.object().add("port",port.name()).add("offset",offset)).generateCommand(deviceRoot);
            }
            return null;
        }

        public Command goToRelativePosition(int position,int speed, boolean stall, StopType stopType, int acceleration, int deceleration) {
            if (isFunctional()) {
                return new CommandContext("scratch.motor_go_to_relative_position",JsonBuilder.object().add("port",port.name()).add("position",position).add("speed",speed).add("stall",stall).add("stop",stopType.ordinal()).add("acceleration",acceleration).add("deceleration",deceleration)).generateCommand(deviceRoot);
            }
            return null;
        }

        public Command goToPositionWithDirection(int position, int speed, PathDirection direction, boolean stall, StopType stopType, int acceleration, int deceleration) {
            if (isFunctional()) {
                return new CommandContext("scratch.motor_go_direction_to_position",JsonBuilder.object().add("port",port.name()).add("position",position).add("speed",speed).add("direction",direction.name().toLowerCase()).add("stall",stall).add("stop",stopType.ordinal()).add("acceleration",acceleration).add("deceleration",deceleration)).generateCommand(deviceRoot);
            }
            return null;
        }
    }
}
