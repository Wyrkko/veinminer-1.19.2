# Veinminer

Just a veinminer. By Jupkke. Minecraft 1.19.2, Forge 43.x.

## Features
- Auto-detects every ore, vanilla or modded (uses the `forge:ores` tag, with a name-based fallback), no config needed.
- In-game config screen: Mods list -> Veinminer -> Config, or `/veinminer`.
- Activation: Sneak (default), Always On, or Keybind (default key: V).
- Optional logs and leaves, plus a whitelist and blacklist (blocks, `#tags`, or `modid:*` for a whole mod).
- Max vein size, extra hunger cost, tool durability protection, correct-tool requirement.

## Commands (client side)
- `/veinminer` or `/veinminer config`: open the config screen
- `/veinminer toggle`: turn veinmining on/off
- `/veinminer mode <sneak|always|keybind>`

## Building with GitHub
1. Push this folder to a GitHub repo.
2. Open the **Actions** tab, run **Build** (it also runs on every push).
3. Download the `jupkke-veinminer` artifact: the jar inside goes in your `mods` folder.
