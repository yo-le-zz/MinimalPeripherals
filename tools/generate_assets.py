#!/usr/bin/env python3
"""
Generates every asset and data file of the mod from the table below:
textures (16x16, drawn with Pillow), models, blockstates, item definitions, lang files,
loot tables, recipes and the pickaxe tag.

    pip install pillow
    python3 tools/generate_assets.py

The ids must match the engine(...) calls of ModRegistry.java (the script checks it).
"""
import json
import os
import random
import re
import shutil
import sys

from PIL import Image

NS = "cryptographyperipherals"
ROOT = os.path.join(os.path.dirname(os.path.abspath(__file__)), "..")
RES = os.path.join(ROOT, "common", "src", "main", "resources")
ASSETS = os.path.join(RES, "assets", NS)
DATA = os.path.join(RES, "data", NS)
REGISTRY = os.path.join(ROOT, "common", "src", "main", "java", "cryptographyperipherals", "ModRegistry.java")
ORIGINAL_TEXTURES = os.path.join(ROOT, "tools", "original_textures")

# colour palettes: (casing, accent)
HASH = ((70, 110, 150), (180, 230, 255))
LEGACY = ((120, 110, 90), (255, 200, 120))
BLAKE = ((70, 120, 100), (170, 255, 200))
CIPHER = ((150, 100, 50), (255, 200, 100))
ASYM = ((110, 80, 150), (220, 180, 255))
SECRET = ((150, 70, 70), (255, 170, 170))
ENTROPY = ((60, 120, 60), (170, 255, 170))
DATA_TOOL = ((90, 100, 110), (190, 220, 255))
FUN = ((140, 120, 50), (255, 235, 150))

# id: (english name, french name, label lines, palette, recipe ingredient)
ENGINES = {
    "cryptographic_accelerator": ("Cryptographic Accelerator", "Accélérateur cryptographique", None, None, None),
    "md5_engine": ("MD5 Engine", "Moteur MD5", ("MD5",), LEGACY, "stick"),
    "sha1_engine": ("SHA-1 Engine", "Moteur SHA-1", ("SHA", "1"), LEGACY, "flint"),
    "sha256_engine": ("SHA-256 Engine", "Moteur SHA-256", ("SHA", "256"), HASH, "iron_ingot"),
    "sha384_engine": ("SHA-384 Engine", "Moteur SHA-384", ("SHA", "384"), HASH, "gold_ingot"),
    "sha512_engine": ("SHA-512 Engine", "Moteur SHA-512", ("SHA", "512"), HASH, "emerald"),
    "sha3_256_engine": ("SHA3-256 Engine", "Moteur SHA3-256", ("SH3", "256"), HASH, "lapis_lazuli"),
    "sha3_512_engine": ("SHA3-512 Engine", "Moteur SHA3-512", ("SH3", "512"), HASH, "amethyst_shard"),
    "ripemd160_engine": ("RIPEMD-160 Engine", "Moteur RIPEMD-160", ("RIP", "160"), BLAKE, "quartz"),
    "blake2b_engine": ("BLAKE2b Engine", "Moteur BLAKE2b", ("BL2", "B"), BLAKE, "copper_ingot"),
    "blake2s_engine": ("BLAKE2s Engine", "Moteur BLAKE2s", ("BL2", "S"), BLAKE, "iron_nugget"),
    "blake3_engine": ("BLAKE3 Engine", "Moteur BLAKE3", ("BL3",), BLAKE, "gold_nugget"),
    "aes_engine": ("AES Engine", "Moteur AES", ("AES",), CIPHER, "ender_pearl"),
    "chacha_engine": ("ChaCha20 Engine", "Moteur ChaCha20", ("CHA", "20"), CIPHER, "feather"),
    "signature_engine": ("Ed25519 Signature Engine", "Moteur de signature Ed25519", ("SIG",), ASYM, "ink_sac"),
    "key_exchange": ("X25519 Key Exchange", "Échange de clés X25519", ("KEX",), ASYM, "compass"),
    "kdf_engine": ("Key Derivation Engine", "Moteur de dérivation de clés", ("KDF",), SECRET, "repeater"),
    "password_hasher": ("Password Hasher", "Hacheur de mots de passe", ("PWD",), SECRET, "tripwire_hook"),
    "totp_generator": ("TOTP Generator", "Générateur TOTP", ("OTP",), SECRET, "clock"),
    "entropy_source": ("Entropy Source", "Source d'entropie", ("RNG",), ENTROPY, "glowstone_dust"),
    "encoder": ("Encoder", "Encodeur", ("ENC",), DATA_TOOL, "paper"),
    "checksum_engine": ("Checksum Engine", "Moteur de checksum", ("CRC",), DATA_TOOL, "string"),
    "compressor": ("Compressor", "Compresseur", ("ZIP",), DATA_TOOL, "piston"),
    "merkle_tree": ("Merkle Tree Engine", "Moteur d'arbre de Merkle", ("MRK",), DATA_TOOL, "oak_sapling"),
    "pow_miner": ("Proof-of-Work Miner", "Mineur de preuve de travail", ("POW",), FUN, "iron_pickaxe"),
    "classic_cipher": ("Classic Cipher Table", "Table de chiffres classiques", ("ABC",), FUN, "book"),
}

FONT = {
    "A": [".#.", "#.#", "###", "#.#", "#.#"], "B": ["##.", "#.#", "##.", "#.#", "##."],
    "C": [".##", "#..", "#..", "#..", ".##"], "D": ["##.", "#.#", "#.#", "#.#", "##."],
    "E": ["###", "#..", "##.", "#..", "###"], "F": ["###", "#..", "##.", "#..", "#.."],
    "G": [".##", "#..", "#.#", "#.#", ".##"], "H": ["#.#", "#.#", "###", "#.#", "#.#"],
    "I": ["###", ".#.", ".#.", ".#.", "###"], "J": ["..#", "..#", "..#", "#.#", ".#."],
    "K": ["#.#", "#.#", "##.", "#.#", "#.#"], "L": ["#..", "#..", "#..", "#..", "###"],
    "M": ["#.#", "###", "###", "#.#", "#.#"], "N": ["##.", "#.#", "#.#", "#.#", "#.#"],
    "O": [".#.", "#.#", "#.#", "#.#", ".#."], "P": ["##.", "#.#", "##.", "#..", "#.."],
    "Q": [".#.", "#.#", "#.#", "##.", ".##"], "R": ["##.", "#.#", "##.", "#.#", "#.#"],
    "S": [".##", "#..", ".#.", "..#", "##."], "T": ["###", ".#.", ".#.", ".#.", ".#."],
    "U": ["#.#", "#.#", "#.#", "#.#", "###"], "V": ["#.#", "#.#", "#.#", "#.#", ".#."],
    "W": ["#.#", "#.#", "###", "###", "#.#"], "X": ["#.#", "#.#", ".#.", "#.#", "#.#"],
    "Y": ["#.#", "#.#", ".#.", ".#.", ".#."], "Z": ["###", "..#", ".#.", "#..", "###"],
    "0": ["###", "#.#", "#.#", "#.#", "###"], "1": [".#.", "##.", ".#.", ".#.", "###"],
    "2": ["##.", "..#", ".#.", "#..", "###"], "3": ["##.", "..#", ".#.", "..#", "##."],
    "4": ["#.#", "#.#", "###", "..#", "..#"], "5": ["###", "#..", "##.", "..#", "##."],
    "6": [".##", "#..", "###", "#.#", "###"], "7": ["###", "..#", ".#.", ".#.", ".#."],
    "8": ["###", "#.#", "###", "#.#", "###"], "9": ["###", "#.#", "###", "..#", "##."],
}


def shade(color, amount):
    return tuple(max(0, min(255, c + amount)) for c in color)


def draw_text(img, lines, color):
    count = len(lines)
    total_height = count * 5 + (count - 1) * 2
    y = (16 - total_height) // 2
    for line in lines:
        width = len(line) * 4 - 1
        x = (16 - width) // 2
        for ch in line:
            for row, pattern in enumerate(FONT[ch]):
                for col, bit in enumerate(pattern):
                    if bit == "#":
                        img.putpixel((x + col, y + row), color + (255,))
            x += 4
        y += 7


def make_textures(name, palette, lines):
    base, accent = palette
    rng = random.Random(name)

    def noisy(amount=7):
        img = Image.new("RGBA", (16, 16))
        for x in range(16):
            for y in range(16):
                img.putpixel((x, y), shade(base, rng.randint(-amount, amount)) + (255,))
        return img

    side = noisy()
    for i in range(16):
        for (x, y) in ((i, 0), (i, 15), (0, i), (15, i)):
            side.putpixel((x, y), shade(base, -35) + (255,))
    for y in (4, 11):  # two vent slits
        for x in range(3, 13):
            side.putpixel((x, y), shade(base, -25) + (255,))

    top = noisy(4)
    for i in range(1, 15):
        for (x, y) in ((i, 1), (i, 14), (1, i), (14, i)):
            top.putpixel((x, y), shade(base, -30) + (255,))
    for (x, y) in ((2, 2), (13, 2), (2, 13), (13, 13)):
        top.putpixel((x, y), accent + (255,))

    front = Image.new("RGBA", (16, 16), (35, 35, 35, 255))
    for i in range(16):
        for (x, y) in ((i, 0), (i, 15), (0, i), (15, i)):
            front.putpixel((x, y), shade(base, -10) + (255,))
    for i in range(1, 15):
        for (x, y) in ((i, 1), (i, 14), (1, i), (14, i)):
            front.putpixel((x, y), (60, 60, 60, 255))
    draw_text(front, lines, accent)
    front.putpixel((13, 3), accent + (255,))  # status LED
    return side, top, front


def write_json(path, obj):
    os.makedirs(os.path.dirname(path), exist_ok=True)
    with open(path, "w", encoding="utf-8") as f:
        json.dump(obj, f, indent=2, ensure_ascii=False)
        f.write("\n")


def check_registry():
    source = open(REGISTRY, encoding="utf-8").read()
    registered = re.findall(r'engine\("([a-z0-9_]+)"', source)
    if registered != list(ENGINES):
        print("ModRegistry.java and tools/generate_assets.py disagree:")
        print("  only in Java:  ", [r for r in registered if r not in ENGINES])
        print("  only in script:", [e for e in ENGINES if e not in registered])
        sys.exit(1)


def main():
    check_registry()
    shutil.rmtree(os.path.join(RES, "assets"), ignore_errors=True)
    shutil.rmtree(os.path.join(RES, "data"), ignore_errors=True)
    tex_dir = os.path.join(ASSETS, "textures", "block")
    os.makedirs(tex_dir, exist_ok=True)

    rotations = {"north": 0, "east": 90, "south": 180, "west": 270}

    for name, (en, fr, lines, palette, ingredient) in ENGINES.items():
        # --- textures and model
        if lines is None:
            for part in ("front", "side", "top", "bottom"):
                shutil.copy(os.path.join(ORIGINAL_TEXTURES, f"cryptographic_accelerator_{part}.png"),
                            os.path.join(tex_dir, f"{name}_{part}.png"))
            model = {"parent": "minecraft:block/orientable_with_bottom", "textures": {
                part: f"{NS}:block/{name}_{part}" for part in ("top", "front", "side", "bottom")}}
        else:
            side, top, front = make_textures(name, palette, lines)
            side.save(os.path.join(tex_dir, f"{name}_side.png"))
            top.save(os.path.join(tex_dir, f"{name}_top.png"))
            front.save(os.path.join(tex_dir, f"{name}_front.png"))
            model = {"parent": "minecraft:block/orientable", "textures": {
                part: f"{NS}:block/{name}_{part}" for part in ("top", "front", "side")}}
        write_json(os.path.join(ASSETS, "models", "block", f"{name}.json"), model)

        # --- blockstate and item definition
        variants = {}
        for facing, y in rotations.items():
            variant = {"model": f"{NS}:block/{name}"}
            if y:
                variant["y"] = y
            variants[f"facing={facing}"] = variant
        write_json(os.path.join(ASSETS, "blockstates", f"{name}.json"), {"variants": variants})
        write_json(os.path.join(ASSETS, "items", f"{name}.json"),
                   {"model": {"type": "minecraft:model", "model": f"{NS}:block/{name}"}})

        # --- loot table
        write_json(os.path.join(DATA, "loot_table", "blocks", f"{name}.json"), {
            "type": "minecraft:block",
            "pools": [{
                "rolls": 1.0, "bonus_rolls": 0.0,
                "conditions": [{"condition": "minecraft:survives_explosion"}],
                "entries": [{"type": "minecraft:item", "name": f"{NS}:{name}"}]}],
            "random_sequence": f"{NS}:blocks/{name}"})

        # --- recipe
        if name == "cryptographic_accelerator":
            write_json(os.path.join(DATA, "recipe", f"{name}.json"), {
                "type": "minecraft:crafting_shaped", "category": "redstone",
                "pattern": ["ici", "cdc", "ici"],
                "key": {"d": "minecraft:diamond", "i": "minecraft:iron_ingot", "c": "minecraft:comparator"},
                "result": {"id": f"{NS}:{name}", "count": 1}})
        else:
            write_json(os.path.join(DATA, "recipe", f"{name}.json"), {
                "type": "minecraft:crafting_shapeless", "category": "redstone",
                "ingredients": [f"{NS}:cryptographic_accelerator", "minecraft:redstone", f"minecraft:{ingredient}"],
                "result": {"id": f"{NS}:{name}", "count": 1}})

    write_json(os.path.join(RES, "data", "minecraft", "tags", "block", "mineable", "pickaxe.json"),
               {"values": [f"{NS}:{name}" for name in ENGINES]})

    for code, index in (("en_us", 0), ("fr_fr", 1)):
        lang = {"itemGroup." + NS: "Cryptography Peripherals"}
        for name, entry in ENGINES.items():
            lang[f"block.{NS}.{name}"] = entry[index]
        write_json(os.path.join(ASSETS, "lang", f"{code}.json"), lang)

    print(f"{len(ENGINES)} blocks generated")


if __name__ == "__main__":
    main()
