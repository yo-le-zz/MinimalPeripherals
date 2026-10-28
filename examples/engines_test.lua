-- engines_test.lua
-- Tests the specialized blocks of Cryptography Peripherals (everything except the general
-- cryptographic_accelerator, which has its own crypto_test.lua).
--
-- Usage:   engines_test [-v]
-- Attach the blocks you want to test (directly or through wired modems). A block that is
-- not attached is skipped. Known-answer tests come from NIST, RFC 4231, 5869, 6070, 6238,
-- 7748, 7914, 8032 and similar sources.

local verbose = false
for _, a in ipairs({ ... }) do
    if a == "-v" or a == "--verbose" then verbose = true end
end

-- ---------------------------------------------------------------- harness

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

local function bytes(n, from)
    local t = {}
    for i = 0, n - 1 do t[#t + 1] = string.char(((from or 0) + i) % 256) end
    return table.concat(t)
end

local passed, failed, skipped, failures, current = 0, 0, 0, {}, ""

local function record(ok, name, detail)
    if ok then
        passed = passed + 1
        if verbose then put(colors.lime, "  PASS "); print(name) end
    else
        failed = failed + 1
        failures[#failures + 1] = current .. ": " .. name
        put(colors.red, "  FAIL "); print(name)
        if detail then put(colors.gray, "       " .. detail .. "\n") end
    end
end

local function try(f, ...)
    local ok, res = pcall(f, ...)
    if ok then return res end
    return "<error: " .. tostring(res) .. ">"
end

local function eq(name, got, expected)
    record(got == expected, name, "got      " .. short(got) .. "\n       expected " .. short(expected))
end

local function check(name, cond, detail) record(cond and true or false, name, detail) end

local function raises(name, text, f, ...)
    local ok, err = pcall(f, ...)
    record(not ok and tostring(err):find(text, 1, true) ~= nil, name,
        ok and "no error was raised" or ("error was: " .. short(err)))
end

-- Starts a section; returns the peripheral, or nil (and counts a skip) if it is not attached.
local function section(title, peripheralType)
    current = title
    local p = peripheral.find(peripheralType)
    if not p then
        skipped = skipped + 1
        put(colors.orange, "-- " .. title .. " (" .. peripheralType .. " not attached, skipped)\n")
        return nil
    end
    put(colors.yellow, "== " .. title .. "\n")
    return p
end

local function b32(s)
    local alphabet = "ABCDEFGHIJKLMNOPQRSTUVWXYZ234567"
    local bits = {}
    for i = 1, #s do
        local b = s:byte(i)
        for j = 7, 0, -1 do bits[#bits + 1] = math.floor(b / 2 ^ j) % 2 end
    end
    while #bits % 5 ~= 0 do bits[#bits + 1] = 0 end
    local out = {}
    for i = 1, #bits, 5 do
        local v = bits[i] * 16 + bits[i + 1] * 8 + bits[i + 2] * 4 + bits[i + 3] * 2 + bits[i + 4]
        out[#out + 1] = alphabet:sub(v + 1, v + 1)
    end
    return table.concat(out)
end

-- ---------------------------------------------------------------- hash engines

local hashEngines = {
    { "md5_engine", "MD5", 16, "900150983cd24fb0d6963f7d28e17f72" },
    { "sha1_engine", "SHA-1", 20, "a9993e364706816aba3e25717850c26c9cd0d89d" },
    { "sha256_engine", "SHA-256", 32, "ba7816bf8f01cfea414140de5dae2223b00361a396177a9cb410ff61f20015ad" },
    { "sha384_engine", "SHA-384", 48,
        "cb00753f45a35e8bb5a03d699ac65007272c32ab0eded1631a8b605a43ff5bed8086072ba1e7cc2358baeca134c825a7" },
    { "sha512_engine", "SHA-512", 64,
        "ddaf35a193617abacc417349ae20413112e6fa4e89a97ea20a9eeee64b55d39a2192992a274fc1a836ba3c23a3feebbd454d4423643ce80e2a9ac94fa54ca49f" },
    { "sha3_256_engine", "SHA3-256", 32, "3a985da74fe225b2045c172d6bd390bd855f086e3e9d525b46bfe24511431532" },
    { "sha3_512_engine", "SHA3-512", 64,
        "b751850b1a57168a5693cd924b6b096e08f621827444f70d884f5d0240d2712e10e116e9192af3c91a7ec57647e3934057340b4cf408d5a56592f8274eec53f0" },
    { "ripemd160_engine", "RIPEMD-160", 20, "8eb208f7e05d987a9b044a8e98c6b087f15a0bfc" },
    { "blake2b_engine", "BLAKE2b", 64,
        "ba80a53f981c4d0d6a2797b69f12f6e94c212f14685ac4b74b12bb6fdbffa2d17d87c5392aab792dc252d5de4533cc9518d38aa8dbf1925ab92386edd4009923" },
    { "blake2s_engine", "BLAKE2s", 32, "508c5e8c327c14e2e1a72ba34eeb452f37458b209ed63a294d999b4c86675982" },
}

for _, spec in ipairs(hashEngines) do
    local engineType, title, size, abc = spec[1], spec[2], spec[3], spec[4]
    local e = section(title .. " engine", engineType)
    if e then
        eq("hash \"abc\"", try(e.hash, "abc"), abc)
        eq("raw", hex(tostring(try(e.raw, "abc"))), abc)
        eq("size", try(e.size), size)
        eq("verify (lower case)", try(e.verify, "abc", abc), true)
        eq("verify (upper case)", try(e.verify, "abc", abc:upper()), true)
        eq("verify wrong data", try(e.verify, "abd", abc), false)
        eq("verify wrong length", try(e.verify, "abc", abc:sub(1, -2)), false)

        local many = try(e.many, { "abc", "", "abc" })
        check("many returns a list of 3", type(many) == "table" and #many == 3, "got " .. short(many))
        if type(many) == "table" and #many == 3 then
            eq("many[1]", many[1], abc)
            eq("many[2] == hash of empty", many[2], try(e.hash, ""))
            eq("many[3]", many[3], abc)
        end
        eq("many with an empty list", #try(e.many, {}), 0)
        raises("many with a non-string item", "list of strings", e.many, { "a", 5 })

        eq("twice == hash(raw hash)", try(e.twice, "abc"), try(e.hash, unhex(abc)))
        eq("chain 1 round", try(e.chain, "abc", 1), abc)
        eq("chain 2 rounds == twice", try(e.chain, "abc", 2), try(e.twice, "abc"))
        raises("chain 0 rounds", "rounds", e.chain, "abc", 0)
        raises("chain too many rounds", "rounds", e.chain, "abc", 10000000)

        local mac = try(e.hmac, "key", "message")
        eq("hmac size", #tostring(mac), size * 2)
        check("hmac depends on the key", mac ~= try(e.hmac, "other", "message"))
        check("hmac depends on the message", mac ~= try(e.hmac, "key", "other"))
        eq("hmac with an empty key works", #tostring(try(e.hmac, "", "message")), size * 2)
        eq("verifyHmac", try(e.verifyHmac, "key", "message", mac), true)
        eq("verifyHmac wrong key", try(e.verifyHmac, "nope", "message", mac), false)
        local macs = try(e.hmacMany, "key", { "message", "other" })
        eq("hmacMany[1]", macs[1], mac)
        eq("hmacMany[2]", macs[2], try(e.hmac, "key", "other"))
    end
end

do
    local e = section("HMAC known answers (RFC 4231 test case 2)", "sha256_engine")
    if e then
        eq("hmac-sha256", try(e.hmac, "Jefe", "what do ya want for nothing?"),
            "5bdcc146bf60754e6a042426089575c75a003f089d2739839dec58b964ec3843")
        eq("sha256d(\"abc\")", try(e.twice, "abc"), "4f8b42c22dd3729b519ba6f68d2da7cc5b2d606d05daed5ad5128cc03e6c6358")
    end
    local s = peripheral.find("sha512_engine")
    if s then
        eq("hmac-sha512", try(s.hmac, "Jefe", "what do ya want for nothing?"),
            "164b7a7bfcf819e2e395fbe73b56e0a387bd64222e831fd610270cd7ea2505549758bf75c05a994a6d034f65f8f0e6fdcaeab1a34d4a6b4b636e070a38bce737")
    end
    local s1 = peripheral.find("sha1_engine")
    if s1 then
        eq("hmac-sha1", try(s1.hmac, "Jefe", "what do ya want for nothing?"), "effcdf6ae5eb2fa2d27416d5f184df9c259a7c79")
    end
    local m5 = peripheral.find("md5_engine")
    if m5 then
        eq("hmac-md5", try(m5.hmac, "Jefe", "what do ya want for nothing?"), "750c783e6ab0b503eaa86e310a5db738")
    end
end

do
    local e = section("BLAKE3 engine", "blake3_engine")
    if e then
        eq("hash empty", try(e.hash, ""), "af1349b9f5f9a1a6a0404dea36dcc9499bcb25c9adc112b7cc9a93cae41f3262")
        eq("raw size", #tostring(try(e.raw, "abc")), 32)
        eq("output size 64", #tostring(try(e.hash, "abc", 64)), 128)
        eq("raw output size 7", #tostring(try(e.raw, "abc", 7)), 7)
        local key = bytes(32)
        local mac = try(e.keyed, key, "abc")
        eq("keyed size", #tostring(mac), 64)
        check("keyed differs from hash", mac ~= try(e.hash, "abc"))
        check("keyed depends on the key", mac ~= try(e.keyed, bytes(32, 1), "abc"))
        eq("derive size", #tostring(try(e.derive, "my context", "material", 48)), 48)
        check("derive depends on the context", try(e.derive, "a", "material") ~= try(e.derive, "b", "material"))
        local many = try(e.many, { "", "abc" })
        eq("many[1]", many[1], "af1349b9f5f9a1a6a0404dea36dcc9499bcb25c9adc112b7cc9a93cae41f3262")
        eq("many[2]", many[2], try(e.hash, "abc"))
        raises("output size 0", "output size", e.hash, "abc", 0)
        raises("output size too large", "output size", e.hash, "abc", 100000)
        raises("keyed with a short key", "", e.keyed, "short", "abc")
    end
end

-- ---------------------------------------------------------------- ciphers

do
    local e = section("AES engine", "aes_engine")
    if e then
        eq("generateKey default", #tostring(try(e.generateKey)), 32)
        eq("generateKey 128", #tostring(try(e.generateKey, 128)), 16)
        eq("generateKey 192", #tostring(try(e.generateKey, 192)), 24)
        raises("generateKey 100", "bits", e.generateKey, 100)
        check("generateKey is random", try(e.generateKey) ~= try(e.generateKey))

        eq("NIST GCM test case 2", hex(tostring(try(e.encrypt, ("\0"):rep(16), ("\0"):rep(16), ("\0"):rep(12)))),
            "0388dace60b6a392f328c2b971b2fe78ab6e47d42cec13bdf53a67b21257bddf")

        local key = try(e.generateKey)
        local sealed = try(e.seal, "secret message", key, "header")
        eq("sealed size (nonce + data + tag)", #tostring(sealed), 12 + #"secret message" + 16)
        eq("open", try(e.open, sealed, key, "header"), "secret message")
        check("two seals differ (random nonce)", sealed ~= try(e.seal, "secret message", key, "header"))
        eq("seal without aad", try(e.open, try(e.seal, "abc", key), key), "abc")
        eq("seal empty data", try(e.open, try(e.seal, "", key), key), "")
        raises("open with another aad", "authentication failed", e.open, sealed, key, "other")
        raises("open with another key", "authentication failed", e.open, sealed, try(e.generateKey), "header")
        local tampered = sealed:sub(1, -2) .. string.char((sealed:byte(-1) + 1) % 256)
        raises("open tampered data", "authentication failed", e.open, tampered, key, "header")
        raises("open too short", "too short", e.open, "tiny", key)
        raises("bad key length", "bad argument", e.seal, "x", "short")

        local many = try(e.sealMany, { "one", "two", "three" }, key)
        eq("sealMany size", #many, 3)
        eq("sealMany[2]", try(e.open, many[2], key), "two")

        local nonce = bytes(12)
        local enc = try(e.encrypt, "data", key, nonce, "aad")
        eq("encrypt/decrypt", try(e.decrypt, enc, key, nonce, "aad"), "data")
        raises("decrypt with another nonce", "authentication failed", e.decrypt, enc, key, bytes(12, 1), "aad")
        raises("bad nonce length", "bad argument", e.encrypt, "x", key, "short")

        local iv = bytes(16)
        local text = "twenty one bytes long"
        local ctr = try(e.ctr, text, key, iv)
        eq("ctr keeps the length", #tostring(ctr), #text)
        eq("ctr is its own inverse", try(e.ctr, ctr, key, iv), text)
        local cbc = try(e.cbcEncrypt, "hello world", key, iv)
        eq("cbc pads to a block", #tostring(cbc), 16)
        eq("cbc roundtrip", try(e.cbcDecrypt, cbc, key, iv), "hello world")
        raises("cbc bad iv", "bad argument", e.cbcEncrypt, "x", key, "short")
    end
end

do
    local e = section("ChaCha20-Poly1305 engine", "chacha_engine")
    if e then
        local key = try(e.generateKey)
        eq("generateKey size", #tostring(key), 32)
        local sealed = try(e.seal, "secret message", key, "header")
        eq("sealed size", #tostring(sealed), 12 + #"secret message" + 16)
        eq("open", try(e.open, sealed, key, "header"), "secret message")
        eq("seal empty data", try(e.open, try(e.seal, "", key), key), "")
        raises("wrong aad", "authentication failed", e.open, sealed, key, "other")
        raises("wrong key", "authentication failed", e.open, sealed, try(e.generateKey), "header")
        local tampered = sealed:sub(1, -2) .. string.char((sealed:byte(-1) + 1) % 256)
        raises("tampered data", "authentication failed", e.open, tampered, key, "header")
        raises("too short", "too short", e.open, "tiny", key)
        raises("key must be 32 bytes", "bad argument", e.seal, "x", bytes(16))
        local many = try(e.sealMany, { "a", "b" }, key)
        eq("sealMany[2]", try(e.open, many[2], key), "b")
        local nonce = bytes(12)
        local enc = try(e.encrypt, "data", key, nonce, "aad")
        eq("encrypt/decrypt", try(e.decrypt, enc, key, nonce, "aad"), "data")
        raises("decrypt with another nonce", "authentication failed", e.decrypt, enc, key, bytes(12, 1), "aad")
    end
end

-- ---------------------------------------------------------------- signatures and key exchange

do
    local e = section("Ed25519 signature engine (RFC 8032 test 1)", "signature_engine")
    if e then
        local priv = unhex("9d61b19deffd5a60ba844af492ec2cc44449c5697b326919703bac031cae7f60")
        local pub = unhex("d75a980182b10ab7d54bfed3c964073a0ee172f3daa62325af021a68f707511a")
        eq("publicKey", try(e.publicKey, priv), pub)
        local sig = try(e.sign, "", priv)
        eq("signature of the empty message", hex(tostring(sig)),
            "e5564300c360ac729086e2cc806e828a84877f1eb8e5d974d873e065224901555fb8821590a33bacc61e39701cf9b46bd25bf5f0595bbe24655141438e7a100b")
        eq("verify", try(e.verify, "", sig, pub), true)
        eq("verify wrong message", try(e.verify, "x", sig, pub), false)

        local pair = try(e.generateKeyPair)
        check("generateKeyPair", type(pair) == "table" and #pair.privateKey == 32 and #pair.publicKey == 32)
        if type(pair) == "table" then
            local messages = { "one", "two", "three" }
            local sigs = try(e.signMany, messages, pair.privateKey)
            eq("signMany size", #sigs, 3)
            local results = try(e.verifyMany, messages, sigs, pair.publicKey)
            eq("verifyMany all true", (results[1] and results[2] and results[3]) and true or false, true)
            local forged = { sigs[1], sigs[3], sigs[2] }
            local mixed = try(e.verifyMany, messages, forged, pair.publicKey)
            eq("verifyMany detects swapped signatures", (mixed[1] and not mixed[2] and not mixed[3]) and true or false, true)
            raises("verifyMany size mismatch", "same size", e.verifyMany, { "a" }, { "a", "b" }, pair.publicKey)
        end
        raises("bad private key", "Invalid key length", e.sign, "x", "short")
    end
end

do
    local e = section("X25519 key exchange (RFC 7748)", "key_exchange")
    if e then
        local alicePriv = unhex("77076d0a7318a57d3c16c17251b26645df4c2f87ebc0992ab177fba51db92c2a")
        local alicePub = unhex("8520f0098930a754748b7ddcb43ef75a0dbf3a0d26381af4eba4a98eaa9b4e6a")
        local bobPriv = unhex("5dab087e624a8a4b79e17f8b83800ee66f3bb1292618b6fd1c2f8b27ff88e0eb")
        local bobPub = unhex("de9edb7d7b7dc1b4d35b61c2ece435373f8343c85b78674dadfc7e146f882b4f")
        local shared = "4a5d9d5ba4ce2de1728e3bf480350f25e07e21c947d19e3376f09b3c1e161742"
        eq("alice public key", try(e.publicKey, alicePriv), alicePub)
        eq("shared secret (alice)", hex(tostring(try(e.sharedSecret, alicePriv, bobPub))), shared)
        eq("shared secret (bob)", hex(tostring(try(e.sharedSecret, bobPriv, alicePub))), shared)
        local k1 = try(e.sessionKey, alicePriv, bobPub, "my protocol v1")
        eq("session key size", #tostring(k1), 32)
        eq("both sides derive the same session key", try(e.sessionKey, bobPriv, alicePub, "my protocol v1"), k1)
        check("info changes the key", k1 ~= try(e.sessionKey, alicePriv, bobPub, "my protocol v2"))
        eq("session key length", #tostring(try(e.sessionKey, alicePriv, bobPub, "x", 64)), 64)
        local pair = try(e.generateKeyPair)
        check("generateKeyPair", type(pair) == "table" and #pair.privateKey == 32 and #pair.publicKey == 32)
        raises("all-zero public key", "invalid public key", e.sharedSecret, alicePriv, ("\0"):rep(32))
    end
end

-- ---------------------------------------------------------------- keys and passwords

do
    local e = section("Key derivation engine", "kdf_engine")
    if e then
        eq("pbkdf2 RFC 6070 (1)", try(e.pbkdf2, "password", "salt", 1, 20, "sha1", "hex"), "0c60c80f961f0e71f3a9b524af6012062fe037a6")
        eq("pbkdf2 RFC 6070 (4096)", try(e.pbkdf2, "password", "salt", 4096, 20, "sha1", "hex"), "4b007901b765489abead49d926f721d065a429c1")
        eq("pbkdf2 default is raw sha256", #tostring(try(e.pbkdf2, "pw", "salt", 10, 32)), 32)
        raises("pbkdf2 too many iterations", "iterations", e.pbkdf2, "pw", "salt", 5000000, 32)
        eq("hkdf RFC 5869 (1)", try(e.hkdf, ("\11"):rep(22), bytes(13), bytes(10, 0xf0), 42, "sha256", "hex"),
            "3cb25f25faacd57a90434f64d0362f2a2d2d0a90cf1a5a4c5db02d56ecc4c5bf34007208d5b887185865")
        raises("hkdf length too large", "length", e.hkdf, "ikm", "", "", 255 * 32 + 1)
        eq("scrypt RFC 7914 (1)", try(e.scrypt, "", "", 16, 1, 1, 64, "hex"),
            "77d6576238657b203b19ca42c18a0497f16b4844e3074ae8dfdffa3fede21442fcd0069ded0948f8326a753a0fc81f17e8d3e0fb2e0d3628cf35e20c38d18906")
        raises("scrypt n not a power of two", "power of two", e.scrypt, "pw", "salt", 100, 1, 1, 32)
        raises("scrypt n too large", "n", e.scrypt, "pw", "salt", 1048576, 1, 1, 32)
        eq("expand length", #tostring(try(e.expand, "secret", "info", 100)), 100)
        check("expand depends on info", try(e.expand, "secret", "a", 32) ~= try(e.expand, "secret", "b", 32))
        eq("format base64", #tostring(try(e.expand, "secret", "info", 32, "base64")), 44)
    end
end

do
    local e = section("Password hasher", "password_hasher")
    if e then
        local stored = try(e.hash, "correct horse battery staple")
        check("format", tostring(stored):match("^%$pbkdf2%-sha256%$i=120000%$[%w_%-]+%$[%w_%-]+$") ~= nil, "got " .. short(stored))
        eq("verify right password", try(e.verify, "correct horse battery staple", stored), true)
        eq("verify wrong password", try(e.verify, "Correct horse battery staple", stored), false)
        eq("verify empty password", try(e.verify, "", stored), false)
        check("hashes are salted", stored ~= try(e.hash, "correct horse battery staple"))
        eq("malformed stored value", try(e.verify, "pw", "not a hash"), false)
        eq("empty stored value", try(e.verify, "pw", ""), false)
        eq("absurd iterations in a stored value", try(e.verify, "pw", "$pbkdf2-sha256$i=999999999$AAAA$AAAA"), false)
        eq("needsRehash on a fresh hash", try(e.needsRehash, stored), false)
        local weak = try(e.hash, "pw", 1000)
        eq("verify a weak hash", try(e.verify, "pw", weak), true)
        eq("needsRehash on a weak hash", try(e.needsRehash, weak), true)
        eq("needsRehash with a lower target", try(e.needsRehash, weak, 1000), false)
        eq("needsRehash on garbage", try(e.needsRehash, "garbage"), true)
        raises("too many iterations", "iterations", e.hash, "pw", 5000000)
        eq("unicode-free binary password", try(e.verify, "\0\1\2\255", try(e.hash, "\0\1\2\255", 100)), true)
    end
end

do
    local e = section("TOTP generator (RFC 6238)", "totp_generator")
    if e then
        local sha1 = b32("12345678901234567890")
        eq("T=59, 8 digits", try(e.totp, sha1, 8, 30, 59), "94287082")
        eq("T=1111111109", try(e.totp, sha1, 8, 30, 1111111109), "07081804")
        eq("T=1111111111", try(e.totp, sha1, 8, 30, 1111111111), "14050471")
        eq("T=1234567890", try(e.totp, sha1, 8, 30, 1234567890), "89005924")
        eq("T=2000000000", try(e.totp, sha1, 8, 30, 2000000000), "69279037")
        eq("SHA-256", try(e.totp, b32("12345678901234567890123456789012"), 8, 30, 59, "sha256"), "46119246")
        eq("SHA-512", try(e.totp, b32("1234567890123456789012345678901234567890123456789012345678901234"), 8, 30, 59, "sha512"), "90693936")
        eq("HOTP RFC 4226 counter 0", try(e.hotp, sha1, 0), "755224")
        eq("HOTP RFC 4226 counter 9", try(e.hotp, sha1, 9), "520489")
        eq("default is 6 digits", #tostring(try(e.totp, sha1)), 6)
        eq("lower case and spaces are accepted", try(e.totp, sha1:lower():sub(1, 8) .. " " .. sha1:lower():sub(9), 8, 30, 59), "94287082")
        eq("verify now", try(e.verify, sha1, try(e.totp, sha1, 6, 30, 1000000), 1, 6, 30, 1000000), true)
        eq("verify one period late", try(e.verify, sha1, try(e.totp, sha1, 6, 30, 1000000), 1, 6, 30, 1000030), true)
        eq("verify too late", try(e.verify, sha1, try(e.totp, sha1, 6, 30, 1000000), 1, 6, 30, 1000090), false)
        eq("verify without window", try(e.verify, sha1, try(e.totp, sha1, 6, 30, 1000000), 0, 6, 30, 1000030), false)
        eq("verify wrong code", try(e.verify, sha1, "000000", 0, 6, 30, 59), false)
        eq("remaining", try(e.remaining, 30, 59), 1)
        eq("remaining at the start of a period", try(e.remaining, 30, 60), 30)
        local secret = try(e.generateSecret)
        eq("generateSecret size", #tostring(secret), 32)
        check("generateSecret is usable", #tostring(try(e.totp, secret)) == 6)
        raises("invalid secret", "base32", e.totp, "1!")
        raises("empty secret", "empty", e.totp, "")
        raises("bad digits", "digits", e.totp, sha1, 4)
    end
end

do
    local e = section("Entropy source", "entropy_source")
    if e then
        eq("bytes size", #tostring(try(e.bytes, 20)), 20)
        eq("bytes hex", #tostring(try(e.bytes, 20, "hex")), 40)
        eq("bytes 0", try(e.bytes, 0), "")
        raises("bytes too large", "length", e.bytes, 100000)
        check("bytes are random", try(e.bytes, 16) ~= try(e.bytes, 16))

        local ok = true
        for _ = 1, 200 do
            local f = try(e.float)
            if type(f) ~= "number" or f < 0 or f >= 1 then ok = false end
        end
        check("float in [0, 1)", ok)

        local seen, inRange = {}, true
        for _ = 1, 600 do
            local r = try(e.int, 1, 6)
            if type(r) ~= "number" or r < 1 or r > 6 then inRange = false else seen[r] = true end
        end
        check("int(1, 6) in range", inRange)
        local distinct = 0
        for _ in pairs(seen) do distinct = distinct + 1 end
        eq("int(1, 6) reaches all values", distinct, 6)
        eq("int(4, 4)", try(e.int, 4, 4), 4)
        raises("int empty interval", "interval is empty", e.int, 5, 4)

        eq("chance(0)", try(e.chance, 0), false)
        eq("chance(1)", try(e.chance, 1), true)
        raises("chance(2)", "probability", e.chance, 2)
        local heads, tails = 0, 0
        for _ = 1, 300 do if try(e.coin) then heads = heads + 1 else tails = tails + 1 end end
        check("coin gives both sides", heads > 0 and tails > 0)

        local uuid = try(e.uuid)
        check("uuid format", tostring(uuid):match("^%x%x%x%x%x%x%x%x%-%x%x%x%x%-%x%x%x%x%-%x%x%x%x%-%x%x%x%x%x%x%x%x%x%x%x%x$") ~= nil)
        local token = try(e.token, 40)
        check("token default alphabet", #tostring(token) == 40 and tostring(token):match("^%w+$") ~= nil)
        eq("token custom alphabet", tostring(try(e.token, 50, "ab")):match("^[ab]+$") ~= nil, true)
        eq("token one-letter alphabet", try(e.token, 5, "z"), "zzzzz")
        raises("token empty alphabet", "alphabet", e.token, 5, "")
        raises("token too long", "length", e.token, 100000)

        local list = { "a", "b", "c", "d", "e", "f", "g", "h" }
        local shuffled = try(e.shuffle, list)
        local sorted = {}
        for _, v in ipairs(shuffled) do sorted[#sorted + 1] = v end
        table.sort(sorted)
        eq("shuffle keeps the items", table.concat(sorted), "abcdefgh")
        local moved = false
        for _ = 1, 20 do
            if table.concat(try(e.shuffle, list)) ~= "abcdefgh" then moved = true end
        end
        check("shuffle changes the order", moved)
        eq("shuffle empty list", #try(e.shuffle, {}), 0)
        local picked = try(e.pick, { "x", "y", "z" })
        check("pick returns an item", picked == "x" or picked == "y" or picked == "z")
        eq("pick from an empty list", try(e.pick, {}), nil)

        local roll = try(e.dice, "3d6+2")
        check("dice", type(roll) == "table" and #roll.rolls == 3 and roll.modifier == 2)
        if type(roll) == "table" then
            local sum = 2
            for _, r in ipairs(roll.rolls) do sum = sum + r; check("die in range", r >= 1 and r <= 6) end
            eq("dice total", roll.total, sum)
        end
        eq("dice 1d1-3", try(e.dice, "1d1-3").total, -2)
        raises("dice bad expression", "bad argument", e.dice, "3 dice")
        raises("dice too many", "dice", e.dice, "5000d6")
    end
end

-- ---------------------------------------------------------------- data tools

do
    local e = section("Encoder", "encoder")
    if e then
        eq("base64", try(e.base64, "Man"), "TWFu")
        eq("unbase64", try(e.unbase64, "TWFu"), "Man")
        eq("unbase64 invalid", try(e.unbase64, "!!!"), nil)
        eq("base64url", try(e.base64url, "\251\255"), "-_8")
        eq("unbase64url", try(e.unbase64url, "-_8"), "\251\255")
        eq("hex", try(e.hex, "abc"), "616263")
        eq("unhex", try(e.unhex, "616263"), "abc")
        eq("unhex invalid", try(e.unhex, "zz"), nil)
        eq("unhex odd length", try(e.unhex, "abc"), nil)
        eq("base32 (RFC 4648)", try(e.base32, "foobar"), "MZXW6YTBOI======")
        eq("base32 without padding", try(e.base32, "foobar", false), "MZXW6YTBOI")
        eq("base32 \"f\"", try(e.base32, "f"), "MY======")
        eq("unbase32", try(e.unbase32, "MZXW6YTBOI======"), "foobar")
        eq("unbase32 lower case, no padding", try(e.unbase32, "mzxw6ytboi"), "foobar")
        eq("unbase32 invalid", try(e.unbase32, "1!"), nil)
        eq("base58 \"Hello World!\"", try(e.base58, "Hello World!"), "2NEpo7TZRRrLZSi2U")
        eq("base58 \"abc\"", try(e.base58, "abc"), "ZiCa")
        eq("unbase58", try(e.unbase58, "2NEpo7TZRRrLZSi2U"), "Hello World!")
        eq("base58 keeps leading zero bytes", try(e.unbase58, try(e.base58, "\0\0abc")), "\0\0abc")
        eq("base58 leading zeros become 1", tostring(try(e.base58, "\0\0abc")):sub(1, 2), "11")
        eq("unbase58 invalid", try(e.unbase58, "0OIl"), nil)
        eq("base58 empty", try(e.base58, ""), "")
        local all = bytes(256)
        eq("base64 roundtrip", try(e.unbase64, try(e.base64, all)), all)
        eq("base32 roundtrip", try(e.unbase32, try(e.base32, all)), all)
        eq("hex roundtrip", try(e.unhex, try(e.hex, all)), all)
        eq("base58 roundtrip", try(e.unbase58, try(e.base58, bytes(100, 1))), bytes(100, 1))
        raises("base58 too large", "limited", e.base58, ("x"):rep(5000))
        eq("xor roundtrip", try(e.xor, try(e.xor, "secret", "key"), "key"), "secret")
        eq("xor value", try(e.xor, "\1\2\3", "\1"), "\0\3\2")
        raises("xor empty key", "key", e.xor, "x", "")
        eq("crc32", try(e.crc32, "123456789"), tonumber("cbf43926", 16))
    end
end

do
    local e = section("Checksum engine", "checksum_engine")
    if e then
        eq("crc32", try(e.crc32, "123456789"), tonumber("cbf43926", 16))
        eq("crc32c", try(e.crc32c, "123456789"), tonumber("e3069283", 16))
        eq("adler32", try(e.adler32, "123456789"), tonumber("091e01de", 16))
        eq("fnv1a32(\"a\")", try(e.fnv1a32, "a"), tonumber("e40c292c", 16))
        eq("fnv1a32 empty", try(e.fnv1a32, ""), tonumber("811c9dc5", 16))
        eq("fnv1a64(\"a\")", try(e.fnv1a64, "a"), "af63dc4c8601ec8c")
        eq("fnv1a64 empty", try(e.fnv1a64, ""), "cbf29ce484222325")
        local all = try(e.all, "123456789")
        check("all", type(all) == "table" and all.crc32 == tonumber("cbf43926", 16) and all.crc32c == tonumber("e3069283", 16)
            and all.adler32 == tonumber("091e01de", 16))
        eq("luhnDigit", try(e.luhnDigit, "7992739871"), 3)
        eq("luhnValid", try(e.luhnValid, "79927398713"), true)
        eq("luhnValid (wrong digit)", try(e.luhnValid, "79927398710"), false)
        eq("luhnValid (single digit)", try(e.luhnValid, "0"), false)
        raises("luhn with letters", "digits", e.luhnDigit, "12a")
    end
end

do
    local e = section("Compressor", "compressor")
    if e then
        local data = ("a"):rep(1000)
        local packed = try(e.deflate, data)
        check("deflate shrinks", #packed < 100)
        eq("inflate", try(e.inflate, packed), data)
        check("level 0 stores", #tostring(try(e.deflate, data, 0)) > 1000)
        check("level 9 beats level 0", #tostring(try(e.deflate, data, 9)) < #tostring(try(e.deflate, data, 0)))
        eq("inflate within maxSize", try(e.inflate, packed, 1000), data)
        raises("inflate over maxSize", "size limit", e.inflate, packed, 10)
        raises("inflate garbage", "invalid", e.inflate, "this is not deflate data")
        raises("bad level", "level", e.deflate, "x", 10)
        local gz = try(e.gzip, data)
        eq("gzip magic number", tostring(gz):sub(1, 2), "\31\139")
        eq("gunzip", try(e.gunzip, gz), data)
        eq("gunzip empty", try(e.gunzip, try(e.gzip, "")), "")
        raises("gunzip over maxSize", "size limit", e.gunzip, gz, 10)
        raises("gunzip garbage", "invalid", e.gunzip, "garbage")
        local binary = bytes(256):rep(4)
        eq("binary roundtrip (deflate)", try(e.inflate, try(e.deflate, binary)), binary)
        eq("binary roundtrip (gzip)", try(e.gunzip, try(e.gzip, binary)), binary)
        check("ratio of repetitive data", try(e.ratio, data) < 0.1)
        eq("ratio of empty data", try(e.ratio, ""), 1)
    end
end

do
    local e = section("Merkle tree", "merkle_tree")
    if e then
        local items = { "alice", "bob", "carol", "dave", "erin" }
        local root = try(e.root, items)
        eq("root size", #tostring(root), 64)
        eq("a single leaf is its own root", try(e.root, { "solo" }), try(e.leafHash, "solo"))
        check("changing an item changes the root", root ~= try(e.root, { "alice", "bob", "carol", "dave", "mallory" }))
        check("order matters", root ~= try(e.root, { "bob", "alice", "carol", "dave", "erin" }))
        eq("root is deterministic", try(e.root, items), root)
        for i, item in ipairs(items) do
            local proof = try(e.proof, items, i)
            eq("proof " .. i .. " verifies (" .. item .. ")", try(e.verify, item, proof, root), true)
        end
        local proof = try(e.proof, items, 2)
        eq("proof of another item fails", try(e.verify, "mallory", proof, root), false)
        eq("proof against another root fails", try(e.verify, "bob", proof, try(e.root, { "x", "y" })), false)
        eq("single leaf proof is empty", #try(e.proof, { "solo" }, 1), 0)
        eq("single leaf verifies", try(e.verify, "solo", {}, try(e.root, { "solo" })), true)
        raises("proof index out of range", "index", e.proof, items, 9)
        raises("empty list", "empty", e.root, {})
        raises("malformed proof", "bad argument", e.verify, "bob", { "oops" }, root)
        local big = {}
        for i = 1, 1000 do big[i] = "item " .. i end
        local bigRoot = try(e.root, big)
        eq("proof in a 1000 leaf tree", try(e.verify, big[777], try(e.proof, big, 777), bigRoot), true)
    end
end

do
    local e = section("Proof-of-work miner", "pow_miner")
    if e then
        local found = try(e.mine, "block-1|", 12)
        check("mine finds a nonce", type(found) == "table" and found.nonce ~= nil, "got " .. short(found))
        if type(found) == "table" then
            eq("hash matches", try(e.hash, "block-1|", found.nonce), found.hash)
            eq("verify", try(e.verify, "block-1|", found.nonce, 12), true)
            local bits = try(e.difficulty, found.hash)
            check("difficulty >= 12", bits >= 12)
            eq("verify at the exact difficulty", try(e.verify, "block-1|", found.nonce, bits), true)
            eq("verify one bit harder", try(e.verify, "block-1|", found.nonce, bits + 1), false)
            check("attempts counted", found.attempts == found.nonce + 1)
        end
        eq("nothing found within the budget", try(e.mine, "block-1|", 60, 1000), nil)
        local again = try(e.mine, "block-1|", 8, 100000, 5000)
        check("startNonce is honoured", type(again) == "table" and again.nonce >= 5000)
        eq("difficulty 0 matches the first nonce", try(e.mine, "x", 0).nonce, 0)
        eq("difficulty of 0x00ff", try(e.difficulty, "00ff"), 8)
        eq("difficulty of 0x0fff", try(e.difficulty, "0fff"), 4)
        eq("difficulty of 0xffff", try(e.difficulty, "ffff"), 0)
        eq("difficulty of 0x00", try(e.difficulty, "0000"), 16)
        raises("difficulty invalid hex", "hexadecimal", e.difficulty, "xyz")
        raises("mine difficulty too high", "difficulty", e.mine, "x", 100)
        raises("mine too many attempts", "maxAttempts", e.mine, "x", 8, 100000000)
    end
end

do
    local e = section("Classic cipher table", "classic_cipher")
    if e then
        eq("caesar 3", try(e.caesar, "Hello, World!", 3), "Khoor, Zruog!")
        eq("caesar -3", try(e.caesar, "Khoor, Zruog!", -3), "Hello, World!")
        eq("caesar 26", try(e.caesar, "Hello", 26), "Hello")
        eq("caesar 29 == 3", try(e.caesar, "Hello", 29), "Khoor")
        eq("rot13", try(e.rot13, "Hello"), "Uryyb")
        eq("rot13 twice", try(e.rot13, try(e.rot13, "Hello, World!")), "Hello, World!")
        eq("rot47 twice", try(e.rot47, try(e.rot47, "Hello, World! 123")), "Hello, World! 123")
        eq("rot47", try(e.rot47, "Hello"), "w6==@")
        eq("atbash", try(e.atbash, "abc XYZ"), "zyx CBA")
        eq("atbash twice", try(e.atbash, try(e.atbash, "Hello")), "Hello")
        eq("vigenere", try(e.vigenere, "ATTACKATDAWN", "LEMON"), "LXFOPVEFRNHR")
        eq("vigenere decrypt", try(e.vigenere, "LXFOPVEFRNHR", "LEMON", true), "ATTACKATDAWN")
        eq("vigenere keeps case and punctuation", try(e.vigenere, "Attack, at dawn!", "lemon", true),
            try(e.vigenere, "Attack, at dawn!", "LEMON", true))
        raises("vigenere empty key", "key", e.vigenere, "x", "")
        raises("vigenere key with digits", "letters", e.vigenere, "x", "k3y")
        local brute = try(e.bruteCaesar, "Khoor")
        eq("bruteCaesar returns 25 candidates", #brute, 25)
        local found = false
        for _, candidate in ipairs(brute) do if candidate == "Hello" then found = true end end
        check("bruteCaesar contains the plaintext", found)
        local freq = try(e.frequency, "Hello World")
        check("frequency", type(freq) == "table" and freq.l == 3 and freq.o == 2 and freq.h == 1 and freq.z == nil)
    end
end

-- ---------------------------------------------------------------- summary

print()
if skipped > 0 then
    put(colors.orange, ("%d section(s) skipped (block not attached).\n"):format(skipped))
end
if failed == 0 then
    put(colors.lime, ("All %d checks passed.\n"):format(passed))
else
    put(colors.red, ("%d of %d checks failed:\n"):format(failed, passed + failed))
    for _, f in ipairs(failures) do put(colors.gray, "  - " .. f .. "\n") end
end
