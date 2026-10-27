package com.voxelstand.blueprint;

import com.voxelstand.api.ItemPoseData;
import com.voxelstand.block.entity.SculptStandBlockEntity;
import com.voxelstand.voxel.VoxelGrid;

import java.util.HashMap;
import java.util.Map;

/**
 * Структура данных чертежа воксельной модели.
 */
public class BlueprintData {
    public String name;
    public String author;
    public SculptStandBlockEntity.StandMode mode = SculptStandBlockEntity.StandMode.WEAPON;
    public String baseItem = "minecraft:iron_sword";
    public int totalVoxels = 0;
    public float qualityModifier = 1.0f;
    public ItemPoseData pose = new ItemPoseData();
    public final Map<String, VoxelGrid> grids = new HashMap<>();

    public BlueprintData() {}

    public BlueprintData(String name, String author) {
        this.name = name;
        this.author = author;
    }
}
