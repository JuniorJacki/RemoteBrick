/*
 * Copyright (c) 2025 JuniorJacki
 * All Rights Reserved
 */

package de.juniorjacki.remotebrick.types;

import de.juniorjacki.remotebrick.Hub;
import de.juniorjacki.remotebrick.devices.Motor;
import de.juniorjacki.remotebrick.utils.JsonBuilder;

public record CommandContext(String method, JsonBuilder payload) {
    public Command generateCommand(Hub hub) {
        return new Command(hub, JsonBuilder.object().add("m", method).addObject("p", payload != null ? payload : JsonBuilder.object()));
    }

    public MotorCommand generateMotorCommand(Hub hub, Motor motor, Motor.SubscribedValue.Type cmdType, int target) {
        return new MotorCommand(hub, JsonBuilder.object().add("m", method).addObject("p", payload != null ? payload : JsonBuilder.object()), motor,cmdType, target);
    }

    public DualMotorCommand generateDualMotorCommand(Hub hub, Motor motor0,int motor0Target,Motor motor1, int motor1Target, Motor.SubscribedValue.Type cmdType) {
        return new DualMotorCommand(hub, JsonBuilder.object().add("m", method).addObject("p", payload != null ? payload : JsonBuilder.object()), motor0,motor0Target,motor1,motor1Target,cmdType);
    }
}