package com.alilshady.tutientranphap.managers;

import com.alilshady.tutientranphap.TuTienTranPhap;
import com.booksaw.betterTeams.Team;
// import com.gmail.nossr50.api.party.PartyAPI; // TẠM VÔ HIỆU HÓA
import net.sacredlabyrinth.phaed.simpleclans.SimpleClans;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.scoreboard.Scoreboard;

public class TeamManager {

    private final TuTienTranPhap plugin;
    private TeamPluginProvider activeProvider = TeamPluginProvider.NONE;
    private SimpleClans simpleClansApi;

    private enum TeamPluginProvider {
        BETTERTEAMS,
        SIMPLECLANS,
        MCMMO, // Vẫn giữ lại để logic không lỗi
        VANILLA,
        NONE
    }

    public TeamManager(TuTienTranPhap plugin) {
        this.plugin = plugin;
        detectTeamPlugin();
    }

    private void detectTeamPlugin() {
        if (Bukkit.getPluginManager().isPluginEnabled("BetterTeams")) {
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
        } else {
            activeProvider = TeamPluginProvider.VANILLA;
            plugin.getLogger().info("Không phát hiện plugin team nào, sử dụng Scoreboard team mặc định.");
        }
    }

    public boolean isAlly(Player player1, Player player2) {
        if (player1 == null || player2 == null) return false;
        if (player1.getUniqueId().equals(player2.getUniqueId())) return true;

        switch (activeProvider) {
            case BETTERTEAMS:
                return isAllyBetterTeams(player1, player2);
            case SIMPLECLANS:
                return isAllySimpleClans(player1, player2);
            case MCMMO:
                return isAllyMcMMO(player1, player2); // SỬA Ở ĐÂY
            case VANILLA:
                return isAllyVanilla(player1, player2);
            default:
                return false;
        }
    }

    private boolean isAllySimpleClans(Player p1, Player p2) {
        if (simpleClansApi == null) return false;
        net.sacredlabyrinth.phaed.simpleclans.Clan player1Clan = simpleClansApi.getClanManager().getClanByPlayerUniqueId(p1.getUniqueId());
        net.sacredlabyrinth.phaed.simpleclans.Clan player2Clan = simpleClansApi.getClanManager().getClanByPlayerUniqueId(p2.getUniqueId());
        if (player1Clan == null || player2Clan == null) {
            return false;
        }
        return player1Clan.isAlly(player2Clan.getTag());
    }

    private boolean isAllyMcMMO(Player p1, Player p2) {
        // TẠM VÔ HIỆU HÓA LOGIC - Sẽ không gây lỗi build
        // Khi nào có thư viện, bạn có thể bỏ comment dòng dưới và xóa dòng "return false;"
        // return PartyAPI.inSameParty(p1, p2);
        return false;
    }

    private boolean isAllyVanilla(Player p1, Player p2) {
        Scoreboard scoreboard = Bukkit.getScoreboardManager().getMainScoreboard();
        org.bukkit.scoreboard.Team team1 = scoreboard.getEntryTeam(p1.getName());
        org.bukkit.scoreboard.Team team2 = scoreboard.getEntryTeam(p2.getName());
        return team1 != null && team1.equals(team2);
    }

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
}