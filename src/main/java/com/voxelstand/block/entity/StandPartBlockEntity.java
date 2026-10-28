package com.voxelstand.block.entity;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

public class StandPartBlockEntity extends BlockEntity {
    private BlockPos masterPos = BlockPos.ZERO;

    public StandPartBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.STAND_PART.get(), pos, state);
    }

    public BlockPos getMasterPos() {
        return masterPos;
    }

    public void setMasterPos(BlockPos pos) {
        this.masterPos = pos;
        setChanged();
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        if (masterPos != null) {
            tag.put("MasterPos", NbtUtils.writeBlockPos(masterPos));
        }
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        if (tag.contains("MasterPos")) {
            NbtUtils.readBlockPos(tag, "MasterPos").ifPresent(p -> this.masterPos = p);
        }
    }
}
