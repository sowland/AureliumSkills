package dev.auramc.auraskills.api.event.mana;

import dev.auramc.auraskills.api.event.AuraSkillsEvent;
import dev.auramc.auraskills.api.player.SkillsPlayer;
import dev.auramc.auraskills.api.AuraSkillsApi;
import dev.auramc.auraskills.api.mana.ManaAbility;
import dev.auramc.auraskills.api.event.Cancellable;

public class ManaAbilityActivateEvent extends AuraSkillsEvent implements Cancellable {

    private final SkillsPlayer skillsPlayer;
    private final ManaAbility manaAbility;
    private int duration;
    private boolean cancelled = false;

    public ManaAbilityActivateEvent(AuraSkillsApi api, SkillsPlayer skillsPlayer, ManaAbility manaAbility, int duration) {
        super(api);
        this.skillsPlayer = skillsPlayer;
        this.manaAbility = manaAbility;
        this.duration = duration;
    }

    public SkillsPlayer getSkillsPlayer() {
        return skillsPlayer;
    }

    public ManaAbility getManaAbility() {
        return manaAbility;
    }

    public int getDuration() {
        return duration;
    }

    public void setDuration(int duration) {
        this.duration = duration;
    }

    @Override
    public boolean isCancelled() {
        return cancelled;
    }

    @Override
    public void setCancelled(boolean cancelled) {
        this.cancelled = cancelled;
    }
}