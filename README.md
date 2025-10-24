# 🏗️ ArkChunk - Plugin de Gestion des Chunks

**Créé par Arklandia Studios**

Un plugin Paper haute performance pour gérer, surveiller et optimiser les chunks de votre serveur Minecraft 1.21.10+.

---

## ✨ Fonctionnalités Principales

### 🔄 Préchargement des Chunks
- **Précharge complète** dans un rayon configurable
- **Précharge autour du joueur** avec rayon ajustable
- **Chargement asynchrone** (sans bloquer le serveur)
- Suivi détaillé des temps de chargement

### 📊 Surveillance & Détection Intelligente
- **Détection automatique** des chunks problématiques
- **Seuils configurables** :
  - 🚨 Trop d'entités (> 500)
  - 🚨 Trop de Tile Entities (> 200)
  - 🚨 Trop de blocs spécialisés (> 150)
- **Notifications en temps réel** pour les administrateurs
- **Historique complet** des erreurs par chunk

### 🎮 Gestion Complète des Chunks
| Commande | Action |
|----------|--------|
| **delete** | Supprime complètement le chunk |
| **reset** | Recharge depuis le disque |
| **unload** | Décharge (packets arrêtés) |
| **reload** | Sauvegarde + recharge |
| **info** | Détails complets du chunk |

---

## 🔐 Système de Permissions

```
arkchunk.admin         → Accès complet
arkchunk.preload       → Précharger les chunks
arkchunk.monitor       → Voir les problèmes
arkchunk.delete        → Supprimer les chunks
arkchunk.reset         → Réinitialiser les chunks
arkchunk.unload        → Décharger les chunks
arkchunk.reload        → Réinstaller les chunks
arkchunk.info          → Voir les infos
```

---

## 🎯 Commandes

### Informations & Monitoring
```bash
/arkchunk info              # Info du chunk actuel
/arkchunk status            # Statut global du serveur
/arkchunk problems          # Liste des chunks problématiques
/arkchunk actions <x> <z>   # Actions disponibles
/arkchunk help              # Aide complète
```

### Gestion des Chunks
```bash
/arkchunk preload [rayon]   # Précharge (rayon défaut: 10)
/arkchunk delete <x> <z>    # Supprime un chunk
/arkchunk reset <x> <z>     # Réinitialise un chunk
/arkchunk unload <x> <z>    # Décharge un chunk
/arkchunk reload <x> <z>    # Réinstalle un chunk
```

---

## 📋 Détails Chunk (Info)

Chaque chunk affiche :
- 📍 Coordonnées (X, Z) et Monde
- 👥 Nombre d'entités
- 📦 Nombre de Tile Entities
- ⚠️ Statut (problématique ou normal)
- ⏱️ Temps de chargement

---

## 🚀 Optimisations

✅ **Asynchrone** - Opérations lourdes en threads séparés  
✅ **Thread-safe** - ConcurrentHashMap pour éviter les conflits  
✅ **Faible impact** - Surveillance toutes les 10 secondes  
✅ **Smart** - Notifications max toutes les 5 minutes par chunk

---

## 📦 Configuration Requise

| Composant | Version |
|-----------|---------|
| **Serveur** | Paper 1.21.10+ |
| **Java** | Java 21+ |

---

## 📊 Critères de Détection

Un chunk est flaggé comme **problématique** si :
- ❌ > 500 entités
- ❌ > 200 Tile Entities
- ❌ > 150 blocs spécialisés (ticking)
  
---

## 🤝 Support & Contribution

Pour toute question ou problème :
- 📧 Contactez **Arklandia Studios**
- ✅ Vérifiez Java 21+ est installé
- ✅ Confirmez Paper 1.21.10+ est en place

---

## 📄 Licence

**GNU Affero General Public License v3.0 (AGPL-3.0)**

Créé par **Arklandia Studios** - 2025

```
This program is free software: you can redistribute it and/or modify
it under the terms of the GNU Affero General Public License as published by
the Free Software Foundation, either version 3 of the License.

This program is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
```

📖 [Lire la licence complète](https://www.gnu.org/licenses/agpl-3.0.html)

---

**Version** : 1.0.0  
**Dernière mise à jour** : 2025
