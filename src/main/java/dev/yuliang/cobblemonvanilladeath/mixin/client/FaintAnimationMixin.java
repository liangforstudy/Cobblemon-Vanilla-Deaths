package dev.yuliang.cobblemonvanilladeath.mixin.client;

import com.bedrockk.molang.runtime.MoLangRuntime;
import com.cobblemon.mod.common.client.entity.PokemonClientDelegate;
import com.cobblemon.mod.common.client.render.models.blockbench.PosableModel;
import com.cobblemon.mod.common.client.render.models.blockbench.PosableState;
import com.cobblemon.mod.common.client.render.models.blockbench.animation.ActiveAnimation;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import dev.yuliang.cobblemonvanilladeath.WildDeathCheck;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/**
 * When DYING_EFFECTS_STARTED syncs, Cobblemon looks up the "faint" animation and plays it. Returning
 * null makes it bail out before playing it or shrinking the model, so the model holds its last pose
 * while vanilla's setupRotations tips it over.
 */
@Mixin(value = PokemonClientDelegate.class, remap = false)
public abstract class FaintAnimationMixin {
    @WrapOperation(
            method = "onSyncedDataUpdated",
            at = @At(
                    value = "INVOKE",
                    target = "Lcom/cobblemon/mod/common/client/render/models/blockbench/PosableModel;getAnimation(Lcom/cobblemon/mod/common/client/render/models/blockbench/PosableState;Ljava/lang/String;Lcom/bedrockk/molang/runtime/MoLangRuntime;)Lcom/cobblemon/mod/common/client/render/models/blockbench/animation/ActiveAnimation;"
            )
    )
    private ActiveAnimation cvd$skipWildFaint(PosableModel model, PosableState state, String name, MoLangRuntime runtime, Operation<ActiveAnimation> original) {
        if ("faint".equals(name) && WildDeathCheck.isWildNonBattleDeath(((PokemonClientDelegate) (Object) this).getCurrentEntity())) {
            return null;
        }
        return original.call(model, state, name, runtime);
    }
}
