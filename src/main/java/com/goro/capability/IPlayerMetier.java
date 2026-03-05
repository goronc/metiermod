package com.goro.capability;

import com.goro.data.MetierPrincipal;
import net.minecraft.nbt.CompoundTag;

public interface IPlayerMetier {

    MetierPrincipal getPrincipal();
    void setPrincipal(MetierPrincipal metier);

    MetierPrincipal getSecondaire();
    void setSecondaire(MetierPrincipal metier);

    int getPrincipalLevel();
    void setPrincipalLevel(int level);

    int getSecondaireLevel();
    void setSecondaireLevel(int level);

    /** Nom du métier de maîtrise obtenu (chaîne vide = aucun). */
    String getMaitrise();
    void setMaitrise(String maitrise);

    int getMaitriseLevel();
    void setMaitriseLevel(int level);

    int getMineurLevel();
    void setMineurLevel(int level);

    int getBucheronLevel();
    void setBucheronLevel(int level);

    CompoundTag saveNBT();
    void loadNBT(CompoundTag nbt);
}