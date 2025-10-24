package com.arklandia.arkchunk.managers;

import com.arklandia.arkchunk.ArkChunk;
import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.chat.ClickEvent;
import net.md_5.bungee.api.chat.ComponentBuilder;
import net.md_5.bungee.api.chat.HoverEvent;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.Bukkit;
import org.bukkit.Chunk;
import org.bukkit.World;
import org.bukkit.entity.Player;

import java.util.*;

/**
 * Moniteur de chunks pour détecter les problèmes
 */
public class ChunkMonitor {

    private final ArkChunk plugin;
    private int monitorTaskId = -1;
    private final Map<String, Integer> chunkErrors;
    private final Map<String, Long> lastNotificationTime;

    public ChunkMonitor(ArkChunk plugin) {
        this.plugin = plugin;
        this.chunkErrors = new HashMap<>();
        this.lastNotificationTime = new HashMap<>();
    }

    /**
     * Démarre la surveillance des chunks
     */
    public void startMonitoring() {
        monitorTaskId = Bukkit.getScheduler().scheduleSyncRepeatingTask(plugin, this::monitorChunks, 0L, 200L); // Tous les 10 secondes
        plugin.getLogger().info("Surveillance des chunks démarrée");
    }

    /**
     * Arrête la surveillance des chunks
     */
    public void stopMonitoring() {
        if (monitorTaskId != -1) {
            Bukkit.getScheduler().cancelTask(monitorTaskId);
            plugin.getLogger().info("Surveillance des chunks arrêtée");
        }
    }

    /**
     * Monitore les chunks pour détecter les problèmes
     */
    private void monitorChunks() {
        ChunkManager chunkManager = plugin.getChunkManager();
        
        for (World world : Bukkit.getWorlds()) {
            Chunk[] chunks = world.getLoadedChunks();
            
            for (Chunk chunk : chunks) {
                try {
                    checkChunkHealth(chunk, chunkManager);
                } catch (Exception e) {
                    plugin.getLogger().warning("Erreur lors de la vérification du chunk " + chunk.getX() + "," + chunk.getZ() + ": " + e.getMessage());
                    recordChunkError(chunk);
                }
            }
        }
    }

    /**
     * Vérifie la santé d'un chunk
     * @param chunk Chunk à vérifier
     * @param chunkManager ChunkManager
     */
    private void checkChunkHealth(Chunk chunk, ChunkManager chunkManager) {
        String chunkKey = chunkManager.getChunkKey(chunk);
        
        // Vérifier le nombre d'erreurs
        int errorCount = chunkErrors.getOrDefault(chunkKey, 0);
        
        // Détecter les chunks qui ralentissent le serveur
        if (isChunkProblematic(chunk)) {
            chunkManager.addProblemChunk(chunk);
            recordChunkError(chunk);
            
            if (shouldNotifyAdmins(chunkKey)) {
                notifyAdminsOfProblem(chunk, errorCount);
                updateLastNotificationTime(chunkKey);
            }
        }
        
        // Nettoyer les chunks sans erreurs
        if (errorCount > 0 && !isChunkProblematic(chunk)) {
            chunkErrors.remove(chunkKey);
        }
    }

    /**
     * Vérifie si un chunk est problématique
     * @param chunk Chunk à vérifier
     * @return true si problématique, false sinon
     */
    private boolean isChunkProblematic(Chunk chunk) {
        // Vérifier plusieurs critères
        return hasHighEntityCount(chunk) || hasHighTileEntityCount(chunk) || hasTooManyTickingBlocks(chunk);
    }

    /**
     * Vérifie si un chunk a trop d'entités
     * @param chunk Chunk à vérifier
     * @return true si trop d'entités, false sinon
     */
    private boolean hasHighEntityCount(Chunk chunk) {
        return chunk.getEntities().length > 500;
    }

    /**
     * Vérifie si un chunk a trop d'entités tuiles
     * @param chunk Chunk à vérifier
     * @return true si trop d'entités tuiles, false sinon
     */
    private boolean hasHighTileEntityCount(Chunk chunk) {
        return chunk.getTileEntities(false).length > 200;
    }

    /**
     * Vérifie si un chunk a trop de blocs qui font des ticks
     * @param chunk Chunk à vérifier
     * @return true si trop de blocs, false sinon
     */
    private boolean hasTooManyTickingBlocks(Chunk chunk) {
        // Cette vérification est basée sur le nombre de blocs spécialisés
        return chunk.getTileEntities(false).length > 150;
    }

    /**
     * Enregistre une erreur pour un chunk
     * @param chunk Chunk avec erreur
     */
    private void recordChunkError(Chunk chunk) {
        String chunkKey = plugin.getChunkManager().getChunkKey(chunk);
        chunkErrors.put(chunkKey, chunkErrors.getOrDefault(chunkKey, 0) + 1);
    }

    /**
     * Vérifie si on doit notifier les admins
     * @param chunkKey Clé du chunk
     * @return true si on doit notifier, false sinon
     */
    private boolean shouldNotifyAdmins(String chunkKey) {
        long lastNotif = lastNotificationTime.getOrDefault(chunkKey, 0L);
        return System.currentTimeMillis() - lastNotif > 300000; // Notifier au max toutes les 5 minutes
    }

    /**
     * Met à jour le temps de dernière notification
     * @param chunkKey Clé du chunk
     */
    private void updateLastNotificationTime(String chunkKey) {
        lastNotificationTime.put(chunkKey, System.currentTimeMillis());
    }

    /**
     * Notifie les admins d'un problème de chunk
     * @param chunk Chunk problématique
     * @param errorCount Nombre d'erreurs
     */
    private void notifyAdminsOfProblem(Chunk chunk, int errorCount) {
        for (Player player : Bukkit.getOnlinePlayers()) {
            if (player.hasPermission("arkchunk.monitor")) {
                sendNotificationToPlayer(player, chunk, errorCount);
            }
        }
    }

    /**
     * Envoie une notification à un joueur
     * @param player Joueur cible
     * @param chunk Chunk problématique
     * @param errorCount Nombre d'erreurs
     */
    private void sendNotificationToPlayer(Player player, Chunk chunk, int errorCount) {
        TextComponent message = new TextComponent(ChatColor.RED + "⚠ Problème détecté dans le chunk ");
        message.addExtra(ChatColor.YELLOW + "" + chunk.getX() + ", " + chunk.getZ() + ChatColor.RED + " (" + chunk.getWorld().getName() + ")");
        message.addExtra("\n");
        
        TextComponent detailsComponent = new TextComponent(ChatColor.GRAY + "[Détails]");
        detailsComponent.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, 
            new ComponentBuilder(ChatColor.YELLOW + "Entités: " + ChatColor.WHITE + chunk.getEntities().length + "\n" +
                ChatColor.YELLOW + "Tile Entities: " + ChatColor.WHITE + chunk.getTileEntities(false).length + "\n" +
                ChatColor.YELLOW + "Erreurs signalées: " + ChatColor.WHITE + errorCount).create()));
        message.addExtra(detailsComponent);
        message.addExtra(" ");
        
        TextComponent actionComponent = new TextComponent(ChatColor.GREEN + "[Actions]");
        actionComponent.setClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/arkchunk actions " + chunk.getX() + " " + chunk.getZ()));
        actionComponent.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, 
            new ComponentBuilder("Cliquez pour afficher les actions disponibles").create()));
        message.addExtra(actionComponent);
        
        player.spigot().sendMessage(message);
    }

    /**
     * Obtient un résumé de tous les chunks problématiques
     * @return Résumé en chaîne de caractères
     */
    public String getProblemChunksSummary() {
        if (chunkErrors.isEmpty()) {
            return ChatColor.GREEN + "Aucun problème détecté";
        }
        
        StringBuilder sb = new StringBuilder();
        sb.append(ChatColor.RED).append("Chunks problématiques détectés: ").append(chunkErrors.size()).append("\n");
        
        chunkErrors.forEach((chunkKey, errorCount) -> {
            sb.append(ChatColor.YELLOW).append("  - ").append(chunkKey).append(": ");
            sb.append(ChatColor.RED).append(errorCount).append(" erreurs\n");
        });
        
        return sb.toString();
    }

    /**
     * Obtient le nombre de chunks problématiques
     * @return Nombre de chunks problématiques
     */
    public int getProblemChunkCount() {
        return chunkErrors.size();
    }

    /**
     * Réinitialise les statistiques
     */
    public void resetStatistics() {
        chunkErrors.clear();
        lastNotificationTime.clear();
    }
}