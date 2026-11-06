package dev.yuliang.cobblemonvanilladeath.mixin;

import com.cobblemon.mod.common.Cobblemon;
import com.cobblemon.mod.common.entity.pokemon.PokemonEntity;
import com.cobblemon.mod.common.entity.pokemon.PokemonServerDelegate;
import dev.yuliang.cobblemonvanilladeath.WildDeathCheck;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * PokemonEntity.tickDeath() only calls delegate.updatePostDeath(). On the server that counts deathTime
 * to 60 before the poof and remove(KILLED). Wild non-battle deaths end at vanilla's 20 instead, using
 * the same end steps Cobblemon would run at 60.
 */
@Mixin(value = PokemonServerDelegate.class, remap = false)
public abstract class PokemonEntityDeathMixin {
    /** LivingEntity.tickDeath removes the entity once deathTime reaches 20. */
    private static final int VANILLA_DEATH_TICKS = 20;

    @Shadow
    public abstract PokemonEntity getEntity();

    @Shadow
    public abstract void doDeathDrops();

    @Inject(method = "updatePostDeath", at = @At("HEAD"), cancellable = true)
    private void cvd$endAtVanillaTiming(CallbackInfo ci) {
        PokemonEntity entity = getEntity();
        if (!WildDeathCheck.isWildNonBattleDeath(entity)) return;
        if (entity.deathTime < VANILLA_DEATH_TICKS || entity.isRemoved()) return;

        // Mirrors Cobblemon's deathTime == 60 branch
        entity.ejectPassengers();
        entity.level().broadcastEntityEvent(entity, (byte) 60); // poof particles
        if (Cobblemon.INSTANCE.getConfig().getDropAfterDeathAnimation()) doDeathDrops();
        entity.remove(Entity.RemovalReason.KILLED);
        ci.cancel();
    }
}
