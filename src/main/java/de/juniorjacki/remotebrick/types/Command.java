/*
 * Copyright (c) 2025 JuniorJacki
 * All Rights Reserved
 */

package de.juniorjacki.remotebrick.types;

import de.juniorjacki.remotebrick.Hub;
import de.juniorjacki.remotebrick.utils.JsonBuilder;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

/**
 * Represents an executable command for the LEGO SPIKE Prime / Inventor Hub.
 * <p>
 * Commands are created by device classes (e.g., {@link de.juniorjacki.remotebrick.devices.Motor},
 * {@link de.juniorjacki.remotebrick.devices.UltrasonicSensor}) and sent via the {@link Hub}.
 * Each command has a dynamically generated unique identifier and a JSON payload compatible with
 * Scratch-style commands.
 * </p>
 *
 * <p><strong>Command Lifecycle:</strong></p>
 * <ol>
 *   <li><strong>Construction:</strong> Via {@code CommandContext.generateCommand(Hub)}</li>
 *   <li><strong>Transmission:</strong> Serialized and sent to the hub</li>
 *   <li><strong>Execution:</strong> Hub processes command and returns result (if any)</li>
 *   <li><strong>Response:</strong> Captured via {@link Hub#getListenerService()} and delivered via {@link CompletableFuture}</li>
 * </ol>
 *
 * <p><strong>Thread Safety:</strong></p>
 * <ul>
 *   <li>Commands are <strong>immutable</strong> and safe to share</li>
 *   <li>Sending methods ({@link #send()}, {@link #sendAsync()}) are <strong>thread-safe</strong></li>
 *   <li>Multiple sends of the same command instance will retransmit with <strong>new identifiers</strong></li>
 * </ul>
 *
 * <p><strong>Examples:</strong></p>
 * <pre>
 * // Synchronous with timeout
 * String result = motor.getControl().runForDegrees(50, 360, true, StopType.BRAKE, 50, 50)
 *                        .send(2, TimeUnit.SECONDS);
 *
 * // Asynchronous
 * motor.getControl().start(75, false, 100)
 *       .sendAsync()
 *       .thenAccept(response -> System.out.println("Motor started: " + response));
 * </pre>
 *
 * @see Hub
 * @see CommandContext
 * @see de.juniorjacki.remotebrick.devices.ConnectedDevice
 */
public class Command {
    /** The hub instance used for sending and receiving. */
    private final Hub hub;
    /** JSON payload containing the actual Scratch command and parameters. */
    private final JsonBuilder commandPayload;

    /**
     * Constructs a new command.
     *
     * @param hub            The {@link Hub} instance to send through.
     * @param commandPayload The command data (without {@code i} field).
     */
    public Command(Hub hub, JsonBuilder commandPayload) {
        this.hub = hub;
        this.commandPayload = commandPayload;
    }

    /**
     * Sends the command to the hub and returns the generated identifier.
     * <p>
     * A unique identifier is generated via {@link Hub#getListenerService()#newTaskID()}.
     * The full JSON sent to the hub is:
     * <pre>
     * {"i": "generated-id", ...commandPayload}
     * </pre>
     * Transmission uses {@link Hub#send(String)}.
     * </p>
     *
     * @return The generated command identifier if transmission was successful, {@code null} otherwise.
     */
    private String push() {
        String identifier = hub.getListenerService().newTaskID();
        if (hub.send(JsonBuilder.object().add("i", identifier).addAll(commandPayload).toString()) == 0) {
            return identifier;
        }
        return null;
    }


    /**
     * Sends the command asynchronously and returns a {@link CompletableFuture}.
     * <p>
     * The future completes when the hub responds, times out, or fails.
     * </p>
     *
     * @return A {@link CompletableFuture} that completes with the hub's response string,
     *         or {@code null} if sending failed.
     */
    public CompletableFuture<String> sendAsync() {
        String identifier = push();
        if (identifier == null) return CompletableFuture.completedFuture(null);
        return hub.getListenerService().queueTaskResult(identifier);
    }

    /**
     * Sends the command and blocks until a response is received.
     * <p>
     * Uses the default timeout configured in the hub's listener.
     * </p>
     *
     * @return The hub's response string, or {@code null} if sending failed, timed out, or was interrupted.
     */
    public String send() {
        String identifier = push();
        if (identifier == null) return null;
        try {
            return hub.getListenerService().queueTaskResult(identifier).get();
        } catch (Exception ignored) {
            return null;
        }
    }

    /**
     * Sends the command with a custom timeout.
     * <p>
     * Blocks until response or timeout expires.
     * </p>
     *
     * @param timeout The maximum time to wait for a response.
     * @param unit    The time unit of the timeout.
     * @return The hub's response string, or {@code null} if sending failed or timed out.
     * @throws NullPointerException if {@code unit} is {@code null}.
     */
    public String send(long timeout, TimeUnit unit) {
        String identifier = push();
        if (identifier == null) return null;
        try {
            return hub.getListenerService().queueTaskResult(identifier).get(timeout, unit);
        } catch (Exception ignored) {
            return null;
        }
    }
}