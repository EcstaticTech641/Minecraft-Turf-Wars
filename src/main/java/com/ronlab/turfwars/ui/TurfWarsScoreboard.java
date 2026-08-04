package com.ronlab.turfwars.ui;

import com.ronlab.turfwars.session.TurfWarsSession;
import io.papermc.paper.scoreboard.numbers.NumberFormat;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.scoreboard.DisplaySlot;
import org.bukkit.scoreboard.Objective;
import org.bukkit.scoreboard.Scoreboard;

public class TurfWarsScoreboard {

    private final Scoreboard scoreboard;
    private final Objective objective;

    public TurfWarsScoreboard() {
        if (Bukkit.getScoreboardManager() != null) {
            this.scoreboard = Bukkit.getScoreboardManager().getNewScoreboard();
            this.objective = scoreboard.registerNewObjective("turfwars", "dummy", "§b§lTURF WARS");
            this.objective.setDisplaySlot(DisplaySlot.SIDEBAR);
            this.objective.numberFormat(NumberFormat.blank());
        } else {
            this.scoreboard = null;
            this.objective = null;
        }
    }

    public void update(TurfWarsSession session) {
        if (scoreboard == null || objective == null) return;

        for (String entry : scoreboard.getEntries()) {
            scoreboard.resetScores(entry);
        }

        double totalLength = TurfWarsSession.MAX_Z - TurfWarsSession.MIN_Z;
        double blackControl = session.getDivideLine() - TurfWarsSession.MIN_Z;

        int blackPercent = (int) Math.max(0, Math.min(100, (blackControl / totalLength) * 100));
        int goldPercent = 100 - blackPercent;

        long elapsedSec = (System.currentTimeMillis() - session.getSessionStartTimeMs()) / 1000;
        long totalMatchSec = 600;
        long remainingSec = Math.max(0, totalMatchSec - elapsedSec);
        String timeFormatted = String.format("%02d:%02d", remainingSec / 60, remainingSec % 60);

        objective.getScore(" ").setScore(9);
        objective.getScore("§fTime Remaining: §e" + timeFormatted).setScore(8);
        objective.getScore("  ").setScore(7);
        objective.getScore("§fTerritory Control:").setScore(6);
        objective.getScore("§8Black: §7" + blackPercent + "%").setScore(5);
        objective.getScore("§6Gold: §e" + goldPercent + "%").setScore(4);
        objective.getScore("   ").setScore(3);
        objective.getScore("§fTeam Kills:").setScore(2);
        objective.getScore("§8Black Kills: §a" + session.getBlackKills()).setScore(1);
        objective.getScore("§6Gold Kills: §a" + session.getGoldKills()).setScore(0);
    }

    public void applyTo(Player player) {
        if (scoreboard != null && player != null && player.isOnline()) {
            player.setScoreboard(scoreboard);
        }
    }

    public Scoreboard getScoreboard() {
        return scoreboard;
    }

    public Objective getObjective() {
        return objective;
    }
}
