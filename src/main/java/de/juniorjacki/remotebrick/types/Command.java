/*
 * Copyright (c) 2025 JuniorJacki
 * All Rights Reserved
 */

package de.juniorjacki.remotebrick.types;

import de.juniorjacki.remotebrick.Hub;
import de.juniorjacki.remotebrick.utils.JsonBuilder;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

public class Command {
    private final Hub hub;
    private final String identifier;
    private final JsonBuilder commandPayload;

    public Command(Hub hub, String identifier, JsonBuilder commandPayload) {
        this.hub = hub;
        this.identifier = identifier;
        this.commandPayload = commandPayload;
    }

    private boolean push() {
        return hub.send(new JsonBuilder().add("i", identifier).add(commandPayload).toString()) == 0;
    }


    public CompletableFuture<String> sendAsync() {
        if (!push()) return CompletableFuture.completedFuture(null);
        return hub.getListener().queueTaskResult(identifier);
    }

    public String send() {
        if (!push()) return null;
        try {
            return hub.getListener().queueTaskResult(identifier).get();
        } catch (Exception ignored) {
            return null;
        }
    }

    public String send(long timeout, TimeUnit unit) {
        if (!push()) return null;
        try {
            return hub.getListener().queueTaskResult(identifier).get(timeout, unit);
        } catch (Exception ignored) {
            return null;
        }
    }

}