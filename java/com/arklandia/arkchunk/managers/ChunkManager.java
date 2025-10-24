package com.arklandia.arkchunk.managers;

import com.arklandia.arkchunk.ArkChunk;
import org.bukkit.Bukkit;
import org.bukkit.Chunk;
import org.bukkit.World;
import org.bukkit.entity.Player;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Gestionnaire principal des chunks
 */
public class ChunkManager {

    private final ArkChunk plugin;
    private final Map<String, ChunkData> chunkDataMap;
    private final Map<String, Long> chunkLoadTimes;
    private final Set<Chunk> problemChunks;
    private final Map<Integer, Integer> preloadTasks; // Map taskId -> playerId
    private final Map<Integer, ProgressCallback> progressCallbacks; // Map taskId -> callback

    @FunctionalInterface
    public interface ProgressCallback {
        void onProgress(int loaded, int total);
    }

    public ChunkManager(ArkChunk plugin) {
        this.plugin = plugin;
        this.chunkDataMap = new ConcurrentHashMap<>();
        this.chunkLoadTimes = new ConcurrentHashMap<>();
        this.problemChunks = Collections.newSetFromMap(new ConcurrentHashMap<>());
        this.preloadTasks = new ConcurrentHashMap<>();
        this.progressCallbacks = new ConcurrentHashMap<>();
    }

    /**
     * Précharge les chunks dans un rayon défini
     * @param world Monde cible
     * @param centerX Coordonnée X du centre
     * @param centerZ Coordonnée Z du centre
     * @param radius Rayon de préchargement
     * @param callback Callback après préchargement
     * @param progressCallback Callback pour les mises à jour de progression
     * @return ID de la tâche de préchargement
     */
    public int preloadChunks(World world, int centerX, int centerZ, int radius, Runnable callback, ProgressCallback progressCallback) {
        int totalChunks = (2 * radius + 1) * (2 * radius + 1);
        int[] loadedChunks = {0};
        int[] taskIdRef = {-1};
        
        org.bukkit.scheduler.BukkitTask mainTask = Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            java.util.List<int[]> chunkCoords = new java.util.ArrayList<>();
            
            for (int x = centerX - radius; x <= centerX + radius; x++) {
                for (int z = centerZ - radius; z <= centerZ + radius; z++) {
                    chunkCoords.add(new int[]{x, z});
                }
            }
            
            int chunksPerTick = 4; // Charger 4 chunks par tick
            int tickDelay = 0;
            
            for (int i = 0; i < chunkCoords.size(); i++) {
                if (taskIdRef[0] != -1 && !preloadTasks.containsKey(taskIdRef[0])) {
                    // Tâche annulée
                    break;
                }
                
                final int[] coords = chunkCoords.get(i);
                final int finalX = coords[0];
                final int finalZ = coords[1];
                final int currentIndex = i;
                
                // Calculer le délai en ticks (un délai tous les X chunks)
                int delay = (i / chunksPerTick);
                
                Bukkit.getScheduler().runTaskLater(plugin, () -> {
                    try {
                        if (taskIdRef[0] != -1 && !preloadTasks.containsKey(taskIdRef[0])) {
                            return; // Tâche annulée
                        }
                        
                        Chunk chunk = world.getChunkAt(finalX, finalZ);
                        long startTime = System.currentTimeMillis();
                        
                        chunk.load(true);
                        
                        long loadTime = System.currentTimeMillis() - startTime;
                        String chunkKey = getChunkKey(chunk);
                        
                        chunkDataMap.put(chunkKey, new ChunkData(chunk, loadTime));
                        chunkLoadTimes.put(chunkKey, loadTime);
                        
                        loadedChunks[0]++;
                        
                        // Envoyer mise à jour de progression (une fois tous les X chunks)
                        if (progressCallback != null && currentIndex % chunksPerTick == 0) {
                            progressCallback.onProgress(loadedChunks[0], totalChunks);
                        }
                        
                        // Vérifier si le chunk est problématique
                        if (loadTime > 100) {
                            problemChunks.add(chunk);
                        }
                    } catch (Exception e) {
                        plugin.getLogger().warning("Erreur lors du préchargement du chunk " + finalX + ", " + finalZ + ": " + e.getMessage());
                    }
                }, delay);
            }
            
            // Calculer le délai total approximatif
            int totalDelay = (chunkCoords.size() / chunksPerTick) + 5;
            
            // Callback exécuté après la fin de tous les chargements
            Bukkit.getScheduler().runTaskLater(plugin, () -> {
                plugin.getLogger().info("Préchargement terminé: " + loadedChunks[0] + "/" + totalChunks + " chunks chargés");
                
                if (taskIdRef[0] != -1) {
                    preloadTasks.remove(taskIdRef[0]);
                }
                
                if (callback != null) {
                    callback.run();
                }
            }, totalDelay);
        });
        
        int taskId = mainTask.getTaskId();
        taskIdRef[0] = taskId;
        preloadTasks.put(taskId, taskId);
        return taskId;
    }

    /**
     * Précharge les chunks dans un rayon défini (sans callback de progression)
     * @param world Monde cible
     * @param centerX Coordonnée X du centre
     * @param centerZ Coordonnée Z du centre
     * @param radius Rayon de préchargement
     * @param callback Callback après préchargement
     * @return ID de la tâche de préchargement
     */
    public int preloadChunks(World world, int centerX, int centerZ, int radius, Runnable callback) {
        return preloadChunks(world, centerX, centerZ, radius, callback, null);
    }

    /**
     * Précharge les chunks autour d'un joueur
     * @param player Joueur cible
     * @param radius Rayon de préchargement
     * @param callback Callback après préchargement
     * @param progressCallback Callback pour les mises à jour de progression
     * @return ID de la tâche de préchargement
     */
    public int preloadAroundPlayer(Player player, int radius, Runnable callback, ProgressCallback progressCallback) {
        Chunk chunk = player.getChunk();
        return preloadChunks(player.getWorld(), chunk.getX(), chunk.getZ(), radius, callback, progressCallback);
    }

    /**
     * Précharge les chunks autour d'un joueur (sans callback de progression)
     * @param player Joueur cible
     * @param radius Rayon de préchargement
     * @param callback Callback après préchargement
     * @return ID de la tâche de préchargement
     */
    public int preloadAroundPlayer(Player player, int radius, Runnable callback) {
        return preloadAroundPlayer(player, radius, callback, null);
    }

    /**
     * Supprime un chunk
     * @param chunk Chunk à supprimer
     * @return true si succès, false sinon
     */
    public boolean deleteChunk(Chunk chunk) {
        try {
            String chunkKey = getChunkKey(chunk);
            
            // Décharger le chunk
            chunk.unload(false);
            
            // Nettoyer les données
            chunkDataMap.remove(chunkKey);
            chunkLoadTimes.remove(chunkKey);
            problemChunks.remove(chunk);
            
            plugin.getLogger().info("Chunk supprimé: " + chunkKey);
            return true;
        } catch (Exception e) {
            plugin.getLogger().warning("Erreur lors de la suppression du chunk: " + e.getMessage());
            return false;
        }
    }

    /**
     * Réinitialise un chunk
     * @param chunk Chunk à réinitialiser
     * @return true si succès, false sinon
     */
    public boolean resetChunk(Chunk chunk) {
        try {
            int x = chunk.getX();
            int z = chunk.getZ();
            World world = chunk.getWorld();
            
            // Décharger le chunk
            world.unloadChunk(chunk);
            
            // Recharger le chunk
            world.loadChunk(x, z);
            
            String chunkKey = getChunkKey(chunk);
            chunkDataMap.remove(chunkKey);
            problemChunks.remove(chunk);
            
            plugin.getLogger().info("Chunk réinitialisé: " + chunkKey);
            return true;
        } catch (Exception e) {
            plugin.getLogger().warning("Erreur lors de la réinitialisation du chunk: " + e.getMessage());
            return false;
        }
    }

    /**
     * Décharge un chunk
     * @param chunk Chunk à décharger
     * @return true si succès, false sinon
     */
    public boolean unloadChunk(Chunk chunk) {
        try {
            String chunkKey = getChunkKey(chunk);
            chunk.unload(true); // true = sauvegarder avant de décharger
            
            chunkDataMap.remove(chunkKey);
            problemChunks.remove(chunk);
            
            plugin.getLogger().info("Chunk déchargé: " + chunkKey);
            return true;
        } catch (Exception e) {
            plugin.getLogger().warning("Erreur lors du déchargement du chunk: " + e.getMessage());
            return false;
        }
    }

    /**
     * Recharge un chunk
     * @param chunk Chunk à recharger
     * @return true si succès, false sinon
     */
    public boolean reloadChunk(Chunk chunk) {
        try {
            int x = chunk.getX();
            int z = chunk.getZ();
            World world = chunk.getWorld();
            
            // Sauvegarder et décharger
            chunk.unload(true);
            
            // Recharger
            world.loadChunk(x, z);
            
            String chunkKey = getChunkKey(chunk);
            long startTime = System.currentTimeMillis();
            long loadTime = System.currentTimeMillis() - startTime;
            
            chunkDataMap.put(chunkKey, new ChunkData(world.getChunkAt(x, z), loadTime));
            chunkLoadTimes.put(chunkKey, loadTime);
            
            plugin.getLogger().info("Chunk réinstallé: " + chunkKey);
            return true;
        } catch (Exception e) {
            plugin.getLogger().warning("Erreur lors de la réinstallation du chunk: " + e.getMessage());
            return false;
        }
    }

    /**
     * Obtient les chunks problématiques
     * @return Set des chunks problématiques
     */
    public Set<Chunk> getProblemChunks() {
        return new HashSet<>(problemChunks);
    }

    /**
     * Ajoute un chunk problématique
     * @param chunk Chunk problématique
     */
    public void addProblemChunk(Chunk chunk) {
        problemChunks.add(chunk);
    }

    /**
     * Vérifie si un chunk est problématique
     * @param chunk Chunk à vérifier
     * @return true si problématique, false sinon
     */
    public boolean isProblemChunk(Chunk chunk) {
        return problemChunks.contains(chunk);
    }

    /**
     * Obtient les données d'un chunk
     * @param chunk Chunk cible
     * @return ChunkData ou null
     */
    public ChunkData getChunkData(Chunk chunk) {
        return chunkDataMap.get(getChunkKey(chunk));
    }

    /**
     * Obtient le temps de chargement d'un chunk
     * @param chunk Chunk cible
     * @return Temps de chargement en ms
     */
    public long getChunkLoadTime(Chunk chunk) {
        return chunkLoadTimes.getOrDefault(getChunkKey(chunk), -1L);
    }

    /**
     * Obtient tous les chunks problématiques d'un monde
     * @param world Monde cible
     * @return Map avec les temps de chargement
     */
    public Map<String, Long> getSlowChunksInWorld(World world) {
        Map<String, Long> slowChunks = new HashMap<>();
        
        for (Map.Entry<String, Long> entry : chunkLoadTimes.entrySet()) {
            if (entry.getValue() > 100) {
                slowChunks.put(entry.getKey(), entry.getValue());
            }
        }
        
        return slowChunks;
    }

    /**
     * Génère une clé unique pour un chunk
     * @param chunk Chunk cible
     * @return Clé unique
     */
    public String getChunkKey(Chunk chunk) {
        return chunk.getWorld().getName() + ":" + chunk.getX() + ":" + chunk.getZ();
    }

    /**
     * Enregistre les données d'un chunk
     * @param chunk Chunk cible
     * @param loadTime Temps de chargement
     */
    public void recordChunkData(Chunk chunk, long loadTime) {
        String chunkKey = getChunkKey(chunk);
        chunkDataMap.put(chunkKey, new ChunkData(chunk, loadTime));
        chunkLoadTimes.put(chunkKey, loadTime);
    }

    /**
     * Annule une tâche de préchargement
     * @param taskId ID de la tâche à annuler
     * @return true si annulée, false sinon
     */
    public boolean cancelPreload(int taskId) {
        if (preloadTasks.containsKey(taskId)) {
            Bukkit.getScheduler().cancelTask(taskId);
            preloadTasks.remove(taskId);
            plugin.getLogger().info("Préchargement annulé (ID: " + taskId + ")");
            return true;
        }
        return false;
    }

    /**
     * Annule tous les préchargements en cours
     * @return nombre de tâches annulées
     */
    public int cancelAllPreloads() {
        int count = preloadTasks.size();
        for (int taskId : new HashSet<>(preloadTasks.keySet())) {
            Bukkit.getScheduler().cancelTask(taskId);
            preloadTasks.remove(taskId);
        }
        if (count > 0) {
            plugin.getLogger().info("Tous les préchargements annulés (" + count + " tâches)");
        }
        return count;
    }

    /**
     * Obtient les IDs des tâches de préchargement en cours
     * @return Set des IDs de tâches
     */
    public Set<Integer> getActivePreloadTasks() {
        return new HashSet<>(preloadTasks.keySet());
    }

    /**
     * Génère une barre de progression avec 20 cases
     * @param loaded Nombre de chunks chargés
     * @param total Nombre total de chunks
     * @return Barre de progression visuelle
     */
    public static String getProgressBar(int loaded, int total) {
        int barSize = 20;
        int filledChars = (int) ((double) loaded / total * barSize);
        
        StringBuilder bar = new StringBuilder();
        bar.append("[");
        
        for (int i = 0; i < barSize; i++) {
            if (i < filledChars) {
                bar.append("█");
            } else {
                bar.append("░");
            }
        }
        
        bar.append("]");
        
        int percentage = (int) ((double) loaded / total * 100);
        bar.append(" ").append(percentage).append("% (").append(loaded).append("/").append(total).append(")");
        
        return bar.toString();
    }

    /**
     * Classe interne pour stocker les données d'un chunk
     */
    public static class ChunkData {
        public final Chunk chunk;
        public final long loadTime;
        public final long loadedAt;

        public ChunkData(Chunk chunk, long loadTime) {
            this.chunk = chunk;
            this.loadTime = loadTime;
            this.loadedAt = System.currentTimeMillis();
        }
    }
}