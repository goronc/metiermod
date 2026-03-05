package com.goro.data;

import java.util.HashMap;
import java.util.UUID;

public class PlayerMetierData {

    public static class Data {
        public MetierPrincipal principal = MetierPrincipal.AUCUN;
        public MetierSecondaire secondaire = MetierSecondaire.AUCUN;
    }

    private static final HashMap<UUID, Data> PLAYERS = new HashMap<>();

    public static Data get(UUID id) {
        return PLAYERS.computeIfAbsent(id, k -> new Data());
    }

}