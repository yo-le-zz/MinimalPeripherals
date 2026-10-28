# Changelog

## 1.0.0

First release of Cryptography Peripherals, for Minecraft 1.21.11 on Fabric and NeoForge.

- Based on the Cryptographic Accelerator of Classic Peripherals. The NFC and RFID blocks are not part of this mod.
- 25 specialized blocks: hash engines (MD5, SHA-1, SHA-256, SHA-384, SHA-512, SHA3-256, SHA3-512, RIPEMD-160, BLAKE2b, BLAKE2s, BLAKE3),
  AES and ChaCha20-Poly1305, Ed25519 signatures, X25519 key exchange, key derivation, password hasher, TOTP generator,
  entropy source, encoder, checksum engine, compressor, Merkle tree engine, proof-of-work miner and classic cipher table.
- Cryptographic Accelerator API extended:
  - every binary result can be returned as `hex`, `base64`, `base64url` or `raw`;
  - generic `hash`, `hmac`, `listHashes` with 13 algorithms;
  - `pbkdf2`, `hkdf`, `scrypt`;
  - `encryptAesGcm`, `decryptAesGcm`, `encryptChaCha20Poly1305`, `decryptChaCha20Poly1305`, AES-CTR;
  - `generateKeyPair`, `constantTimeEquals`, `crc32`, `adler32`, `randomInt`, `randomUuid`, `encodeHex`/`decodeHex`, url-safe base64;
  - `deflate` level, `inflate` size limit, `getLimits`.
- Fixed: the HMAC functions of Classic Peripherals read the key length instead of the message length.
- Fixed: `inflate` is now limited to 8 MiB of output (decompression bombs).
- Multi-loader build (shared `common` sources), asset generator, Lua test programs.
