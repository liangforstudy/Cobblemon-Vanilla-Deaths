package dev.yuliang.cobblemonvanilladeath.mixin;

import com.cobblemon.mod.common.entity.pokemon.PokemonEntity;
import dev.yuliang.cobblemonvanilladeath.VanillaDeathLatch;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

/** Storage for WildDeathCheck's per-death decision. Adds a field only, no injections. */
@Mixin(PokemonEntity.class)
public abstract class PokemonEntityLatchMixin implements VanillaDeathLatch {
    @Unique
    private Boolean cvd$vanillaDeath;

    @Override
    public Boolean cvd$getVanillaDeath() {
        return cvd$vanillaDeath;
    }

    @Override
    public void cvd$setVanillaDeath(Boolean vanillaDeath) {
        cvd$vanillaDeath = vanillaDeath;
    }
}
