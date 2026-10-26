# Lua API

All peripherals are used through the CC: Tweaked `peripheral` API.

```lua
local crypto = peripheral.find("cryptographic_accelerator")
local nfc    = peripheral.find("nfc_reader")
local rfid   = peripheral.find("rfid_scanner")
```

## Conventions

- Lua strings are treated as **byte strings**: one character is one byte (ISO-8859-1). Keys, signatures and ciphertexts are therefore returned as strings of raw bytes unless you ask for another format.
- `[x]` means an optional argument.

### Output formats

Every function that returns binary data (hashes, MACs, derived keys, random bytes) takes an optional `format` argument:

| Value | Result |
|---|---|
| `"hex"` | lowercase hexadecimal |
| `"base64"` | standard base64 |
| `"base64url"` | URL-safe base64 without padding |
| `"raw"` | the raw bytes |
| `true` | same as `"hex"` (historical behaviour) |
| `false` | same as `"raw"` (historical behaviour) |

Defaults: hashes and MACs return `"hex"`; key derivation functions and `randomBytes` return `"raw"`.

### Hash algorithms

`md5`, `sha1`, `sha224`, `sha256`, `sha384`, `sha512`, `sha3_256`, `sha3_384`, `sha3_512`, `ripemd160`, `blake2b_256`, `blake2b_512`, `blake2s_256`.
`listHashes()` returns this list. MD5 and SHA-1 are broken for security purposes, they are only here for compatibility.

## Cryptographic Accelerator (`cryptographic_accelerator`)

### Hashing

| Function | Description |
|---|---|
| `md5(data [, format])`, `sha1`, `sha256`, `sha512` | Hash `data`. |
| `hash(algorithm, data [, format])` | Hash with any algorithm of the list above. |
| `hmac(algorithm, key, data [, format])` | HMAC with any algorithm. |
| `hmacMd5(key, data [, format])`, `hmacSha1`, `hmacSha256`, `hmacSha512` | HMAC shortcuts. |
| `blake3(data [, format [, mode [, keyOrContext [, outputSize]]]])` | BLAKE3. `mode` is `"hash"` (default), `"keyed"` (32 byte key) or `"derive"` (context string). `outputSize` is 1 to 4096 bytes, default 32. |
| `crc32(data [, format])`, `adler32(data [, format])` | Checksums. A number by default, a 4 byte string when `format` is given. |
| `constantTimeEquals(a, b)` | Compares two strings in constant time. Use it to compare MACs. |
| `listHashes()` | Returns the supported algorithm names. |

```lua
print(crypto.hash("sha3_256", "hello"))
print(crypto.hmac("sha512", "key", "message", "base64"))
```

### Key derivation

| Function | Description |
|---|---|
| `pbkdf2(password, salt, iterations, length [, algorithm [, format]])` | PBKDF2. Default `"sha256"`, `"raw"`. 1 to 1,000,000 iterations, `length` 1 to 1024. |
| `hkdf(inputKeyMaterial, salt, info, length [, algorithm [, format]])` | HKDF. Default `"sha256"`, `"raw"`. `salt` may be `""`. |
| `scrypt(password, salt, n, r, p, length [, format])` | scrypt. `n` is a power of two up to 32768, `r` up to 8, `p` up to 4. |

```lua
local key = crypto.pbkdf2("my password", salt, 100000, 32)   -- a 32 byte AES key
```

### Symmetric encryption

| Function | Description |
|---|---|
| `encryptAes(data, key, iv [, mode])` / `decryptAes(...)` | AES. `key` is 16, 24 or 32 bytes, `iv` is 16 bytes. `mode` is `"cbc"` (default, PKCS7 padding) or `"ctr"` (no padding). |
| `encryptAesGcm(data, key, nonce [, aad])` / `decryptAesGcm(...)` | AES-GCM, authenticated. `nonce` is 12 bytes, `aad` is optional authenticated data. The 16 byte tag is appended to the ciphertext. |
| `encryptChaCha20Poly1305(data, key, nonce [, aad])` / `decryptChaCha20Poly1305(...)` | ChaCha20-Poly1305, authenticated. `key` is 32 bytes, `nonce` is 12 bytes. |

The decryption functions throw `authentication failed` if the data was modified or the key is wrong. **Never reuse a nonce with the same key.**

```lua
local key, nonce = crypto.randomBytes(32), crypto.randomBytes(12)
local sealed = crypto.encryptAesGcm("secret", key, nonce, "header")
print(crypto.decryptAesGcm(sealed, key, nonce, "header"))
```

### Signatures and key exchange

| Function | Description |
|---|---|
| `generateKeyPair([type])` | Returns `{privateKey = ..., publicKey = ...}`. `type` is `"ed25519"` (default) or `"x25519"`. |
| `generatePrivateKey()` | 32 random bytes, usable as an Ed25519 or X25519 private key. |
| `derivePublicKey(privateKey)` | Ed25519 public key. |
| `sign(message, privateKey)` | Ed25519 signature (64 bytes). |
| `verify(message, signature, publicKey)` | Returns `true` or `false`. |
| `deriveEcdhPublicKey(privateKey)` | X25519 public key. |
| `computeSharedSecret(privateKey, peerPublicKey)` | X25519 shared secret (32 bytes). Run it through `hkdf` before using it as a key. |

### Random

| Function | Description |
|---|---|
| `random([[min,] max])` | Secure replacement of `math.random` for floats: `random()` is in [0, 1), `random(max)` in [0, max), `random(min, max)` in [min, max). |
| `randomInt(min, max)` | Uniform integer, both bounds included (between -2^53 and 2^53). |
| `randomBytes(length [, format])` | `length` bytes, up to 65536. Default `"raw"`. |
| `randomUuid()` | Random UUID string. |

### Encodings and compression

| Function | Description |
|---|---|
| `encodeBase64(data [, urlSafe])` / `decodeBase64(data [, urlSafe])` | Base64. `decodeBase64` returns `nil` on invalid input. |
| `encodeHex(data)` / `decodeHex(data)` | Hexadecimal. `decodeHex` returns `nil` on invalid input. |
| `deflate(data [, level])` | zlib compression, `level` 0 (store) to 9 (best), default -1 (balanced). |
| `inflate(data [, maxSize])` | Decompression. The result is limited to `maxSize` bytes (default and maximum 8 MiB), bigger results throw an error. |

### Information

`getLimits()` returns a table with the limits enforced by the peripheral: `randomBytesMaxSize`, `blake3MaxOutput`, `kdfMaxOutput`, `pbkdf2MaxIterations`, `scryptMaxN`, `scryptMaxR`, `scryptMaxP` and `inflateMaxSize`.

## NFC Reader (`nfc_reader`)

Right-click the reader with an **NFC Card** or an **RFID Badge** to use it.
When no operation is pending, a right-click **reads** the item. Calling `write` or `sign` arms the reader for the **next** right-click.

| Function | Description |
|---|---|
| `write(data [, label [, readOnly [, privateKey]]])` | Arms a write. `data` is cut to 128 characters, `label` becomes the item name (32 characters), `readOnly = true` locks the item, `privateKey` (32 bytes) replaces the key of a card. Cancels a pending `sign`. |
| `cancelWrite()` | Cancels a pending write, returns whether there was one. |
| `sign(data)` | Arms a signature. The next card used signs `data` with its own private key. Only cards have a key. Cancels a pending `write`. |
| `cancelSign()` | Cancels a pending signature, returns whether there was one. |

| Event | Arguments | When |
|---|---|---|
| `nfc_data` | `name, data, publicKey` | An item holding data was read. `publicKey` is `nil` for badges. |
| `nfc_write` | `name, success, message` | A write finished. `message` is `"success"` or `"read_only"`. |
| `nfc_sign` | `name, signature, publicKey` | A card signed the data. |

A card gets its own random private key the first time it is read (when it holds data) or used to sign.
Verify a signature with the accelerator: `crypto.verify(data, signature, publicKey)`.

```lua
nfc.sign("open the door")
local _, _, signature, publicKey = os.pullEvent("nfc_sign")
print(crypto.verify("open the door", signature, publicKey))   -- true
```

## RFID Scanner (`rfid_scanner`)

The scanner can be placed on any side of a block and is a peripheral on every face.

| Function | Description |
|---|---|
| `scan()` | Starts a scan and waits for it (about 4 ticks, the block lights up). Returns a list of `{data = string, distance = number}`, one entry per badge that holds data within 8 blocks. |

It detects badges in the inventory of players, in the hands of other living entities and on the ground as item entities.
The event `rfid_scan` (`name, list`) is also queued on the attached computers.
