package de.juniorjacki.remotebrick;

import de.juniorjacki.remotebrick.devices.ColorSensor;
import de.juniorjacki.remotebrick.devices.ConnectedDevice;
import de.juniorjacki.remotebrick.devices.Motor;
import de.juniorjacki.remotebrick.devices.UltrasonicSensor;
import de.juniorjacki.remotebrick.types.*;
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
import java.util.*;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;

public class Hub {

    static List<Hub> connectedHubs = new ArrayList<>();
    static {
        try {
            System.load(new File("src/libs/BluetoothHubJNI.dll").getAbsolutePath());
            Runtime.getRuntime().addShutdownHook(new Thread(() -> {
               connectedHubs.forEach(hub ->  hub.disconnect(false));
               connectedHubs.clear();
            }));
        } catch (Exception e) {
            System.err.println("DLL nicht geladen: " + e.getMessage());
        }
    }

    public static Hub connect(String macAddress) {
        ByteBuffer h = connectNative(macAddress);
        if (h == null || !h.isDirect()) {
            return null;
        }
        try {
            Thread.sleep(2000);
        } catch (InterruptedException e) {}
        return new Hub(h);
    }

    public Control getHubControl() {
        return hubControl;
    }

    public Listener getListener() {
        return hubListener;
    }


    // Hub POWER
    private final AtomicReference<Double> batteryVoltage =  new AtomicReference<>(0.0);
    private final AtomicInteger batteryPercentage =  new AtomicInteger(0);
    private final AtomicBoolean pluggedIn = new AtomicBoolean(false);

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

    private final AtomicReference<String> unknownData = new AtomicReference<>("");
    private final Map<Port, ConnectedDevice> connectedDevices = new ConcurrentHashMap<>();

    public int getAccelerationX() { return accelerationX.get(); }
    public int getAccelerationY() { return accelerationY.get(); }
    public int getAccelerationZ() { return accelerationZ.get(); }

    public int getRotationX() { return rotationX.get(); }
    public int getRotationY() { return rotationY.get(); }
    public int getRotationZ() { return rotationZ.get(); }

    public int getYaw() { return yaw.get(); }
    public int getPitch() { return pitch.get(); }
    public int getRoll() { return roll.get(); }

    public long getProgrammTime() { return programmTime.get(); }
    public String getUnknownData() { return unknownData.get(); }

    // Immutable Snapshot der Geräte
    public List<ConnectedDevice> getDevices() {
        return List.copyOf(connectedDevices.values());
    }

    public ConnectedDevice getDevice(Port port) {
        return connectedDevices.get(port);
    }

    /**
     * Control Class for Device unabhängige Commands (Motor panzersteuerung ...)
     */
    public class Control {
        final Hub hub;
        final Display display;
        final Sound  sound;
        final Move move;


        protected Control(Hub hub) {
            this.hub = hub;
            this.display = new Display(hub);
            this.sound = new Sound(hub);
            this.move = new Move(hub);

        }

        public Display display() {
            return this.display;
        }

        public Sound sound() {
            return this.sound;
        }

        public class Move {
            final Hub hub;
            protected Move(Hub hub) {
                this.hub = hub;
            }

            public Command tankDegrees(Motor motorL, Motor motorR, int lSpeed, int rSpeed, int degrees, StopType stopType, int acceleration, int deceleration) {
                if (!(motorL.isFunctional() && motorR.isFunctional())) {
                    return null;
                }
                return new CommandContext("scratch.move_tank_degrees", new JsonBuilder().add("lmotor",motorL.getPort()).add("rmotor",motorR.getPort()).add("lspeed",lSpeed).add("rspeed",rSpeed).add("degrees",degrees).add("stop",stopType.ordinal()).add("acceleration",acceleration).add("deceleration",deceleration)).generateCommand(hub);
            }

            public Command startSpeeds(Motor motorL,Motor motorR, int lSpeed,int rSpeed,int acceleration) {
                if (!(motorL.isFunctional() && motorR.isFunctional())) {
                    return null;
                }
                return new CommandContext("scratch.move_start_speeds", new JsonBuilder().add("lmotor",motorL.getPort()).add("rmotor",motorR.getPort()).add("lspeed",lSpeed).add("rspeed",rSpeed).add("acceleration",acceleration)).generateCommand(hub);
            }

            public Command startPowers(Motor motorL,Motor motorR, int lPower,int rPower,int acceleration) {
                if (!(motorL.isFunctional() && motorR.isFunctional())) {
                    return null;
                }
                return new CommandContext("scratch.move_start_powers", new JsonBuilder().add("lmotor",motorL.getPort()).add("rmotor",motorR.getPort()).add("lpower",lPower).add("rpower",rPower).add("acceleration",acceleration)).generateCommand(hub);
            }

            public Command stop(Motor motorL,Motor motorR, StopType stopType) {
                if (!(motorL.isFunctional() && motorR.isFunctional())) {
                    return null;
                }
                return new CommandContext("scratch.move_stop", new JsonBuilder().add("lmotor",motorL.getPort()).add("rmotor",motorR.getPort()).add("stop",stopType.ordinal())).generateCommand(hub);
            }

        }

        public class Sound {
            final Hub hub;
            protected Sound(Hub hub) {
                this.hub = hub;
            }

            public Command beep(int note,int volume) {
                return new CommandContext("scratch.sound_beep", new JsonBuilder().add("note",note).add("volume",volume)).generateCommand(hub);
            }

            public Command beep(int note,int volume,long duration) {
                return new CommandContext("scratch.sound_beep_for_time", new JsonBuilder().add("duration",duration).add("note",note).add("volume",volume)).generateCommand(hub);
            }

            public Command off() {
                return new CommandContext("scratch.sound_off", null).generateCommand(hub);
            }
        }

        public class Display {
            final Hub hub;
            protected Display(Hub hub) {
                this.hub = hub;
            }

            public Command text(String text) {
                return new CommandContext("scratch.display_text", new JsonBuilder().add("text",text)).generateCommand(hub);
            }

            public Command image(Image image) {
                return new CommandContext("scratch.display_image", image.toJson()).generateCommand(hub);
            }

            public Command image(Image image, long duration) {
                return new CommandContext("scratch.display_image_for", image.toJson().add("duration",duration)).generateCommand(hub);
            }

            public Command animation(Animation animation, boolean async, long delay, int fade, boolean loop) {
                return new CommandContext("scratch.display_animation", animation.toJson().add("async",async).add("delay",delay).add("fade",fade).add("loop",loop)).generateCommand(hub);
            }

            public Command clear() {
                return new CommandContext("scratch.display_clear", null).generateCommand(hub);
            }

            public Command setPixel(byte x, byte y, int brightness) {
                return new CommandContext("scratch.display_set_pixel", new JsonBuilder().add("brightness",brightness).add("x",x).add("y",y)).generateCommand(hub);
            }

            public Command rotateDirection(Direction direction) {
                return new CommandContext("scratch.display_rotate_direction", new JsonBuilder().add("direction",direction.name().toLowerCase())).generateCommand(hub);
            }

            public Command rotateOrientation(Orientation orientation) {
                return new CommandContext("scratch.display_rotate_orientation", new JsonBuilder().add("orientation",orientation.ordinal()+1)).generateCommand(hub);
            }

            public Command buttonLight(int color) {
                return new CommandContext("scratch.center_button_lights", new JsonBuilder().add("color",color)).generateCommand(hub);
            }

        }
    }




    public class Listener {
        boolean isActive = true;
        final Hub hub;

        private Listener(Hub hub) {
            this.hub = hub;
            startService();
        }

        public interface HubEventListener {
            void newDeviceConnected(ConnectedDevice device);
            void deviceDisconnected(ConnectedDevice device);
        }

        private List<HubEventListener> listeners = new ArrayList<HubEventListener>();

        public void addListener(HubEventListener listener) {
            listeners.add(listener);
        }

        public void removeListener(HubEventListener listener) {
            listeners.remove(listener);
        }

        private void newDeviceConnected(ConnectedDevice device) {
            listeners.forEach(listener -> listener.newDeviceConnected(device));
        }

        private void deviceDisconnected(ConnectedDevice device) {
            listeners.forEach(listener -> listener.deviceDisconnected(device));
        }

        List<String> taskIDsInUse = new ArrayList<>(); // TaskIDs currently in Use
        HashMap<String,CompletableFuture<String>> waitingResults = new HashMap<>(); // Waiting Task results
        HashMap<String,String> passedResults = new HashMap<>(); // Passed Taskresults without Listener

        String chars = "0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz";
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

                System.out.println("new task result " + taskID + " " + result);
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
                SimpleJson parsedData = JsonParser.parseObject(data);
                switch (parsedData.getInt("m")) {
                    case 0 -> {
                        SimpleJsonArray hubDataArray = parsedData.getJSONArray("p");
                        new Thread(() -> { updateDevices(hubDataArray);}).start();
                        new Thread(() -> { updateHubData(hubDataArray);}).start();
                    }
                    case 2 -> {
                        SimpleJsonArray hubDataArray = parsedData.getJSONArray("p");
                        new Thread(() -> { updatePowerData(hubDataArray);}).start();
                    }
                    default -> {
                        System.out.println("Unknown Data Received with Code: " + parsedData.getInt("m") + " " + data);
                    }
                }
            } catch (Exception ignored) {
                ignored.printStackTrace();
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

        private void startService() {
            servicethread = new Thread(() -> {
                System.out.println("Starting Hub listener Service");
                while (isActive) {
                    try (RawInputStream raw = new RawInputStream(this.hub.handle)) {
                        byte[] packet;
                        while (isActive) {
                            packet = raw.readPacket();
                            if (packet != null && packet.length > 0) {
                                String packetValue = new String(packet, 0, packet.length - 1,StandardCharsets.UTF_8);
                                if (packetValue.contains("{\"i\":")) {
                                    taskResult(packetValue);
                                } else {
                                    dataUpdate(packetValue);
                                }
                            } else {
                                Thread.sleep(10);
                            }
                        }
                    } catch (Exception e) {}
                }
            });
            servicethread.start();
        }


        public void stopService() {
            isActive = false;
            servicethread.interrupt();
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

        // Hilfsmethode: Text zwischen zwei Strings extrahieren
        private static String extractBetween(String text, String start, String end) {
            int startIdx = text.indexOf(start) + start.length();
            int endIdx = text.indexOf(end, startIdx);
            if (startIdx < start.length() || endIdx == -1) return null;
            return text.substring(startIdx, endIdx);
        }
    }


    private final Control hubControl;
    private final Listener hubListener;

    private Hub(ByteBuffer handle) {
        this.handle = handle;
        this.hubListener = new Listener(this);
        this.hubControl = new Control(this);
        connectedHubs.add(this);
    }

    public int send(String data) {
        System.out.println("Sending: " + data);
        return sendNative(handle,data.getBytes(StandardCharsets.UTF_8));
    }

    public void disconnect() {
        disconnect(true);
    }

    private void disconnect(boolean removeFromList) {
        hubListener.stopService();
        if (handle != null) disconnectNative(handle);
        if (removeFromList) connectedHubs.remove(this);
    }

    private final ByteBuffer handle;
    private static native ByteBuffer connectNative(String mac);
    private static native void disconnectNative(ByteBuffer handle);
    private static native int sendNative(ByteBuffer handle, byte[] data);
    private static native int readStreamNative(ByteBuffer handle, byte[] buffer, int offset, int max);
}
