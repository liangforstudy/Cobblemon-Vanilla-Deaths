package dev.yuliang.cobblemonvanilladeath;

/** Added to PokemonEntity by PokemonEntityLatchMixin. Holds WildDeathCheck's decision for this death. */
public interface VanillaDeathLatch {
    /** null until the death starts, then whether this death uses the vanilla animation. */
    Boolean cvd$getVanillaDeath();

    void cvd$setVanillaDeath(Boolean vanillaDeath);
}
