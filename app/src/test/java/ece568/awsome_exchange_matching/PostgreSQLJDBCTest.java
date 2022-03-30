package ece568.awsome_exchange_matching;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class PostgreSQLJDBCTest {
    protected PostgreSQLJDBC postgreJDBC = null;
    @Test
    void populateOrder() throws Exception {
        postgreJDBC = postgreJDBC.getInstance("postgres", "postgres",
                "jdbc:postgresql://localhost:4444/postgres");
    }
}