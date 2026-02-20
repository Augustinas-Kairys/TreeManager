package com.trees;

import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import me.clip.placeholderapi.expansion.PlaceholderExpansion;

public class TreeManagerPlaceholder extends PlaceholderExpansion {

    private final TreeManagerPlugin plugin;

    public TreeManagerPlaceholder(TreeManagerPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public @NotNull String getIdentifier() {
        return "treemanager";
    }

    @Override
    public @NotNull String getAuthor() {
        return "Pope";
    }

    @Override
    public @NotNull String getVersion() {
        return "1.0.0";
    }

    @Override
    public boolean persist() {
        return true;
    }

    @Override
    public boolean canRegister() {
        return true;
    }

    @Override
    public String onPlaceholderRequest(Player player, @NotNull String params) {
        if (player == null) {
            return "";
        }

        // treemanager_access_<zone> - returns "yes" or "no"
        if (params.startsWith("access_")) {
            String zoneName = params.substring(7);
            boolean hasAccess = plugin.hasAccessToZone(player, zoneName);
            return hasAccess ? "yes" : "no";
        }

        // treemanager_hasaccess_<zone> - returns "true" or "false"
        if (params.startsWith("hasaccess_")) {
            String zoneName = params.substring(10);
            boolean hasAccess = plugin.hasAccessToZone(player, zoneName);
            return String.valueOf(hasAccess);
        }

        // treemanager_requiredrank_<zone> - returns required rank name
        if (params.startsWith("requiredrank_")) {
            String zoneName = params.substring(13);
            return plugin.getRequiredRank(zoneName);
        }

        // treemanager_status_<zone> - returns formatted status message
        if (params.startsWith("status_")) {
            String zoneName = params.substring(7);
            boolean hasAccess = plugin.hasAccessToZone(player, zoneName);
            String requiredRank = plugin.getRequiredRank(zoneName);
            
            if (hasAccess) {
                return "§a✓ Access Granted";
            } else {
                return "§c✗ Requires: " + requiredRank;
            }
        }

        return null;
    }
}