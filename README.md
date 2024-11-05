# RaidCooldown

A Minecraft Paper plugin that adds a configurable cooldown for starting raids, ensuring players can only trigger a raid after a specified cooldown period.

## Features

- **Cooldown Management**: Players must wait a configurable amount of time between starting raids.
- **Command Support**: Players can check their remaining cooldown time and reset cooldowns for other players.
- **Configurable Messages**: Customize messages for cooldown notifications, permissions errors, and other events through the configuration file.
- **Persistence**: Cooldown data is saved in a YAML file, ensuring that cooldowns are maintained across server restarts.

## Commands

- `/raidcooldown`: Displays the cooldown status of the player issuing the command.
- `/raidcooldown check <player>`: Checks the cooldown status of the specified player (requires permission).
- `/raidcooldown reset <player>`: Resets the cooldown for the specified player (requires permission).
- `/raidcooldown reload`: Reloads the plugin's configuration file (requires permission).

## Permissions

- `raidcooldown.check`: Allows the player to check another player's cooldown status.
- `raidcooldown.reset`: Allows the player to reset another player's cooldown.
- `raidcooldown.reload`: Allows the player to reload the plugin's configuration.

## Configuration

The plugin's main configuration file can be found at `plugins/RaidCooldown/config.yml`. The default settings include the cooldown duration and message formats.

### Example Configuration

```yaml
# config.yml
raidCooldownSeconds: 86400  # 24 hours in seconds
messages:
  onlyPlayersMessage: "<red>You must be a player to use this command."
  noPermissionMessage: "<red>You do not have permission to use this command."
  reloadMessage: "<green>Configuration reloaded successfully."
  playerNotFoundMessage: "<red>Player {player} not found."
  resetCooldownMessage: "<green>Cooldown for player {player} has been reset."
  cooldownResetNotification: "<green>Your cooldown has been reset by {player}."
  cooldownRemainingMessage: "<yellow>Your cooldown remaining: {time}."
  cooldownRemainingOtherMessage: "<yellow>Cooldown remaining for {player}: {time}."
  raidAvailableMessage: "<green>You can start a raid now!"
  raidAvailableOtherMessage: "<green>{player} can start a raid now!"
  usage: "<yellow>Usage: /raidcooldown [check/reset/reload]"
  hour: "<#A180D0>ʜ "
  minute: "<#A180D0>ᴍ "
  second: "<#A180D0>ꜱ"
```

## Installation

1. Download the latest release of `RaidCooldown` from the [Releases](https://github.com/yourusername/RaidCooldown/releases) section.
2. Place the downloaded `.jar` file into the `plugins` folder of your Paper server.
3. Start or restart your server to generate the configuration files.
4. Adjust the settings in `config.yml` as needed.

## Contributing

Contributions are welcome! Please feel free to submit issues, feature requests, or pull requests.

## License

This project is licensed under the MIT License. See the [LICENSE](LICENSE) file for more information.

## Contact

For questions or support, feel free to open an issue on GitHub or reach out to me directly.
