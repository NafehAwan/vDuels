package org.bukkit.block;

public interface Block {
    org.bukkit.block.data.BlockData getBlockData();
    org.bukkit.Location getLocation();
    void setBlockData(org.bukkit.block.data.BlockData a0, boolean a1);
}
