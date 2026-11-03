package com.shopledger.util;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;

import java.io.InputStream;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Properties;

/**
 * 数据库工具：加载配置、创建数据库、初始化连接池并执行建表脚本。
 */
public final class Db {

    private static final Properties PROPS = new Properties();
    private static HikariDataSource dataSource;
    private static boolean initialized = false;

    private Db() {
    }

    public interface SqlFunction<T> {
        T apply(Connection connection) throws SQLException;
    }

    public static synchronized void init() {
        if (initialized) {
            return;
        }
        try (InputStream in = Db.class.getClassLoader().getResourceAsStream("db.properties")) {
            if (in == null) {
                throw new IllegalStateException("找不到 db.properties 配置文件");
            }
            PROPS.load(in);
            Class.forName(PROPS.getProperty("jdbc.driver", "com.mysql.cj.jdbc.Driver"));
            ensureDatabase();
            HikariConfig config = new HikariConfig();
            config.setJdbcUrl(PROPS.getProperty("jdbc.url"));
            config.setUsername(PROPS.getProperty("jdbc.username"));
            config.setPassword(PROPS.getProperty("jdbc.password"));
            config.setMaximumPoolSize(Integer.parseInt(PROPS.getProperty("pool.maximumPoolSize", "10")));
            config.setMinimumIdle(Integer.parseInt(PROPS.getProperty("pool.minimumIdle", "2")));
            config.setPoolName("shop-ledger-pool");
            config.setConnectionTimeout(10000);
            dataSource = new HikariDataSource(config);
            runSchema();
            initialized = true;
        } catch (Exception e) {
            throw new IllegalStateException("数据库初始化失败：" + e.getMessage(), e);
        }
    }

    /** 数据库已存在时直接复用；不存在时才尝试自动创建。 */
    private static void ensureDatabase() throws SQLException {
        String url = PROPS.getProperty("jdbc.url");
        String username = PROPS.getProperty("jdbc.username");
        String password = PROPS.getProperty("jdbc.password");

        // 先尝试连接目标库。使用专用账号时通常没有 CREATE DATABASE 权限，
        // 只要库已经由管理员创建好，这里就应该直接通过。
        try (Connection ignored = DriverManager.getConnection(url, username, password)) {
            return;
        } catch (SQLException e) {
            if (e.getErrorCode() != 1049) { // 1049 = Unknown database
                throw e;
            }
        }

        int queryIndex = url.indexOf('?');
        String base = queryIndex >= 0 ? url.substring(0, queryIndex) : url;
        int slash = base.lastIndexOf('/');
        if (slash < 0 || slash == base.length() - 1) {
            return;
        }
        String database = base.substring(slash + 1);
        String serverUrl = base.substring(0, slash + 1) + "?" + (queryIndex >= 0 ? url.substring(queryIndex + 1) : "");
        try (Connection conn = DriverManager.getConnection(serverUrl, username, password);
             Statement st = conn.createStatement()) {
            st.executeUpdate("CREATE DATABASE IF NOT EXISTS `" + database
                    + "` DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci");
        } catch (SQLException e) {
            throw new SQLException("数据库 " + database
                    + " 不存在，且当前账号无权创建。请先用管理员账号创建数据库，或给该账号授予建库权限。", e);
        }
    }

    private static void runSchema() throws Exception {
        String script;
        try (InputStream in = Db.class.getClassLoader().getResourceAsStream("schema.sql")) {
            if (in == null) {
                throw new IllegalStateException("找不到 schema.sql 建表脚本");
            }
            java.io.ByteArrayOutputStream out = new java.io.ByteArrayOutputStream();
            byte[] buffer = new byte[4096];
            int len;
            while ((len = in.read(buffer)) != -1) {
                out.write(buffer, 0, len);
            }
            script = new String(out.toByteArray(), java.nio.charset.StandardCharsets.UTF_8);
        }
        StringBuilder cleaned = new StringBuilder();
        for (String line : script.split("\\r?\\n")) {
            String trimmed = line.trim();
            if (trimmed.startsWith("--") || trimmed.isEmpty()) {
                continue;
            }
            cleaned.append(line).append('\n');
        }
        try (Connection conn = getConnection(); Statement st = conn.createStatement()) {
            for (String statement : cleaned.toString().split(";")) {
                if (!statement.trim().isEmpty()) {
                    st.execute(statement);
                }
            }
        }
    }

    public static Connection getConnection() throws SQLException {
        if (dataSource == null) {
            init();
        }
        return dataSource.getConnection();
    }

    public static <T> T inTransaction(SqlFunction<T> function) {
        try (Connection conn = getConnection()) {
            boolean autoCommit = conn.getAutoCommit();
            conn.setAutoCommit(false);
            try {
                T result = function.apply(conn);
                conn.commit();
                return result;
            } catch (Exception e) {
                conn.rollback();
                throw e;
            } finally {
                conn.setAutoCommit(autoCommit);
            }
        } catch (SQLException e) {
            throw new IllegalStateException("数据库操作失败：" + e.getMessage(), e);
        } catch (Exception e) {
            throw new IllegalStateException(e.getMessage(), e);
        }
    }

    public static synchronized void shutdown() {
        if (dataSource != null) {
            dataSource.close();
            dataSource = null;
            initialized = false;
        }
    }
}
