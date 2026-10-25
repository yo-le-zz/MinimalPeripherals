package minimalperipherals.dataholder;

import minimalperipherals.ModRegistry;
import minimalperipherals.core.Crypto;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;

import java.util.Optional;

/** Helpers to read/write the data stored in the components of a card or badge. */
public final class DataHolder {
    private DataHolder() {
    }

    public static Optional<String> getData(ItemStack stack) {
        var data = stack.get(ModRegistry.NFC_DATA);
        return data == null || data.isEmpty() ? Optional.empty() : Optional.of(data);
    }

    public static void setData(ItemStack stack, String data) {
        stack.set(ModRegistry.NFC_DATA, data);
    }

    public static boolean isReadOnly(ItemStack stack) {
        return Boolean.TRUE.equals(stack.get(ModRegistry.NFC_READONLY));
    }

    public static void setReadOnly(ItemStack stack, boolean readOnly) {
        stack.set(ModRegistry.NFC_READONLY, readOnly);
    }

    public static void setLabel(ItemStack stack, String label) {
        stack.set(DataComponents.CUSTOM_NAME, Component.literal(label));
    }

    public static void clearLabel(ItemStack stack) {
        stack.remove(DataComponents.CUSTOM_NAME);
    }

    /** Returns the private key of the stack, generating one if the item supports keys and has none yet. */
    public static Optional<String> getPrivateKey(ItemStack stack) {
        if (!(stack.getItem() instanceof DataHolderItem holder) || !holder.supportsPrivateKey()) {
            return Optional.empty();
        }

        var key = stack.get(ModRegistry.NFC_PRIVATE_KEY);
        if (key == null) {
            key = Crypto.generatePrivateKey();
            stack.set(ModRegistry.NFC_PRIVATE_KEY, key);
        }
        return Optional.of(key);
    }

    public static void setPrivateKey(ItemStack stack, String privateKey) {
        if (stack.getItem() instanceof DataHolderItem holder && holder.supportsPrivateKey()) {
            stack.set(ModRegistry.NFC_PRIVATE_KEY, privateKey);
        }
    }
}
