package com.hackclub.hccoreapi;

import com.hackclub.hccoreapi.DataTypes.APIKey;
import org.bukkit.configuration.ConfigurationSection;

public class KeyManager {
    private HCCoreAPI plugin;
    public KeyManager(HCCoreAPI plugin) {
        this.plugin = plugin;
    }
    public String getKeyByID(String id) {
        ConfigurationSection keySection = plugin.getConfig().getConfigurationSection("keys");
        if (keySection == null) {
            return null;
        }
        keySection.getKeys(false).stream().filter(item -> item == "").findFirst();
        return "aaaa";
    }
    public APIKey checkKey(String key) {
        return new APIKey();
    }
}
