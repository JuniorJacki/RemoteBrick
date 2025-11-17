package de.juniorjacki.remotebrick.types;

import de.juniorjacki.remotebrick.Hub;
import de.juniorjacki.remotebrick.devices.Motor;
import de.juniorjacki.remotebrick.utils.JsonBuilder;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

public class DualMotorCommand extends Command {
    final Motor motor0;
    final int motor0Target;
    final Motor motor1;
    final int motor1Target;

    private int variance = 5;
    final Motor.SubscribedValue.Type type;

    /**
     * Constructs a new command.
     *
     * @param hub            The {@link Hub} instance to send through.
     * @param commandPayload The command data (without {@code i} field).
     */
    public DualMotorCommand(Hub hub, JsonBuilder commandPayload, Motor motor0, int motor0Target, Motor motor1, int motor1Target, Motor.SubscribedValue.Type type) {
        super(hub, commandPayload);
        this.motor0 = motor0;
        this.motor0Target = motor0Target;
        this.motor1 = motor1;
        this.motor1Target = motor1Target;
        this.type = type;
    }

    public void setVariance(int variance) {
        this.variance = variance;
    }

    @Override
    public String send() {
        return send(0,TimeUnit.MILLISECONDS);
    }

    @Override
    public String send(long timeout, TimeUnit unit) {
        String identifier = push();
        if (identifier == null) return null;
        CompletableFuture<Integer> subscriber0 = motor0.newSubscriber(type, motor0Target, variance);
        CompletableFuture<Integer> subscriber1 = motor1.newSubscriber(type, motor1Target, variance);
        try {
            if (timeout > 0) {
                return CompletableFuture.anyOf(CompletableFuture.allOf(subscriber0,subscriber1), hub.getListenerService().queueTaskResult(identifier)).get(timeout,unit).toString();
            } else {
                return CompletableFuture.anyOf(CompletableFuture.allOf(subscriber0,subscriber1), hub.getListenerService().queueTaskResult(identifier)).get().toString();
            }
        } catch (Exception ignored) {
            return null;
        } finally {
            motor0.deleteSubscriber(type,subscriber0);
            motor1.deleteSubscriber(type,subscriber1);
        }
    }

}
