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

public class MetalLockListener implements Listener {

    private final JavaPlugin plugin;
    private final AbilitySystem abilitySystem;

    public MetalLockListener(JavaPlugin plugin, AbilitySystem abilitySystem) {
        this.plugin = plugin;
        this.abilitySystem = abilitySystem;
    }

    private MetalAbility getMetalAbility(Player player) {
        if (abilitySystem.getAbility(player) instanceof MetalAbility metalAbility) {
            return metalAbility;
        }
        return null;
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) return;

        MetalAbility metal = getMetalAbility(player);
        if (metal == null) return;

        if (event.getClickedInventory() instanceof PlayerInventory) {
            int slot = event.getSlot();
            if (slot >= 28 && slot <= 35) {
                event.setCancelled(true);
                return;
            }
        }

        ItemStack current = event.getCurrentItem();
        ItemStack cursor = event.getCursor();

        if (metal.isLockedSlotItem(current) || metal.isLockedSlotItem(cursor)) {
            event.setCancelled(true);
            return;
        }

        if (event.getClick().isKeyboardClick()
                && event.getClickedInventory() instanceof PlayerInventory) {
            int slot = event.getSlot();
            if (slot >= 28 && slot <= 35) {
                event.setCancelled(true);
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onInventoryDrag(InventoryDragEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) return;

        MetalAbility metal = getMetalAbility(player);
        if (metal == null) return;

        for (int slot : event.getRawSlots()) {
            if (slot >= 28 && slot <= 35) {
                event.setCancelled(true);
                return;
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onDrop(PlayerDropItemEvent event) {
        Player player = event.getPlayer();

        MetalAbility metal = getMetalAbility(player);
        if (metal == null) return;

        if (metal.isLockedSlotItem(event.getItemDrop().getItemStack())) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onPickup(EntityPickupItemEvent event) {
        if (!(event.getEntity() instanceof Player player)) return;

        MetalAbility metal = getMetalAbility(player);
        if (metal == null) return;

        PlayerInventory inv = player.getInventory();
        for (int slot = 28; slot <= 35; slot++) {
            ItemStack item = inv.getItem(slot);
            if (!metal.isLockedSlotItem(item)) {
                inv.setItem(slot, metal.createLockedSlotItem());
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onDeath(PlayerDeathEvent event) {
        Player player = event.getEntity();

        MetalAbility metal = getMetalAbility(player);
        if (metal == null) return;

        event.getDrops().removeIf(metal::isLockedSlotItem);
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onRespawn(PlayerRespawnEvent event) {
        Player player = event.getPlayer();

        MetalAbility metal = getMetalAbility(player);
        if (metal == null) return;

        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            MetalAbility again = getMetalAbility(player);
            if (again != null) {
                again.applyLockedSlots(player);
            }
        }, 1L);
    }
}