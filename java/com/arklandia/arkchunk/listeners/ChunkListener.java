package com.arklandia.arkchunk.listeners;

import com.arklandia.arkchunk.ArkChunk;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.world.ChunkLoadEvent;
import org.bukkit.event.world.ChunkUnloadEvent;
import org.bukkit.Chunk;

/**
 * Listener pour les événements des chunks
 */
public class ChunkListener implements Listener {

    private final ArkChunk plugin;

    public ChunkListener(ArkChunk plugin) {
        this.plugin = plugin;
    }

    /**
     * Événement de chargement d'un chunk
     */
    @EventHandler
    public void onChunkLoad(ChunkLoadEvent event) {
        Chunk chunk = event.getChunk();
        long startTime = System.currentTimeMillis();
        long loadTime = System.currentTimeMillis() - startTime;
        
        // Enregistrer le temps de chargement
        plugin.getChunkManager().recordChunkData(chunk, loadTime);
    }

    /**
     * Événement de déchargement d'un chunk
     */
    @EventHandler
    public void onChunkUnload(ChunkUnloadEvent event) {
        // Les données seront supprimées par le ChunkManager lors du déchargement
    }
}