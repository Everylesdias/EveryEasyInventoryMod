# Publier EveryEasyInventoryMod

Tout ce qu'il faut pour créer les pages Modrinth et CurseForge. Les comptes, les clés et l'envoi sont à faire
par vous.

## Avant de commencer

1. **Construire les jars** : `./gradlew build`, puis prendre
   - `fabric/build/libs/everyeasyinventorymod-fabric-26.3-1.3.0.jar`
   - `neoforge/build/libs/everyeasyinventorymod-neoforge-26.3-1.3.0.jar`

   (pas les `-sources` ni `-javadoc`).
2. **Publier le code sur GitHub** dans un dépôt public `Everylesdias/EveryEasyInventoryMod` : c'est le lien
   « Sources » déjà inscrit dans le mod, et la licence LGPL demande que le code soit disponible.
   Les liens sont réglés dans `gradle.properties` (`homepage_url`, `sources_url`, `issues_url`) si le nom change.
   Le dépôt local est prêt : branche `main`, remote `origin` vers ce dépôt, commits signés avec l'adresse
   GitHub masquée. Sur github.com → **New repository** → nom `EveryEasyInventoryMod`, **Public**, **sans**
   README, licence ni .gitignore (ils existent déjà), puis dans le dossier du projet :

   ```bash
   git push -u origin main
   ```

   Ne poussez que `main` (pas `--all`) : la branche locale `backup-avant-email` garde l'ancien historique avec
   l'adresse gmail, à supprimer quand tout est en ligne (`git branch -D backup-avant-email`).
   Dans **About** du dépôt, mettre la description et le site `https://modrinth.com/mod/everyeasyinventorymod`.
3. **Vérifier le nom** : `everyeasyinventorymod` était libre sur Modrinth le 2026-10-30. Sur CurseForge, le site
   le dira à la création du projet.

## Champs de la page (identiques sur les deux sites)

| Champ | Valeur |
|---|---|
| Nom | EveryEasyInventoryMod |
| Adresse (slug) | `everyeasyinventorymod` (le lien « page » du mod pointe vers `modrinth.com/mod/everyeasyinventorymod`) |
| Résumé | Sort, quick transfers, send to nearby chests, content holograms, item search and container renaming. |
| Description | contenu de [description.md](description.md) |
| Icône | [docs/icon/icon_512.png](../icon/icon_512.png) |
| Catégories | Storage, Utility (ou les plus proches proposées par le site) |
| Côtés | client et serveur requis (le client seul marche sur un serveur sans le mod, en mode réduit) |
| Licence | LGPL-3.0-only |
| Sources | https://github.com/Everylesdias/EveryEasyInventoryMod |
| Signaler un bug | https://github.com/Everylesdias/EveryEasyInventoryMod/issues |
| Galerie | captures de `docs/captures` (hologramme, recherche, renommage) — vos captures en jeu avec shaders rendent mieux |

## Versions à envoyer

| | Fichier | Loader | Minecraft | Dépendance | Nom de version |
|---|---|---|---|---|---|
| 1 | `everyeasyinventorymod-fabric-26.3-1.3.0.jar` | Fabric | 26.3 | Fabric API (requise) | 1.3.0+fabric |
| 2 | `everyeasyinventorymod-neoforge-26.3-1.3.0.jar` | NeoForge | 26.3 | aucune | 1.3.0+neoforge |

Canal : **Release**. Changelog : la section 1.3.0 de [CHANGELOG.md](../../CHANGELOG.md) (ou tout le fichier pour la
première publication).

## Après l'envoi

- La modération vérifie le projet avant qu'il soit public ; elle peut demander des corrections.
- Pour une mise à jour : changer `version` dans `gradle.properties`, compléter CHANGELOG.md, `./gradlew build`,
  envoyer les deux nouveaux jars.
- Publication automatique possible plus tard (un plugin Gradle qui envoie sur les deux sites en une commande,
  avec une clé API créée sur chaque site).
