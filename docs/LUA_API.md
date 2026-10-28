# Lua API

Every block is a CC: Tweaked peripheral. Find it by its type:

```lua
local sha = peripheral.find("sha256_engine")
local aes = peripheral.find("aes_engine")
local crypto = peripheral.find("cryptographic_accelerator")   -- the general one
```

| Block | Peripheral type |
|---|---|
| Cryptographic Accelerator | `cryptographic_accelerator` |
| MD5 / SHA-1 / SHA-256 / SHA-384 / SHA-512 Engine | `md5_engine` `sha1_engine` `sha256_engine` `sha384_engine` `sha512_engine` |
| SHA3-256 / SHA3-512 Engine | `sha3_256_engine` `sha3_512_engine` |
| RIPEMD-160 / BLAKE2b / BLAKE2s Engine | `ripemd160_engine` `blake2b_engine` `blake2s_engine` |
| BLAKE3 Engine | `blake3_engine` |
| AES Engine, ChaCha20 Engine | `aes_engine` `chacha_engine` |
| Ed25519 Signature Engine, X25519 Key Exchange | `signature_engine` `key_exchange` |
| Key Derivation Engine, Password Hasher, TOTP Generator | `kdf_engine` `password_hasher` `totp_generator` |
| Entropy Source | `entropy_source` |
| Encoder, Checksum Engine, Compressor | `encoder` `checksum_engine` `compressor` |
| Merkle Tree Engine, Proof-of-Work Miner, Classic Cipher Table | `merkle_tree` `pow_miner` `classic_cipher` |

The accelerator does everything in one block with many options. The **engines** do one thing each, with a shorter call path
(no algorithm lookup, no format parsing), JVM-native implementations where they exist, and batch functions that process a
whole list in a single call.

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

---

# Specialized engines

Conventions of the engines: strings are byte strings, `[x]` is optional, a "list" is a Lua table `{ "a", "b", "c" }`
(at most 10,000 items), and results are lowercase hexadecimal unless said otherwise.
Functions that decode return `nil` on invalid input.

## Hash engines

`md5_engine`, `sha1_engine`, `sha256_engine`, `sha384_engine`, `sha512_engine`, `sha3_256_engine`, `sha3_512_engine`,
`ripemd160_engine`, `blake2b_engine` (512 bits) and `blake2s_engine` (256 bits) all have the same functions:

| Function | Description |
|---|---|
| `hash(data)` | Hash as hexadecimal. |
| `raw(data)` | Hash as raw bytes. |
| `many(list)` | Hashes every string of the list, returns the list of hexadecimal hashes. |
| `twice(data)` | `hash(hash(data))`, the double hash used by Bitcoin with SHA-256. |
| `chain(data, rounds)` | Applies the hash `rounds` times (1 to 1,000,000). |
| `hmac(key, data)` | HMAC, hexadecimal. An empty key is allowed. |
| `hmacMany(key, list)` | HMAC of every string of the list. |
| `verify(data, expectedHex)` | Checks a hash in constant time (case-insensitive). |
| `verifyHmac(key, data, expectedHex)` | Checks an HMAC in constant time. |
| `size()` | Size of the hash in bytes: MD5 16, SHA-1 20, SHA-256 32, SHA-384 48, SHA-512 64, SHA3-256 32, SHA3-512 64, RIPEMD-160 20, BLAKE2b 64, BLAKE2s 32. |

```lua
local sha = peripheral.find("sha256_engine")
print(sha.hash("abc"))
local hashes = sha.many({ "one", "two", "three" })     -- one call for three hashes
print(sha.verify("abc", hashes[1]))
local token = sha.hmac(secret, user .. "|" .. expiry)
```

## BLAKE3 engine (`blake3_engine`)

| Function | Description |
|---|---|
| `hash(data [, size])` | BLAKE3, 32 bytes by default, up to 4096 (extendable output). |
| `raw(data [, size])` | Same, as raw bytes. |
| `keyed(key, data [, size])` | Keyed hash (MAC). `key` is 32 bytes. |
| `derive(context, material [, size])` | Key derivation. Returns raw bytes. |
| `many(list [, size])` | Hashes every string of the list. |

## AES engine (`aes_engine`)

AES-GCM is the default: it encrypts **and** authenticates. Keys are 16, 24 or 32 bytes.

| Function | Description |
|---|---|
| `generateKey([bits])` | Random key: 128, 192 or 256 bits (default 256). Raw bytes. |
| `seal(data, key [, aad])` | Encrypts with a random nonce. Returns `nonce .. ciphertext .. tag`. This is the function to use. |
| `open(sealed, key [, aad])` | Reverses `seal`. Throws `authentication failed` if anything was modified. |
| `sealMany(list, key [, aad])` | Seals every string of the list. |
| `encrypt(data, key, nonce [, aad])` | AES-GCM with a nonce you choose (12 bytes, **never reuse it with the same key**). |
| `decrypt(data, key, nonce [, aad])` | Reverses `encrypt`. |
| `ctr(data, key, iv)` | AES-CTR (16 byte `iv`), no authentication. The same call encrypts and decrypts. |
| `cbcEncrypt(data, key, iv)` / `cbcDecrypt(...)` | AES-CBC with PKCS7 padding, no authentication. |

```lua
local aes = peripheral.find("aes_engine")
local key = aes.generateKey()
local sealed = aes.seal("meet me at noon", key, "message-id-42")
print(aes.open(sealed, key, "message-id-42"))
```

## ChaCha20 engine (`chacha_engine`)

ChaCha20-Poly1305 (RFC 8439). The key is 32 bytes, the nonce 12 bytes.
Functions: `generateKey()`, `seal(data, key [, aad])`, `open(sealed, key [, aad])`, `sealMany(list, key [, aad])`,
`encrypt(data, key, nonce [, aad])`, `decrypt(data, key, nonce [, aad])`. They behave exactly like the AES engine ones.

## Ed25519 signature engine (`signature_engine`)

Keys are 32 raw bytes, signatures are 64 raw bytes.

| Function | Description |
|---|---|
| `generateKeyPair()` | Returns `{privateKey = ..., publicKey = ...}`. |
| `publicKey(privateKey)` | Public key of a private key. |
| `sign(message, privateKey)` | Signature. |
| `verify(message, signature, publicKey)` | `true` or `false`. |
| `signMany(messages, privateKey)` | Signs every message of the list. |
| `verifyMany(messages, signatures, publicKey)` | List of booleans, one per message. |

## X25519 key exchange (`key_exchange`)

| Function | Description |
|---|---|
| `generateKeyPair()` | Returns `{privateKey = ..., publicKey = ...}`. |
| `publicKey(privateKey)` | Public key of a private key. |
| `sharedSecret(privateKey, peerPublicKey)` | The raw 32 byte secret. Both sides get the same one. Prefer `sessionKey`. |
| `sessionKey(privateKey, peerPublicKey, info [, length])` | HKDF-SHA256 of the shared secret, 32 bytes by default. `info` names the use of the key (`"my protocol v1"`). |

```lua
local kx = peripheral.find("key_exchange")
local alice, bob = kx.generateKeyPair(), kx.generateKeyPair()
assert(kx.sessionKey(alice.privateKey, bob.publicKey, "chat") == kx.sessionKey(bob.privateKey, alice.publicKey, "chat"))
```

## Key derivation engine (`kdf_engine`)

Results are raw bytes unless a `format` (`"hex"`, `"base64"`, `"base64url"`, `"raw"`) is given as the last argument.
`algorithm` is any hash of the list in "Hash algorithms", `"sha256"` by default.

| Function | Description |
|---|---|
| `pbkdf2(password, salt, iterations, length [, algorithm [, format]])` | PBKDF2. 1 to 1,000,000 iterations, `length` 1 to 1024. |
| `hkdf(inputKeyMaterial, salt, info, length [, algorithm [, format]])` | HKDF. `salt` may be `""`. |
| `scrypt(password, salt, n, r, p, length [, format])` | scrypt. `n` is a power of two up to 32768, `r` up to 8, `p` up to 4. |
| `expand(secret, info, length [, format])` | HKDF-SHA256 without salt: several keys from one secret, using a different `info` for each. |

## Password hasher (`password_hasher`)

Stores passwords the right way in one call: PBKDF2-HMAC-SHA256 with a random 16 byte salt. The result is a
self-contained string (`$pbkdf2-sha256$i=120000$salt$hash`) that you can store as it is.

| Function | Description |
|---|---|
| `hash(password [, iterations])` | Returns the string to store. 120,000 iterations by default, up to 1,000,000. |
| `verify(password, stored)` | `true` if the password matches. Never throws, a malformed `stored` just gives `false`. |
| `needsRehash(stored [, iterations])` | `true` if the stored hash is malformed or uses fewer iterations than the target: rehash on the next login. |

## TOTP generator (`totp_generator`)

One time passwords compatible with Google Authenticator, Authy, Aegis and the like (RFC 4226 and 6238).
Secrets are base32 strings (case, spaces and padding are ignored). Times are in seconds and default to the real time of the server.
`algorithm` is `"sha1"` (default), `"sha256"` or `"sha512"`; `digits` is 6 to 10 (default 6).

| Function | Description |
|---|---|
| `generateSecret([bytes])` | Random base32 secret, 20 bytes by default (10 to 64). |
| `hotp(secret, counter [, digits [, algorithm]])` | Counter based code. |
| `totp(secret [, digits [, period [, time [, algorithm]]]])` | Current code. `period` is 30 seconds by default. |
| `verify(secret, code [, window [, digits [, period [, time [, algorithm]]]]])` | Checks a code, allowing `window` periods before and after (1 by default) for clock drift. |
| `remaining([period [, time]])` | Seconds until the code changes. |

```lua
local otp = peripheral.find("totp_generator")
local secret = otp.generateSecret()          -- show it once, as text or as a QR code
print(otp.totp(secret), otp.remaining())
print(otp.verify(secret, read()))
```

## Entropy source (`entropy_source`)

Secure randomness (the JVM's `SecureRandom`).

| Function | Description |
|---|---|
| `bytes(length [, format])` | Up to 65536 random bytes. Raw by default. |
| `float()` | A number in [0, 1). |
| `int(min, max)` | Uniform integer, both bounds included (within +/- 2^53). |
| `coin()` | `true` or `false`. |
| `chance(probability)` | `true` with the given probability (0 to 1). |
| `uuid()` | Random UUID string. |
| `token(length [, alphabet])` | Random string, base62 by default (up to 4096 characters). `alphabet` is any string of 1 to 256 characters. |
| `shuffle(list)` | A new list with the same items in random order. |
| `pick(list)` | A random item (`nil` for an empty list). |
| `dice(expression)` | `"3d6+2"`, returns `{total = ..., rolls = {...}, modifier = ...}`. Up to 1000 dice. |

## Encoder (`encoder`)

| Function | Description |
|---|---|
| `base64(data)` / `unbase64(text)` | Standard base64. |
| `base64url(data)` / `unbase64url(text)` | URL-safe base64 without padding. |
| `hex(data)` / `unhex(text)` | Hexadecimal. |
| `base32(data [, padding])` / `unbase32(text)` | RFC 4648 base32 (padding by default). Decoding ignores case, spaces and padding. |
| `base58(data)` / `unbase58(text)` | Base58 with the Bitcoin alphabet. Input is limited to 4096 bytes. |
| `xor(data, key)` | XOR with a repeating key. |
| `crc32(data)` | CRC-32 as a number. |

## Checksum engine (`checksum_engine`)

Fast fingerprints. They are **not** cryptographic: never use them for security.

| Function | Description |
|---|---|
| `crc32(data)`, `crc32c(data)`, `adler32(data)` | As numbers. `crc32c` is the Castagnoli variant. |
| `fnv1a32(data)` | As a number. |
| `fnv1a64(data)` | As 16 hexadecimal digits (a Lua number cannot hold 64 bits). |
| `all(data)` | Table with every checksum. |
| `luhnDigit(digits)` | Luhn check digit to append to a number. |
| `luhnValid(number)` | `true` if the last digit is a valid Luhn check digit. |

## Compressor (`compressor`)

Decompression is limited to 8 MiB of output (`maxSize` can lower it) to stop decompression bombs.

| Function | Description |
|---|---|
| `deflate(data [, level])` / `inflate(data [, maxSize])` | zlib. `level` is 0 to 9. |
| `gzip(data)` / `gunzip(data [, maxSize])` | gzip. |
| `ratio(data)` | Compressed size divided by original size (best deflate level). |

## Merkle tree engine (`merkle_tree`)

Merkle trees over SHA-256, to prove that an item belongs to a big list while only holding one hash.
Leaves are hashed as `sha256(0x00 .. data)` and inner nodes as `sha256(0x01 .. left .. right)`, so a leaf can never pass for a node.
A node without sibling is promoted to the next level.

| Function | Description |
|---|---|
| `root(list)` | Merkle root, hexadecimal. |
| `leafHash(data)` | Hash of one leaf. |
| `proof(list, index)` | Proof for the item at position `index` (1-based): a list of `{hash = hexadecimal, left = boolean}` where `left` tells if the sibling is on the left. |
| `verify(item, proof, rootHex)` | `true` if the item belongs to the tree with this root. |

```lua
local tree = peripheral.find("merkle_tree")
local files = { "a.lua", "b.lua", "c.lua" }
local root = tree.root(files)
local proof = tree.proof(files, 2)
print(tree.verify("b.lua", proof, root))   -- true
```

## Proof-of-work miner (`pow_miner`)

Finds a number `nonce` such that `sha256(prefix .. nonce)` (nonce in decimal) starts with a number of zero **bits**.
Native code runs about 4 million attempts per second, far faster than Lua.

| Function | Description |
|---|---|
| `mine(prefix, difficulty [, maxAttempts [, startNonce]])` | Returns `{nonce, hash, attempts}`, or `nil` if nothing was found within `maxAttempts` (1,000,000 by default, 5,000,000 at most). To continue, call it again with `startNonce + maxAttempts`. `difficulty` is 0 to 64. |
| `hash(prefix, nonce)` | `sha256(prefix .. nonce)`, hexadecimal. |
| `verify(prefix, nonce, difficulty)` | `true` if the nonce gives at least `difficulty` zero bits. |
| `difficulty(hashHex)` | Number of leading zero bits of a hexadecimal hash. |

## Classic cipher table (`classic_cipher`)

Historical ciphers for puzzles and teaching. They offer **no security**.

| Function | Description |
|---|---|
| `caesar(text, shift)` | Shifts every letter; a negative shift decrypts. |
| `rot13(text)`, `rot47(text)`, `atbash(text)` | Self-inverse ciphers. |
| `vigenere(text, key [, decrypt])` | Vigenère cipher; the key only contains letters. |
| `bruteCaesar(text)` | The 25 other possible shifts. |
| `frequency(text)` | How many times each letter appears. |
