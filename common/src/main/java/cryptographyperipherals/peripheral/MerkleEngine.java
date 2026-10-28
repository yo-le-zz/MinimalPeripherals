package cryptographyperipherals.peripheral;

import cryptographyperipherals.block.CryptoBlockEntity;
import dan200.computercraft.api.lua.IArguments;
import dan200.computercraft.api.lua.LuaException;
import dan200.computercraft.api.lua.LuaFunction;

import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static cryptographyperipherals.peripheral.LuaUtil.bytes;
import static cryptographyperipherals.peripheral.LuaUtil.hex;

/**
 * Merkle trees over SHA-256. Leaves are hashed as sha256(0x00 .. data) and inner nodes as
 * sha256(0x01 .. left .. right), so a leaf can never be mistaken for a node. A node without a sibling
 * is promoted to the next level unchanged.
 */
public class MerkleEngine extends EnginePeripheral {
    public MerkleEngine(CryptoBlockEntity owner) {
        super(owner, "merkle_tree");
    }

    private static MessageDigest sha256() {
        try {
            return MessageDigest.getInstance("SHA-256");
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException(e);
        }
    }

    private static byte[] leaf(MessageDigest md, byte[] data) {
        md.update((byte) 0);
        md.update(data);
        return md.digest();
    }

    private static byte[] node(MessageDigest md, byte[] left, byte[] right) {
        md.update((byte) 1);
        md.update(left);
        md.update(right);
        return md.digest();
    }

    private static List<byte[]> leaves(Object table) throws LuaException {
        if (!(table instanceof Map<?, ?> map)) {
            throw new LuaException("bad argument #1 (expected a list of strings)");
        }

        var items = LuaUtil.strings(map);
        if (items.isEmpty()) {
            throw new LuaException("bad argument #1 (the list is empty)");
        }

        var md = sha256();
        var result = new ArrayList<byte[]>(items.size());
        for (var item : items) {
            result.add(leaf(md, bytes(item)));
        }
        return result;
    }

    private static List<byte[]> nextLevel(MessageDigest md, List<byte[]> level) {
        var next = new ArrayList<byte[]>((level.size() + 1) / 2);
        for (var i = 0; i < level.size(); i += 2) {
            if (i + 1 < level.size()) {
                next.add(node(md, level.get(i), level.get(i + 1)));
            } else {
                next.add(level.get(i));
            }
        }
        return next;
    }

    /** leafHash(data): the hash of a single leaf, as hexadecimal. */
    @LuaFunction
    public final String leafHash(String data) {
        return hex(leaf(sha256(), bytes(data)));
    }

    /** root(list): the Merkle root of a list of strings, as hexadecimal. */
    @LuaFunction
    public final String root(IArguments args) throws LuaException {
        var md = sha256();
        var level = leaves(args.get(0));
        while (level.size() > 1) {
            level = nextLevel(md, level);
        }
        return hex(level.get(0));
    }

    /**
     * proof(list, index): the proof that the item at position index belongs to the tree: a list of
     * {hash = hexadecimal, left = boolean} where left tells if the sibling is on the left.
     */
    @LuaFunction
    public final List<Map<String, Object>> proof(IArguments args) throws LuaException {
        var md = sha256();
        var level = leaves(args.get(0));
        var position = args.getInt(1) - 1;
        if (position < 0 || position >= level.size()) {
            throw new LuaException("bad argument #2 (index out of range)");
        }

        var proof = new ArrayList<Map<String, Object>>();
        while (level.size() > 1) {
            var siblingIndex = position % 2 == 0 ? position + 1 : position - 1;
            if (siblingIndex < level.size()) {
                var entry = new HashMap<String, Object>();
                entry.put("hash", hex(level.get(siblingIndex)));
                entry.put("left", siblingIndex < position);
                proof.add(entry);
            }

            level = nextLevel(md, level);
            position /= 2;
        }
        return proof;
    }

    /** verify(item, proof, rootHex): true if the item belongs to the tree with this root. */
    @LuaFunction
    public final boolean verify(String item, Map<?, ?> proof, String rootHex) throws LuaException {
        var md = sha256();
        var current = leaf(md, bytes(item));
        for (var step : LuaUtil.values(proof, 64)) {
            if (!(step instanceof Map<?, ?> entry) || !(entry.get("hash") instanceof String hash) || !(entry.get("left") instanceof Boolean left)) {
                throw new LuaException("bad argument #2 (expected a list of {hash = string, left = boolean})");
            }

            byte[] sibling;
            try {
                sibling = java.util.HexFormat.of().parseHex(hash);
            } catch (IllegalArgumentException e) {
                return false;
            }

            current = left ? node(md, sibling, current) : node(md, current, sibling);
        }
        return LuaUtil.hexEquals(current, rootHex);
    }
}
