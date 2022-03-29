package ece568.awsome_exchange_matching;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.ResultSet;

public class PostgreSQLJDBC {
    private static PostgreSQLJDBC postgreJDBC = null;

    private static String url;
    private static String user;
    private static String password;
    private static Connection c = null;
    private static Statement stmt = null;
    /**
     * constructor
     * @param _user
     * @param _password
     * @param _url
     */
    private PostgreSQLJDBC(String _user, String _password, String _url) throws Exception{
        url = _url;
        user = _user;
        password = _password;
        connectDB();
        stmt = c.createStatement();
        deleteTables();
        createTables();
        stmt.close();
        c.close();
    }

    /**
     * get the singleton PostgreSQLJDBC object
     * @param _user
     * @param _password
     * @param _url
     * @return
     * @throws Exception
     */
    public static PostgreSQLJDBC getInstance(String _user, String _password, String _url) throws Exception {
        if (postgreJDBC == null){
            synchronized(PostgreSQLJDBC.class){
                if (postgreJDBC == null){
                    postgreJDBC = new PostgreSQLJDBC(_user, _password, _url);
                }
            }
        }
        return postgreJDBC;
    }
    /**
     * connect to database
     * @return
     */
    public static void connectDB() throws Exception{
        Class.forName("org.postgresql.Driver");
        c = DriverManager
                .getConnection(url, user, password);
        System.out.println("Opened database successfully");

    }

    private static void deleteTables() throws Exception{
        String sql = "DROP TABLE IF EXISTS ORDERS;\n"+
                "DROP TABLE IF EXISTS POSITION;\n"+
                "DROP TABLE IF EXISTS ACCOUNT;";
        stmt.executeUpdate(sql);
        System.out.println("delete tables successfully");
    }

    /**
     * create tables Account, position, orders
     * @throws Exception
     */
    private static void createTables() throws Exception{
        stmt = c.createStatement();
        String sql = "CREATE TABLE IF NOT EXISTS ACCOUNT " +
                "(ID VARCHAR(25) PRIMARY KEY     NOT NULL CHECK(ID ~ '^[0-9]*$') ," +
                " BALANCE      NUMERIC           NOT NULL CHECK (BALANCE>=0));";

        String sql2 = "CREATE TABLE IF NOT EXISTS POSITION"+
                "(ID SERIAL PRIMARY KEY    NOT NULL,"+
                "SYMBOL VARCHAR(255)       NOT NULL,"+
                "AMOUNT NUMERIC            NOT NULL  CHECK (AMOUNT>0),"+
                "ACCOUNT_ID VARCHAR(25)    NOT NULL  CHECK(ACCOUNT_ID ~ '^[0-9]*$'),"+
                "CONSTRAINT FK_POSITION FOREIGN KEY(ACCOUNT_ID) \n" +
                "   REFERENCES ACCOUNT(ID)\n" +
                "   ON DELETE CASCADE\n" +
                "   ON UPDATE CASCADE,"+
                "CONSTRAINT CHK_POSITION CHECK(AMOUNT > 0))";

        String sql3 = "CREATE TABLE IF NOT EXISTS ORDERS"+
                "(ID SERIAL PRIMARY KEY    NOT NULL,"+
                "TRANSACTION_ID SERIAL     NOT NULL,"+
                "TIME TIMESTAMP            NOT NULL,"+
                "SYMBOL VARCHAR(255)       NOT NULL,"+
                "AMOUNT NUMERIC            NOT NULL,"+
                "ACCOUNT_ID VARCHAR(25)    NOT NULL     CHECK(ACCOUNT_ID ~ '^[0-9]*$'),"+
                "PRICE NUMERIC             NOT NULL,"+
                "STATUS VARCHAR(255)       NOT NULL DEFAULT \'OPEN\',"+
                "CONSTRAINT FK_ORDER FOREIGN KEY(ACCOUNT_ID) \n" +
                "   REFERENCES ACCOUNT(ID)\n" +
                "   ON DELETE CASCADE\n" +
                "   ON UPDATE CASCADE,"+
                "CONSTRAINT CHK_ORDER CHECK(AMOUNT != 0))";
        stmt.executeUpdate(sql);
        System.out.println("Create table ACCOUNT successfully");
        stmt.executeUpdate(sql2);
        System.out.println("Create table POSITION successfully");
        stmt.executeUpdate(sql3);
        System.out.println("Create table ORDER successfully");
    }

    public void populateAccount(String accountID, String balance) throws SQLException {
        c = DriverManager
                .getConnection(url, user, password);
        c.setAutoCommit(false);
        stmt = c.createStatement();
        String sql = "INSERT INTO ACCOUNT (ID, BALANCE) VALUES (\'" + accountID +
                "\', " + balance + ");";
        stmt.executeUpdate(sql);
        System.out.println("insert elem into table ACCOUNT successfully");
        stmt.close();
        c.commit();
        c.close();
    }

    public void populatePosition(String Symbol, String accountID, String amt) throws SQLException {
        c = DriverManager
                .getConnection(url, user, password);
        c.setAutoCommit(false);
        stmt = c.createStatement();
        String sql = "INSERT INTO POSITION (SYMBOL, AMOUNT, ACCOUNT_ID) VALUES (\'" + Symbol +
                "\', " + amt + ", \'"+ accountID +"\'" + ");";
        stmt.executeUpdate(sql);
        System.out.println("insert elem into table POSITION successfully");
        stmt.close();
        c.commit();
        c.close();
    }

    /**
     * insert transactions
     * @param accountId
     * @param symbol
     * @param amount
     * @param limit
     * @throws SQLException
     */
    public void populateOrder(String accountId, String symbol, String amount, String limit) throws SQLException {
        c = DriverManager.getConnection(url, user, password);
        c.setAutoCommit(false);
        stmt = c.createStatement();
        String sql = "INSERT INTO ORDERS (TIME, SYMBOL, AMOUNT, ACCOUNT_ID, PRICE) " +
                "VALUES (CURRENT_TIMESTAMP, \'" + symbol + "\', " + amount + ", \'" +
                accountId + "\', " + limit + ");";
        stmt.executeUpdate(sql);
        System.out.println("insert elem into table ORDERS successfully");
        stmt.close();
        c.commit();
        c.close();
    }

    public void checkMatchOrders() throws SQLException {

    }

    public void queryTransaction(String trans_id) throws SQLException {
        c = DriverManager.getConnection(url, user, password);
        c.setAutoCommit(false);
        stmt = c.createStatement();
        String sql = "SELECT TRANSACTION_ID, STATUS, AMOUNT, TIME, PRICE FROM ORDERS " +
                "WHERE TRANSACTION_ID=" + trans_id + ";";
        ResultSet rs = stmt.executeQuery(sql);

        rs.close();
        stmt.close();
        c.close();
    }

    public void cancelTransaction(String trans_id) throws SQLException {
        c = DriverManager.getConnection(url, user, password);
        c.setAutoCommit(false);
        stmt = c.createStatement();
        String selectSql = "SELECT TRANSACTION_ID, STATUS, AMOUNT, TIME, PRICE FROM ORDERS " +
                "WHERE TRANSACTION_ID=" + trans_id + " AND STATUS=OPEN;";
        ResultSet rs = stmt.executeQuery(selectSql);
        if (rs.next()) {
            //invalid cancellation: no open portion of this transaction_id
        }
        String updateSql = "UPDATE ORDERS SET STATE = CANCELED " +
                "WHERE TRANSACTION_ID=" + trans_id + ";";
        stmt.executeUpdate(updateSql);
        c.commit();
        stmt.close();
        c.close();
    }
}
