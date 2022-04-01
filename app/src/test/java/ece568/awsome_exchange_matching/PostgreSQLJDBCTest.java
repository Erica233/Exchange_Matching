package ece568.awsome_exchange_matching;

import org.junit.jupiter.api.Test;

import java.sql.*;

import static org.junit.jupiter.api.Assertions.*;

class PostgreSQLJDBCTest {
    protected PostgreSQLJDBC postgreJDBC = null;

    @Test
    void handleSell() {
    }

    @Test
    void handleBuy() {
    }

    PostgreSQLJDBC createdb() throws SQLException {
        try {
            postgreJDBC = postgreJDBC.getInstance("postgres", "postgres",
                    "jdbc:postgresql://localhost:4444/postgres");
            //jdbc:postgresql://database:5432/postgres
        } catch (Exception e) {
            e.printStackTrace();
            System.err.println(e.getClass().getName() + ": " + e.getMessage());
            System.exit(0);
        }
        postgreJDBC.populateAccount("1", "1000000");
        postgreJDBC.populateAccount("2", "1000000");
        postgreJDBC.populateAccount("3", "1000000");
        postgreJDBC.populateAccount("4", "1000000");
        postgreJDBC.populateAccount("5", "1000000");
        postgreJDBC.populateAccount("6", "1000000");
        postgreJDBC.populateAccount("7", "1000000");
        postgreJDBC.populateAccount("8", "1000000");
        postgreJDBC.populateAccount("9", "1000000");
        postgreJDBC.populatePosition("USD", "1", "10000");
        postgreJDBC.populatePosition("USD", "2", "10000");
        postgreJDBC.populatePosition("USD", "3", "10000");
        postgreJDBC.populatePosition("USD", "4", "10000");
        postgreJDBC.populatePosition("USD", "5", "10000");
        postgreJDBC.populatePosition("USD", "6", "10000");
        postgreJDBC.populatePosition("USD", "7", "10000");
        postgreJDBC.populatePosition("USD", "8", "10000");
        postgreJDBC.populatePosition("USD", "9", "10000");
        postgreJDBC.populateOrder("1", "USD", "300", "125");
        postgreJDBC.populateOrder("2", "USD", "-100", "130");
        postgreJDBC.populateOrder("3", "USD", "200", "127");
        postgreJDBC.populateOrder("4", "USD", "-500", "128");
        postgreJDBC.populateOrder("5", "USD", "-200", "140");
        postgreJDBC.populateOrder("6", "USD", "400", "125");
        return postgreJDBC;
    }

    @Test
    void populateOrder() throws SQLException {
        postgreJDBC = createdb();

        //postgreJDBC.populateOrder("7", "USD", "-400", "124");
        //postgreJDBC.populateOrder("8", "USD", "550", "135");
        //postgreJDBC.populateOrder("9", "USD", "-100", "127.5");
        //postgreJDBC.populateOrder("9", "USD", "100", "127.5");
        postgreJDBC.populateOrder("7", "USD", "-400", "127");
        Connection c = DriverManager.getConnection(postgreJDBC.getUrl(), postgreJDBC.getUser(), postgreJDBC.getPassword());
        postgreJDBC.setC(c);
        Statement st = postgreJDBC.getC().createStatement();

        ResultSet rs= st.executeQuery("SELECT * FROM ORDERS WHERE ACCOUNT_ID = \'7\' AND STATUS = \'EXECUTED\'");
        assertTrue(rs.next());
        while(rs.next()){
            double amount = rs.getDouble("amount");
            assertEquals(amount, 200);
        }
        assertThrows(IllegalArgumentException.class, ()->postgreJDBC.populateOrder("100000", "USD", "100", "100"));
    }

    @Test
    void queryTransaction() throws SQLException {
        postgreJDBC = createdb();
    }

    @Test
    void cancelTransaction() throws SQLException {
        postgreJDBC = createdb();
        //postgreJDBC.cancelTransaction("1", "99");
        //postgreJDBC.cancelTransaction("99", "1");
        postgreJDBC.cancelTransaction("1", "1");
    }
}