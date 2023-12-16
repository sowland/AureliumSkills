package dev.aurelium.auraskills.bukkit.loot.provider;

import com.archyx.lootmanager.loot.LootPool;
import dev.aurelium.auraskills.api.source.XpSource;
import dev.aurelium.auraskills.api.event.loot.LootDropEvent;
import dev.aurelium.auraskills.bukkit.AuraSkills;
import dev.aurelium.auraskills.bukkit.loot.handler.BlockLootHandler;
import dev.aurelium.auraskills.common.user.User;

public abstract class SkillLootProvider {

    protected final AuraSkills plugin;
    protected final BlockLootHandler handler;

    public SkillLootProvider(AuraSkills plugin, BlockLootHandler handler) {
        this.plugin = plugin;
        this.handler = handler;
    }

    public abstract double getChance(LootPool pool, User user);

    public abstract LootDropEvent.Cause getCause(LootPool pool);

    public abstract boolean isApplicable(LootPool pool, XpSource source);

}