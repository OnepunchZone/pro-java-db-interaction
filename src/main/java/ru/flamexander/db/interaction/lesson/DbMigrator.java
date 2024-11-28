package ru.flamexander.db.interaction.lesson;

import org.h2.jdbc.JdbcSQLSyntaxErrorException;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.sql.SQLException;
import java.sql.Connection;
import java.sql.Statement;

public class DbMigrator {
    private DataSource dataSource;

    public DbMigrator(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    public void migrate() throws SQLException, IOException {
        String sqlFileName = "dbinit.sql";

        String sql = loadSqlFromFile(sqlFileName);

        executeSql(dataSource.getConnection(), sql);

    }

    private String loadSqlFromFile(String fileName) throws IOException {
        StringBuilder builder = new StringBuilder();

        try (InputStream inputS = getClass().getClassLoader().getResourceAsStream(fileName)) {
            assert inputS != null;
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(inputS))) {

                String line;
                while ((line = reader.readLine()) != null) {
                    builder.append(line).append("\n");
                }
            }
        }

        return builder.toString();
    }

    private void executeSql(Connection connection, String sql) throws SQLException {
        try (Statement statement = connection.createStatement()) {
            for (String command : sql.split(";")) {
                if (!command.trim().isEmpty()) {
                    statement.execute(command.trim());
                    System.out.println("Выполнилась команда: " + command.trim());
                }
            }
        } catch (JdbcSQLSyntaxErrorException e) {
            System.out.println("Таблица уже существует");
        }
    }
}
