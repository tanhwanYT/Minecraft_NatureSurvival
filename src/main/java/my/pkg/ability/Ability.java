package my.pkg.ability;

import org.bukkit.entity.Player;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityPickupItemEvent;
import org.bukkit.event.inventory.CraftItemEvent;
import org.bukkit.event.player.PlayerItemConsumeEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerToggleSneakEvent;

public interface Ability {
    String name();

    default void onGrant(Player player) {}
    default void onRemove(Player player) {}

    default void onTick(Player player) {}

    default void onAttack(Player player, EntityDamageByEntityEvent event) {}
    default void onDamaged(Player player, EntityDamageEvent event) {}
    default void onMove(Player player, PlayerMoveEvent event) {}
    default void onBreakBlock(Player player, BlockBreakEvent event) {}
    default void onConsume(Player player, PlayerItemConsumeEvent event) {}
    default void onCraft(Player player, CraftItemEvent event) {}
    default void onSneakToggle(Player player, PlayerToggleSneakEvent event) {}
    default void onPickup(Player player, EntityPickupItemEvent event) {}
}
