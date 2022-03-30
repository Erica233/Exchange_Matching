package ece568.awsome_exchange_matching;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.ResultSet;
import java.util.ArrayList;

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

    public void populateOrder(String accountId, String symbol, String amount, String limit) throws SQLException {
        c = DriverManager.getConnection(url, user, password);
        c.setAutoCommit(false);
        stmt = c.createStatement();

        // get next transaction id
        String getSql = "SELECT TRANSACTION_ID FROM ORDERS ORDER BY -ID LIMIT 1;";
        ResultSet rs_id = stmt.executeQuery(getSql);
        int new_id = rs_id.getInt("TRANSACTION_ID") + 1;

        // first check if any matched orders
        double amount_double = Double.parseDouble(amount);
        double limit_double = Double.parseDouble(limit);
        String checkSql;
        //sell
        if (amount_double < 0) {
            checkSql = "SELECT * FROM ORDERS " +
                    "WHERE SYMBOL='" + symbol + "' AND PRICE<=" + limit_double +
                    " ORDER BY PRICE AND TIME;";
        } else {
            //buy
            checkSql = "SELECT * FROM ORDERS " +
                    "WHERE SYMBOL='" + symbol + "' AND PRICE>=" + limit_double +
                    " ORDER BY -PRICE AND TIME;";
        }
        ResultSet rs = stmt.executeQuery(checkSql);
        // if has matched orders, find the matched orders and execute
        while (rs.next() && amount_double != 0) {
            int id = rs.getInt("ID");
            int transaction_id = rs.getInt("transaction_id");
            double curr_amount = rs.getDouble("amount");
            double curr_limit = rs.getDouble("price");
            if (-amount_double >= curr_amount) {
                // executed portion, and update original
                String executeSql = "UPDATE ORDERS SET STATE=EXECUTED WHERE ID=" + id + ";";
                stmt.executeUpdate(executeSql);
                String insertSql = "INSERT INTO ORDERS (TIME, SYMBOL, AMOUNT, ACCOUNT_ID, PRICE, STATUS) " +
                        "VALUES (CURRENT_TIMESTAMP, '" + symbol + "', " + (-curr_amount) + ", '" +
                        accountId + "', " + curr_limit + ", EXECUTED);";
                stmt.executeUpdate(insertSql);
                amount_double += curr_amount;
                // update account and position

            } else {
                String executeSql = "UPDATE ORDERS SET AMOUNT=" + (curr_amount + amount_double) +
                        " WHERE ID=" + id + ";";
                stmt.executeUpdate(executeSql);
                String insertSql = "INSERT INTO ORDERS (TIME, SYMBOL, AMOUNT, ACCOUNT_ID, PRICE, STATUS) " +
                        "VALUES (CURRENT_TIMESTAMP, '" + symbol + "', " + (-amount_double) + ", '" +
                        accountId + "', " + curr_limit + ", EXECUTED);";
                stmt.executeUpdate(insertSql);
                String insertSql2 = "INSERT INTO ORDERS (TRANSACTION_ID, TIME, SYMBOL, AMOUNT, ACCOUNT_ID, PRICE, STATUS) " +
                        "VALUES (" + new_id + ", CURRENT_TIMESTAMP, '" + symbol + "', " + amount_double + ", '" +
                        accountId + "', " + curr_limit + ", EXECUTED);";
                stmt.executeUpdate(insertSql2);
                amount_double = 0;
                // update account and position

            }
        }
        // if left any unmatched portion, insert a new order
        if (amount_double != 0) {
            String sql = "INSERT INTO ORDERS (TRANSACTION_ID, TIME, SYMBOL, AMOUNT, ACCOUNT_ID, PRICE) " +
                    "VALUES (" + new_id + ", CURRENT_TIMESTAMP, '" + symbol + "', " + amount_double + ", '" +
                    accountId + "', " + limit + ");";
            stmt.executeUpdate(sql);
        }
        rs.close();
        rs_id.close();
        stmt.close();
        c.commit();
        c.close();
    }

    public void checkMatchOrders() throws SQLException {

    }

    public ArrayList<Transaction> queryTransaction(String trans_id) throws SQLException {
        c = DriverManager.getConnection(url, user, password);
        c.setAutoCommit(false);
        stmt = c.createStatement();
        String sql = "SELECT TRANSACTION_ID, STATUS, AMOUNT, TIME, PRICE FROM ORDERS " +
                "WHERE TRANSACTION_ID=" + trans_id + ";";
        ResultSet rs = stmt.executeQuery(sql);
        ArrayList<Transaction> outputs = new ArrayList<Transaction>();
        while (rs.next()) {
            Transaction tran = new Transaction(rs.getInt("TRANSACTION_ID"),
                    rs.getString("STATUS"), rs.getDouble("AMOUNT"),
                    rs.getTimestamp("TIME"), rs.getDouble("PRICE"));
            outputs.add(tran);
        }
        rs.close();
        stmt.close();
        c.close();
        return outputs;
    }

    public void cancelTransaction(String trans_id) throws SQLException, IllegalArgumentException {
        c = DriverManager.getConnection(url, user, password);
        c.setAutoCommit(false);
        stmt = c.createStatement();
        //check if the cancellation operation is valid
        String selectSql = "SELECT ACCOUNT.ID, ACCOUNT.BALANCE, POSITION.AMOUNT, " +
                "ORDERS.AMOUNT, ORDERS.SYMBOL, ORDERS.AMOUNT, ORDERS.PRICE FROM ACCOUNT, POSITION, ORDERS " +
                "WHERE TRANSACTION_ID=" + trans_id + " AND STATUS=OPEN " +
                "AND ACCOUNT.ID=ORDERS.ACCOUNT_ID AND ACCOUNT.ID=POSITION.ACCOUNT_ID;";
        ResultSet rs = stmt.executeQuery(selectSql);
        String accountId = rs.getString("ACCOUNT.ID");
        double balance = rs.getDouble("ACCOUNT.BALANCE");
        double positionAmount = rs.getDouble("POSITION.AMOUNT");
        double ordersAmount = rs.getDouble("ORDERS.AMOUNT");
        String symbol = rs.getString("ORDERS.SYMBOL");
        double price = rs.getDouble("ORDERS.PRICE");
        if (!rs.next()) {
            //invalid cancellation: no open portion of this transaction_id
            throw new IllegalArgumentException("invalid cancellation: no open portion of this transaction_id");
        } else {
            // in table 'orders': change status
            String updateOrdersSql = "UPDATE ORDERS SET STATE = CANCELED " +
                    "WHERE TRANSACTION_ID=" + trans_id + ";";
            stmt.executeUpdate(updateOrdersSql);

            // in table 'account' and 'position':
            if (ordersAmount < 0) {
                // if cancel a sell order: give back seller's shares
                String updatePositionSql = "UPDATE POSITION SET AMOUNT = " + (positionAmount + ordersAmount) +
                        "WHERE SYMBOL='" + symbol + "' AND ACCOUNT_ID='"+ accountId +"';";
                stmt.executeUpdate(updatePositionSql);
            } else {
                // if cancel a buy order: refunds buyer's account
                String updateAccountSql = "UPDATE ACCOUNT SET BALANCE= "+ (balance + ordersAmount * price) +
                        "WHERE ID='" + accountId + "';";
                stmt.executeUpdate(updateAccountSql);
            }
        }
        c.commit();
        stmt.close();
        c.close();
    }
}
