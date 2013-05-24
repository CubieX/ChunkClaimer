package com.github.CubieX.ChunkClaimer;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import net.milkbowl.vault.economy.Economy;
import net.milkbowl.vault.economy.EconomyResponse;
import net.milkbowl.vault.permission.Permission;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Chunk;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.World.Environment;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import com.sk89q.worldguard.LocalPlayer;
import com.sk89q.worldguard.bukkit.BukkitPlayer;
import com.sk89q.worldguard.bukkit.WorldGuardPlugin;
import com.sk89q.worldguard.domains.DefaultDomain;
import com.sk89q.worldguard.protection.ApplicableRegionSet;
import com.sk89q.worldguard.protection.GlobalRegionManager;
import com.sk89q.worldguard.protection.managers.RegionManager;
import com.sk89q.worldguard.protection.regions.ProtectedCuboidRegion;
import com.sk89q.worldguard.protection.regions.ProtectedRegion;
import com.sk89q.worldedit.BlockVector;

public class CCEntityListener implements Listener
{
   private ChunkClaimer plugin = null;
   WorldGuardPlugin wgInst = null;
   Permission perm = null;
   Economy econ = null;
   List<Block> borderBlocks = new ArrayList<Block>();
   String buildState = "No";
   GlobalRegionManager wgGlobalRM; // RegionManager that can access any given world

   public CCEntityListener(ChunkClaimer plugin, WorldGuardPlugin wgInst, Permission perm, Economy econ)
   {
      this.plugin = plugin;
      this.wgInst = wgInst;
      this.perm = perm;
      this.econ = econ;
      wgGlobalRM = wgInst.getGlobalRegionManager();

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
                           "Besitzer: " + ChatColor.WHITE + getOwnerNamesOfRegion(wgCurrWorldRM, ccChunkRegionName) + "\n" + ChatColor.GREEN + 
                           "Freunde: " + ChatColor.WHITE + getMemberNamesOfRegion(wgCurrWorldRM, ccChunkRegionName));}

                     if(ChunkClaimer.language.equals("en")){e.getPlayer().sendMessage(ChatColor.GREEN + plugin.getDescription().getName() + "-Protection: " + ChatColor.WHITE + ccChunkRegionName + ChatColor.GREEN + "\n" +
                           "Owners: " + ChatColor.WHITE + getOwnerNamesOfRegion(wgCurrWorldRM, ccChunkRegionName) + "\n" + ChatColor.GREEN + 
                           "Friends: " + ChatColor.WHITE + getMemberNamesOfRegion(wgCurrWorldRM, ccChunkRegionName));}
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

                     if(e.getPlayer().isOp() ||
                           e.getPlayer().hasPermission("chunkclaimer.admin") ||
                           arSet.canBuild(lPlayer))
                     {
                        if(ChunkClaimer.language.equals("de")){buildState = ChatColor.GREEN + "Ja";}
                        if(ChunkClaimer.language.equals("en")){buildState = ChatColor.GREEN + "Yes";}
                     }
                     else
                     {
                        if(ChunkClaimer.language.equals("de")){buildState = ChatColor.RED + "Nein";}
                        if(ChunkClaimer.language.equals("en")){buildState = ChatColor.RED + "No";}
                     }

                     if(ChunkClaimer.language.equals("de")){e.getPlayer().sendMessage(ChatColor.WHITE + "Keine " + plugin.getDescription().getName() + "-Protection gefunden.\n" +
                           "Kannst du hier bauen und eine Region beanspruchen: " + buildState);}
                     if(ChunkClaimer.language.equals("en")){e.getPlayer().sendMessage(ChatColor.WHITE + "No " + plugin.getDescription().getName() + "-Protection found.\n" +
                           "Can you build and claim a region here: " + buildState);}
                  }

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
                     (e.getPlayer().hasPermission("chunkclaimer.buy")))
               {
                  if(wgInst.canBuild(e.getPlayer(), e.getClickedBlock()))
                  {
                     if(!wgCurrWorldRM.hasRegion(ccChunkRegionName)) // Try only to create a new region, if a ChunkClaimer protection is not already existing at this point
                     {
                        int playerRegionCount = getPlayersGlobalCCregionCount(lPlayer);

                        if(ChunkClaimer.debug){e.getPlayer().sendMessage(ChatColor.WHITE + "Du besitzt global " + ChatColor.GREEN + playerRegionCount + " / " + getPlayersGlobalClaimLimit(e.getPlayer()) + ChatColor.WHITE + " Chunks.");}

                        if(getPlayersGlobalClaimLimit(e.getPlayer()) > playerRegionCount)
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

                                       playerRegionCount = getPlayersGlobalCCregionCount(lPlayer);

                                       if(ChunkClaimer.language.equals("de")){e.getPlayer().sendMessage(ChatColor.GREEN + "Du bist jetzt Besitzer der Region " + ChatColor.WHITE + ccChunkRegionName + ChatColor.GREEN + ".\n" +
                                             "Du besitzt nun " + ChatColor.WHITE + playerRegionCount + "/" + getPlayersGlobalClaimLimit(e.getPlayer()) + ChatColor.GREEN + " Chunks.");}

                                       if(ChunkClaimer.language.equals("en")){e.getPlayer().sendMessage(ChatColor.GREEN + "You are now owner of region " + ccChunkRegionName + ChatColor.WHITE + ccChunkRegionName + ChatColor.GREEN + ".\n" +
                                             "You are now owning " + ChatColor.WHITE + playerRegionCount + "/" + getPlayersGlobalClaimLimit(e.getPlayer()) + ChatColor.GREEN + " Chunks.");}
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
                           if(ChunkClaimer.language.equals("de")){e.getPlayer().sendMessage(ChatColor.GOLD + "Du hast dein Limit von " + ChatColor.WHITE + getPlayersGlobalClaimLimit(e.getPlayer()) + ChatColor.GOLD + " Regionen die du besitzen kannst erreicht!");}
                           if(ChunkClaimer.language.equals("en")){e.getPlayer().sendMessage(ChatColor.GOLD + "You have reached your limit of " + ChatColor.WHITE + getPlayersGlobalClaimLimit(e.getPlayer()) + ChatColor.GOLD + " regions that you can claim!");}
                        }
                     }
                     else
                     {  
                        if(ChunkClaimer.language.equals("de")){e.getPlayer().sendMessage(ChatColor.GOLD + "Dieser Chunk " + ChatColor.WHITE + ccChunkRegionName + ChatColor.GOLD + " ist schon geschuetzt durch " + plugin.getDescription().getName() + "\n" +
                              "Besitzer: " + ChatColor.WHITE + getOwnerNamesOfRegion(wgCurrWorldRM, ccChunkRegionName));}

                        if(ChunkClaimer.language.equals("en")){e.getPlayer().sendMessage(ChatColor.GOLD + "This chunk " + ChatColor.WHITE + ccChunkRegionName + ChatColor.GOLD + " is already protected by " + plugin.getDescription().getName() + "\n" +
                              "Besitzer: " + ChatColor.WHITE + getOwnerNamesOfRegion(wgCurrWorldRM, ccChunkRegionName));}
                     }
                  }
                  else
                  {
                     if(ChunkClaimer.language.equals("de")){e.getPlayer().sendMessage(ChatColor.GOLD + "Du kannst diese Region nicht fuer dich beanspruchen, weil du hier kein Baurecht hast.");}
                     if(ChunkClaimer.language.equals("en")){e.getPlayer().sendMessage(ChatColor.GOLD + "You are not allowed to claim this area, because you have no building rights here.");}
                  }
               }
            }
            // =================================================================================

            // CHUNK PROTECTION REMOVER ========================================================
            if(e.getPlayer().getItemInHand().getType() == Material.STICK)
            {
               if((e.getPlayer().isOp()) ||
                     (e.getPlayer().hasPermission("chunkclaimer.buy")))
               {
                  if(wgCurrWorldRM.hasRegion(ccChunkRegionName))
                  {
                     if(wgCurrWorldRM.getRegion(ccChunkRegionName).isOwner(lPlayer))
                     {
                        wgInst.getRegionManager(e.getPlayer().getWorld()).removeRegion(ccChunkRegionName);

                        try
                        {
                           wgInst.getRegionManager(e.getPlayer().getWorld()).save();
                           int playerRegionCount = getPlayersGlobalCCregionCount(lPlayer);

                           if(ChunkClaimer.language.equals("de")){e.getPlayer().sendMessage(ChatColor.GREEN + "Region " + ccChunkRegionName + " wurde entfernt.\n" +
                                 "Du besitzt jetzt " + ChatColor.WHITE + playerRegionCount + "/" + getPlayersGlobalClaimLimit(e.getPlayer()) + ChatColor.GREEN + " Chunks.");}

                           if(ChunkClaimer.language.equals("en")){e.getPlayer().sendMessage(ChatColor.GREEN + "Region " + ccChunkRegionName + " has been removed.\n" +
                                 "You are now owning " + ChatColor.WHITE + playerRegionCount + "/" + getPlayersGlobalClaimLimit(e.getPlayer()) + ChatColor.GREEN + " Chunks.");}                        
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
                        if(ChunkClaimer.language.equals("de")){e.getPlayer().sendMessage(ChatColor.RED + plugin.getDescription().getName() + "-Regionen koennen nur von ihren Besitzern, oder Spielern mit den noetigen Rechten entfernt werden.");}
                        if(ChunkClaimer.language.equals("en")){e.getPlayer().sendMessage(ChatColor.RED + plugin.getDescription().getName() + "-Regions may only be removed by it's owners, or players with proper permissions.");}
                     }
                  }
                  else
                  {
                     if(ChunkClaimer.language.equals("de")){e.getPlayer().sendMessage(ChatColor.WHITE + "Es ist keine " + plugin.getDescription().getName() + "-Region fuer diesen Chunk vorhanden.");}
                     if(ChunkClaimer.language.equals("en")){e.getPlayer().sendMessage(ChatColor.WHITE + "There is no " + plugin.getDescription().getName() + " protection present for this chunk.");}
                  }
               }
            }
         }
      }
      // =================================================================================
   }

   // ##########################################################################################

   String getOwnerNamesOfRegion(RegionManager rm, String region)
   {
      String ownerNames = "";
      String[] owners = rm.getRegion(region).getOwners().getPlayers().toArray(new String[0]);
      int ownerCount = rm.getRegion(region).getOwners().getPlayers().size();

      for(int i = 0; i < ownerCount; i++)
      {
         if(!((i + 1) == ownerCount))
         {  // not the last found owner
            ownerNames = ownerNames + owners[i] + ", ";
         }
         else
         {
            // last found owner, so omit the comma
            ownerNames = ownerNames + owners[i];
         }
      }

      return (ownerNames);
   }

   String getMemberNamesOfRegion(RegionManager rm, String region)
   {
      String memberNames = "";

      // Get member Groups
      String[] memberGroups = rm.getRegion(region).getMembers().getGroups().toArray(new String[0]);
      int memberGroupCount = rm.getRegion(region).getMembers().getGroups().size();

      for(int i = 0; i < memberGroupCount; i++)
      {         
         if(!((i + 1) == memberGroupCount))
         {  // not the last found memberGroup
            memberNames = memberNames + "*" + memberGroups[i] + ", ";
         }
         else
         {
            // last found memberGroup, so omit the comma
            memberNames = memberNames + "*" + memberGroups[i];
         }
      }

      // get member Players
      String[] memberPlayers = rm.getRegion(region).getMembers().getPlayers().toArray(new String[0]);
      int memberPlayersCount = rm.getRegion(region).getMembers().getPlayers().size();

      for(int i = 0; i < memberPlayersCount; i++)
      {
         if(i == 0)
         {
            if(memberGroupCount > 0)
            {
               // first member and one ore more groups are already present, so add comma in front of it
               memberNames = memberNames + ", " + memberPlayers[i];
            }
            else
            {
               // first member and no groups are present, so don't add comma
               memberNames = memberNames + memberPlayers[i];
            }
         }
         else if((i > 0) && ((i + 1) == memberPlayersCount))
         {  // one or more members present and the last one, so add comma only in front
            memberNames = memberNames + ", " + memberPlayers[i];
         }
         else
         {
            // one or more members present and not the last one, so add comma in front and behind it
            memberNames = memberNames + ", " + memberPlayers[i] + ", ";
         }
      }

      return (memberNames);
   }

   // Counts all CC regions of a player (globally) in all enabled worlds
   private int getPlayersGlobalCCregionCount(LocalPlayer lPlayer)
   {
      int count = 0;

      // counts owned CC regions of player in all enabled worlds
      for(World w : Bukkit.getServer().getWorlds())
      {
         if(plugin.getConfig().getStringList("enabledWorlds").contains(w.getName()))
         {
            Set<String> playerRegionsInWorld = wgGlobalRM.get(w).getRegions().keySet();

            for(String reg : playerRegionsInWorld)
            {
               if(reg.startsWith(ChunkClaimer.ccRegionPrefix))
               {
                  count ++;
               }
            }
         }
      }

      return (count);
   }

   // gets global claimLimit of player (defined for his group in config)
   private int getPlayersGlobalClaimLimit(Player player)
   {
      int limit = 0;

      if(ChunkClaimer.claimingLimits.containsKey(perm.getPrimaryGroup(player)))
      {         
         limit = ChunkClaimer.claimingLimits.get(perm.getPrimaryGroup(player));
      }
      else
      {
         limit = 100000; // this players group is not restricted in config file for claiming chunks
      }

      return (limit);
   }
}

