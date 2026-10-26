# Building and developing (Linux)

## Requirements

- **JDK 21** (a JRE is not enough). Check with `java -version` and `javac -version`.
  - Debian / Ubuntu: `sudo apt install openjdk-21-jdk`
  - Fedora: `sudo dnf install java-21-openjdk-devel`
  - Arch: `sudo pacman -S jdk21-openjdk`
- Git and an internet connection for the first build (Minecraft, mappings and CC: Tweaked are downloaded and cached in `~/.gradle`).

You do not need to install Gradle: use the wrapper `./gradlew`.

## Build

```bash
chmod +x gradlew          # only if "Permission denied"
./gradlew build           # both loaders, with sources jars
./gradlew :fabric:build
./gradlew :neoforge:build
./gradlew clean build     # from scratch
```

Output:

| Loader | Jar to install |
|---|---|
| Fabric | `fabric/build/libs/minimal-peripherals-fabric-<version>+1.21.11.jar` |
| NeoForge | `neoforge/build/libs/minimal-peripherals-neoforge-<version>+1.21.11.jar` |

Ignore `*-sources.jar` and `fabric/build/devlibs/` (development jars, not for the game).

## Run the game from Gradle

```bash
./gradlew :fabric:runClient
./gradlew :fabric:runServer
./gradlew :neoforge:runClient
./gradlew :neoforge:runServer
```

Dedicated servers need `eula=true` in `run-server/eula.txt` (Fabric) or the matching run folder (NeoForge).
CC: Tweaked is added to the dev runtime automatically.

## IDE

- **IntelliJ IDEA**: open the project folder, let Gradle import it, then use the generated run configurations.
- **VS Code**: install the Java and Gradle extensions, then run `./gradlew :fabric:genSources` for readable Minecraft sources.

## Layout

All the gameplay code lives in `common/`. A loader module only contains what must use loader APIs:

| File | Role |
|---|---|
| `common/.../ModRegistry.java` | Creates every block, item, block entity type, component and the creative tab. |
| `common/.../platform/Platform.java` | Things built differently per loader (block entity type, creative tab). |
| `fabric/.../MinimalPeripheralsFabric.java` | Registers everything with `Registry.register` and the Fabric peripheral lookup. |
| `neoforge/.../MinimalPeripheralsNeoForge.java` | Registers everything in `RegisterEvent` and the NeoForge peripheral capability. |

Assets, recipes and loot tables are plain JSON in `common/src/main/resources` (no data generation).

## Troubleshooting

| Problem | Fix |
|---|---|
| `Permission denied: ./gradlew` | `chmod +x gradlew` |
| `bad interpreter: /bin/sh^M` | The script has Windows line endings: `sed -i "s/\r$//" gradlew` |
| `Unsupported class file major version` / `release 21 is not supported` | Gradle runs with a JDK older than 21. Set `JAVA_HOME` to a JDK 21. |
| `Could not resolve cc.tweaked:...` | CC: Tweaked publishes 1.21.11 builds up to 1.117.1. Check `cct_version` in `gradle.properties` and that https://maven.squiddev.cc is reachable. |
| `Could not resolve net.neoforged:neoforge:...` | Check `neoforge_version` in `gradle.properties` against https://projects.neoforged.net/neoforged/neoforge. |
| Out of memory | Raise `org.gradle.jvmargs=-Xmx3G` in `gradle.properties`. |

## Updating versions

All versions are in `gradle.properties`: `minecraft_version`, `fabric_version`, `loader_version`, `neoforge_version`, `cct_version`, `parchment_version`.
