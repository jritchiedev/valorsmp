package net.thevalorsmp.config;

import static org.assertj.core.api.Assertions.assertThat;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.Test;
import org.slf4j.helpers.NOPLogger;

class ProgressionConfigTest {

    @Test
    void load_emptySection_usesDocumentedDefaults() {
        ProgressionConfig config = ProgressionConfig.load(emptySection(), NOPLogger.NOP_LOGGER);

        assertThat(config.currentSeason()).isEqualTo(1);
        assertThat(config.tierThresholds()).containsExactly(0, 5, 9, 13, 17);
        assertThat(config.extendedPotionEffects()).isTrue();
        assertThat(config.potionDurationExtensions())
                .containsEntry(9600, 12000)
                .containsEntry(1800, 3600);
    }

    @Test
    void load_customExtensions_areParsed() {
        ConfigurationSection section = emptySection();
        ConfigurationSection extensions = section.createSection("potion-duration-extensions");
        extensions.set("2400", 4800);

        ProgressionConfig config = ProgressionConfig.load(section, NOPLogger.NOP_LOGGER);

        assertThat(config.potionDurationExtensions()).containsExactlyInAnyOrderEntriesOf(java.util.Map.of(2400, 4800));
    }

    @Test
    void load_invalidThresholds_fallBackToDefault() {
        ConfigurationSection section = emptySection();
        section.set("tier-thresholds", java.util.List.of(0, 5, 5, 13, 17)); // not strictly ascending

        ProgressionConfig config = ProgressionConfig.load(section, NOPLogger.NOP_LOGGER);

        assertThat(config.tierThresholds()).containsExactly(0, 5, 9, 13, 17);
    }

    private static ConfigurationSection emptySection() {
        return new YamlConfiguration().createSection("progression");
    }
}
