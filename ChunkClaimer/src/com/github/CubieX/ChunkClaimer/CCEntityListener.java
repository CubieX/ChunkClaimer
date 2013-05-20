package com.github.CubieX.ChunkClaimer;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.bukkit.ChatColor;
import org.bukkit.Chunk;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import com.sk89q.worldguard.bukkit.WorldGuardPlugin;
import com.sk89q.worldguard.protection.regions.ProtectedCuboidRegion;
import com.sk89q.worldedit.BlockVector;

public class CCEntityListener implements Listener
{
   private ChunkClaimer plugin = null;
   WorldGuardPlugin weInst = null;
   List<Block> borderBlocks = new ArrayList<Block>();

   public CCEntityListener(ChunkClaimer plugin, WorldGuardPlugin weInst)
   {
      this.plugin = plugin;
      this.weInst = weInst;

      plugin.getServer().getPluginManager().registerEvents(this, plugin);
   }

   //================================================================================================
   @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
   public void onPlayerInteract(PlayerInteractEvent e)
   {
      if(e.getAction() == Action.RIGHT_CLICK_BLOCK)
      {
         // CHUNK FINDER ======================================================
         if(e.getPlayer().getItemInHand().getType() == Material.BONE)
         {
            if((e.getPlayer().isOp()) ||
                  (e.getPlayer().hasPermission("chunkclaimer.admin") ||
                        (e.getPlayer().hasPermission("chunkclaimer.buy"))))
            {
               Chunk chunk = e.getClickedBlock().getChunk();

               e.getPlayer().sendMessage("This is chunk: X= " + chunk.getX() + " | Z= " + chunk.getZ() + " (Chunk-Coords)");

               ChunkFinderUtil.revertBorderBlocks(e.getPlayer(), borderBlocks);
               ChunkFinderUtil.reCalculateBorderBlocks(chunk, borderBlocks);
               ChunkFinderUtil.sendBorderBlocks(e.getPlayer(), borderBlocks);                
            }
         }
         // ===================================================================

         // CHUNK PROTECTION CREATOR ========================================================
         if(e.getPlayer().getItemInHand().getType() == Material.BLAZE_ROD)
         {
            if((e.getPlayer().isOp()) ||
                  (e.getPlayer().hasPermission("chunkclaimer.admin") ||
                        (e.getPlayer().hasPermission("chunkclaimer.buy"))))
            {
               Chunk chunk = e.getClickedBlock().getChunk();
               // get both points to define this region
               BlockVector bvMin = WEWGutil.convertToSk89qBV(ChunkFinderUtil.getLowerChunkDelimitingLocation(chunk));
               BlockVector bvMax = WEWGutil.convertToSk89qBV(ChunkFinderUtil.getUpperChunkDelimitingLocation(chunk));
               // define a region
               ProtectedCuboidRegion reg = new ProtectedCuboidRegion("fb_" + chunk.getX() + "_" + chunk.getZ(), bvMin, bvMax);

               if(weInst.canBuild(e.getPlayer(), e.getClickedBlock()))
               {
                  //if(reg.getIntersectingRegions(arg0))  // TODO check if there is a intersecting region which does not allow building for that player and cancel
                  weInst.getRegionManager(e.getPlayer().getWorld()).addRegion(reg);

                  try
                  {
                     weInst.getRegionManager(e.getPlayer().getWorld()).save();
                     e.getPlayer().sendMessage("Region " + "fb_" + chunk.getX() + "_" + chunk.getZ() + " has been created.");
                  }
                  catch (IOException ex)
                  {
                     e.getPlayer().sendMessage(ChunkClaimer.logPrefix + ChatColor.RED + "ERROR while saving this region!");
                     ChunkClaimer.log.severe(ChunkClaimer.logPrefix + "ERROR while saving this region!");
                  }
               }
               else
               {
                  e.getPlayer().sendMessage("Sorry. You are not allowed to protect this area, because you have no building rights here.");
               }
            }
         }
         // =================================================================================

         // CHUNK PROTECTION REMOVER ========================================================
         if(e.getPlayer().getItemInHand().getType() == Material.STICK)
         {
            if((e.getPlayer().isOp()) ||
                  (e.getPlayer().hasPermission("chunkclaimer.admin") ||
                        (e.getPlayer().hasPermission("chunkclaimer.buy"))))
            {
               Chunk chunk = e.getClickedBlock().getChunk();            
               weInst.getRegionManager(e.getPlayer().getWorld()).removeRegion("fb_" + chunk.getX() + "_" + chunk.getZ());

               try
               {
                  weInst.getRegionManager(e.getPlayer().getWorld()).save();
                  e.getPlayer().sendMessage("Region " + "fb_" + chunk.getX() + "_" + chunk.getZ() + " has been removed.");
               }
               catch (IOException ex)
               {
                  e.getPlayer().sendMessage(ChunkClaimer.logPrefix + ChatColor.RED + "ERROR while saving this region!");
                  ChunkClaimer.log.severe(ChunkClaimer.logPrefix + "ERROR while saving this region!");
               }
            }
         }
         // =================================================================================
      }
   }
}

