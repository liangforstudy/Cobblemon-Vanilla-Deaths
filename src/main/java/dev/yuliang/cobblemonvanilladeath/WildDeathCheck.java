package dev.yuliang.cobblemonvanilladeath;

import com.cobblemon.mod.common.entity.pokemon.PokemonEntity;

/**
 * The one place the mod's scope rule lives: wild, not in battle, dying.
 * Every mixin asks this first and leaves Cobblemon alone when it says no.
 */
public final class WildDeathCheck {
    private WildDeathCheck() {}

    public static boolean isWildNonBattleDeath(PokemonEntity entity) {
        if (entity == null || !isDying(entity)) return false;

        // Decided once, when the death starts: a wild Pokémon that faints in battle starts dying before
        // the battle ends, and battleId is cleared on BattleEndPacket, mid-death (NOTES.md, item 2).
        VanillaDeathLatch latch = (VanillaDeathLatch) entity;
        Boolean decided = latch.cvd$getVanillaDeath();
        if (decided == null) {
            decided = isWild(entity) && !entity.isBattling();
            latch.cvd$setVanillaDeath(decided);
        }
        return decided;
    }

    private static boolean isDying(PokemonEntity entity) {
        return entity.isDeadOrDying() || entity.getEntityData().get(PokemonEntity.getDYING_EFFECTS_STARTED());
    }

    private static boolean isWild(PokemonEntity entity) {
        if (entity.getOwnerUUID() != null) return false;
        // Pokemon.isWild() reads the storage, which only exists server-side; on the client every
        // Pokémon reads as wild, so the synced owner UUID is all we have there.
        return entity.level().isClientSide() || entity.getPokemon().isWild();
    }
}
