package com.alilshady.tutientranphap.managers;

import com.alilshady.tutientranphap.TuTienTranPhap;
import com.booksaw.betterTeams.Team; // Thêm import cho BetterTeams
import com.gmail.nossr50.api.party.PartyAPI;
import com.massivecraft.factions.FPlayer;
import com.massivecraft.factions.FPlayers;
import com.massivecraft.factions.struct.Relation;
import com.palmergames.bukkit.towny.TownyAPI;
import com.palmergames.bukkit.towny.object.Resident;
import net.sacredlabyrinth.phaed.simpleclans.SimpleClans;
import net.sacredlabyrinth.phaed.simpleclans.managers.ClanManager;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.scoreboard.Scoreboard;

public class TeamManager {

    private final TuTienTranPhap plugin;
    private TeamPluginProvider activeProvider = TeamPluginProvider.NONE;
    private SimpleClans simpleClansApi;

    private enum TeamPluginProvider {
        FACTIONS,
        TOWNY,
        BETTERTEAMS, // Thêm provider mới
        SIMPLECLANS,
        MCMMO,
        ULTIMATETEAM, // Thêm provider cho UltimateTeam (để dành)
        VANILLA,
        NONE
    }

    public TeamManager(TuTienTranPhap plugin) {
        this.plugin = plugin;
        detectTeamPlugin();
    }

    private void detectTeamPlugin() {
        if (Bukkit.getPluginManager().isPluginEnabled("Factions")) {
            activeProvider = TeamPluginProvider.FACTIONS;
            plugin.getLogger().info("Phát hiện Factions! Đang tích hợp hệ thống team.");
        } else if (Bukkit.getPluginManager().isPluginEnabled("Towny")) {
            activeProvider = TeamPluginProvider.TOWNY;
            plugin.getLogger().info("Phát hiện Towny! Đang tích hợp hệ thống team.");
        } else if (Bukkit.getPluginManager().isPluginEnabled("BetterTeams")) {
            activeProvider = TeamPluginProvider.BETTERTEAMS;
            plugin.getLogger().info("Phát hiện BetterTeams! Đang tích hợp hệ thống team.");
        } else if (Bukkit.getPluginManager().isPluginEnabled("SimpleClans")) {
            Plugin scPlugin = Bukkit.getPluginManager().getPlugin("SimpleClans");
            if (scPlugin instanceof SimpleClans) {
                this.simpleClansApi = (SimpleClans) scPlugin;
                activeProvider = TeamPluginProvider.SIMPLECLANS;
                plugin.getLogger().info("Phát hiện SimpleClans! Đang tích hợp hệ thống team.");
            }
        } else if (Bukkit.getPluginManager().isPluginEnabled("mcMMO")) {
            activeProvider = TeamPluginProvider.MCMMO;
            plugin.getLogger().info("Phát hiện mcMMO! Đang tích hợp hệ thống party.");
        }
        /*
        // MÃ MẪU CHO ULTIMATETEAM - Bỏ comment khi bạn đã có API
        else if (Bukkit.getPluginManager().isPluginEnabled("UltimateTeam")) {
            activeProvider = TeamPluginProvider.ULTIMATETEAM;
            plugin.getLogger().info("Phát hiện UltimateTeam! Đang tích hợp hệ thống team.");
        }
        */
        else {
            activeProvider = TeamPluginProvider.VANILLA;
            plugin.getLogger().info("Không phát hiện plugin team nào, sử dụng Scoreboard team mặc định.");
        }
    }

    public boolean isAlly(Player player1, Player player2) {
        if (player1 == null || player2 == null) return false;
        if (player1.getUniqueId().equals(player2.getUniqueId())) return true;

        switch (activeProvider) {
            case FACTIONS:
                return isAllyFactions(player1, player2);
            case TOWNY:
                return isAllyTowny(player1, player2);
            case BETTERTEAMS:
                return isAllyBetterTeams(player1, player2);
            case SIMPLECLANS:
                return isAllySimpleClans(player1, player2);
            case MCMMO:
                return isAllyMcMMO(player1, player2);
            /*
            // MÃ MẪU CHO ULTIMATETEAM
            case ULTIMATETEAM:
                return isAllyUltimateTeam(player1, player2);
            */
            case VANILLA:
                return isAllyVanilla(player1, player2);
            default:
                return false;
        }
    }

    // --- Các phương thức isAlly... cũ giữ nguyên ---
    private boolean isAllyFactions(Player p1, Player p2) { /* ... giữ nguyên ... */ }
    private boolean isAllyTowny(Player p1, Player p2) { /* ... giữ nguyên ... */ }
    private boolean isAllySimpleClans(Player p1, Player p2) { /* ... giữ nguyên ... */ }
    private boolean isAllyMcMMO(Player p1, Player p2) { /* ... giữ nguyên ... */ }
    private boolean isAllyVanilla(Player p1, Player p2) { /* ... giữ nguyên ... */ }


    // --- Phương thức MỚI cho BetterTeams ---
    private boolean isAllyBetterTeams(Player player1, Player player2) {
        Team team1 = Team.getTeam(player1);
        Team team2 = Team.getTeam(player2);

        if (team1 == null || team2 == null) {
            return false;
        }
        if (team1.equals(team2)) {
            return true;
        }
        return team1.isAlly(team2);
    }

    /*
    // --- MÃ MẪU cho UltimateTeam ---
    // Bạn cần tìm hiểu API của UltimateTeam để viết logic chính xác cho hàm này
    private boolean isAllyUltimateTeam(Player player1, Player player2) {
        // LƯU Ý: Đây là mã giả định. Bạn cần thay thế bằng các phương thức API thực tế của UltimateTeam.
        // Ví dụ:
        // me.fabio.ultimateteam.api.UltimateTeamAPI api = ...;
        // Optional<Team> team1 = api.getTeamOfPlayer(player1.getUniqueId());
        // Optional<Team> team2 = api.getTeamOfPlayer(player2.getUniqueId());
        //
        // if (team1.isPresent() && team2.isPresent()) {
        //     if (team1.get().equals(team2.get())) return true;
        //     // Kiểm tra quan hệ đồng minh nếu có
        //     // return team1.get().isAlliedTo(team2.get());
        // }

        plugin.getLogger().warning("Tích hợp UltimateTeam chưa được hoàn thiện!");
        return false;
    }
    */
}