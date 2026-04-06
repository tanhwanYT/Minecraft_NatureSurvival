package my.pkg;

import my.pkg.ability.Ability;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.HumanEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.event.player.PlayerJoinEvent;

import java.util.*;

public class NatureSurvivalManager implements Listener, CommandExecutor {

    private final JavaPlugin plugin;
    private final AbilitySystem abilitySystem;

    private final Map<UUID, String> selectedElements = new HashMap<>();
    private boolean gameStarting = false;
    private boolean gameStarted = false;

    private static final String GUI_TITLE = "원소 선택";

    public NatureSurvivalManager(JavaPlugin plugin, AbilitySystem abilitySystem) {
        this.plugin = plugin;
        this.abilitySystem = abilitySystem;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {

        if (!command.getName().equalsIgnoreCase("naturesurvival")) {
            return false;
        }

        if (args.length == 0) {
            sender.sendMessage("§c사용법: /NatureSurvival start");
            return true;
        }

        if (args[0].equalsIgnoreCase("start")) {
            if (!sender.isOp()) {
                sender.sendMessage("§cOP만 사용할 수 있습니다.");
                return true;
            }

            if (gameStarted || gameStarting) {
                sender.sendMessage("§e이미 게임이 진행 중이거나 선택 중입니다.");
                return true;
            }

            startSelectionPhase();
            Bukkit.broadcastMessage("§a[원소 서바이벌] 게임을 시작합니다!");
            Bukkit.broadcastMessage("§f각자 원하는 원소를 선택하세요. 이미 선택된 원소는 다른 사람이 고를 수 없습니다.");
            return true;
        }

        if (args[0].equalsIgnoreCase("pick")) {
            if (!(sender instanceof Player player)) {
                sender.sendMessage("§c플레이어만 사용할 수 있습니다.");
                return true;
            }

            if (!gameStarted) {
                player.sendMessage("§c게임 시작 후에만 사용할 수 있습니다.");
                return true;
            }

            selectedElements.remove(player.getUniqueId());
            abilitySystem.remove(player);

            openSelectionGui(player);
            refreshAllSelectionGuis();

            player.sendMessage("§e원소를 다시 선택하세요.");
            return true;
        }

        return true;
    }

    private void startSelectionPhase() {
        gameStarting = true;
        selectedElements.clear();

        for (Player player : Bukkit.getOnlinePlayers()) {
            abilitySystem.remove(player);
            openSelectionGui(player);
        }
    }

    private void openSelectionGui(Player player) {
        Inventory inv = Bukkit.createInventory(null, 27, GUI_TITLE);

        setElementItem(inv, 10, "불", Material.BLAZE_POWDER, List.of(
                "§a[장점]",
                "§7- 공격 시 불 부여",
                "§7- 화염저항",
                "§7- 용암에서 이동속도 증가",
                "§c[단점]",
                "§7- 물에 닿으면 큰 피해",
                "§7- 바닥 아이템을 주울 때 가끔 타서 사라짐"
        ));

        setElementItem(inv, 11, "물", Material.HEART_OF_THE_SEA, List.of(
                "§a[장점]",
                "§7- 물속 공격력/방어력 증가",
                "§7- 수중호흡, 돌고래의 가호",
                "§7- 물속 채굴 패널티 제거",
                "§c[단점]",
                "§7- 물 밖에서 이동속도 감소",
                "§7- 물 밖에서 숨을 쉴수 없음, 물 안 속에서는 호흡게이지가 달지 않음"
        ));

        setElementItem(inv, 12, "바람", Material.FEATHER, List.of(
                "§a[장점]",
                "§7- 낙하대미지 제거",
                "§7- 신속 버프",
                "§7- 쉬프트를 눌러 비행 추진",
                "§7- 아이템 흡수 범위 증가",
                "§c[단점]",
                "§7- 최대체력 2칸 감소",
                "§7- 인벤토리 9칸 감소"
        ));

        setElementItem(inv, 13, "대지", Material.GRASS_BLOCK, List.of(
                "§a[장점]",
                "§7- 최대체력 2칸 증가",
                "§7- 원석 자동 제련",
                "§7- 자원 2배 획득 확률",
                "§c[단점]",
                "§7- 네더/엔드에서 장점 비활성화",
                "§7- 공격속도 감소"
        ));

        setElementItem(inv, 14, "전기", Material.LIGHTNING_ROD, List.of(
                "§a[장점]",
                "§7- 공격 시 연쇄 대미지",
                "§7- 신속, 야간투시",
                "§7- 비전투 시 흡수체력 3칸",
                "§e[기타]",
                "§7- 가끔 근처에 번개 연출",
                "§c[단점]",
                "§7- 물에 닿으면 큰 피해",
                "§7- 배고픔이 빨리 닳음"
        ));

        setElementItem(inv, 15, "빛", Material.END_ROD, List.of(
                "§a[장점]",
                "§7- 바라보는 엔티티 발광",
                "§7- 재생, 포화 버프",
                "§7- 주변 플레이어에게 약한 재생 공유",
                "§c[단점]",
                "§7- 어두운 곳에서 능력 약화",
                "§7- 흉갑 착용 불가"
        ));

        setElementItem(inv, 16, "금속", Material.IRON_INGOT, List.of(
                "§a[장점]",
                "§7- 쉬프트 시 손 장비 내구도 회복",
                "§7- 제작 장비 랜덤 인챈트",
                "§7- 인벤세이브",
                "§7- 화염 대미지 감소",
                "§c[단점]",
                "§7- 이동속도 감소",
                "§7- 인벤토리 9칸 감소"
        ));

        markTakenElements(inv);
        player.openInventory(inv);
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();

        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            if (!player.isOnline()) return;
            if (selectedElements.containsKey(player.getUniqueId())) return;
            if (abilitySystem.hasAbility(player)) return;

            openSelectionGui(player);
            player.sendMessage("§e사용할 원소를 선택하세요.");
            refreshAllSelectionGuis();
        }, 20L);
    }

    private void setElementItem(Inventory inv, int slot, String elementName, Material material, List<String> lore) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return;

        meta.setDisplayName("§e" + elementName);
        meta.setLore(lore);
        meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES);
        item.setItemMeta(meta);

        inv.setItem(slot, item);
    }

    private void markTakenElements(Inventory inv) {
        for (Map.Entry<UUID, String> entry : selectedElements.entrySet()) {
            String selected = entry.getValue();

            for (int i = 0; i < inv.getSize(); i++) {
                ItemStack item = inv.getItem(i);
                if (item == null || !item.hasItemMeta()) continue;
                ItemMeta meta = item.getItemMeta();
                if (meta == null || meta.getDisplayName() == null) continue;

                String plainName = stripColor(meta.getDisplayName());
                if (!plainName.equalsIgnoreCase(selected)) continue;

                ItemStack blocked = new ItemStack(Material.BARRIER);
                ItemMeta blockedMeta = blocked.getItemMeta();
                if (blockedMeta != null) {
                    blockedMeta.setDisplayName("§c" + selected + " §7(선택됨)");
                    blockedMeta.setLore(List.of("§7다른 플레이어가 이미 선택한 원소입니다."));
                    blocked.setItemMeta(blockedMeta);
                }
                inv.setItem(i, blocked);
            }
        }
    }

    private String stripColor(String text) {
        return text.replaceAll("§.", "");
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!event.getView().getTitle().equals(GUI_TITLE)) return;
        event.setCancelled(true);

        if (!(event.getWhoClicked() instanceof Player player)) return;

        ItemStack clicked = event.getCurrentItem();
        if (clicked == null || !clicked.hasItemMeta()) return;

        ItemMeta meta = clicked.getItemMeta();
        if (meta == null || meta.getDisplayName() == null) return;

        String clickedName = stripColor(meta.getDisplayName());

        if (clicked.getType() == Material.BARRIER) {
            player.sendMessage("§c이미 다른 플레이어가 선택한 원소입니다.");
            player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1f, 1f);
            return;
        }

        if (selectedElements.containsValue(clickedName)) {
            player.sendMessage("§c이미 다른 플레이어가 선택한 원소입니다.");
            player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1f, 1f);
            refreshAllSelectionGuis();
            return;
        }

        Ability ability = abilitySystem.getRegisteredAbility(clickedName);
        if (ability == null) {
            player.sendMessage("§c등록되지 않은 원소입니다: " + clickedName);
            return;
        }

        selectedElements.put(player.getUniqueId(), clickedName);
        abilitySystem.grant(player, ability);

        player.sendMessage("§a[원소] " + clickedName + " 원소를 선택했습니다.");
        player.playSound(player.getLocation(), Sound.UI_TOAST_CHALLENGE_COMPLETE, 1f, 1.2f);
        player.closeInventory();

        refreshAllSelectionGuis();
        checkAllSelectedAndStart();
    }

    @EventHandler
    public void onInventoryClose(InventoryCloseEvent event) {
        if (!gameStarting || gameStarted) return;
        if (!event.getView().getTitle().equals(GUI_TITLE)) return;

        if (!(event.getPlayer() instanceof Player player)) return;

        if (selectedElements.containsKey(player.getUniqueId())) return;

        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            if (!gameStarting || gameStarted) return;
            if (player.isOnline() && !selectedElements.containsKey(player.getUniqueId())) {
                openSelectionGui(player);
                player.sendMessage("§e원소를 선택해야 게임을 시작할 수 있습니다.");
            }
        }, 1L);
    }

    private void refreshAllSelectionGuis() {
        for (Player online : Bukkit.getOnlinePlayers()) {
            if (selectedElements.containsKey(online.getUniqueId())) continue;

            if (online.getOpenInventory().getTitle().equals(GUI_TITLE)) {
                openSelectionGui(online);
            }
        }
    }

    private void checkAllSelectedAndStart() {
        List<Player> players = new ArrayList<>(Bukkit.getOnlinePlayers());
        if (players.isEmpty()) return;

        for (Player player : players) {
            if (!selectedElements.containsKey(player.getUniqueId())) {
                return;
            }
        }

        gameStarting = false;
        gameStarted = true;

        Bukkit.broadcastMessage("§6[원소 서바이벌] 모든 플레이어의 원소 선택이 완료되었습니다!");
        Bukkit.broadcastMessage("§a야생을 시작합니다!");
    }

    public boolean isGameStarted() {
        return gameStarted;
    }

    public boolean isGameStarting() {
        return gameStarting;
    }

    public Map<UUID, String> getSelectedElements() {
        return Collections.unmodifiableMap(selectedElements);
    }
}