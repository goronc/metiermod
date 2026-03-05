package com.goro.capability;

import com.goro.config.MetierLevelConfig;
import com.goro.data.MetierPrincipal;
import net.minecraft.nbt.CompoundTag;

public class PlayerMetier implements IPlayerMetier {

    private MetierPrincipal  principal      = MetierPrincipal.AUCUN;
    private MetierPrincipal secondaire      = MetierPrincipal.AUCUN;
    private int              principalLevel  = 1;
    private int              secondaireLevel = 1;
    private String           maitrise        = "";
    private int              maitriseLevel   = 1;
    private int              mineurLevel     = 1;
    private int              bucheronLevel   = 1;

    @Override public MetierPrincipal  getPrincipal()    { return principal;    }
    @Override public MetierPrincipal getSecondaire()   { return secondaire;   }
    @Override public String           getMaitrise()     { return maitrise;     }
    @Override public int              getMaitriseLevel(){ return maitriseLevel; }

    @Override public void setPrincipal(MetierPrincipal m)   { this.principal  = m; }
    @Override public void setSecondaire(MetierPrincipal m) { this.secondaire = m; }
    @Override public void setMaitrise(String m)             { this.maitrise   = m == null ? "" : m; }

    @Override
    public void setMaitriseLevel(int level) {
        int max = maitrise.isEmpty() ? 5 : MetierLevelConfig.getMaxLevel(maitrise);
        this.maitriseLevel = Math.max(1, Math.min(max, level));
    }

    @Override
    public int getPrincipalLevel() { return principalLevel; }

    @Override
    public void setPrincipalLevel(int level) {
        int max = MetierLevelConfig.getMaxLevel(principal.name());
        this.principalLevel = Math.max(1, Math.min(max, level));
    }

    @Override
    public int getSecondaireLevel() { return secondaireLevel; }

    @Override
    public void setSecondaireLevel(int level) {
        int max = MetierLevelConfig.getMaxLevel(secondaire.name());
        this.secondaireLevel = Math.max(1, Math.min(max, level));
    }

    @Override public int getMineurLevel()   { return mineurLevel;   }
    @Override public int getBucheronLevel() { return bucheronLevel; }

    @Override
    public void setMineurLevel(int level) {
        int max = MetierLevelConfig.getMaxLevel("MINEUR");
        this.mineurLevel = Math.max(1, Math.min(max, level));
    }

    @Override
    public void setBucheronLevel(int level) {
        int max = MetierLevelConfig.getMaxLevel("BUCHERON");
        this.bucheronLevel = Math.max(1, Math.min(max, level));
    }

    @Override
    public CompoundTag saveNBT() {
        CompoundTag nbt = new CompoundTag();
        nbt.putString("Principal",      principal.name());
        nbt.putString("Secondaire",     secondaire.name());
        nbt.putInt   ("PrincipalLevel", principalLevel);
        nbt.putInt   ("SecondaireLevel",secondaireLevel);
        nbt.putString("Maitrise",       maitrise);
        nbt.putInt   ("MaitriseLevel",  maitriseLevel);
        nbt.putInt   ("MineurLevel",    mineurLevel);
        nbt.putInt   ("BucheronLevel",  bucheronLevel);
        return nbt;
    }

    @Override
    public void loadNBT(CompoundTag nbt) {
        try { principal  = MetierPrincipal.valueOf(nbt.getString("Principal"));   } catch (Exception e) { principal  = MetierPrincipal.AUCUN;  }
        try { secondaire = MetierPrincipal.valueOf(nbt.getString("Secondaire")); } catch (Exception e) { secondaire = MetierPrincipal.AUCUN; }
        principalLevel  = nbt.contains("PrincipalLevel")  ? nbt.getInt("PrincipalLevel")  : 1;
        secondaireLevel = nbt.contains("SecondaireLevel") ? nbt.getInt("SecondaireLevel") : 1;
        maitrise        = nbt.contains("Maitrise")        ? nbt.getString("Maitrise")     : "";
        maitriseLevel   = nbt.contains("MaitriseLevel")   ? nbt.getInt("MaitriseLevel")   : 1;
        mineurLevel     = nbt.contains("MineurLevel")     ? nbt.getInt("MineurLevel")     : 1;
        bucheronLevel   = nbt.contains("BucheronLevel")   ? nbt.getInt("BucheronLevel")   : 1;
    }
}
