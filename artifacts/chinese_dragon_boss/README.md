# Chinese Dragon Boss — model deliverables

## Ready-to-open files

- `chinese_dragon_boss_delivery.zip` — ready-built mod, model files, animation
  sidecar, concept, provenance, and all review renders in one archive.
- `dragon-1.0.0.jar` — ready-built NeoForge 26.2 mod.
- `build/chinese_dragon_boss.bbmodel` — native editable Blockbench model.
- `build/chinese_dragon_boss.png` — generated 512×512 pixel texture atlas.
- `build/chinese_dragon_boss.geo.json` — Bedrock geometry export.
- `build/chinese_dragon_boss.model-spec.json` — portable source specification.
- `build/chinese_dragon_boss.audit.json` — structural audit.
- `build/chinese_dragon_boss.manifest.json` — hashes and build provenance.
- `build/chinese_dragon_boss.zip` — the complete deterministic compiler bundle.
- `animations/chinese_dragon_boss.animation.json` — five Blockbench/Bedrock
  animation clips matching the portable rig's bone names.

The model uses the compiler maximum of 96 cuboids and 51 animation-ready bones.
The separate in-game Java model uses 151 cuboids across 56 bones so its flight
and combat poses can remain expressive without being constrained by the
portable compiler format.

## Rebuild

From the repository root:

```bash
python3 artifacts/chinese_dragon_boss/work/render_concept.py
python3 -m img2blockbench probe artifacts/chinese_dragon_boss/references/chinese_dragon_concept.png \
  --output artifacts/chinese_dragon_boss/work/reference.json
python3 artifacts/chinese_dragon_boss/work/build_model_spec.py
python3 -m img2blockbench validate artifacts/chinese_dragon_boss/work/model-spec.json --strict
python3 -m img2blockbench preview-threejs artifacts/chinese_dragon_boss/work/model-spec.json \
  --output artifacts/chinese_dragon_boss/work/createChineseDragonBossModel.ts
python3 -m img2blockbench build artifacts/chinese_dragon_boss/work/model-spec.json \
  --output artifacts/chinese_dragon_boss/build
python3 artifacts/chinese_dragon_boss/work/render_audit_views.py
python3 artifacts/chinese_dragon_boss/work/build_animation_json.py
python3 artifacts/chinese_dragon_boss/work/render_animation_previews.py
```

`renders/` contains front, back, left, right, isometric, bilateral head, and
joint-closeup review images. It also includes isometric, top, and side playback
previews of the two-axis travelling flight wave and independently phased legs.
`references/README.md` records the visual research sources and licensing.
