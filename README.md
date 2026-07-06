# EnderPocket

Open your **real ender chest** straight from the inventory screen. No chest nearby, no commands,
no new items — just a button.

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

## Requirements

- Minecraft **26.2**
- Fabric Loader **0.19.3+**
- Fabric API

## License

CC0-1.0 — do whatever you want with it.
