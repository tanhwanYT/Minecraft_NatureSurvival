package my.pkg.ability;

import org.bukkit.GameMode;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.player.PlayerToggleSneakEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.util.Vector;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class WindAbility implements Ability {

    private final JavaPlugin plugin;
    private final NamespacedKey lockedSlotKey;

    private final Map<UUID, Long> sneakStartTime = new HashMap<>();
    private final Map<UUID, Boolean> chargedState = new HashMap<>();

    private static final double DEFAULT_MAX_HEALTH = 20.0;
    private static final double WIND_MAX_HEALTH = 16.0;

    private static final long FLY_CHARGE_MS = 3000L;
    private static final double FLY_UP_POWER = 1.2;

    public WindAbility(JavaPlugin plugin) {
        this.plugin = plugin;
        this.lockedSlotKey = new NamespacedKey(plugin, "wind_locked_slot");
    }

    @Override
    public String name() {
        return "바람";
    }

    @Override
    public void onGrant(Player player) {
        applyMaxHealthPenalty(player);
        applyPassiveBuff(player);
        applyLockedSlots(player);
    }

    @Override
    public void onRemove(Player player) {
        resetMaxHealth(player);
        clearLockedSlots(player);
        sneakStartTime.remove(player.getUniqueId());
        chargedState.remove(player.getUniqueId());
    }

    @Override
    public void onTick(Player player) {
        applyPassiveBuff(player);
        updateChargeBar(player);
        ensureLockedSlots(player);
    }

    @Override
    public void onDamaged(Player player, EntityDamageEvent event) {
        if (event.getCause() == EntityDamageEvent.DamageCause.FALL) {
            event.setCancelled(true);
        }
    }

    @Override
    public void onSneakToggle(Player player, PlayerToggleSneakEvent event) {
        UUID uuid = player.getUniqueId();

        if (event.isSneaking()) {
            if (!canUseWindJump(player)) return;

            sneakStartTime.put(uuid, System.currentTimeMillis());
            chargedState.put(uuid, false);
            player.sendActionBar("§f[바람] §7기류 충전 시작...");
            return;
        }

        Long start = sneakStartTime.get(uuid);
        boolean charged = chargedState.getOrDefault(uuid, false);

        sneakStartTime.remove(uuid);
        chargedState.remove(uuid);

        if (start == null) return;
        if (!canUseWindJump(player)) return;

        if (!charged) {
            player.sendActionBar("§c[바람] 충전이 완료되지 않았습니다.");
            return;
        }

        launchUp(player);
    }

    private void applyPassiveBuff(Player player) {
        player.addPotionEffect(new PotionEffect(
                PotionEffectType.SPEED,
                40,
                0,
                false,
                false,
                false
        ));
    }

    private void applyMaxHealthPenalty(Player player) {
        AttributeInstance attr = player.getAttribute(Attribute.MAX_HEALTH);
        if (attr == null) return;

        if (Math.abs(attr.getBaseValue() - WIND_MAX_HEALTH) > 0.0001) {
            attr.setBaseValue(WIND_MAX_HEALTH);
        }

        if (player.getHealth() > WIND_MAX_HEALTH) {
            player.setHealth(WIND_MAX_HEALTH);
        }
    }

    private void resetMaxHealth(Player player) {
        AttributeInstance attr = player.getAttribute(Attribute.MAX_HEALTH);
        if (attr == null) return;

        if (Math.abs(attr.getBaseValue() - DEFAULT_MAX_HEALTH) > 0.0001) {
            attr.setBaseValue(DEFAULT_MAX_HEALTH);
        }

        if (player.getHealth() > DEFAULT_MAX_HEALTH) {
            player.setHealth(DEFAULT_MAX_HEALTH);
        }
    }

    private boolean canUseWindJump(Player player) {
        if (player.isFlying()) return false;
        if (player.getGameMode() == GameMode.SPECTATOR) return false;
        if (player.isInsideVehicle()) return false;
        return true;
    }

    private void updateChargeBar(Player player) {
        if (!player.isSneaking()) return;
        if (!canUseWindJump(player)) return;

        UUID uuid = player.getUniqueId();
        Long start = sneakStartTime.get(uuid);
        if (start == null) return;

        long now = System.currentTimeMillis();
        long held = now - start;

        if (held >= FLY_CHARGE_MS) {
            if (!chargedState.getOrDefault(uuid, false)) {
                chargedState.put(uuid, true);
                player.sendMessage("§b[바람] 상승 기류 준비 완료! 쉬프트를 떼면 날아오릅니다.");
                player.sendActionBar("§b[바람] 준비 완료! §f쉬프트를 떼세요");
            } else {
                player.sendActionBar("§b[바람] 준비 완료! §f쉬프트를 떼세요");
            }
            return;
        }

        double progress = (double) held / FLY_CHARGE_MS;
        int bars = (int) Math.floor(progress * 10);

        StringBuilder gauge = new StringBuilder("§f[바람] §7충전: ");
        for (int i = 0; i < 10; i++) {
            gauge.append(i < bars ? "§b■" : "§7■");
        }

        int percent = (int) (progress * 100);
        gauge.append(" §f").append(percent).append("%");

        player.sendActionBar(gauge.toString());
    }

    private void launchUp(Player player) {
        Vector current = player.getVelocity();
        Vector launch = new Vector(0, FLY_UP_POWER, 0);

        if (current.getY() > 0) {
            launch.setY(Math.max(FLY_UP_POWER, current.getY()));
        }

        player.setVelocity(launch);
        player.setFallDistance(0f);

        player.sendMessage("§b[바람] 상승 기류를 타고 날아올랐습니다!");
        player.sendActionBar("§b[바람] 상승!");
    }

    public void applyLockedSlots(Player player) {
        for (int slot = 27; slot <= 35; slot++) {
            ItemStack current = player.getInventory().getItem(slot);
            if (!isLockedSlotItem(current)) {
                player.getInventory().setItem(slot, createLockedSlotItem());
            }
        }
        player.updateInventory();
    }

    public void clearLockedSlots(Player player) {
        for (int slot = 27; slot <= 35; slot++) {
            ItemStack item = player.getInventory().getItem(slot);
            if (isLockedSlotItem(item)) {
                player.getInventory().setItem(slot, null);
            }
        }
        player.updateInventory();
    }

    public void ensureLockedSlots(Player player) {
        for (int slot = 27; slot <= 35; slot++) {
            ItemStack item = player.getInventory().getItem(slot);
            if (!isLockedSlotItem(item)) {
                player.getInventory().setItem(slot, createLockedSlotItem());
            }
        }
    }

    public boolean isLockedSlotItem(ItemStack item) {
        if (item == null || item.getType() != Material.LIGHT_BLUE_STAINED_GLASS_PANE) return false;
        if (!item.hasItemMeta()) return false;

        ItemMeta meta = item.getItemMeta();
        if (meta == null) return false;

        PersistentDataContainer pdc = meta.getPersistentDataContainer();
        Byte value = pdc.get(lockedSlotKey, PersistentDataType.BYTE);
        return value != null && value == (byte) 1;
    }

    public ItemStack createLockedSlotItem() {
        ItemStack item = new ItemStack(Material.LIGHT_BLUE_STAINED_GLASS_PANE);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName("§b가벼워진 수납칸");
            meta.setLore(List.of(
                    "§7바람 원소의 페널티로",
                    "§79칸을 사용할 수 없습니다."
            ));
            meta.getPersistentDataContainer().set(lockedSlotKey, PersistentDataType.BYTE, (byte) 1);
            item.setItemMeta(meta);
        }
        return item;
    }
}