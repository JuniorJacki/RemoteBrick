/*
 * Copyright (c) 2025 JuniorJacki
 * All Rights Reserved
 */

package de.juniorjacki.remotebrick.types;

import de.juniorjacki.remotebrick.Hub;
import de.juniorjacki.remotebrick.utils.JsonBuilder;

public record CommandContext(String method, JsonBuilder payload) {
    public Command generateCommand(Hub hub) {
        return new Command(hub, hub.getListener().newTaskID(), new JsonBuilder().add("m", method).addObject("p", payload != null ? payload : new JsonBuilder()));
    }
}