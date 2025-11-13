package de.juniorjacki.remotebrick;

import de.juniorjacki.remotebrick.devices.ConnectedDevice;
import de.juniorjacki.remotebrick.devices.Motor;
import de.juniorjacki.remotebrick.utils.JsonBuilder;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

public class Hub {

    static {
        try {
            System.load(new File("src/libs/BluetoothHubJNI.dll").getAbsolutePath());
        } catch (Exception e) {
            System.err.println("DLL nicht geladen: " + e.getMessage());
        }
    }
    static List<Hub> connectedHubs = new ArrayList<>();

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


    /**
     * Control Class for Device unabhängige Commands (Motor panzersteuerung ...)
     */
    public class Control {
        final Hub hub;
        final Display display;
        final Sound  sound;
        final Move move;

        enum Direction{
            ClOCKWISE,
            COUNTERCLOCKWISE
        }

        enum Orientation {
            NORMAL,
            LEFT,
            UPSIDEDOWN,
            RIGHT
        }

        enum StopType {
            RUNOUT,
            BRAKE,
            KEEP_POSITION,
        }

        protected record CommandContext(String method,JsonBuilder payload) {
            public Command generateCommand(Hub hub) {
                return new Command(hub,hub.hubListener.newTaskID(),new JsonBuilder().add("m",method).addObject("p",payload != null ? payload : new JsonBuilder()));
            }
        }

        public static class Command {
            private final Hub hub;
            private final String identifier;
            private final JsonBuilder commandPayload;

            private Command(Hub hub, String identifier, JsonBuilder commandPayload) {
                this.hub = hub;
                this.identifier = identifier;
                this.commandPayload = commandPayload;
            }

            private boolean push() {
                return hub.send(new JsonBuilder().add("i",identifier).add(commandPayload).toString()) == 0;
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

            public String send(long timeout,TimeUnit unit) {
                if (!push()) return null;
                try {
                    return hub.getListener().queueTaskResult(identifier).get(timeout,unit);
                } catch (Exception ignored) {
                    return null;
                }
            }

        }

        protected Control(Hub hub) {
            this.hub = hub;
            this.display = new Display(hub);
            this.sound = new Sound(hub);
            this.move = new Move(hub);

        }

        public List<ConnectedDevice> getDevices() {
            return null;
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

            public Command tankDegrees(Motor motorL,Motor motorR, int lSpeed,int rSpeed,int degrees,StopType stopType,int acceleration,int deceleration) {
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

            public static class Image {
                private final int[][] pixels = new int[5][5];

                public Image setPixel(int x, int y, int brightness) {
                    if (x < 0 || x > 4 || y < 0 || y > 4) {
                        throw new IllegalArgumentException("x und y müssen 0-4 sein!");
                    }
                    if (brightness < 0 || brightness > 9) {
                        throw new IllegalArgumentException("Helligkeit muss 0-9 sein!");
                    }
                    pixels[y][x] = brightness; // y = Zeile, x = Spalte
                    return this; // für Chaining
                }

                public JsonBuilder toJson() {
                    StringBuilder data = new StringBuilder();
                    for (int y = 0; y < 5; y++) {
                        for (int x = 0; x < 5; x++) {
                            data.append(pixels[y][x]);
                        }
                        if (y < 4) data.append(":");
                    }
                    return new JsonBuilder().add("image", data.toString());
                }
            }
            public Command image(Image image) {
                return new CommandContext("scratch.display_image", image.toJson()).generateCommand(hub);
            }
            public Command image(Image image,long duration) {
                return new CommandContext("scratch.display_image_for", image.toJson().add("duration",duration)).generateCommand(hub);
            }
            public class Animation {
                private final java.util.List<String> frames = new java.util.ArrayList<>();

                // Füge ein Image als Frame hinzu
                public Animation addImage(Image image) {
                    // Konvertiere Image → "99099:99099:..."
                    StringBuilder frameData = new StringBuilder();
                    int[][] pixels = getPixelsFromImage(image);
                    for (int y = 0; y < 5; y++) {
                        for (int x = 0; x < 5; x++) {
                            frameData.append(pixels[y][x]);
                        }
                        if (y < 4) frameData.append(":");
                    }
                    frames.add(frameData.toString());
                    return this;
                }

                // Füge einen Frame direkt als String hinzu (muss 5x5 sein: "00000:00000:...")
                public Animation addFrame(String frameData) {
                    validateFrame(frameData);
                    frames.add(frameData);
                    return this;
                }

                public JsonBuilder toJson() {
                    return new JsonBuilder().addArray("frames", new java.util.ArrayList<>(frames));
                }


                @Override
                public String toString() {
                    return toJson().toString();
                }

                // --- Hilfsmethode: Image → pixels (Reflection oder direkt) ---
                private int[][] getPixelsFromImage(Image image) {
                    // Da Image private pixels hat → wir nutzen toString() und parsen!
                    // Alternativ: Freundschaft mit Package, aber wir bleiben sauber
                    String data = extractImageData(image.toString());
                    if (data == null) return new int[5][5];

                    String[] rows = data.split(":");
                    int[][] pixels = new int[5][5];
                    for (int y = 0; y < 5; y++) {
                        String row = rows[y];
                        for (int x = 0; x < 5; x++) {
                            pixels[y][x] = row.charAt(x) - '0';
                        }
                    }
                    return pixels;
                }

                private String extractImageData(String json) {
                    int start = json.indexOf("\"image\":\"");
                    if (start == -1) return null;
                    start += 9;
                    int end = json.indexOf("\"", start);
                    return end != -1 ? json.substring(start, end) : null;
                }

                // --- Validierung ---
                private void validateFrame(String frame) {
                    if (frame == null) throw new IllegalArgumentException("Frame darf nicht null sein");
                    String[] rows = frame.split(":");
                    if (rows.length != 5) throw new IllegalArgumentException("Frame muss 5 Zeilen haben");
                    for (String row : rows) {
                        if (row.length() != 5) throw new IllegalArgumentException("Jede Zeile muss 5 Zeichen haben");
                        for (char c : row.toCharArray()) {
                            if (c < '0' || c > '9') throw new IllegalArgumentException("Nur Ziffern 0-9 erlaubt");
                        }
                    }
                }
            }
            public Command animation(Animation animation,boolean async,long delay,int fade,boolean loop) {
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








        public void sendAndWait(String method, String payloadJson) {
            String id = hub.hubListener.newTaskID();
            String json = String.format("{\"i\":\"%s\",\"m\":\"%s\",\"p\":%s}", id, method, payloadJson);

            System.out.println("Sende: " + json);
            hub.send(json);

            try {
                hub.hubListener.queueTaskResult(id).get(5000, TimeUnit.MILLISECONDS);

            } catch (Exception e) {}
        }
    }




    private class Listener {
        boolean isActive = true;
        final Hub hub;

        private Listener(Hub hub) {
            this.hub = hub;
            startService();
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

        private void statusUpdate(String status) {
            System.out.println(status);
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
                                    statusUpdate(packetValue);
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
    }

    protected int send(String data) {
        System.out.println("Sending: " + data);
        return sendNative(handle,data.getBytes(StandardCharsets.UTF_8));
    }

    public void disconnect() {
        hubListener.stopService();
        if (handle != null) disconnectNative(handle);
    }

    private final ByteBuffer handle;
    private static native ByteBuffer connectNative(String mac);
    private static native void disconnectNative(ByteBuffer handle);
    private static native int sendNative(ByteBuffer handle, byte[] data);
    private static native int readStreamNative(ByteBuffer handle, byte[] buffer, int offset, int max);
}
