# Pi-Shock CS2 Integration

Enhance your gaming experience with Pi-Shock for Counter-Strike 2 (CS2)! This integration enables real-time feedback via electric shocks using the PiShock device, aligning in-game hits with immediate physical sensations through seamless serial communication.

## Features
- **Customizable Configuration**: Modify settings directly through the configuration file to suit your gameplay.
- **User-Friendly**: Straightforward installation and operation.
- **Minimal Latency**: Experience no delay between in-game actions and corresponding shocks.

# Configuration
- **Currently allowed Distrubutions**: `FIRST, LAST, RANDOM, ALL`

# Information
- Currently tested only on Windows 11.
- Still in development and may contain bugs.
- Not affiliated with PiShock or Counter-Strike 2.
- Provided as-is and may not be suitable for all users.
- Use at your own risk.
- Open-source and free to use.
- Not designed for commercial purposes.
- Not recommended for individuals with:
    - Pacemakers
    - Epilepsy
    - Heart conditions
    - Other medical conditions
- Not suitable for individuals with disabilities or sensitivities.

# To-Do
- Add more features such as methods triggered when you kill someone, get killed by a taser, or get knifed.
- Expand configuration options.
- Improve the functionality, for example, random device.
- Implement additional shortcuts.
- Control GUI

## Installation
Follow these steps to set up the Pi-Shock integration with CS2:
1. Download the latest release.
2. Move `gamestate_integration_cs2shock.cfg` to your CS2 configuration directory at `\Counter-Strike Global Offensive\game\csgo\cfg\`.
3. Install Java 21 from the official site.
4. Extract and run `PiShock.jar` from the downloaded ZIP file.
5. Launch CS2.
6. Execute `startup.bat` (Windows) or `startup.sh` (Linux/Mac).
7. Enjoy!

## Shortcuts
- **CTRL + M**: Emergency exit.

## Special Thanks
- Thanks to [@VellusFox](https://vellusfox.de/) for assisting with testing since i dont have a own PiShock device. ♥️

# Used Dependencies
- [Lombok](https://github.com/projectlombok/lombok) - Simplifies Java development by reducing boilerplate code.
- [Spark](https://github.com/perwendel/spark) - A micro framework for creating web applications in Kotlin and Java with minimal effort.
- [Gson](https://github.com/google/gson) - A Java serialization/deserialization library to convert Java Objects into JSON and back.
- [jSerialComm](https://github.com/Fazecast/jSerialComm) - A platform-independent serial communication library for Java.
- [JNativeHook](https://github.com/kwhat/jnativehook) - Provides global keyboard and mouse listeners for Java.


## Credits
- PiShock integration is based on the work by ScoreUnder available on GitHub: [PiShock Zap Fabric](https://github.com/ScoreUnder/pishock-zap-fabric/tree/mc1.21).
