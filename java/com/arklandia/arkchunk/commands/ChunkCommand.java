package com.arklandia.arkchunk.commands;

import com.arklandia.arkchunk.ArkChunk;
import com.arklandia.arkchunk.managers.ChunkManager;
import com.arklandia.arkchunk.managers.ChunkMonitor;
import net.md_5.bungee.api.ChatColor;
import org.bukkit.Bukkit;
import org.bukkit.Chunk;
import org.bukkit.World;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

/**
 * Gestionnaire des commandes du plugin
 */
public class ChunkCommand implements CommandExecutor {

    private final ArkChunk plugin;
    private final ChunkManager chunkManager;
    private final ChunkMonitor chunkMonitor;

    public ChunkCommand(ArkChunk plugin) {
        this.plugin = plugin;
        this.chunkManager = plugin.getChunkManager();
        this.chunkMonitor = plugin.getChunkMonitor();
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("arkchunk.admin")) {
            sender.sendMessage(ChatColor.RED + "Vous n'avez pas la permission d'utiliser cette commande");
            return true;
        }

        if (args.length == 0) {
            sendHelpMessage(sender);
            return true;
        }

        String subCommand = args[0].toLowerCase();

        return switch (subCommand) {
            case "preload" -> handlePreload(sender, args);
            case "stoppreload" -> handleStopPreload(sender, args);
            case "status" -> handleStatus(sender, args);
            case "delete" -> handleDelete(sender, args);
            case "reset" -> handleReset(sender, args);
            case "unload" -> handleUnload(sender, args);
            case "reload" -> handleReload(sender, args);
            case "info" -> handleInfo(sender, args);
            case "problems" -> handleProblems(sender, args);
            case "actions" -> handleActions(sender, args);
            case "help" -> {
                sendHelpMessage(sender);
                yield true;
            }
            default -> {
                sender.sendMessage(ChatColor.RED + "Commande inconnue: " + subCommand);
                sendHelpMessage(sender);
                yield true;
            }
        };
    }

    /**
     * Gère la commande preload
     */
    private boolean handlePreload(CommandSender sender, String[] args) {
        if (!sender.hasPermission("arkchunk.preload")) {
            sender.sendMessage(ChatColor.RED + "Permission insuffisante: arkchunk.preload");
            return true;
        }

        if (!(sender instanceof Player player)) {
            sender.sendMessage(ChatColor.RED + "Cette commande doit être exécutée par un joueur");
            return true;
        }

        int radius = 10;
        if (args.length > 1) {
            try {
                radius = Integer.parseInt(args[1]);
            } catch (NumberFormatException e) {
                sender.sendMessage(ChatColor.RED + "Rayon invalide");
                return true;
            }
        }

        int taskId = chunkManager.preloadAroundPlayer(player, radius, () -> {
            player.sendMessage(ChatColor.GREEN + "✓ Préchargement terminé!");
        }, (loaded, total) -> {
            // Envoyer la progression au joueur
            player.sendMessage(ChatColor.YELLOW + ChunkManager.getProgressBar(loaded, total));
        });
        
        sender.sendMessage(ChatColor.YELLOW + "Préchargement des chunks en cours... (rayon: " + radius + ")");
        sender.sendMessage(ChatColor.GRAY + "ID de la tâche: " + taskId + " (utilisez /arkchunk stoppreload " + taskId + " pour l'arrêter)");

        return true;
    }

    /**
     * Gère la commande stoppreload
     */
    private boolean handleStopPreload(CommandSender sender, String[] args) {
        if (!sender.hasPermission("arkchunk.preload")) {
            sender.sendMessage(ChatColor.RED + "Permission insuffisante: arkchunk.preload");
            return true;
        }

        if (args.length > 1) {
            // Arrêter une tâche spécifique par ID
            try {
                int taskId = Integer.parseInt(args[1]);
                if (chunkManager.cancelPreload(taskId)) {
                    sender.sendMessage(ChatColor.GREEN + "Tâche de préchargement #" + taskId + " annulée!");
                } else {
                    sender.sendMessage(ChatColor.RED + "Tâche de préchargement #" + taskId + " introuvable");
                }
            } catch (NumberFormatException e) {
                sender.sendMessage(ChatColor.RED + "ID de tâche invalide");
            }
        } else {
            // Arrêter tous les préchargements
            int count = chunkManager.cancelAllPreloads();
            if (count > 0) {
                sender.sendMessage(ChatColor.GREEN + "Tous les préchargements ont été annulés (" + count + " tâches)");
            } else {
                sender.sendMessage(ChatColor.YELLOW + "Aucun préchargement en cours");
            }
        }

        return true;
    }

    /**
     * Gère la commande status
     */
    private boolean handleStatus(CommandSender sender, String[] args) {
        sender.sendMessage(ChatColor.GOLD + "════════ Status ArkChunk ════════");
        sender.sendMessage(ChatColor.YELLOW + "Chunks problématiques: " + ChatColor.WHITE + chunkMonitor.getProblemChunkCount());
        
        int loadedChunks = 0;
        for (World world : Bukkit.getWorlds()) {
            loadedChunks += world.getLoadedChunks().length;
        }
        
        sender.sendMessage(ChatColor.YELLOW + "Chunks chargés: " + ChatColor.WHITE + loadedChunks);
        sender.sendMessage(ChatColor.YELLOW + "Mondes actifs: " + ChatColor.WHITE + Bukkit.getWorlds().size());
        sender.sendMessage(ChatColor.GOLD + "════════════════════════════════");
        
        return true;
    }

    /**
     * Gère la commande delete
     */
    private boolean handleDelete(CommandSender sender, String[] args) {
        if (!sender.hasPermission("arkchunk.delete")) {
            sender.sendMessage(ChatColor.RED + "Permission insuffisante: arkchunk.delete");
            return true;
        }

        if (!(sender instanceof Player player)) {
            sender.sendMessage(ChatColor.RED + "Cette commande doit être exécutée par un joueur");
            return true;
        }

        if (args.length < 3) {
            sender.sendMessage(ChatColor.RED + "Usage: /arkchunk delete <x> <z>");
            return true;
        }

        try {
            int x = Integer.parseInt(args[1]);
            int z = Integer.parseInt(args[2]);
            
            Chunk chunk = player.getWorld().getChunkAt(x, z);
            
            if (chunkManager.deleteChunk(chunk)) {
                sender.sendMessage(ChatColor.GREEN + "Chunk supprimé avec succès!");
            } else {
                sender.sendMessage(ChatColor.RED + "Erreur lors de la suppression du chunk");
            }
        } catch (NumberFormatException e) {
            sender.sendMessage(ChatColor.RED + "Coordonnées invalides");
        }

        return true;
    }

    /**
     * Gère la commande reset
     */
    private boolean handleReset(CommandSender sender, String[] args) {
        if (!sender.hasPermission("arkchunk.reset")) {
            sender.sendMessage(ChatColor.RED + "Permission insuffisante: arkchunk.reset");
            return true;
        }

        if (!(sender instanceof Player player)) {
            sender.sendMessage(ChatColor.RED + "Cette commande doit être exécutée par un joueur");
            return true;
        }

        if (args.length < 3) {
            sender.sendMessage(ChatColor.RED + "Usage: /arkchunk reset <x> <z>");
            return true;
        }

        try {
            int x = Integer.parseInt(args[1]);
            int z = Integer.parseInt(args[2]);
            
            Chunk chunk = player.getWorld().getChunkAt(x, z);
            sender.sendMessage(ChatColor.YELLOW + "Réinitialisation du chunk...");
            
            Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
                if (chunkManager.resetChunk(chunk)) {
                    player.sendMessage(ChatColor.GREEN + "Chunk réinitialisé avec succès!");
                } else {
                    player.sendMessage(ChatColor.RED + "Erreur lors de la réinitialisation du chunk");
                }
            });
        } catch (NumberFormatException e) {
            sender.sendMessage(ChatColor.RED + "Coordonnées invalides");
        }

        return true;
    }

    /**
     * Gère la commande unload
     */
    private boolean handleUnload(CommandSender sender, String[] args) {
        if (!sender.hasPermission("arkchunk.unload")) {
            sender.sendMessage(ChatColor.RED + "Permission insuffisante: arkchunk.unload");
            return true;
        }

        if (!(sender instanceof Player player)) {
            sender.sendMessage(ChatColor.RED + "Cette commande doit être exécutée par un joueur");
            return true;
        }

        if (args.length < 3) {
            sender.sendMessage(ChatColor.RED + "Usage: /arkchunk unload <x> <z>");
            return true;
        }

        try {
            int x = Integer.parseInt(args[1]);
            int z = Integer.parseInt(args[2]);
            
            Chunk chunk = player.getWorld().getChunkAt(x, z);
            
            if (chunkManager.unloadChunk(chunk)) {
                sender.sendMessage(ChatColor.GREEN + "Chunk déchargé avec succès!");
            } else {
                sender.sendMessage(ChatColor.RED + "Erreur lors du déchargement du chunk");
            }
        } catch (NumberFormatException e) {
            sender.sendMessage(ChatColor.RED + "Coordonnées invalides");
        }

        return true;
    }

    /**
     * Gère la commande reload
     */
    private boolean handleReload(CommandSender sender, String[] args) {
        if (!sender.hasPermission("arkchunk.reload")) {
            sender.sendMessage(ChatColor.RED + "Permission insuffisante: arkchunk.reload");
            return true;
        }

        if (!(sender instanceof Player player)) {
            sender.sendMessage(ChatColor.RED + "Cette commande doit être exécutée par un joueur");
            return true;
        }

        if (args.length < 3) {
            sender.sendMessage(ChatColor.RED + "Usage: /arkchunk reload <x> <z>");
            return true;
        }

        try {
            int x = Integer.parseInt(args[1]);
            int z = Integer.parseInt(args[2]);
            
            Chunk chunk = player.getWorld().getChunkAt(x, z);
            sender.sendMessage(ChatColor.YELLOW + "Réinstallation du chunk...");
            
            Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
                if (chunkManager.reloadChunk(chunk)) {
                    player.sendMessage(ChatColor.GREEN + "Chunk réinstallé avec succès!");
                } else {
                    player.sendMessage(ChatColor.RED + "Erreur lors de la réinstallation du chunk");
                }
            });
        } catch (NumberFormatException e) {
            sender.sendMessage(ChatColor.RED + "Coordonnées invalides");
        }

        return true;
    }

    /**
     * Gère la commande info
     */
    private boolean handleInfo(CommandSender sender, String[] args) {
        if (!sender.hasPermission("arkchunk.info")) {
            sender.sendMessage(ChatColor.RED + "Permission insuffisante: arkchunk.info");
            return true;
        }

        if (!(sender instanceof Player player)) {
            sender.sendMessage(ChatColor.RED + "Cette commande doit être exécutée par un joueur");
            return true;
        }

        Chunk chunk = player.getChunk();
        
        sender.sendMessage(ChatColor.GOLD + "════════ Info Chunk ════════");
        sender.sendMessage(ChatColor.YELLOW + "Coordonnées: " + ChatColor.WHITE + chunk.getX() + ", " + chunk.getZ());
        sender.sendMessage(ChatColor.YELLOW + "Monde: " + ChatColor.WHITE + chunk.getWorld().getName());
        sender.sendMessage(ChatColor.YELLOW + "Entités: " + ChatColor.WHITE + chunk.getEntities().length);
        sender.sendMessage(ChatColor.YELLOW + "Tile Entities: " + ChatColor.WHITE + chunk.getTileEntities(false).length);
        sender.sendMessage(ChatColor.YELLOW + "Problématique: " + ChatColor.WHITE + (chunkManager.isProblemChunk(chunk) ? ChatColor.RED + "OUI" : ChatColor.GREEN + "NON"));
        
        ChunkManager.ChunkData data = chunkManager.getChunkData(chunk);
        if (data != null) {
            sender.sendMessage(ChatColor.YELLOW + "Temps de chargement: " + ChatColor.WHITE + data.loadTime + "ms");
        }
        
        sender.sendMessage(ChatColor.GOLD + "═════════════════════════════");

        return true;
    }

    /**
     * Gère la commande problems
     */
    private boolean handleProblems(CommandSender sender, String[] args) {
        if (!sender.hasPermission("arkchunk.monitor")) {
            sender.sendMessage(ChatColor.RED + "Permission insuffisante: arkchunk.monitor");
            return true;
        }

        sender.sendMessage(ChatColor.GOLD + "════════ Chunks Problématiques ════════");
        sender.sendMessage(chunkMonitor.getProblemChunksSummary());
        sender.sendMessage(ChatColor.GOLD + "════════════════════════════════════════");

        return true;
    }

    /**
     * Gère la commande actions
     */
    private boolean handleActions(CommandSender sender, String[] args) {
        if (!sender.hasPermission("arkchunk.monitor")) {
            sender.sendMessage(ChatColor.RED + "Permission insuffisante: arkchunk.monitor");
            return true;
        }

        if (!(sender instanceof Player player)) {
            sender.sendMessage(ChatColor.RED + "Cette commande doit être exécutée par un joueur");
            return true;
        }

        if (args.length < 3) {
            sender.sendMessage(ChatColor.RED + "Usage: /arkchunk actions <x> <z>");
            return true;
        }

        try {
            int x = Integer.parseInt(args[1]);
            int z = Integer.parseInt(args[2]);
            
            sender.sendMessage(ChatColor.GOLD + "════════ Actions Disponibles ════════");
            sender.sendMessage(ChatColor.YELLOW + "Chunk: " + x + ", " + z);
            sender.sendMessage(" ");
            
            if (player.hasPermission("arkchunk.reset")) {
                sender.sendMessage(ChatColor.GREEN + "/arkchunk reset " + x + " " + z + ChatColor.GRAY + " - Réinitialiser le chunk");
            }
            
            if (player.hasPermission("arkchunk.reload")) {
                sender.sendMessage(ChatColor.GREEN + "/arkchunk reload " + x + " " + z + ChatColor.GRAY + " - Réinstaller le chunk");
            }
            
            if (player.hasPermission("arkchunk.unload")) {
                sender.sendMessage(ChatColor.GREEN + "/arkchunk unload " + x + " " + z + ChatColor.GRAY + " - Décharger le chunk");
            }
            
            if (player.hasPermission("arkchunk.delete")) {
                sender.sendMessage(ChatColor.GREEN + "/arkchunk delete " + x + " " + z + ChatColor.GRAY + " - Supprimer le chunk");
            }
            
            sender.sendMessage(ChatColor.GOLD + "═════════════════════════════════════");
        } catch (NumberFormatException e) {
            sender.sendMessage(ChatColor.RED + "Coordonnées invalides");
        }

        return true;
    }

    /**
     * Envoie le message d'aide
     */
    private void sendHelpMessage(CommandSender sender) {
        sender.sendMessage(ChatColor.GOLD + "════════ Aide ArkChunk ════════");
        sender.sendMessage(" ");
        sender.sendMessage(ChatColor.YELLOW + "/arkchunk preload [rayon]" + ChatColor.GRAY + " - Précharger les chunks");
        sender.sendMessage(ChatColor.YELLOW + "/arkchunk stoppreload [ID]" + ChatColor.GRAY + " - Arrêter le préchargement");
        sender.sendMessage(ChatColor.YELLOW + "/arkchunk status" + ChatColor.GRAY + " - Voir le statut");
        sender.sendMessage(ChatColor.YELLOW + "/arkchunk info" + ChatColor.GRAY + " - Info du chunk courant");
        sender.sendMessage(ChatColor.YELLOW + "/arkchunk problems" + ChatColor.GRAY + " - Voir les problèmes");
        sender.sendMessage(ChatColor.YELLOW + "/arkchunk delete <x> <z>" + ChatColor.GRAY + " - Supprimer un chunk");
        sender.sendMessage(ChatColor.YELLOW + "/arkchunk reset <x> <z>" + ChatColor.GRAY + " - Réinitialiser un chunk");
        sender.sendMessage(ChatColor.YELLOW + "/arkchunk unload <x> <z>" + ChatColor.GRAY + " - Décharger un chunk");
        sender.sendMessage(ChatColor.YELLOW + "/arkchunk reload <x> <z>" + ChatColor.GRAY + " - Réinstaller un chunk");
        sender.sendMessage(ChatColor.YELLOW + "/arkchunk actions <x> <z>" + ChatColor.GRAY + " - Actions disponibles");
        sender.sendMessage(" ");
        sender.sendMessage(ChatColor.GOLD + "════════════════════════════");
    }
}