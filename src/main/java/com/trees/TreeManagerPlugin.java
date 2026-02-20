package com.trees;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;

import com.sk89q.worldguard.WorldGuard;
import com.sk89q.worldguard.protection.regions.ProtectedRegion;

public class TreeManagerPlugin extends JavaPlugin implements Listener {

    private Map<String, ZoneConfig> zoneConfigs;
    private Set<Material> allowedToBreak;
    private Set<Material> allowedToUse;
    private boolean hasPapi;
    private Map<String, String> messages;

    private class ZoneConfig {
        public String permission;
        public String requiredRank;
        public String noPermissionMessage;
        
        public ZoneConfig(String permission, String requiredRank, String noPermissionMessage) {
            this.permission = permission;
            this.requiredRank = requiredRank;
            this.noPermissionMessage = noPermissionMessage;
        }
    }

    @Override
    public void onEnable() {
        zoneConfigs = new HashMap<>();
        messages = new HashMap<>();
        setupAllowedMaterials();
        loadConfig();
        loadMessages();
        
        hasPapi = getServer().getPluginManager().getPlugin("PlaceholderAPI") != null;
        if (hasPapi) {
            getLogger().info("PlaceholderAPI found - registering placeholders");
            registerPlaceholders();
        } else {
            getLogger().warning("PlaceholderAPI not found");
        }
        
        getServer().getPluginManager().registerEvents(this, this);
        getLogger().info("TreeManagerPlugin enabled - Loaded " + zoneConfigs.size() + " zones");
        
        getServer().getScheduler().runTaskLater(this, this::testPlaceholder, 100L);
    }

    @Override
    public void onDisable() {
        getLogger().info("TreeManagerPlugin disabled");
    }

    private void registerPlaceholders() {
        if (!hasPapi) return;
        
        try {
            new TreeManagerPlaceholder(this).register();
            getLogger().info("✓ TreeManager placeholders registered successfully!");
        } catch (Exception e) {
            getLogger().warning("✗ Failed to register placeholders: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void testPlaceholder() {
        if (!hasPapi) return;
        
        Player testPlayer = getServer().getOnlinePlayers().stream().findFirst().orElse(null);
        if (testPlayer != null) {
            getLogger().info("Testing placeholders with player: " + testPlayer.getName());
            
            for (String zoneName : zoneConfigs.keySet()) {
                getLogger().info("Zone: " + zoneName + 
                    ", Required Rank: " + getRequiredRank(zoneName) + 
                    ", Player Access: " + hasAccessToZone(testPlayer, zoneName));
            }
            
            try {
                Class<?> papiClass = Class.forName("me.clip.placeholderapi.PlaceholderAPI");
                java.lang.reflect.Method parseMethod = papiClass.getMethod("setPlaceholders", Player.class, String.class);
                
                for (String zoneName : zoneConfigs.keySet()) {
                    String result = (String) parseMethod.invoke(null, testPlayer, "%treemanager_status_" + zoneName + "%");
                    getLogger().info("Placeholder test " + zoneName + ": " + result);
                }
            } catch (Exception e) {
                getLogger().warning("Placeholder test failed: " + e.getMessage());
            }
        }
    }

    public boolean hasAccessToZone(Player player, String zoneName) {
        ZoneConfig zoneConfig = zoneConfigs.get(zoneName);
        if (zoneConfig == null) {
            getLogger().warning("Zone not found: " + zoneName);
            return false;
        }
        return hasZonePermission(player, zoneConfig);
    }

    public String getRequiredRank(String zoneName) {
        ZoneConfig zoneConfig = zoneConfigs.get(zoneName);
        return zoneConfig != null ? zoneConfig.requiredRank : "Unknown";
    }

    public Set<String> getZoneNames() {
        return new HashSet<>(zoneConfigs.keySet());
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (command.getName().equalsIgnoreCase("treemanager")) {
            if (args.length > 0 && args[0].equalsIgnoreCase("reload")) {
                if (!sender.hasPermission("treemanager.reload")) {
                    sender.sendMessage("§cYou don't have permission to reload the config!");
                    return true;
                }
                
                reloadConfig();
                zoneConfigs.clear();
                messages.clear();
                loadConfig();
                loadMessages();
                
                sender.sendMessage("§aTreeManager configuration reloaded!");
                getLogger().info("Configuration reloaded by " + sender.getName());
                return true;
            } else {
                sender.sendMessage("§6TreeManager Commands:");
                sender.sendMessage("§e/treemanager reload §7- Reload configuration");
                return true;
            }
        }
        return false;
    }

    private void setupAllowedMaterials() {
        allowedToBreak = new HashSet<>();
        allowedToBreak.add(Material.OAK_LOG);
        allowedToBreak.add(Material.SPRUCE_LOG);
        allowedToBreak.add(Material.BIRCH_LOG);
        allowedToBreak.add(Material.JUNGLE_LOG);
        allowedToBreak.add(Material.ACACIA_LOG);
        allowedToBreak.add(Material.DARK_OAK_LOG);
        allowedToBreak.add(Material.MANGROVE_LOG);
        allowedToBreak.add(Material.CHERRY_LOG);
        
        allowedToBreak.add(Material.OAK_LEAVES);
        allowedToBreak.add(Material.SPRUCE_LEAVES);
        allowedToBreak.add(Material.BIRCH_LEAVES);
        allowedToBreak.add(Material.JUNGLE_LEAVES);
        allowedToBreak.add(Material.ACACIA_LEAVES);
        allowedToBreak.add(Material.DARK_OAK_LEAVES);
        allowedToBreak.add(Material.MANGROVE_LEAVES);
        allowedToBreak.add(Material.CHERRY_LEAVES);
        
        allowedToBreak.add(Material.APPLE);
        allowedToBreak.add(Material.STICK);

        allowedToUse = new HashSet<>();
        allowedToUse.add(Material.OAK_SAPLING);
        allowedToUse.add(Material.SPRUCE_SAPLING);
        allowedToUse.add(Material.BIRCH_SAPLING);
        allowedToUse.add(Material.JUNGLE_SAPLING);
        allowedToUse.add(Material.ACACIA_SAPLING);
        allowedToUse.add(Material.DARK_OAK_SAPLING);
        allowedToUse.add(Material.CHERRY_SAPLING);
        allowedToUse.add(Material.MANGROVE_PROPAGULE);
        allowedToUse.add(Material.BONE_MEAL);
    }

    private void loadConfig() {
        saveDefaultConfig();
        
        if (getConfig().getConfigurationSection("zones") != null) {
            for (String zone : getConfig().getConfigurationSection("zones").getKeys(false)) {
                String permission = getConfig().getString("zones." + zone + ".permission", "");
                String requiredRank = getConfig().getString("zones." + zone + ".required-rank", "");
                String noPermissionMessage = getConfig().getString("zones." + zone + ".no-permission-message", "no-permission");
                
                zoneConfigs.put(zone, new ZoneConfig(permission, requiredRank, noPermissionMessage));
                getLogger().info("Loaded zone: " + zone + " -> permission: " + permission + ", rank: " + requiredRank);
            }
        } else {
            getLogger().warning("No 'zones' section found in config.yml!");
        }
    }
    
    private void loadMessages() {
        saveResource("messages.yml", false);
        reloadConfig();
        
        if (getConfig().getConfigurationSection("messages") != null) {
            for (String key : getConfig().getConfigurationSection("messages").getKeys(false)) {
                String message = getConfig().getString("messages." + key);
                messages.put(key, message);
            }
        } else {
            setDefaultMessages();
        }
    }
    
    private void setDefaultMessages() {
        messages.put("no-permission", "&cYou don't have permission to use this farm!");
        messages.put("break-not-allowed", "&cYou can only break trees and leaves in this area!");
        messages.put("place-not-allowed", "&cYou can only place saplings in this area!");
        messages.put("bonemeal-not-allowed", "&cYou can only use bone meal on saplings in this area!");
        messages.put("no-permission-basic", "&c❌ Access Denied! &7You need &e{required_rank} &7rank. Your current rank: %luckperms_primary_group_name%");
        messages.put("no-permission-premium", "&c🔒 Premium Farm Locked! &7You need &6{required_rank} &7rank. Your current rank: %luckperms_primary_group_name%");
        messages.put("no-permission-vip", "&c⭐ VIP Farm Restricted! &7You need &d{required_rank} &7rank. Your current rank: %luckperms_primary_group_name%");
        messages.put("public-area", "&a🌳 Public Garden &7- Everyone can use this area!");
    }
    
    private String getMessage(String key, String defaultValue) {
        return messages.getOrDefault(key, defaultValue).replace("&", "§");
    }

    @EventHandler
    public void onBlockBreak(BlockBreakEvent event) {
        Player player = event.getPlayer();
        Block block = event.getBlock();
        
        ZoneConfig zoneConfig = getBlockZoneConfig(block);
        if (zoneConfig == null) {
            return;
        }

        if (!hasZonePermission(player, zoneConfig)) {
            event.setCancelled(true);
            String message = formatMessage(zoneConfig.noPermissionMessage, player, zoneConfig.requiredRank);
            player.sendMessage(message);
            return;
        }

        if (!allowedToBreak.contains(block.getType())) {
            event.setCancelled(true);
            player.sendMessage(getMessage("break-not-allowed", "§cYou can only break trees and leaves in this area!"));
        }
    }

    @EventHandler
    public void onBlockPlace(BlockPlaceEvent event) {
        Player player = event.getPlayer();
        Block block = event.getBlock();
        
        ZoneConfig zoneConfig = getBlockZoneConfig(block);
        if (zoneConfig == null) {
            return;
        }

        if (!hasZonePermission(player, zoneConfig)) {
            event.setCancelled(true);
            String message = formatMessage(zoneConfig.noPermissionMessage, player, zoneConfig.requiredRank);
            player.sendMessage(message);
            return;
        }

        if (!isSapling(block.getType())) {
            event.setCancelled(true);
            player.sendMessage(getMessage("place-not-allowed", "§cYou can only place saplings in this area!"));
        }
    }

    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {
        Player player = event.getPlayer();
        ItemStack item = event.getItem();
        Block clickedBlock = event.getClickedBlock();
        
        if (clickedBlock == null || item == null) {
            return;
        }
        
        ZoneConfig zoneConfig = getBlockZoneConfig(clickedBlock);
        if (zoneConfig == null) {
            return;
        }

        if (!hasZonePermission(player, zoneConfig)) {
            event.setCancelled(true);
            String message = formatMessage(zoneConfig.noPermissionMessage, player, zoneConfig.requiredRank);
            player.sendMessage(message);
            return;
        }

        if (item.getType() == Material.BONE_MEAL && !isSapling(clickedBlock.getType())) {
            event.setCancelled(true);
            player.sendMessage(getMessage("bonemeal-not-allowed", "§cYou can only use bone meal on saplings in this area!"));
        }
    }

    private boolean isSapling(Material material) {
        return material == Material.OAK_SAPLING ||
               material == Material.SPRUCE_SAPLING ||
               material == Material.BIRCH_SAPLING ||
               material == Material.JUNGLE_SAPLING ||
               material == Material.ACACIA_SAPLING ||
               material == Material.DARK_OAK_SAPLING ||
               material == Material.MANGROVE_PROPAGULE ||
               material == Material.CHERRY_SAPLING;
    }

    private ZoneConfig getBlockZoneConfig(Block block) {
        try {
            com.sk89q.worldedit.util.Location location = com.sk89q.worldedit.bukkit.BukkitAdapter.adapt(block.getLocation());
            com.sk89q.worldguard.protection.regions.RegionContainer container = WorldGuard.getInstance().getPlatform().getRegionContainer();
            com.sk89q.worldguard.protection.regions.RegionQuery query = container.createQuery();
            
            Set<ProtectedRegion> regions = query.getApplicableRegions(location).getRegions();
            for (ProtectedRegion region : regions) {
                if (zoneConfigs.containsKey(region.getId())) {
                    return zoneConfigs.get(region.getId());
                }
            }
        } catch (Exception e) {
            getLogger().warning("Error checking WorldGuard regions for block: " + e.getMessage());
        }
        return null;
    }

    private boolean hasZonePermission(Player player, ZoneConfig zoneConfig) {
        if (zoneConfig.permission == null || zoneConfig.permission.isEmpty()) {
            return true;
        }
        return player.hasPermission(zoneConfig.permission);
    }

    private String formatMessage(String messageKey, Player player, String requiredRank) {
        String message = getMessage(messageKey, "&cYou do not have access! You need to be {required_rank}. You are currently: %luckperms_primary_group_name%");
        String formatted = message.replace("{required_rank}", requiredRank).replace("&", "§");
        
        if (hasPapi) {
            try {
                Class<?> papiClass = Class.forName("me.clip.placeholderapi.PlaceholderAPI");
                java.lang.reflect.Method setPlaceholdersMethod = papiClass.getMethod("setPlaceholders", Player.class, String.class);
                formatted = (String) setPlaceholdersMethod.invoke(null, player, formatted);
            } catch (Exception e) {
                getLogger().warning("Failed to set PlaceholderAPI placeholders: " + e.getMessage());
            }
        }
        
        return formatted;
    }
}