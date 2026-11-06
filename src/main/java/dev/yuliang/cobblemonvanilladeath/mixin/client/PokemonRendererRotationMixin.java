package dev.yuliang.cobblemonvanilladeath.mixin.client;

import com.cobblemon.mod.common.entity.pokemon.PokemonEntity;
import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.sugar.Local;
import dev.yuliang.cobblemonvanilladeath.WildDeathCheck;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/**
 * PokemonRenderer doesn't override setupRotations, so vanilla's deathTime tip runs, but it overrides
 * getFlipDegrees() to return 0. Put vanilla's default back at the one place setupRotations reads it.
 * Targets the vanilla class so it remaps like any vanilla mixin; the instanceof keeps it Pokémon-only.
 */
@Mixin(LivingEntityRenderer.class)
public abstract class PokemonRendererRotationMixin {
    /** LivingEntityRenderer.getFlipDegrees() default. */
    private static final float VANILLA_FLIP_DEGREES = 90.0F;

    @ModifyExpressionValue(
            method = "setupRotations",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/entity/LivingEntityRenderer;getFlipDegrees(Lnet/minecraft/world/entity/LivingEntity;)F")
    )
    private float cvd$vanillaFlip(float flipDegrees, @Local(argsOnly = true) LivingEntity entity) {
        if (entity instanceof PokemonEntity pokemon && WildDeathCheck.isWildNonBattleDeath(pokemon)) {
            return VANILLA_FLIP_DEGREES;
        }
        return flipDegrees;
    }
}
