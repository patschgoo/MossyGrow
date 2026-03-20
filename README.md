# MossyGrow

MossyGrow is a legacy CraftBukkit/Poseidon plugin that turns player-placed cobblestone into mossy cobblestone after a configurable delay.

## Features

- Tracks cobblestone placed by players.
- Converts placed cobblestone into mossy cobblestone when either condition matches:
  - Near water (supports both flowing and stationary water).
  - Near mossy cobblestone.
- Separate configuration for each condition:
  - enable/disable
  - delay (seconds)
  - radius
- Global on/off toggle for spreading with command control.

## Compatibility

- Project Poseidon / CraftBukkit 1.7.3 era API (CB1060 style)
- Java 8 runtime

## Installation

1. Copy `builds/MossyGrow.jar` into your server `plugins` folder.
2. Start or restart the server.
3. Edit `plugins/MossyGrow/config.yml` as needed.
4. Run `/mossy reload` (or restart) after config changes.

## Commands

- `/mossy reload`
  - Reload plugin configuration.
  - Permission: `mossy.reload`

- `/mossy on`
  - Enable moss spreading globally.
  - Permission: `mossy.admin`

- `/mossy off`
  - Disable moss spreading globally.
  - Permission: `mossy.admin`

## Permissions

- `mossy.reload` (default: op)
- `mossy.admin` (default: op)

## Configuration

Default config:

```yaml
conversion:
  enabled: true
  near-water:
    enabled: true
    delay-seconds: 300
    radius: 1
  near-mossy-cobblestone:
    enabled: true
    delay-seconds: 180
    radius: 1
```

Meaning:

- `conversion.enabled`: Master switch for all spreading.
- `conversion.near-water.*`: Rules for water-based conversion checks.
- `conversion.near-mossy-cobblestone.*`: Rules for mossy-neighbor conversion checks.

## Build

From project root:

```bash
mvn -DskipTests package
```

Build outputs:

- `target/MossyGrow.jar`
- `builds/MossyGrow.jar` (if copied after build)

## Notes

- Only player-placed cobblestone is tracked.
- Nearby water does not need to be player-placed.
