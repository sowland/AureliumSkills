package dev.aurelium.auraskills.bukkit.trait;

import dev.aurelium.auraskills.api.trait.Trait;
import dev.aurelium.auraskills.api.trait.Traits;
import dev.aurelium.auraskills.bukkit.AuraSkills;
import dev.aurelium.auraskills.bukkit.skills.archery.ArcheryAbilities;
import dev.aurelium.auraskills.common.user.User;
import dev.aurelium.auraskills.api.util.NumberUtil;
import org.bukkit.entity.Player;

import java.util.Random;

public class CritChanceTrait extends TraitImpl {

    private final Random rand = new Random();

    CritChanceTrait(AuraSkills plugin) {
        super(plugin, Traits.CRIT_CHANCE);
    }

    @Override
    public double getBaseLevel(Player player, Trait trait) {
        return Traits.CRIT_CHANCE.optionDouble("base");
    }

    @Override
    public String getMenuDisplay(double value, Trait trait) {
        return NumberUtil.format1(value) + "%";
    }

    @Override
    protected void reload(Player player, Trait trait) {
        User user = plugin.getUser(player);
        plugin.getAbilityManager().getAbilityImpl(ArcheryAbilities.class).reloadCritChance(player, user);
    }

    public boolean isCrit(User user) {
        return rand.nextDouble() < (user.getEffectiveTraitLevel(Traits.CRIT_CHANCE) / 100);
    }
}