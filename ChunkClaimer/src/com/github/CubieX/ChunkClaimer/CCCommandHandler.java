package com.github.CubieX.ChunkClaimer;

import java.util.ArrayList;
import java.util.List;

import net.milkbowl.vault.economy.Economy;
import net.milkbowl.vault.economy.EconomyResponse;
import net.milkbowl.vault.permission.Permission;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Chunk;
import org.bukkit.OfflinePlayer;
import org.bukkit.block.Block;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import com.sk89q.worldedit.BlockVector;
import com.sk89q.worldguard.LocalPlayer;
import com.sk89q.worldguard.bukkit.BukkitPlayer;
import com.sk89q.worldguard.bukkit.WorldGuardPlugin;
import com.sk89q.worldguard.domains.DefaultDomain;
import com.sk89q.worldguard.protection.ApplicableRegionSet;
import com.sk89q.worldguard.protection.GlobalRegionManager;
import com.sk89q.worldguard.protection.managers.RegionManager;
import com.sk89q.worldguard.protection.regions.ProtectedCuboidRegion;
import com.sk89q.worldguard.protection.regions.ProtectedRegion;

public class CCCommandHandler implements CommandExecutor
{
   private ChunkClaimer plugin = null;
   private CCConfigHandler cHandler = null;
   private WorldGuardPlugin wgInst = null;
   Permission perm = null;
   Economy econ = null;
   List<Block> borderBlocks = new ArrayList<Block>();
   String buildState = "No";
   GlobalRegionManager wgGlobalRM; // RegionManager that can access any given world

   public CCCommandHandler(ChunkClaimer plugin, CCConfigHandler cHandler, WorldGuardPlugin wgInst, Permission perm, Economy econ) 
   {
      this.plugin = plugin;
      this.cHandler = cHandler;
      this.wgInst = wgInst;
      this.perm = perm;
      this.econ = econ;
      wgGlobalRM = wgInst.getGlobalRegionManager();
   }

   @Override
   public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args)
   {
      Player player = null;

      if (sender instanceof Player) 
      {
         player = (Player) sender;
      }

      if (cmd.getName().equalsIgnoreCase("cclaimer"))
      {
         if (args.length == 0)
         { //no arguments, so help will be displayed
            return false;
         }

         // VERSION ======================================================================
         if (args.length == 1)
         {
            if (args[0].equalsIgnoreCase("version"))
            {
               if(ChunkClaimer.language.equals("de")){sender.sendMessage(ChatColor.GREEN + "Auf diesem Server laeuft " + plugin.getDescription().getName() + " Version " + plugin.getDescription().getVersion());}
               if(ChunkClaimer.language.equals("en")){sender.sendMessage(ChatColor.GREEN + "This server is running " + plugin.getDescription().getName() + " version " + plugin.getDescription().getVersion());}

               return true;
            }

            // RELOAD ======================================================================
            if (args[0].equalsIgnoreCase("reload"))
            {            
               if(sender.hasPermission("chunkclaimer.admin"))
               {                        
                  cHandler.reloadConfig(sender);                  
               }
               else
               {
                  if(ChunkClaimer.language.equals("de")){sender.sendMessage(ChatColor.RED + "Du hast keine Rechte zum Neu-laden von " + plugin.getDescription().getName() + "!");}
                  if(ChunkClaimer.language.equals("en")){sender.sendMessage(ChatColor.RED + "You do not have sufficient permission to reload " + plugin.getDescription().getName() + "!");}
               }

               return true;
            }

            // HELP ======================================================================
            if (args[0].equalsIgnoreCase("help"))
            {
               if(null != player)
               {
                  if(ChunkClaimer.language.equals("de")){sender.sendMessage(ChunkClaimer.logPrefix + ChatColor.GREEN + "Rechtsklicke mit einem Knochen auf den Boden um den Umriss des Chunks zu sehen.\n" +                       
                        "Rechtsklicke mit einer Lohenrute auf den Boden um diesen Chunk fuer dich zu beanspruchen." +
                        "Rechtsklicke mit einem Stab auf den Boden um diesen Chunk wieder freizugeben.");}

                  if(ChunkClaimer.language.equals("en")){sender.sendMessage(ChunkClaimer.logPrefix + ChatColor.GREEN + "Hit the ground with a bone to display this chunks outlines and info.\n" +                       
                        "Hit the ground with a Blaze Rod to claim this chunk for yourself." +
                        "Hit the ground with a Stick to release this chunk.");}
               }
               else
               {
                  if(ChunkClaimer.language.equals("de")){sender.sendMessage(ChunkClaimer.logPrefix + "Rechtsklicke mit einem Knochen auf den Boden um den Umriss des Chunks zu sehen.\n" +                       
                        "Rechtsklicke mit einer Lohenrute auf den Boden um diesen Chunk fuer dich zu beanspruchen." +
                        "Rechtsklicke mit einem Stab auf den Boden um diesen Chunk wieder freizugeben.");}

                  if(ChunkClaimer.language.equals("en")){sender.sendMessage(ChunkClaimer.logPrefix + "Hit the ground with a bone to display this chunks outlines and info.\n" +                       
                        "Hit the ground with a Blaze Rod to claim this chunk for yourself." +
                        "Hit the ground with a Stick to release this chunk.");}
               }

               return true;
            }

            // CLAIM OR BUY CHUNK ======================================================================            
            if ((args[0].equalsIgnoreCase("kaufen")) || ((args[0].equalsIgnoreCase("buy"))))
            {
               if(null != player) // command only usable by a player ingame
               {
                  if((player.isOp()) ||
                        (player.hasPermission("chunkclaimer.buy")))
                  {
                     RegionManager wgCurrWorldRM = wgInst.getRegionManager(player.getWorld()); // regionManager for current world
                     Chunk chunk = player.getLocation().getChunk();
                     String ccChunkRegionName = ChunkClaimer.ccRegionPrefix + "_" + chunk.getX() + "_" + chunk.getZ(); // if there is or will be an applicable region, it is called like this
                     LocalPlayer lPlayer = new BukkitPlayer(wgInst, player);

                     if(wgInst.canBuild(player, player.getLocation().getBlock()))
                     {
                        if(!wgCurrWorldRM.hasRegion(ccChunkRegionName)) // Try only to create a new region, if a ChunkClaimer protection is not already existing at this point
                        {
                           // Claim new region ======
                           int playerRegionCount = plugin.getPlayersGlobalCCregionCount(wgGlobalRM, lPlayer);

                           if(ChunkClaimer.debug){player.sendMessage(ChatColor.WHITE + "Du besitzt global " + ChatColor.GREEN + playerRegionCount + " / " + plugin.getPlayersGlobalClaimLimit(player) + ChatColor.WHITE + " Chunks.");}

                           if(plugin.getPlayersGlobalClaimLimit(player) > playerRegionCount)
                           {
                              DefaultDomain owners = new DefaultDomain();
                              DefaultDomain members = new DefaultDomain();
                              owners.addPlayer(lPlayer);

                              // this is the group of the player (needed later)
                              DefaultDomain playerGroup = new DefaultDomain();
                              playerGroup.addGroup(perm.getPrimaryGroup(player));

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

                              if(player.isOp() ||
                                    player.hasPermission("chunkclaimer.admin") ||
                                    arSet.canBuild(lPlayer))
                              {
                                 if(player.isOp() ||
                                       player.hasPermission("chunkclaimer.admin") ||
                                       plugin.clearanceZoneIsMaintained(wgCurrWorldRM, chunk, player.getName()))
                                 {
                                    int price = plugin.getPriceOfNewChunkProtection(lPlayer);

                                    if(econ.has(player.getName(), price)) // has player enough money?
                                    {
                                       EconomyResponse ecoRes = econ.withdrawPlayer(player.getName(), price);
                                       if(ecoRes.transactionSuccess()) // claimed region successfully payed
                                       {
                                          // Surrounding areas are free to create that region and player has been charged successfully, so create protection for that player
                                          wgCurrWorldRM.addRegion(reg);

                                          if(ChunkClaimer.language.equals("de")){player.sendMessage(ChatColor.GREEN + "Dir wurden " + ChatColor.WHITE + price + " " + ChunkClaimer.currency + ChatColor.GREEN + " abgezogen.");}
                                          if(ChunkClaimer.language.equals("en")){player.sendMessage(ChatColor.GREEN + "You have been charged with " + ChatColor.WHITE + price + " " + ChunkClaimer.currency + ChatColor.GREEN + ".");}

                                          if(WEWGutil.saveWGregionManager(wgCurrWorldRM)) // Try to save all region changes
                                          {
                                             // don't place outline blocks in the Nether, because they will end up on the roof
                                             /*if(player.getWorld().getEnvironment() != Environment.NETHER)
                                          {
                                             ChunkFinderUtil.placeOutlineForClaimedChunk(chunk, borderBlocks);
                                          }*/

                                             playerRegionCount = plugin.getPlayersGlobalCCregionCount(wgGlobalRM, lPlayer);

                                             if(ChunkClaimer.language.equals("de")){player.sendMessage(ChatColor.GREEN + "Du bist jetzt Besitzer der Region " + ChatColor.WHITE + ccChunkRegionName + ChatColor.GREEN + ".\n" +
                                                   "Du besitzt nun " + ChatColor.WHITE + playerRegionCount + "/" + plugin.getPlayersGlobalClaimLimit(player) + ChatColor.GREEN + " Chunks.");}

                                             if(ChunkClaimer.language.equals("en")){player.sendMessage(ChatColor.GREEN + "You are now owner of region " + ccChunkRegionName + ChatColor.WHITE + ccChunkRegionName + ChatColor.GREEN + ".\n" +
                                                   "You are now owning " + ChatColor.WHITE + playerRegionCount + "/" + plugin.getPlayersGlobalClaimLimit(player) + ChatColor.GREEN + " Chunks.");}
                                          }
                                          else
                                          {
                                             if(ChunkClaimer.language.equals("de")){player.sendMessage(ChunkClaimer.logPrefix + ChatColor.RED + "FEHLER beim Speichern dieser Region!");}
                                             if(ChunkClaimer.language.equals("en")){player.sendMessage(ChunkClaimer.logPrefix + ChatColor.RED + "ERROR while saving this region!");}

                                             ChunkClaimer.log.severe(ChunkClaimer.logPrefix + "ERROR while saving this region!");
                                          }
                                       }
                                       else
                                       {
                                          // Eco transfer failed. Abort.
                                          if(ChunkClaimer.language.equals("de")){player.sendMessage(ChatColor.RED + "Fehler beim Bezahlen der Region " + ccChunkRegionName + ". Bitte informiere einen Admin!");}
                                          if(ChunkClaimer.language.equals("en")){player.sendMessage(ChatColor.RED + "Error on paying for the region " + ccChunkRegionName + ". Please inform an Admin!");}

                                          ChunkClaimer.log.severe("Error on charging " + player.getName() + " for region " + ccChunkRegionName);
                                       }
                                    }
                                    else
                                    {
                                       if(ChunkClaimer.language.equals("de")){player.sendMessage(ChatColor.GOLD + "Du hast nicht genuegend Geld (" + ChatColor.WHITE + price + ChatColor.GOLD + ") um diesen Chunk zu beanspruchen!");}
                                       if(ChunkClaimer.language.equals("en")){player.sendMessage(ChatColor.GOLD + "You do not have enough money (" + ChatColor.WHITE + price + ChatColor.GOLD + ") to claim this chunk!");}
                                    }
                                 }
                                 else
                                 {
                                    if(ChunkClaimer.language.equals("de")){player.sendMessage(ChatColor.GOLD + "Beanspruchung nicht moeglich. Diese Region ist zu nahe an benachbarten Regionen\n" +
                                          " auf denen du weder ein 'Freund' noch ein 'Besitzer' bist.");}
                                    if(ChunkClaimer.language.equals("en")){player.sendMessage(ChatColor.GOLD + "Claiming not possible. This region is too close to neighboring regions\n" +
                                          " on where you are neither a 'Friend' nor an 'Owner'.");}
                                 }
                              }
                              else
                              {
                                 if(ChunkClaimer.language.equals("de")){player.sendMessage(ChatColor.GOLD + "Beanspruchung nicht moeglich. Die Region ueberschneidet sich mit anderen Regionen die dir nicht gehoeren.");}
                                 if(ChunkClaimer.language.equals("en")){player.sendMessage(ChatColor.GOLD + "Claiming not possible. This region is intersecting other regions you do not own.");}
                              }
                           }
                           else
                           {
                              if(ChunkClaimer.language.equals("de")){player.sendMessage(ChatColor.GOLD + "Du hast dein Limit von " + ChatColor.WHITE + plugin.getPlayersGlobalClaimLimit(player) + ChatColor.GOLD + " Regionen die du besitzen kannst erreicht!");}
                              if(ChunkClaimer.language.equals("en")){player.sendMessage(ChatColor.GOLD + "You have reached your limit of " + ChatColor.WHITE + plugin.getPlayersGlobalClaimLimit(player) + ChatColor.GOLD + " regions that you can claim!");}
                           }
                        }
                        else
                        {
                           // there is a CC region present. So check if it's on sale to buy it =======
                           if(plugin.chunkIsOnSale(player.getWorld().getName(), ccChunkRegionName))
                           {
                              // check if this region is not owned by the player
                              if(!player.getName().equals(plugin.getSellingPlayerOfChunkOnSale(player.getWorld().getName(), ccChunkRegionName)))
                              {
                                 int playerRegionCount = plugin.getPlayersGlobalCCregionCount(wgGlobalRM, lPlayer);
                                 int price = ChunkClaimer.basePricePerClaimedRegion;

                                 if(plugin.getPlayersGlobalClaimLimit(player) > playerRegionCount)
                                 {
                                    if(ChunkClaimer.priceIncreasePerClaimedChunk > 0)
                                    {
                                       price = plugin.getPriceOfChunkOnSale(player.getWorld().getName(), ccChunkRegionName);
                                    }

                                    if(econ.has(player.getName(), price))
                                    {
                                       EconomyResponse ecoRes = econ.withdrawPlayer(player.getName(), price);
                                       if(ecoRes.transactionSuccess()) // claimed region successfully payed
                                       {
                                          ProtectedRegion chunkToBuy = wgCurrWorldRM.getRegion(ccChunkRegionName);                                 
                                          DefaultDomain newOwner = new DefaultDomain();
                                          newOwner.addPlayer(player.getName());                              
                                          chunkToBuy.getOwners().getPlayers().clear();
                                          chunkToBuy.setOwners(newOwner);

                                          if(ChunkClaimer.language.equals("de")){player.sendMessage(ChatColor.GREEN + "Dir wurden " + ChatColor.WHITE + price + " " + ChunkClaimer.currency + ChatColor.GREEN + " abgezogen.");}
                                          if(ChunkClaimer.language.equals("en")){player.sendMessage(ChatColor.GREEN + "You have been charged with " + ChatColor.WHITE + price + " " + ChunkClaimer.currency + ChatColor.GREEN + ".");}

                                          if(WEWGutil.saveWGregionManager(wgCurrWorldRM)) // Try to save all region changes
                                          {
                                             // now transfer the money to the seller
                                             OfflinePlayer sellerOff = Bukkit.getOfflinePlayer(plugin.getSellingPlayerOfChunkOnSale(player.getWorld().getName(), ccChunkRegionName));

                                             if(sellerOff.hasPlayedBefore()) // player data still present?
                                             {
                                                if(econ.hasAccount(sellerOff.getName()))
                                                {
                                                   EconomyResponse ecoResPaySeller = econ.depositPlayer(sellerOff.getName(), price);
                                                   if(ecoResPaySeller.transactionSuccess()) // claimed region successfully payed
                                                   {
                                                      if(sellerOff.isOnline())
                                                      {
                                                         Player seller = (Player) sellerOff;
                                                         LocalPlayer lSeller = new BukkitPlayer(wgInst, seller);

                                                         playerRegionCount = plugin.getPlayersGlobalCCregionCount(wgGlobalRM, lSeller);

                                                         if(ChunkClaimer.language.equals("de")){seller.sendMessage(ChatColor.WHITE + player.getName() + ChatColor.GREEN + " hat deine Region " + ChatColor.WHITE + ccChunkRegionName + ChatColor.GREEN + " gekauft.\n" +
                                                               "Du hast " + ChatColor.WHITE + price + " " + ChunkClaimer.currency + ChatColor.GREEN + " erhalten und besitzt nun " + ChatColor.WHITE + playerRegionCount + "/" + plugin.getPlayersGlobalClaimLimit(seller) + ChatColor.GREEN + " Chunks.");}

                                                         if(ChunkClaimer.language.equals("en")){seller.sendMessage(ChatColor.WHITE + player.getName() + ChatColor.GREEN + " has bought your region " + ChatColor.WHITE + ccChunkRegionName + ChatColor.GREEN + "\n" +
                                                               "You have received " + ChatColor.WHITE + price + " " + ChunkClaimer.currency + ChatColor.GREEN + " and are now owning " + ChatColor.WHITE + playerRegionCount + "/" + plugin.getPlayersGlobalClaimLimit(seller) + ChatColor.GREEN + " chunks.");}
                                                      }
                                                   }
                                                }
                                             }

                                             plugin.removeChunkFromFromSellList(player.getWorld().getName(), ccChunkRegionName);                                             
                                             playerRegionCount = plugin.getPlayersGlobalCCregionCount(wgGlobalRM, lPlayer);

                                             if(ChunkClaimer.language.equals("de")){player.sendMessage(ChatColor.GREEN + "Du bist jetzt neuer Besitzer der Region " + ChatColor.WHITE + ccChunkRegionName + ChatColor.GREEN + ".\n" +
                                                   "Du besitzt nun " + ChatColor.WHITE + playerRegionCount + "/" + plugin.getPlayersGlobalClaimLimit(player) + ChatColor.GREEN + " Chunks.");}

                                             if(ChunkClaimer.language.equals("en")){player.sendMessage(ChatColor.GREEN + "You are now new owner of region " + ccChunkRegionName + ChatColor.WHITE + ccChunkRegionName + ChatColor.GREEN + ".\n" +
                                                   "You are now owning " + ChatColor.WHITE + playerRegionCount + "/" + plugin.getPlayersGlobalClaimLimit(player) + ChatColor.GREEN + " Chunks.");}                                             
                                          }
                                          else
                                          {
                                             if(ChunkClaimer.language.equals("de")){player.sendMessage(ChunkClaimer.logPrefix + ChatColor.RED + "FEHLER beim Speichern dieser Region!");}
                                             if(ChunkClaimer.language.equals("en")){player.sendMessage(ChunkClaimer.logPrefix + ChatColor.RED + "ERROR while saving this region!");}

                                             ChunkClaimer.log.severe(ChunkClaimer.logPrefix + "ERROR while saving this region!");
                                          }
                                       }
                                       else
                                       {
                                          // Eco transfer failed. Abort.
                                          if(ChunkClaimer.language.equals("de")){player.sendMessage(ChatColor.RED + "Fehler beim Bezahlen der Region " + ccChunkRegionName + ". Bitte informiere einen Admin!");}
                                          if(ChunkClaimer.language.equals("en")){player.sendMessage(ChatColor.RED + "Error on paying for the region " + ccChunkRegionName + ". Please inform an Admin!");}

                                          ChunkClaimer.log.severe("Error on charging " + player.getName() + " for region " + ccChunkRegionName);
                                       }
                                    }
                                    else
                                    {
                                       if(ChunkClaimer.language.equals("de")){player.sendMessage(ChatColor.GOLD + "Du hast nicht genuegend Geld (" + ChatColor.WHITE + price + ChatColor.GOLD + ") um diesen Chunk zu kaufen!");}
                                       if(ChunkClaimer.language.equals("en")){player.sendMessage(ChatColor.GOLD + "You do not have enough money (" + ChatColor.WHITE + price + ChatColor.GOLD + ") to buy this chunk!");}
                                    }                                    
                                 }
                                 else
                                 {
                                    if(ChunkClaimer.language.equals("de")){player.sendMessage(ChatColor.GOLD + "Du hast dein Limit von " + ChatColor.WHITE + plugin.getPlayersGlobalClaimLimit(player) + ChatColor.GOLD + " Regionen die du besitzen kannst erreicht!");}
                                    if(ChunkClaimer.language.equals("en")){player.sendMessage(ChatColor.GOLD + "You have reached your limit of " + ChatColor.WHITE + plugin.getPlayersGlobalClaimLimit(player) + ChatColor.GOLD + " regions that you can claim!");}
                                 }
                              }
                              else
                              {
                                 if(ChunkClaimer.language.equals("de")){player.sendMessage(ChatColor.GOLD + "Dieser Chunk gehoert dir bereits!");}
                                 if(ChunkClaimer.language.equals("en")){player.sendMessage(ChatColor.GOLD + "This chunk is already owned by you!");}
                              }
                           }
                           else
                           {
                              if(ChunkClaimer.language.equals("de")){player.sendMessage(ChatColor.GOLD + "Dieser Chunk steht nicht zum Verkauf!");}
                              if(ChunkClaimer.language.equals("en")){player.sendMessage(ChatColor.GOLD + "This chunk is not for sale!");}
                           }
                        }
                     }
                     else
                     {
                        if(ChunkClaimer.language.equals("de")){player.sendMessage(ChatColor.GOLD + "Du kannst diese Region nicht fuer dich beanspruchen, weil du hier kein Baurecht hast.");}
                        if(ChunkClaimer.language.equals("en")){player.sendMessage(ChatColor.GOLD + "You are not allowed to claim this area, because you have no building rights here.");}
                     }
                  }
               }

               return true;
            }

            // RELOAD ======================================================================
            if (args[0].equalsIgnoreCase("reload"))
            {            
               if(sender.hasPermission("chunkclaimer.admin"))
               {                        
                  cHandler.reloadConfig(sender);                  
               }
               else
               {
                  if(ChunkClaimer.language.equals("de")){sender.sendMessage(ChatColor.RED + "Du hast keine Rechte zum Neu-laden von " + plugin.getDescription().getName() + "!");}
                  if(ChunkClaimer.language.equals("en")){sender.sendMessage(ChatColor.RED + "You do not have sufficient permission to reload " + plugin.getDescription().getName() + "!");}
               }

               return true;
            }
         }

         if(args.length == 2)
         {
            if(null != player)
            {
               RegionManager wgRM = wgInst.getRegionManager(player.getWorld());

               // SELL CHUNK ==================================================================================
               if ((args[0].equalsIgnoreCase("verkaufen")) || (args[0].equalsIgnoreCase("sell"))) // args[1] = price
               {
                  if(player.isOp() || (player.hasPermission("chunkclaimer.sell")))
                  {
                     Chunk chunk = player.getLocation().getChunk();
                     String ccChunkRegionName = ChunkClaimer.ccRegionPrefix + "_" + chunk.getX() + "_" + chunk.getZ();

                     if(wgRM.hasRegion(ccChunkRegionName))
                     {
                        // is this chunk owned by the player?
                        if(wgRM.getRegion(ccChunkRegionName).getOwners().getPlayers().contains(player.getName().toLowerCase()))
                        {
                           if((CCUtil.isInteger(args[1])))
                           {
                              int price = Integer.parseInt(args[1]); // double to prevent errors on values > Integer.MAX_VALUE

                              if((price >= 0) && (price <= ChunkClaimer.maxSellingPrice))
                              {
                                 if(plugin.chunkIsOnSale(player.getWorld().getName(), ccChunkRegionName))
                                 {
                                    // update selling information in list (new price)

                                    plugin.updateChunkOnSellList(player.getWorld().getName(), ccChunkRegionName, price);

                                    if(ChunkClaimer.language.equals("de")){player.sendMessage(ChatColor.GREEN + "Preis der Region " + ccChunkRegionName + " aktualisiert auf " + ChatColor.WHITE + price + " " + ChatColor.GREEN + ChunkClaimer.currency);}
                                    if(ChunkClaimer.language.equals("en")){player.sendMessage(ChatColor.GREEN + "Price of region " + ccChunkRegionName + " has updated to " + ChatColor.WHITE + price + " " + ChatColor.GREEN + ChunkClaimer.currency);}
                                 }
                                 else
                                 {
                                    // add selling information to list

                                    plugin.addChunkToSellList(player.getWorld().getName(), ccChunkRegionName, player.getName() ,price);

                                    // TODO Wie den Chunk ingame markieren als "zum verkauf" ???

                                    if(ChunkClaimer.language.equals("de")){player.sendMessage(ChatColor.GREEN + "Du hast diesen Chunk fuer " + ChatColor.WHITE + price + " " + ChunkClaimer.currency + ChatColor.GREEN + " zum Verkauf gesetzt.");}
                                    if(ChunkClaimer.language.equals("en")){player.sendMessage(ChatColor.GREEN + "You have offered this chunk for selling for " + ChatColor.WHITE + price + " " + ChunkClaimer.currency);}
                                 }
                              }
                              else
                              {
                                 if(ChunkClaimer.language.equals("de")){player.sendMessage(ChatColor.RED + "Der angegebene Preis ist zu hoch! (Max: " + ChunkClaimer.maxSellingPrice + " " + ChunkClaimer.currency + ")");}
                                 if(ChunkClaimer.language.equals("en")){player.sendMessage(ChatColor.RED + "The given price is too high! (Max: " + ChunkClaimer.maxSellingPrice + " " + ChunkClaimer.currency + ")");}
                              }
                           }
                           else
                           {
                              if(ChunkClaimer.language.equals("de")){player.sendMessage(ChatColor.RED + "Zweites Argument (Preis) muss eine positive Zahl <= " + ChunkClaimer.maxSellingPrice + " sein!");}
                              if(ChunkClaimer.language.equals("en")){player.sendMessage(ChatColor.RED + "The second argument (Price) must be a positive number <= " + ChunkClaimer.maxSellingPrice + " !");}
                           }
                        }
                        else
                        {
                           if(ChunkClaimer.language.equals("de")){player.sendMessage(ChatColor.RED + "Dieser Chunk gehoert nicht dir!");}
                           if(ChunkClaimer.language.equals("en")){player.sendMessage(ChatColor.RED + "You do not own this chunk!");}
                        }
                     }
                     else
                     {
                        if(ChunkClaimer.language.equals("de")){player.sendMessage(ChatColor.RED + "Keine ChunkClaimer Region gefunden!");}
                        if(ChunkClaimer.language.equals("en")){player.sendMessage(ChatColor.RED + "No ChunkClaimer region found!");}
                     }
                  }
                  else
                  {
                     if(ChunkClaimer.language.equals("de")){sender.sendMessage(ChatColor.RED + "Du hast keine Rechte zum Verkaufen con ChunkClaimer Regionen!");}
                     if(ChunkClaimer.language.equals("en")){sender.sendMessage(ChatColor.RED + "You do not have sufficient permission to sell ChuncClaimer regions!");}
                  }
               }
            }
            else
            {
               if(ChunkClaimer.language.equals("de")){sender.sendMessage(ChunkClaimer.logPrefix + "Dieser Befehl is nur im Spiel verfuegbar!");}
               if(ChunkClaimer.language.equals("en")){sender.sendMessage(ChunkClaimer.logPrefix + "This command is only available in-game!");}
            }

            return true;
         }

         if (args.length == 3) // TODO modify, so multiple friends can be added at once
         {
            if(null != player)
            {
               RegionManager wgRM = wgInst.getRegionManager(player.getWorld());
               LocalPlayer sendingPlayer = new BukkitPlayer(wgInst, player);

               // ADD_FRIEND ==================================================================================
               if (args[0].equalsIgnoreCase("addfriend")) // args[1] = regionName, args[2] = Friends name
               {
                  if(wgRM.hasRegion(args[1]))
                  {
                     if(Bukkit.getServer().getOfflinePlayer(args[2]).hasPlayedBefore())
                     {
                        if((wgRM.getRegion(args[1]).isOwner(sendingPlayer)) || (player.hasPermission("chunkclaimer.manage")))
                        {
                           wgRM.getRegion(args[1]).getMembers().addPlayer(args[2]);

                           if(WEWGutil.saveWGregionManager(wgRM)) // Try to save all region changes
                           {
                              if(ChunkClaimer.language.equals("de")){player.sendMessage(ChatColor.GREEN + "Spieler " + ChatColor.WHITE + args[2] + ChatColor.GREEN + " wurde als Freund hinzugefuegt zur Region " + ChatColor.WHITE + args[1]);}
                              if(ChunkClaimer.language.equals("en")){player.sendMessage(ChatColor.GREEN + "Player " + ChatColor.WHITE + args[2] + ChatColor.GREEN + " has been added as friend to region " + ChatColor.WHITE + args[1]);}
                           }
                           else
                           {
                              if(ChunkClaimer.language.equals("de")){player.sendMessage(ChunkClaimer.logPrefix + ChatColor.RED + "FEHLER beim Speichern dieser Region!");}
                              if(ChunkClaimer.language.equals("en")){player.sendMessage(ChunkClaimer.logPrefix + ChatColor.RED + "ERROR while saving this region!");}

                              ChunkClaimer.log.severe(ChunkClaimer.logPrefix + "ERROR while saving this region!");
                           }
                        }
                        else
                        {
                           if(ChunkClaimer.language.equals("de")){player.sendMessage(ChatColor.RED + "Du kannst keine Freunde zu Regionen hinzufuegen, die dir nicht gehoeren!");}
                           if(ChunkClaimer.language.equals("en")){player.sendMessage(ChatColor.RED + "You can not add friends to regions you do not own!");}
                        }
                     }
                     else
                     {
                        if(ChunkClaimer.language.equals("de")){player.sendMessage(ChatColor.YELLOW + "Spieler " + args[2] + " war nie auf diesem Server!");}
                        if(ChunkClaimer.language.equals("en")){player.sendMessage(ChatColor.YELLOW + "Player " + args[2] + " has never played on this server!");}
                     }
                  }
                  else
                  {
                     if(ChunkClaimer.language.equals("de")){player.sendMessage(ChatColor.YELLOW + "Region " + args[1] + " existiert nicht!");}
                     if(ChunkClaimer.language.equals("en")){player.sendMessage(ChatColor.YELLOW + "Region " + args[1] + " does not exist!");}
                  }

                  return true;
               }

               // REMOVE_FRIEND ====================================================================================
               if (args[0].equalsIgnoreCase("removefriend")) // args[1] = regionName, args[2] = ex-friends name
               {
                  if(wgRM.hasRegion(args[1]))
                  {
                     if((wgRM.getRegion(args[1]).isOwner(sendingPlayer)) || (player.hasPermission("chunkclaimer.manage")))
                     {
                        wgRM.getRegion(args[1]).getMembers().removePlayer(args[2]);

                        if(WEWGutil.saveWGregionManager(wgRM)) // Try to save all region changes
                        {                           
                           if(ChunkClaimer.language.equals("de")){player.sendMessage(ChatColor.GREEN + "Spieler " + ChatColor.WHITE + args[2] + ChatColor.GREEN + " wurde als Freund entfernt von der Region " + ChatColor.WHITE + args[1]);}
                           if(ChunkClaimer.language.equals("en")){player.sendMessage(ChatColor.GREEN + "Player " + ChatColor.WHITE + args[2] + ChatColor.GREEN + " has been removed as friend from region " + ChatColor.WHITE + args[1]);}
                        }
                        else
                        {
                           if(ChunkClaimer.language.equals("de")){player.sendMessage(ChunkClaimer.logPrefix + ChatColor.RED + "FEHLER beim Speichern dieser Region!");}
                           if(ChunkClaimer.language.equals("en")){player.sendMessage(ChunkClaimer.logPrefix + ChatColor.RED + "ERROR while saving this region!");}

                           ChunkClaimer.log.severe(ChunkClaimer.logPrefix + "ERROR while saving this region!");
                        }
                     }
                     else
                     {
                        if(ChunkClaimer.language.equals("de")){player.sendMessage(ChatColor.RED + "Du kannst keine Freunde von Regionen entfernen, die dir nicht gehoeren!!");}
                        if(ChunkClaimer.language.equals("en")){player.sendMessage(ChatColor.RED + "You can not remove friends from regions you do not own!");}
                     }
                  }
                  else
                  {
                     if(ChunkClaimer.language.equals("de")){player.sendMessage(ChatColor.YELLOW + "Region " + args[1] + " existiert nicht!");}
                     if(ChunkClaimer.language.equals("en")){player.sendMessage(ChatColor.YELLOW + "Region " + args[1] + " does not exist!");}
                  }

                  return true;
               }
            }
            else
            {
               if(ChunkClaimer.language.equals("de")){sender.sendMessage(ChunkClaimer.logPrefix + "Dieser Befehl is nur im Spiel verfuegbar!");}
               if(ChunkClaimer.language.equals("en")){sender.sendMessage(ChunkClaimer.logPrefix + "This command is only available in-game!");}
            }

            return true;
         }

         if(ChunkClaimer.language.equals("de")){sender.sendMessage(ChatColor.YELLOW + "Falsche Anzahl an Parametern!");}
         if(ChunkClaimer.language.equals("en")){sender.sendMessage(ChatColor.YELLOW + "Wrong parameter count!");}

         return false;
      }

      return false; // if false is returned, the help for the command stated in the plugin.yml will be displayed to the player
   }
}