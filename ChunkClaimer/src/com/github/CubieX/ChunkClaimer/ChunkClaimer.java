package com.github.CubieX.ChunkClaimer;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Set;
import java.util.logging.Logger;

import net.milkbowl.vault.economy.Economy;
import net.milkbowl.vault.permission.Permission;

import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.java.JavaPlugin;
import com.sk89q.worldguard.LocalPlayer;
import com.sk89q.worldguard.bukkit.WorldGuardPlugin;
import com.sk89q.worldguard.protection.GlobalRegionManager;
import com.sk89q.worldguard.protection.managers.RegionManager;

public class ChunkClaimer extends JavaPlugin
{
   static final Logger log = Bukkit.getServer().getLogger();
   static final String logPrefix = "[ChunkClaimer] "; // Prefix to go in front of all log entries
   static ArrayList<String> availableLanguages = new ArrayList<String>();
   static Permission perm = null;
   static Economy econ = null;
   
   static final String ccRegionPrefix = "chunk"; // WARNING: Change this only for very good reasons! Will break compatibility with earlier created CC regions!!!
   static final String keyOfferingTime = "offeringTime";
   static final String keySellingPlayer = "sellingPlayer";
   static final String keyPrice = "price";
   static final int maxSellingPrice = 100000000; // max. 100.000.000
   
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
      comHandler = new CCCommandHandler(this, cHandler, wgInst, perm, econ);      
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

   public String getOwnerNamesOfRegionAsString(RegionManager rm, String region)
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
  
   public String getMemberNamesOfRegion(RegionManager rm, String region)
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
   public int getPlayersGlobalCCregionCount(GlobalRegionManager wgGlobalRM, LocalPlayer lPlayer)
   {
      int count = 0;

      // counts owned CC regions of player in all enabled worlds
      for(World w : Bukkit.getServer().getWorlds())
      {
         if(plugin.getConfig().getStringList("enabledWorlds").contains(w.getName()))
         {
            Set<String> playerRegionsInWorld = wgGlobalRM.get(w).getRegions().keySet();

            for(String regName : playerRegionsInWorld)
            {
               if(regName.startsWith(ChunkClaimer.ccRegionPrefix))
               {
                  if(wgGlobalRM.get(w).getRegion(regName).getOwners().getPlayers().contains(lPlayer.getName().toLowerCase()))
                  {
                     count ++;
                  }
               }
            }
         }
      }

      return (count);
   }

   // gets global claimLimit of player (defined for his group in config)
   public int getPlayersGlobalClaimLimit(Player player)
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

   // *****************************************************
   // ChunksToSell list actions
   // *****************************************************

   public void addChunkToSellList(String worldName, String ccRegionName, String playerName, int price)
   {
      cHandler.getChunksToSellListFile().set(worldName + "." + ccRegionName + "." + keyOfferingTime, getCurrTimeInMillis()); // adds the chunk and the offering time
      cHandler.getChunksToSellListFile().set(worldName + "." + ccRegionName + "." + keySellingPlayer, playerName); // adds the name of the selling player
      cHandler.getChunksToSellListFile().set(worldName + "." + ccRegionName + "." + keyPrice, price); // adds the price for the chunk
      cHandler.saveFireListFile();
   }
   
   public void updateChunkOnSellList(String worldName, String ccRegionName, int price)
   {      
      cHandler.getChunksToSellListFile().set(worldName + "." + ccRegionName + "." + keyPrice, price); // updates the price for the chunk
      cHandler.saveFireListFile();
   }

   public void removeChunkFromFromSellList(String worldName, String ccRegionName)
   {
      cHandler.getChunksToSellListFile().set(worldName + "." + ccRegionName, null); // delete the entry
      cHandler.saveFireListFile();
   }

   public boolean chunkIsOnSale(String worldName, String ccRegionName)
   {
      boolean isOnSale = false;
      
      if(cHandler.getChunksToSellListFile().contains(worldName + "." + ccRegionName))
      {
         isOnSale = true;
      }

      return(isOnSale);
   }
   
   public String getSellingPlayerOfChunkOnSale(String worldName, String ccRegionName)
   {
      String playerName = cHandler.getChunksToSellListFile().getString(worldName + "." + ccRegionName + "." + keySellingPlayer);
      
      return(playerName);
   }
   
   public int getPriceOfChunkOnSale(String worldName, String ccRegionName)
   {
      int price = cHandler.getChunksToSellListFile().getInt(worldName + "." + ccRegionName + "." + keyPrice);
      
      return(price);
   }

   public long getCurrTimeInMillis()
   {
      return (((Calendar)Calendar.getInstance()).getTimeInMillis());
   }
}


