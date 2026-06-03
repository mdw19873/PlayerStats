package com.artemis.the.gr8.playerstats.core.config;

import com.artemis.the.gr8.playerstats.MockBukkitTestBase;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.File;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Guards the cached-settings path: scalar getters serve fields populated on
 * construction and refreshed on {@link ConfigHandler#reload()}, so a config
 * edit must be visible after a reload.
 */
@DisplayName("ConfigHandler cached settings")
class ConfigHandlerTest extends MockBukkitTestBase {

    @Test
    @DisplayName("reload() refreshes cached scalar settings from disk")
    void reloadRefreshesCachedSettings() throws Exception {
        ConfigHandler config = ConfigHandler.getInstance();
        assertThat(config.getTopListMaxSize()).isEqualTo(10);  //bundled default

        File file = new File(plugin.getDataFolder(), "config.yml");
        FileConfiguration raw = YamlConfiguration.loadConfiguration(file);
        raw.set("top-list-max-size", 25);
        raw.save(file);

        config.reload();

        assertThat(config.getTopListMaxSize()).isEqualTo(25);
    }
}
