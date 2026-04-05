package my.pkg;

import my.pkg.ability.*;
import org.bukkit.command.PluginCommand;
import org.bukkit.plugin.java.JavaPlugin;

public final class MyPlugin extends JavaPlugin {

    private AbilitySystem abilitySystem;
    private NatureSurvivalManager natureSurvivalManager;

    @Override
    public void onEnable() {
        this.abilitySystem = new AbilitySystem(this);

        abilitySystem.registerAbility(new FireAbility(this));
        abilitySystem.registerAbility(new WaterAbility(this));
        abilitySystem.registerAbility(new WindAbility(this));
        abilitySystem.registerAbility(new EarthAbility(this));
        abilitySystem.registerAbility(new ElectricAbility(this));
        abilitySystem.registerAbility(new LightAbility(this));
        abilitySystem.registerAbility(new MetalAbility(this));

        getServer().getPluginManager().registerEvents(abilitySystem, this);
        getServer().getPluginManager().registerEvents(new LightArmorListener(abilitySystem), this);
        getServer().getPluginManager().registerEvents(new MetalLockListener(this, abilitySystem), this);
        getServer().getPluginManager().registerEvents(new MetalKeepInventoryListener(this, abilitySystem), this);

        new WindPickupListener(this, abilitySystem);

        this.natureSurvivalManager = new NatureSurvivalManager(this, abilitySystem);
        getServer().getPluginManager().registerEvents(natureSurvivalManager, this);

        PluginCommand cmd = getCommand("naturesurvival");
        if (cmd == null) {
            getLogger().severe("plugin.yml에 naturesurvival 명령어가 등록되지 않았습니다.");
            getServer().getPluginManager().disablePlugin(this);
            return;
        }
        cmd.setExecutor(natureSurvivalManager);

        getLogger().info("원소 AbilitySystem 활성화 완료");
    }

    public AbilitySystem getAbilitySystem() {
        return abilitySystem;
    }
}