package my.pkg.ability;

import my.pkg.AbilitySystem;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class MetalKeepInventoryListener implements Listener {

    private final JavaPlugin plugin;
    private final AbilitySystem abilitySystem;

    private final Map<UUID, ItemStack[]> savedContents = new HashMap<>();
    private final Map<UUID, ItemStack[]> savedArmor = new HashMap<>();
    private final Map<UUID, ItemStack[]> savedExtra = new HashMap<>();

    public MetalKeepInventoryListener(JavaPlugin plugin, AbilitySystem abilitySystem) {
        this.plugin = plugin;
        this.abilitySystem = abilitySystem;
    }

    private boolean isMetal(Player player) {
        return abilitySystem.getAbility(player) instanceof MetalAbility;
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onDeath(PlayerDeathEvent event) {
        Player player = event.getEntity();
        if (!isMetal(player)) return;

        savedContents.put(player.getUniqueId(), player.getInventory().getContents().clone());
        savedArmor.put(player.getUniqueId(), player.getInventory().getArmorContents().clone());
        savedExtra.put(player.getUniqueId(), player.getInventory().getExtraContents().clone());

        event.getDrops().clear();
        event.setKeepInventory(true);
        event.setKeepLevel(true);
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onRespawn(PlayerRespawnEvent event) {
        Player player = event.getPlayer();
        if (!isMetal(player)) return;

        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            UUID uuid = player.getUniqueId();

            ItemStack[] contents = savedContents.remove(uuid);
            ItemStack[] armor = savedArmor.remove(uuid);
            ItemStack[] extra = savedExtra.remove(uuid);

            if (contents != null) {
                player.getInventory().setContents(contents);
            }
            if (armor != null) {
                player.getInventory().setArmorContents(armor);
            }
            if (extra != null) {
                player.getInventory().setExtraContents(extra);
            }

            player.updateInventory();
        }, 1L);
    }
}