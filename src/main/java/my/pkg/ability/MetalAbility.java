package my.pkg.ability;

import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.inventory.CraftItemEvent;
import org.bukkit.event.player.PlayerToggleSneakEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.Recipe;
import org.bukkit.inventory.meta.Damageable;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

public class MetalAbility implements Ability {

    private final JavaPlugin plugin;
    private final NamespacedKey lockedSlotKey;

    private static final double DEFAULT_MOVE_SPEED = 0.1;
    private static final double METAL_MOVE_SPEED = 0.085;

    // 쉬프트 1번에 내구도 몇 회복할지
    private static final int REPAIR_AMOUNT = 12;

    // 제작 시 인챈트 확률
    private static final double ENCHANT_CHANCE = 0.28;

    public MetalAbility(JavaPlugin plugin) {
        this.plugin = plugin;
        this.lockedSlotKey = new NamespacedKey(plugin, "metal_locked_slot");
    }

    @Override
    public String name() {
        return "금속";
    }

    @Override
    public void onGrant(Player player) {
        applyLockedSlots(player);
        applyMovePenalty(player);
    }

    @Override
    public void onRemove(Player player) {
        clearLockedSlots(player);
        resetMoveSpeed(player);
    }

    @Override
    public void onTick(Player player) {
        ensureLockedSlots(player);
        applyMovePenalty(player);
    }

    @Override
    public void onDamaged(Player player, EntityDamageEvent event) {
        EntityDamageEvent.DamageCause cause = event.getCause();

        if (cause == EntityDamageEvent.DamageCause.FIRE
                || cause == EntityDamageEvent.DamageCause.FIRE_TICK
                || cause == EntityDamageEvent.DamageCause.LAVA
                || cause == EntityDamageEvent.DamageCause.HOT_FLOOR) {
            event.setDamage(event.getDamage() * 0.6); // 40% 감소
        }
    }

    @Override
    public void onSneakToggle(Player player, PlayerToggleSneakEvent event) {
        if (!event.isSneaking()) return;

        ItemStack hand = player.getInventory().getItemInMainHand();
        if (hand == null || hand.getType() == Material.AIR) return;
        if (!isRepairable(hand)) return;

        ItemMeta meta = hand.getItemMeta();
        if (!(meta instanceof Damageable damageable)) return;

        int currentDamage = damageable.getDamage();
        if (currentDamage <= 0) {
            player.sendActionBar("§7[금속] 이미 내구도가 최대입니다.");
            return;
        }

        int repaired = Math.min(REPAIR_AMOUNT, currentDamage);
        damageable.setDamage(currentDamage - repaired);
        hand.setItemMeta((ItemMeta) damageable);

        player.getInventory().setItemInMainHand(hand);
        player.sendActionBar("§b[금속] 장비의 내구도를 회복했습니다.");
    }

    @Override
    public void onCraft(Player player, CraftItemEvent event) {
        Recipe recipe = event.getRecipe();
        if (recipe == null || recipe.getResult() == null) return;

        if (ThreadLocalRandom.current().nextDouble() >= ENCHANT_CHANCE) return;

        ItemStack result = recipe.getResult().clone();
        if (!isEnchantableCraftResult(result)) return;

        applyRandomEnchant(result);

        // 결과칸 덮어쓰기
        event.getInventory().setResult(result);
        player.sendActionBar("§7[금속] 제작한 장비에 금속의 축복이 깃들었습니다.");
    }

    private void applyMovePenalty(Player player) {
        AttributeInstance moveAttr = player.getAttribute(Attribute.MOVEMENT_SPEED);
        if (moveAttr == null) return;

        if (Math.abs(moveAttr.getBaseValue() - METAL_MOVE_SPEED) > 0.0001) {
            moveAttr.setBaseValue(METAL_MOVE_SPEED);
        }
    }

    private void resetMoveSpeed(Player player) {
        AttributeInstance moveAttr = player.getAttribute(Attribute.MOVEMENT_SPEED);
        if (moveAttr == null) return;

        if (Math.abs(moveAttr.getBaseValue() - DEFAULT_MOVE_SPEED) > 0.0001) {
            moveAttr.setBaseValue(DEFAULT_MOVE_SPEED);
        }
    }

    private boolean isRepairable(ItemStack item) {
        if (item == null || item.getType() == Material.AIR) return false;
        return item.getItemMeta() instanceof Damageable;
    }

    private boolean isEnchantableCraftResult(ItemStack item) {
        if (item == null || item.getType() == Material.AIR) return false;

        Material type = item.getType();
        String name = type.name();

        return name.endsWith("_SWORD")
                || name.endsWith("_AXE")
                || name.endsWith("_PICKAXE")
                || name.endsWith("_SHOVEL")
                || name.endsWith("_HOE")
                || name.endsWith("_HELMET")
                || name.endsWith("_CHESTPLATE")
                || name.endsWith("_LEGGINGS")
                || name.endsWith("_BOOTS");
    }

    private void applyRandomEnchant(ItemStack item) {
        Material type = item.getType();
        String name = type.name();

        if (name.endsWith("_HELMET") || name.endsWith("_CHESTPLATE")
                || name.endsWith("_LEGGINGS") || name.endsWith("_BOOTS")) {

            int level = ThreadLocalRandom.current().nextBoolean() ? 1 : 2;
            item.addUnsafeEnchantment(Enchantment.PROTECTION, level);

            if (ThreadLocalRandom.current().nextDouble() < 0.45) {
                item.addUnsafeEnchantment(Enchantment.UNBREAKING, 1 + ThreadLocalRandom.current().nextInt(2));
            }
            return;
        }

        if (name.endsWith("_PICKAXE") || name.endsWith("_AXE")
                || name.endsWith("_SHOVEL") || name.endsWith("_HOE")) {

            int effLevel = ThreadLocalRandom.current().nextBoolean() ? 1 : 2;
            item.addUnsafeEnchantment(Enchantment.EFFICIENCY, effLevel);

            if (ThreadLocalRandom.current().nextDouble() < 0.45) {
                item.addUnsafeEnchantment(Enchantment.UNBREAKING, 1 + ThreadLocalRandom.current().nextInt(2));
            }
            return;
        }

        if (name.endsWith("_SWORD")) {
            if (ThreadLocalRandom.current().nextBoolean()) {
                item.addUnsafeEnchantment(Enchantment.SHARPNESS, 1 + ThreadLocalRandom.current().nextInt(2));
            } else {
                item.addUnsafeEnchantment(Enchantment.UNBREAKING, 1 + ThreadLocalRandom.current().nextInt(2));
            }
        }
    }

    public void applyLockedSlots(Player player) {
        for (int slot = 28; slot <= 35; slot++) {
            ItemStack current = player.getInventory().getItem(slot);
            if (!isLockedSlotItem(current)) {
                player.getInventory().setItem(slot, createLockedSlotItem());
            }
        }
        player.updateInventory();
    }

    public void clearLockedSlots(Player player) {
        for (int slot = 28; slot <= 35; slot++) {
            ItemStack item = player.getInventory().getItem(slot);
            if (isLockedSlotItem(item)) {
                player.getInventory().setItem(slot, null);
            }
        }
        player.updateInventory();
    }

    public void ensureLockedSlots(Player player) {
        for (int slot = 28; slot <= 35; slot++) {
            ItemStack item = player.getInventory().getItem(slot);
            if (!isLockedSlotItem(item)) {
                player.getInventory().setItem(slot, createLockedSlotItem());
            }
        }
    }

    public boolean isLockedSlotItem(ItemStack item) {
        if (item == null || item.getType() != Material.GRAY_STAINED_GLASS_PANE) return false;
        if (!item.hasItemMeta()) return false;

        ItemMeta meta = item.getItemMeta();
        if (meta == null) return false;

        PersistentDataContainer pdc = meta.getPersistentDataContainer();
        Byte value = pdc.get(lockedSlotKey, PersistentDataType.BYTE);
        return value != null && value == (byte) 1;
    }

    public ItemStack createLockedSlotItem() {
        ItemStack item = new ItemStack(Material.GRAY_STAINED_GLASS_PANE);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName("§8잠긴 슬롯");
            meta.setLore(List.of(
                    "§7금속 원소의 패널티로",
                    "§7사용할 수 없는 슬롯입니다."
            ));
            meta.getPersistentDataContainer().set(lockedSlotKey, PersistentDataType.BYTE, (byte) 1);
            item.setItemMeta(meta);
        }
        return item;
    }
}