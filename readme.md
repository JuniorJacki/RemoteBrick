# RemoteBrick – The **Ultimate** Java API for LEGO® Inventor Hub (51515)

[![License: CC BY-NC 4.0](https://img.shields.io/badge/License-CC%20BY--NC%204.0-lightgrey.svg)](https://creativecommons.org/licenses/by-nc/4.0/)
[![Java 21+](https://img.shields.io/badge/Java-21%2B-blue?logo=openjdk)](https://www.oracle.com/java/technologies/downloads/)
[![Windows 10/11](https://img.shields.io/badge/Windows-10%2F11-green?logo=windows)](https://www.microsoft.com/windows)
[![Bluetooth Classic](https://img.shields.io/badge/Bluetooth-Classic%20(SPP)-blue?logo=bluetooth)](https://en.wikipedia.org/wiki/Bluetooth#Classic_Bluetooth)
[![Plug & Play](https://img.shields.io/badge/Setup-Plug%20&%20Play-success)](https://en.wikipedia.org/wiki/Plug_and_play)
[![GitHub stars](https://img.shields.io/github/stars/juniorjacki/RemoteBrick?style=social)](https://github.com/juniorjacki/RemoteBrick)

> **Control your LEGO® Inventor Hub (51515) from Java — like the official LEGO app, but **100× more powerful**.**  
> **No LEGO software. No Python. No limits. With Official Firmware on the HUB**

---

## What is RemoteBrick?

**RemoteBrick** is a **native, high-performance Java API** for the **LEGO® Education Inventor Hub (51515)** used in **SPIKE™ Prime** and **Inventor** kits.

It communicates **directly via Bluetooth Classic (SPP)** — giving you **full control** of every feature the hub offers.

---

## Why RemoteBrick?

| Feature | RemoteBrick | LEGO App | Pybricks |
|--------|-------------|----------|----------|
| **Language** | **Java 21+** | Scratch/Python | MicroPython |
| **Real-time Events** | Full | Limited | Full |
| **Thread-safe API** | Yes | No | Yes |
| **No Installation** | Yes | No | No |
| **100% LEGO App Parity** | Yes | Yes | Yes |
| **Windows Plug & Play** | Yes | YES | NO |

> **RemoteBrick = LEGO App Features + Java  + Easy Setup**

---

## Features (100% LEGO App Coverage)

| Category | Supported | Examples |
|--------|-----------|--------|
| **Display** | Full | Text, Images, Animations, Pixels, Rotation |
| **Motors** | Full | Speed, Power, Tank Drive, Degrees, Ramp |
| **Sensors** | Full | Color, Distance, Force, Gyro, Tilt |
| **Sound** | Full | Beeps, Notes, Duration, Volume |
| **Buttons** | Full | Press/Release, Duration |
| **Hub State** | Full | Tilt, Knock, Battery, Runtime |
| **Broadcasts** | Full | Send & receive messages |
| **Async Commands** | Full | Non-blocking `.sendAsync()` |

---


## Plug & Play on Windows

> **No installation. No drivers. No LEGO app.**

### Requirements
- **Windows 10 / 11** (64-bit)
- **Java 21 or higher** (`java --version`)
- **Bluetooth enabled** Built-in or adapter
- **LEGO Inventor Hub (51515)** with Original Firmware

---

## Quick Start in 3 Steps

**No need to clone or build!**  
Just download the latest **release JAR** from GitHub and add it to your project.

### 1. Download the Latest Release

Go to: [Releases](https://github.com/juniorjacki/RemoteBrick/releases/latest)

Download: RemoteBrick-#.#.#.jar

---

### 2. Add to Your Project

#### **IntelliJ IDEA / Eclipse / VS Code**

1. Create a new Java project (`java --version` ≥ 21)
2. Copy `RemoteBrick-1.0.0.jar` into your project folder (e.g., `lib/`)
3. Add as **external library**:
    - **IntelliJ**: File → Project Structure → Libraries → `+` → Java → select JAR
    - **VS Code**: Add to `lib/`, ensure `java.project.referencedLibraries` includes it

---
### 3. Pair Your Hub

1. Turn on the hub
2. Open **Windows Bluetooth Settings**
3. Find `LEGO Hub` → **Pair**
4. Copy the **MAC address** → e.g., `AA:BB:CC:DD:EE:FF`

---

## Code Examples

### Connect to the Hub

```java
import de.juniorjacki.remotebrick.Hub;

public class MyRobot {
    public static void main(String[] args) {
        // Replace with your hub's MAC address
        Hub hub = Hub.connect("AA:BB:CC:DD:EE:FF");
        
        if (hub == null) {
            System.out.println("Failed to connect!");
            return;
        }

        System.out.println("Connected! Battery: " + hub.getBatteryPercentage() + "%");
    }
}
```
### Display Text & Animation

```java
import de.juniorjacki.remotebrick.Hub;
import de.juniorjacki.remotebrick.types.*;

public class DisplayDemo {
    public static void main(String[] args) {
        Hub hub = Hub.connect("AA:BB:CC:DD:EE:FF");
        if (hub == null) return;

        // Scrolling text
        hub.getControl().display().text("HELLO WORLD").send();

        // Heartbeat animation (looping)
        hub.getControl().display()
           .animation(Animation.HEARTBEAT, false, 400, 100, true)
           .send();

        // Show smiley for 2 seconds
        hub.getControl().display()
           .image(Image.SMILEY, 2000)
           .send();
    }
}
```

### Motor Control (Tank Drive)
```java
import de.juniorjacki.remotebrick.Hub;
import de.juniorjacki.remotebrick.devices.Motor;
import de.juniorjacki.remotebrick.types.*;

public class MotorDemo {
    public static void main(String[] args) {
        Hub hub = Hub.connect("AA:BB:CC:DD:EE:FF");
        if (hub == null) return;

        Motor left  = (Motor) hub.getDevice(Port.A);
        Motor right = (Motor) hub.getDevice(Port.B);

        if (left != null && right != null) {
            // Forward for 3 seconds
            hub.getControl().move()
                    .startSpeeds(left, right, 70, 70, 100)
                    .send();

            try { Thread.sleep(3000); } catch (Exception e) {}

            // Stop with brake
            hub.getControl().move()
               .stop(left, right, StopType.BRAKE)
               .send();
        }
    }
}
```

### React to Hub Events
```java
import de.juniorjacki.remotebrick.Hub;

public class EventsDemo {
    public static void main(String[] args) {
        Hub hub = Hub.connect("AA:BB:CC:DD:EE:FF");
        if (hub == null) return;

        hub.getListenerService().addListener(new Hub.Listener.HubEventListener() {
            @Override
            public void newDeviceConnected(ConnectedDevice device) {
                System.out.println("New Device Connected Port " + device.getPort() + " Type:" + device.getType());
            }

            @Override
            public void deviceDisconnected(ConnectedDevice device) {
                System.out.println("Device Disconnected Port " + device.getPort() + " Type:" + device.getType());
            }

            @Override
            public void hubWasKnocked() {
                System.out.println("KNOCK! Hub was tapped!");
            }

            @Override
            public void hubChangedState(HubState newState) {
                System.out.println("New Hubstate " + newState.name());
            }

            @Override
            public void hubButtonPressed(HubButton button) {
                System.out.println("Button " + button.name() +" pressed");
            }

            @Override
            public void hubButtonReleased(HubButton button, long duration) {
                System.out.println("Button " + button.name() +" held: " + duration + "ms");
            }

            @Override
            public void receivedBroadcastMessage(long hash, String message) {
                System.out.println("Received broadcast message: " + message);
            }
        });

        System.out.println("Listening... Press buttons or knock the hub.");
    }
}
```

## Troubleshooting

| Issue | Solution |
|------|----------|
| `Hub.connect()` returns `null` | Check MAC address, pairing, Bluetooth on |
| DLL error | Run on **Windows only** |
| Motors not moving | Check port, motor type |
| JAR not found | Add `RemoteBrick-*.jar` to classpath |


---

## License
> CC BY-NC 4.0 License  
> Copyright (c) 2025 JuniorJacki
