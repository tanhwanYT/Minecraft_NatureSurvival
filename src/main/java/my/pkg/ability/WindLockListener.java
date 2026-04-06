package my.pkg.ability;

import my.pkg.AbilitySystem;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityPickupItemEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.plugin.java.JavaPlugin;

public class WindLockListener implements Listener {

    private final JavaPlugin plugin;
    private final AbilitySystem abilitySystem;

    public WindLockListener(JavaPlugin plugin, AbilitySystem abilitySystem) {
        this.plugin = plugin;
        this.abilitySystem = abilitySystem;
    }

    private WindAbility getWindAbility(Player player) {
        if (abilitySystem.getAbility(player) instanceof WindAbility windAbility) {
            return windAbility;
        }
        return null;
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) return;

        WindAbility wind = getWindAbility(player);
        if (wind == null) return;

        if (event.getClickedInventory() instanceof PlayerInventory) {
            int slot = event.getSlot();
            if (slot >= 27 && slot <= 35) {
                event.setCancelled(true);
                return;
            }
        }

        ItemStack current = event.getCurrentItem();
        ItemStack cursor = event.getCursor();

        if (wind.isLockedSlotItem(current) || wind.isLockedSlotItem(cursor)) {
            event.setCancelled(true);
            return;
        }

        if (event.getClick().isKeyboardClick()
                && event.getClickedInventory() instanceof PlayerInventory) {
            int slot = event.getSlot();
            if (slot >= 27 && slot <= 35) {
                event.setCancelled(true);
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onInventoryDrag(InventoryDragEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) return;

        WindAbility wind = getWindAbility(player);
        if (wind == null) return;

        for (int slot : event.getRawSlots()) {
            if (slot >= 27 && slot <= 35) {
                event.setCancelled(true);
                return;
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onDrop(PlayerDropItemEvent event) {
        Player player = event.getPlayer();

        WindAbility wind = getWindAbility(player);
        if (wind == null) return;

        if (wind.isLockedSlotItem(event.getItemDrop().getItemStack())) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onPickup(EntityPickupItemEvent event) {
        if (!(event.getEntity() instanceof Player player)) return;

        WindAbility wind = getWindAbility(player);
        if (wind == null) return;

        PlayerInventory inv = player.getInventory();
        for (int slot = 27; slot <= 35; slot++) {
            ItemStack item = inv.getItem(slot);
            if (!wind.isLockedSlotItem(item)) {
                inv.setItem(slot, wind.createLockedSlotItem());
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onDeath(PlayerDeathEvent event) {
        Player player = event.getEntity();

        WindAbility wind = getWindAbility(player);
        if (wind == null) return;

        event.getDrops().removeIf(wind::isLockedSlotItem);
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onRespawn(PlayerRespawnEvent event) {
        Player player = event.getPlayer();

        WindAbility wind = getWindAbility(player);
        if (wind == null) return;

        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            WindAbility again = getWindAbility(player);
            if (again != null) {
                again.applyLockedSlots(player);
            }
        }, 1L);
    }
}