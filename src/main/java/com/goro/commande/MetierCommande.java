package com.goro.commande;

import java.util.List;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;

import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

import com.goro.capability.PlayerMetierProvider;
import com.goro.comportement.MetierComportements;
import com.goro.config.MetierLevelConfig;
import com.goro.data.MetierPrincipal;
import com.goro.data.MetierSecondaire;
import com.goro.network.MetierNetwork;
import com.goro.network.OpenGuiPacket;
import com.goro.network.SyncCapabilityPacket;
import net.minecraftforge.network.PacketDistributor;

public class MetierCommande {

    /** Évite literal() absent des mappings SRG — utilise Brigadier directement. */
    private static LiteralArgumentBuilder<CommandSourceStack> literal(String name) {
        return LiteralArgumentBuilder.literal(name);
    }

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {

        dispatcher.register(
            literal("metier")

                // /metier info
                .then(literal("info")
                    .executes(context -> {
                        ServerPlayer player = context.getSource().getPlayerOrException();
                        var cap = player.getCapability(PlayerMetierProvider.METIER_CAP).resolve();
                        if (cap.isPresent()) {
                            var c = cap.get();
                            int pMax = MetierLevelConfig.getMaxLevel(c.getPrincipal().name());
                            int sMax = MetierLevelConfig.getMaxLevel(c.getSecondaire().name());
                            String maitriseStr = c.getMaitrise().isEmpty() ? "" :
                                "  |  Maîtrise : " + c.getMaitrise() +
                                " (niv. " + c.getMaitriseLevel() + "/" + MetierLevelConfig.getMaxLevel(c.getMaitrise()) + ")";
                            player.sendSystemMessage(Component.literal(
                                "Principal : " + c.getPrincipal() + " (niv. " + c.getPrincipalLevel() + "/" + pMax + ")" +
                                "  |  Secondaire : " + c.getSecondaire() + " (niv. " + c.getSecondaireLevel() + "/" + sMax + ")" +
                                maitriseStr
                            ));
                            return 1;
                        } else {
                            player.sendSystemMessage(Component.literal("Erreur : capability non trouvée."));
                            return 0;
                        }
                    })
                )

                // /metier level add|remove <joueur> principal|secondaire  — op level 2
                .then(literal("level")
                    .requires(source -> source.hasPermission(2))

                    .then(literal("remove")
                        .then(Commands.argument("joueur", EntityArgument.player())
                            .then(literal("principal")
                                .executes(context -> {
                                    ServerPlayer target = EntityArgument.getPlayer(context, "joueur");
                                    target.getCapability(PlayerMetierProvider.METIER_CAP).ifPresent(cap -> {
                                        int actuel = cap.getPrincipalLevel();
                                        if (actuel <= 1) {
                                            context.getSource().sendSystemMessage(Component.literal(
                                                target.getName().getString() + " est déjà au niveau minimum (1) en principal."));
                                            return;
                                        }
                                        cap.setPrincipalLevel(actuel - 1);
                                        int nouveau = cap.getPrincipalLevel();
                                        int max = MetierLevelConfig.getMaxLevel(cap.getPrincipal().name());
                                        target.sendSystemMessage(Component.literal(
                                            "Ton métier principal " + cap.getPrincipal() + " est maintenant niveau " + nouveau + "/" + max + "."));
                                        context.getSource().sendSuccess(() -> Component.literal(
                                            target.getName().getString() + " → " + cap.getPrincipal() + " principal niv. " + nouveau + "/" + max), true);
                                    });
                                    SyncCapabilityPacket.sendTo(target);
                                    return 1;
                                })
                            )
                            .then(literal("secondaire")
                                .executes(context -> {
                                    ServerPlayer target = EntityArgument.getPlayer(context, "joueur");
                                    target.getCapability(PlayerMetierProvider.METIER_CAP).ifPresent(cap -> {
                                        int actuel = cap.getSecondaireLevel();
                                        if (actuel <= 1) {
                                            context.getSource().sendSystemMessage(Component.literal(
                                                target.getName().getString() + " est déjà au niveau minimum (1) en secondaire."));
                                            return;
                                        }
                                        cap.setSecondaireLevel(actuel - 1);
                                        int nouveau = cap.getSecondaireLevel();
                                        int max = MetierLevelConfig.getMaxLevel(cap.getSecondaire().name());
                                        target.sendSystemMessage(Component.literal(
                                            "Ton métier secondaire " + cap.getSecondaire() + " est maintenant niveau " + nouveau + "/" + max + "."));
                                        context.getSource().sendSuccess(() -> Component.literal(
                                            target.getName().getString() + " → " + cap.getSecondaire() + " secondaire niv. " + nouveau + "/" + max), true);
                                    });
                                    SyncCapabilityPacket.sendTo(target);
                                    return 1;
                                })
                            )
                            .then(literal("maitrise")
                                .executes(context -> {
                                    ServerPlayer target = EntityArgument.getPlayer(context, "joueur");
                                    target.getCapability(PlayerMetierProvider.METIER_CAP).ifPresent(cap -> {
                                        if (cap.getMaitrise().isEmpty()) {
                                            context.getSource().sendSystemMessage(Component.literal(
                                                target.getName().getString() + " n'a pas de métier de maîtrise."));
                                            return;
                                        }
                                        int actuel = cap.getMaitriseLevel();
                                        if (actuel <= 1) {
                                            context.getSource().sendSystemMessage(Component.literal(
                                                target.getName().getString() + " est déjà au niveau minimum (1) en maîtrise."));
                                            return;
                                        }
                                        cap.setMaitriseLevel(actuel - 1);
                                        int nouveau = cap.getMaitriseLevel();
                                        int max = MetierLevelConfig.getMaxLevel(cap.getMaitrise());
                                        target.sendSystemMessage(Component.literal(
                                            "Ton métier de maîtrise " + cap.getMaitrise() + " est maintenant niveau " + nouveau + "/" + max + "."));
                                        context.getSource().sendSuccess(() -> Component.literal(
                                            target.getName().getString() + " → maîtrise " + cap.getMaitrise() + " niv. " + nouveau + "/" + max), true);
                                    });
                                    SyncCapabilityPacket.sendTo(target);
                                    return 1;
                                })
                            )
                        )
                    )

                    .then(literal("add")
                        .then(Commands.argument("joueur", EntityArgument.player())
                            .then(literal("principal")
                                .executes(context -> {
                                    ServerPlayer target = EntityArgument.getPlayer(context, "joueur");
                                    target.getCapability(PlayerMetierProvider.METIER_CAP).ifPresent(cap -> {
                                        int actuel = cap.getPrincipalLevel();
                                        int max = MetierLevelConfig.getMaxLevel(cap.getPrincipal().name());
                                        if (actuel >= max) {
                                            context.getSource().sendSystemMessage(Component.literal(
                                                target.getName().getString() + " est déjà au niveau max (" + max + ") en principal."));
                                            return;
                                        }
                                        cap.setPrincipalLevel(actuel + 1);
                                        int nouveau = cap.getPrincipalLevel();
                                        // Notification des spécialisations disponibles au niveau max
                                        if (nouveau >= max && cap.getMaitrise().isEmpty()) {
                                            List<String> opts = MetierLevelConfig.getMaitriseOptions(cap.getPrincipal().name());
                                            if (!opts.isEmpty()) {
                                                target.sendSystemMessage(Component.literal(
                                                    "§6Niveau max atteint ! Choisis ta spécialisation avec §e/metier maitrise <nom>§6 :"));
                                                opts.forEach(opt -> target.sendSystemMessage(Component.literal("  §e- " + opt)));
                                            }
                                        }
                                        target.sendSystemMessage(Component.literal(
                                            "Ton métier principal " + cap.getPrincipal() + " est maintenant niveau " + nouveau + "/" + max + " !"));
                                        context.getSource().sendSuccess(() -> Component.literal(
                                            target.getName().getString() + " → " + cap.getPrincipal() + " principal niv. " + nouveau + "/" + max), true);
                                    });
                                    SyncCapabilityPacket.sendTo(target);
                                    return 1;
                                })
                            )
                            .then(literal("secondaire")
                                .executes(context -> {
                                    ServerPlayer target = EntityArgument.getPlayer(context, "joueur");
                                    target.getCapability(PlayerMetierProvider.METIER_CAP).ifPresent(cap -> {
                                        int actuel = cap.getSecondaireLevel();
                                        int max = MetierLevelConfig.getMaxLevel(cap.getSecondaire().name());
                                        if (actuel >= max) {
                                            context.getSource().sendSystemMessage(Component.literal(
                                                target.getName().getString() + " est déjà au niveau max (" + max + ") en secondaire."));
                                            return;
                                        }
                                        cap.setSecondaireLevel(actuel + 1);
                                        int nouveau = cap.getSecondaireLevel();
                                        if (nouveau >= max && cap.getMaitrise().isEmpty()) {
                                            List<String> opts = MetierLevelConfig.getMaitriseOptions(cap.getSecondaire().name());
                                            if (!opts.isEmpty()) {
                                                target.sendSystemMessage(Component.literal(
                                                    "§6Niveau max atteint ! Choisis ta spécialisation avec §e/metier maitrise <nom>§6 :"));
                                                opts.forEach(opt -> target.sendSystemMessage(Component.literal("  §e- " + opt)));
                                            }
                                        }
                                        target.sendSystemMessage(Component.literal(
                                            "Ton métier secondaire " + cap.getSecondaire() + " est maintenant niveau " + nouveau + "/" + max + " !"));
                                        context.getSource().sendSuccess(() -> Component.literal(
                                            target.getName().getString() + " → " + cap.getSecondaire() + " secondaire niv. " + nouveau + "/" + max), true);
                                    });
                                    SyncCapabilityPacket.sendTo(target);
                                    return 1;
                                })
                            )
                            .then(literal("maitrise")
                                .executes(context -> {
                                    ServerPlayer target = EntityArgument.getPlayer(context, "joueur");
                                    target.getCapability(PlayerMetierProvider.METIER_CAP).ifPresent(cap -> {
                                        if (cap.getMaitrise().isEmpty()) {
                                            context.getSource().sendSystemMessage(Component.literal(
                                                target.getName().getString() + " n'a pas de métier de maîtrise."));
                                            return;
                                        }
                                        int actuel = cap.getMaitriseLevel();
                                        int max = MetierLevelConfig.getMaxLevel(cap.getMaitrise());
                                        if (actuel >= max) {
                                            context.getSource().sendSystemMessage(Component.literal(
                                                target.getName().getString() + " est déjà au niveau max (" + max + ") en maîtrise."));
                                            return;
                                        }
                                        cap.setMaitriseLevel(actuel + 1);
                                        int nouveau = cap.getMaitriseLevel();
                                        target.sendSystemMessage(Component.literal(
                                            "Ton métier de maîtrise " + cap.getMaitrise() + " est maintenant niveau " + nouveau + "/" + max + " !"));
                                        context.getSource().sendSuccess(() -> Component.literal(
                                            target.getName().getString() + " → maîtrise " + cap.getMaitrise() + " niv. " + nouveau + "/" + max), true);
                                    });
                                    SyncCapabilityPacket.sendTo(target);
                                    return 1;
                                })
                            )
                        )
                    )
                )

                // /metier config — ouvre le GUI de configuration (op level 2)
                .then(literal("config")
                    .requires(source -> source.hasPermission(2))
                    .executes(context -> {
                        ServerPlayer player = context.getSource().getPlayerOrException();
                        MetierNetwork.CHANNEL.send(
                            PacketDistributor.PLAYER.with(() -> player),
                            new OpenGuiPacket(MetierLevelConfig.toJson())
                        );
                        return 1;
                    })
                )

                // /metier admin — gestion admin des métiers (op level 2)
                .then(literal("admin")
                    .requires(source -> source.hasPermission(2))

                    // set <joueur> principal|secondaire|maitrise <valeur>
                    .then(literal("set")
                        .then(Commands.argument("joueur", EntityArgument.player())
                            .then(literal("principal")
                                .then(Commands.argument("metier", StringArgumentType.word())
                                    .executes(context -> {
                                        ServerPlayer target = EntityArgument.getPlayer(context, "joueur");
                                        String input = StringArgumentType.getString(context, "metier").toUpperCase();
                                        try {
                                            MetierPrincipal m = MetierPrincipal.valueOf(input);
                                            target.getCapability(PlayerMetierProvider.METIER_CAP).ifPresent(cap -> {
                                                cap.setPrincipal(m);
                                                cap.setPrincipalLevel(1);
                                                cap.setMaitrise("");
                                                cap.setMaitriseLevel(0);
                                            });
                                            context.getSource().sendSuccess(() -> Component.literal(
                                                target.getName().getString() + " → principal : " + m + " niv.1 (maîtrise réinitialisée)"), true);
                                            target.sendSystemMessage(Component.literal("§6[Admin] Métier principal défini : §e" + m));
                                            SyncCapabilityPacket.sendTo(target);
                                            return 1;
                                        } catch (IllegalArgumentException e) {
                                            context.getSource().sendFailure(Component.literal("Métier principal invalide : " + input));
                                            return 0;
                                        }
                                    })
                                )
                            )
                            .then(literal("secondaire")
                                .then(Commands.argument("metier", StringArgumentType.word())
                                    .executes(context -> {
                                        ServerPlayer target = EntityArgument.getPlayer(context, "joueur");
                                        String input = StringArgumentType.getString(context, "metier").toUpperCase();
                                        try {
                                            MetierSecondaire m = MetierSecondaire.valueOf(input);
                                            target.getCapability(PlayerMetierProvider.METIER_CAP).ifPresent(cap -> {
                                                cap.setSecondaire(m);
                                                cap.setSecondaireLevel(1);
                                            });
                                            context.getSource().sendSuccess(() -> Component.literal(
                                                target.getName().getString() + " → secondaire : " + m + " niv.1"), true);
                                            target.sendSystemMessage(Component.literal("§6[Admin] Métier secondaire défini : §e" + m));
                                            SyncCapabilityPacket.sendTo(target);
                                            return 1;
                                        } catch (IllegalArgumentException e) {
                                            context.getSource().sendFailure(Component.literal("Métier secondaire invalide : " + input));
                                            return 0;
                                        }
                                    })
                                )
                            )
                            .then(literal("maitrise")
                                .then(Commands.argument("nom", StringArgumentType.word())
                                    .executes(context -> {
                                        ServerPlayer target = EntityArgument.getPlayer(context, "joueur");
                                        String nom = StringArgumentType.getString(context, "nom").toUpperCase();
                                        target.getCapability(PlayerMetierProvider.METIER_CAP).ifPresent(cap -> {
                                            cap.setMaitrise(nom);
                                            cap.setMaitriseLevel(1);
                                        });
                                        context.getSource().sendSuccess(() -> Component.literal(
                                            target.getName().getString() + " → maîtrise : " + nom + " niv.1"), true);
                                        target.sendSystemMessage(Component.literal("§6[Admin] Maîtrise définie : §e" + nom));
                                        SyncCapabilityPacket.sendTo(target);
                                        return 1;
                                    })
                                )
                            )
                        )
                    )

                    // remove <joueur> principal|secondaire|maitrise
                    .then(literal("remove")
                        .then(Commands.argument("joueur", EntityArgument.player())
                            .then(literal("principal")
                                .executes(context -> {
                                    ServerPlayer target = EntityArgument.getPlayer(context, "joueur");
                                    target.getCapability(PlayerMetierProvider.METIER_CAP).ifPresent(cap -> {
                                        cap.setPrincipal(MetierPrincipal.AUCUN);
                                        cap.setPrincipalLevel(0);
                                        cap.setMaitrise("");
                                        cap.setMaitriseLevel(0);
                                    });
                                    context.getSource().sendSuccess(() -> Component.literal(
                                        target.getName().getString() + " → principal retiré (maîtrise aussi réinitialisée)"), true);
                                    target.sendSystemMessage(Component.literal("§6[Admin] Ton métier principal a été retiré."));
                                    SyncCapabilityPacket.sendTo(target);
                                    return 1;
                                })
                            )
                            .then(literal("secondaire")
                                .executes(context -> {
                                    ServerPlayer target = EntityArgument.getPlayer(context, "joueur");
                                    target.getCapability(PlayerMetierProvider.METIER_CAP).ifPresent(cap -> {
                                        cap.setSecondaire(MetierSecondaire.AUCUN);
                                        cap.setSecondaireLevel(0);
                                    });
                                    context.getSource().sendSuccess(() -> Component.literal(
                                        target.getName().getString() + " → secondaire retiré"), true);
                                    target.sendSystemMessage(Component.literal("§6[Admin] Ton métier secondaire a été retiré."));
                                    SyncCapabilityPacket.sendTo(target);
                                    return 1;
                                })
                            )
                            .then(literal("maitrise")
                                .executes(context -> {
                                    ServerPlayer target = EntityArgument.getPlayer(context, "joueur");
                                    target.getCapability(PlayerMetierProvider.METIER_CAP).ifPresent(cap -> {
                                        cap.setMaitrise("");
                                        cap.setMaitriseLevel(0);
                                    });
                                    context.getSource().sendSuccess(() -> Component.literal(
                                        target.getName().getString() + " → maîtrise retirée"), true);
                                    target.sendSystemMessage(Component.literal("§6[Admin] Ta maîtrise a été retirée."));
                                    SyncCapabilityPacket.sendTo(target);
                                    return 1;
                                })
                            )
                        )
                    )

                    // setlevel <joueur> principal|secondaire|maitrise <niveau>
                    .then(literal("setlevel")
                        .then(Commands.argument("joueur", EntityArgument.player())
                            .then(literal("principal")
                                .then(Commands.argument("niveau", IntegerArgumentType.integer(0))
                                    .executes(context -> {
                                        ServerPlayer target = EntityArgument.getPlayer(context, "joueur");
                                        int niveau = IntegerArgumentType.getInteger(context, "niveau");
                                        target.getCapability(PlayerMetierProvider.METIER_CAP).ifPresent(cap ->
                                            cap.setPrincipalLevel(niveau));
                                        context.getSource().sendSuccess(() -> Component.literal(
                                            target.getName().getString() + " → principal niv. " + niveau), true);
                                        target.sendSystemMessage(Component.literal(
                                            "§6[Admin] Niveau principal défini à §e" + niveau));
                                        SyncCapabilityPacket.sendTo(target);
                                        return 1;
                                    })
                                )
                            )
                            .then(literal("secondaire")
                                .then(Commands.argument("niveau", IntegerArgumentType.integer(0))
                                    .executes(context -> {
                                        ServerPlayer target = EntityArgument.getPlayer(context, "joueur");
                                        int niveau = IntegerArgumentType.getInteger(context, "niveau");
                                        target.getCapability(PlayerMetierProvider.METIER_CAP).ifPresent(cap ->
                                            cap.setSecondaireLevel(niveau));
                                        context.getSource().sendSuccess(() -> Component.literal(
                                            target.getName().getString() + " → secondaire niv. " + niveau), true);
                                        target.sendSystemMessage(Component.literal(
                                            "§6[Admin] Niveau secondaire défini à §e" + niveau));
                                        SyncCapabilityPacket.sendTo(target);
                                        return 1;
                                    })
                                )
                            )
                            .then(literal("maitrise")
                                .then(Commands.argument("niveau", IntegerArgumentType.integer(0))
                                    .executes(context -> {
                                        ServerPlayer target = EntityArgument.getPlayer(context, "joueur");
                                        int niveau = IntegerArgumentType.getInteger(context, "niveau");
                                        target.getCapability(PlayerMetierProvider.METIER_CAP).ifPresent(cap ->
                                            cap.setMaitriseLevel(niveau));
                                        context.getSource().sendSuccess(() -> Component.literal(
                                            target.getName().getString() + " → maîtrise niv. " + niveau), true);
                                        target.sendSystemMessage(Component.literal(
                                            "§6[Admin] Niveau maîtrise défini à §e" + niveau));
                                        SyncCapabilityPacket.sendTo(target);
                                        return 1;
                                    })
                                )
                            )
                        )
                    )

                    // bypass <joueur> — active/désactive le bypass de toutes les restrictions
                    .then(literal("bypass")
                        .then(Commands.argument("joueur", EntityArgument.player())
                            .executes(context -> {
                                ServerPlayer target = EntityArgument.getPlayer(context, "joueur");
                                java.util.UUID uid = target.getUUID();
                                boolean active;
                                if (MetierComportements.bypassPlayers.contains(uid)) {
                                    MetierComportements.bypassPlayers.remove(uid);
                                    active = false;
                                } else {
                                    MetierComportements.bypassPlayers.add(uid);
                                    active = true;
                                }
                                String state = active ? "§aACTIVÉ" : "§cDÉSACTIVÉ";
                                target.sendSystemMessage(Component.literal(
                                    "§6[Admin] Bypass des restrictions métier : " + state));
                                context.getSource().sendSuccess(() -> Component.literal(
                                    target.getName().getString() + " → bypass " + (active ? "activé" : "désactivé")), true);
                                return 1;
                            })
                        )
                    )
                )

        );
    }
}
