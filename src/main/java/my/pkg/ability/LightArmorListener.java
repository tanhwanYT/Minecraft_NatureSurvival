package my.pkg.ability;

import my.pkg.AbilitySystem;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;

public class LightArmorListener implements Listener {

    private final AbilitySystem abilitySystem;

    public LightArmorListener(AbilitySystem abilitySystem) {
        this.abilitySystem = abilitySystem;
    }

    private boolean isLightPlayer(Player player) {
        return abilitySystem.getAbility(player) instanceof LightAbility;
    }

    private boolean isChestplate(ItemStack item) {
        if (item == null) return false;
        Material type = item.getType();
        String name = type.name();
        return name.endsWith("_CHESTPLATE");
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) return;
        if (!isLightPlayer(player)) return;

        // 갑옷 슬롯 중 흉갑 슬롯
        if (event.getSlotType() == org.bukkit.event.inventory.InventoryType.SlotType.ARMOR
                && event.getSlot() == 38) {
            ItemStack cursor = event.getCursor();
            if (isChestplate(cursor)) {
                event.setCancelled(true);
                player.sendActionBar("§e[빛] 흉갑은 착용할 수 없습니다.");
                return;
            }
        }

        ItemStack current = event.getCurrentItem();
        ItemStack cursor = event.getCursor();

        if (isChestplate(cursor) && event.getClickedInventory() instanceof PlayerInventory) {
            if (event.getSlot() == 38) {
                event.setCancelled(true);
                player.sendActionBar("§e[빛] 흉갑은 착용할 수 없습니다.");
                return;
            }
        }

        if (event.getClick().isShiftClick() && isChestplate(current)) {
            event.setCancelled(true);
            player.sendActionBar("§e[빛] 흉갑은 착용할 수 없습니다.");
        }
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onInventoryDrag(InventoryDragEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) return;
        if (!isLightPlayer(player)) return;

        ItemStack oldCursor = event.getOldCursor();
        if (!isChestplate(oldCursor)) return;

        for (int slot : event.getRawSlots()) {
            if (slot == 6) { // 플레이어 인벤 GUI 상 흉갑 슬롯
                event.setCancelled(true);
                player.sendActionBar("§e[빛] 흉갑은 착용할 수 없습니다.");
                return;
            }
        }
    }
}