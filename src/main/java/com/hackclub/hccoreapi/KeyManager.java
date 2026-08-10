package com.hackclub.hccoreapi;

import com.hackclub.hccoreapi.DataTypes.APIAccess;
import org.bukkit.configuration.ConfigurationSection;

import javax.annotation.Nullable;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.Objects;

public class KeyManager {
    private final HCCoreAPI plugin;
    public KeyManager(HCCoreAPI plugin) {
        this.plugin = plugin;
    }
    @Nullable
    public APIAccess getAccessByID(String id) {
        ConfigurationSection keySection = plugin.getConfig().getConfigurationSection("keys");
        if (keySection == null) {
            return null;
        }
        ConfigurationSection keyData = keySection.getConfigurationSection(id);
        if (keyData == null) {
            return null;
        }
        return new APIAccess(id, keyData.getString("key"), keyData.getInt("rate_limit"), keyData.getBoolean("enabled"), this);
    }
    @Nullable
    public APIAccess getAccessByKey(String key) {
        ConfigurationSection keySection = plugin.getConfig().getConfigurationSection("keys");
        if (keySection == null) {
            return null;
        }
        String id = keySection.getKeys(false).stream().filter(item -> Objects.equals(keySection.getString(item + ".key"), key)).findFirst().orElse(null);
        if (id == null) {
            return null;
        }
        return getAccessByID(id);
    }
    public boolean isKeyValid(APIAccess access) {
        return plugin.getConfig().getBoolean("keys." + access.id + ".enabled", false);
    }
    @Nullable
    public APIAccess createNewKey(String id) {
        if (plugin.getConfig().contains("keys." + id)) {
            return null;
        }
        byte[] random = new byte[24];
        new SecureRandom().nextBytes(random);
        String key = Base64.getUrlEncoder().withoutPadding().encodeToString(random);
        ConfigurationSection section = plugin.getConfig().createSection("keys." + id);
        section.set("key", key);
        section.set("rate_limit", 10);
        section.set("enabled", true);
        return new APIAccess(id, key, 10, true, this);
    }
}
