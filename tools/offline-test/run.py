#!/usr/bin/env python3
"""
Runs the Lua test programs of examples/ against the real Java peripherals, without Minecraft.

A Python bridge (JPype + Lupa) exposes every @LuaFunction of the engines to a Lua interpreter, with the same
conversions as CC: Tweaked (strings are bytes, tables are maps, nil is null). Use run.sh to call it.

    pip install jpype1 lupa
"""
import sys, jpype, lupa
from jpype import JClass, JObject, JArray

BUILD_DIR, BC_JAR, TEST_FILE = sys.argv[1], sys.argv[2], sys.argv[3]
jpype.startJVM(classpath=[BUILD_DIR, BC_JAR])
rt = lupa.LuaRuntime(encoding="latin-1", unpack_returned_tuples=True)

Args = JClass("Args")
LuaFunction = JClass("dan200.computercraft.api.lua.LuaFunction")
LuaException = JClass("dan200.computercraft.api.lua.LuaException")
Owner = JClass("cryptographyperipherals.block.CryptoBlockEntity")
HashMap = JClass("java.util.HashMap")
Double = JClass("java.lang.Double")
Integer = JClass("java.lang.Integer")
Long = JClass("java.lang.Long")
Boolean = JClass("java.lang.Boolean")
Optional = JClass("java.util.Optional")
Number = JClass("java.lang.Number")
BcDigest = {}
P = "cryptographyperipherals.peripheral."

def digest_supplier(cls, *args):
    Supplier = JClass("java.util.function.Supplier")
    D = JClass(cls)
    @jpype.JImplements(Supplier)
    class S:
        @jpype.JOverride
        def get(self):
            return D(*args)
    return S()

def make(name):
    o = Owner()
    jdk = lambda t, a, m: JClass(P + "JdkHashEngine")(o, t, a, m)
    table = {
        "cryptographic_accelerator": lambda: JClass(P + "CryptographicAcceleratorPeripheral")(o),
        "md5_engine": lambda: jdk("md5_engine", "MD5", "HmacMD5"),
        "sha1_engine": lambda: jdk("sha1_engine", "SHA-1", "HmacSHA1"),
        "sha256_engine": lambda: jdk("sha256_engine", "SHA-256", "HmacSHA256"),
        "sha384_engine": lambda: jdk("sha384_engine", "SHA-384", "HmacSHA384"),
        "sha512_engine": lambda: jdk("sha512_engine", "SHA-512", "HmacSHA512"),
        "sha3_256_engine": lambda: jdk("sha3_256_engine", "SHA3-256", "HmacSHA3-256"),
        "sha3_512_engine": lambda: jdk("sha3_512_engine", "SHA3-512", "HmacSHA3-512"),
        "ripemd160_engine": lambda: JClass(P + "BcHashEngine")(o, "ripemd160_engine", digest_supplier("org.bouncycastle.crypto.digests.RIPEMD160Digest")),
        "blake2b_engine": lambda: JClass(P + "BcHashEngine")(o, "blake2b_engine", digest_supplier("org.bouncycastle.crypto.digests.Blake2bDigest", 512)),
        "blake2s_engine": lambda: JClass(P + "BcHashEngine")(o, "blake2s_engine", digest_supplier("org.bouncycastle.crypto.digests.Blake2sDigest", 256)),
        "blake3_engine": lambda: JClass(P + "Blake3Engine")(o),
        "aes_engine": lambda: JClass(P + "AesEngine")(o),
        "chacha_engine": lambda: JClass(P + "ChaChaEngine")(o),
        "signature_engine": lambda: JClass(P + "SignatureEngine")(o),
        "key_exchange": lambda: JClass(P + "KeyExchangeEngine")(o),
        "kdf_engine": lambda: JClass(P + "KdfEngine")(o),
        "password_hasher": lambda: JClass(P + "PasswordEngine")(o),
        "totp_generator": lambda: JClass(P + "TotpEngine")(o),
        "entropy_source": lambda: JClass(P + "EntropyEngine")(o),
        "encoder": lambda: JClass(P + "EncoderEngine")(o),
        "checksum_engine": lambda: JClass(P + "ChecksumEngine")(o),
        "compressor": lambda: JClass(P + "CompressorEngine")(o),
        "merkle_tree": lambda: JClass(P + "MerkleEngine")(o),
        "pow_miner": lambda: JClass(P + "PowEngine")(o),
        "classic_cipher": lambda: JClass(P + "ClassicCipherEngine")(o),
    }
    return table[name]()

def is_table(v):
    return lupa.lua_type(v) == "table"

def to_java_value(v):
    if v is None:
        return None
    if isinstance(v, bool):
        return Boolean(v)
    if isinstance(v, (int, float)):
        return Double(float(v))
    if isinstance(v, str):
        return str(v)
    if is_table(v):
        m = HashMap()
        for k, x in v.items():
            m.put(to_java_value(k), to_java_value(x))
        return m
    raise TypeError(repr(v))

def from_java(o):
    if o is None:
        return None
    if not hasattr(o, "getClass"):
        return o
    cls = str(o.getClass().getName())
    if cls == "java.lang.String":
        return str(o)
    if cls == "java.lang.Boolean":
        return bool(o.booleanValue())
    if isinstance(o, JObject) and Number.class_.isInstance(o):
        d = float(o.doubleValue())
        return int(d) if d.is_integer() and abs(d) < 2**53 else d
    if hasattr(o, "size") and hasattr(o, "get") and "List" in cls:
        return rt.table_from([from_java(o.get(i)) for i in range(int(o.size()))])
    if hasattr(o, "entrySet"):
        d = {}
        for e in o.entrySet():
            d[from_java(e.getKey())] = from_java(e.getValue())
        return rt.table_from(d)
    raise TypeError(cls)

def convert_param(v, ptype):
    name = str(ptype.getName())
    if name == "java.lang.String":
        if isinstance(v, (int, float)) and not isinstance(v, bool):
            return str(v)
        if not isinstance(v, str):
            raise lupa.LuaError("bad argument (string expected)")
        return v
    if name in ("int", "long"):
        if not isinstance(v, (int, float)) or isinstance(v, bool):
            raise lupa.LuaError("bad argument (number expected)")
        if float(v) != int(v):
            raise lupa.LuaError("bad argument (number has no integer representation)")
        return Integer(int(v)) if name == "int" else Long(int(v))
    if name == "double":
        return Double(float(v))
    if name == "boolean":
        return Boolean(bool(v))
    if name == "java.util.Map":
        if not is_table(v):
            raise lupa.LuaError("bad argument (table expected)")
        return to_java_value(v)
    if name == "java.util.Optional":
        return Optional.ofNullable(to_java_value(v))
    raise TypeError(name)

def bind(peripheral, method):
    ptypes = list(method.getParameterTypes())
    def call(*args):
        try:
            if len(ptypes) == 1 and str(ptypes[0].getName()) == "dan200.computercraft.api.lua.IArguments":
                jargs = Args(JArray(JObject)([to_java_value(a) for a in args]))
                inv = [jargs]
            else:
                inv = []
                for i, pt in enumerate(ptypes):
                    a = args[i] if i < len(args) else None
                    if a is None and str(pt.getName()) == "java.util.Optional":
                        inv.append(Optional.empty())
                    elif a is None:
                        raise lupa.LuaError("bad argument #%d (value expected)" % (i + 1))
                    else:
                        inv.append(convert_param(a, pt))
            result = method.invoke(peripheral, JArray(JObject)(inv))
        except jpype.JException as e:
            cause = e.getCause() if hasattr(e, "getCause") else None
            if cause is not None and LuaException.class_.isInstance(cause):
                raise lupa.LuaError(str(cause.getMessage()))
            raise lupa.LuaError("JAVA BUG: " + str(cause if cause is not None else e))
        return from_java(result)
    return call

def wrap(name):
    p = make(name)
    funcs = {}
    for m in p.getClass().getMethods():
        ann = m.getAnnotation(LuaFunction)
        if ann is None:
            continue
        names = [str(n) for n in ann.value()] or [str(m.getName())]
        for n in names:
            funcs[n] = bind(p, m)
    return rt.table_from(funcs)

ENV = """
colors = { lime = 1, red = 2, gray = 3, orange = 4, yellow = 5 }
term = { isColor = function() return false end }
write = io.write
peripheral = { find = find, wrap = function(n) return find(n) end }
"""
lua = rt.execute("return function(find, wrapFn) " + ENV + " end")
wrapped = {}
def find(name):
    try:
        if name not in wrapped:
            wrapped[name] = wrap(name)
        return wrapped[name]
    except KeyError:
        return None

env_setup = lua(find, None)
code = open(TEST_FILE).read()
chunk = rt.eval("function(src, name) return load(src, name) end")(code, "@" + TEST_FILE)
if isinstance(chunk, tuple):
    print("LOAD ERROR", chunk); sys.exit(2)
chunk(*sys.argv[4:])
