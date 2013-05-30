package com.github.CubieX.ChunkClaimer;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import net.milkbowl.vault.economy.Economy;
import net.milkbowl.vault.permission.Permission;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Chunk;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerQuitEvent;

import com.sk89q.worldguard.LocalPlayer;
import com.sk89q.worldguard.bukkit.BukkitPlayer;
import com.sk89q.worldguard.bukkit.WorldGuardPlugin;
import com.sk89q.worldguard.protection.ApplicableRegionSet;
import com.sk89q.worldguard.protection.GlobalRegionManager;
import com.sk89q.worldguard.protection.managers.RegionManager;
import com.sk89q.worldguard.protection.regions.ProtectedCuboidRegion;
import com.sk89q.worldedit.BlockVector;

public class CCEntityListener implements Listener
{
   private ChunkClaimer plugin = null;
   WorldGuardPlugin wgInst = null;
   Permission perm = null;
   Economy econ = null;   
   HashMap<String, List<Block>> playersMarkedBorderBlocks = new HashMap<String, List<Block>>();
   String buildState = "No";
   GlobalRegionManager wgGlobalRM; // RegionManager that can access any given world
   CCSchedulerHandler schedHandler = null;

   public CCEntityListener(ChunkClaimer plugin, WorldGuardPlugin wgInst, Permission perm, Economy econ, CCSchedulerHandler schedHandler)
   {
      this.plugin = plugin;
      this.wgInst = wgInst;
      this.perm = perm;
      this.econ = econ;
      wgGlobalRM = wgInst.getGlobalRegionManager();
      this.schedHandler = schedHandler;

      plugin.getServer().getPluginManager().registerEvents(this, plugin);
   }

   //================================================================================================
   @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
   public void onPlayerInteract(PlayerInteractEvent e)
   {
      if(e.getAction() == Action.RIGHT_CLICK_BLOCK)
      {
         if(plugin.getConfig().getStringList("enabledWorlds").contains(e.getPlayer().getWorld().getName()))
         {
            RegionManager wgCurrWorldRM = wgInst.getRegionManager(e.getPlayer().getWorld()); // regionManager for current world
            Chunk chunk = e.getClickedBlock().getChunk();
            String ccChunkRegionName = ChunkClaimer.ccRegionPrefix + "_" + chunk.getX() + "_" + chunk.getZ(); // if there is or will be an applicable region, it is called like this
            LocalPlayer lPlayer = new BukkitPlayer(wgInst, e.getPlayer());

            // CHUNK FINDER ======================================================
            if(e.getPlayer().getItemInHand().getType() == Material.BONE)
            {
               if((e.getPlayer().isOp()) ||                  
                     (e.getPlayer().hasPermission("chunkclaimer.buy")))
               {
                  if(ChunkClaimer.language.equals("de")){e.getPlayer().sendMessage(ChatColor.WHITE + "Dies ist Chunk: X= " + chunk.getX() + " | Z= " + chunk.getZ() + " (Chunk-Koords)");}
                  if(ChunkClaimer.language.equals("en")){e.getPlayer().sendMessage(ChatColor.WHITE + "This is chunk: X= " + chunk.getX() + " | Z= " + chunk.getZ() + " (Chunk-Coords)");}

                  if(wgCurrWorldRM.hasRegion(ccChunkRegionName))
                  {
                     if(ChunkClaimer.language.equals("de")){e.getPlayer().sendMessage(ChatColor.GREEN + plugin.getDescription().getName() + "-Protection: " + ChatColor.WHITE + ccChunkRegionName + ChatColor.GREEN + "\n" +
                           "Besitzer: " + ChatColor.WHITE + plugin.getOwnerNamesOfRegionAsString(wgCurrWorldRM, ccChunkRegionName) + "\n" + ChatColor.GREEN + 
                           "Freunde: " + ChatColor.WHITE + plugin.getMemberNamesOfRegion(wgCurrWorldRM, ccChunkRegionName));}

                     if(ChunkClaimer.language.equals("en")){e.getPlayer().sendMessage(ChatColor.GREEN + plugin.getDescription().getName() + "-Protection: " + ChatColor.WHITE + ccChunkRegionName + ChatColor.GREEN + "\n" +
                           "Owners: " + ChatColor.WHITE + plugin.getOwnerNamesOfRegionAsString(wgCurrWorldRM, ccChunkRegionName) + "\n" + ChatColor.GREEN + 
                           "Friends: " + ChatColor.WHITE + plugin.getMemberNamesOfRegion(wgCurrWorldRM, ccChunkRegionName));}

                     if(plugin.chunkIsOnSale(e.getPlayer().getWorld().getName(), ccChunkRegionName))
                     {
                        int price = plugin.getPriceOfChunkOnSale(e.getPlayer().getWorld().getName(), ccChunkRegionName);

                        if(plugin.getSellingPlayerOfChunkOnSale(e.getPlayer().getWorld().getName(), ccChunkRegionName).equals(e.getPlayer().getName()))
                        {
                           if(ChunkClaimer.language.equals("de")){e.getPlayer().sendMessage(ChatColor.WHITE + "Du hast diesen Chunk zum Verkauf gesetzt fuer " + ChatColor.GREEN + price + " " + ChunkClaimer.currency + ChatColor.WHITE + ".");}
                           if(ChunkClaimer.language.equals("en")){e.getPlayer().sendMessage(ChatColor.WHITE + "You have set this chunk on sale for " + ChatColor.GREEN + price + " " + ChunkClaimer.currency + ChatColor.WHITE + ".");}
                        }
                        else
                        {
                           if(ChunkClaimer.language.equals("de")){e.getPlayer().sendMessage(ChatColor.WHITE + "Du kannst diesen Chunk kaufen fuer " + ChatColor.GREEN + price + " " + ChunkClaimer.currency + ChatColor.WHITE + ".");}
                           if(ChunkClaimer.language.equals("en")){e.getPlayer().sendMessage(ChatColor.WHITE + "You can buy this chunk for " + ChatColor.GREEN + price + " " + ChunkClaimer.currency + ChatColor.WHITE + ".");}
                        }                        
                     }
                  }
                  else
                  {
                     // get both points to define this region
                     BlockVector bvMin = WEWGutil.convertToSk89qBV(ChunkFinderUtil.getLowerChunkDelimitingLocation(chunk));
                     BlockVector bvMax = WEWGutil.convertToSk89qBV(ChunkFinderUtil.getUpperChunkDelimitingLocation(chunk));
                     // define a region
                     ProtectedCuboidRegion reg = new ProtectedCuboidRegion(ccChunkRegionName, bvMin, bvMax);

                     // FIXME deprecated. evt. ersetzen.
                     ApplicableRegionSet arSet = wgCurrWorldRM.getApplicableRegions(reg);

                     if(ChunkClaimer.language.equals("de")){e.getPlayer().sendMessage(ChatColor.WHITE + "Keine " + plugin.getDescription().getName() + "-Protection gefunden.");}
                     if(ChunkClaimer.language.equals("en")){e.getPlayer().sendMessage(ChatColor.WHITE + "No " + plugin.getDescription().getName() + "-Protection found.");}

                     if(e.getPlayer().isOp() ||
                           e.getPlayer().hasPermission("chunkclaimer.admin") ||
                           (arSet.canBuild(lPlayer)))
                     {
                        if(plugin.clearanceZoneIsMaintained(wgCurrWorldRM, chunk, e.getPlayer().getName()))
                        {
                           if(ChunkClaimer.language.equals("de")){buildState = ChatColor.GREEN + "Ja";}
                           if(ChunkClaimer.language.equals("en")){buildState = ChatColor.GREEN + "Yes";}

                           int price = plugin.getPriceOfNewChunkProtection(lPlayer);

                           if(ChunkClaimer.language.equals("de")){e.getPlayer().sendMessage(ChatColor.WHITE + "Du kannst diesen Chunk beanspruchen fuer " + ChatColor.GREEN + price + " " + ChunkClaimer.currency);}
                           if(ChunkClaimer.language.equals("en")){e.getPlayer().sendMessage(ChatColor.WHITE + "You can claim this chunk for " + ChatColor.GREEN + price + " " + ChunkClaimer.currency);}
                        }
                        else
                        {
                           if(ChunkClaimer.language.equals("de")){e.getPlayer().sendMessage(ChatColor.YELLOW + "Dieser Chunk ist nicht beanspruchbar, da er zu nahe an fremden Regionen liegt.");}
                           if(ChunkClaimer.language.equals("en")){e.getPlayer().sendMessage(ChatColor.YELLOW + "This chunk is not claimable, because it's too close to foreign regions.");}
                        }
                     }
                     else
                     {
                        if(ChunkClaimer.language.equals("de")){e.getPlayer().sendMessage(ChatColor.YELLOW + "Dieser Chunk ist nicht beanspruchbar, da du hier kein Baurecht hast.");}
                        if(ChunkClaimer.language.equals("en")){e.getPlayer().sendMessage(ChatColor.YELLOW + "This chunk is not claimable, because you have no building rights here.");}
                     }
                  }

                  // set the new chunk marking with torches =============================

                  if(!playersMarkedBorderBlocks.containsKey(e.getPlayer().getName()))
                  {
                     List<Block> borderBlocks = new ArrayList<Block>();
                     playersMarkedBorderBlocks.put(e.getPlayer().getName(), borderBlocks); // initialize borderBlock list for player
                  }

                  ChunkFinderUtil.revertBorderBlocks(e.getPlayer(), playersMarkedBorderBlocks.get(e.getPlayer().getName()));
                  ChunkFinderUtil.reCalculateBorderBlocks(chunk, playersMarkedBorderBlocks.get(e.getPlayer().getName()));
                  ChunkFinderUtil.sendBorderBlocks(e.getPlayer(), playersMarkedBorderBlocks.get(e.getPlayer().getName()));

                  if(ChunkClaimer.playersWithActiveQuery.containsKey(e.getPlayer().getName()))
                  {
                     // cancel existing query timer for player
                     Bukkit.getServer().getScheduler().cancelTask(ChunkClaimer.playersWithActiveQuery.get(e.getPlayer().getName()));                     
                  }

                  // create new or update existing query timer for player and store TaskID of new timer to prevent multiple timers running for this player                  
                  int taskID = schedHandler.startChunkMarkingTimer_Delayed(e.getPlayer(), playersMarkedBorderBlocks.get(e.getPlayer().getName()), ChunkClaimer.playersWithActiveQuery).getTaskId();
                  ChunkClaimer.playersWithActiveQuery.put(e.getPlayer().getName(), taskID);
               }

               return;
            }
            // ===================================================================

            // CHUNK PROTECTION CREATOR ========================================================
            /*if(e.getPlayer().getItemInHand().getType() == Material.BLAZE_ROD)
            {
               if((e.getPlayer().isOp()) ||
                     (e.getPlayer().hasPermission("chunkclaimer.buy")))
               {
                  if(wgInst.canBuild(e.getPlayer(), e.getClickedBlock()))
                  {
                     if(!wgCurrWorldRM.hasRegion(ccChunkRegionName)) // Try only to create a new region, if a ChunkClaimer protection is not already existing at this point
                     {
                        int playerRegionCount = plugin.getPlayersGlobalCCregionCount(wgGlobalRM, lPlayer);

                        if(ChunkClaimer.debug){e.getPlayer().sendMessage(ChatColor.WHITE + "Du besitzt global " + ChatColor.GREEN + playerRegionCount + " / " + plugin.getPlayersGlobalClaimLimit(e.getPlayer()) + ChatColor.WHITE + " Chunks.");}

                        if(plugin.getPlayersGlobalClaimLimit(e.getPlayer()) > playerRegionCount)
                        {
                           DefaultDomain owners = new DefaultDomain();
                           DefaultDomain members = new DefaultDomain();
                           owners.addPlayer(lPlayer);

                           // this is the group of the player (needed later)
                           DefaultDomain playerGroup = new DefaultDomain();
                           playerGroup.addGroup(perm.getPrimaryGroup(e.getPlayer()));

                           // add defined groups to protection as members
                           if(null != ChunkClaimer.autoAddGroupsAsMembers)
                           {
                              for(String group : ChunkClaimer.autoAddGroupsAsMembers)
                              {
                                 members.addGroup(group);
                              }
                           }

                           // get both points to define this region
                           BlockVector bvMin = WEWGutil.convertToSk89qBV(ChunkFinderUtil.getLowerChunkDelimitingLocation(chunk));
                           BlockVector bvMax = WEWGutil.convertToSk89qBV(ChunkFinderUtil.getUpperChunkDelimitingLocation(chunk));
                           // define a region
                           ProtectedCuboidRegion reg = new ProtectedCuboidRegion(ccChunkRegionName, bvMin, bvMax);
                           reg.setOwners(owners);
                           reg.setMembers(members);
                           reg.setPriority(1); // must be the priority of the underlying region + 1 to protect it properly in freebuild

                           // FIXME deprecated. evt. ersetzen.
                           ApplicableRegionSet arSet = wgCurrWorldRM.getApplicableRegions(reg);

                           List<ProtectedRegion> prList = new ArrayList<ProtectedRegion>();
                           prList.add(reg);

                           if(e.getPlayer().isOp() ||
                                 e.getPlayer().hasPermission("chunkclaimer.admin") ||
                                 arSet.canBuild(lPlayer))
                           {
                              if(econ.has(e.getPlayer().getName(), ChunkClaimer.basePricePerClaimedRegion)) // has player enough money?
                              {
                                 EconomyResponse ecoRes = econ.withdrawPlayer(e.getPlayer().getName(), ChunkClaimer.basePricePerClaimedRegion);
                                 if(ecoRes.transactionSuccess()) // claimed region successfully payed
                                 {
                                    // Surrounding areas are free to create that region and player has been charged successfully, so create protection for that player
                                    wgCurrWorldRM.addRegion(reg);

                                    if(ChunkClaimer.language.equals("de")){e.getPlayer().sendMessage(ChatColor.GREEN + "Dir wurden " + ChatColor.WHITE + ChunkClaimer.basePricePerClaimedRegion + " " + ChunkClaimer.currency + ChatColor.GREEN + " abgezogen.");}
                                    if(ChunkClaimer.language.equals("en")){e.getPlayer().sendMessage(ChatColor.GREEN + "You have been charged with " + ChatColor.WHITE + ChunkClaimer.basePricePerClaimedRegion + " " + ChunkClaimer.currency + ChatColor.GREEN + ".");}

                                    if(WEWGutil.saveWGregionManager(wgCurrWorldRM)) // Try to save all region changes
                                    {
                                       // don't place outline blocks in the Nether, because they will end up on the roof
                                       if(e.getPlayer().getWorld().getEnvironment() != Environment.NETHER)
                                       {
                                          ChunkFinderUtil.placeOutlineForClaimedChunk(chunk, borderBlocks);
                                       }

                                       playerRegionCount = plugin.getPlayersGlobalCCregionCount(wgGlobalRM, lPlayer);

                                       if(ChunkClaimer.language.equals("de")){e.getPlayer().sendMessage(ChatColor.GREEN + "Du bist jetzt Besitzer der Region " + ChatColor.WHITE + ccChunkRegionName + ChatColor.GREEN + ".\n" +
                                             "Du besitzt nun " + ChatColor.WHITE + playerRegionCount + "/" + plugin.getPlayersGlobalClaimLimit(e.getPlayer()) + ChatColor.GREEN + " Chunks.");}

                                       if(ChunkClaimer.language.equals("en")){e.getPlayer().sendMessage(ChatColor.GREEN + "You are now owner of region " + ccChunkRegionName + ChatColor.WHITE + ccChunkRegionName + ChatColor.GREEN + ".\n" +
                                             "You are now owning " + ChatColor.WHITE + playerRegionCount + "/" + plugin.getPlayersGlobalClaimLimit(e.getPlayer()) + ChatColor.GREEN + " Chunks.");}
                                    }
                                    else
                                    {
                                       if(ChunkClaimer.language.equals("de")){e.getPlayer().sendMessage(ChunkClaimer.logPrefix + ChatColor.RED + "FEHLER beim Speichern dieser Region!");}
                                       if(ChunkClaimer.language.equals("en")){e.getPlayer().sendMessage(ChunkClaimer.logPrefix + ChatColor.RED + "ERROR while saving this region!");}

                                       ChunkClaimer.log.severe(ChunkClaimer.logPrefix + "ERROR while saving this region!");
                                    }
                                 }
                                 else
                                 {
                                    // Eco transfer failed. Abort.
                                    if(ChunkClaimer.language.equals("de")){e.getPlayer().sendMessage(ChatColor.RED + "Fehler beim Bezahlen der Region " + ccChunkRegionName + ". Bitte informiere einen Admin!");}
                                    if(ChunkClaimer.language.equals("en")){e.getPlayer().sendMessage(ChatColor.RED + "Error on withdrawing amout for claiming region " + ccChunkRegionName + ". Please inform an Admin!");}

                                    ChunkClaimer.log.severe("Error on charging " + e.getPlayer().getName() + " for region " + ccChunkRegionName);
                                 }
                              }
                              else
                              {
                                 if(ChunkClaimer.language.equals("de")){e.getPlayer().sendMessage(ChatColor.GOLD + "Du hast nicht genuegend Geld (" + ChatColor.WHITE + ChunkClaimer.basePricePerClaimedRegion + ChatColor.GOLD + ") um dir diesen Chunk leisten zu koennen!");}
                                 if(ChunkClaimer.language.equals("en")){e.getPlayer().sendMessage(ChatColor.GOLD + "You do not have enough money (" + ChatColor.WHITE + ChunkClaimer.basePricePerClaimedRegion + ChatColor.GOLD + ") to afford this chunk!");}
                              }                           
                           }
                           else
                           {
                              if(ChunkClaimer.language.equals("de")){e.getPlayer().sendMessage(ChatColor.GOLD + "Kauf nicht moeglich. Die Region ueberschneidet sich mit anderen Regionen die dir nicht gehoeren.");}
                              if(ChunkClaimer.language.equals("en")){e.getPlayer().sendMessage(ChatColor.GOLD + "Purchase not possible. This region is intersecting other regions you do not own.");}
                           }
                        }
                        else
                        {
                           if(ChunkClaimer.language.equals("de")){e.getPlayer().sendMessage(ChatColor.GOLD + "Du hast dein Limit von " + ChatColor.WHITE + plugin.getPlayersGlobalClaimLimit(e.getPlayer()) + ChatColor.GOLD + " Regionen die du besitzen kannst erreicht!");}
                           if(ChunkClaimer.language.equals("en")){e.getPlayer().sendMessage(ChatColor.GOLD + "You have reached your limit of " + ChatColor.WHITE + plugin.getPlayersGlobalClaimLimit(e.getPlayer()) + ChatColor.GOLD + " regions that you can claim!");}
                        }
                     }
                     else
                     {  
                        if(ChunkClaimer.language.equals("de")){e.getPlayer().sendMessage(ChatColor.GOLD + "Dieser Chunk " + ChatColor.WHITE + ccChunkRegionName + ChatColor.GOLD + " ist schon geschuetzt durch " + plugin.getDescription().getName() + "\n" +
                              "Besitzer: " + ChatColor.WHITE + plugin.getOwnerNamesOfRegionAsString(wgCurrWorldRM, ccChunkRegionName));}

                        if(ChunkClaimer.language.equals("en")){e.getPlayer().sendMessage(ChatColor.GOLD + "This chunk " + ChatColor.WHITE + ccChunkRegionName + ChatColor.GOLD + " is already protected by " + plugin.getDescription().getName() + "\n" +
                              "Besitzer: " + ChatColor.WHITE + plugin.getOwnerNamesOfRegionAsString(wgCurrWorldRM, ccChunkRegionName));}
                     }
                  }
                  else
                  {
                     if(ChunkClaimer.language.equals("de")){e.getPlayer().sendMessage(ChatColor.GOLD + "Du kannst diese Region nicht fuer dich beanspruchen, weil du hier kein Baurecht hast.");}
                     if(ChunkClaimer.language.equals("en")){e.getPlayer().sendMessage(ChatColor.GOLD + "You are not allowed to claim this area, because you have no building rights here.");}
                  }
               }

               return;
            }*/
            // =================================================================================

            // CHUNK PROTECTION REMOVER ========================================================
            if(e.getPlayer().getItemInHand().getType() == Material.STICK)
            {
               if((e.getPlayer().isOp()) ||
                     (e.getPlayer().hasPermission("chunkclaimer.admin")))
               {
                  if(wgCurrWorldRM.hasRegion(ccChunkRegionName))
                  {
                     wgInst.getRegionManager(e.getPlayer().getWorld()).removeRegion(ccChunkRegionName);

                     try
                     {
                        wgInst.getRegionManager(e.getPlayer().getWorld()).save();

                        if(ChunkClaimer.language.equals("de")){e.getPlayer().sendMessage(ChatColor.WHITE + "Region " + ChatColor.GREEN + ccChunkRegionName + ChatColor.WHITE + " wurde entfernt.");}

                        if(ChunkClaimer.language.equals("en")){e.getPlayer().sendMessage(ChatColor.WHITE + "Region " + ChatColor.GREEN + ccChunkRegionName + ChatColor.WHITE + " has been removed.");}                        
                     }
                     catch (IOException ex)
                     {
                        if(ChunkClaimer.language.equals("de")){e.getPlayer().sendMessage(ChunkClaimer.logPrefix + ChatColor.RED + "FEHLER beim Speichern dieser Region!");}
                        if(ChunkClaimer.language.equals("en")){e.getPlayer().sendMessage(ChunkClaimer.logPrefix + ChatColor.RED + "ERROR while saving this region!");}

                        ChunkClaimer.log.severe(ChunkClaimer.logPrefix + "ERROR while saving this region!");
                     }
                  }
                  else
                  {
                     if(ChunkClaimer.language.equals("de")){e.getPlayer().sendMessage(ChatColor.WHITE + "Es ist keine " + plugin.getDescription().getName() + "-Region fuer diesen Chunk vorhanden.");}
                     if(ChunkClaimer.language.equals("en")){e.getPlayer().sendMessage(ChatColor.WHITE + "There is no " + plugin.getDescription().getName() + " protection present for this chunk.");}
                  }
               }

               return;
            }
         }
      }
      // =================================================================================
   }

   //================================================================================================
   @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
   public void onPlayerQuitEvent(PlayerQuitEvent e)
   {
      if(ChunkClaimer.debug){ChunkClaimer.log.info(e.getPlayer().getName() + "Has left the game. Borderblocks deleted.");}

      // cleanup
      if(playersMarkedBorderBlocks.containsKey(e.getPlayer().getName()))
      {
         playersMarkedBorderBlocks.remove(e.getPlayer().getName());
      }
   }

   // ##########################################################################################


}

