package dev.auramc.auraskills.common;

import dev.auramc.auraskills.common.ability.AbilityManager;
import dev.auramc.auraskills.common.ability.AbilityRegistry;
import dev.auramc.auraskills.common.config.ConfigProvider;
import dev.auramc.auraskills.common.data.PlayerData;
import dev.auramc.auraskills.common.data.PlayerManager;
import dev.auramc.auraskills.common.event.AuraSkillsEventManager;
import dev.auramc.auraskills.common.hooks.HookManager;
import dev.auramc.auraskills.common.item.ItemRegistry;
import dev.auramc.auraskills.common.leaderboard.LeaderboardManager;
import dev.auramc.auraskills.common.leveler.Leveler;
import dev.auramc.auraskills.common.leveler.XpRequirements;
import dev.auramc.auraskills.common.mana.ManaAbilityManager;
import dev.auramc.auraskills.common.mana.ManaAbilityRegistry;
import dev.auramc.auraskills.common.message.MessageKey;
import dev.auramc.auraskills.common.message.MessageProvider;
import dev.auramc.auraskills.common.message.PlatformLogger;
import dev.auramc.auraskills.common.skill.SkillRegistry;
import dev.auramc.auraskills.common.stat.StatManager;
import dev.auramc.auraskills.common.stat.StatRegistry;

import java.util.Locale;

public interface AuraSkillsPlugin {

    MessageProvider getMessageProvider();

    ConfigProvider getConfigProvider();

    AbilityManager getAbilityManager();

    ManaAbilityManager getManaAbilityManager();

    StatManager getStatManager();

    ItemRegistry getItemRegistry();

    Leveler getLeveler();

    PlayerManager getPlayerManager();

    XpRequirements getXpRequirements();

    AuraSkillsEventManager getEventManager();

    PlatformLogger getLogger();

    SkillRegistry getSkillRegistry();

    StatRegistry getStatRegistry();

    AbilityRegistry getAbilityRegistry();

    ManaAbilityRegistry getManaAbilityRegistry();

    HookManager getHookManager();

    LeaderboardManager getLeaderboardManager();

    // Message convenience methods

    /**
     * Gets a message from the message provider.
     *
     * @param key The message key
     * @param locale The language to get the message in
     * @return The message
     */
    String getMsg(MessageKey key, Locale locale);

    /**
     * Gets the default language of the plugin as set by the server's configuration.
     *
     * @return The default language
     */
    default Locale getDefaultLanguage() {
        return getMessageProvider().getDefaultLanguage();
    }

    // Platform-dependent Minecraft methods

    /**
     * Executes a command as the console
     *
     * @param command The command to execute
     */
    void runConsoleCommand(String command);

    /**
     * Executes a command as a player
     *
     * @param playerData The player to execute the command as
     * @param command The command to execute
     */
    void runPlayerCommand(PlayerData playerData, String command);

}