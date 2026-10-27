package com.voxelstand.block.entity;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

public class PlateCrafterBlockEntity extends BlockEntity {
    public float wheelAngle = 0.0f;
    public float prevWheelAngle = 0.0f;
    public float spinSpeed = 0.0f;
    public int spinTicksRemaining = 0;

    public PlateCrafterBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.PLATE_CRAFTER.get(), pos, state);
    }

    public void triggerSpin() {
        this.spinSpeed = 35.0f;
        this.spinTicksRemaining = 45;
        setChanged();
        if (level != null && !level.isClientSide) {
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
        }
    }

    public static void clientTick(Level level, BlockPos pos, BlockState state, PlateCrafterBlockEntity be) {
        be.prevWheelAngle = be.wheelAngle;
        if (be.spinTicksRemaining > 0) {
            be.wheelAngle = (be.wheelAngle + be.spinSpeed) % 360.0f;
            be.spinTicksRemaining--;
            be.spinSpeed *= 0.94f;

            // Высекаем искры из точки соприкосновения точила
            if (level.random.nextFloat() < 0.45f) {
                double px = pos.getX() + 9.0 / 16.0 + (level.random.nextDouble() - 0.5) * 0.12;
                double py = pos.getY() + 20.0 / 16.0;
                double pz = pos.getZ() + 8.0 / 16.0 + (level.random.nextDouble() - 0.5) * 0.12;
                level.addParticle(ParticleTypes.CRIT, px, py, pz, 0.0, 0.08, 0.0);
            }
        }
    }

    @Override
    public ClientboundBlockEntityDataPacket getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    @Override
    public CompoundTag getUpdateTag() {
        CompoundTag tag = super.getUpdateTag();
        tag.putFloat("WheelAngle", this.wheelAngle);
        tag.putFloat("SpinSpeed", this.spinSpeed);
        tag.putInt("SpinTicks", this.spinTicksRemaining);
        return tag;
    }

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
        this.wheelAngle = tag.getFloat("WheelAngle");
        this.spinSpeed = tag.getFloat("SpinSpeed");
        this.spinTicksRemaining = tag.getInt("SpinTicks");
    }

    @Override
    protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        tag.putFloat("WheelAngle", this.wheelAngle);
        tag.putFloat("SpinSpeed", this.spinSpeed);
        tag.putInt("SpinTicks", this.spinTicksRemaining);
    }
}
