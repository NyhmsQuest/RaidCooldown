package net.vanillymc.raidCooldown;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.raid.RaidTriggerEvent;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.UUID;

public class RaidCooldown extends JavaPlugin implements Listener, CommandExecutor, TabCompleter {

    private static final int DEFAULT_COOLDOWN_SECONDS = 86400;
    private final HashMap<UUID, LocalDateTime> raidCooldowns = new HashMap<>();
    private FileConfiguration config;
    private File cooldownFile;
    private FileConfiguration cooldownConfig;
    private MiniMessage miniMessage;

    @Override
    public void onEnable() {
        this.saveDefaultConfig();
        config = this.getConfig();
        miniMessage = MiniMessage.miniMessage();

        // Set up the cooldown data file
        cooldownFile = new File(getDataFolder(), "cooldowns.yml");
        if (!cooldownFile.exists()) {
            try {
                cooldownFile.createNewFile();
            } catch (IOException e) {
                getLogger().severe("Could not create cooldowns.yml file.");
                e.printStackTrace();
            }
        }
        cooldownConfig = YamlConfiguration.loadConfiguration(cooldownFile);

        // Load cooldown data from file
        loadCooldownData();

        // Register the event listener
        getServer().getPluginManager().registerEvents(this, this);

        // Register the command executor and tab completer
        if (this.getCommand("raidcooldown") != null) {
            this.getCommand("raidcooldown").setExecutor(this);
            this.getCommand("raidcooldown").setTabCompleter(this);
        }
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 0) {
            if (!(sender instanceof Player)) {
                sender.sendMessage(miniMessage.deserialize(config.getString("messages.onlyPlayersMessage")));
                return true;
            }
            Player player = (Player) sender;
            sendCooldownStatus(player, player);
            return true;

        } else if (args.length == 1 && args[0].equalsIgnoreCase("reload")) {
            if (!sender.hasPermission("raidcooldown.reload")) {
                sender.sendMessage(miniMessage.deserialize(config.getString("messages.noPermissionMessage")));
                return true;
            }
            reloadConfig();
            config = getConfig();
            sender.sendMessage(miniMessage.deserialize(config.getString("messages.reloadMessage")));
            return true;

        } else if (args.length == 2) {
            if (args[0].equalsIgnoreCase("check")) {
                if (!sender.hasPermission("raidcooldown.check")) {
                    sender.sendMessage(miniMessage.deserialize(config.getString("messages.noPermissionMessage")));
                    return true;
                }

                Player target = Bukkit.getPlayer(args[1]);
                if (target == null) {
                    sender.sendMessage(miniMessage.deserialize(config.getString("messages.playerNotFoundMessage").replace("{player}", args[1])));
                    return true;
                }
                sendCooldownStatus(sender, target);
                return true;

            } else if (args[0].equalsIgnoreCase("reset")) {
                if (!sender.hasPermission("raidcooldown.reset")) {
                    sender.sendMessage(miniMessage.deserialize(config.getString("messages.noPermissionMessage")));
                    return true;
                }

                Player target = Bukkit.getPlayer(args[1]);
                if (target == null) {
                    sender.sendMessage(miniMessage.deserialize(config.getString("messages.playerNotFoundMessage").replace("{player}", args[1])));
                    return true;
                }

                // Reset the cooldown for the specified player
                raidCooldowns.remove(target.getUniqueId());
                cooldownConfig.set(target.getUniqueId().toString(), null);
                saveCooldownConfig();

                sender.sendMessage(miniMessage.deserialize(config.getString("messages.resetCooldownMessage").replace("{player}", target.getName())));
                target.sendMessage(miniMessage.deserialize(config.getString("messages.cooldownResetNotification")));
                return true;
            }
        }

        sender.sendMessage(miniMessage.deserialize(config.getString("messages.usage")));
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> completions = new ArrayList<>();
        if (args.length == 1) {
            if (sender.hasPermission("raidcooldown.reload") && "reload".startsWith(args[0].toLowerCase())) {
                completions.add("reload");
            }
            if (sender.hasPermission("raidcooldown.check") && "check".startsWith(args[0].toLowerCase())) {
                completions.add("check");
            }
            if (sender.hasPermission("raidcooldown.reset") && "reset".startsWith(args[0].toLowerCase())) {
                completions.add("reset");
            }
        } else if (args.length == 2) {
            if (args[0].equalsIgnoreCase("check") && sender.hasPermission("raidcooldown.check")) {
                for (Player player : Bukkit.getOnlinePlayers()) {
                    completions.add(player.getName());
                }
            } else if (args[0].equalsIgnoreCase("reset") && sender.hasPermission("raidcooldown.reset")) {
                for (Player player : Bukkit.getOnlinePlayers()) {
                    completions.add(player.getName());
                }
            }
        }
        return completions;
    }

    @EventHandler
    public void onRaidTrigger(RaidTriggerEvent event) {
        Player player = event.getPlayer();
        UUID playerUUID = player.getUniqueId();

        if (!canStartRaid(player)) {
            event.setCancelled(true);
            return;
        }

        // Start raid and save the cooldown time
        LocalDateTime now = LocalDateTime.now();
        raidCooldowns.put(playerUUID, now);
        saveCooldownData(playerUUID, now);
    }

    private boolean canStartRaid(Player player) {
        UUID playerUUID = player.getUniqueId();
        Duration remainingCooldown = getRemainingCooldown(playerUUID);

        // Adjust the check to only block if the cooldown is positive
        if (remainingCooldown.isZero() || remainingCooldown.isNegative()) {
            return true; // Cooldown is complete; allow the raid
        }

        // If the cooldown is positive, block the raid and notify the player
        player.sendMessage(formatCooldownMessage(playerUUID, true));
        return false;
    }

    private Duration getRemainingCooldown(UUID playerUUID) {
        LocalDateTime lastRaidTime = raidCooldowns.get(playerUUID);
        if (lastRaidTime == null) {
            return Duration.ZERO;
        }

        int cooldownSeconds = config.getInt("raidCooldownSeconds", DEFAULT_COOLDOWN_SECONDS);
        Duration remaining = Duration.between(LocalDateTime.now(), lastRaidTime.plusSeconds(cooldownSeconds));

        // If the remaining duration is negative, reset it to zero
        return remaining.isNegative() ? Duration.ZERO : remaining;
    }

    private void sendCooldownStatus(CommandSender sender, Player target) {
        sender.sendMessage(formatCooldownMessage(target.getUniqueId(), sender.equals(target)));
    }

    private Component formatCooldownMessage(UUID playerUUID, boolean isSelfCheck) {
        Duration remaining = getRemainingCooldown(playerUUID);
        String messagePath = isSelfCheck ? "messages.cooldownRemainingMessage" : "messages.cooldownRemainingOtherMessage";

        if (remaining.isZero() || remaining.isNegative()) {
            messagePath = isSelfCheck ? "messages.raidAvailableMessage" : "messages.raidAvailableOtherMessage";
        }

        // Replace the {player} placeholder with the actual player name
        String rawMessage = config.getString(messagePath, "");
        String formattedMessage = rawMessage.replace("{player}", Bukkit.getOfflinePlayer(playerUUID).getName());

        // Parse the formatted message with MiniMessage for colors and formatting
        return miniMessage.deserialize(formattedMessage)
                .replaceText(builder -> builder.matchLiteral("{time}").replacement(formatDuration(remaining)));
    }

    private Component formatDuration(Duration duration) {
        long hours = duration.toHours();
        long minutes = duration.minusHours(hours).toMinutes();
        long seconds = duration.minusHours(hours).minusMinutes(minutes).getSeconds();

        String hourSuffix = config.getString("messages.hour", "<#A180D0>ʜ ");
        String minuteSuffix = config.getString("messages.minute", "<#A180D0>ᴍ ");
        String secondSuffix = config.getString("messages.second", "<#A180D0>ꜱ");

        Component hoursComponent = miniMessage.deserialize(hours + hourSuffix);
        Component minutesComponent = miniMessage.deserialize(minutes + minuteSuffix);
        Component secondsComponent = miniMessage.deserialize(seconds + secondSuffix);

        return hoursComponent.append(minutesComponent).append(secondsComponent);
    }

    private void saveCooldownData(UUID playerUUID, LocalDateTime time) {
        cooldownConfig.set(playerUUID.toString(), time.toEpochSecond(ZoneOffset.UTC));
        saveCooldownConfig();
    }

    private void saveCooldownConfig() {
        try {
            cooldownConfig.save(cooldownFile);
        } catch (IOException e) {
            getLogger().severe("Could not save cooldown data.");
            e.printStackTrace();
        }
    }

    private void loadCooldownData() {
        for (String key : cooldownConfig.getKeys(false)) {
            try {
                UUID playerUUID = UUID.fromString(key);
                long epochSecond = cooldownConfig.getLong(key);
                LocalDateTime time = LocalDateTime.ofEpochSecond(epochSecond, 0, ZoneOffset.UTC);
                raidCooldowns.put(playerUUID, time);
            } catch (IllegalArgumentException e) {
                getLogger().warning("Invalid UUID in cooldown data: " + key);
            }
        }
    }

    @Override
    public void onDisable() {
        raidCooldowns.forEach(this::saveCooldownData);
        raidCooldowns.clear();
    }
}
