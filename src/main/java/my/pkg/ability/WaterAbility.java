package my.pkg.ability;

import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.util.List;

public class WaterAbility implements Ability {

    private final JavaPlugin plugin;
    private final NamespacedKey lockedSlotKey;

    private static final double DEFAULT_MOVE_SPEED = 0.1;
    private static final double OUT_OF_WATER_MOVE_SPEED = 0.075;

    public WaterAbility(JavaPlugin plugin) {
        this.plugin = plugin;
        this.lockedSlotKey = new NamespacedKey(plugin, "water_locked_slot");
    }

    @Override
    public String name() {
        return "물";
    }

    @Override
    public void onGrant(Player player) {
        applyLockedSlots(player);
        applyOutsideWaterSlow(player);
    }

    @Override
    public void onRemove(Player player) {
        clearLockedSlots(player);
        clearWaterOnlyBuffs(player);
        resetMoveSpeed(player);
    }

    @Override
    public void onTick(Player player) {
        boolean inWater = isInWater(player);

        // 잠금 슬롯이 비어 있거나 사라졌으면 다시 채움
        ensureLockedSlots(player);

        if (inWater) {
            applyWaterBuffs(player);
            resetMoveSpeed(player);
        } else {
            clearWaterOnlyBuffs(player);
            applyOutsideWaterSlow(player);
        }
    }

    @Override
    public void onMove(Player player, PlayerMoveEvent event) {
        boolean inWater = isInWater(player);

        if (inWater) {
            resetMoveSpeed(player);
        } else {
            applyOutsideWaterSlow(player);
        }
    }

    @Override
    public void onBreakBlock(Player player, BlockBreakEvent event) {
        if (!isInWater(player)) return;

        player.addPotionEffect(new PotionEffect(
                PotionEffectType.HASTE,
                40,
                1,
                false,
                false,
                false
        ));
    }

    private void applyWaterBuffs(Player player) {
        player.addPotionEffect(new PotionEffect(
                PotionEffectType.WATER_BREATHING,
                40,
                0,
                false,
                false,
                false
        ));

        player.addPotionEffect(new PotionEffect(
                PotionEffectType.DOLPHINS_GRACE,
                40,
                0,
                false,
                false,
                false
        ));

        player.addPotionEffect(new PotionEffect(
                PotionEffectType.STRENGTH,
                40,
                0,
                false,
                false,
                false
        ));

        player.addPotionEffect(new PotionEffect(
                PotionEffectType.RESISTANCE,
                40,
                0,
                false,
                false,
                false
        ));

        player.addPotionEffect(new PotionEffect(
                PotionEffectType.HASTE,
                40,
                1,
                false,
                false,
                false
        ));
    }

    private void clearWaterOnlyBuffs(Player player) {
        player.removePotionEffect(PotionEffectType.WATER_BREATHING);
        player.removePotionEffect(PotionEffectType.DOLPHINS_GRACE);
        player.removePotionEffect(PotionEffectType.STRENGTH);
        player.removePotionEffect(PotionEffectType.RESISTANCE);
        player.removePotionEffect(PotionEffectType.HASTE);
    }

    private void applyOutsideWaterSlow(Player player) {
        AttributeInstance moveAttr = player.getAttribute(Attribute.MOVEMENT_SPEED);
        if (moveAttr == null) return;

        if (Math.abs(moveAttr.getBaseValue() - OUT_OF_WATER_MOVE_SPEED) > 0.0001) {
            moveAttr.setBaseValue(OUT_OF_WATER_MOVE_SPEED);
        }
    }

    private void resetMoveSpeed(Player player) {
        AttributeInstance moveAttr = player.getAttribute(Attribute.MOVEMENT_SPEED);
        if (moveAttr == null) return;

        if (Math.abs(moveAttr.getBaseValue() - DEFAULT_MOVE_SPEED) > 0.0001) {
            moveAttr.setBaseValue(DEFAULT_MOVE_SPEED);
        }
    }

    private boolean isInWater(Player player) {
        Block feet = player.getLocation().getBlock();
        Block head = player.getEyeLocation().getBlock();
        Block below = player.getLocation().clone().subtract(0, 0.2, 0).getBlock();

        return isWater(feet.getType())
                || isWater(head.getType())
                || isWater(below.getType())
                || player.isInWater()
                || player.isUnderWater();
    }

    private boolean isWater(Material material) {
        return material == Material.WATER || material == Material.BUBBLE_COLUMN;
    }

    public void applyLockedSlots(Player player) {
        for (int slot = 30; slot <= 35; slot++) {
            ItemStack current = player.getInventory().getItem(slot);
            if (!isLockedSlotItem(current)) {
                player.getInventory().setItem(slot, createLockedSlotItem());
            }
        }
        player.updateInventory();
    }

    public void clearLockedSlots(Player player) {
        for (int slot = 30; slot <= 35; slot++) {
            ItemStack item = player.getInventory().getItem(slot);
            if (isLockedSlotItem(item)) {
                player.getInventory().setItem(slot, null);
            }
        }
        player.updateInventory();
    }

    public void ensureLockedSlots(Player player) {
        for (int slot = 30; slot <= 35; slot++) {
            ItemStack item = player.getInventory().getItem(slot);
            if (!isLockedSlotItem(item)) {
                player.getInventory().setItem(slot, createLockedSlotItem());
            }
        }
    }

    public boolean isLockedSlotItem(ItemStack item) {
        if (item == null || item.getType() != Material.RED_STAINED_GLASS_PANE) return false;
        if (!item.hasItemMeta()) return false;

        ItemMeta meta = item.getItemMeta();
        if (meta == null) return false;

        PersistentDataContainer pdc = meta.getPersistentDataContainer();
        Byte value = pdc.get(lockedSlotKey, PersistentDataType.BYTE);
        return value != null && value == (byte) 1;
    }

    public ItemStack createLockedSlotItem() {
        ItemStack item = new ItemStack(Material.RED_STAINED_GLASS_PANE);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName("§c잠긴 슬롯");
            meta.setLore(List.of(
                    "§7물 원소의 패널티로",
                    "§7사용할 수 없는 슬롯입니다."
            ));
            meta.getPersistentDataContainer().set(lockedSlotKey, PersistentDataType.BYTE, (byte) 1);
            item.setItemMeta(meta);
        }
        return item;
    }
}