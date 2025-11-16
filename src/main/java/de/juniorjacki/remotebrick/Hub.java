/*
 * Copyright (c) 2025 JuniorJacki
 * All Rights Reserved
 */

package de.juniorjacki.remotebrick;

import de.juniorjacki.remotebrick.devices.ColorSensor;
import de.juniorjacki.remotebrick.devices.ConnectedDevice;
import de.juniorjacki.remotebrick.devices.Motor;
import de.juniorjacki.remotebrick.devices.UltrasonicSensor;
import de.juniorjacki.remotebrick.types.*;
import de.juniorjacki.remotebrick.types.Image;
import de.juniorjacki.remotebrick.utils.JsonBuilder;
import de.juniorjacki.remotebrick.utils.JsonParser;
import de.juniorjacki.remotebrick.utils.SimpleJson;
import de.juniorjacki.remotebrick.utils.SimpleJsonArray;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.*;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;


/**
 * Central class representing a connected LEGO Inventor Hub.
 * <p>
 * Manages Bluetooth communication,
 * display control, sound, movement, and event handling. All instances are tracked globally.
 * </p>
 *
 * <p><strong>Key Features:</strong></p>
 * <ul>
 *   <li>Automatic device detection (Motor, ColorSensor, UltrasonicSensor)</li>
 *   <li>Real-time sensor & hub state updates</li>
 *   <li>Display control with {@link Image}, {@link Animation}, text</li>
 *   <li>Sound playback, motor control, tank steering</li>
 *   <li>Event system via {@link BrickListener} and {@link Listener.HubEventListener}</li>
 *   <li>Thread-safe, async command execution via {@link Command}</li>
 * </ul>
 *
 * <p><strong>Connection Workflow:</strong></p>
 * <pre>
 * // 1. Pair hub via Bluetooth settings or Mindstorms app
 * Hub hub = Hub.connect("AA:BB:CC:DD:EE:FF");
 *
 * // 2. Use control methods
 * hub.getHubControl().display().image(Image.HEART).send();
 * </pre>
 *
 * @see HubControl
 * @see Listener
 * @see Command
 * @see ConnectedDevice
 */
public class Hub {

    /** Global list of all active hub instances. */
    static List<Hub> connectedHubs = new ArrayList<>();

    /**
     * @return List of All currently connected Lego Hubs
     */
    public static List<Hub> getConnectedHubs() {
        return java.util.Collections.unmodifiableList(connectedHubs);
    }

    static {
        try {
            String dll = "/HubConnector.dll";

            try (InputStream in = Hub.class.getResourceAsStream(dll)) {
                if (in == null) throw new IOException("DLL nicht gefunden: " + dll);

                File temp = File.createTempFile("hub_", ".dll");
                temp.deleteOnExit();
                Files.copy(in, temp.toPath(), StandardCopyOption.REPLACE_EXISTING);

                System.load(temp.getAbsolutePath());

            } catch (Exception e) {
                throw new UnsatisfiedLinkError("Konnte " + dll + " nicht laden: " + e.getMessage());
            }
        } catch (Exception e) {
            System.err.println("DLL nicht geladen: " + e.getMessage());
        }

        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            connectedHubs.forEach(hub ->  hub.disconnect(false));
            connectedHubs.clear();
        }));
    }

    /** Enables debug logging for native layer and packet parsing. */
    static boolean debugLogging = false;

    /**
     * Enables or disables debug logging.
     *
     * @param enable {@code true} to enable debug output.
     */
    public static void debugLogging(boolean enable) {
        debugLogging = enable;
    }

    /** Global listeners for hub connection events. */
    private static List<BrickListener> listeners = new ArrayList<BrickListener>();

    /**
     * Interface for global hub connection events.
     */
    public interface BrickListener {
        void newHubConnected(Hub hub);
        void hubDisconnected(Hub hub);
    }

    /**
     * Subscribes a listener to global hub events.
     *
     * @param listener The listener to add.
     */
    public static void addListener(BrickListener listener) {
        listeners.add(listener);
    }

    /**
     * Unsubscribes a listener from global hub events.
     *
     * @param listener The listener to remove.
     */
    public static void removeListener(BrickListener listener) {
        listeners.remove(listener);
    }

    /**
     * Connects to a LEGO Inventor Hub via Bluetooth.
     * <p>
     * The hub must be paired beforehand via system Bluetooth settings or the Mindstorms app.
     * </p>
     *
     * @param macAddress The Bluetooth MAC address of the hub (e.g., {@code "AA:BB:CC:DD:EE:FF"}).
     * @return A connected {@link Hub} instance, or {@code null} if connection failed.
     */
    public static Hub connect(String macAddress) {
        ByteBuffer h = connectNative(macAddress);
        if (h == null || !h.isDirect()) {
            return null;
        }
        try {
            Thread.sleep(2000);
        } catch (InterruptedException e) {}
        return new Hub(h,macAddress);
    }

    /**
     * Returns the control interface for display, sound, and movement.
     *
     * @return The {@link HubControl} instance.
     */
    public HubControl getControl() {
        return hubControl;
    }

    /**
     * Returns the event listener interface for hub and device events.
     *
     * @return The {@link Listener} instance.
     */
    public Listener getListenerService() {
        return hubListener;
    }


    // --- Power & Battery ---
    private final AtomicReference<Double> batteryVoltage = new AtomicReference<>(0.0);
    private final AtomicInteger batteryPercentage = new AtomicInteger(0);
    private final AtomicBoolean pluggedIn = new AtomicBoolean(false);

    /** @return Current battery voltage in volts (e.g., 7.4). */
    public Double getBatteryVoltage() { return batteryVoltage.get(); }

    /** @return Current battery charge level (0–100%). */
    public Integer getBatteryPercentage() { return batteryPercentage.get(); }

    /** @return {@code true} if hub is connected to power. */
    public Boolean isPluggedIn() { return pluggedIn.get(); }

    // HUBDATA
    private final AtomicInteger accelerationX = new AtomicInteger(0);
    private final AtomicInteger accelerationY = new AtomicInteger(0);
    private final AtomicInteger accelerationZ = new AtomicInteger(0);
    private final AtomicInteger rotationX = new AtomicInteger(0);
    private final AtomicInteger rotationY = new AtomicInteger(0);
    private final AtomicInteger rotationZ = new AtomicInteger(0);
    private final AtomicInteger yaw = new AtomicInteger(0);
    private final AtomicInteger pitch = new AtomicInteger(0);
    private final AtomicInteger roll = new AtomicInteger(0);
    private final AtomicLong programmTime = new AtomicLong(0);
    private final AtomicReference<HubState> state = new AtomicReference<>(HubState.Laying);
    private final AtomicReference<String> unknownData = new AtomicReference<>("");
    private final Map<Port, ConnectedDevice> connectedDevices = new ConcurrentHashMap<>();

    /** @return Current hub orientation state. */
    public HubState getHubState() {return state.get();}

    public int getAccelerationX() { return accelerationX.get(); }
    public int getAccelerationY() { return accelerationY.get(); }
    public int getAccelerationZ() { return accelerationZ.get(); }

    public int getRotationX() { return rotationX.get(); }
    public int getRotationY() { return rotationY.get(); }
    public int getRotationZ() { return rotationZ.get(); }

    public int getYaw() { return yaw.get(); }
    public int getPitch() { return pitch.get(); }
    public int getRoll() { return roll.get(); }

    /** @return Runtime of current program in milliseconds. */
    public long getProgrammTime() { return programmTime.get(); }

    /** @return Raw unknown data field from hub data Index 10. */
    public String getUnknownData() { return unknownData.get(); }

    /**
     * Returns all currently connected devices.
     *
     * @return Unmodifiable list of {@link ConnectedDevice} instances.
     */
    public List<ConnectedDevice> getDevices() {
        return List.copyOf(connectedDevices.values());
    }

    /**
     * Returns the device connected to a specific port.
     *
     * @param port The port (A–F).
     * @return The {@link ConnectedDevice}, or {@code null} if none.
     */
    public ConnectedDevice getDevice(Port port) {
        return connectedDevices.get(port);
    }

    /**
     * Control interface for hub-wide commands (display, sound, tank steering, etc.).
     */
    public class HubControl {
        final Hub hub;
        final Display display;
        final Sound  sound;
        final Move move;


        protected HubControl(Hub hub) {
            this.hub = hub;
            this.display = new Display(hub);
            this.sound = new Sound(hub);
            this.move = new Move(hub);
        }

        /**
         * Enables or disables broadcast message listening.
         *
         * @param enable {@code true} to listen.
         * @return A {@link Command} to send.
         */
        private Command listenBroadcast(boolean enable) {
            return new CommandContext("scratch.broadcast_listen", JsonBuilder.object().add("enable",enable)).generateCommand(hub);
        }

        /**
         * Sends a broadcast signal to the hub.
         *
         * @param hash  Channel identifier.
         * @param value Message content.
         * @return A {@link Command} to send.
         */
        public Command broadcastSignal(long hash,String value) {
            return new CommandContext("scratch.broadcast_signal", JsonBuilder.object().add("hash",hash).add("value",value)).generateCommand(hub);
        }

        /** @return Display control methods. */
        public Display display() {
            return this.display;
        }

        /** @return Sound control methods. */
        public Sound sound() {
            return this.sound;
        }

        /** @return Move control methods. */
        public Move move() {
            return move;
        }

        /** Tank-style movement control. */
        public class Move {
            final Hub hub;
            protected Move(Hub hub) {
                this.hub = hub;
            }

            /**
             * Drives two motors for a specific number of degrees.
             * @param motorL        Left motor.
             * @param motorR        Right motor.
             * @param lSpeed        Left speed (-100 to 100).
             * @param rSpeed        Right speed (-100 to 100).
             * @param degrees       Degrees to rotate.
             * @param stopType      Stop behavior.
             * @param acceleration  Acceleration (0–100).
             * @param deceleration  Deceleration (0–100).
             * @return A {@link Command}, or {@code null} if motors invalid.
             */
            public Command tankDegrees(Motor motorL, Motor motorR, int lSpeed, int rSpeed, int degrees, StopType stopType, int acceleration, int deceleration) {
                if (!(motorL.isFunctional() && motorR.isFunctional())) {
                    return null;
                }
                return new CommandContext("scratch.move_tank_degrees", JsonBuilder.object().add("lmotor",motorL.getPort()).add("rmotor",motorR.getPort()).add("lspeed",lSpeed).add("rspeed",rSpeed).add("degrees",degrees).add("stop",stopType.ordinal()).add("acceleration",acceleration).add("deceleration",deceleration)).generateCommand(hub);
            }

            /** Starts continuous movement with speed control.
             * @param motorL        Left motor.
             * @param motorR        Right motor.
             * @param lSpeed        Left speed (-100 to 100).
             * @param rSpeed        Right speed (-100 to 100).
             * @param acceleration  Acceleration (0–100).
             * @return A {@link Command}, or {@code null} if motors invalid.
             */
            public Command startSpeeds(Motor motorL,Motor motorR, int lSpeed,int rSpeed,int acceleration) {
                if (!(motorL.isFunctional() && motorR.isFunctional())) {
                    return null;
                }
                return new CommandContext("scratch.move_start_speeds", JsonBuilder.object().add("lmotor",motorL.getPort()).add("rmotor",motorR.getPort()).add("lspeed",lSpeed).add("rspeed",rSpeed).add("acceleration",acceleration)).generateCommand(hub);
            }

            /** Starts continuous movement with power control.
             * @param motorL        Left motor.
             * @param motorR        Right motor.
             * @param lPower        Left power (-100 to 100).
             * @param rPower        Right power (-100 to 100).
             * @param acceleration  Acceleration (0–100).
             * @return A {@link Command}, or {@code null} if motors invalid.
             */
            public Command startPowers(Motor motorL,Motor motorR, int lPower,int rPower,int acceleration) {
                if (!(motorL.isFunctional() && motorR.isFunctional())) {
                    return null;
                }
                return new CommandContext("scratch.move_start_powers", JsonBuilder.object().add("lmotor",motorL.getPort()).add("rmotor",motorR.getPort()).add("lpower",lPower).add("rpower",rPower).add("acceleration",acceleration)).generateCommand(hub);
            }

            /** Stops both motors.
             * @param motorL        Left motor.
             * @param motorR        Right motor.
             * @param stopType      Stop behavior.
             * @return A {@link Command}, or {@code null} if motors invalid.
             */
            public Command stop(Motor motorL,Motor motorR, StopType stopType) {
                if (!(motorL.isFunctional() && motorR.isFunctional())) {
                    return null;
                }
                return new CommandContext("scratch.move_stop", JsonBuilder.object().add("lmotor",motorL.getPort()).add("rmotor",motorR.getPort()).add("stop",stopType.ordinal())).generateCommand(hub);
            }

        }

        /** Sound playback control. */
        public class Sound {
            final Hub hub;
            protected Sound(Hub hub) {
                this.hub = hub;
            }

            /**
             * Plays a single beep.
             *
             * @param note   MIDI note (0–127).
             * @param volume Volume (0–100).
             * @return A {@link Command}.
             */
            public Command beep(int note,int volume) {
                return new CommandContext("scratch.sound_beep", JsonBuilder.object().add("note",note).add("volume",volume)).generateCommand(hub);
            }

            /**
             * Plays a beep for a specific duration.
             *
             * @param note     MIDI note (0–127).
             * @param volume   Volume (0–100).
             * @param duration Duration in milliseconds.
             * @return A {@link Command}.
             */
            public Command beep(int note,int volume,long duration) {
                return new CommandContext("scratch.sound_beep_for_time", JsonBuilder.object().add("duration",duration).add("note",note).add("volume",volume)).generateCommand(hub);
            }

            /** Stops all sound output. */
            public Command off() {
                return new CommandContext("scratch.sound_off", null).generateCommand(hub);
            }
        }

        /** 5x5 LED matrix display control. */
        public class Display {
            final Hub hub;
            protected Display(Hub hub) {
                this.hub = hub;
            }

            /**
             * Shows scrolling text.
             *
             * @param text The text to display.
             * @return A {@link Command}.
             */
            public Command text(String text) {
                return new CommandContext("scratch.display_text", JsonBuilder.object().add("text",text)).generateCommand(hub);
            }

            /**
             * Shows a static image.
             *
             * @param image The {@link Image} to display.
             * @return A {@link Command}.
             */
            public Command image(Image image) {
                return new CommandContext("scratch.display_image", image.toJson()).generateCommand(hub);
            }

            /**
             * Shows an image for a duration.
             *
             * @param image    The {@link Image} to display.
             * @param duration Duration in milliseconds.
             * @return A {@link Command}.
             */
            public Command image(Image image, long duration) {
                return new CommandContext("scratch.display_image_for", image.toJson().add("duration",duration)).generateCommand(hub);
            }

            /**
             * Plays an animation.
             *
             * @param animation The {@link Animation} to play.
             * @param async     {@code true} to run in background.
             * @param delay     Delay between frames in milliseconds.
             * @param fade      Fade duration between frames in milliseconds.
             * @param loop      {@code true} to loop animation.
             * @return A {@link Command}.
             */
            public Command animation(Animation animation, boolean async, long delay, int fade, boolean loop) {
                return new CommandContext("scratch.display_animation", animation.toJson().add("async",async).add("delay",delay).add("fade",fade).add("loop",loop)).generateCommand(hub);
            }

            /** Clears the display. */
            public Command clear() {
                return new CommandContext("scratch.display_clear", null).generateCommand(hub);
            }

            /**
             * Sets a single pixel.
             *
             * @param x          X coordinate (0–4).
             * @param y          Y coordinate (0–4).
             * @param brightness Brightness (0–9).
             * @return A {@link Command}.
             */
            public Command setPixel(byte x, byte y, int brightness) {
                return new CommandContext("scratch.display_set_pixel", JsonBuilder.object().add("brightness",brightness).add("x",x).add("y",y)).generateCommand(hub);
            }

            /**
             * Rotates display content.
             *
             * @param direction The rotation direction.
             * @return A {@link Command}.
             */
            public Command rotateDirection(Direction direction) {
                return new CommandContext("scratch.display_rotate_direction", JsonBuilder.object().add("direction",direction.name().toLowerCase())).generateCommand(hub);
            }

            /**
             * Sets display orientation.
             *
             * @param orientation The orientation (0–3).
             * @return A {@link Command}.
             */
            public Command rotateOrientation(Orientation orientation) {
                return new CommandContext("scratch.display_rotate_orientation", JsonBuilder.object().add("orientation",orientation.ordinal()+1)).generateCommand(hub);
            }

            /**
             * Sets center button LED color.
             *
             * @param color RGB color value.
             * @return A {@link Command}.
             */
            public Command buttonLight(int color) {
                return new CommandContext("scratch.center_button_lights", JsonBuilder.object().add("color",color)).generateCommand(hub);
            }

        }
    }

    /**
     * Event listener for hub and device events.
     */
    public class Listener {
        boolean isActive = true;
        final Hub hub;

        private Listener(Hub hub) {
            this.hub = hub;
            startService();
        }

        /** Listener interface for hub events. */
        public interface HubEventListener {
            void newDeviceConnected(ConnectedDevice device);
            void deviceDisconnected(ConnectedDevice device);
            void hubWasKnocked();
            void hubChangedState(HubState newState);
            void hubButtonPressed(HubButton button);
            void hubButtonReleased(HubButton button,long duration);
            void receivedBroadcastMessage(long hash,String message);
        }

        private List<HubEventListener> listeners = new ArrayList<HubEventListener>();

        /** @param listener Listener to add. */
        public void addListener(HubEventListener listener) {
            listeners.add(listener);
        }

        /** @param listener Listener to remove. */
        public void removeListener(HubEventListener listener) {
            listeners.remove(listener);
        }

        private void newDeviceConnected(ConnectedDevice device) {
            listeners.forEach(listener -> new Thread(() -> listener.newDeviceConnected(device)).start());
        }

        private void deviceDisconnected(ConnectedDevice device) {
            listeners.forEach(listener -> new Thread(() -> listener.deviceDisconnected(device)).start());
        }

        private void hubWasKnocked() {
            listeners.forEach(listener -> new Thread(listener::hubWasKnocked).start());
        }

        private void hubChangedState(HubState newState) {
            listeners.forEach(listener -> new Thread(() -> listener.hubChangedState(newState)).start());
        }

        private void hubButtonWasPressed(SimpleJsonArray data) {
            new Thread(() -> {
                try {
                    String button = data.optString(0);
                    HubButton hButton = switch (button) {
                        case "left" -> HubButton.LEFT;
                        case "right" -> HubButton.RIGHT;
                        case "center" -> HubButton.CENTER;
                        default -> null;
                    };
                    if (hButton != null) {
                        int duration = data.optInt(1);
                        if (duration > 0) {
                            listeners.forEach(listener -> new Thread(() -> listener.hubButtonReleased(hButton,duration)).start());
                        } else {
                            listeners.forEach(listener -> new Thread(() -> listener.hubButtonPressed(hButton)).start());
                        }
                    }
                } catch (Exception ex) {
                    if (debugLogging) ex.printStackTrace();
                }
            }).start();
        }

        private void receivedBroadcastMessage(SimpleJsonArray data) {
            new Thread(() -> {
                try {
                    long hash = data.optLong(0);
                    String message = data.optString(1);
                    listeners.forEach(listener -> new Thread(() -> listener.receivedBroadcastMessage(hash,message)).start());
                } catch (Exception ignored) {}
            }).start();
        }

        // Task ID Management
        List<String> taskIDsInUse = new ArrayList<>(); // TaskIDs currently in Use
        HashMap<String,CompletableFuture<String>> waitingResults = new HashMap<>(); // Waiting Task results
        HashMap<String,String> passedResults = new HashMap<>(); // Passed Task results without Listener
        String chars = "0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz";

        /**
         * Generates a unique 4-character task ID.
         *
         * @return A unique task ID.
         */
        public String newTaskID() {
            String randomID = new Random().ints(0, chars.length())
                    .limit(4)
                    .mapToObj(i -> String.valueOf(chars.charAt(i)))
                    .collect(StringBuilder::new, StringBuilder::append, StringBuilder::append)
                    .toString();
            if (taskIDsInUse.contains(randomID)) {
                return newTaskID();
            }
            taskIDsInUse.add(randomID);
            return randomID;
        }

        /**
         * Queues a task result future.
         *
         * @param taskID The task ID.
         * @return A {@link CompletableFuture} for the result.
         */
        public CompletableFuture<String> queueTaskResult(String taskID) {
            CompletableFuture<String> future = new CompletableFuture<>();
            if (passedResults.containsKey(taskID)) {
                future.complete(passedResults.get(taskID));
                passedResults.remove(taskID);
                taskIDsInUse.remove(taskID);
            }
            waitingResults.put(taskID, future);
            return future;
        }

        private void taskResult(String packet) {
            try {
                String taskID = extractBetween(packet, "\"i\":\"", "\"");
                String result = extractBetween(packet, "\"r\":", "}");
                if (waitingResults.containsKey(taskID)) {
                    waitingResults.get(taskID).complete(result);
                    taskIDsInUse.remove(taskID);
                } else {
                    passedResults.put(taskID, result);
                }
            } catch (Exception e) {}
        }

        private void dataUpdate(String data) {
            try {
                if (data.startsWith("{")) {
                    SimpleJson parsedData = JsonParser.parseObject(data);
                    switch (parsedData.optInt("m",-1)) {
                        case 0 -> {
                            SimpleJsonArray hubDataArray = parsedData.getJSONArray("p");
                            new Thread(() -> { updateDevices(hubDataArray);}).start();
                            new Thread(() -> { updateHubData(hubDataArray);}).start();
                        }
                        case 2 -> {
                            SimpleJsonArray hubDataArray = parsedData.getJSONArray("p");
                            new Thread(() -> { updatePowerData(hubDataArray);}).start();
                        }
                        case 3 -> {
                            SimpleJsonArray hubDataArray = parsedData.getJSONArray("p");
                            hubButtonWasPressed(hubDataArray);
                        }
                        case 4 -> {
                            hubWasKnocked();
                        }
                        case 14 -> {
                            HubState state = HubState.values()[parsedData.optInt("p")];
                            hubChangedState(state);
                            hub.state.set(state);
                        }
                        case 15 -> {
                            SimpleJsonArray hubDataArray = parsedData.getJSONArray("p");
                            receivedBroadcastMessage(hubDataArray);
                        }
                        case -1 -> {
                            switch (parsedData.optString("m")) {
                                case "runtime_error" -> {
                                    SimpleJsonArray hubDataArray = parsedData.getJSONArray("p");
                                    System.err.println(hub.getMacAddress() + " " + new String(Base64.getDecoder().decode(hubDataArray.optString(3))));
                                }
                                default -> {
                                    System.out.println(hub.getMacAddress() + " Unknown Data Received with Code: " + parsedData.optString("m") + " " + data);
                                }
                            }
                        }
                        default -> {
                            System.out.println(hub.getMacAddress() + " Unknown Data Received with Code: " + parsedData.optInt("m") + " " + data);
                        }
                    }
                }
            } catch (Exception ex) {
                if (debugLogging) ex.printStackTrace();
            }
        }

        private void updatePowerData(SimpleJsonArray hubDataArray) {
            hub.batteryVoltage.set(hubDataArray.getDouble(0));
            hub.batteryPercentage.set(hubDataArray.getInt(1));
            hub.pluggedIn.set(hubDataArray.getBoolean(2));
        }

        private void updateHubData(SimpleJsonArray hubDataArray) {
            SimpleJsonArray acceleration = hubDataArray.getJSONArray(7);
            hub.accelerationX.set(acceleration.getInt(0));
            hub.accelerationY.set(acceleration.getInt(1));
            hub.accelerationZ.set(acceleration.getInt(2));

            SimpleJsonArray rotation = hubDataArray.getJSONArray(8);
            hub.rotationX.set(rotation.getInt(0));
            hub.rotationY.set(rotation.getInt(1));
            hub.rotationZ.set(rotation.getInt(2));

            SimpleJsonArray orientation = hubDataArray.optJSONArray(9);
            if (orientation != null) {
                hub.yaw.set(orientation.getInt(0));
                hub.pitch.set(orientation.getInt(1));
                hub.roll.set(orientation.getInt(2));
            }

            hub.unknownData.set(hubDataArray.optString(10));

            hub.programmTime.set(hubDataArray.optLong(11));

        }

        private void updateDevices(SimpleJsonArray hubDataArray) {
            for (int i=0; i<6; i++) {
                SimpleJsonArray deviceData = hubDataArray.getJSONArray(i);
                int deviceType = deviceData.getInt(0);
                Port devicePort = Port.values()[i];

                if (hub.connectedDevices.containsKey(devicePort)) {
                    ConnectedDevice connectedDevice = hub.connectedDevices.get(devicePort);
                    if (deviceType == 0) {
                        hub.connectedDevices.remove(devicePort);
                        deviceDisconnected(connectedDevice);
                        continue;
                    } else {
                        if (connectedDevice.getType() == deviceType) {
                            connectedDevice.update(deviceData.getJSONArray(1));
                            continue;
                        } else {
                            deviceDisconnected(connectedDevice);
                            hub.connectedDevices.remove(devicePort);
                        }
                    }
                }

                ConnectedDevice device = switch (deviceType) {
                    case 75 ->  new Motor(hub,devicePort);
                    case 62 -> new UltrasonicSensor(hub,devicePort);
                    case 61 -> new ColorSensor(hub,devicePort);
                    default -> null;
                };
                if (device != null) {
                    connectedDevices.put(devicePort, device);
                    device.update(deviceData.getJSONArray(1));
                    newDeviceConnected(device);
                }
            }
        }


        Thread servicethread = null;
        long lastDataTimestamp = System.currentTimeMillis();

        private void startService() {
            servicethread = new Thread(() -> {
                while (isActive) {
                    try (RawInputStream raw = new RawInputStream(this.hub.handle)) {
                        byte[] packet;
                        while (isActive) {
                            packet = raw.readPacket();
                            if (packet != null && packet.length > 0) {
                                lastDataTimestamp = System.currentTimeMillis();
                                String packetValue = new String(packet, 0, packet.length - 1,StandardCharsets.UTF_8);
                                if (packetValue.contains("{\"i\":")) {
                                    taskResult(packetValue);
                                } else {
                                    dataUpdate(packetValue);
                                }
                            } else {
                                if (System.currentTimeMillis() - lastDataTimestamp > 5000) {
                                    hub.disconnect();
                                }
                                Thread.sleep(10);
                            }
                        }
                    } catch (Exception ex) {
                        if (debugLogging) ex.printStackTrace();
                    }
                }
            });
            servicethread.start();
        }


        public void stopService() {
            isActive = false;
            servicethread.interrupt();
            connectedDevices.clear();
        }

        static class RawInputStream extends InputStream {
            private final ByteBuffer h;
            private final byte[] buf = new byte[4096];
            private int pos = 0;
            private int end = 0;

            RawInputStream(ByteBuffer h) { this.h = h; }

            @Override
            public int read() throws IOException {
                if (pos >= end) {
                    fill();
                    if (pos >= end) return -1;
                }
                return buf[pos++] & 0xFF;
            }

            @Override
            public int read(byte[] b, int off, int len) throws IOException {
                if (pos >= end) {
                    fill();
                    if (pos >= end) return -1;
                }
                int avail = end - pos;
                int toRead = Math.min(avail, len);
                System.arraycopy(buf, pos, b, off, toRead);
                pos += toRead;
                return toRead;
            }

            private void fill() {
                Arrays.fill(buf, (byte) 0xFF);  // Debug: 0xFF
                pos = 0;
                end = readStreamNative(h, buf, 0, buf.length);
                if (end <= 0) end = 0;
            }

            public byte[] readPacket() throws IOException {
                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                int b;
                while ((b = read()) != -1) {
                    baos.write(b);
                    if (b == 0x0D) {
                        return baos.toByteArray();  // inkl. 0x0D
                    }
                }
                return baos.size() > 0 ? baos.toByteArray() : null;
            }
        }

        private static String extractBetween(String text, String start, String end) {
            int startIdx = text.indexOf(start) + start.length();
            int endIdx = text.indexOf(end, startIdx);
            if (startIdx < start.length() || endIdx == -1) return null;
            return text.substring(startIdx, endIdx);
        }
    }



    private Hub(ByteBuffer handle,String mac) {
        this.handle = handle;
        this.hubListener = new Listener(this);
        this.hubControl = new HubControl(this);
        this.mac = mac;
        connectedHubs.add(this);
        getControl().listenBroadcast(true).sendAsync();
        new Thread(() -> listeners.forEach(brickListener -> brickListener.newHubConnected(this))).start();
    }

    /**
     * Sends a raw JSON command to the hub.
     *
     * @param data The JSON string to send.
     * @return 0 on success, error code otherwise.
     */
    public int send(String data) {
        return sendNative(handle,data.getBytes(StandardCharsets.UTF_8));
    }

    /** Disconnects from the hub and cleans up resources. */
    public void disconnect() {
        disconnect(true);
    }

    private void disconnect(boolean removeFromList) {
        hubListener.stopService();
        if (handle != null) disconnectNative(handle);
        if (removeFromList) connectedHubs.remove(this);
        new Thread(() -> listeners.forEach(brickListener -> brickListener.hubDisconnected(this))).start();
    }

    /** @return The Bluetooth MAC address of this hub. */
    public String getMacAddress() {
        return mac;
    }


    private final HubControl hubControl;
    private final Listener hubListener;
    private final String mac;
    private final ByteBuffer handle;

    // Native methods
    private static native ByteBuffer connectNative(String mac);
    private static native void disconnectNative(ByteBuffer handle);
    private static native int sendNative(ByteBuffer handle, byte[] data);
    private static native int readStreamNative(ByteBuffer handle, byte[] buffer, int offset, int max);
}
