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

public class WaterLockListener implements Listener {

    private final JavaPlugin plugin;
    private final AbilitySystem abilitySystem;

    public WaterLockListener(JavaPlugin plugin, AbilitySystem abilitySystem) {
        this.plugin = plugin;
        this.abilitySystem = abilitySystem;
    }

    private WaterAbility getWaterAbility(Player player) {
        if (abilitySystem.getAbility(player) instanceof WaterAbility waterAbility) {
            return waterAbility;
        }
        return null;
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) return;

        WaterAbility water = getWaterAbility(player);
        if (water == null) return;

        // 플레이어 인벤토리의 30~35 슬롯 직접 클릭 금지
        if (event.getClickedInventory() instanceof PlayerInventory) {
            int slot = event.getSlot();
            if (slot >= 30 && slot <= 35) {
                event.setCancelled(true);
                return;
            }
        }

        ItemStack current = event.getCurrentItem();
        ItemStack cursor = event.getCursor();

        // 잠금 유리 자체 조작 금지
        if (water.isLockedSlotItem(current) || water.isLockedSlotItem(cursor)) {
            event.setCancelled(true);
            return;
        }

        // 숫자키 스왑 방지
        if (event.getClick().isKeyboardClick()
                && event.getClickedInventory() instanceof PlayerInventory) {
            int slot = event.getSlot();
            if (slot >= 30 && slot <= 35) {
                event.setCancelled(true);
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onInventoryDrag(InventoryDragEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) return;

        WaterAbility water = getWaterAbility(player);
        if (water == null) return;

        for (int slot : event.getRawSlots()) {
            // 플레이어 인벤토리 30~35칸 막기
            if (slot >= 30 && slot <= 35) {
                event.setCancelled(true);
                return;
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onDrop(PlayerDropItemEvent event) {
        Player player = event.getPlayer();

        WaterAbility water = getWaterAbility(player);
        if (water == null) return;

        if (water.isLockedSlotItem(event.getItemDrop().getItemStack())) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onPickup(EntityPickupItemEvent event) {
        if (!(event.getEntity() instanceof Player player)) return;

        WaterAbility water = getWaterAbility(player);
        if (water == null) return;

        PlayerInventory inv = player.getInventory();

        // 잠금 슬롯에 아이템이 들어갈 여지 없게 보정
        for (int slot = 30; slot <= 35; slot++) {
            ItemStack item = inv.getItem(slot);
            if (!water.isLockedSlotItem(item)) {
                inv.setItem(slot, water.createLockedSlotItem());
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onDeath(PlayerDeathEvent event) {
        Player player = event.getEntity();

        WaterAbility water = getWaterAbility(player);
        if (water == null) return;

        event.getDrops().removeIf(water::isLockedSlotItem);
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onRespawn(PlayerRespawnEvent event) {
        Player player = event.getPlayer();

        WaterAbility water = getWaterAbility(player);
        if (water == null) return;

        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            WaterAbility again = getWaterAbility(player);
            if (again != null) {
                again.applyLockedSlots(player);
            }
        }, 1L);
    }
}