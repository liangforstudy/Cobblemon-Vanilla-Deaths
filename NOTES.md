# NOTES - Cobblemon internals (Step 0 findings)

Source: GitLab `cable-mc/cobblemon`, tag listed per section. JVM signatures checked with `javap`
against the release jar.

## Cobblemon 1.8.1+1.21.1 (Stonecutter node `1.21.1`)

### 1. `PokemonEntity.tickDeath()` - overridden, never calls super

```kotlin
override fun tickDeath() { delegate.updatePostDeath() }
```

- **Server** `PokemonServerDelegate.updatePostDeath()`:
  - First call (`DYING_EFFECTS_STARTED` false): stop riding, eject passengers, set
    `DYING_EFFECTS_STARTED = true` (synced; this is what triggers the client faint anim),
    recall-with-animation if owner is a `PokemonSender` and `beamMode == -1`.
  - `deathTime == 0`: wipe effects, `deathTime = 1`, `doDeathDrops()` unless
    `config.dropAfterDeathAnimation`, return.
  - While `effects.progress` not done: return (deathTime frozen).
  - `++deathTime`; at 30 owned Pokémon get recall beam (`BEAM_MODE = 3`).
  - **At 60**: eject passengers, `broadcastEntityEvent(60)` (poof), `doDeathDrops()` if
    `dropAfterDeathAnimation`, `remove(KILLED)`.
- **Client** `PokemonClientDelegate.updatePostDeath()`: just `++currentEntity.deathTime`.
  So client `deathTime` counts up every tick from death, exactly like vanilla.

=> The vanilla sequence is blocked only by *timing* (60 instead of 20). The poof and
`remove(KILLED)` already exist. The mixin only needs to end the sequence at `deathTime >= 20`
for wild kills and reuse Cobblemon's own end-of-death steps (eject, poof, drops, remove).

### 2. Wild / battle checks

- `Pokemon.isWild()` = `storeCoordinates.get() == null`. **Server-only meaningful.** On the
  client the `Pokemon` object has no store, so it reads as wild for everything.
- `PokemonEntity.getOwnerUUID()` (TamableAnimal, synced) - null for wild. Use on both sides.
- `PokemonEntity.getBattleId()` / `isBattling()` - synced `BATTLE_ID`. Valid on both sides.
- `battleId` is cleared in `PokemonBattleActor.sendUpdate(BattleEndPacket)`. A wild Pokémon
  that faints in battle starts dying (health set to 0 by `PokemonServerDelegate.updateHealth`)
  **before** the battle ends, so `battleId` can go null mid-death. => The scope decision
  must be **latched** when the death starts, not re-evaluated every tick.

### 3. Renderer

- `PokemonRenderer extends MobRenderer<PokemonEntity, PosablePokemonEntityModel>`.
- Does **not** override `setupRotations`, so vanilla's `deathTime > 0` branch runs.
- **But** `getFlipDegrees(PokemonEntity)` is overridden to return `0F`, which kills the tip.
  => Need `PokemonRendererRotationMixin`: return 90 (vanilla default) for latched wild deaths.

### 4. Faint animation

- `PokemonClientDelegate.onSyncedDataUpdated(EntityDataAccessor)`: when
  `DYING_EFFECTS_STARTED` becomes true it calls `model.getAnimation(this, "faint", runtime)`
  (`PosableModel.getAnimation(PosableState, String, MoLangRuntime)`), wraps it in a
  `PrimaryAnimation`, and schedules `activeSendoutScale = 0F` after 3 s.
  If `getAnimation` returns null it returns early and skips both.
  => `FaintAnimationMixin`: `@WrapOperation` that `getAnimation` call -> null for wild deaths.

### 5. Removal path

- Only `PokemonServerDelegate.updatePostDeath` at `deathTime == 60` (see 1). If we remove at
  20 first, that branch is never reached. No other race found.
- `PokemonEntity.remove()` resets `pokemon.state`, emits `removalObservable`, kills
  evolution entity. All still runs because we call `remove(KILLED)`.

### Red tint

- `PosableEntityModel.renderToBuffer` ignores the packed overlay and computes its own via
  `getOverlayTexture(entity)`, which is red when `hurtTime > 0 || deathTime > 0`. Since
  client `deathTime` already counts up, **the tint works with no mixin**.

### Drops

- `dropAllDeathLoot` (vanilla loot table + `delegate.drop`) runs in `die()` for wild only -
  unaffected.
- Cobblemon's species drops + held item: `PokemonServerDelegate.doDeathDrops()`, at
  `deathTime == 0` by default, or at the removal step when `dropAfterDeathAnimation` is on.
  Our early-removal path must call `doDeathDrops()` in that config case.

### How the mod maps onto this (1.8.1)

| Piece | Target | Why |
|---|---|---|
| `PokemonEntityLatchMixin` | `PokemonEntity` (field only) | Holds `WildDeathCheck`'s decision for the death (see item 2) |
| `PokemonEntityDeathMixin` | `PokemonServerDelegate.updatePostDeath` HEAD, `remap = false` | Ends the death at `deathTime >= 20` with Cobblemon's own end steps. Targeting the Cobblemon method instead of the `tickDeath` override avoids remapping an inherited vanilla name on a mod class, and keeps Cobblemon's first-tick logic (eject, effects wipe, drops) |
| `FaintAnimationMixin` | `PokemonClientDelegate.onSyncedDataUpdated`, wraps `PosableModel.getAnimation` | null -> no faint anim, no 3 s shrink |
| `PokemonRendererRotationMixin` | vanilla `LivingEntityRenderer.setupRotations`, wraps the `getFlipDegrees` call | Restores 90 for Pokémon only. On the vanilla class so it remaps normally |

Open questions to settle in game:
- NPC-owned Pokémon: server sees `isWild() == false`, but the client only checks `ownerUUID`.
  If NPC Pokémon have no owner UUID, the client would tip them while the server keeps the 60-tick
  removal. Check whether NPC trainer Pokémon ever die outside battle.
- Server deathTime freezes while `effects.progress` isn't done, so removal can come later than 20
  ticks if a Pokémon dies with an effect running. The client tip has already finished by then.
