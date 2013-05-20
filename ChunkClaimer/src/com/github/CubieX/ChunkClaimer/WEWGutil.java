package com.github.CubieX.ChunkClaimer;

import org.bukkit.Location;

import com.sk89q.worldedit.BlockVector;

public class WEWGutil
{
   // This converts a Bukkit BlockVector to a WorldEdit BlockVector
   public static BlockVector convertToSk89qBV(Location location)
   {
      return (new BlockVector(location.getX(), location.getY(), location.getZ()));
   }
}
