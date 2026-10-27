package com.voxelstand.voxel;

import com.voxelstand.api.VoxelConstants;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

/**
 * Ультракомпактное RLE-сжатие воксельной сетки 32x32x32.
 * Сохраняет 32768 ячеек в сжатый массив байт для NBT (обычно от 300 до 1200 байт).
 */
public final class VoxelCompressor {
    private VoxelCompressor() {}

    /**
     * Сжатие сетки VoxelGrid в массив байтов (RLE + GZIP).
     */
    public static byte[] compress(VoxelGrid grid) {
        byte[] raw = grid.getRawData();
        ByteArrayOutputStream baos = new ByteArrayOutputStream();

        try (GZIPOutputStream gzos = new GZIPOutputStream(baos);
             DataOutputStream out = new DataOutputStream(gzos)) {

            int currentLength = 0;
            byte currentMaterial = raw[0];

            for (int i = 0; i < raw.length; i++) {
                byte mat = (byte) (raw[i] & VoxelConstants.MATERIAL_MASK);

                if (mat == currentMaterial && currentLength < 32767) {
                    currentLength++;
                } else {
                    out.writeShort(currentLength);
                    out.writeByte(currentMaterial);

                    currentMaterial = mat;
                    currentLength = 1;
                }
            }

            // Запись последнего блока
            if (currentLength > 0) {
                out.writeShort(currentLength);
                out.writeByte(currentMaterial);
            }

        } catch (IOException e) {
            throw new RuntimeException("Error compressing voxel grid", e);
        }

        return baos.toByteArray();
    }

    /**
     * Распаковка сжатого массива байтов обратно в VoxelGrid.
     */
    public static VoxelGrid decompress(byte[] compressedData) {
        VoxelGrid grid = new VoxelGrid();
        if (compressedData == null || compressedData.length == 0) {
            return grid;
        }

        byte[] raw = grid.getRawData();
        int writeIndex = 0;

        try (ByteArrayInputStream bais = new ByteArrayInputStream(compressedData);
             GZIPInputStream gzis = new GZIPInputStream(bais);
             DataInputStream in = new DataInputStream(gzis)) {

            while (in.available() > 0 && writeIndex < raw.length) {
                int length;
                try {
                    length = in.readUnsignedShort();
                } catch (IOException eof) {
                    break;
                }
                byte material = (byte) (in.readByte() & VoxelConstants.MATERIAL_MASK);

                int end = Math.min(writeIndex + length, raw.length);
                while (writeIndex < end) {
                    raw[writeIndex++] = material;
                }
            }

        } catch (IOException e) {
            throw new RuntimeException("Error decompressing voxel grid", e);
        }

        grid.recalculateCount();
        return grid;
    }
}
