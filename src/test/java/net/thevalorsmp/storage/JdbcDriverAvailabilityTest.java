package net.thevalorsmp.storage;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.sql.DriverManager;
import java.sql.SQLException;
import org.junit.jupiter.api.Test;

/**
 * Guards the runtime driver contract: every backend {@link DataSourceFactory} can configure must have
 * a JDBC driver both on the compile classpath and declared in {@code plugin.yml} for Paper's library
 * loader, otherwise the pool fails at startup with "No suitable driver".
 */
class JdbcDriverAvailabilityTest {

    @Test
    void driverManager_sqliteUrl_resolvesDriver() throws SQLException {
        assertThat(DriverManager.getDriver("jdbc:sqlite:data.db"))
                .isInstanceOf(org.sqlite.JDBC.class);
    }

    @Test
    void driverManager_mysqlUrl_resolvesDriver() throws SQLException {
        assertThat(DriverManager.getDriver("jdbc:mysql://localhost:3306/valorsmp"))
                .isInstanceOf(com.mysql.cj.jdbc.Driver.class);
    }

    @Test
    void pluginYml_declaresDriverLibraryForEveryBackend() throws IOException {
        String pluginYml = readResource("plugin.yml");

        assertThat(pluginYml)
                .contains("org.xerial:sqlite-jdbc:")
                .contains("com.mysql:mysql-connector-j:")
                .doesNotContain("${");
    }

    private static String readResource(String name) throws IOException {
        try (InputStream stream = JdbcDriverAvailabilityTest.class.getClassLoader().getResourceAsStream(name)) {
            assertThat(stream).as("resource %s on the test classpath", name).isNotNull();
            return new String(stream.readAllBytes(), StandardCharsets.UTF_8);
        }
    }
}
