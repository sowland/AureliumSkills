package dev.aurelium.auraskills.common.hooks;

import dev.aurelium.auraskills.common.AuraSkillsPlugin;
import dev.aurelium.auraskills.common.data.PlayerData;

public abstract class PlaceholderHook extends Hook {

    public PlaceholderHook(AuraSkillsPlugin plugin) {
        super(plugin);
    }

    public abstract String setPlaceholders(PlayerData playerData, String message);

}