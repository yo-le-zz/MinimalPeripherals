package minimalperipherals.rfid;

import dan200.computercraft.api.lua.LuaFunction;
import dan200.computercraft.api.lua.MethodResult;
import dan200.computercraft.api.lua.ObjectLuaTable;
import dan200.computercraft.api.peripheral.AttachedComputerSet;
import dan200.computercraft.api.peripheral.IComputerAccess;
import dan200.computercraft.api.peripheral.IPeripheral;
import minimalperipherals.Limits;
import minimalperipherals.ModRegistry;
import minimalperipherals.dataholder.DataHolder;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class RfidScannerPeripheral implements IPeripheral {
    /** Time (in ticks) between scan() and the rfid_scan event. */
    public static final int SCAN_TIME = 4;

    private record ScannedBadge(String data, double distance) {
    }

    private final RfidScannerBlockEntity scanner;
    private final AttachedComputerSet computers = new AttachedComputerSet();

    private long scanAt = 0;
    private boolean scanScheduled = false;
    private boolean scanStarted = false;

    public RfidScannerPeripheral(RfidScannerBlockEntity scanner) {
        this.scanner = scanner;
    }

    @Override
    public String getType() {
        return "rfid_scanner";
    }

    @Override
    public boolean equals(IPeripheral other) {
        return other instanceof RfidScannerPeripheral o && scanner == o.scanner;
    }

    @Override
    public void attach(IComputerAccess computer) {
        computers.add(computer);
    }

    @Override
    public void detach(IComputerAccess computer) {
        computers.remove(computer);
    }

    // ---- scanning ----

    private void updateState(boolean active) {
        var level = scanner.getLevel();
        if (level != null) {
            level.setBlockAndUpdate(scanner.getBlockPos(), scanner.getBlockState().setValue(RfidScannerBlock.ACTIVE, active));
        }
    }

    private void addIfBadge(ItemStack stack, double distance, List<ScannedBadge> badges) {
        if (stack.is(ModRegistry.RFID_BADGE)) {
            DataHolder.getData(stack).ifPresent(data -> badges.add(new ScannedBadge(data, distance)));
        }
    }

    private List<ScannedBadge> scanBadges(Level level) {
        var badges = new ArrayList<ScannedBadge>();
        var origin = Vec3.atCenterOf(scanner.getBlockPos().relative(scanner.getDirection()));
        double range = Limits.RFID_SCAN_RANGE;
        var area = new AABB(origin, origin).inflate(range);

        // Badges in the inventory of nearby players
        for (var player : level.players()) {
            var distance = player.position().distanceTo(origin);
            if (distance > range) {
                continue;
            }

            var inventory = player.getInventory();
            for (var slot = 0; slot < inventory.getContainerSize(); slot++) {
                addIfBadge(inventory.getItem(slot), distance, badges);
            }
        }

        // Badges held by other living entities
        for (var entity : level.getEntitiesOfClass(LivingEntity.class, area)) {
            if (entity instanceof Player) {
                continue;
            }

            var distance = entity.position().distanceTo(origin);
            addIfBadge(entity.getMainHandItem(), distance, badges);
            addIfBadge(entity.getOffhandItem(), distance, badges);
        }

        // Dropped badges
        for (var item : level.getEntitiesOfClass(ItemEntity.class, area)) {
            addIfBadge(item.getItem(), item.position().distanceTo(origin), badges);
        }

        return badges;
    }

    public void tick(Level level) {
        if (level.isClientSide() || !scanScheduled) {
            return;
        }

        if (scanStarted) {
            scanStarted = false;
            updateState(true);
        }

        if (level.getGameTime() >= scanAt) {
            scanScheduled = false;
            emitScanEvent(scanBadges(level));
            updateState(false);
        }
    }

    private void scheduleScan() {
        var level = scanner.getLevel();
        if (level == null) {
            return;
        }

        scanAt = level.getGameTime() + SCAN_TIME;
        scanScheduled = true;
        scanStarted = true;
    }

    private void emitScanEvent(List<ScannedBadge> badges) {
        var map = new HashMap<Integer, ObjectLuaTable>();
        for (var i = 1; i <= badges.size(); i++) {
            var badge = badges.get(i - 1);
            map.put(i, new ObjectLuaTable(Map.of(
                    "data", badge.data(),
                    "distance", badge.distance()
            )));
        }

        var table = new ObjectLuaTable(map);
        computers.forEach(computer -> computer.queueEvent("rfid_scan", computer.getAttachmentName(), table));
    }

    // ---- Lua API ----

    @LuaFunction(mainThread = true)
    public final MethodResult scan() {
        scheduleScan();
        return MethodResult.pullEvent("rfid_scan", args -> {
            if (args.length != 3) {
                return null;
            }

            return MethodResult.of(args[2]);
        });
    }
}
