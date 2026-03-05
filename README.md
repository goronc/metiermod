# Métier Mod

Mod Minecraft Forge 1.20.1 développé par **Goro**.

Ajoute un système de **métiers** sur un serveur multijoueur : chaque joueur possède un métier principal, un secondaire et éventuellement une maîtrise. Selon son métier et son niveau, il peut ou non utiliser certains items, casser certains blocs, crafter des recettes, brasser des potions, élever des animaux, pêcher, chasser des monstres ou fondre des minerais.

---

## Fonctionnalités

| Catégorie | Description |
|-----------|-------------|
| **Items** | Restreindre l'utilisation, la casse de blocs, le craft ou l'interaction avec des blocs |
| **Chasse** | Restreindre l'attaque de mobs selon le niveau CHASSEUR |
| **Élevage** | Restreindre la reproduction et l'apprivoisement d'animaux |
| **Potions** | Restreindre le brassage de certaines potions |
| **Pêche** | Restreindre l'utilisation de la canne à pêche |
| **Forge** | Restreindre la fonte de minerais dans le smeltery Tinkers' Construct |
| **Niveaux** | Configurer le niveau maximum par métier (1–20) |
| **Maîtrises** | Définir des spécialisations débloquées au niveau max |
| **Couleurs de chat** | Personnaliser la couleur du pseudo selon le métier |

---

## Installation

1. Télécharger le fichier `.jar` depuis la page des releases
2. Le placer dans le dossier `mods/` du serveur **et** du client
3. Démarrer le serveur — la config est vide par défaut (aucune restriction active)

**Dépendances requises :** Minecraft Forge 1.20.1 (build 47.x ou supérieur)
**Optionnel :** Tinkers' Construct (pour la restriction de fonte)

---

## Utilisation

### Ouvrir l'interface de configuration

En jeu, depuis le client, utilisez la commande :
```
/metier config
```
> Réservé aux joueurs avec le permission level OP (niveau 4).

L'écran de configuration s'ouvre. Il contient 9 sections :

- **Items** — sélectionner un item dans la grille, clic gauche pour augmenter le niveau requis, clic droit pour diminuer. Filtrer par mod ou par recherche texte.
- **Niv.** — définir le niveau maximum par métier
- **Maît.** — saisir les noms des spécialisations disponibles au niveau max
- **Élev.** — définir le niveau requis pour élever/apprivoiser chaque animal
- **Pot.** — définir le niveau requis pour brasser chaque potion
- **Pêche** — définir le niveau requis pour pêcher selon le métier
- **Chas.** — définir le niveau CHASSEUR requis pour attaquer chaque mob (compatible tous mods)
- **Forge** — définir le niveau FORGERON requis pour fondre chaque item dans Tinkers'
- **Coul.** — saisir un code couleur hexadécimal pour le chat de chaque métier

Cliquer **Sauvegarder** applique la config immédiatement sur le serveur sans redémarrage.

---

### Commandes admin

| Commande | Description |
|----------|-------------|
| `/metier config` | Ouvre l'interface de configuration (OP uniquement) |
| `/metier admin bypass <joueur>` | Active/désactive le bypass total des restrictions pour un joueur |
| `/metier set principal <joueur> <métier>` | Définit le métier principal d'un joueur |
| `/metier set secondaire <joueur> <métier>` | Définit le métier secondaire d'un joueur |
| `/metier set maitrise <joueur> <spé>` | Définit la maîtrise d'un joueur |
| `/metier set niveau principal <joueur> <n>` | Définit le niveau de métier principal |
| `/metier set niveau secondaire <joueur> <n>` | Définit le niveau de métier secondaire |
| `/metier info <joueur>` | Affiche le métier et le niveau d'un joueur |

> Le **bypass** est temporaire : il se réinitialise au redémarrage du serveur.

---

### Commandes joueur

| Commande | Description |
|----------|-------------|
| `/metier info` | Affiche son propre métier et niveau |

---

## Métiers disponibles

Les métiers sont définis dans le code source (`MetierPrincipal`, `MetierSecondaire`).
Exemples typiques : `MINEUR`, `FORGERON`, `CHASSEUR`, `FERMIER`, `ALCHIMISTE`, `PECHEUR`...

---

## Configuration

La config est stockée sur le serveur et synchronisée automatiquement avec les clients à la connexion. Elle est sauvegardée dans :
```
world/metier_config.json
```

---

## Compilation

```bash
./gradlew build
```

Le JAR se trouve dans `build/libs/`.

---

*dev by Goro*
