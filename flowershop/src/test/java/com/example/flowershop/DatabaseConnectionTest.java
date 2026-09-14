package com.example.flowershop;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
public class DatabaseConnectionTest {

    @Autowired
    private DataSource dataSource;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void testConnection() throws SQLException {
        assertThat(dataSource).isNotNull();
        try (Connection connection = dataSource.getConnection()) {
            assertThat(connection).isNotNull();
            assertThat(connection.isClosed()).isFalse();
            System.out.println("==========================================");
            System.out.println("--- KẾT NỐI DATABASE THÀNH CÔNG ---");
            System.out.println("Database Product Name: " + connection.getMetaData().getDatabaseProductName());
            System.out.println("Database Product Version: " + connection.getMetaData().getDatabaseProductVersion());
            System.out.println("==========================================");
        }
    }

    @Test
    void testQuery() {
        Integer result = jdbcTemplate.queryForObject("SELECT 1", Integer.class);
        assertThat(result).isEqualTo(1);
        System.out.println("==========================================");
        System.out.println("--- TRUY VẤN KIỂM TRA (SELECT 1) THÀNH CÔNG ---");
        System.out.println("==========================================");
    }
}
