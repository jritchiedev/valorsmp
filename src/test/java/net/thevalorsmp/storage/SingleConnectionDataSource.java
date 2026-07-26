package net.thevalorsmp.storage;

import java.io.PrintWriter;
import java.lang.reflect.Proxy;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.SQLFeatureNotSupportedException;
import java.util.logging.Logger;
import javax.sql.DataSource;

/**
 * Test-only {@link DataSource} handing out the same {@link Connection} repeatedly, with
 * {@code close()} suppressed so code under test can use try-with-resources without ending the
 * test's SQLite session.
 */
final class SingleConnectionDataSource implements DataSource {

    private final Connection sharedConnection;

    private SingleConnectionDataSource(Connection sharedConnection) {
        this.sharedConnection = sharedConnection;
    }

    static DataSource wrapping(Connection connection) {
        Connection nonClosing = (Connection) Proxy.newProxyInstance(
                SingleConnectionDataSource.class.getClassLoader(),
                new Class<?>[] {Connection.class},
                (proxy, method, args) -> {
                    if ("close".equals(method.getName())) {
                        return null;
                    }
                    try {
                        return method.invoke(connection, args);
                    } catch (java.lang.reflect.InvocationTargetException e) {
                        throw e.getCause();
                    }
                });
        return new SingleConnectionDataSource(nonClosing);
    }

    @Override
    public Connection getConnection() {
        return sharedConnection;
    }

    @Override
    public Connection getConnection(String username, String password) {
        return sharedConnection;
    }

    @Override
    public PrintWriter getLogWriter() throws SQLException {
        throw new SQLFeatureNotSupportedException();
    }

    @Override
    public void setLogWriter(PrintWriter out) throws SQLException {
        throw new SQLFeatureNotSupportedException();
    }

    @Override
    public void setLoginTimeout(int seconds) throws SQLException {
        throw new SQLFeatureNotSupportedException();
    }

    @Override
    public int getLoginTimeout() {
        return 0;
    }

    @Override
    public Logger getParentLogger() throws SQLFeatureNotSupportedException {
        throw new SQLFeatureNotSupportedException();
    }

    @Override
    public <T> T unwrap(Class<T> iface) throws SQLException {
        if (iface.isInstance(this)) {
            return iface.cast(this);
        }
        throw new SQLException("Cannot unwrap to " + iface.getName());
    }

    @Override
    public boolean isWrapperFor(Class<?> iface) {
        return iface.isInstance(this);
    }
}
