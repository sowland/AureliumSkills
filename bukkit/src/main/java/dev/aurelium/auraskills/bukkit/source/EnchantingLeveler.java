package dev.aurelium.auraskills.bukkit.source;

import dev.aurelium.auraskills.api.skill.Skill;
import dev.aurelium.auraskills.api.source.type.EnchantingXpSource;
import dev.aurelium.auraskills.bukkit.AuraSkills;
import dev.aurelium.auraskills.common.source.SourceType;
import dev.aurelium.auraskills.common.user.User;
import dev.aurelium.auraskills.common.util.data.Pair;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.enchantment.EnchantItemEvent;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.Nullable;

import java.util.Map;

public class EnchantingLeveler extends SourceLeveler {

    public EnchantingLeveler(AuraSkills plugin) {
        super(plugin, SourceType.ENCHANTING);
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onEnchant(EnchantItemEvent event) {
        if (disabled()) return;

        Player player = event.getEnchanter();

        var sourcePair = getSource(event.getItem());
        if (sourcePair == null) return;

        EnchantingXpSource source = sourcePair.first();
        Skill skill = sourcePair.second();

        if (failsChecks(event, player, event.getEnchantBlock().getLocation(), skill)) return;

        User user = plugin.getUser(player);

        double xp = source.getXp();
        String unit = source.getUnit();
        if (unit != null && unit.equals("{sources.units.enchant_level}")) {
            xp *= event.getExpLevelCost();
        }

        plugin.getLevelManager().addXp(user, skill, xp);
    }

    @Nullable
    private Pair<EnchantingXpSource, Skill> getSource(ItemStack item) {
        for (Map.Entry<EnchantingXpSource, Skill> entry : plugin.getSkillManager().getSourcesOfType(EnchantingXpSource.class).entrySet()) {
            if (plugin.getItemRegistry().passesFilter(item, entry.getKey().getItem())) { // Return source that passes item filter
                return Pair.fromEntry(entry);
            }
        }
        return null;
    }

}