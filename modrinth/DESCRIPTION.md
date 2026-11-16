RawCarry is a lightweight Paper plugin that lets players carry each other on their shoulders. Pick up another player, move them around, and put them down safely — or throw them.

## Features

- Pick up another player with `/carry`
- Pick up another player by sneaking and right-clicking them
- Put the carried player down with `/uncarry`
- Put the carried player down by sneaking
- Throw the carried player 7-8 blocks in your look direction by sneaking and left-clicking
- Carry a whole chain of players as one tower with `/train`
- Opt out of being picked up with `/carry toggle`
- Weight system that slows carriers down, growing with tower depth
- Optional cloud particles on carry, drop and yeet
- Maximum distance between carrier and passenger
- Stacking limit for how many players can be carried in one chain
- Cooldown between carry actions
- Optional fall damage protection for carried players

## Commands

| Command | Description |
| --- | --- |
| `/carry <player>` | Carry another player on your shoulders |
| `/carry toggle` | Stop other players from picking you up |
| `/uncarry` | Stop carrying the player on your shoulders |
| `/train <player> <player>...` | Carry a chain of players as a single tower |

Throwing is a player action, not a command: while carrying someone, sneak and left-click to throw them in the direction you are looking. Sneaking without left-clicking puts the player down after a short delay.

Throwing never damages the player you click on — the attack is cancelled and the throw happens instead.

The `/carry toggle` state is kept in memory only. It is not saved to disk and resets when the server restarts.

## Permissions

| Permission | Default | Description |
| --- | --- | --- |
| `rawcarry.use` | `true` | Allows the use of `/carry`, `/uncarry` and `/train` |

## Installation

1. Download the latest `raw-carry-1.0.1.jar`.
2. Drop the jar into your server's `plugins/` folder.
3. Restart the server. A default `config.yml` is generated on first start.

Requires Paper 1.21 or newer and Java 21 or newer.

## Configuration

| Option | Default | Description |
| --- | --- | --- |
| `max-distance` | `3.0` | Maximum distance between carrier and passenger |
| `max-stack` | `3` | Maximum number of players that can be carried in one chain |
| `cooldown-seconds` | `2` | Cooldown in seconds between carry actions |
| `fall-damage-protection` | `true` | Protects carried players from fall damage |
| `weight-system` | `true` | Gives carriers Slowness while carrying; the level grows with tower depth |
| `weight-slowdown-level` | `1` | Base Slowness amplifier applied to the carrier |
| `yeet-power` | `1.2` | Horizontal launch strength of the sneak left-click throw |
| `yeet-vertical` | `0.45` | Upward boost of the sneak left-click throw |
| `effects` | `true` | Shows cloud particles on carry, drop and yeet |

## License

RawCarry is released under the [MIT License](https://opensource.org/license/mit).
