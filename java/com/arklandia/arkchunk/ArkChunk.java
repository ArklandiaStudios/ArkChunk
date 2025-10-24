package com.arklandia.arkchunk;

import com.arklandia.arkchunk.commands.ChunkCommand;
import com.arklandia.arkchunk.listeners.ChunkListener;
import com.arklandia.arkchunk.managers.ChunkManager;
import com.arklandia.arkchunk.managers.ChunkMonitor;
import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;

/**
 * Plugin ArkChunk - Gestionnaire puissant des chunks pour Spigot 1.21.8+
 * Créé par Arklandia Studios
 */
public class ArkChunk extends JavaPlugin {

    private static ArkChunk instance;
    private ChunkManager chunkManager;
    private ChunkMonitor chunkMonitor;

    @Override
    public void onEnable() {
        instance = this;
        
        getLogger().info("╔════════════════════════════════════╗");
        getLogger().info("║     ArkChunk v1.0.0 - Activé       ║");
        getLogger().info("║     Arklandia Studios 2025         ║");
        getLogger().info("╚════════════════════════════════════╝");

        // Initialiser les managers
        this.chunkManager = new ChunkManager(this);
        this.chunkMonitor = new ChunkMonitor(this);

        // Enregistrer les commandes
        getCommand("arkchunk").setExecutor(new ChunkCommand(this));
        
        // Enregistrer les listeners
        Bukkit.getPluginManager().registerEvents(new ChunkListener(this), this);

        // Démarrer le moniteur de chunks
        chunkMonitor.startMonitoring();

        getLogger().info("ArkChunk est prêt !");
    }

    @Override
    public void onDisable() {
        getLogger().info("╔════════════════════════════════════╗");
        getLogger().info("║     ArkChunk v1.0.0 - Désactivé    ║");
        getLogger().info("║     Arklandia Studios 2025         ║");
        getLogger().info("╚════════════════════════════════════╝");
        
        if (chunkMonitor != null) {
            chunkMonitor.stopMonitoring();
        }
    }

    public static ArkChunk getInstance() {
        return instance;
    }

    public ChunkManager getChunkManager() {
        return chunkManager;
    }

    public ChunkMonitor getChunkMonitor() {
        return chunkMonitor;
    }
}