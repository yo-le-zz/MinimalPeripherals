package minimalperipherals;

import com.mojang.serialization.Codec;
import minimalperipherals.crypto.CryptographicAcceleratorBlock;
import minimalperipherals.crypto.CryptographicAcceleratorBlockEntity;
import minimalperipherals.nfc.NfcCardItem;
import minimalperipherals.nfc.NfcReaderBlock;
import minimalperipherals.nfc.NfcReaderBlockEntity;
import minimalperipherals.platform.PeripheralRegistrar;
import minimalperipherals.platform.Platform;
import minimalperipherals.rfid.RfidBadgeItem;
import minimalperipherals.rfid.RfidScannerBlock;
import minimalperipherals.rfid.RfidScannerBlockEntity;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.BiConsumer;
import java.util.function.Function;

/**
 * Every object of the mod is created here, once, without touching any registry.
 * The loader modules then register them at the right time with the registerXxx methods.
 * {@link MinimalPeripherals#PLATFORM} must be set before this class is loaded.
 */
public final class ModRegistry {
    private ModRegistry() {
    }

    private static final Map<Identifier, DataComponentType<?>> COMPONENTS = new LinkedHashMap<>();
    private static final Map<Identifier, Block> BLOCKS = new LinkedHashMap<>();
    private static final Map<Identifier, Item> ITEMS = new LinkedHashMap<>();
    private static final Map<Identifier, BlockEntityType<?>> BLOCK_ENTITIES = new LinkedHashMap<>();
    private static final Map<Identifier, CreativeModeTab> TABS = new LinkedHashMap<>();

    // ---- Data components (data written on cards and badges) ----
    public static final DataComponentType<String> NFC_DATA = component("nfc_data", Codec.STRING, ByteBufCodecs.STRING_UTF8);
    public static final DataComponentType<String> NFC_PRIVATE_KEY = component("nfc_private_key", Codec.STRING, ByteBufCodecs.STRING_UTF8);
    public static final DataComponentType<Boolean> NFC_READONLY = component("nfc_readonly", Codec.BOOL, ByteBufCodecs.BOOL);

    // ---- Blocks (each one also creates its BlockItem) ----
    public static final CryptographicAcceleratorBlock CRYPTOGRAPHIC_ACCELERATOR = block("cryptographic_accelerator", CryptographicAcceleratorBlock::new);
    public static final NfcReaderBlock NFC_READER = block("nfc_reader", NfcReaderBlock::new);
    public static final RfidScannerBlock RFID_SCANNER = block("rfid_scanner", RfidScannerBlock::new);

    // ---- Items ----
    public static final NfcCardItem NFC_CARD = item("nfc_card", NfcCardItem::new, new Item.Properties().stacksTo(1));
    public static final RfidBadgeItem RFID_BADGE = item("rfid_badge", RfidBadgeItem::new, new Item.Properties().stacksTo(1));

    // ---- Block entities ----
    public static final BlockEntityType<CryptographicAcceleratorBlockEntity> CRYPTOGRAPHIC_ACCELERATOR_BE =
            blockEntity("cryptographic_accelerator", CryptographicAcceleratorBlockEntity::new, CRYPTOGRAPHIC_ACCELERATOR);
    public static final BlockEntityType<NfcReaderBlockEntity> NFC_READER_BE =
            blockEntity("nfc_reader", NfcReaderBlockEntity::new, NFC_READER);
    public static final BlockEntityType<RfidScannerBlockEntity> RFID_SCANNER_BE =
            blockEntity("rfid_scanner", RfidScannerBlockEntity::new, RFID_SCANNER);

    // ---- Creative tab ----
    public static final CreativeModeTab TAB = tab("minimalperipherals", MinimalPeripherals.PLATFORM.createTab(
            Component.translatable("itemGroup.minimalperipherals"),
            () -> new ItemStack(NFC_READER),
            (parameters, output) -> {
                output.accept(CRYPTOGRAPHIC_ACCELERATOR);
                output.accept(NFC_READER);
                output.accept(NFC_CARD);
                output.accept(RFID_SCANNER);
                output.accept(RFID_BADGE);
            }));

    // ---- Registration hooks, called by the loaders ----
    public static void registerDataComponents(BiConsumer<Identifier, DataComponentType<?>> register) {
        COMPONENTS.forEach(register);
    }

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
        registrar.register(CRYPTOGRAPHIC_ACCELERATOR_BE, (be, side) -> be.peripheral());
        registrar.register(NFC_READER_BE, (be, side) -> be.peripheral());
        registrar.register(RFID_SCANNER_BE, (be, side) -> be.peripheral(side));
    }

    // ---- helpers ----
    private static <T> DataComponentType<T> component(String name, Codec<T> codec, StreamCodec<? super RegistryFriendlyByteBuf, T> stream) {
        var type = DataComponentType.<T>builder().persistent(codec).networkSynchronized(stream).build();
        COMPONENTS.put(MinimalPeripherals.id(name), type);
        return type;
    }

    private static <T extends Block> T block(String name, Function<BlockBehaviour.Properties, T> factory) {
        var id = MinimalPeripherals.id(name);
        var blockKey = ResourceKey.create(Registries.BLOCK, id);
        var itemKey = ResourceKey.create(Registries.ITEM, id);

        var properties = BlockBehaviour.Properties.of()
                .strength(2.0F)
                .mapColor(MapColor.STONE)
                .setId(blockKey);
        var block = factory.apply(properties);
        BLOCKS.put(id, block);

        ITEMS.put(id, new BlockItem(block, new Item.Properties().setId(itemKey).useBlockDescriptionPrefix()));
        return block;
    }

    private static <T extends Item> T item(String name, Function<Item.Properties, T> factory, Item.Properties properties) {
        var id = MinimalPeripherals.id(name);
        var item = factory.apply(properties.setId(ResourceKey.create(Registries.ITEM, id)));
        ITEMS.put(id, item);
        return item;
    }

    private static <T extends BlockEntity> BlockEntityType<T> blockEntity(String name, Platform.BlockEntityFactory<T> factory, Block... blocks) {
        var type = MinimalPeripherals.PLATFORM.createBlockEntityType(factory, blocks);
        BLOCK_ENTITIES.put(MinimalPeripherals.id(name), type);
        return type;
    }

    private static CreativeModeTab tab(String name, CreativeModeTab tab) {
        TABS.put(MinimalPeripherals.id(name), tab);
        return tab;
    }
}
