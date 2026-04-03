package my.pkg.ability;

import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.block.Block;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.util.Vector;

import java.util.Collection;

public class LightAbility implements Ability {

    private final JavaPlugin plugin;

    private static final int GLOW_DURATION_TICKS = 40;
    private static final double SHARE_RADIUS = 6.0;
    private static final double LOOK_RANGE = 16.0;
    private static final double LOOK_DOT_THRESHOLD = 0.965; // 높을수록 더 정확히 바라봐야 함

    public LightAbility(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public String name() {
        return "빛";
    }

    @Override
    public void onGrant(Player player) {
        removeChestplate(player);
        applyBuffs(player);
    }

    @Override
    public void onRemove(Player player) {
        player.removePotionEffect(PotionEffectType.REGENERATION);
        player.removePotionEffect(PotionEffectType.SATURATION);
    }

    @Override
    public void onTick(Player player) {
        removeChestplate(player);
        applyBuffs(player);
        applyLookingGlow(player);
        shareRegenToNearbyPlayers(player);
        spawnLightParticles(player);
    }

    @Override
    public void onMove(Player player, PlayerMoveEvent event) {
        removeChestplate(player);
    }

    private void applyBuffs(Player player) {
        boolean dark = isInDarkness(player);

        if (dark) {
            player.addPotionEffect(new PotionEffect(
                    PotionEffectType.REGENERATION,
                    40,
                    0,
                    false,
                    false,
                    false
            ));
        } else {
            player.addPotionEffect(new PotionEffect(
                    PotionEffectType.REGENERATION,
                    40,
                    1,
                    false,
                    false,
                    false
            ));

            player.addPotionEffect(new PotionEffect(
                    PotionEffectType.SATURATION,
                    40,
                    0,
                    false,
                    false,
                    false
            ));
        }
    }

    private void applyLookingGlow(Player player) {
        Collection<Entity> nearby = player.getWorld().getNearbyEntities(
                player.getEyeLocation(),
                LOOK_RANGE, LOOK_RANGE, LOOK_RANGE
        );

        for (Entity entity : nearby) {
            if (!(entity instanceof LivingEntity living)) continue;
            if (living == player) continue;

            if (!isLookingAt(player, living, LOOK_RANGE)) continue;

            living.addPotionEffect(new PotionEffect(
                    PotionEffectType.GLOWING,
                    GLOW_DURATION_TICKS,
                    0,
                    false,
                    false,
                    false
            ));
        }
    }

    private void shareRegenToNearbyPlayers(Player player) {
        for (Player nearby : player.getWorld().getPlayers()) {
            boolean dark = isInDarkness(player);
            if (nearby == player) continue;
            if (nearby.getLocation().distanceSquared(player.getLocation()) > SHARE_RADIUS * SHARE_RADIUS) continue;

            nearby.addPotionEffect(new PotionEffect(
                    PotionEffectType.REGENERATION,
                    40,
                    dark ? 0 : 1,
                    false,
                    false,
                    false
            ));
        }
    }

    private boolean isLookingAt(Player player, LivingEntity target, double range) {
        Vector eye = player.getEyeLocation().toVector();
        Vector direction = player.getEyeLocation().getDirection().normalize();

        Vector toTarget = target.getEyeLocation().toVector().subtract(eye);
        double distance = toTarget.length();

        if (distance > range) return false;
        if (distance <= 0.001) return false;

        Vector toTargetDir = toTarget.normalize();

        // 얼마나 정면으로 보고 있는지
        double dot = direction.dot(toTargetDir);
        if (dot < LOOK_DOT_THRESHOLD) return false;

        // 벽 뒤에 있는 대상은 제외
        if (!player.hasLineOfSight(target)) return false;

        return true;
    }

    private boolean isInDarkness(Player player) {
        Block block = player.getLocation().getBlock();

        int blockLight = block.getLightFromBlocks();
        int skyLight = block.getLightFromSky();
        int totalLight = block.getLightLevel();

        return totalLight <= 7 && blockLight <= 7 && skyLight <= 7;
    }

    private void removeChestplate(Player player) {
        ItemStack chest = player.getInventory().getChestplate();
        if (chest == null || chest.getType() == Material.AIR) return;

        player.getInventory().setChestplate(null);

        var leftover = player.getInventory().addItem(chest);
        if (!leftover.isEmpty()) {
            leftover.values().forEach(item ->
                    player.getWorld().dropItemNaturally(player.getLocation(), item)
            );
        }

        player.sendActionBar("§e[빛] 흉갑은 착용할 수 없습니다.");
    }

    private void spawnLightParticles(Player player) {
        player.getWorld().spawnParticle(
                Particle.END_ROD,
                player.getLocation().add(0, 1.0, 0),
                2,
                0.3, 0.5, 0.3,
                0.01
        );
    }
}