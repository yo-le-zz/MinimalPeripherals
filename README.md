# Cryptography Peripherals

A [CC: Tweaked](https://tweaked.cc) add-on for **Minecraft 1.21.11** (**Fabric** and **NeoForge**) that adds a general
**Cryptographic Accelerator** and **25 specialized crypto blocks**: one block per algorithm, each with its own fast, focused
Lua API.

It started as the Cryptographic Accelerator of [Classic Peripherals](https://github.com/Ale32bit/ClassicPeripherals) by AlexDevs,
which it extends a lot.

## Blocks

| Group | Blocks | Peripheral types |
|---|---|---|
| General | Cryptographic Accelerator | `cryptographic_accelerator` |
| Hashes | MD5, SHA-1, SHA-256, SHA-384, SHA-512, SHA3-256, SHA3-512, RIPEMD-160, BLAKE2b, BLAKE2s Engines | `md5_engine` `sha1_engine` `sha256_engine` `sha384_engine` `sha512_engine` `sha3_256_engine` `sha3_512_engine` `ripemd160_engine` `blake2b_engine` `blake2s_engine` |
| | BLAKE3 Engine (hash, MAC, key derivation, extendable output) | `blake3_engine` |
| Encryption | AES Engine (AES-GCM, CTR, CBC), ChaCha20 Engine (ChaCha20-Poly1305) | `aes_engine` `chacha_engine` |
| Asymmetric | Ed25519 Signature Engine, X25519 Key Exchange | `signature_engine` `key_exchange` |
| Passwords and keys | Key Derivation Engine (PBKDF2, HKDF, scrypt), Password Hasher, TOTP Generator | `kdf_engine` `password_hasher` `totp_generator` |
| Randomness | Entropy Source (bytes, integers, tokens, shuffle, dice...) | `entropy_source` |
| Data tools | Encoder (base64/32/58, hex), Checksum Engine (CRC32, CRC32C, Adler, FNV, Luhn), Compressor (deflate, gzip) | `encoder` `checksum_engine` `compressor` |
| Fun and teaching | Merkle Tree Engine, Proof-of-Work Miner, Classic Cipher Table | `merkle_tree` `pow_miner` `classic_cipher` |

### Why specialized blocks?

The accelerator does everything, with many options. A specialized block does **one thing**, so it can be faster and simpler:

- JVM-native implementations (which use CPU instructions like SHA and AES extensions) instead of generic ones. In a micro-benchmark,
  `sha256_engine.hash` is **about 1.5x faster on small strings and 2.2x faster on 4 KiB** than `cryptographic_accelerator.sha256`.
- A shorter call path: no algorithm lookup, no output-format parsing.
- **Batch functions** (`many`, `hmacMany`, `sealMany`, `signMany`...) that process a whole list in a single call, which saves the
  cost of one Lua-to-Java call per item.
- Higher level functions that only make sense for one algorithm: `chain`, `twice` (double SHA-256), `seal`/`open` (encryption with
  a random nonce and authentication in one call), `sessionKey` (X25519 + HKDF), password hashing with salt and parameters in one
  string, TOTP, Merkle proofs, proof of work in native code (about 4 million attempts per second)...

## Compatibility

| | Fabric | NeoForge |
|---|---|---|
| Minecraft | 1.21.11 | 1.21.11 |
| Loader | Fabric Loader 0.19.5+ | NeoForge 21.11.45+ |
| Required mods | Fabric API, CC: Tweaked 1.117.1+ | CC: Tweaked 1.117.1+ |
| Java | 21+ | 21+ |

CC: Tweaked 1.117.1 is the newest CC: Tweaked build published for 1.21.11.

## Installation

1. Install Fabric or NeoForge for Minecraft 1.21.11.
2. Put in the `mods/` folder: `cryptography-peripherals-fabric-<version>.jar` (or `-neoforge-`), CC: Tweaked, and Fabric API on Fabric.

On Linux the default folders are `~/.minecraft/mods` (vanilla launcher), `~/.local/share/PrismLauncher/instances/<instance>/minecraft/mods` (Prism Launcher) and `<server>/mods` (dedicated servers).

## Quick start

```lua
local sha = peripheral.find("sha256_engine")
print(sha.hash("abc"))
local many = sha.many({ "one", "two", "three" })       -- three hashes, one call

local aes = peripheral.find("aes_engine")
local key = aes.generateKey()
local sealed = aes.seal("secret", key)                  -- random nonce + authentication
print(aes.open(sealed, key))

local pw = peripheral.find("password_hasher")
local stored = pw.hash("hunter2")                       -- store this string
print(pw.verify("hunter2", stored))                     -- true

local otp = peripheral.find("totp_generator")
local secret = otp.generateSecret()
print(otp.totp(secret), otp.remaining())
```

The complete reference is in [docs/LUA_API.md](docs/LUA_API.md).

## Crafting

The Cryptographic Accelerator is the base of everything else.

| Block | Recipe |
|---|---|
| Cryptographic Accelerator | `ICI` / `CDC` / `ICI` (I iron ingot, C comparator, D diamond) |
| Every other block | shapeless: Cryptographic Accelerator + redstone + the item below |

| Block | Item | Block | Item |
|---|---|---|---|
| MD5 Engine | stick | AES Engine | ender pearl |
| SHA-1 Engine | flint | ChaCha20 Engine | feather |
| SHA-256 Engine | iron ingot | Ed25519 Signature Engine | ink sac |
| SHA-384 Engine | gold ingot | X25519 Key Exchange | compass |
| SHA-512 Engine | emerald | Key Derivation Engine | repeater |
| SHA3-256 Engine | lapis lazuli | Password Hasher | tripwire hook |
| SHA3-512 Engine | amethyst shard | TOTP Generator | clock |
| RIPEMD-160 Engine | quartz | Entropy Source | glowstone dust |
| BLAKE2b Engine | copper ingot | Encoder | paper |
| BLAKE2s Engine | iron nugget | Checksum Engine | string |
| BLAKE3 Engine | gold nugget | Compressor | piston |
| Merkle Tree Engine | oak sapling | Proof-of-Work Miner | iron pickaxe |
| Classic Cipher Table | book | | |

## Building (Linux)

```bash
sudo apt install openjdk-21-jdk        # JDK 21 is required (Fedora: java-21-openjdk-devel, Arch: jdk21-openjdk)

git clone <your repository url>
cd CryptographyPeripherals
chmod +x gradlew                       # only if the executable bit was lost

./gradlew build                        # both loaders
./gradlew :fabric:build                # Fabric only
./gradlew :neoforge:build              # NeoForge only
```

The jars are in `fabric/build/libs/` and `neoforge/build/libs/` (ignore the `-sources.jar` files).
More in [docs/BUILDING.md](docs/BUILDING.md): running the game from Gradle, IDE setup, regenerating assets, adding a block.

## Testing in game

Two Lua programs in `examples/` check every function against official test vectors (NIST, RFC 4231, 5869, 6070, 6238, 7748, 7914, 8032):

- `crypto_test.lua` for the Cryptographic Accelerator,
- `engines_test.lua` for all the specialized blocks.

Place the blocks next to a computer (or connect them with wired modems), copy a program to the computer and run it:
`engines_test` (add `-v` to see every check). Blocks that are not attached are skipped.

You can also run the same programs **without Minecraft**, against the real Java code:
`tools/offline-test/run.sh` (needs a JDK 21, `libbcprov-java` and `pip install jpype1 lupa`).

## Project layout

```
common/      code and assets shared by both loaders
  .../peripheral/   one class per block: the Lua APIs
  .../ModRegistry   the list of blocks (one line per block)
fabric/      Fabric entrypoint, fabric.mod.json
neoforge/    NeoForge entrypoint, neoforge.mods.toml
tools/       generate_assets.py (textures, models, recipes, loot tables, translations) and offline-test/
examples/    Lua test programs
docs/        Lua API reference, build guide
```

## Credits and license

The accelerator and its original API come from [Classic Peripherals](https://github.com/Ale32bit/ClassicPeripherals) by AlexDevs (MIT).
Released under the MIT License, see [LICENSE.txt](LICENSE.txt).
