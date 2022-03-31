package ece568.awsome_exchange_matching;

import java.sql.*;
import java.util.ArrayList;
import java.util.concurrent.locks.ReentrantReadWriteLock;

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
        ResultSet rs = stmt.executeQuery( "SELECT * FROM POSITION WHERE" +
                " SYMBOL = \'"+ Symbol +"\' AND ACCOUNT_ID = \'" + accountID+"\';" );
        if (!rs.next()) {
            String sql = "INSERT INTO POSITION (SYMBOL, AMOUNT, ACCOUNT_ID) VALUES (\'" + Symbol +
                    "\', " + amt + ", \'" + accountID + "\'" + ");";
            stmt.executeUpdate(sql);
            System.out.println("insert elem into table POSITION successfully");
        }
        //if rs is not empty
        else{
            String sql = "UPDATE POSITION SET AMOUNT = AMOUNT +" + amt +
                    "WHERE" +
                    " SYMBOL = \'"+ Symbol +"\' AND ACCOUNT_ID = \'" + accountID+"\';";
            stmt.executeUpdate(sql);
            System.out.println("update table POSITION successfully");
        }
        stmt.close();
        c.commit();
        c.close();
    }

    public void handleSell(int new_id, String accountId, String symbol, double amount_double, double limit_double) throws SQLException {
        // place order: deduct shares from seller's position
        String positionSql = "SELECT AMOUNT FROM POSITION " +
                "WHERE ACCOUNT_ID='" + accountId + "' AND SYMBOL='" + symbol + "';";
        ResultSet position_rs = stmt.executeQuery(positionSql);
        if ((position_rs.getDouble("AMOUNT") + amount_double) < 0) {
            throw new IllegalArgumentException("Invalid transaction: insufficient amount of shares!");
        } else {
            String sql = "UPDATE POSITION SET AMOUNT=" + (position_rs.getDouble("AMOUNT") + amount_double) +
                    " WHERE ID=" + accountId + ";";
            stmt.executeUpdate(sql);
            c.commit();
        }

        // execute order: match orders and credit seller's account
        String checkSql = "SELECT * FROM ORDERS " +
                "WHERE SYMBOL='" + symbol + "' AND PRICE>=" + limit_double +
                " ORDER BY -PRICE AND TIME;";
        ResultSet rs = stmt.executeQuery(checkSql);
        // if has matched orders, find the matched orders and execute one by one
        while (rs.next() && amount_double != 0) {
            int id = rs.getInt("ID");
            int curr_tran_id = rs.getInt("transaction_id");
            double curr_amount = rs.getDouble("amount");
            double curr_limit = rs.getDouble("price");
            int buyer_acc_id = rs.getInt("account_id");
            double matched_amount = 0;
            if (-amount_double >= curr_amount) {
                matched_amount = curr_amount;
                // update buyer side to executed
                String executeSql = "UPDATE ORDERS SET STATE=EXECUTED WHERE ID=" + id + ";";
                stmt.executeUpdate(executeSql);
                c.commit();
                // insert seller side - executed
                String insertSql = "INSERT INTO ORDERS (TRANSACTION_ID, TIME, SYMBOL, AMOUNT, ACCOUNT_ID, PRICE, STATUS) " +
                        "VALUES (" + new_id + ", CURRENT_TIMESTAMP, '" + symbol + "', " + (-curr_amount) + ", '" +
                        accountId + "', " + curr_limit + ", EXECUTED);";
                stmt.executeUpdate(insertSql);
                c.commit();
                amount_double += curr_amount;
            } else {
                matched_amount = amount_double;
                // update amount of buyer side
                String executeSql = "UPDATE ORDERS SET AMOUNT=" + (curr_amount + amount_double) +
                        " WHERE ID=" + id + ";";
                stmt.executeUpdate(executeSql);
                c.commit();
                // insert buyer side - executed
                String insertSql = "INSERT INTO ORDERS (TRANSACTION_ID, TIME, SYMBOL, AMOUNT, ACCOUNT_ID, PRICE, STATUS) " +
                        "VALUES (" + curr_tran_id + ", CURRENT_TIMESTAMP, '" + symbol + "', " + (-amount_double) + ", '" +
                        buyer_acc_id + "', " + curr_limit + ", EXECUTED);";
                stmt.executeUpdate(insertSql);
                c.commit();

                // insert seller side - executed
                String insertSql2 = "INSERT INTO ORDERS (TRANSACTION_ID, TIME, SYMBOL, AMOUNT, ACCOUNT_ID, PRICE, STATUS) " +
                        "VALUES (" + new_id + ", CURRENT_TIMESTAMP, '" + symbol + "', " + amount_double + ", '" +
                        accountId + "', " + curr_limit + ", EXECUTED);";
                stmt.executeUpdate(insertSql2);
                c.commit();
                amount_double = 0;
            }
            // update account: add seller's balance
            String updateSellerSql = "UPDATE ACCOUNT SET BALANCE=" + matched_amount * curr_limit +
                    " WHERE ID=" + accountId + ";";
            stmt.executeUpdate(updateSellerSql);
            c.commit();
            // update position: add buyer's amount
            String updateBuyerSql = "UPDATE POSITION SET AMOUNT=" + matched_amount +
                    " WHERE ACCOUNT_ID=" + buyer_acc_id + ";";
            stmt.executeUpdate(updateBuyerSql);
            c.commit();
        }
        // if left any unmatched portion, insert a new order
        if (amount_double != 0) {
            String sql = "INSERT INTO ORDERS (TRANSACTION_ID, TIME, SYMBOL, AMOUNT, ACCOUNT_ID, PRICE) " +
                    "VALUES (" + new_id + ", CURRENT_TIMESTAMP, '" + symbol + "', " + amount_double + ", '" +
                    accountId + "', " + limit_double + ");";
            stmt.executeUpdate(sql);
        }
    }

    public void handleBuy(int new_id, String accountId, String symbol, double amount_double, double limit_double) throws SQLException {
        // place order: deduct balance from buyer's account
        String accountSql = "SELECT BALANCE FROM ACCOUNT WHERE ID=" + accountId + ";";
        ResultSet account_rs = stmt.executeQuery(accountSql);
        if ((account_rs.getDouble("BALANCE") - amount_double * limit_double) < 0) {
            throw new IllegalArgumentException("Invalid transaction: insufficient funds!");
        } else {
            String sql = "UPDATE ACCOUNT SET BALANCE=" + (account_rs.getDouble("BALANCE") - amount_double * limit_double) +
                    " WHERE ID=" + accountId + ";";
            stmt.executeUpdate(sql);
            c.commit();
        }

        // execute order: match orders and credit seller's account
        String checkSql = "SELECT * FROM ORDERS " +
                "WHERE SYMBOL='" + symbol + "' AND PRICE<=" + limit_double +
                " ORDER BY PRICE AND TIME;";
        ResultSet rs = stmt.executeQuery(checkSql);
        // if has matched orders, find the matched orders and execute one by one
        while (rs.next() && amount_double != 0) {
            int id = rs.getInt("ID");
            int curr_tran_id = rs.getInt("transaction_id");
            double curr_amount = rs.getDouble("amount");
            double curr_limit = rs.getDouble("price");
            int curr_acc_id = rs.getInt("account_id");
            double matched_amount = 0;
            if (amount_double >= -curr_amount) {
                matched_amount = -curr_amount;
                // update seller side to executed
                String executeSql = "UPDATE ORDERS SET STATE=EXECUTED WHERE ID=" + id + ";";
                stmt.executeUpdate(executeSql);
                c.commit();
                // insert buyer side - executed
                String insertSql = "INSERT INTO ORDERS (TRANSACTION_ID, TIME, SYMBOL, AMOUNT, ACCOUNT_ID, PRICE, STATUS) " +
                        "VALUES (" + new_id + ", CURRENT_TIMESTAMP, '" + symbol + "', " + (-curr_amount) + ", '" +
                        accountId + "', " + curr_limit + ", EXECUTED);";
                stmt.executeUpdate(insertSql);
                c.commit();
                amount_double += curr_amount;
            } else {
                matched_amount = amount_double;
                // update amount of existed side
                String executeSql = "UPDATE ORDERS SET AMOUNT=" + (curr_amount + amount_double) +
                        " WHERE ID=" + id + ";";
                stmt.executeUpdate(executeSql);
                c.commit();
                // insert existed side - executed
                String insertSql = "INSERT INTO ORDERS (TRANSACTION_ID, TIME, SYMBOL, AMOUNT, ACCOUNT_ID, PRICE, STATUS) " +
                        "VALUES (" + curr_tran_id + ", CURRENT_TIMESTAMP, '" + symbol + "', " + (-amount_double) + ", '" +
                        curr_acc_id + "', " + curr_limit + ", EXECUTED);";
                stmt.executeUpdate(insertSql);
                c.commit();

                // insert populated side - executed
                String insertSql2 = "INSERT INTO ORDERS (TRANSACTION_ID, TIME, SYMBOL, AMOUNT, ACCOUNT_ID, PRICE, STATUS) " +
                        "VALUES (" + new_id + ", CURRENT_TIMESTAMP, '" + symbol + "', " + amount_double + ", '" +
                        accountId + "', " + curr_limit + ", EXECUTED);";
                stmt.executeUpdate(insertSql2);
                c.commit();
                amount_double = 0;
            }
            // update account: add seller's balance
            String updateSellerSql = "UPDATE ACCOUNT SET BALANCE=" + matched_amount * curr_limit +
                    " WHERE ID=" + curr_acc_id + ";";
            stmt.executeUpdate(updateSellerSql);
            c.commit();

            // update account: credit buyer's balance
            String updateCreditSql = "UPDATE ACCOUNT SET BALANCE=" + matched_amount * (curr_limit - limit_double) +
                    " WHERE ID=" + curr_acc_id + ";";
            stmt.executeUpdate(updateCreditSql);
            c.commit();
            // update position: add buyer's amount
            String updateBuyerSql = "UPDATE POSITION SET AMOUNT=" + matched_amount +
                    " WHERE ACCOUNT_ID=" + accountId + ";";
            stmt.executeUpdate(updateBuyerSql);
            c.commit();
        }
        // if left any unmatched portion, insert a new order
        if (amount_double != 0) {
            String sql = "INSERT INTO ORDERS (TRANSACTION_ID, TIME, SYMBOL, AMOUNT, ACCOUNT_ID, PRICE) " +
                    "VALUES (" + new_id + ", CURRENT_TIMESTAMP, '" + symbol + "', " + amount_double + ", '" +
                    accountId + "', " + limit_double + ");";
            stmt.executeUpdate(sql);
        }
    }

    public void matchOrders() {

    }

    public ArrayList<Transaction> populateOrder(String accountId, String symbol, String amount, String limit) throws SQLException {
        c = DriverManager.getConnection(url, user, password);
        c.setAutoCommit(false);
        stmt = c.createStatement();
        ArrayList<Transaction> outputs = new ArrayList<Transaction>();

        // get next transaction id
        String getSql = "SELECT TRANSACTION_ID FROM ORDERS ORDER BY -ID LIMIT 1;";
        ResultSet rs_id = stmt.executeQuery(getSql);
        int new_id = rs_id.getInt("TRANSACTION_ID") + 1;

        // check if the order is valid and if any matched orders
        double amount_double = Double.parseDouble(amount);
        double limit_double = Double.parseDouble(limit);
        if (amount_double < 0) {
            handleSell(new_id, accountId, symbol, amount_double, limit_double);
        } else {
            handleBuy(new_id, accountId, symbol, amount_double, limit_double);
        }
        outputs.add(new Transaction(symbol, amount_double, limit_double, new_id));
        rs_id.close();
        stmt.close();
        c.commit();
        c.close();
        return outputs;
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
            outputs.add(new Transaction(rs.getInt("TRANSACTION_ID"),
                    rs.getString("STATUS"), rs.getDouble("AMOUNT"),
                    rs.getTimestamp("TIME"), rs.getDouble("PRICE")));
        }
        rs.close();
        stmt.close();
        c.close();
        return outputs;
    }

    public ArrayList<Transaction> cancelTransaction(String trans_id) throws SQLException, IllegalArgumentException {
        c = DriverManager.getConnection(url, user, password);
        c.setAutoCommit(false);
        stmt = c.createStatement();
        ArrayList<Transaction> outputs = new ArrayList<Transaction>();
        String selectSql = "SELECT * FROM ACCOUNT, POSITION, ORDERS " +
                "WHERE TRANSACTION_ID=" + trans_id + " AND ACCOUNT.ID=ORDERS.ACCOUNT_ID " +
                "AND ACCOUNT.ID=POSITION.ACCOUNT_ID ORDER BY -STATUS;";
        ResultSet rs = stmt.executeQuery(selectSql);
        // check if the cancellation operation is valid
        if (rs == null) {
            throw new IllegalArgumentException("invalid cancellation: transaction_id is not existed!");
        }
        while (rs.next()) {
            int transaction_id = rs.getInt("ORDERS.TRANSACTION_ID");
            String status = rs.getString("ORDERS.STATUS");
            int id = rs.getInt("ORDERS.ID");
            String accountId = rs.getString("ACCOUNT.ID");
            double balance = rs.getDouble("ACCOUNT.BALANCE");
            double positionAmount = rs.getDouble("POSITION.AMOUNT");
            double ordersAmount = rs.getDouble("ORDERS.AMOUNT");
            String symbol = rs.getString("ORDERS.SYMBOL");
            double price = rs.getDouble("ORDERS.PRICE");
            Timestamp time = rs.getTimestamp("ORDERS.TIME");

            if (rs.isFirst()) {
                if (status != "OPEN") {
                    throw new IllegalArgumentException("invalid cancellation: no open orders in this transaction id!");
                }
                outputs.add(new Transaction(transaction_id, "CANCELED", ordersAmount, time, price));
                // in table 'orders': change status
                String updateOrdersSql = "UPDATE ORDERS SET STATE = CANCELED WHERE ID=" + id + ";";
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
            } else {
                outputs.add(new Transaction(transaction_id, status, ordersAmount, time, price));
            }
        }
        c.commit();
        stmt.close();
        c.close();
        return outputs;
    }


}
