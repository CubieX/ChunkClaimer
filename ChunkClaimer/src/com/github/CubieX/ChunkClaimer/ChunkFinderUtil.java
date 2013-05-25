package com.github.CubieX.ChunkClaimer;

import java.util.List;
import org.bukkit.Chunk;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;

public final class ChunkFinderUtil
{
   
   private ChunkFinderUtil()   
   {
      // static class
   }
   
   public static void reCalculateBorderBlocks(Chunk chunk, List<Block> borderBlocks)
   {
      borderBlocks.clear();

      // start locations are a chunks 4 corners (in 2D)
      Location corner1 = chunk.getBlock(0, 0, 0).getLocation();
      Location corner2 = chunk.getBlock(15, 0, 0).getLocation();
      Location corner3 = chunk.getBlock(0, 0, 15).getLocation();
      Location corner4 = chunk.getBlock(15, 0, 15).getLocation();

      // calculate edge blocks for each side (cornerX-vars are actually now edge blocks and not only corners
      for (int i = 0; i < 15; i = i + 3)
      {
         corner1 = chunk.getBlock(i, 0, 0).getLocation();
         corner2 = chunk.getBlock(15, 0, i).getLocation();
         corner3 = chunk.getBlock(15 - i, 0, 15).getLocation();
         corner4 = chunk.getBlock(0, 0, 15 - i).getLocation();

         borderBlocks.add(corner1.getWorld().getHighestBlockAt(corner1));
         borderBlocks.add(corner2.getWorld().getHighestBlockAt(corner2));
         borderBlocks.add(corner3.getWorld().getHighestBlockAt(corner3));
         borderBlocks.add(corner4.getWorld().getHighestBlockAt(corner4));
      }
   }

   public static void placeOutlineForClaimedChunk(Chunk chunk, List<Block> borderBlocks)
   {
      borderBlocks.clear();

      // start locations are a chunks 4 corners (in 2D)
      Location corner1 = chunk.getBlock(0, 0, 0).getLocation();
      Location corner2 = chunk.getBlock(15, 0, 0).getLocation();
      Location corner3 = chunk.getBlock(0, 0, 15).getLocation();
      Location corner4 = chunk.getBlock(15, 0, 15).getLocation();

      // calculate edge blocks for each side (cornerX-vars are actually now edge blocks and not only corners
      for (int i = 0; i < 15; i++)
      {
         corner1 = chunk.getBlock(i, 0, 0).getLocation();
         corner2 = chunk.getBlock(15, 0, i).getLocation();
         corner3 = chunk.getBlock(15 - i, 0, 15).getLocation();
         corner4 = chunk.getBlock(0, 0, 15 - i).getLocation();

         borderBlocks.add(corner1.getWorld().getHighestBlockAt(corner1).getRelative(0, -1, 0));
         borderBlocks.add(corner2.getWorld().getHighestBlockAt(corner2).getRelative(0, -1, 0));
         borderBlocks.add(corner3.getWorld().getHighestBlockAt(corner3).getRelative(0, -1, 0));
         borderBlocks.add(corner4.getWorld().getHighestBlockAt(corner4).getRelative(0, -1, 0));
      }
            
      for (Block block : borderBlocks)
      {
         block.setTypeId(43);        
      }
   }

   public static void revertBorderBlocks(Player player, List<Block> borderBlocks)
   {
      for (Block block : borderBlocks)
      {
         player.sendBlockChange(block.getLocation(), block.getType(), block.getData());
      }
   }

   public static void sendBorderBlocks(Player player, List<Block> borderBlocks)
   {
      for (Block block : borderBlocks)
      {
         player.sendBlockChange(block.getLocation(), Material.TORCH, (byte)0);
      }
   }

   public static Location getUpperChunkDelimitingLocation(Chunk chunk)
   {      
      Location upperCorner = chunk.getBlock(15, chunk.getWorld().getMaxHeight() - 1, 15).getLocation();

      return upperCorner;
   }

   public static Location getLowerChunkDelimitingLocation(Chunk chunk)
   {
      Location lowerCorner = chunk.getBlock(0, 0, 0).getLocation();      

      return lowerCorner;
   }
}
