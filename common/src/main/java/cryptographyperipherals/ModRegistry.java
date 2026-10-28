package cryptographyperipherals;

import cryptographyperipherals.block.CryptoBlock;
import cryptographyperipherals.block.CryptoBlockEntity;
import cryptographyperipherals.peripheral.AesEngine;
import cryptographyperipherals.peripheral.BcHashEngine;
import cryptographyperipherals.peripheral.Blake3Engine;
import cryptographyperipherals.peripheral.ChaChaEngine;
import cryptographyperipherals.peripheral.ChecksumEngine;
import cryptographyperipherals.peripheral.ClassicCipherEngine;
import cryptographyperipherals.peripheral.CompressorEngine;
import cryptographyperipherals.peripheral.CryptographicAcceleratorPeripheral;
import cryptographyperipherals.peripheral.EncoderEngine;
import cryptographyperipherals.peripheral.EnginePeripheral;
import cryptographyperipherals.peripheral.EntropyEngine;
import cryptographyperipherals.peripheral.JdkHashEngine;
import cryptographyperipherals.peripheral.KdfEngine;
import cryptographyperipherals.peripheral.KeyExchangeEngine;
import cryptographyperipherals.peripheral.MerkleEngine;
import cryptographyperipherals.peripheral.PasswordEngine;
import cryptographyperipherals.peripheral.PowEngine;
import cryptographyperipherals.peripheral.SignatureEngine;
import cryptographyperipherals.peripheral.TotpEngine;
import cryptographyperipherals.platform.PeripheralRegistrar;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import org.bouncycastle.crypto.digests.Blake2bDigest;
import org.bouncycastle.crypto.digests.Blake2sDigest;
import org.bouncycastle.crypto.digests.RIPEMD160Digest;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.BiConsumer;
import java.util.function.Function;

/**
 * Every object of the mod is created here, once, without touching any registry.
 * The loader modules then register them at the right time with the registerXxx methods.
 * {@link CryptographyPeripherals#PLATFORM} must be set before this class is loaded.
 * <p>
 * Every block is a {@link CryptoBlock} that exposes one peripheral. All of them share one block entity type.
 */
public final class ModRegistry {
    private ModRegistry() {
    }

    private static final Map<Identifier, Block> BLOCKS = new LinkedHashMap<>();
    private static final Map<Identifier, Item> ITEMS = new LinkedHashMap<>();
    private static final Map<Identifier, BlockEntityType<?>> BLOCK_ENTITIES = new LinkedHashMap<>();
    private static final Map<Identifier, CreativeModeTab> TABS = new LinkedHashMap<>();
    private static final Map<String, CryptoBlock> ENGINES = new LinkedHashMap<>();

    static {
        // The general purpose block: every algorithm, one block.
        engine("cryptographic_accelerator", CryptographicAcceleratorPeripheral::new);

        // Dedicated hash engines: one algorithm each, shorter call path, batch functions.
        engine("md5_engine", o -> new JdkHashEngine(o, "md5_engine", "MD5", "HmacMD5"));
        engine("sha1_engine", o -> new JdkHashEngine(o, "sha1_engine", "SHA-1", "HmacSHA1"));
        engine("sha256_engine", o -> new JdkHashEngine(o, "sha256_engine", "SHA-256", "HmacSHA256"));
        engine("sha384_engine", o -> new JdkHashEngine(o, "sha384_engine", "SHA-384", "HmacSHA384"));
        engine("sha512_engine", o -> new JdkHashEngine(o, "sha512_engine", "SHA-512", "HmacSHA512"));
        engine("sha3_256_engine", o -> new JdkHashEngine(o, "sha3_256_engine", "SHA3-256", "HmacSHA3-256"));
        engine("sha3_512_engine", o -> new JdkHashEngine(o, "sha3_512_engine", "SHA3-512", "HmacSHA3-512"));
        engine("ripemd160_engine", o -> new BcHashEngine(o, "ripemd160_engine", RIPEMD160Digest::new));
        engine("blake2b_engine", o -> new BcHashEngine(o, "blake2b_engine", () -> new Blake2bDigest(512)));
        engine("blake2s_engine", o -> new BcHashEngine(o, "blake2s_engine", () -> new Blake2sDigest(256)));
        engine("blake3_engine", Blake3Engine::new);

        // Ciphers, signatures and key agreement.
        engine("aes_engine", AesEngine::new);
        engine("chacha_engine", ChaChaEngine::new);
        engine("signature_engine", SignatureEngine::new);
        engine("key_exchange", KeyExchangeEngine::new);

        // Passwords and keys.
        engine("kdf_engine", KdfEngine::new);
        engine("password_hasher", PasswordEngine::new);
        engine("totp_generator", TotpEngine::new);
        engine("entropy_source", EntropyEngine::new);

        // Data tools.
        engine("encoder", EncoderEngine::new);
        engine("checksum_engine", ChecksumEngine::new);
        engine("compressor", CompressorEngine::new);
        engine("merkle_tree", MerkleEngine::new);
        engine("pow_miner", PowEngine::new);
        engine("classic_cipher", ClassicCipherEngine::new);
    }

    /** The block entity type shared by every crypto block. */
    public static final BlockEntityType<CryptoBlockEntity> CRYPTO_BE = blockEntity("crypto", CryptoBlockEntity::new);

    public static final CreativeModeTab TAB = tab("cryptographyperipherals", CryptographyPeripherals.PLATFORM.createTab(
            Component.translatable("itemGroup.cryptographyperipherals"),
            () -> new ItemStack(ENGINES.get("cryptographic_accelerator")),
            (parameters, output) -> ENGINES.values().forEach(block -> output.accept(block))));

    // ---- Registration hooks, called by the loaders ----
    public static void registerBlocks(BiConsumer<Identifier, Block> register) {
        BLOCKS.forEach(register);
    }

    public static void registerItems(BiConsumer<Identifier, Item> register) {
        ITEMS.forEach(register);
    }

    public static void registerBlockEntityTypes(BiConsumer<Identifier, BlockEntityType<?>> register) {
        BLOCK_ENTITIES.forEach(register);
    }

    public static void registerCreativeTabs(BiConsumer<Identifier, CreativeModeTab> register) {
        TABS.forEach(register);
    }

    public static void registerPeripherals(PeripheralRegistrar registrar) {
        registrar.register(CRYPTO_BE, (be, side) -> be.peripheral());
    }

    // ---- helpers ----
    private static void engine(String name, Function<CryptoBlockEntity, EnginePeripheral> peripheral) {
        var id = CryptographyPeripherals.id(name);
        var blockKey = ResourceKey.create(Registries.BLOCK, id);
        var itemKey = ResourceKey.create(Registries.ITEM, id);

        var properties = BlockBehaviour.Properties.of()
                .strength(2.0F)
                .mapColor(MapColor.STONE)
                .setId(blockKey);
        var block = new CryptoBlock(properties, peripheral);
        BLOCKS.put(id, block);
        ENGINES.put(name, block);
        ITEMS.put(id, new BlockItem(block, new Item.Properties().setId(itemKey).useBlockDescriptionPrefix()));
    }

    private static BlockEntityType<CryptoBlockEntity> blockEntity(String name, cryptographyperipherals.platform.Platform.BlockEntityFactory<CryptoBlockEntity> factory) {
        var type = CryptographyPeripherals.PLATFORM.createBlockEntityType(factory, ENGINES.values().toArray(new Block[0]));
        BLOCK_ENTITIES.put(CryptographyPeripherals.id(name), type);
        return type;
    }

    private static CreativeModeTab tab(String name, CreativeModeTab tab) {
        TABS.put(CryptographyPeripherals.id(name), tab);
        return tab;
    }
}
