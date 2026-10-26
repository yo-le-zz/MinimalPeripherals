# Changelog

## 1.0.0

First release. Minecraft 1.21.11, Fabric and NeoForge.

- Cryptographic Accelerator, NFC Reader, NFC Card, RFID Scanner and RFID Badge, ported from Classic Peripherals.
- Multi-loader project (shared `common` sources, `fabric` and `neoforge` modules).
- Cryptographic Accelerator API extended (see docs/LUA_API.md):
  - output formats: every binary result can be returned as `hex`, `base64`, `base64url` or `raw`;
  - new hashes: SHA-224/384, SHA3-256/384/512, RIPEMD-160, BLAKE2b, BLAKE2s, plus the generic `hash`, `hmac` and `listHashes`;
  - key derivation: `pbkdf2`, `hkdf`, `scrypt`;
  - authenticated encryption: `encryptAesGcm` / `decryptAesGcm`, `encryptChaCha20Poly1305` / `decryptChaCha20Poly1305`;
  - AES-CTR through the `mode` argument of `encryptAes` / `decryptAes`;
  - `generateKeyPair`, `constantTimeEquals`, `crc32`, `adler32`, `randomInt`, `randomUuid`, `encodeHex` / `decodeHex`, url-safe base64;
  - `deflate` compression level, `inflate` size limit, `getLimits`.
- Fixed: the HMAC functions of Classic Peripherals read the wrong number of bytes of the message (the key length instead of the message length).
- Fixed: `inflate` is now limited to 8 MiB of output (decompression bombs).
