# Celestial Dragon

A NeoForge 26.2 boss mod built around a wingless Chinese dragon. The boss has
500 health, persistent flight, a visible boss bar, and a deeply segmented model
whose motion travels from its head through sixteen body bones and four tail
bones.

## Encounter

- **Rush** — a short telegraph followed by a straight, high-speed charge that
  deals 22 contact damage.
- **Coiling blast** — circles and tightens around its target, charges, then
  releases an 11-block shockwave for up to 38 damage and heavy knockback.
- **Fireball volley** — launches three explosion-power-2 large fireballs.
- **Lightning strike (stage one)** — marks a target location, charges for 1.6
  seconds, then calls down a non-igniting lightning bolt for 24 area damage.
- **Sonic boom (stage two)** — unlocks only below 50% health, traces to the
  first blocking surface, and deals repeated sonic damage along the beam.

It spawns very rarely and alone in biomes in `#minecraft:is_mountain`, above
sea level with open sky. Natural bosses keep 256 blocks apart.

Operators can summon one at their position or at an explicit position:

```mcfunction
/dragon-boss
/dragon-boss ~ ~10 ~
```

The normal entity command also works:

```mcfunction
/summon dragon:dragon_boss
```

## Model and animation

The runtime model contains 151 rendered cuboids across 56 bones. It includes a
continuous axial chain, articulated jaw, mane, branching antlers, segmented
whiskers, four jointed legs with individual talons, dorsal spines, and a flame
tail. Procedural animation covers idle flight, speed-sensitive side-to-side and
vertical waves through all 20 axial/tail joints, rush straightening, a full
coiling windup/release, fireball recoil, a horn-led lightning charge, and sonic
boom aiming. Every leg has its own phase and cadence, with separately animated
upper leg, lower leg, and paw plus attack-specific reaching, bracing, tucking,
recoil, and release poses.

The editable Blockbench bundle is in
[`artifacts/chinese_dragon_boss/build`](artifacts/chinese_dragon_boss/build).
The deterministic `img2blockbench` source has 96 purposeful cuboids (the
compiler maximum), 51 bones, a 512×512 atlas, Bedrock geometry, audit metadata,
and a ZIP. Multi-angle audit renders are in
[`artifacts/chinese_dragon_boss/renders`](artifacts/chinese_dragon_boss/renders).

## Build and test

```bash
./gradlew build
./gradlew runGameTestServer
./gradlew runClient
```

The GameTests cover boss creation/flight setup, the damaging coiled ultimate,
the exact 50% phase boundary, mid-windup phase changes, and lightning damage.
The natural-spawn biome modifier and entity loot table are under
`src/main/resources/data/dragon`.

## Reference provenance

The direct pixel-cuboid concept is original to this project. Anatomical and
motion research used CC0/Open Access works from the Smithsonian National Museum
of Asian Art, the Cleveland Museum of Art, and The Metropolitan Museum of Art.
Links and notes are recorded in
[`artifacts/chinese_dragon_boss/references/README.md`](artifacts/chinese_dragon_boss/references/README.md).
