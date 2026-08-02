package net.thevalorsmp.storage.migration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.sql.SQLException;
import java.util.List;
import java.util.regex.Pattern;
import net.thevalorsmp.storage.RepositoryException;
import org.junit.jupiter.api.Test;

class SqlDialectTest {

    /** Valid on SQLite, a syntax error on MySQL — the reason this dialect layer exists. */
    private static final Pattern CREATE_INDEX_IF_NOT_EXISTS =
            Pattern.compile("CREATE\\s+(UNIQUE\\s+)?INDEX\\s+IF\\s+NOT\\s+EXISTS", Pattern.CASE_INSENSITIVE);

    @Test
    void translate_createIndexIfNotExists_mysqlDropsUnsupportedClause() {
        String translated = SqlDialect.MYSQL.translate(
                "CREATE INDEX IF NOT EXISTS idx_player_profiles_last_seen_at ON player_profiles (last_seen_at)");

        assertThat(translated)
                .isEqualTo("CREATE INDEX idx_player_profiles_last_seen_at ON player_profiles (last_seen_at)");
    }

    @Test
    void translate_createTableIfNotExists_mysqlKeepsSupportedClause() {
        String statement = "CREATE TABLE IF NOT EXISTS valor_scores (uuid CHAR(36) NOT NULL, PRIMARY KEY (uuid))";

        assertThat(SqlDialect.MYSQL.translate(statement)).isEqualTo(statement);
    }

    @Test
    void translate_sqlite_returnsStatementVerbatim() {
        String statement = "CREATE INDEX IF NOT EXISTS idx_valor_scores_season_score ON valor_scores (season, score)";

        assertThat(SqlDialect.SQLITE.translate(statement)).isEqualTo(statement);
    }

    @Test
    void translate_everyShippedMigration_isMysqlCompatible() throws IOException {
        for (String migration : SqlMigrationRunner.DEFAULT_MIGRATIONS) {
            for (String statement : statementsOf(migration)) {
                assertThat(SqlDialect.MYSQL.translate(statement))
                        .as("%s: %s", migration, statement)
                        .doesNotContainPattern(CREATE_INDEX_IF_NOT_EXISTS);
            }
        }
    }

    @Test
    void forProductName_recognisesSupportedBackends() {
        assertThat(SqlDialect.forProductName("SQLite")).isEqualTo(SqlDialect.SQLITE);
        assertThat(SqlDialect.forProductName("MySQL")).isEqualTo(SqlDialect.MYSQL);
        assertThat(SqlDialect.forProductName("MariaDB")).isEqualTo(SqlDialect.MYSQL);
    }

    @Test
    void forProductName_unsupportedBackend_rejected() {
        assertThatThrownBy(() -> SqlDialect.forProductName("Microsoft SQL Server"))
                .isInstanceOf(RepositoryException.class)
                .hasMessageContaining("Microsoft SQL Server");
    }

    @Test
    void isDuplicateObjectError_onlyMysqlTreatsDuplicateIndexAsApplied() {
        SQLException duplicateIndex = new SQLException("Duplicate key name 'idx'", "42000", 1061);
        SQLException other = new SQLException("Table doesn't exist", "42S02", 1146);

        assertThat(SqlDialect.MYSQL.isDuplicateObjectError(duplicateIndex)).isTrue();
        assertThat(SqlDialect.MYSQL.isDuplicateObjectError(other)).isFalse();
        assertThat(SqlDialect.SQLITE.isDuplicateObjectError(duplicateIndex)).isFalse();
    }

    @Test
    void supportsTransactionalDdl_trueOnlyWhereRollbackIsReal() {
        assertThat(SqlDialect.SQLITE.supportsTransactionalDdl()).isTrue();
        assertThat(SqlDialect.MYSQL.supportsTransactionalDdl()).isFalse();
    }

    private static List<String> statementsOf(String migration) throws IOException {
        String resource = "db/migration/" + migration;
        try (InputStream in = SqlDialectTest.class.getClassLoader().getResourceAsStream(resource)) {
            assertThat(in).as("migration resource %s", resource).isNotNull();
            // Split exactly as the runner does, so translation is exercised on the real statements.
            return SqlMigrationRunner.splitStatements(new String(in.readAllBytes(), StandardCharsets.UTF_8));
        }
    }
}
