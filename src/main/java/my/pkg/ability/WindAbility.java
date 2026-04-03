package my.pkg.ability;

import org.bukkit.GameMode;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerToggleSneakEvent;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.util.Vector;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class WindAbility implements Ability {

    private final JavaPlugin plugin;

    // 쉬프트 시작 시간
    private final Map<UUID, Long> sneakStartTime = new HashMap<>();

    // 마지막 비행 상승 시간
    private final Map<UUID, Long> lastFlyBoostTime = new HashMap<>();

    private static final double DEFAULT_MAX_HEALTH = 20.0;
    private static final double WIND_MAX_HEALTH = 16.0; // 2칸 감소

    private static final long FLY_CHARGE_MS = 3000L;      // 3초 이상 쉬프트 유지 시 발동
    private static final long FLY_BOOST_COOLDOWN_MS = 700L;
    private static final double FLY_UP_POWER = 0.75;
    private static final double FLY_FORWARD_POWER = 0.6;

    public WindAbility(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public String name() {
        return "바람";
    }

    @Override
    public void onGrant(Player player) {
        applyMaxHealthPenalty(player);
        applyPassiveBuff(player);
    }

    @Override
    public void onRemove(Player player) {
        resetMaxHealth(player);
        sneakStartTime.remove(player.getUniqueId());
        lastFlyBoostTime.remove(player.getUniqueId());
    }

    @Override
    public void onTick(Player player) {
        applyPassiveBuff(player);
        handleFlightCharge(player);
    }

    @Override
    public void onDamaged(Player player, EntityDamageEvent event) {
        if (event.getCause() == EntityDamageEvent.DamageCause.FALL) {
            event.setCancelled(true);
        }
    }

    @Override
    public void onMove(Player player, PlayerMoveEvent event) {
        handleFlightCharge(player);
    }

    @Override
    public void onSneakToggle(Player player, PlayerToggleSneakEvent event) {
        UUID uuid = player.getUniqueId();

        if (event.isSneaking()) {
            sneakStartTime.put(uuid, System.currentTimeMillis());
        } else {
            sneakStartTime.remove(uuid);
        }
    }

    private void applyPassiveBuff(Player player) {
        player.addPotionEffect(new PotionEffect(
                PotionEffectType.SPEED,
                40,
                0,
                false,
                false,
                false
        ));
    }

    private void applyMaxHealthPenalty(Player player) {
        AttributeInstance attr = player.getAttribute(Attribute.MAX_HEALTH);
        if (attr == null) return;

        if (Math.abs(attr.getBaseValue() - WIND_MAX_HEALTH) > 0.0001) {
            attr.setBaseValue(WIND_MAX_HEALTH);
        }

        if (player.getHealth() > WIND_MAX_HEALTH) {
            player.setHealth(WIND_MAX_HEALTH);
        }
    }

    private void resetMaxHealth(Player player) {
        AttributeInstance attr = player.getAttribute(Attribute.MAX_HEALTH);
        if (attr == null) return;

        if (Math.abs(attr.getBaseValue() - DEFAULT_MAX_HEALTH) > 0.0001) {
            attr.setBaseValue(DEFAULT_MAX_HEALTH);
        }

        if (player.getHealth() > DEFAULT_MAX_HEALTH) {
            player.setHealth(DEFAULT_MAX_HEALTH);
        }
    }

    private void handleFlightCharge(Player player) {
        if (!player.isSneaking()) return;
        if (player.isFlying()) return;
        if (player.getGameMode() == GameMode.SPECTATOR) return;
        if (player.isInsideVehicle()) return;

        UUID uuid = player.getUniqueId();
        Long start = sneakStartTime.get(uuid);
        if (start == null) {
            sneakStartTime.put(uuid, System.currentTimeMillis());
            return;
        }

        long now = System.currentTimeMillis();
        long held = now - start;
        long lastBoost = lastFlyBoostTime.getOrDefault(uuid, 0L);

        if (held < FLY_CHARGE_MS) return;
        if (now - lastBoost < FLY_BOOST_COOLDOWN_MS) return;

        // 공중/지상 모두 사용 가능하지만, 너무 남발되지 않게 속도 체크
        Vector direction = player.getLocation().getDirection().normalize();

        Vector boost = direction.multiply(FLY_FORWARD_POWER);
        boost.setY(FLY_UP_POWER);

        Vector current = player.getVelocity();
        Vector result = current.add(boost);

        // 상승 속도 상한
        if (result.getY() > 1.0) {
            result.setY(1.0);
        }

        player.setVelocity(result);
        player.setFallDistance(0f);

        lastFlyBoostTime.put(uuid, now);
    }
}