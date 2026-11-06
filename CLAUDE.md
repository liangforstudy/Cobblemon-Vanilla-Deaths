# CLAUDE.md - Cobblemon Early Death Animation

## What this mod does

Replaces Cobblemon's faint animation for **wild Pokémon killed outside battle** with Minecraft's vanilla mob death:

1. Model turns red (hurt overlay)
2. Model tips sideways over ~20 ticks
3. Entity vanishes in a puff of smoke (poof particles) and is removed server-side, so it leaves no hitbox behind

Battle faints are **out of scope for now**. In battle, this mod must not change anything.

- Mod name: **Cobblemon Early Death Animation** (renamed from "Vanilla Cobblemon Death Animation"; mod id and package unchanged)
- Mod id: `cobblemon_vanilla_death`
- Java package suffix: `cobblemonvanilladeath`

## Core idea - let vanilla do the work

Do NOT reimplement the death visuals. Vanilla already drives all three effects from `LivingEntity.deathTime`:

| Effect | Vanilla source | Condition |
|---|---|---|
| Sideways tip | `LivingEntityRenderer.setupRotations` (uses `getFlipDegrees()`, default 90) | `deathTime > 0` |
| Red tint | `LivingEntityRenderer.getOverlayCoords` | `hurtTime > 0 \|\| deathTime > 0` |
| Poof + removal | `LivingEntity.tickDeath` -> entity event 60 (`makePoofParticles`) + `remove(KILLED)` at `deathTime >= 20` | server tick |

The mod's job is to stop Cobblemon from blocking or overriding that vanilla sequence for wild kills.

## Stack

- Build: **Stonecutter** multi-version setup (Gradle, Kotlin DSL)
- Loader: Fabric first (1.21.1 is the primary target). NeoForge can come later through Stonecutter.
- Language: Java for mixins. Kotlin is fine elsewhere, since Cobblemon itself is Kotlin.
- Dependencies: Fabric API, Cobblemon (pinned per Stonecutter node), MixinExtras (bundled with Fabric Loader)
- Sides: one jar, with common + client mixins. The server mixin handles removal and must be on the server for the "no hitbox" behaviour.

## Project layout

```
.
├── CLAUDE.md
├── settings.gradle.kts          # Stonecutter: declare versions here
├── stonecutter.gradle.kts       # Stonecutter controller (active version)
├── build.gradle.kts             # shared build logic, reads per-version props
├── gradle.properties            # shared props (mod_id, mod_version, maven group)
├── versions/
│   └── 1.21.1/
│       └── gradle.properties    # minecraft, yarn/mojmap, loader, fabric_api, cobblemon versions
└── src/main/
    ├── java/<group>/cobblemonvanilladeath/
    │   ├── CobblemonVanillaDeath.java          # common entrypoint (minimal, logging only)
    │   ├── WildDeathCheck.java                 # single helper: isWildNonBattleDeath(entity)
    │   ├── mixin/
    │   │   └── PokemonEntityDeathMixin.java    # common/server
    │   └── mixin/client/
    │       ├── FaintAnimationMixin.java        
    │       └── PokemonRendererRotationMixin.java  # ONLY if needed (see step 3)
    └── resources/
        ├── fabric.mod.json
        └── cobblemon_vanilla_death.mixins.json
```

## Implementation steps

### Step 0 - Read Cobblemon source before writing any mixin

Cobblemon is open source on GitLab. For the Cobblemon version pinned in each Stonecutter node, find and note the following. These names are **unverified guesses**, so confirm each one:

1. **`PokemonEntity`** - does it override `tickDeath()`? What does it do instead: a faint pose, delayed removal, a custom timer?
2. **Wild check** - how to tell a wild Pokémon apart. Candidates: `pokemon.isWild()`, `ownerUUID == null`, and some battle flag or battle id (`isBattling`, `battleId`).
3. **Renderer** - the Pokémon entity renderer class. Does it extend `LivingEntityRenderer`? Does it override `setupRotations`, and if so, does it skip the `deathTime` branch?
4. **Faint animation** - where the poser or animation system selects the faint animation for a dying entity, and what condition triggers it (`deathTime`, `isDeadOrDying`, a pose enum, an entity data tracker).
5. **Removal path** - whether Cobblemon removes or despawns wild fainted entities itself, which could race with vanilla's removal.

Write the findings into a `NOTES.md`, keyed by Cobblemon version, before implementing.

### Step 1 - `WildDeathCheck`

One static helper that every mixin calls, so the scope rule lives in a single place:

```java
static boolean isWildNonBattleDeath(PokemonEntity e) {
    // wild AND not in battle AND dying
}
```

If this returns false, every mixin must return early and leave Cobblemon's behaviour untouched.

### Step 2 - `PokemonEntityDeathMixin` (common)

- Target: `PokemonEntity.tickDeath()` (or whatever Step 0 found)
- `@Inject(at = @At("HEAD"), cancellable = true)`
- If `isWildNonBattleDeath`, run vanilla's tickDeath logic, then `ci.cancel()`:

```java
deathTime++;
if (deathTime >= 20 && !level().isClientSide() && !isRemoved()) {
    level().broadcastEntityEvent(this, (byte) 60); // poof particles
    remove(RemovalReason.KILLED);
}
```

- A mixin cannot call `super.tickDeath()` on an overridden method, so copy vanilla's ~8 lines. Do NOT copy any rendering math.
- If Cobblemon does not override `tickDeath`, this mixin may be unnecessary. Instead, cancel whatever is blocking it (Step 0, item 5).
- Item drops happen in `die()`, not `tickDeath()`. Confirm that loot and Cobblemon's own drop logic still fire.

### Step 3 - Client: let the vanilla rotation and tint show

- **`FaintAnimationMixin`**: when `isWildNonBattleDeath`, skip or cancel the faint animation so the model holds its last pose while vanilla tips it over.
- **Rotation**: if Cobblemon's renderer calls through to `LivingEntityRenderer.setupRotations` and `deathTime` now increments, it should work with no extra code. Test this before writing anything else.
  - Only if the renderer blocks it: add `PokemonRendererRotationMixin` to restore the death branch. Call vanilla's path where possible instead of rewriting the formula.
- **Red tint**: expected to work for free through `getOverlayCoords`. Verify that Cobblemon's model render passes the packed overlay through. If it hardcodes `NO_OVERLAY`, fix that call site.

### Step 4 - Test matrix (per Stonecutter version)

- [x] Wild Pokémon killed with a sword: tint, tip, poof, removed at ~20 ticks (1.21.1, single player)
- [ ] No leftover hitbox or interactable entity after the poof
- [x] Drops still appear (1.21.1)
- [ ] Wild Pokémon fainted **in battle**: unchanged Cobblemon behaviour
- [ ] Owned / player Pokémon death or faint: unchanged
- [ ] Dedicated server with the mod + client with the mod
- [ ] Dedicated server with the mod + client without the mod: no crash, server removal still works
- [ ] Client with the mod on a server without it: no crash, and note what you see

## Stonecutter notes

- `LivingEntityRenderer.setupRotations` gained a `scale` parameter around 1.20.5. Wrap any signature-sensitive mixin code in `//? if >=1.20.5 {` blocks.
- Each MC version pins its own Cobblemon version in `versions/<mc>/gradle.properties`. Cobblemon internals (class names, wild check, animation code) can differ per node. Keep version-specific findings in `NOTES.md`.
- Prefer `@Inject` / MixinExtras (`@WrapOperation`, `@ModifyExpressionValue`) over `@Overwrite` or `@Redirect`, for compatibility with other Cobblemon addons.

## Conventions

- Keep the mod minimal: no config file until it's needed. The likely first config option is "also apply in battle", which is currently out of scope.
- Every mixin goes through `WildDeathCheck` first. No scope logic duplicated across mixins.
- Mark mixins with `require = 1` only where a silent miss would be a real bug. Otherwise use `require = 0` and log a warning, so a Cobblemon update degrades gracefully instead of crashing.
- Before changing scope or adding features, re-read the "What this mod does" section.

## Out of scope (for now)

- Battle faints
- Custom particles, sounds, or a different tip direction or speed
- Replacing per-species animation JSON files
- KubeJS or datapack versions
