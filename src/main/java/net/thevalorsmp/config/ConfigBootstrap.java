package net.thevalorsmp.config;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Objects;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.YamlConfiguration;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;

/**
 * Materialises the shipped default config files into the operator-facing data folder and merges keys
 * added by newer plugin versions into files the operator has already customised
 * (CONFIGURATION.md sections 2 and 5).
 */
public final class ConfigBootstrap {

    /** Supplies the shipped default file for a classpath-relative resource name. */
    @FunctionalInterface
    public interface ResourceLoader {

        /**
         * Opens the packaged default resource.
         *
         * @param resourceName classpath-relative name, e.g. {@code config/database.yml}
         * @return an open stream, or {@code null} if the resource is not packaged
         */
        @Nullable InputStream open(@NotNull String resourceName);
    }

    private final Path dataFolder;
    private final Logger logger;

    /**
     * Creates a bootstrap rooted at the plugin's data folder.
     *
     * @param dataFolder the plugin data folder (operator-facing config lives beneath it)
     * @param logger     plugin logger
     */
    public ConfigBootstrap(@NotNull Path dataFolder, @NotNull Logger logger) {
        this.dataFolder = Objects.requireNonNull(dataFolder, "dataFolder");
        this.logger = Objects.requireNonNull(logger, "logger");
    }

    /**
     * Writes any missing config file from its packaged default, and adds keys that exist in the
     * packaged default but not in the operator's file. Operator-set values are never overwritten.
     *
     * @param resourceNames classpath-relative resource names to materialise
     * @param loader        access to the packaged defaults
     */
    public void copyAndMergeDefaults(@NotNull List<String> resourceNames, @NotNull ResourceLoader loader) {
        Objects.requireNonNull(resourceNames, "resourceNames");
        Objects.requireNonNull(loader, "loader");
        for (String resourceName : resourceNames) {
            YamlConfiguration defaults = readPackagedDefault(resourceName, loader);
            Path target = dataFolder.resolve(resourceName);
            try {
                Files.createDirectories(target.getParent());
                if (!Files.exists(target)) {
                    writePackagedDefault(resourceName, loader, target);
                    logger.info("Created default config file {}.", resourceName);
                    continue;
                }
                mergeMissingKeys(defaults, target, resourceName);
            } catch (IOException e) {
                throw new UncheckedIOException("Failed to bootstrap config file " + resourceName, e);
            }
        }
    }

    /**
     * Loads an operator-facing config file that {@link #copyAndMergeDefaults} has already materialised.
     *
     * @param resourceName classpath-relative resource name, e.g. {@code config/database.yml}
     * @return the parsed configuration
     */
    public @NotNull YamlConfiguration load(@NotNull String resourceName) {
        Objects.requireNonNull(resourceName, "resourceName");
        return YamlConfiguration.loadConfiguration(dataFolder.resolve(resourceName).toFile());
    }

    private void mergeMissingKeys(YamlConfiguration defaults, Path target, String resourceName) throws IOException {
        YamlConfiguration existing = YamlConfiguration.loadConfiguration(target.toFile());
        boolean changed = false;
        for (String key : defaults.getKeys(true)) {
            if (defaults.isConfigurationSection(key) || existing.contains(key)) {
                continue;
            }
            existing.set(key, defaults.get(key));
            changed = true;
            logger.info("Added new config key {} to {} with its default value.", key, resourceName);
        }
        if (changed) {
            existing.save(target.toFile());
        }
    }

    private YamlConfiguration readPackagedDefault(String resourceName, ResourceLoader loader) {
        try (InputStream in = openOrThrow(resourceName, loader);
                InputStreamReader reader = new InputStreamReader(in, StandardCharsets.UTF_8)) {
            YamlConfiguration configuration = new YamlConfiguration();
            configuration.load(reader);
            return configuration;
        } catch (IOException e) {
            throw new UncheckedIOException("Failed to read packaged default " + resourceName, e);
        } catch (InvalidConfigurationException e) {
            throw new IllegalStateException("Packaged default " + resourceName + " is not valid YAML", e);
        }
    }

    private void writePackagedDefault(String resourceName, ResourceLoader loader, Path target) throws IOException {
        try (InputStream in = openOrThrow(resourceName, loader)) {
            Files.copy(in, target);
        }
    }

    private InputStream openOrThrow(String resourceName, ResourceLoader loader) {
        InputStream in = loader.open(resourceName);
        if (in == null) {
            throw new IllegalStateException("Packaged default config resource is missing: " + resourceName);
        }
        return in;
    }
}
