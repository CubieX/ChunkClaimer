package com.github.CubieX.ChunkClaimer;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

public class CCConfigHandler 
{
   private FileConfiguration config;
   private FileConfiguration chunksToSellCfg = null;
   private File chunksToSellListFile = null;
   private final ChunkClaimer plugin;
   private final String chunksToSellListFileName = "chunksToSellList.yml";

   public CCConfigHandler(ChunkClaimer plugin) 
   {
      this.plugin = plugin;      

      initConfig();
   }

   private void initConfig()
   {
      plugin.saveDefaultConfig(); //creates a copy of the provided config.yml in the plugins data folder, if it does not exist
      config = plugin.getConfig(); //re-reads config out of memory. (Reads the config from file only, when invoked the first time!)

      // chunks to sell list 
      reloadChunksToSellListFile();    // load file from disk and create objects
      saveChunksToSellListDefaultFile();       // creates a copy of the provided chunksToSellList.yml
      reloadChunksToSellListFile();    // load file again after it is physically present now
   }

   public FileConfiguration getConfig()
   {
      return (config);
   }

   /*public void saveConfig() //saves the config to disc (needed when entries have been altered via the plugin in-game)
   {
      // get and set values here!
      plugin.saveConfig();
   }*/

   //reloads the config from disc (used if user made manual changes to the config.yml file)
   public void reloadConfig(CommandSender sender)
   {
      plugin.reloadConfig();
      config = plugin.getConfig(); // new assignment necessary when returned value is assigned to a variable or static field(!)

      reloadChunksToSellListFile();

      plugin.readConfigValues();      

      sender.sendMessage(ChunkClaimer.logPrefix + plugin.getDescription().getName() + " " + plugin.getDescription().getVersion() + " reloaded!");       
   }

   // =========================
   // fireList file handling
   // =========================

   // reload from disk
   public void reloadChunksToSellListFile()
   {
      if (chunksToSellListFile == null)
      {
         chunksToSellListFile = new File(plugin.getDataFolder(), chunksToSellListFileName);
      }
      chunksToSellCfg = YamlConfiguration.loadConfiguration(chunksToSellListFile);

      // Look for defaults in the jar
      InputStream defConfigStream = plugin.getResource(chunksToSellListFileName);
      if (defConfigStream != null)
      {
         YamlConfiguration defConfig = YamlConfiguration.loadConfiguration(defConfigStream);
         chunksToSellCfg.setDefaults(defConfig);
      }
   }

   // reload config and return it
   public FileConfiguration getChunksToSellListFile()
   {
      if (chunksToSellCfg == null)
      {
         this.reloadChunksToSellListFile();
      }
      return chunksToSellCfg;
   }

   //save config
   public void saveFireListFile()
   {
      if (chunksToSellCfg == null || chunksToSellListFile == null)
      {
         return;
      }
      try
      {
         getChunksToSellListFile().save(chunksToSellListFile);
      }
      catch (IOException ex)
      {
         ChunkClaimer.log.severe("Could not save data to " + chunksToSellListFile.getName());
         ChunkClaimer.log.severe(ex.getMessage());
      }
   }

   // safe a default config if there is no file present
   public void saveChunksToSellListDefaultFile()
   {
      if (!chunksToSellListFile.exists())
      {            
         plugin.saveResource(chunksToSellListFileName, false);
      }
   }
}
