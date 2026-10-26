# Minimal Peripherals

A small [CC: Tweaked](https://tweaked.cc) add-on for **Minecraft 1.21.11**, on **Fabric** and **NeoForge**.
It keeps three peripherals from [Classic Peripherals](https://github.com/Ale32bit/ClassicPeripherals) by AlexDevs and nothing else.

| Block / Item | Peripheral type | What it does |
|---|---|---|
| **Cryptographic Accelerator** | `cryptographic_accelerator` | Hashes, HMAC, key derivation, AES / ChaCha20 encryption, Ed25519 signatures, X25519 key exchange, encodings, compression, secure random |
| **NFC Reader** + **NFC Card** | `nfc_reader` | Write, read and sign data on cards by right-clicking the reader |
| **RFID Scanner** + **RFID Badge** | `rfid_scanner` | Detects badges nearby: in inventories, held by mobs, dropped on the ground |

## Compatibility

| | Fabric | NeoForge |
|---|---|---|
| Minecraft | 1.21.11 | 1.21.11 |
| Loader | Fabric Loader 0.19.5+ | NeoForge 21.11.45+ |
| Required mods | Fabric API, CC: Tweaked 1.117.1+ | CC: Tweaked 1.117.1+ |
| Java | 21+ | 21+ |

CC: Tweaked 1.117.1 is the newest CC: Tweaked build published for 1.21.11.

## Installation

1. Install the loader (Fabric or NeoForge) for Minecraft 1.21.11.
2. Put these files in the `mods/` folder:
   - `minimal-peripherals-fabric-<version>.jar` or `minimal-peripherals-neoforge-<version>.jar`
   - CC: Tweaked (and Fabric API on Fabric)

On Linux the default folders are `~/.minecraft/mods` (vanilla launcher), `~/.local/share/PrismLauncher/instances/<instance>/minecraft/mods` (Prism Launcher) and `<server>/mods` (dedicated servers).

## Quick start

```lua
-- Cryptographic Accelerator
local crypto = peripheral.find("cryptographic_accelerator")
print(crypto.sha256("hello"))                    -- hex by default
print(crypto.hash("sha3_256", "hello", "base64"))

-- NFC Reader: write a card, then read it
local nfc = peripheral.find("nfc_reader")
nfc.write("my data", "My card")                  -- then right-click the reader with a card
print(os.pullEvent("nfc_write"))                 -- nfc_write, name, true, "success"
print(os.pullEvent("nfc_data"))                  -- right-click again: nfc_data, name, data, publicKey

-- RFID Scanner
local rfid = peripheral.find("rfid_scanner")
for _, badge in ipairs(rfid.scan()) do
    print(badge.data, badge.distance)
end
```

The complete Lua reference is in [docs/LUA_API.md](docs/LUA_API.md).

## Crafting

| Result | Recipe |
|---|---|
| NFC Reader | `SSS` / `SRS` / `SCS` (S stone, R redstone, C copper ingot) |
| NFC Card | shapeless: paper + redstone + copper ingot |
| RFID Badge | iron nuggets in a plus shape around a copper ingot |
| RFID Scanner | ring of 8 stone around a copper ingot |
| Cryptographic Accelerator | `ICI` / `CDC` / `ICI` (I iron ingot, C comparator, D diamond) |

## Building (Linux)

```bash
# JDK 21 is required
sudo apt install openjdk-21-jdk        # Debian / Ubuntu
# sudo dnf install java-21-openjdk-devel   # Fedora
# sudo pacman -S jdk21-openjdk             # Arch

git clone <your repository url>
cd MinimalPeripherals
chmod +x gradlew                       # only needed if the executable bit was lost (zip, some file managers)

./gradlew build                        # both loaders
./gradlew :fabric:build                # Fabric only
./gradlew :neoforge:build              # NeoForge only
```

The jars are written to:

- `fabric/build/libs/minimal-peripherals-fabric-<version>.jar`
- `neoforge/build/libs/minimal-peripherals-neoforge-<version>.jar`

The `-sources.jar` files are only for developers, do not put them in `mods/`.
More details (running the game from Gradle, IDE setup, troubleshooting) in [docs/BUILDING.md](docs/BUILDING.md).

## Project layout

```
common/      sources and assets shared by both loaders (blocks, items, peripherals, crypto, models, recipes)
fabric/      Fabric entrypoint, registration, fabric.mod.json
neoforge/    NeoForge entrypoint, registration, neoforge.mods.toml
docs/        Lua API reference, build guide
```

`common` is not a separate Gradle project: each loader compiles its sources directly.
Everything that differs between loaders (registration, block entity types, creative tab, peripheral lookup) goes through the small `Platform` interface.

## Differences from Classic Peripherals

- Card and badge data and private keys are stored in item components (no server-side saved state).
- No pocket computer NFC support, turtle/pocket upgrades or card dyeing.
- No Trinkets, Curios or Sable integration, no config file (limits are fixed in `Limits.java`).
- The Cryptographic Accelerator has a much larger API (see the changelog).
- The HMAC functions of Classic Peripherals only hashed the first `#key` bytes of the message; this is fixed here.

## Credits and license

Based on [Classic Peripherals](https://github.com/Ale32bit/ClassicPeripherals) by AlexDevs (MIT).
Released under the MIT License, see [LICENSE.txt](LICENSE.txt).
