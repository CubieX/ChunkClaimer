package com.github.CubieX.ChunkClaimer;

import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class CCCommandHandler implements CommandExecutor
{
   private ChunkClaimer plugin = null;
   private CCConfigHandler cHandler = null;

   public CCCommandHandler(ChunkClaimer plugin, CCConfigHandler cHandler) 
   {
      this.plugin = plugin;
      this.cHandler = cHandler;
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
         
         if (args.length==1)
         {
            if (args[0].equalsIgnoreCase("version")) // argument 0 is given and correct
            {            
               sender.sendMessage(ChatColor.GREEN + "This server is running " + plugin.getDescription().getName() + " version " + plugin.getDescription().getVersion());

               return true;
            }
            
            if (args[0].equalsIgnoreCase("reload")) // argument 0 is given and correct
            {            
               if(sender.hasPermission("chunkclaimer.admin"))
               {                        
                  cHandler.reloadConfig(sender);
                  return true;
               }
               else
               {
                  sender.sendMessage(ChatColor.RED + "You do not have sufficient permission to reload " + plugin.getDescription().getName() + "!");
               }
            }
           
            if (args[0].equalsIgnoreCase("help")) // argument 0 is given and correct
            {
               if(null != player)
               {
                  player.sendMessage(ChatColor.GREEN + ChunkClaimer.logPrefix + "Right click the ground with a bone to display the outlines of the chunk.");
               }
               else
               {
                  sender.sendMessage(ChunkClaimer.logPrefix + "Hit the ground with a bone to display the outlines of the chunk.\n" +
                  		"Hit the ground with a Blaze Rod to protect this chunk." +
                  		"Hit the ground with a Stick to delete the protection of this chunk.");
               }

               return true;
            }
         }
         else
         {
            sender.sendMessage(ChatColor.YELLOW + "Wrong parameter count!");
         }                

      }         
      return false; // if false is returned, the help for the command stated in the plugin.yml will be displayed to the player
   }
}
