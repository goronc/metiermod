package com.goro.capability;

import com.goro.data.MetierPrincipal;
import com.goro.data.MetierSecondaire;
import net.minecraft.nbt.CompoundTag;

public interface IPlayerMetier {

    MetierPrincipal getPrincipal();
    void setPrincipal(MetierPrincipal metier);

    MetierSecondaire getSecondaire();
    void setSecondaire(MetierSecondaire metier);

    int getPrincipalLevel();
    void setPrincipalLevel(int level);

    int getSecondaireLevel();
    void setSecondaireLevel(int level);

    /** Nom du métier de maîtrise obtenu (chaîne vide = aucun). */
    String getMaitrise();
    void setMaitrise(String maitrise);

    int getMaitriseLevel();
    void setMaitriseLevel(int level);

    CompoundTag saveNBT();
    void loadNBT(CompoundTag nbt);
}