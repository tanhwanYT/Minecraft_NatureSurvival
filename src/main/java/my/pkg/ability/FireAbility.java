package my.pkg.ability;

import org.bukkit.Material;
import org.bukkit.Tag;
import org.bukkit.block.Block;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.player.PlayerItemConsumeEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.event.entity.EntityPickupItemEvent;
import org.bukkit.util.Vector;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

public class FireAbility implements Ability {

    private final JavaPlugin plugin;

    // 물 데미지 쿨타임
    private final Map<UUID, Long> lastWaterDamageTime = new HashMap<>();

    // 설정값
    private static final double WATER_DAMAGE = 4.0; // 2칸
    private static final long WATER_DAMAGE_COOLDOWN_MS = 1000L; // 1초
    private static final double FOOD_BURN_CHANCE = 0.15; // 15%

    // 용암 이동 보정값
    private static final double LAVA_SWIM_SPEED = 0.38;
    private static final double LAVA_UP_SPEED = 0.18;
    private static final double LAVA_DOWN_LIMIT = -0.08;

    public FireAbility(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public String name() {
        return "불";
    }

    @Override
    public void onGrant(Player player) {
        applyPassiveEffects(player);
    }

    @Override
    public void onRemove(Player player) {
        lastWaterDamageTime.remove(player.getUniqueId());
    }

    @Override
    public void onTick(Player player) {
        applyPassiveEffects(player);
        handleWaterPenalty(player);
        handleLavaMovement(player);
    }

    @Override
    public void onAttack(Player player, EntityDamageByEntityEvent event) {
        if (!(event.getEntity() instanceof LivingEntity target)) return;

        // 3초 불
        target.setFireTicks(Math.max(target.getFireTicks(), 60));
    }

    @Override
    public void onMove(Player player, PlayerMoveEvent event) {
        // 이동할 때도 용암 가속 체크
        handleLavaMovement(player);
    }

    @Override
    public void onConsume(Player player, PlayerItemConsumeEvent event) {
        ItemStack item = event.getItem();
        if (item == null) return;

        Material type = item.getType();

        // 음식만 판정
        if (!type.isEdible()) return;

        // 낮은 확률로 음식이 타버림
        if (ThreadLocalRandom.current().nextDouble() >= FOOD_BURN_CHANCE) return;

        event.setCancelled(true);
        burnOneConsumedItem(player, event.getHand(), item);

        player.sendMessage("§c[불] 음식이 타버려서 먹지 못했습니다!");
        player.playSound(player.getLocation(), org.bukkit.Sound.ENTITY_GENERIC_EXTINGUISH_FIRE, 1.0f, 1.2f);
    }

    @Override
    public void onPickup(Player player, EntityPickupItemEvent event) {

        // 확률 체크 (15%)
        if (ThreadLocalRandom.current().nextDouble() > 0.15) return;

        // 아이템 태우기
        event.setCancelled(true);

        event.getItem().remove(); // 바닥 아이템 삭제

        player.sendMessage("§c[불] 아이템이 불타 사라졌습니다!");
        player.playSound(player.getLocation(), org.bukkit.Sound.BLOCK_FIRE_EXTINGUISH, 1.0f, 1.2f);
    }

    private void applyPassiveEffects(Player player) {
        player.addPotionEffect(new PotionEffect(
                PotionEffectType.FIRE_RESISTANCE,
                40, 0, false, false, false
        ));
    }

    private void handleLavaMovement(Player player) {
        if (!isTouchingLava(player)) return;
        if (player.isFlying()) return;
        if (player.isInsideVehicle()) return;

        Vector current = player.getVelocity();
        Vector direction = player.getLocation().getDirection().clone();

        // 바라보는 방향 기준 수평 이동
        direction.setY(0);
        if (direction.lengthSquared() > 0.0001) {
            direction.normalize().multiply(LAVA_SWIM_SPEED);
        }

        double y = current.getY();

        // 점프 중이면 약간 더 뜨게
        if (player.isJumping()) {
            y = Math.max(y, LAVA_UP_SPEED);
        } else if (y < LAVA_DOWN_LIMIT) {
            y = LAVA_DOWN_LIMIT;
        }

        Vector result = new Vector(direction.getX(), y, direction.getZ());

        // 가만히 있을 땐 너무 끌려가지 않게
        if (player.getLocation().getDirection().lengthSquared() < 0.0001) return;

        player.setVelocity(result);
        player.setFallDistance(0f);
    }


    private void handleWaterPenalty(Player player) {
        if (!isTouchingWater(player)) return;

        long now = System.currentTimeMillis();
        long last = lastWaterDamageTime.getOrDefault(player.getUniqueId(), 0L);

        if (now - last < WATER_DAMAGE_COOLDOWN_MS) return;

        lastWaterDamageTime.put(player.getUniqueId(), now);
        player.damage(WATER_DAMAGE);
        player.sendActionBar("§9물이 불 원소를 약화시킵니다!");
    }

    private boolean isTouchingWater(Player player) {
        Block feet = player.getLocation().getBlock();
        Block head = player.getEyeLocation().getBlock();
        Block below = player.getLocation().clone().subtract(0, 0.2, 0).getBlock();

        return isWaterBlock(feet.getType())
                || isWaterBlock(head.getType())
                || isWaterBlock(below.getType())
                || player.isInWater()
                || player.isUnderWater();
    }

    private boolean isTouchingLava(Player player) {
        Block feet = player.getLocation().getBlock();
        Block below = player.getLocation().clone().subtract(0, 0.2, 0).getBlock();

        return isLavaBlock(feet.getType())
                || isLavaBlock(below.getType());
    }

    private boolean isWaterBlock(Material type) {
        return type == Material.WATER || type == Material.BUBBLE_COLUMN;
    }

    private boolean isLavaBlock(Material type) {
        return type == Material.LAVA;
    }

    private void burnOneConsumedItem(Player player, EquipmentSlot hand, ItemStack consumed) {
        ItemStack handItem;

        if (hand == EquipmentSlot.OFF_HAND) {
            handItem = player.getInventory().getItemInOffHand();
            removeOneIfSimilar(player.getInventory().getItemInOffHand(), player, true);
        } else {
            handItem = player.getInventory().getItemInMainHand();
            removeOneIfSimilar(player.getInventory().getItemInMainHand(), player, false);
        }
    }

    private void removeOneIfSimilar(ItemStack stack, Player player, boolean offHand) {
        if (stack == null || stack.getType() == Material.AIR) return;

        int amount = stack.getAmount();
        if (amount <= 1) {
            if (offHand) {
                player.getInventory().setItemInOffHand(null);
            } else {
                player.getInventory().setItemInMainHand(null);
            }
        } else {
            stack.setAmount(amount - 1);
            if (offHand) {
                player.getInventory().setItemInOffHand(stack);
            } else {
                player.getInventory().setItemInMainHand(stack);
            }
        }
    }
}