# Explorer's Ender Chest

Open your **real ender chest** straight from the inventory screen. No chest nearby, no commands,
no new items — just a button. (Internally codenamed *EnderPocket*.)

![Fabric](https://img.shields.io/badge/loader-Fabric-blue) ![Minecraft 26.2](https://img.shields.io/badge/minecraft-26.2-green)

## What it does

- Adds a small ender-chest button next to your inventory. Click it (or bind a key) and a
  27-slot panel opens to the right with your **actual** ender chest inventory — perfectly in sync
  with every physical ender chest you use anywhere else.
- Shift-click works both ways, chest-style: deposit from your inventory while the panel is open,
  pull items back out with a click.
- **Earned, not given:** the button only appears after you've opened a real ender chest at least
  once in that world. The unlock is per-world, saves with your player, and survives death.
- Smart fitting: if there isn't enough horizontal space, the panel gently shrinks to fit —
  and with the recipe book open, the whole layout scales down smoothly instead of clipping.
- Plays nice with other inventory mods (built and tested alongside
  [Terrastorage](https://modrinth.com/mod/terrastorage)), and follows your resource pack's GUI
  theme automatically.

## Multiplayer

Fully multiplayer-safe — all item movement goes through the vanilla server-side container logic
(no client-side cheating, no dupes). The server needs the mod too; on servers without it, the
button simply hides itself.

## Resource pack integration

The mod follows your pack's GUI theme automatically — the panel window is assembled at
runtime from your pack's own `generic_54.png` chest texture. For packs that restyle the
inventory layout more aggressively, everything else is overridable too:

- **Button icon**: `assets/enderpocket/textures/gui/sprites/button/ender_pocket.png` (16×16)
- **Dedicated panel window**: `assets/enderpocket/textures/gui/ender_panel.png` (176×78) —
  if present, it's used instead of the generic_54 composite. Slots sit at x=8+col·18, y=18+row·18.
- **Positions**: `assets/enderpocket/gui_layout.json` — all coordinates relative to the
  inventory GUI's top-left corner:

```json
{
	"button_row_spots": [[128, 61], [152, 61]],
	"button_corner": [184, 0],
	"panel_offset": [0, 0],
	"effects_clearance": 26
}
```

`button_row_spots` are tried in order and used when nothing else (e.g. another mod's buttons)
occupies them; `button_corner` is the fallback spot outside the GUI. `panel_offset` shifts the
open panel (and its slot hitboxes) by x/y pixels. `effects_clearance` is how far the potion
effect list gets pushed down when the button uses the corner spot.

## Requirements

- Minecraft **26.2**
- Fabric Loader **0.19.3+**
- Fabric API

## License

CC0-1.0 — do whatever you want with it.
