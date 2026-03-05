package com.goro.capability;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.CapabilityManager;
import net.minecraftforge.common.capabilities.CapabilityToken;
import net.minecraftforge.common.capabilities.ICapabilityProvider;
import net.minecraftforge.common.capabilities.ICapabilitySerializable;
import net.minecraftforge.common.util.LazyOptional;

public class PlayerMetierProvider implements ICapabilityProvider, ICapabilitySerializable<CompoundTag> {

    public static final Capability<IPlayerMetier> METIER_CAP =
            CapabilityManager.get(new CapabilityToken<>() {});

    private final PlayerMetier backend = new PlayerMetier();
    private final LazyOptional<IPlayerMetier> optional = LazyOptional.of(() -> backend);

    @Override
    public <T> @NotNull LazyOptional<T> getCapability(@NotNull Capability<T> cap, @Nullable Direction side) {
        return cap == METIER_CAP ? optional.cast() : LazyOptional.empty();
    }

    @Override
    public CompoundTag serializeNBT() {
        return backend.saveNBT();
    }

    @Override
    public void deserializeNBT(CompoundTag nbt) {
        backend.loadNBT(nbt);
    }
}