package my.pkg.ability;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.block.Block;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

public class ElectricAbility implements Ability {

    private final JavaPlugin plugin;

    // 마지막 전투 시각
    private final Map<UUID, Long> lastCombatTime = new HashMap<>();

    // 물 대미지 쿨타임
    private final Map<UUID, Long> lastWaterDamageTime = new HashMap<>();

    private final Set<UUID> chainDamageVictims = new HashSet<>();

    // 설정값
    private static final double CHAIN_DAMAGE = 3.0;           // 1.5칸
    private static final double WATER_DAMAGE = 4.0;           // 2칸
    private static final long WATER_DAMAGE_COOLDOWN_MS = 1000L;
    private static final long ABSORPTION_DELAY_MS = 8000L;    // 8초 비전투시 흡수 부여
    private static final double ABSORPTION_AMOUNT = 6.0;      // 노란 체력 3칸
    private static final double CHAIN_RADIUS = 4.0;
    private static final double LIGHTNING_CHANCE_PER_TICK = 0.03; // 3%
    private static final float EXHAUSTION_PER_TICK = 0.12f;   // 허기 빨리 닳게

    public ElectricAbility(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public String name() {
        return "전기";
    }

    @Override
    public void onGrant(Player player) {
        applyPassiveBuffs(player);
    }

    @Override
    public void onRemove(Player player) {
        lastCombatTime.remove(player.getUniqueId());
        lastWaterDamageTime.remove(player.getUniqueId());
        player.setAbsorptionAmount(0);
    }

    @Override
    public void onTick(Player player) {
        applyPassiveBuffs(player);
        handleWaterPenalty(player);
        handleAbsorption(player);
        handleExtraHunger(player);
        handleRandomLightningEffect(player);
    }

    @Override
    public void onAttack(Player player, EntityDamageByEntityEvent event) {
        if (!(event.getEntity() instanceof LivingEntity mainTarget)) return;

        // 추가: 이미 연쇄 피해로 들어온 이벤트면 다시 연쇄 발동 금지
        if (chainDamageVictims.contains(mainTarget.getUniqueId())) {
            return;
        }

        long now = System.currentTimeMillis();
        lastCombatTime.put(player.getUniqueId(), now);

        Location center = mainTarget.getLocation().add(0, 1.0, 0);

        for (Entity entity : mainTarget.getNearbyEntities(CHAIN_RADIUS, CHAIN_RADIUS, CHAIN_RADIUS)) {
            if (!(entity instanceof LivingEntity nearby)) continue;
            if (nearby.equals(mainTarget)) continue;
            if (nearby.equals(player)) continue;
            if (nearby.isDead()) continue;

            UUID victimId = nearby.getUniqueId();

            try {
                chainDamageVictims.add(victimId);
                nearby.damage(CHAIN_DAMAGE, player);
            } finally {
                chainDamageVictims.remove(victimId);
            }

            Location loc = nearby.getLocation().add(0, 1.0, 0);
            nearby.getWorld().spawnParticle(Particle.ELECTRIC_SPARK, loc, 18, 0.25, 0.35, 0.25, 0.02);
            nearby.getWorld().spawnParticle(Particle.CRIT, loc, 6, 0.2, 0.2, 0.2, 0.01);
        }

        mainTarget.getWorld().spawnParticle(Particle.ELECTRIC_SPARK, center, 22, 0.3, 0.4, 0.3, 0.03);
        mainTarget.getWorld().playSound(center, Sound.ENTITY_LIGHTNING_BOLT_IMPACT, 0.35f, 1.7f);
    }

    @Override
    public void onDamaged(Player player, EntityDamageEvent event) {
        lastCombatTime.put(player.getUniqueId(), System.currentTimeMillis());
    }


    private void applyPassiveBuffs(Player player) {
        player.addPotionEffect(new PotionEffect(
                PotionEffectType.SPEED,
                40,
                0,
                false,
                false,
                false
        ));

        player.addPotionEffect(new PotionEffect(
                PotionEffectType.NIGHT_VISION,
                220,
                0,
                false,
                false,
                false
        ));
    }

    private void handleAbsorption(Player player) {
        long now = System.currentTimeMillis();
        long lastCombat = lastCombatTime.getOrDefault(player.getUniqueId(), 0L);

        if (now - lastCombat >= ABSORPTION_DELAY_MS) {
            if (player.getAbsorptionAmount() < ABSORPTION_AMOUNT) {
                player.setAbsorptionAmount(ABSORPTION_AMOUNT);
            }
        }
    }

    private void handleWaterPenalty(Player player) {
        if (!isTouchingWater(player)) return;

        long now = System.currentTimeMillis();
        long last = lastWaterDamageTime.getOrDefault(player.getUniqueId(), 0L);

        if (now - last < WATER_DAMAGE_COOLDOWN_MS) return;

        lastWaterDamageTime.put(player.getUniqueId(), now);
        lastCombatTime.put(player.getUniqueId(), now); // 물에 맞아도 비전투 판정 끊기게

        player.damage(WATER_DAMAGE);
        player.sendActionBar("§b전기 원소가 물에 의해 크게 약화됩니다!");
        player.getWorld().spawnParticle(
                Particle.ELECTRIC_SPARK,
                player.getLocation().add(0, 1.0, 0),
                30, 0.35, 0.5, 0.35, 0.05
        );
        player.getWorld().playSound(player.getLocation(), Sound.ENTITY_LIGHTNING_BOLT_THUNDER, 0.25f, 2.0f);
    }

    private void handleExtraHunger(Player player) {
        // 배고픔이 빨리 닳게 약간의 exhaustion 누적
        if (player.getFoodLevel() > 0) {
            player.setExhaustion(player.getExhaustion() + EXHAUSTION_PER_TICK);
        }
    }

    private void handleRandomLightningEffect(Player player) {
        if (ThreadLocalRandom.current().nextDouble() >= LIGHTNING_CHANCE_PER_TICK) return;

        Location base = player.getLocation();
        double offsetX = ThreadLocalRandom.current().nextDouble(-6.0, 6.0);
        double offsetZ = ThreadLocalRandom.current().nextDouble(-6.0, 6.0);
        Location strikeLoc = base.clone().add(offsetX, 0, offsetZ);
        strikeLoc.setY(player.getWorld().getHighestBlockYAt(strikeLoc) + 1);

        // 실제 피해 없는 번개 연출
        player.getWorld().strikeLightningEffect(strikeLoc);
    }

    private boolean isTouchingWater(Player player) {
        Block feet = player.getLocation().getBlock();
        Block head = player.getEyeLocation().getBlock();
        Block below = player.getLocation().clone().subtract(0, 0.2, 0).getBlock();

        return isWaterBlock(feet.getType())
                || isWaterBlock(head.getType())
                || isWaterBlock(below.getType())
                || player.isInWater()
                || player.isUnderWater();
    }

    private boolean isWaterBlock(Material type) {
        return type == Material.WATER || type == Material.BUBBLE_COLUMN;
    }
}