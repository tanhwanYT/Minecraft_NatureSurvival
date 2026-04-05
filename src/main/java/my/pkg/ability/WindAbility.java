package my.pkg.ability;

import org.bukkit.GameMode;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDamageEvent;
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

    // 충전 완료 상태
    private final Map<UUID, Boolean> chargedState = new HashMap<>();

    private static final double DEFAULT_MAX_HEALTH = 20.0;
    private static final double WIND_MAX_HEALTH = 16.0; // 2칸 감소

    private static final long FLY_CHARGE_MS = 3000L;   // 3초 충전
    private static final double FLY_UP_POWER = 1.2;    // 수직 발사 세기

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
        chargedState.remove(player.getUniqueId());
    }

    @Override
    public void onTick(Player player) {
        applyPassiveBuff(player);
        updateChargeBar(player);
    }

    @Override
    public void onDamaged(Player player, EntityDamageEvent event) {
        if (event.getCause() == EntityDamageEvent.DamageCause.FALL) {
            event.setCancelled(true);
        }
    }

    @Override
    public void onSneakToggle(Player player, PlayerToggleSneakEvent event) {
        UUID uuid = player.getUniqueId();

        if (event.isSneaking()) {
            if (!canUseWindJump(player)) return;

            sneakStartTime.put(uuid, System.currentTimeMillis());
            chargedState.put(uuid, false);
            player.sendActionBar("§f[바람] §7기류 충전 시작...");
            return;
        }

        // 쉬프트를 뗄 때
        Long start = sneakStartTime.get(uuid);
        boolean charged = chargedState.getOrDefault(uuid, false);

        sneakStartTime.remove(uuid);
        chargedState.remove(uuid);

        if (start == null) return;
        if (!canUseWindJump(player)) return;

        if (!charged) {
            player.sendActionBar("§c[바람] 충전이 완료되지 않았습니다.");
            return;
        }

        launchUp(player);
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

    private boolean canUseWindJump(Player player) {
        if (player.isFlying()) return false;
        if (player.getGameMode() == GameMode.SPECTATOR) return false;
        if (player.isInsideVehicle()) return false;
        return true;
    }

    private void updateChargeBar(Player player) {
        if (!player.isSneaking()) return;
        if (!canUseWindJump(player)) return;

        UUID uuid = player.getUniqueId();
        Long start = sneakStartTime.get(uuid);
        if (start == null) return;

        long now = System.currentTimeMillis();
        long held = now - start;

        if (held >= FLY_CHARGE_MS) {
            if (!chargedState.getOrDefault(uuid, false)) {
                chargedState.put(uuid, true);
                player.sendMessage("§b[바람] 상승 기류 준비 완료! 쉬프트를 떼면 날아오릅니다.");
                player.sendActionBar("§b[바람] 준비 완료! §f쉬프트를 떼세요");
            } else {
                player.sendActionBar("§b[바람] 준비 완료! §f쉬프트를 떼세요");
            }
            return;
        }

        double progress = (double) held / FLY_CHARGE_MS;
        int bars = (int) Math.floor(progress * 10);

        StringBuilder gauge = new StringBuilder("§f[바람] §7충전: ");
        for (int i = 0; i < 10; i++) {
            if (i < bars) {
                gauge.append("§b■");
            } else {
                gauge.append("§7■");
            }
        }

        int percent = (int) (progress * 100);
        gauge.append(" §f").append(percent).append("%");

        player.sendActionBar(gauge.toString());
    }

    private void launchUp(Player player) {
        Vector current = player.getVelocity();

        // X, Z는 유지하거나 0으로 만들 수 있는데
        // "위로만" 원하면 완전히 0으로 고정
        Vector launch = new Vector(0, FLY_UP_POWER, 0);

        // 기존 아래로 떨어지는 속도는 제거
        if (current.getY() > 0) {
            launch.setY(Math.max(FLY_UP_POWER, current.getY()));
        }

        player.setVelocity(launch);
        player.setFallDistance(0f);

        player.sendMessage("§b[바람] 상승 기류를 타고 날아올랐습니다!");
        player.sendActionBar("§b[바람] 상승!");
    }
}