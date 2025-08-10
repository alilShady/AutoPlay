// src/main/java/com/alilshady/tutientranphap/commands/CommandTabCompleter.java
package com.alilshady.tutientranphap.commands;

import com.alilshady.tutientranphap.TuTienTranPhap;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.util.StringUtil;
import org.jetbrains.annotations.NotNull;
import javax.annotation.Nullable;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

public class CommandTabCompleter implements TabCompleter {

    private final TuTienTranPhap plugin;
    private static final List<String> SUB_COMMANDS = Arrays.asList("reload", "give", "test");

    public CommandTabCompleter(TuTienTranPhap plugin) {
        this.plugin = plugin;
    }

    @Nullable
    @Override
    public List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String alias, @NotNull String[] args) {
        final List<String> completions = new ArrayList<>();

        // Gợi ý cho lệnh con (reload, give, test)
        if (args.length == 1) {
            StringUtil.copyPartialMatches(args[0], SUB_COMMANDS, completions);
            Collections.sort(completions);
            return completions;
        }

        // Gợi ý cho các tham số của lệnh /ttp give
        if (args[0].equalsIgnoreCase("give")) {
            // Gợi ý tên người chơi: /ttp give <player>
            if (args.length == 2) {
                List<String> playerNames = Bukkit.getOnlinePlayers().stream()
                        .map(Player::getName)
                        .collect(Collectors.toList());
                StringUtil.copyPartialMatches(args[1], playerNames, completions);
                Collections.sort(completions);
                return completions;
            }
            // Gợi ý ID trận pháp: /ttp give <player> <formation_id>
            else if (args.length == 3) {
                List<String> formationIds = new ArrayList<>(plugin.getFormationManager().getAllFormationIds());
                StringUtil.copyPartialMatches(args[2], formationIds, completions);
                Collections.sort(completions);
                return completions;
            }
            // Gợi ý số lượng: /ttp give <player> <formation_id> [amount]
            else if (args.length == 4) {
                List<String> amounts = Arrays.asList("1", "16", "32", "64");
                StringUtil.copyPartialMatches(args[3], amounts, completions);
                return completions;
            }
        }

        // Gợi ý cho các tham số của lệnh /ttp test
        if (args[0].equalsIgnoreCase("test")) {
            // Gợi ý ID trận pháp: /ttp test <formation_id>
            if (args.length == 2) {
                List<String> formationIds = new ArrayList<>(plugin.getFormationManager().getAllFormationIds());
                StringUtil.copyPartialMatches(args[1], formationIds, completions);
                Collections.sort(completions);
                return completions;
            }
        }

        return Collections.emptyList(); // Không có gợi ý
    }
}