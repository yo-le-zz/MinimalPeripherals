package minimalperipherals.neoforge;

import dan200.computercraft.api.peripheral.PeripheralCapability;
import minimalperipherals.MinimalPeripherals;
import minimalperipherals.ModRegistry;
import minimalperipherals.platform.PeripheralProvider;
import minimalperipherals.platform.PeripheralRegistrar;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;
import net.neoforged.neoforge.registries.RegisterEvent;

@Mod(MinimalPeripherals.MOD_ID)
public class MinimalPeripheralsNeoForge {
    public MinimalPeripheralsNeoForge(IEventBus modBus) {
        MinimalPeripherals.PLATFORM = new NeoForgePlatform();

        modBus.addListener(this::onRegister);
        modBus.addListener(this::onRegisterCapabilities);
    }

    private void onRegister(RegisterEvent event) {
        // RegisterEvent is fired once per registry; the objects themselves are created once in ModRegistry.
        event.register(Registries.DATA_COMPONENT_TYPE, helper -> ModRegistry.registerDataComponents((id, type) -> helper.register(id, type)));
        event.register(Registries.BLOCK, helper -> ModRegistry.registerBlocks((id, block) -> helper.register(id, block)));
        event.register(Registries.ITEM, helper -> ModRegistry.registerItems((id, item) -> helper.register(id, item)));
        event.register(Registries.BLOCK_ENTITY_TYPE, helper -> ModRegistry.registerBlockEntityTypes((id, type) -> helper.register(id, type)));
        event.register(Registries.CREATIVE_MODE_TAB, helper -> ModRegistry.registerCreativeTabs((id, tab) -> helper.register(id, tab)));
    }

    private void onRegisterCapabilities(RegisterCapabilitiesEvent event) {
        ModRegistry.registerPeripherals(new PeripheralRegistrar() {
            @Override
            public <B extends BlockEntity> void register(BlockEntityType<B> type, PeripheralProvider<B> provider) {
                event.registerBlockEntity(PeripheralCapability.get(), type, provider::getPeripheral);
            }
        });
    }
}
