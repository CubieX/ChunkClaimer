package com.github.CubieX.ChunkClaimer;

import java.util.HashMap;
import java.util.List;

import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitTask;

public class CCSchedulerHandler
{
   private ChunkClaimer plugin = null;

   public CCSchedulerHandler(ChunkClaimer plugin)
   {
      this.plugin = plugin;
   }

   public BukkitTask startChunkMarkingTimer_Delayed(final Player player, final List<Block> borderBlocks, final HashMap<String, Integer> playersWithActiveQuery)
   {
      BukkitTask task = plugin.getServer().getScheduler().runTaskLater(plugin, new Runnable()
      {
         public void run()
         {
            if(player.isOnline())
            {
               ChunkFinderUtil.revertBorderBlocks(player, borderBlocks);
            }

            if(playersWithActiveQuery.containsKey(player.getName()))
            {
               playersWithActiveQuery.remove(player.getName());
            }
         }
      }, ChunkClaimer.chunkMarkingTime * 20L);

      return task;
   }
}
