package minimalperipherals.fabric;

import dan200.computercraft.api.peripheral.PeripheralLookup;
import minimalperipherals.MinimalPeripherals;
import minimalperipherals.ModRegistry;
import minimalperipherals.platform.PeripheralProvider;
import minimalperipherals.platform.PeripheralRegistrar;
import net.fabricmc.api.ModInitializer;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;

public class MinimalPeripheralsFabric implements ModInitializer {
    @Override
    public void onInitialize() {
        MinimalPeripherals.PLATFORM = new FabricPlatform();

        // The order matters: blocks and items before block entity types.
        ModRegistry.registerDataComponents((id, type) -> Registry.register(BuiltInRegistries.DATA_COMPONENT_TYPE, id, type));
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
