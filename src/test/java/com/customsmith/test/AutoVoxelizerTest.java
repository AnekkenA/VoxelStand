package com.voxelstand.test;

import com.voxelstand.api.VoxelConstants;
import com.voxelstand.voxel.AutoVoxelizer;
import com.voxelstand.voxel.VoxelGrid;
import com.voxelstand.voxel.VoxelMaterial;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class AutoVoxelizerTest {

    @Test
    public void testWeaponAutoVoxelization() {
        VoxelGrid grid = new VoxelGrid();
        ItemStack diamondSword = new ItemStack(Items.DIAMOND_SWORD);

        AutoVoxelizer.voxelizeWeapon(diamondSword, grid);

        int count = grid.getNonEmptyCount();
        Assertions.assertTrue(count > 50, "Авто-каркас меча должен содержать достаточное число вокселей, факт: " + count);
        Assertions.assertTrue(count < VoxelConstants.DEFAULT_VOXEL_LIMIT, "Каркас не должен превышать лимит вокселей");

        // Проверяем наличие алмазных вокселей клинка (Y=20)
        Assertions.assertEquals(VoxelMaterial.DIAMOND.getId(), grid.get(15, 20, 15), "Воксель лезвия должен быть алмазным");

        // Проверяем наличие рукояти (Y=9)
        Assertions.assertEquals(VoxelMaterial.DARK_WOOD.getId(), grid.get(15, 9, 15), "Воксель рукояти должен быть деревянным");
    }

    @Test
    public void testHelmetAutoVoxelization() {
        VoxelGrid head = new VoxelGrid();
        ItemStack ironHelmet = new ItemStack(Items.IRON_HELMET);

        AutoVoxelizer.voxelizeHelmet(ironHelmet, head);

        int count = head.getNonEmptyCount();
        Assertions.assertTrue(count > 100, "Шлем должен формировать купол вокруг головы, вокселей: " + count);

        // Макушка (Y=24) должна быть железной
        Assertions.assertEquals(VoxelMaterial.IRON.getId(), head.get(16, 24, 16));
    }

    @Test
    public void testShieldAutoVoxelization() {
        VoxelGrid shield = new VoxelGrid();
        ItemStack shieldItem = new ItemStack(Items.SHIELD);

        AutoVoxelizer.voxelizeShield(shieldItem, shield);

        int count = shield.getNonEmptyCount();
        Assertions.assertTrue(count > 100, "Щит должен иметь достаточное число вокселей, факт: " + count);
        Assertions.assertEquals(VoxelMaterial.IRON.getId(), shield.get(15, 15, 17), "Умбон щита должен быть железным");
        Assertions.assertEquals(VoxelMaterial.OAK.getId(), shield.get(10, 10, 15), "Поле щита должно быть дубовым");
    }

    @Test
    public void testBowAutoVoxelization() {
        VoxelGrid bow = new VoxelGrid();
        ItemStack bowItem = new ItemStack(Items.BOW);

        AutoVoxelizer.voxelizeBow(bowItem, bow);

        int count = bow.getNonEmptyCount();
        Assertions.assertTrue(count > 20, "Лук должен формировать дугу и тетиву, факт: " + count);
        Assertions.assertEquals(VoxelMaterial.QUARTZ.getId(), bow.get(15, 10, 17), "Тетива должна быть нитью/кварцем");
    }

    @Test
    public void testCrossbowAutoVoxelization() {
        VoxelGrid crossbow = new VoxelGrid();
        ItemStack crossbowItem = new ItemStack(Items.CROSSBOW);

        AutoVoxelizer.voxelizeCrossbow(crossbowItem, crossbow);

        int count = crossbow.getNonEmptyCount();
        Assertions.assertTrue(count > 40, "Арбалет должен иметь ложе и дугу, факт: " + count);
        Assertions.assertEquals(VoxelMaterial.DARK_WOOD.getId(), crossbow.get(15, 15, 10), "Ложе арбалета должно быть деревянным");
        Assertions.assertEquals(VoxelMaterial.IRON.getId(), crossbow.get(10, 15, 24), "Дуга арбалета должна быть железной");
    }
}
