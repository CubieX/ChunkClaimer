package com.github.CubieX.ChunkClaimer;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Chunk;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import com.sk89q.worldguard.LocalPlayer;
import com.sk89q.worldguard.bukkit.BukkitPlayer;
import com.sk89q.worldguard.bukkit.WorldGuardPlugin;
import com.sk89q.worldguard.protection.managers.RegionManager;

public class CCCommandHandler implements CommandExecutor
{
   private ChunkClaimer plugin = null;
   private CCConfigHandler cHandler = null;
   private WorldGuardPlugin wgInst = null;

   public CCCommandHandler(ChunkClaimer plugin, CCConfigHandler cHandler, WorldGuardPlugin wgInst) 
   {
      this.plugin = plugin;
      this.cHandler = cHandler;
      this.wgInst = wgInst;
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
         }

         if(ChunkClaimer.language.equals("de")){sender.sendMessage(ChatColor.YELLOW + "Falsche Anzahl an Parametern!");}
         if(ChunkClaimer.language.equals("en")){sender.sendMessage(ChatColor.YELLOW + "Wrong parameter count!");}

         return false;
      }

      return false; // if false is returned, the help for the command stated in the plugin.yml will be displayed to the player
   }
}