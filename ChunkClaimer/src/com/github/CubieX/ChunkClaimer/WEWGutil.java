package com.github.CubieX.ChunkClaimer;

import java.io.IOException;

import org.bukkit.ChatColor;
import org.bukkit.Location;

import com.sk89q.worldedit.BlockVector;
import com.sk89q.worldguard.protection.managers.RegionManager;

public class WEWGutil
{
   // This converts a Bukkit BlockVector to a WorldEdit BlockVector
   public static BlockVector convertToSk89qBV(Location location)
   {
      return (new BlockVector(location.getX(), location.getY(), location.getZ()));
   }

   public static boolean saveWGregionManager(RegionManager wgRM)
   {
      boolean saveOK = false;
      
      try
      {
         wgRM.save();
         saveOK = true;         
      }
      catch (IOException ex)
      {
         ChunkClaimer.log.severe("ERROR while saving RegionManager changes to file!");         
      }
      
      return (saveOK);
   }
}
