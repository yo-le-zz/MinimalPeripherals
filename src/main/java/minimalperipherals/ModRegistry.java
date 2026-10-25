package minimalperipherals;

import com.mojang.serialization.Codec;
import dan200.computercraft.api.peripheral.PeripheralLookup;
import minimalperipherals.crypto.CryptographicAcceleratorBlock;
import minimalperipherals.crypto.CryptographicAcceleratorBlockEntity;
import minimalperipherals.nfc.NfcCardItem;
import minimalperipherals.nfc.NfcReaderBlock;
import minimalperipherals.nfc.NfcReaderBlockEntity;
import minimalperipherals.rfid.RfidBadgeItem;
import minimalperipherals.rfid.RfidScannerBlock;
import minimalperipherals.rfid.RfidScannerBlockEntity;
import net.fabricmc.fabric.api.itemgroup.v1.FabricItemGroup;
import net.fabricmc.fabric.api.object.builder.v1.block.entity.FabricBlockEntityTypeBuilder;
import net.minecraft.core.Registry;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
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

import java.util.function.Function;

public final class ModRegistry {
    private ModRegistry() {
    }

    // ---- Data components (data written on cards and badges) ----
    public static final DataComponentType<String> NFC_DATA = component("nfc_data", Codec.STRING, ByteBufCodecs.STRING_UTF8);
    public static final DataComponentType<String> NFC_PRIVATE_KEY = component("nfc_private_key", Codec.STRING, ByteBufCodecs.STRING_UTF8);
    public static final DataComponentType<Boolean> NFC_READONLY = component("nfc_readonly", Codec.BOOL, ByteBufCodecs.BOOL);

    // ---- Blocks (each one also registers its BlockItem) ----
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
    public static final CreativeModeTab TAB = Registry.register(
            BuiltInRegistries.CREATIVE_MODE_TAB,
            MinimalPeripherals.id("minimalperipherals"),
            FabricItemGroup.builder()
                    .title(Component.translatable("itemGroup.minimalperipherals"))
                    .icon(() -> new ItemStack(NFC_READER))
                    .displayItems((parameters, output) -> {
                        output.accept(CRYPTOGRAPHIC_ACCELERATOR);
                        output.accept(NFC_READER);
                        output.accept(NFC_CARD);
                        output.accept(RFID_SCANNER);
                        output.accept(RFID_BADGE);
                    })
                    .build());

    /** Called from the mod initializer: loading this class registers everything above. */
    public static void init() {
        PeripheralLookup.get().registerForBlockEntity(
                (be, side) -> ((CryptographicAcceleratorBlockEntity) be).peripheral(), CRYPTOGRAPHIC_ACCELERATOR_BE);
        PeripheralLookup.get().registerForBlockEntity(
                (be, side) -> ((NfcReaderBlockEntity) be).peripheral(), NFC_READER_BE);
        PeripheralLookup.get().registerForBlockEntity(
                (be, side) -> ((RfidScannerBlockEntity) be).peripheral(side), RFID_SCANNER_BE);
    }

    // ---- helpers ----
    private static <T> DataComponentType<T> component(String name, Codec<T> codec, StreamCodec<? super RegistryFriendlyByteBuf, T> stream) {
        return Registry.register(
                BuiltInRegistries.DATA_COMPONENT_TYPE,
                MinimalPeripherals.id(name),
                DataComponentType.<T>builder().persistent(codec).networkSynchronized(stream).build());
    }

    private static <T extends Block> T block(String name, Function<BlockBehaviour.Properties, T> factory) {
        var id = MinimalPeripherals.id(name);
        var blockKey = ResourceKey.create(Registries.BLOCK, id);
        var itemKey = ResourceKey.create(Registries.ITEM, id);

        var properties = BlockBehaviour.Properties.of()
                .strength(2.0F)
                .mapColor(MapColor.STONE)
                .setId(blockKey);
        var block = Registry.register(BuiltInRegistries.BLOCK, blockKey, factory.apply(properties));

        Registry.register(BuiltInRegistries.ITEM, itemKey,
                new BlockItem(block, new Item.Properties().setId(itemKey).useBlockDescriptionPrefix()));
        return block;
    }

    private static <T extends Item> T item(String name, Function<Item.Properties, T> factory, Item.Properties properties) {
        var itemKey = ResourceKey.create(Registries.ITEM, MinimalPeripherals.id(name));
        return Registry.register(BuiltInRegistries.ITEM, itemKey, factory.apply(properties.setId(itemKey)));
    }

    private static <T extends BlockEntity> BlockEntityType<T> blockEntity(String name, FabricBlockEntityTypeBuilder.Factory<T> factory, Block... blocks) {
        return Registry.register(
                BuiltInRegistries.BLOCK_ENTITY_TYPE,
                MinimalPeripherals.id(name),
                FabricBlockEntityTypeBuilder.<T>create(factory, blocks).build());
    }
}
