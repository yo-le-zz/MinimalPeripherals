-- crypto_test.lua
-- Tests every function of the Cryptographic Accelerator (Cryptography Peripherals).
--
-- Usage:   crypto_test [side] [-v] [--bench]
--   side       peripheral name (default: the first cryptographic_accelerator found)
--   -v         print every passing check too
--   --bench    run a small speed benchmark at the end
--
-- Most checks use official test vectors (NIST, RFC 4231, RFC 5869, RFC 6070, RFC 7748,
-- RFC 7914, RFC 8032), so a failure means a real bug, not a typo in the test.

local verbose, bench, side = false, false, nil
for _, a in ipairs({ ... }) do
    if a == "-v" or a == "--verbose" then verbose = true
    elseif a == "--bench" then bench = true
    else side = a end
end

local crypto = side and peripheral.wrap(side) or peripheral.find("cryptographic_accelerator")
if not crypto then
    error("No cryptographic accelerator found. Attach one (or pass its name as argument).", 0)
end

-- ---------------------------------------------------------------- helpers

local function put(color, text)
    if term.isColor() then
        local old = term.getTextColor()
        term.setTextColor(color)
        write(text)
        term.setTextColor(old)
    else
        write(text)
    end
end

local function hex(s)
    return (s:gsub(".", function(c) return ("%02x"):format(c:byte()) end))
end

local function unhex(h)
    return (h:gsub("%x%x", function(b) return string.char(tonumber(b, 16)) end))
end

local function short(v)
    local s = tostring(v)
    if #s > 70 then s = s:sub(1, 67) .. "..." end
    return s
end

local passed, failed, failures, current = 0, 0, {}, ""

local function record(ok, name, detail)
    if ok then
        passed = passed + 1
        if verbose then
            put(colors.lime, "  PASS ")
            print(name)
        end
    else
        failed = failed + 1
        failures[#failures + 1] = current .. ": " .. name
        put(colors.red, "  FAIL ")
        print(name)
        if detail then
            put(colors.gray, "       " .. detail .. "\n")
        end
    end
end

local function section(title)
    current = title
    put(colors.yellow, "== " .. title .. "\n")
end

-- Calls a peripheral function; returns its result, or "<error: ...>" if it throws.
local function try(f, ...)
    local ok, res = pcall(f, ...)
    if ok then return res end
    return "<error: " .. tostring(res) .. ">"
end

local function eq(name, got, expected)
    record(got == expected, name, "got      " .. short(got) .. "\n       expected " .. short(expected))
end

local function check(name, cond, detail)
    record(cond and true or false, name, detail)
end

-- The call must raise an error containing `text` (plain match).
local function raises(name, text, f, ...)
    local ok, err = pcall(f, ...)
    record(not ok and tostring(err):find(text, 1, true) ~= nil, name,
        ok and "no error was raised" or ("error was: " .. short(err)))
end

local function bytes(n, from)
    local t = {}
    for i = 0, n - 1 do t[#t + 1] = string.char((from or 0) + i) end
    return table.concat(t)
end

-- ---------------------------------------------------------------- hashes

section("Hashes (known answers for \"abc\")")
eq("md5", try(crypto.md5, "abc"), "900150983cd24fb0d6963f7d28e17f72")
eq("md5 empty", try(crypto.md5, ""), "d41d8cd98f00b204e9800998ecf8427e")
eq("sha1", try(crypto.sha1, "abc"), "a9993e364706816aba3e25717850c26c9cd0d89d")
eq("sha256", try(crypto.sha256, "abc"), "ba7816bf8f01cfea414140de5dae2223b00361a396177a9cb410ff61f20015ad")
eq("sha512", try(crypto.sha512, "abc"),
    "ddaf35a193617abacc417349ae20413112e6fa4e89a97ea20a9eeee64b55d39a2192992a274fc1a836ba3c23a3feebbd454d4423643ce80e2a9ac94fa54ca49f")
eq("hash sha224", try(crypto.hash, "sha224", "abc"), "23097d223405d8228642a477bda255b32aadbce4bda0b3f7e36c9da7")
eq("hash sha384", try(crypto.hash, "sha384", "abc"),
    "cb00753f45a35e8bb5a03d699ac65007272c32ab0eded1631a8b605a43ff5bed8086072ba1e7cc2358baeca134c825a7")
eq("hash sha3_256", try(crypto.hash, "sha3_256", "abc"), "3a985da74fe225b2045c172d6bd390bd855f086e3e9d525b46bfe24511431532")
eq("hash ripemd160", try(crypto.hash, "ripemd160", "abc"), "8eb208f7e05d987a9b044a8e98c6b087f15a0bfc")
eq("hash blake2b_512", try(crypto.hash, "blake2b_512", "abc"),
    "ba80a53f981c4d0d6a2797b69f12f6e94c212f14685ac4b74b12bb6fdbffa2d17d87c5392aab792dc252d5de4533cc9518d38aa8dbf1925ab92386edd4009923")
eq("hash blake2s_256", try(crypto.hash, "blake2s_256", "abc"), "508c5e8c327c14e2e1a72ba34eeb452f37458b209ed63a294d999b4c86675982")
eq("hash sha256 == sha256()", try(crypto.hash, "sha256", "abc"), try(crypto.sha256, "abc"))
check("hash sha3_384 length", #tostring(try(crypto.hash, "sha3_384", "abc")) == 96)
check("hash sha3_512 length", #tostring(try(crypto.hash, "sha3_512", "abc")) == 128)
check("hash blake2b_256 length", #tostring(try(crypto.hash, "blake2b_256", "abc")) == 64)
raises("hash unknown algorithm", "bad argument", crypto.hash, "nope", "abc")

section("listHashes")
do
    local list = try(crypto.listHashes)
    check("returns a table", type(list) == "table", "got " .. type(list))
    if type(list) == "table" then
        local found = {}
        for _, name in pairs(list) do found[name] = true end
        eq("13 algorithms", #list, 13)
        for _, name in ipairs({ "md5", "sha1", "sha224", "sha256", "sha384", "sha512", "sha3_256", "sha3_384",
            "sha3_512", "ripemd160", "blake2b_256", "blake2b_512", "blake2s_256" }) do
            check("lists " .. name, found[name])
            -- every listed algorithm must actually work
            check("hash(" .. name .. ") works", type(try(crypto.hash, name, "x")) == "string"
                and not tostring(try(crypto.hash, name, "x")):find("<error", 1, true))
        end
    end
end

section("Output formats")
do
    local expected = unhex("ba7816bf8f01cfea414140de5dae2223b00361a396177a9cb410ff61f20015ad")
    eq("format hex", try(crypto.sha256, "abc", "hex"), hex(expected))
    eq("format true == hex", try(crypto.sha256, "abc", true), hex(expected))
    eq("format raw", try(crypto.sha256, "abc", "raw"), expected)
    eq("format false == raw", try(crypto.sha256, "abc", false), expected)
    eq("format base64", try(crypto.sha256, "abc", "base64"), "ungWv48Bz+pBQUDeXa4iI7ADYaOWF3qctBD/YfIAFa0=")
    eq("format base64url", try(crypto.sha256, "abc", "base64url"), "ungWv48Bz-pBQUDeXa4iI7ADYaOWF3qctBD_YfIAFa0")
    raises("format invalid", "bad argument", crypto.sha256, "abc", "octal")
    raises("format wrong type", "bad argument", crypto.sha256, "abc", 12)
end

section("HMAC (RFC 4231 test case 2)")
do
    local key, data = "Jefe", "what do ya want for nothing?"
    eq("hmacSha256", try(crypto.hmacSha256, key, data), "5bdcc146bf60754e6a042426089575c75a003f089d2739839dec58b964ec3843")
    eq("hmacSha512", try(crypto.hmacSha512, key, data),
        "164b7a7bfcf819e2e395fbe73b56e0a387bd64222e831fd610270cd7ea2505549758bf75c05a994a6d034f65f8f0e6fdcaeab1a34d4a6b4b636e070a38bce737")
    eq("hmacSha1", try(crypto.hmacSha1, key, data), "effcdf6ae5eb2fa2d27416d5f184df9c259a7c79")
    eq("hmacMd5", try(crypto.hmacMd5, key, data), "750c783e6ab0b503eaa86e310a5db738")
    eq("hmac sha256 generic", try(crypto.hmac, "sha256", key, data), try(crypto.hmacSha256, key, data))
    eq("hmac sha3_256 vs itself", try(crypto.hmac, "sha3_256", key, data), try(crypto.hmac, "sha3_256", key, data))
    check("hmac depends on key", try(crypto.hmac, "sha256", "a", data) ~= try(crypto.hmac, "sha256", "b", data))
    -- regression: the whole message must be hashed, not only #key bytes of it
    check("hmac covers the whole message", try(crypto.hmacSha256, "k", "message-one") ~= try(crypto.hmacSha256, "k", "message-two"))
    eq("hmac base64", try(crypto.hmacSha256, key, data, "base64"), "W9zBRr9gdU5qBCQmCJV1x1oAPwidJzmDnexYuWTsOEM=")
end

section("BLAKE3")
do
    eq("blake3 empty", try(crypto.blake3, ""), "af1349b9f5f9a1a6a0404dea36dcc9499bcb25c9adc112b7cc9a93cae41f3262")
    eq("blake3 output size 64", #tostring(try(crypto.blake3, "abc", "hex", "hash", nil, 64)), 128)
    eq("blake3 raw size", #tostring(try(crypto.blake3, "abc", "raw")), 32)
    local key = bytes(32)
    local keyed = try(crypto.blake3, "abc", "hex", "keyed", key)
    check("blake3 keyed differs from hash", keyed ~= try(crypto.blake3, "abc") and #keyed == 64)
    check("blake3 keyed depends on key", keyed ~= try(crypto.blake3, "abc", "hex", "keyed", bytes(32, 1)))
    check("blake3 derive", #tostring(try(crypto.blake3, "material", "hex", "derive", "my context")) == 64)
    raises("blake3 keyed with a short key", "", crypto.blake3, "abc", "hex", "keyed", "short")
    raises("blake3 output size 0", "output size", crypto.blake3, "abc", "hex", "hash", nil, 0)
    raises("blake3 output too large", "output size", crypto.blake3, "abc", "hex", "hash", nil, 100000)
    raises("blake3 unknown mode", "bad argument", crypto.blake3, "abc", "hex", "weird")
end

section("Checksums and comparison")
do
    eq("crc32", try(crypto.crc32, "123456789"), tonumber("cbf43926", 16))
    eq("adler32", try(crypto.adler32, "123456789"), tonumber("091e01de", 16))
    eq("crc32 hex", try(crypto.crc32, "123456789", "hex"), "cbf43926")
    eq("adler32 hex", try(crypto.adler32, "123456789", "hex"), "091e01de")
    eq("constantTimeEquals equal", try(crypto.constantTimeEquals, "secret", "secret"), true)
    eq("constantTimeEquals different", try(crypto.constantTimeEquals, "secret", "secreT"), false)
    eq("constantTimeEquals length", try(crypto.constantTimeEquals, "secret", "secret1"), false)
end

-- ---------------------------------------------------------------- key derivation

section("PBKDF2 (RFC 6070, HMAC-SHA1)")
do
    eq("1 iteration", try(crypto.pbkdf2, "password", "salt", 1, 20, "sha1", "hex"), "0c60c80f961f0e71f3a9b524af6012062fe037a6")
    eq("2 iterations", try(crypto.pbkdf2, "password", "salt", 2, 20, "sha1", "hex"), "ea6c014dc72d6f8ccd1ed92ace1d41f0d8de8957")
    eq("4096 iterations", try(crypto.pbkdf2, "password", "salt", 4096, 20, "sha1", "hex"), "4b007901b765489abead49d926f721d065a429c1")
    eq("default is raw sha256", #tostring(try(crypto.pbkdf2, "pw", "salt", 10, 32)), 32)
    eq("length 64", #tostring(try(crypto.pbkdf2, "pw", "salt", 10, 64, "sha512")), 64)
    raises("too many iterations", "iterations", crypto.pbkdf2, "pw", "salt", 5000000, 32)
    raises("zero iterations", "iterations", crypto.pbkdf2, "pw", "salt", 0, 32)
    raises("length too large", "length", crypto.pbkdf2, "pw", "salt", 1, 100000)
end

section("HKDF (RFC 5869 test case 1)")
do
    local ikm = ("\11"):rep(22)
    local salt = bytes(13)
    local info = bytes(10, 0xf0)
    eq("okm", try(crypto.hkdf, ikm, salt, info, 42, "sha256", "hex"),
        "3cb25f25faacd57a90434f64d0362f2a2d2d0a90cf1a5a4c5db02d56ecc4c5bf34007208d5b887185865")
    eq("empty salt and info", #tostring(try(crypto.hkdf, ikm, "", "", 32)), 32)
    raises("length too large for sha256", "length", crypto.hkdf, ikm, salt, info, 255 * 32 + 1)
end

section("scrypt (RFC 7914 test vector 1)")
do
    eq("n=16 r=1 p=1", try(crypto.scrypt, "", "", 16, 1, 1, 64, "hex"),
        "77d6576238657b203b19ca42c18a0497f16b4844e3074ae8dfdffa3fede21442fcd0069ded0948f8326a753a0fc81f17e8d3e0fb2e0d3628cf35e20c38d18906")
    raises("n not a power of two", "power of two", crypto.scrypt, "pw", "salt", 100, 1, 1, 32)
    raises("n too large", "n", crypto.scrypt, "pw", "salt", 1048576, 1, 1, 32)
    raises("r too large", "r", crypto.scrypt, "pw", "salt", 16, 100, 1, 32)
    raises("p too large", "p", crypto.scrypt, "pw", "salt", 16, 1, 100, 32)
end

-- ---------------------------------------------------------------- symmetric encryption

section("AES-CBC / AES-CTR")
do
    for _, size in ipairs({ 16, 24, 32 }) do
        local key, iv = bytes(size, 1), bytes(16, 100)
        local enc = try(crypto.encryptAes, "hello world", key, iv)
        eq("cbc roundtrip, " .. size * 8 .. " bit key", try(crypto.decryptAes, enc, key, iv), "hello world")
    end
    local key, iv = bytes(32), bytes(16)
    eq("cbc pads to a block", #tostring(try(crypto.encryptAes, "hello world", key, iv)), 16)
    eq("cbc explicit mode", try(crypto.encryptAes, "hello world", key, iv, "cbc"), try(crypto.encryptAes, "hello world", key, iv))
    local data = "twenty one bytes long"
    local ctr = try(crypto.encryptAes, data, key, iv, "ctr")
    eq("ctr keeps the length", #tostring(ctr), #data)
    eq("ctr roundtrip", try(crypto.decryptAes, ctr, key, iv, "ctr"), data)
    check("ctr differs from cbc", ctr ~= try(crypto.encryptAes, data, key, iv))
    raises("bad key length", "bad argument", crypto.encryptAes, "x", "short", iv)
    raises("bad iv length", "bad argument", crypto.encryptAes, "x", key, "short")
    raises("unknown mode", "bad argument", crypto.encryptAes, "x", key, iv, "ecb")
end

section("AES-GCM")
do
    -- NIST GCM test case 2: key = 0^128, nonce = 0^96, plaintext = 0^128
    eq("NIST test case 2", hex(try(crypto.encryptAesGcm, ("\0"):rep(16), ("\0"):rep(16), ("\0"):rep(12))),
        "0388dace60b6a392f328c2b971b2fe78ab6e47d42cec13bdf53a67b21257bddf")
    local key, nonce = bytes(32), bytes(12)
    local sealed = try(crypto.encryptAesGcm, "secret message", key, nonce, "header")
    eq("tag is 16 bytes", #tostring(sealed), #"secret message" + 16)
    eq("roundtrip with aad", try(crypto.decryptAesGcm, sealed, key, nonce, "header"), "secret message")
    eq("roundtrip without aad", try(crypto.decryptAesGcm, try(crypto.encryptAesGcm, "abc", key, nonce), key, nonce), "abc")
    raises("wrong aad", "authentication failed", crypto.decryptAesGcm, sealed, key, nonce, "other")
    raises("wrong key", "authentication failed", crypto.decryptAesGcm, sealed, bytes(32, 1), nonce, "header")
    raises("wrong nonce", "authentication failed", crypto.decryptAesGcm, sealed, key, bytes(12, 1), "header")
    local tampered = sealed:sub(1, -2) .. string.char((sealed:byte(-1) + 1) % 256)
    raises("tampered data", "authentication failed", crypto.decryptAesGcm, tampered, key, nonce, "header")
    raises("data too short", "too short", crypto.decryptAesGcm, "tiny", key, nonce)
    raises("bad nonce length", "bad argument", crypto.encryptAesGcm, "x", key, "short")
    raises("bad key length", "bad argument", crypto.encryptAesGcm, "x", "short", nonce)
end

section("ChaCha20-Poly1305")
do
    local key, nonce = bytes(32), bytes(12)
    local sealed = try(crypto.encryptChaCha20Poly1305, "secret message", key, nonce, "header")
    eq("tag is 16 bytes", #tostring(sealed), #"secret message" + 16)
    eq("roundtrip with aad", try(crypto.decryptChaCha20Poly1305, sealed, key, nonce, "header"), "secret message")
    eq("roundtrip without aad", try(crypto.decryptChaCha20Poly1305, try(crypto.encryptChaCha20Poly1305, "abc", key, nonce), key, nonce), "abc")
    check("differs from AES-GCM", sealed ~= try(crypto.encryptAesGcm, "secret message", key, nonce, "header"))
    raises("wrong aad", "authentication failed", crypto.decryptChaCha20Poly1305, sealed, key, nonce, "other")
    raises("wrong key", "authentication failed", crypto.decryptChaCha20Poly1305, sealed, bytes(32, 1), nonce, "header")
    local tampered = sealed:sub(1, -2) .. string.char((sealed:byte(-1) + 1) % 256)
    raises("tampered data", "authentication failed", crypto.decryptChaCha20Poly1305, tampered, key, nonce, "header")
    raises("key must be 32 bytes", "bad argument", crypto.encryptChaCha20Poly1305, "x", bytes(16), nonce)
    raises("nonce must be 12 bytes", "bad argument", crypto.encryptChaCha20Poly1305, "x", key, bytes(16))
end

-- ---------------------------------------------------------------- asymmetric

section("Ed25519 (RFC 8032 test 1)")
do
    local priv = unhex("9d61b19deffd5a60ba844af492ec2cc44449c5697b326919703bac031cae7f60")
    local pub = unhex("d75a980182b10ab7d54bfed3c964073a0ee172f3daa62325af021a68f707511a")
    eq("derivePublicKey", try(crypto.derivePublicKey, priv), pub)
    local sig = try(crypto.sign, "", priv)
    eq("signature of the empty message", hex(sig),
        "e5564300c360ac729086e2cc806e828a84877f1eb8e5d974d873e065224901555fb8821590a33bacc61e39701cf9b46bd25bf5f0595bbe24655141438e7a100b")
    eq("verify", try(crypto.verify, "", sig, pub), true)
    eq("verify wrong message", try(crypto.verify, "x", sig, pub), false)
    local bad = sig:sub(1, -2) .. string.char((sig:byte(-1) + 1) % 256)
    eq("verify tampered signature", try(crypto.verify, "", bad, pub), false)
    raises("sign with a bad key", "Invalid key length", crypto.sign, "x", "short")
    raises("verify with a bad key", "Invalid key length", crypto.verify, "x", sig, "short")
    raises("verify with a bad signature", "Invalid signature length", crypto.verify, "x", "short", pub)
end

section("Key generation")
do
    local priv = try(crypto.generatePrivateKey)
    eq("generatePrivateKey length", #tostring(priv), 32)
    check("generatePrivateKey is random", priv ~= try(crypto.generatePrivateKey))

    local pair = try(crypto.generateKeyPair)
    check("generateKeyPair returns a table", type(pair) == "table")
    if type(pair) == "table" then
        eq("ed25519 private length", #pair.privateKey, 32)
        eq("ed25519 public length", #pair.publicKey, 32)
        eq("public key matches the private key", try(crypto.derivePublicKey, pair.privateKey), pair.publicKey)
        local sig = try(crypto.sign, "hello", pair.privateKey)
        eq("signature length", #tostring(sig), 64)
        eq("sign + verify", try(crypto.verify, "hello", sig, pair.publicKey), true)
        eq("signatures are deterministic", try(crypto.sign, "hello", pair.privateKey), sig)
    end

    local x = try(crypto.generateKeyPair, "x25519")
    check("x25519 pair", type(x) == "table" and #x.privateKey == 32 and #x.publicKey == 32)
    if type(x) == "table" then
        eq("x25519 public matches", try(crypto.deriveEcdhPublicKey, x.privateKey), x.publicKey)
    end
    raises("unknown key type", "bad argument", crypto.generateKeyPair, "rsa")
end

section("X25519 (RFC 7748)")
do
    local alicePriv = unhex("77076d0a7318a57d3c16c17251b26645df4c2f87ebc0992ab177fba51db92c2a")
    local alicePub = unhex("8520f0098930a754748b7ddcb43ef75a0dbf3a0d26381af4eba4a98eaa9b4e6a")
    local bobPriv = unhex("5dab087e624a8a4b79e17f8b83800ee66f3bb1292618b6fd1c2f8b27ff88e0eb")
    local bobPub = unhex("de9edb7d7b7dc1b4d35b61c2ece435373f8343c85b78674dadfc7e146f882b4f")
    local shared = "4a5d9d5ba4ce2de1728e3bf480350f25e07e21c947d19e3376f09b3c1e161742"
    eq("alice public key", try(crypto.deriveEcdhPublicKey, alicePriv), alicePub)
    eq("bob public key", try(crypto.deriveEcdhPublicKey, bobPriv), bobPub)
    eq("shared secret (alice side)", hex(try(crypto.computeSharedSecret, alicePriv, bobPub)), shared)
    eq("shared secret (bob side)", hex(try(crypto.computeSharedSecret, bobPriv, alicePub)), shared)
    raises("all-zero public key", "invalid public key", crypto.computeSharedSecret, alicePriv, ("\0"):rep(32))
    raises("bad key length", "Invalid key length", crypto.computeSharedSecret, "short", bobPub)
end

-- ---------------------------------------------------------------- random, encodings, compression

section("Random")
do
    local ok = true
    for _ = 1, 200 do
        local r = try(crypto.random)
        if type(r) ~= "number" or r < 0 or r >= 1 then ok = false end
    end
    check("random() in [0, 1)", ok)

    ok = true
    for _ = 1, 200 do
        local r = try(crypto.random, 10)
        if type(r) ~= "number" or r < 0 or r >= 10 then ok = false end
    end
    check("random(max) in [0, max)", ok)

    ok = true
    for _ = 1, 200 do
        local r = try(crypto.random, 5, 6)
        if type(r) ~= "number" or r < 5 or r >= 6 then ok = false end
    end
    check("random(min, max) in [min, max)", ok)
    raises("random empty interval", "interval is empty", crypto.random, 5, 5)
    raises("random negative max", "interval is empty", crypto.random, -1)

    local seen, inRange = {}, true
    for _ = 1, 600 do
        local r = try(crypto.randomInt, 1, 6)
        if type(r) ~= "number" or r < 1 or r > 6 or r ~= math.floor(r) then inRange = false else seen[r] = true end
    end
    check("randomInt(1, 6) stays in range", inRange)
    local distinct = 0
    for _ in pairs(seen) do distinct = distinct + 1 end
    eq("randomInt(1, 6) reaches all 6 values", distinct, 6)
    eq("randomInt(3, 3)", try(crypto.randomInt, 3, 3), 3)
    raises("randomInt empty interval", "interval is empty", crypto.randomInt, 5, 4)

    eq("randomBytes length", #tostring(try(crypto.randomBytes, 16)), 16)
    eq("randomBytes 0", try(crypto.randomBytes, 0), "")
    eq("randomBytes hex", #tostring(try(crypto.randomBytes, 16, "hex")), 32)
    eq("randomBytes base64", #tostring(try(crypto.randomBytes, 12, "base64")), 16)
    check("randomBytes is random", try(crypto.randomBytes, 16) ~= try(crypto.randomBytes, 16))
    raises("randomBytes negative", "negative", crypto.randomBytes, -1)
    raises("randomBytes too large", "limit", crypto.randomBytes, 100000)

    local uuid = try(crypto.randomUuid)
    check("randomUuid format", tostring(uuid):match("^%x%x%x%x%x%x%x%x%-%x%x%x%x%-%x%x%x%x%-%x%x%x%x%-%x%x%x%x%x%x%x%x%x%x%x%x$") ~= nil,
        "got " .. short(uuid))
    check("randomUuid is random", uuid ~= try(crypto.randomUuid))
end

section("Encodings")
do
    eq("encodeBase64", try(crypto.encodeBase64, "Man"), "TWFu")
    eq("encodeBase64 padding", try(crypto.encodeBase64, "Ma"), "TWE=")
    eq("decodeBase64", try(crypto.decodeBase64, "TWFu"), "Man")
    eq("decodeBase64 invalid", try(crypto.decodeBase64, "!!!"), nil)
    eq("base64 standard alphabet", try(crypto.encodeBase64, "\251\255"), "+/8=")
    eq("base64 url-safe alphabet", try(crypto.encodeBase64, "\251\255", true), "-_8")
    eq("decode url-safe", try(crypto.decodeBase64, "-_8", true), "\251\255")
    local all = bytes(256)
    eq("base64 roundtrip, all byte values", try(crypto.decodeBase64, try(crypto.encodeBase64, all)), all)
    eq("url-safe roundtrip, all byte values", try(crypto.decodeBase64, try(crypto.encodeBase64, all, true), true), all)
    eq("encodeHex", try(crypto.encodeHex, "abc"), "616263")
    eq("decodeHex", try(crypto.decodeHex, "616263"), "abc")
    eq("decodeHex upper case", try(crypto.decodeHex, "6162AB"), "ab\171")
    eq("decodeHex invalid", try(crypto.decodeHex, "zz"), nil)
    eq("decodeHex odd length", try(crypto.decodeHex, "abc"), nil)
    eq("hex roundtrip, all byte values", try(crypto.decodeHex, try(crypto.encodeHex, all)), all)
end

section("Compression")
do
    local data = ("a"):rep(1000)
    local packed = try(crypto.deflate, data)
    check("deflate shrinks repetitive data", #packed < 100, "size " .. #packed)
    eq("inflate roundtrip", try(crypto.inflate, packed), data)
    local stored, best = try(crypto.deflate, data, 0), try(crypto.deflate, data, 9)
    check("level 0 stores", #stored > 1000, "size " .. #stored)
    check("level 9 is smaller than level 0", #best < #stored)
    eq("inflate level 0", try(crypto.inflate, stored), data)
    eq("inflate level 9", try(crypto.inflate, best), data)
    eq("empty string", try(crypto.inflate, try(crypto.deflate, "")), "")
    local binary = bytes(256):rep(4)
    eq("binary roundtrip", try(crypto.inflate, try(crypto.deflate, binary)), binary)
    eq("inflate within maxSize", try(crypto.inflate, packed, 1000), data)
    raises("inflate over maxSize", "size limit", crypto.inflate, packed, 10)
    raises("inflate invalid data", "invalid", crypto.inflate, "this is not deflate data")
    raises("deflate bad level", "level", crypto.deflate, "x", 10)
    raises("inflate maxSize above the global limit", "maxSize", crypto.inflate, packed, 1 / 0 > 0 and 2 ^ 31 - 1 or 0)
end

section("getLimits")
do
    local limits = try(crypto.getLimits)
    check("returns a table", type(limits) == "table")
    if type(limits) == "table" then
        for _, key in ipairs({ "randomBytesMaxSize", "blake3MaxOutput", "kdfMaxOutput", "pbkdf2MaxIterations",
            "scryptMaxN", "scryptMaxR", "scryptMaxP", "inflateMaxSize" }) do
            check("has " .. key, type(limits[key]) == "number" and limits[key] > 0)
        end
        eq("randomBytesMaxSize", limits.randomBytesMaxSize, 65536)
        eq("inflateMaxSize", limits.inflateMaxSize, 8 * 1024 * 1024)
    end
end

-- ---------------------------------------------------------------- benchmark

if bench then
    section("Benchmark")
    local function time(label, count, f)
        local start = os.epoch("utc")
        for _ = 1, count do f() end
        local ms = os.epoch("utc") - start
        print(("  %-34s %6d ms  (%d calls)"):format(label, ms, count))
    end
    local blob = ("x"):rep(4096)
    time("sha256, 4 KiB", 200, function() crypto.sha256(blob) end)
    time("hash sha3_512, 4 KiB", 200, function() crypto.hash("sha3_512", blob) end)
    time("blake3, 4 KiB", 200, function() crypto.blake3(blob) end)
    time("encryptAesGcm, 4 KiB", 200, function() crypto.encryptAesGcm(blob, bytes(32), bytes(12)) end)
    time("sign (Ed25519)", 100, function() crypto.sign("message", bytes(32)) end)
    time("pbkdf2 sha256, 100000 iterations", 3, function() crypto.pbkdf2("pw", "salt", 100000, 32) end)
    time("scrypt n=16384 r=8 p=1", 3, function() crypto.scrypt("pw", "salt", 16384, 8, 1, 32) end)
end

-- ---------------------------------------------------------------- summary

print()
if failed == 0 then
    put(colors.lime, ("All %d checks passed.\n"):format(passed))
else
    put(colors.red, ("%d of %d checks failed:\n"):format(failed, passed + failed))
    for _, f in ipairs(failures) do
        put(colors.gray, "  - " .. f .. "\n")
    end
end
