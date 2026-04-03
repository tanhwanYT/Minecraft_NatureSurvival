package my.pkg.ability;

import org.bukkit.GameMode;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.EnumMap;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;

public class EarthAbility implements Ability {

    private final JavaPlugin plugin;

    private static final double DEFAULT_MAX_HEALTH = 20.0;
    private static final double EARTH_MAX_HEALTH = 24.0;

    private static final double DEFAULT_ATTACK_SPEED = 4.0;
    private static final double EARTH_ATTACK_SPEED = 3.0;

    private static final double DOUBLE_DROP_CHANCE = 0.25;

    private final Map<Material, Material> smeltMap = new EnumMap<>(Material.class);
    private final Map<Material, ItemStack> bonusDropMap = new EnumMap<>(Material.class);

    public EarthAbility(JavaPlugin plugin) {
        this.plugin = plugin;
        initSmeltMap();
        initBonusDropMap();
    }

    @Override
    public String name() {
        return "대지";
    }

    @Override
    public void onGrant(Player player) {
        applyBaseStats(player);
    }

    @Override
    public void onRemove(Player player) {
        resetBaseStats(player);
    }

    @Override
    public void onTick(Player player) {
        applyBaseStats(player);
    }

    @Override
    public void onBreakBlock(Player player, BlockBreakEvent event) {
        if (!isInOverworld(player)) return;
        if (player.getGameMode() == GameMode.CREATIVE) return;

        Block block = event.getBlock();
        Material type = block.getType();

        Material smelted = smeltMap.get(type);
        if (smelted != null) {
            event.setDropItems(false);
            block.getWorld().dropItemNaturally(block.getLocation(), new ItemStack(smelted, 1));

            if (shouldDoubleDrop()) {
                block.getWorld().dropItemNaturally(block.getLocation(), new ItemStack(smelted, 1));
            }
            return;
        }

        ItemStack bonus = bonusDropMap.get(type);
        if (bonus != null && shouldDoubleDrop()) {
            block.getWorld().dropItemNaturally(block.getLocation(), bonus.clone());
        }
    }

    private void applyBaseStats(Player player) {
        if (isInOverworld(player)) {
            setMaxHealth(player, EARTH_MAX_HEALTH);
        } else {
            setMaxHealth(player, DEFAULT_MAX_HEALTH);
        }

        setAttackSpeed(player, EARTH_ATTACK_SPEED);
    }

    private void resetBaseStats(Player player) {
        setMaxHealth(player, DEFAULT_MAX_HEALTH);
        setAttackSpeed(player, DEFAULT_ATTACK_SPEED);
    }

    private void setMaxHealth(Player player, double value) {
        AttributeInstance attr = player.getAttribute(Attribute.MAX_HEALTH);
        if (attr == null) return;

        if (Math.abs(attr.getBaseValue() - value) > 0.0001) {
            attr.setBaseValue(value);
        }

        if (player.getHealth() > value) {
            player.setHealth(value);
        }
    }

    private void setAttackSpeed(Player player, double value) {
        AttributeInstance attr = player.getAttribute(Attribute.ATTACK_SPEED);
        if (attr == null) return;

        if (Math.abs(attr.getBaseValue() - value) > 0.0001) {
            attr.setBaseValue(value);
        }
    }

    private boolean isInOverworld(Player player) {
        return player.getWorld().getEnvironment() == World.Environment.NORMAL;
    }

    private boolean shouldDoubleDrop() {
        return ThreadLocalRandom.current().nextDouble() < DOUBLE_DROP_CHANCE;
    }

    private void initSmeltMap() {
        smeltMap.put(Material.IRON_ORE, Material.IRON_INGOT);
        smeltMap.put(Material.DEEPSLATE_IRON_ORE, Material.IRON_INGOT);

        smeltMap.put(Material.GOLD_ORE, Material.GOLD_INGOT);
        smeltMap.put(Material.DEEPSLATE_GOLD_ORE, Material.GOLD_INGOT);

        smeltMap.put(Material.COPPER_ORE, Material.COPPER_INGOT);
        smeltMap.put(Material.DEEPSLATE_COPPER_ORE, Material.COPPER_INGOT);

        smeltMap.put(Material.ANCIENT_DEBRIS, Material.NETHERITE_SCRAP);

        smeltMap.put(Material.SAND, Material.GLASS);
        smeltMap.put(Material.RED_SAND, Material.GLASS);
    }

    private void initBonusDropMap() {
        bonusDropMap.put(Material.COAL_ORE, new ItemStack(Material.COAL, 1));
        bonusDropMap.put(Material.DEEPSLATE_COAL_ORE, new ItemStack(Material.COAL, 1));

        bonusDropMap.put(Material.DIAMOND_ORE, new ItemStack(Material.DIAMOND, 1));
        bonusDropMap.put(Material.DEEPSLATE_DIAMOND_ORE, new ItemStack(Material.DIAMOND, 1));

        bonusDropMap.put(Material.EMERALD_ORE, new ItemStack(Material.EMERALD, 1));
        bonusDropMap.put(Material.DEEPSLATE_EMERALD_ORE, new ItemStack(Material.EMERALD, 1));

        bonusDropMap.put(Material.LAPIS_ORE, new ItemStack(Material.LAPIS_LAZULI, 1));
        bonusDropMap.put(Material.DEEPSLATE_LAPIS_ORE, new ItemStack(Material.LAPIS_LAZULI, 1));

        bonusDropMap.put(Material.REDSTONE_ORE, new ItemStack(Material.REDSTONE, 1));
        bonusDropMap.put(Material.DEEPSLATE_REDSTONE_ORE, new ItemStack(Material.REDSTONE, 1));

        bonusDropMap.put(Material.NETHER_QUARTZ_ORE, new ItemStack(Material.QUARTZ, 1));
        bonusDropMap.put(Material.NETHER_GOLD_ORE, new ItemStack(Material.GOLD_NUGGET, 2));

        bonusDropMap.put(Material.OAK_LOG, new ItemStack(Material.OAK_LOG, 1));
        bonusDropMap.put(Material.SPRUCE_LOG, new ItemStack(Material.SPRUCE_LOG, 1));
        bonusDropMap.put(Material.BIRCH_LOG, new ItemStack(Material.BIRCH_LOG, 1));
        bonusDropMap.put(Material.JUNGLE_LOG, new ItemStack(Material.JUNGLE_LOG, 1));
        bonusDropMap.put(Material.ACACIA_LOG, new ItemStack(Material.ACACIA_LOG, 1));
        bonusDropMap.put(Material.DARK_OAK_LOG, new ItemStack(Material.DARK_OAK_LOG, 1));
        bonusDropMap.put(Material.MANGROVE_LOG, new ItemStack(Material.MANGROVE_LOG, 1));
        bonusDropMap.put(Material.CHERRY_LOG, new ItemStack(Material.CHERRY_LOG, 1));
        bonusDropMap.put(Material.PALE_OAK_LOG, new ItemStack(Material.PALE_OAK_LOG, 1));
    }
}