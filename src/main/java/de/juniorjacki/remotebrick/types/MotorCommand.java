package de.juniorjacki.remotebrick.types;

import de.juniorjacki.remotebrick.Hub;
import de.juniorjacki.remotebrick.devices.Motor;
import de.juniorjacki.remotebrick.utils.JsonBuilder;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

public class MotorCommand extends Command {


    final Motor motor;
    final int target;
    private int variance = 5;
    final Motor.SubscribedValue.Type type;

    /**
     * Constructs a new command.
     *
     * @param hub            The {@link Hub} instance to send through.
     * @param commandPayload The command data (without {@code i} field).
     */
    public MotorCommand(Hub hub, JsonBuilder commandPayload, Motor motor,Motor.SubscribedValue.Type cmdType, int target) {
        super(hub, commandPayload);
        this.motor = motor;
        this.target = target;
        this.type = cmdType;
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
        CompletableFuture<Integer> subscriber = motor.newSubscriber(type, target, variance);
        try {
            if (timeout > 0) {
                return CompletableFuture.anyOf(subscriber, hub.getListenerService().queueTaskResult(identifier)).get(timeout,unit).toString();
            } else {
                return CompletableFuture.anyOf(subscriber, hub.getListenerService().queueTaskResult(identifier)).get().toString();
            }
        } catch (Exception ignored) {
            return null;
        } finally {
            motor.deleteSubscriber(type,subscriber);
        }
    }
}
