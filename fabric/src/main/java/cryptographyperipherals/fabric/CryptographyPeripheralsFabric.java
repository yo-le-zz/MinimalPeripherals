package cryptographyperipherals.fabric;

import cryptographyperipherals.CryptographyPeripherals;
import cryptographyperipherals.ModRegistry;
import cryptographyperipherals.platform.PeripheralProvider;
import cryptographyperipherals.platform.PeripheralRegistrar;
import dan200.computercraft.api.peripheral.PeripheralLookup;
import net.fabricmc.api.ModInitializer;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;

public class CryptographyPeripheralsFabric implements ModInitializer {
    @Override
    public void onInitialize() {
        CryptographyPeripherals.PLATFORM = new FabricPlatform();

        // The order matters: blocks and items before block entity types.
        ModRegistry.registerBlocks((id, block) -> Registry.register(BuiltInRegistries.BLOCK, id, block));
        ModRegistry.registerItems((id, item) -> Registry.register(BuiltInRegistries.ITEM, id, item));
        ModRegistry.registerBlockEntityTypes((id, type) -> Registry.register(BuiltInRegistries.BLOCK_ENTITY_TYPE, id, type));
        ModRegistry.registerCreativeTabs((id, tab) -> Registry.register(BuiltInRegistries.CREATIVE_MODE_TAB, id, tab));

        ModRegistry.registerPeripherals(new PeripheralRegistrar() {
            @Override
            @SuppressWarnings("unchecked")
            public <B extends BlockEntity> void register(BlockEntityType<B> type, PeripheralProvider<B> provider) {
                PeripheralLookup.get().registerForBlockEntity((be, side) -> provider.getPeripheral((B) be, side), type);
            }
        });
    }
}
