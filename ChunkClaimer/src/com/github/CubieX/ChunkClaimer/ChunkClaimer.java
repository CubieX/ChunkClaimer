package com.github.CubieX.ChunkClaimer;

import java.util.logging.Logger;
import org.bukkit.Bukkit;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;
import com.sk89q.worldguard.bukkit.WorldGuardPlugin;

public class ChunkClaimer extends JavaPlugin
{
   public static final Logger log = Bukkit.getServer().getLogger();
   static final String logPrefix = "[ChunkClaimer] "; // Prefix to go in front of all log entries

   private ChunkClaimer plugin = null;
   WorldGuardPlugin weInst = null;
   private CCCommandHandler comHandler = null;
   private CCConfigHandler cHandler = null;
   private CCEntityListener eListener = null;
   //private BSchedulerHandler schedHandler = null;

   static boolean debug = false;

   //*************************************************
   static String usedConfigVersion = "1"; // Update this every time the config file version changes, so the plugin knows, if there is a suiting config present
   //*************************************************

   @Override
   public void onEnable()
   {
      this.plugin = this;
      cHandler = new CCConfigHandler(this);

      if(!checkConfigFileVersion())
      {
         log.severe(logPrefix + "Outdated or corrupted config file(s). Please delete your config files."); 
         log.severe(logPrefix + "will generate a new config for you.");
         log.severe(logPrefix + "will be disabled now. Config file is outdated or corrupted.");
         getServer().getPluginManager().disablePlugin(this);
         return;
      }
      
      if(!getWorldGuard())
      {
         log.severe(logPrefix + " will be disabled now. Error on getting WorldEdit instance.");
         getServer().getPluginManager().disablePlugin(this);
      }

      eListener = new CCEntityListener(this, weInst);      
      comHandler = new CCCommandHandler(this, cHandler);      
      getCommand("cclaimer").setExecutor(comHandler);

      //schedHandler = new BSchedulerHandler(this);

      readConfigValues();
      
      log.info(this.getDescription().getName() + " version " + getDescription().getVersion() + " is enabled!");

      //schedHandler.startPlayerInWaterCheckScheduler_SynchRepeating();
   }

   private boolean checkConfigFileVersion()
   {      
      boolean configOK = false;     

      if(cHandler.getConfig().isSet("config_version"))
      {
         String configVersion = cHandler.getConfig().getString("config_version");

         if(configVersion.equals(usedConfigVersion))
         {
            configOK = true;
         }
      }

      return (configOK);
   }

   public void readConfigValues()
   {
      boolean exceed = false;
      boolean invalid = false;

      debug = cHandler.getConfig().getBoolean("debug");

      if(exceed)
      {
         log.warning("One or more config values are exceeding their allowed range. Please check your config file!");
      }

      if(invalid)
      {
         log.warning("One or more config values are invalid. Please check your config file!");
      }
   }

   private boolean getWorldGuard()
   {
      boolean ok = false;
      
      Plugin wePlugin = getServer().getPluginManager().getPlugin("WorldGuard");

      // WorldGuard may not be loaded
      if ((null != wePlugin) && (wePlugin instanceof WorldGuardPlugin))
      {        
         this.weInst = (WorldGuardPlugin) wePlugin;
         ok = true;
      }

      return ok;
   }

   @Override
   public void onDisable()
   {
      this.getServer().getScheduler().cancelAllTasks();
      cHandler = null;
      eListener = null;
      comHandler = null;
      //schedHandler = null; // TODO ACTIVATE THIS AGAIN IF USED!
      log.info(this.getDescription().getName() + " version " + getDescription().getVersion() + " is disabled!");
   }

   // #########################################################


}


