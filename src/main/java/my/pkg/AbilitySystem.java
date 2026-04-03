package my.pkg;

import my.pkg.ability.Ability;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityPickupItemEvent;
import org.bukkit.event.inventory.CraftItemEvent;
import org.bukkit.event.player.PlayerItemConsumeEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.event.player.PlayerToggleSneakEvent;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.*;

public class AbilitySystem implements Listener {

    private final JavaPlugin plugin;

    // 플레이어가 현재 가진 능력
    private final Map<UUID, Ability> playerAbilities = new HashMap<>();

    // 등록된 능력 목록
    private final Map<String, Ability> registry = new LinkedHashMap<>();

    public AbilitySystem(JavaPlugin plugin) {
        this.plugin = plugin;
        startTickTask();
    }

    // =========================
    // 능력 등록
    // =========================
    public void registerAbility(Ability ability) {
        registry.put(ability.name().toLowerCase(Locale.ROOT), ability);
    }

    public Map<String, Ability> getRegisteredAbilityMap() {
        return Collections.unmodifiableMap(registry);
    }

    public Ability getRegisteredAbility(String name) {
        if (name == null) return null;
        return registry.get(name.toLowerCase(Locale.ROOT));
    }

    public Collection<Ability> getAllRegisteredAbilities() {
        return registry.values();
    }

    // =========================
    // 능력 부여 / 제거 / 조회
    // =========================
    public void grant(Player player, Ability ability) {
        if (player == null || ability == null) return;

        // 기존 능력 제거
        remove(player);

        playerAbilities.put(player.getUniqueId(), ability);
        ability.onGrant(player);

        player.sendMessage(ChatColor.GREEN + "[원소] " + ability.name() + ChatColor.WHITE + " 능력이 부여되었습니다.");
    }

    public void grant(Player player, String abilityName) {
        Ability ability = getRegisteredAbility(abilityName);
        if (ability == null) {
            player.sendMessage(ChatColor.RED + "존재하지 않는 능력입니다: " + abilityName);
            return;
        }
        grant(player, ability);
    }

    public void remove(Player player) {
        if (player == null) return;

        Ability old = playerAbilities.remove(player.getUniqueId());
        if (old != null) {
            old.onRemove(player);
        }

        resetPlayerBaseState(player);
    }

    public Ability getAbility(Player player) {
        if (player == null) return null;
        return playerAbilities.get(player.getUniqueId());
    }

    public boolean hasAbility(Player player) {
        return getAbility(player) != null;
    }

    public String getAbilityName(Player player) {
        Ability ability = getAbility(player);
        return ability == null ? "없음" : ability.name();
    }

    public Set<UUID> getAssignedPlayerIds() {
        return Collections.unmodifiableSet(playerAbilities.keySet());
    }

    @EventHandler
    public void onPickup(EntityPickupItemEvent event) {
        if (!(event.getEntity() instanceof Player player)) return;

        Ability ability = getAbility(player);
        if (ability == null) return;

        ability.onPickup(player, event);
    }

    // =========================
    // 랜덤 배정
    // =========================
    public void grantRandomUniqueToOnlinePlayers() {
        List<Player> players = new ArrayList<>(Bukkit.getOnlinePlayers());
        List<Ability> abilities = new ArrayList<>(registry.values());

        if (players.isEmpty()) return;

        if (abilities.size() < players.size()) {
            Bukkit.broadcastMessage(ChatColor.RED + "[원소] 플레이어 수보다 등록된 능력 수가 적습니다.");
            return;
        }

        Collections.shuffle(abilities);

        for (int i = 0; i < players.size(); i++) {
            grant(players.get(i), abilities.get(i));
        }
    }

    // =========================
    // 틱 처리
    // =========================
    private void startTickTask() {
        new BukkitRunnable() {
            @Override
            public void run() {
                for (Player player : Bukkit.getOnlinePlayers()) {
                    Ability ability = getAbility(player);
                    if (ability == null) continue;

                    try {
                        ability.onTick(player);
                    } catch (Exception e) {
                        plugin.getLogger().warning("[AbilitySystem] onTick 오류 - " + player.getName() + " / " + ability.name());
                        e.printStackTrace();
                    }
                }
            }
        }.runTaskTimer(plugin, 0L, 20L); // 1초마다
    }

    // =========================
    // 플레이어 초기화
    // =========================
    public void resetAllPlayers() {
        for (Player player : Bukkit.getOnlinePlayers()) {
            remove(player);
        }
    }

    private void resetPlayerBaseState(Player player) {
        // 능력 제거 시 남아있을 수 있는 기본 상태 정리
        player.setFireTicks(0);
        player.setFreezeTicks(0);
        player.setFoodLevel(Math.max(player.getFoodLevel(), 20));
        player.setSaturation(Math.max(player.getSaturation(), 5f));
        player.setAbsorptionAmount(0);

        var attrHealth = player.getAttribute(Attribute.MAX_HEALTH);
        if (attrHealth != null && attrHealth.getBaseValue() != 20.0) {
            attrHealth.setBaseValue(20.0);
            if (player.getHealth() > 20.0) {
                player.setHealth(20.0);
            }
        }

        var attrMove = player.getAttribute(Attribute.MOVEMENT_SPEED);
        if (attrMove != null) {
            attrMove.setBaseValue(0.1);
        }

        var attrAttackSpeed = player.getAttribute(Attribute.ATTACK_SPEED);
        if (attrAttackSpeed != null) {
            attrAttackSpeed.setBaseValue(4.0);
        }
    }

    // =========================
    // 이벤트 분배
    // =========================
    @EventHandler
    public void onAttack(EntityDamageByEntityEvent event) {
        if (!(event.getDamager() instanceof Player player)) return;

        Ability ability = getAbility(player);
        if (ability == null) return;

        try {
            ability.onAttack(player, event);
        } catch (Exception e) {
            plugin.getLogger().warning("[AbilitySystem] onAttack 오류 - " + player.getName() + " / " + ability.name());
            e.printStackTrace();
        }
    }

    @EventHandler
    public void onDamaged(EntityDamageEvent event) {
        if (!(event.getEntity() instanceof Player player)) return;

        Ability ability = getAbility(player);
        if (ability == null) return;

        try {
            ability.onDamaged(player, event);
        } catch (Exception e) {
            plugin.getLogger().warning("[AbilitySystem] onDamaged 오류 - " + player.getName() + " / " + ability.name());
            e.printStackTrace();
        }
    }

    @EventHandler
    public void onMove(PlayerMoveEvent event) {
        Player player = event.getPlayer();

        Ability ability = getAbility(player);
        if (ability == null) return;

        try {
            ability.onMove(player, event);
        } catch (Exception e) {
            plugin.getLogger().warning("[AbilitySystem] onMove 오류 - " + player.getName() + " / " + ability.name());
            e.printStackTrace();
        }
    }

    @EventHandler
    public void onBreakBlock(BlockBreakEvent event) {
        Player player = event.getPlayer();

        Ability ability = getAbility(player);
        if (ability == null) return;

        try {
            ability.onBreakBlock(player, event);
        } catch (Exception e) {
            plugin.getLogger().warning("[AbilitySystem] onBreakBlock 오류 - " + player.getName() + " / " + ability.name());
            e.printStackTrace();
        }
    }

    @EventHandler
    public void onConsume(PlayerItemConsumeEvent event) {
        Player player = event.getPlayer();

        Ability ability = getAbility(player);
        if (ability == null) return;

        try {
            ability.onConsume(player, event);
        } catch (Exception e) {
            plugin.getLogger().warning("[AbilitySystem] onConsume 오류 - " + player.getName() + " / " + ability.name());
            e.printStackTrace();
        }
    }

    @EventHandler
    public void onCraft(CraftItemEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) return;

        Ability ability = getAbility(player);
        if (ability == null) return;

        try {
            ability.onCraft(player, event);
        } catch (Exception e) {
            plugin.getLogger().warning("[AbilitySystem] onCraft 오류 - " + player.getName() + " / " + ability.name());
            e.printStackTrace();
        }
    }

    @EventHandler
    public void onSneakToggle(PlayerToggleSneakEvent event) {
        Player player = event.getPlayer();

        Ability ability = getAbility(player);
        if (ability == null) return;

        try {
            ability.onSneakToggle(player, event);
        } catch (Exception e) {
            plugin.getLogger().warning("[AbilitySystem] onSneakToggle 오류 - " + player.getName() + " / " + ability.name());
            e.printStackTrace();
        }
    }

    // 선택사항: 죽고 리스폰해도 능력 유지
    @EventHandler
    public void onRespawn(PlayerRespawnEvent event) {
        Player player = event.getPlayer();
        Ability ability = getAbility(player);
        if (ability == null) return;

        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            try {
                ability.onGrant(player);
            } catch (Exception e) {
                plugin.getLogger().warning("[AbilitySystem] onRespawn 재적용 오류 - " + player.getName() + " / " + ability.name());
                e.printStackTrace();
            }
        }, 1L);
    }

    // 선택사항: 나가면 그대로 두거나 제거할지 선택
    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        // 필요하면 remove(event.getPlayer());
    }
}