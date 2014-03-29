package me.vaqxine.PartyMechanics;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.CopyOnWriteArrayList;

import me.vaqxine.HealthMechanics.HealthMechanics;
import me.vaqxine.Hive.Hive;
import me.vaqxine.InstanceMechanics.InstanceMechanics;
import me.vaqxine.KarmaMechanics.KarmaMechanics;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.scoreboard.DisplaySlot;
import org.bukkit.scoreboard.Objective;
import org.bukkit.scoreboard.Score;
import org.bukkit.scoreboard.Scoreboard;

public class Party {
    Player leader;
    CopyOnWriteArrayList<Player> players = new CopyOnWriteArrayList<Player>();
    Scoreboard party_ui;

    public Party(Player leader) {
        this.leader = leader;
        party_ui = leader.getScoreboard();
        addPlayer(leader);
    }

    public List<Player> getPartyMembers() {
        return players;
    }

    public void addPlayer(Player pl) {
        if (players.contains(pl))
            return;
        players.add(pl);
        PartyMechanics.party_map.put(pl.getName(), this);
        String p_name = pl.getName();
        if (getPartyList().size() == 1) {
            // Just the leader so dont really do anything pls ty
            return;
        }
        System.out.print("SET SCOREBOARD TO " + pl.getName() + " FROM ADD PLAYER OBJECTIVES: " + party_ui.getObjectives().toString());
        Objective obj = party_ui.getObjective(DisplaySlot.SIDEBAR);
        Score hp = obj.getScore(Bukkit.getOfflinePlayer(ChatColor.stripColor(p_name)));
        hp.setScore(HealthMechanics.getPlayerHP(pl.getName()));
        pl.setScoreboard(party_ui);

        int party_count = getPartyMembers().size();

        for (String s : getPartyList()) {
            if (s.equalsIgnoreCase(pl.getName())) {
                continue;
            }
            if (Bukkit.getPlayer(s) == null) {
                continue;
            }
            final Player p_mem = Bukkit.getPlayer(s);
            if (party_count == 4) {
                p_mem.sendMessage(ChatColor.GRAY + "You now have " + ChatColor.BOLD + "4/8" + ChatColor.GRAY
                        + " party members. You will now recieve increased drop rates when fighting together.");
            }
            if (party_count == 8) {
                p_mem.sendMessage(ChatColor.GRAY + "You now have " + ChatColor.BOLD + "8/8" + ChatColor.GRAY
                        + " party members. You will now recieve +5% DMG/ARMOR AND " + ChatColor.UNDERLINE + "GREATLY" + ChatColor.GRAY
                        + " increased drop rates when fighting together.");
            }

            p_mem.setScoreboard(party_ui);
            /*
             * this.getServer().getScheduler().scheduleSyncDelayedTask(this, new Runnable() { public void run() { sendPartyColor(pl, p_mem, true); } }, 20L);
             */
        }
    }

    public void removePlayer(Player p) {
        players.remove(p);
        PartyMechanics.party_map.remove(p.getName());
        // Removes the invite
        // Removes the party only chat
        PartyMechanics.party_only.remove(p.getName());
        KarmaMechanics.sendAlignColor(p, p);

        Objective obj = party_ui.getObjective(DisplaySlot.SIDEBAR);
        if (obj != null) {
            Score hp = obj.getScore(Bukkit.getOfflinePlayer(p.getName()));
            hp.setScore(0);
        }

        InstanceMechanics.teleport_on_load.remove(p.getName());

        if (InstanceMechanics.saved_location_instance.containsKey(p.getName())) {
            // Inside an instance.
            p.teleport(InstanceMechanics.saved_location_instance.get(p.getName()));
            InstanceMechanics.saved_location_instance.remove(p.getName());
        }
        InstanceMechanics.removeFromInstanceParty(p.getName());

        if (p.getName().equalsIgnoreCase(leader.getName()) && getPartyList().size() > 0) {
            String new_leader = "";
            int size_mod = 1;
            if (getPartyList().size() <= 1) {
                size_mod = 0;
            }
            int party_index = new Random().nextInt(getPartyList().size() - size_mod);
            List<String> remaining_members = new ArrayList<String>();
            for (String s : getPartyList()) {
                if (s.equalsIgnoreCase(p.getName())) {
                    continue;
                }
                remaining_members.add(s);
            }
            leader = Bukkit.getPlayer(remaining_members.get(party_index));

            // TODO MOVES THIS

            for (String s : remaining_members) {
                if (Bukkit.getPlayer(s) != null) {
                    Player pty_mem = Bukkit.getPlayer(s);
                    if (!(pty_mem.getScoreboard().getPlayers().contains(Bukkit.getOfflinePlayer(s)))) {
                        pty_mem.setScoreboard(Bukkit.getPlayer(s).getScoreboard());
                    }
                    /*
                     * if(!new_ui.hasPlayerAdded(pty_mem)){ new_ui.showToPlayer(pty_mem); }
                     */
                    pty_mem.sendMessage(ChatColor.LIGHT_PURPLE.toString() + "<" + ChatColor.BOLD + "P" + ChatColor.LIGHT_PURPLE + ">" + ChatColor.GRAY + " "
                            + p.getName() + ChatColor.GRAY.toString() + " has " + ChatColor.LIGHT_PURPLE + ChatColor.UNDERLINE + "left"
                            + ChatColor.GRAY.toString() + " your party.");
                    pty_mem.sendMessage(ChatColor.LIGHT_PURPLE.toString() + "<" + ChatColor.BOLD + "P" + ChatColor.LIGHT_PURPLE + "> " + ChatColor.GRAY
                            + ChatColor.LIGHT_PURPLE.toString() + new_leader + ChatColor.GRAY.toString() + " has been promoted to " + ChatColor.UNDERLINE
                            + "Party Leader");
                }
            }
        } else {
            for (String s : getPartyList()) {
                if (Bukkit.getPlayer(s) != null && s != p.getName()) {
                    Player pty_mem = Bukkit.getPlayer(s);
                    pty_mem.sendMessage(ChatColor.LIGHT_PURPLE.toString() + "<" + ChatColor.BOLD + "P" + ChatColor.LIGHT_PURPLE + ">" + ChatColor.GRAY + " "
                            + p.getName() + ChatColor.GRAY.toString() + " has " + ChatColor.RED + ChatColor.UNDERLINE + "left" + ChatColor.GRAY.toString()
                            + " your party.");
                }
            }
        }

        if (!Hive.pending_upload.contains(p.getName())) {
            HealthMechanics.setOverheadHP(p, HealthMechanics.getPlayerHP(p.getName()));
        }

    }

    public Player getLeader() {
        return leader;
    }

    public List<String> getPartyList() {
        List<String> to_return = new ArrayList<String>();
        for (Player p : players) {
            to_return.add(p.getName());
        }
        return to_return;
    }

    public String getPlayerName(Player p) {
        return ChatColor.WHITE.toString() + (getLeader().getName().equalsIgnoreCase(p.getName()) ? ChatColor.BOLD.toString() + p.getName() : p.getName());
    }

    public void updateScoreboard(Update update) {
        if (update == Update.HEALTH) {
            Scoreboard sb = party_ui;
            Objective obj = sb.getObjective(DisplaySlot.SIDEBAR);
            if (obj == null)
                return;
            for (Player p : getPartyMembers()) {
                sb.resetScores(Bukkit.getOfflinePlayer(getPlayerName(p)));
                obj.getScore(Bukkit.getOfflinePlayer(getPlayerName(p))).setScore(HealthMechanics.getPlayerHP(ChatColor.stripColor(p.getName())));
                // p.setScoreboard(sb);
            }
        }
    }

    public enum Update {
        HEALTH, LEADER_CHANGE;
    }
}
