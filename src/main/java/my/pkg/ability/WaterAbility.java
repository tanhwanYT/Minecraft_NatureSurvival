package my.pkg.ability;

import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

public class WaterAbility implements Ability {

    private final JavaPlugin plugin;

    private static final int MAX_AIR = 300;
    private static final int AIR_RECOVER_IN_WATER = 60;   // 물속에서 1초당 회복량
    private static final int AIR_LOSS_OUTSIDE = 20;       // 물밖에서 1초당 감소량

    public WaterAbility(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public String name() {
        return "물";
    }

    @Override
    public void onGrant(Player player) {
        player.setRemainingAir(MAX_AIR);
        applyState(player);
    }

    @Override
    public void onRemove(Player player) {
        clearWaterOnlyBuffs(player);
        player.setRemainingAir(player.getMaximumAir());
    }

    @Override
    public void onTick(Player player) {
        applyState(player);
    }

    @Override
    public void onMove(Player player, PlayerMoveEvent event) {
        applyState(player);
    }

    @Override
    public void onBreakBlock(Player player, BlockBreakEvent event) {
        if (!isInWater(player)) return;

        player.addPotionEffect(new PotionEffect(
                PotionEffectType.HASTE,
                40,
                1,
                false,
                false,
                false
        ));
    }

    private void applyState(Player player) {
        boolean inWater = isInWater(player);

        if (inWater) {
            applyWaterBuffs(player);

            int nextAir = Math.min(player.getMaximumAir(), player.getRemainingAir() + AIR_RECOVER_IN_WATER);
            player.setRemainingAir(nextAir);
            player.sendActionBar("§b[물] 물속에서 호흡 중");
        } else {
            clearWaterOnlyBuffs(player);

            int nextAir = player.getRemainingAir() - AIR_LOSS_OUTSIDE;
            player.setRemainingAir(Math.max(-20, nextAir));
            player.sendActionBar("§c[물] 물 밖에서는 숨을 쉴 수 없습니다!");
        }
    }

    private void applyWaterBuffs(Player player) {
        player.addPotionEffect(new PotionEffect(
                PotionEffectType.WATER_BREATHING,
                40,
                0,
                false,
                false,
                false
        ));

        player.addPotionEffect(new PotionEffect(
                PotionEffectType.DOLPHINS_GRACE,
                40,
                0,
                false,
                false,
                false
        ));

        player.addPotionEffect(new PotionEffect(
                PotionEffectType.STRENGTH,
                40,
                0,
                false,
                false,
                false
        ));

        player.addPotionEffect(new PotionEffect(
                PotionEffectType.RESISTANCE,
                40,
                0,
                false,
                false,
                false
        ));

        player.addPotionEffect(new PotionEffect(
                PotionEffectType.HASTE,
                40,
                1,
                false,
                false,
                false
        ));
    }

    private void clearWaterOnlyBuffs(Player player) {
        player.removePotionEffect(PotionEffectType.WATER_BREATHING);
        player.removePotionEffect(PotionEffectType.DOLPHINS_GRACE);
        player.removePotionEffect(PotionEffectType.STRENGTH);
        player.removePotionEffect(PotionEffectType.RESISTANCE);
        player.removePotionEffect(PotionEffectType.HASTE);
    }

    private boolean isInWater(Player player) {
        Block feet = player.getLocation().getBlock();
        Block head = player.getEyeLocation().getBlock();
        Block below = player.getLocation().clone().subtract(0, 0.2, 0).getBlock();

        return isWater(feet.getType())
                || isWater(head.getType())
                || isWater(below.getType())
                || player.isInWater()
                || player.isUnderWater();
    }

    private boolean isWater(Material material) {
        return material == Material.WATER || material == Material.BUBBLE_COLUMN;
    }
}