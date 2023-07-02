package dev.aurelium.auraskills.common.reward;

import dev.aurelium.auraskills.api.skill.Skill;
import dev.aurelium.auraskills.common.hooks.HookManager;
import dev.aurelium.auraskills.common.AuraSkillsPlugin;
import dev.aurelium.auraskills.common.data.PlayerData;

import java.util.Locale;

public abstract class SkillReward {

    protected final AuraSkillsPlugin plugin;
    protected final HookManager hooks;

    public SkillReward(AuraSkillsPlugin plugin) {
        this.plugin = plugin;
        this.hooks = plugin.getHookManager();
    }

    public abstract void giveReward(PlayerData playerData, Skill skill, int level);

    public abstract String getMenuMessage(PlayerData playerData, Locale locale, Skill skill, int level);

    public abstract String getChatMessage(PlayerData playerData, Locale locale, Skill skill, int level);

}