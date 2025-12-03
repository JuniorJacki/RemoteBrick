/*
 * Copyright (c) 2025 JuniorJacki
 * All Rights Reserved
 */

package de.juniorjacki.remotebrick.devices;

import de.juniorjacki.remotebrick.Hub;
import de.juniorjacki.remotebrick.types.*;
import de.juniorjacki.remotebrick.utils.JsonBuilder;
import de.juniorjacki.remotebrick.utils.SimpleJsonArray;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Represents a motor connected to a LEGO Inventor Hub.
 * <p>
 * Provides real-time feedback via encoder: speed, position, relative position, and power.
 * All control commands are executed through the {@link MotorControl} inner class.
 * </p>
 * <p>
 * Use {@link #getControl()} to issue movement commands.
 * </p>
 *
 * @see MotorControl
 * @see StopType
 * @see PathDirection
 */
public class Motor extends ConnectedDevice<Motor.MotorDataType>{

    public enum MotorDataType implements DataType {
        RelativePosition,
        AbsolutePosition,
        Speed,
        Power
    }



    public record SubscribedValue(Motor motor, int target, int variance, CompletableFuture<Integer> callback){
        private boolean check(int curPosition) {
            if (this.target +variance >= curPosition && this.target -variance <= curPosition) {
                complete(curPosition+variance);
                return true;
            }
            return false;
        }

        private void complete(int curPosition) {
            callback.complete(curPosition);
        }

        public enum Type {
            RelativePosition,
            AbsolutePosition,
            Speed,
            Power
        }
    };



    CopyOnWriteArrayList<SubscribedValue> subscribedRelativePosition =  new CopyOnWriteArrayList<>();
    CopyOnWriteArrayList<SubscribedValue> subscribedSpeed = new CopyOnWriteArrayList<>();
    CopyOnWriteArrayList<SubscribedValue> subscribedPower = new CopyOnWriteArrayList<>();
    CopyOnWriteArrayList<SubscribedValue> subscribedAbsolutePosition = new CopyOnWriteArrayList<>();

    private void updatePowerSubscriber(int currentPower) {
        subscribedPower.removeIf(s -> s.check(currentPower));
    }

    private void updateSpeedSubscriber(int currentSpeed) {
        subscribedSpeed.removeIf(s -> s.check(currentSpeed));
    }

    private void updateRelativePositionSubscriber(int currentPosition) {
        subscribedRelativePosition.removeIf(s -> s.check(currentPosition));
    }

    private void updateAbsolutePositionSubscriber(int currentPosition) {
        subscribedAbsolutePosition.removeIf(s -> s.check(currentPosition));
    }


    public CompletableFuture<Integer> newSubscriber(SubscribedValue.Type type,int target, int variance){
        CompletableFuture<Integer> newCallback = new CompletableFuture<>();
        switch (type){
            case RelativePosition -> subscribedRelativePosition.add(new SubscribedValue(this, target, variance, newCallback));
            case Speed -> subscribedSpeed.add(new SubscribedValue(this, target, variance, newCallback));
            case Power -> subscribedPower.add(new SubscribedValue(this, target, variance, newCallback));
            case AbsolutePosition ->  subscribedAbsolutePosition.add(new SubscribedValue(this, target, variance, newCallback));
        }
        return newCallback;
    }

    public void deleteSubscriber(SubscribedValue.Type type,CompletableFuture<Integer> callback){
        switch (type){
            case RelativePosition -> subscribedRelativePosition.removeIf(s -> s.callback.equals(callback));
            case AbsolutePosition -> subscribedAbsolutePosition.removeIf(s -> s.callback.equals(callback));
            case Power -> subscribedPower.removeIf(s -> s.callback.equals(callback));
            case Speed -> subscribedSpeed.removeIf(s -> s.callback.equals(callback));
        }
    }

    private void setSpeed(int speed) {
        updateSpeedSubscriber(speed);
        this.speed = speed;
    }

    private void setRelativePosition(int relativePosition) {
        updateRelativePositionSubscriber(relativePosition);
        this.relativePosition = relativePosition;
    }

    private void setPosition(int position) {
        updateAbsolutePositionSubscriber(position);
        this.position = position;
    }

    private void setPower(int power) {
        updatePowerSubscriber(power);
        this.power = power;
    }



    /**
     * Current motor speed in percent.
     * <p><strong>Range:</strong> -100 to 100 (percent of max speed)<br>
     */
    private int speed = 0;

    /**
     * Current relative position in degrees.
     * <p>
     * Position from power-on or last {@link MotorControl#setPosition(int)}.
     * </p>
     */
    private int relativePosition = 0;

    /**
     * Current absolute position in degrees.
     * <p><strong>Range:</strong> Unlimited (integer); wraps conceptually at 180° but tracks full rotation count</p>
     */
    private int position = 0;

    /**
     * Current motor power output
     * <p>
     * Represents direct power applied to motor, not regulated speed.
     * </p>
     * <p><strong>Range:</strong> -100 to 100 (percent)<br>
     */
    private int power = 0;

    /**
     * Returns the current motor speed.
     *
     * @return Speed in percent ({@code -100} to {@code 100}).
     */
    public int getSpeed() {
        return speed;
    }

    /**
     * Returns the current relative position.
     *
     * @return Relative position in degrees.
     */
    public int getRelativePosition() {
        return relativePosition;
    }

    /**
     * Returns the current absolute position.
     *
     * @return Absolute position in degrees.
     */
    public int getPosition() {
        return position;
    }

    /**
     * Returns the current motor power.
     *
     * @return Power in percent ({@code -100} to {@code 100}).
     */
    public int getPower() {
        return power;
    }

    /**
     * @return Controls for the Motor
     */
    public MotorControl getControl() {
        return control;
    }


    private final MotorControl control = new MotorControl(this);
    public Motor(Hub deviceRoot, Port port) {
        super(deviceRoot, port,75);
    }

    @Override
    public Object parseData(SimpleJsonArray data, MotorDataType type) {
        return switch (type) {
            case Speed -> data.optInt(0);
            case RelativePosition -> data.optInt(1);
            case AbsolutePosition -> data.optInt(2);
            case Power -> data.optInt(3);
        };
    }

    @Override
    public List<MotorDataType> update(SimpleJsonArray array) {
        List<MotorDataType> dataTypes = new ArrayList<>();
        if (array != null) {
            int newData = array.optInt(0);
            if (newData != getSpeed()) {setSpeed(newData); dataTypes.add(MotorDataType.Speed);}
            newData = array.optInt(1);
            if (newData != getRelativePosition()) {setRelativePosition(newData); dataTypes.add(MotorDataType.RelativePosition);}
            newData = array.optInt(2);
            if (newData != getPosition()) { setPosition(newData); dataTypes.add(MotorDataType.AbsolutePosition);}
            newData = array.optInt(3);
            if (newData != getPower()) {setPower(newData); dataTypes.add(MotorDataType.Power);}
        }
        return dataTypes;
    }

    /**
     * Provides control commands for the motor.
     * <p>
     * All methods return a {@link Command} that can be executed in Thread or Async.
     * Commands are only generated if the motor is {@linkplain ConnectedDevice#isFunctional() functional}.
     * </p>
     *
     * @see Command
     * @see CommandContext
     */
    public class MotorControl {

        final Motor motorref;
        private MotorControl (Motor ref) {
            this.motorref = ref;
        }

        /**
         * Runs the motor for a specified number of degrees.
         * <p>
         * The motor rotates at the given speed until the target degree count is reached.
         * Stall detection can be enabled, and stop behavior, acceleration, and deceleration
         * can be precisely controlled.
         * </p>
         * <p><strong>Parameter Ranges:</strong></p>
         * <ul>
         * <li>{@code speed}: -100 to 100 (percent; positive = forward, negative = backward)</li>
         * <li>{@code degrees}: Integer value (no strict max; limited by encoder precision ~0.1°)</li>
         * <li>{@code stall}: {@code true} to enable stall detection (motor stops if stalled, e.g., high torque & zero speed)</li>
         * <li>{@code stopType}: Type of stop action after reaching the position (e.g., COAST, BRAKE, HOLD). See {@link StopType}.</li>
         * <li>{@code acceleration}: 0 to 100 (percent of max acceleration per second; 0 = instant)</li>
         * <li>{@code deceleration}: 0 to 100 (percent of max deceleration per second; 0 = instant)</li>
         * </ul>
         *
         * @param speed         Motor speed in percent ({@code -100} to {@code 100}).
         *                      Positive values = forward, negative = backward.
         * @param degrees       Number of degrees the motor should rotate.
         * @param stall         {@code true} to enable stall detection (motor stops if blocked).
         * @param stopType      Type of stop action after reaching the position.
         *                      See {@link StopType}.
         * @param acceleration  Acceleration rate in percent per second ({@code 0} = instant).
         * @param deceleration  Deceleration rate in percent per second ({@code 0} = instant).
         * @return An executable {@link Command} object, or {@code null} if the motor is not functional.
         * @see StopType
         */
        public MotorCommand runForDegrees(int speed, int degrees, boolean stall, StopType stopType, int acceleration, int deceleration) {
            if (isFunctional()) {
                return new CommandContext("scratch.motor_run_for_degrees",JsonBuilder.object().add("port",port.name()).add("speed",speed).add("degrees",degrees).add("stall",stall).add("stop",stopType.ordinal()).add("acceleration",acceleration).add("deceleration",deceleration)).generateMotorCommand(deviceRoot,motorref, SubscribedValue.Type.RelativePosition,getRelativePosition()+degrees);
            }
            return null;
        }

        /**
         * Runs the motor for a specified duration in milliseconds.
         * <p>
         * The motor operates at the given speed for the defined time period.
         * Supports stall detection and customizable stop behavior.
         * </p>
         * <p><strong>Parameter Ranges:</strong></p>
         * <ul>
         * <li>{@code speed}: -100 to 100 (percent)</li>
         * <li>{@code time}: Positive long value in ms (practical max ~86,400,000 ms/day)</li>
         * <li>{@code stall}: {@code true} to enable stall detection</li>
         * <li>{@code stopType}: See {@link StopType} (e.g., COAST: free spin, BRAKE: active stop, HOLD: maintain position)</li>
         * <li>{@code acceleration}: 0 to 100 (%/s)</li>
         * <li>{@code deceleration}: 0 to 100 (%/s)</li>
         * </ul>
         *
         * @param speed         Motor speed in percent ({@code -100} to {@code 100}).
         * @param time          Duration in milliseconds to run the motor.
         * @param stall         {@code true} to enable stall detection.
         * @param stopType      Stop action after time expires. See {@link StopType}.
         * @param acceleration  Acceleration rate in percent per second.
         * @param deceleration  Deceleration rate in percent per second.
         * @return An executable {@link Command}, or {@code null} if not functional.
         * @see StopType
         */
        public Command runTimed(int speed, long time, boolean stall, StopType stopType, int acceleration, int deceleration) {
            if (isFunctional()) {
                return new CommandContext("scratch.motor_run_timed",JsonBuilder.object().add("port",port.name()).add("speed",speed).add("time",time).add("stall",stall).add("stop",stopType.ordinal()).add("acceleration",acceleration).add("deceleration",deceleration)).generateCommand(deviceRoot);
            }
            return null;
        }

        /**
         * Starts the motor at a constant speed without a stopping condition.
         * <p>
         * The motor runs indefinitely until {@link #stop(StopType, int)} is called.
         * </p>
         * <p><strong>Parameter Ranges:</strong></p>
         * <ul>
         * <li>{@code speed}: -100 to 100 (%)</li>
         * <li>{@code stall}: {@code true} to enable stall detection</li>
         * <li>{@code acceleration}: 0 to 100 (%/s)</li>
         * </ul>
         *
         * @param speed         Motor speed in percent ({@code -100} to {@code 100}).
         * @param stall         {@code true} to enable stall detection.
         * @param acceleration  Acceleration rate in percent per second.
         * @return An executable {@link Command}, or {@code null} if not functional.
         */
        public MotorCommand start(int speed, boolean stall, int acceleration) {
            if (isFunctional()) {
                return new CommandContext("scratch.motor_start",JsonBuilder.object().add("port",port.name()).add("speed",speed).add("stall",stall).add("acceleration",acceleration)).generateMotorCommand(deviceRoot,motorref, SubscribedValue.Type.Speed,speed);
            }
            return null;
        }


        /**
         * Stops the motor with the specified stop behavior and deceleration.
         * <p>
         * Supported stop types: COAST (freewheel), BRAKE (active resistance), HOLD (maintain position with power).
         * </p>
         * <p><strong>Parameter Ranges:</strong></p>
         * <ul>
         * <li>{@code stopType}: Enum value from {@link StopType}</li>
         * <li>{@code deceleration}: 0 to 100 (%/s; 0 = instant stop)</li>
         * </ul>
         *
         * @param stopType      Type of stop action. See {@link StopType}.
         * @param deceleration  Deceleration rate in percent per second ({@code 0} = instant).
         * @return An executable {@link Command}, or {@code null} if not functional.
         * @see StopType
         */
        public MotorCommand stop(StopType stopType,int deceleration) {
            if (isFunctional()) {
                return new CommandContext("scratch.motor_stop",JsonBuilder.object().add("port",port.name()).add("stop",stopType.ordinal()).add("deceleration",deceleration)).generateMotorCommand(deviceRoot,motorref, SubscribedValue.Type.Speed,0);
            }
            return null;
        }

        /**
         * Sets the motor to run at a specific PWM power level.
         * <p><strong>Parameter Ranges:</strong></p>
         * <ul>
         * <li>{@code power}: -100 to 100 (% duty cycle)</li>
         * <li>{@code stall}: {@code true} to enable stall detection</li>
         * <li>{@code acceleration}: 0 to 100 (%/s)</li>
         * </ul>
         *
         * @param power         PWM power level ({@code -100} to {@code 100}).
         * @param stall         {@code true} to enable stall detection.
         * @param acceleration  Acceleration rate in percent per second.
         * @return An executable {@link Command}, or {@code null} if not functional.
         */
        public MotorCommand pwm(int power, boolean stall, int acceleration) {
            if (isFunctional()) {
                return new CommandContext("scratch.motor_pwm",JsonBuilder.object().add("port",port.name()).add("power",power).add("stall",stall).add("acceleration",acceleration)).generateMotorCommand(deviceRoot,motorref, SubscribedValue.Type.Power,power);
            }
            return null;
        }

        /**
         * Resets or sets the motor's current position (encoder offset).
         * <p>
         * This defines the zero point for relative position.
         * </p>
         * <p><strong>Parameter Ranges:</strong></p>
         * <ul>
         * <li>{@code offset}: Integer degrees (no max; wraps at 360° for absolute, but tracks cumulatively)</li>
         * </ul>
         *
         * @param offset        New position offset in degrees.
         * @return An executable {@link Command}, or {@code null} if not functional.
         */
        public Command setPosition(int offset) {
            if (isFunctional()) {
                // TODO DEBUG Potential LOCK
                if (!motorref.subscribedRelativePosition.isEmpty()) return null;
                return new CommandContext("scratch.motor_set_position",JsonBuilder.object().add("port",port.name()).add("offset",offset)).generateCommand(deviceRoot);
            }
            return null;
        }

        /**
         * Moves the motor to a relative position from the current location.
         * <p>
         * Target is relative to current encoder position.
         * </p>
         * <p><strong>Parameter Ranges:</strong></p>
         * <ul>
         * <li>{@code position}: Integer degrees relative</li>
         * <li>{@code speed}: -100 to 100 (%)</li>
         * <li>{@code stall}: {@code true} to enable</li>
         * <li>{@code stopType}: See {@link StopType}</li>
         * <li>{@code acceleration}: 0 to 100 (%/s)</li>
         * <li>{@code deceleration}: 0 to 100 (%/s)</li>
         * </ul>
         *
         * @param position      Target position relative to current (in degrees).
         * @param speed         Motor speed in percent.
         * @param stall         {@code true} to enable stall detection.
         * @param stopType      Stop behavior on arrival. See {@link StopType}.
         * @param acceleration  Acceleration rate.
         * @param deceleration  Deceleration rate.
         * @return An executable {@link Command}, or {@code null} if not functional.
         * @see StopType
         */
        public MotorCommand goToRelativePosition(int position,int speed, boolean stall, StopType stopType, int acceleration, int deceleration) {
            if (isFunctional()) {
                return new CommandContext("scratch.motor_go_to_relative_position",JsonBuilder.object().add("port",port.name()).add("position",position).add("speed",speed).add("stall",stall).add("stop",stopType.ordinal()).add("acceleration",acceleration).add("deceleration",deceleration)).generateMotorCommand(deviceRoot,motorref, SubscribedValue.Type.RelativePosition,position);
            }
            return null;
        }

        /**
         * Moves the motor to an absolute position with explicit direction control.
         * <p>
         * Allows specifying whether to take the shortest path or force a direction.
         * </p>
         * <p><strong>Parameter Ranges:</strong></p>
         * <ul>
         * <li>{@code position}: Integer absolute degrees</li>
         * <li>{@code speed}: -100 to 100 (%)</li>
         * <li>{@code direction}: Enum from {@link PathDirection} (e.g., CLOCKWISE, COUNTERCLOCKWISE, SHORTEST)</li>
         * <li>{@code stall}: {@code true} to enable</li>
         * <li>{@code stopType}: See {@link StopType}</li>
         * <li>{@code acceleration}: 0 to 100 (%/s)</li>
         * <li>{@code deceleration}: 0 to 100 (%/s)</li>
         * </ul>
         *
         * @param targetPosition      Absolute target position in degrees.
         * @param speed         Motor speed in percent.
         * @param direction     Forced rotation direction. See {@link PathDirection}.
         * @param stall         {@code true} to enable stall detection.
         * @param stopType      Stop behavior on arrival. See {@link StopType}.
         * @param acceleration  Acceleration rate.
         * @param deceleration  Deceleration rate.
         * @return An executable {@link Command}, or {@code null} if not functional.
         * @see PathDirection
         * @see StopType
         */
        public MotorCommand goToPositionWithDirection(int targetPosition, int speed, PathDirection direction, boolean stall, StopType stopType, int acceleration, int deceleration) {
            if (isFunctional()) {
                return new CommandContext("scratch.motor_go_direction_to_position",JsonBuilder.object().add("port",port.name()).add("position",targetPosition).add("speed",speed).add("direction",direction.name().toLowerCase()).add("stall",stall).add("stop",stopType.ordinal()).add("acceleration",acceleration).add("deceleration",deceleration)).generateMotorCommand(deviceRoot,motorref, SubscribedValue.Type.AbsolutePosition,targetPosition);
            }
            return null;
        }
    }
}
