package net.thevalorsmp.config;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.slf4j.helpers.NOPLogger;

class ConfigBootstrapTest {

    private static final String RESOURCE = "config/database.yml";

    @TempDir
    private Path dataFolder;

    @Test
    void copyAndMergeDefaults_firstRun_writesPackagedDefault() {
        ConfigBootstrap bootstrap = newBootstrap();

        bootstrap.copyAndMergeDefaults(List.of(RESOURCE), loaderFor(Map.of(RESOURCE, """
                database:
                  backend: sqlite
                """)));

        assertThat(dataFolder.resolve(RESOURCE)).exists();
        assertThat(bootstrap.load(RESOURCE).getString("database.backend")).isEqualTo("sqlite");
    }

    @Test
    void copyAndMergeDefaults_existingFile_addsNewKeysAndKeepsOperatorValues() throws Exception {
        writeExisting("""
                database:
                  backend: mysql
                """);

        ConfigBootstrap bootstrap = newBootstrap();
        bootstrap.copyAndMergeDefaults(List.of(RESOURCE), loaderFor(Map.of(RESOURCE, """
                database:
                  backend: sqlite
                  pool:
                    maximum-pool-size: 10
                """)));

        YamlConfiguration merged = bootstrap.load(RESOURCE);
        assertThat(merged.getString("database.backend")).isEqualTo("mysql");
        assertThat(merged.getInt("database.pool.maximum-pool-size")).isEqualTo(10);
    }

    @Test
    void copyAndMergeDefaults_noKeysToAdd_leavesFileByteIdentical() throws Exception {
        String operatorFile = """
                database:
                  backend: mysql
                """;
        writeExisting(operatorFile);

        newBootstrap().copyAndMergeDefaults(List.of(RESOURCE), loaderFor(Map.of(RESOURCE, """
                database:
                  backend: sqlite
                """)));

        assertThat(Files.readString(dataFolder.resolve(RESOURCE))).isEqualTo(operatorFile);
    }

    @Test
    void copyAndMergeDefaults_missingPackagedResource_failsFast() {
        ConfigBootstrap bootstrap = newBootstrap();

        assertThat(catchThrowableOfType(
                IllegalStateException.class,
                () -> bootstrap.copyAndMergeDefaults(List.of(RESOURCE), loaderFor(Map.of()))))
                .hasMessageContaining(RESOURCE);
    }

    @Test
    void load_missingFile_returnsEmptyConfiguration() {
        assertThat(newBootstrap().load(RESOURCE).getKeys(false)).isEmpty();
    }

    private static <T extends Throwable> T catchThrowableOfType(Class<T> type, Runnable runnable) {
        try {
            runnable.run();
        } catch (Throwable thrown) {
            assertThat(thrown).isInstanceOf(type);
            return type.cast(thrown);
        }
        throw new AssertionError("Expected " + type.getSimpleName() + " to be thrown");
    }

    private ConfigBootstrap newBootstrap() {
        return new ConfigBootstrap(dataFolder, NOPLogger.NOP_LOGGER);
    }

    private void writeExisting(String contents) throws Exception {
        Path target = dataFolder.resolve(RESOURCE);
        Files.createDirectories(target.getParent());
        Files.writeString(target, contents);
    }

    private static ConfigBootstrap.ResourceLoader loaderFor(Map<String, String> resources) {
        return name -> {
            String contents = resources.get(name);
            if (contents == null) {
                return null;
            }
            InputStream stream = new ByteArrayInputStream(contents.getBytes(StandardCharsets.UTF_8));
            return stream;
        };
    }
}
