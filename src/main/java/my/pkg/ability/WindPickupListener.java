package my.pkg.ability;

import my.pkg.AbilitySystem;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.event.Listener;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

public class WindPickupListener implements Listener {

    private final JavaPlugin plugin;
    private final AbilitySystem abilitySystem;

    private static final double PICKUP_RANGE = 3.5; // 추가 흡입 범위
    private static final double PULL_STRENGTH = 0.18;

    public WindPickupListener(JavaPlugin plugin, AbilitySystem abilitySystem) {
        this.plugin = plugin;
        this.abilitySystem = abilitySystem;
        startTask();
    }

    private void startTask() {
        new BukkitRunnable() {
            @Override
            public void run() {
                for (Player player : Bukkit.getOnlinePlayers()) {
                    if (!(abilitySystem.getAbility(player) instanceof WindAbility)) continue;
                    pullNearbyItems(player);
                }
            }
        }.runTaskTimer(plugin, 0L, 5L);
    }

    private void pullNearbyItems(Player player) {
        Location playerLoc = player.getLocation().add(0, 0.6, 0);

        for (Item item : player.getWorld().getEntitiesByClass(Item.class)) {
            if (!item.getWorld().equals(player.getWorld())) continue;
            if (item.isDead() || !item.isValid()) continue;

            double distance = item.getLocation().distance(playerLoc);
            if (distance > PICKUP_RANGE || distance < 0.3) continue;

            Vector direction = playerLoc.toVector().subtract(item.getLocation().toVector()).normalize();
            item.setVelocity(direction.multiply(PULL_STRENGTH));
        }
    }
}