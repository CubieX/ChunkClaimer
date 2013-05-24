package com.github.CubieX.ChunkClaimer;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.logging.Logger;

import net.milkbowl.vault.economy.Economy;
import net.milkbowl.vault.permission.Permission;

import org.bukkit.Bukkit;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.java.JavaPlugin;
import com.sk89q.worldguard.bukkit.WorldGuardPlugin;

public class ChunkClaimer extends JavaPlugin
{
   static final Logger log = Bukkit.getServer().getLogger();
   static final String logPrefix = "[ChunkClaimer] "; // Prefix to go in front of all log entries
   static ArrayList<String> availableLanguages = new ArrayList<String>();
   static Permission perm = null;
   static Economy econ = null;
   static final String ccRegionPrefix = "chunk"; // WARNING: Change this only for very good reasons! Will break compatibility with earlier created CC regions!!!

   private ChunkClaimer plugin = null;
   WorldGuardPlugin wgInst = null;
   private CCCommandHandler comHandler = null;
   private CCConfigHandler cHandler = null;
   private CCEntityListener eListener = null;
   //private BSchedulerHandler schedHandler = null;

   // from config file
   static boolean debug = false;
   static String language = "en";
   static List<String> autoAddGroupsAsMembers = null;   
   static HashMap<String, Integer> claimingLimits = new HashMap<String, Integer>();
   static String currency = "$";
   static double basePricePerClaimedRegion = 0;
   static int priceIncreasePerClaimedChunk = 0;

   //*************************************************
   static String usedConfigVersion = "1"; // Update this every time the config file version changes, so the plugin knows, if there is a suiting config present
   //*************************************************

   @Override
   public void onEnable()
   {
      this.plugin = this;
      initLanguageList();
      cHandler = new CCConfigHandler(this);

      if(!checkConfigFileVersion())
      {
         log.severe(logPrefix + "Outdated or corrupted config file(s). Please delete your config files."); 
         log.severe(logPrefix + "will generate a new config for you.");
         log.severe(logPrefix + "will be disabled now. Config file is outdated or corrupted.");
         getServer().getPluginManager().disablePlugin(this);
         return;
      }
      
      if (!setupPermissions())
      {
         log.info(logPrefix + "- Disabled because could not hook Vault to permission system!");
         getServer().getPluginManager().disablePlugin(this);
         return;
      }

      if (!setupEconomy())
      {
         log.info(logPrefix + "- Disabled because could not hook Vault to economy system!");
         getServer().getPluginManager().disablePlugin(this);
         return;
      }

      if(!getWorldGuard())
      {
         log.severe(logPrefix + " will be disabled now. Error on getting WorldEdit instance.");
         getServer().getPluginManager().disablePlugin(this);
      }

      eListener = new CCEntityListener(this, wgInst, perm, econ);
      comHandler = new CCCommandHandler(this, cHandler, wgInst);      
      getCommand("cclaimer").setExecutor(comHandler);

      //schedHandler = new BSchedulerHandler(this);

      readConfigValues();

      log.info(this.getDescription().getName() + " version " + getDescription().getVersion() + " is enabled!");

      //schedHandler.startPlayerInWaterCheckScheduler_SynchRepeating();
   }

   private void initLanguageList()
   {
      availableLanguages.add("de");
      availableLanguages.add("en");
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

   private boolean setupEconomy() 
   {
      RegisteredServiceProvider<Economy> rsp = getServer().getServicesManager().getRegistration(Economy.class);
      if (rsp != null)
      {
         econ = rsp.getProvider();
      }

      return (econ != null);
   }

   private boolean setupPermissions()
   {  
      RegisteredServiceProvider<Permission> permissionProvider = getServer().getServicesManager().getRegistration(net.milkbowl.vault.permission.Permission.class);
      if (permissionProvider != null)
      {
         perm = permissionProvider.getProvider();
      }
      return (perm != null);
   }

   public void readConfigValues()
   {
      boolean exceed = false;
      boolean invalid = false;

      debug = cHandler.getConfig().getBoolean("debug");      
      language = cHandler.getConfig().getString("language");

      if(!availableLanguages.contains(language))
      {
         language = "en"; // English is default
         invalid = true;
      }

      autoAddGroupsAsMembers = cHandler.getConfig().getStringList("autoAddGroupsAsMembers");      

      for(String group : cHandler.getConfig().getConfigurationSection("claimingLimits").getKeys(false))
      {
         claimingLimits.put(group, cHandler.getConfig().getConfigurationSection("claimingLimits").getInt(group));  
      }

      currency = cHandler.getConfig().getString("currency");
      basePricePerClaimedRegion = cHandler.getConfig().getDouble("basePricePerClaimedRegion");
      if(basePricePerClaimedRegion < 0){basePricePerClaimedRegion = 0; exceed = true;}
      if(basePricePerClaimedRegion > 100000){basePricePerClaimedRegion = 100000; exceed = true;}
      
      priceIncreasePerClaimedChunk = cHandler.getConfig().getInt("priceIncreasePerClaimedChunk");
      if(priceIncreasePerClaimedChunk < 0){priceIncreasePerClaimedChunk = 0; exceed = true;}
      if(priceIncreasePerClaimedChunk > 500){priceIncreasePerClaimedChunk = 500; exceed = true;}
      
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

      Plugin wgPlugin = getServer().getPluginManager().getPlugin("WorldGuard");

      // WorldGuard may not be loaded
      if ((null != wgPlugin) && (wgPlugin instanceof WorldGuardPlugin))
      {        
         this.wgInst = (WorldGuardPlugin) wgPlugin;
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


