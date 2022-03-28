package ece568.awsome_exchange_matching;

import org.w3c.dom.Element;
import org.w3c.dom.Node;

import java.util.concurrent.locks.ReentrantReadWriteLock;

public class accountCreateHandler implements PostgresHandler{
    protected PostgreSQLJDBC postgreJDBC = null;
    Node n;
    private int accountID;
    private double balance;
    private static final ReentrantReadWriteLock lock = new ReentrantReadWriteLock(true);

    public accountCreateHandler(Node _n){
        n = _n;
        try {
            postgreJDBC = postgreJDBC.getInstance("postgres", "postgres",
                    "jdbc:postgresql://localhost:4444/postgres");
        } catch (Exception e) {
            e.printStackTrace();
            System.err.println(e.getClass().getName() + ": " + e.getMessage());
            System.exit(0);
        }
    }

    @Override
    public String reader(Node node) {
        String output = null;
        if (node.getNodeType() == Node.ELEMENT_NODE) {
            try {
                Element account = (Element) node;
                this.accountID = Integer.parseInt(account.getAttribute("id"));
                this.balance = Double.parseDouble(account.getAttribute("balance"));
                System.out.println("id: " + accountID + ", balance: " + balance);
            }catch(Exception e){
                output = e.getMessage();
            }finally{
                return output;
            }
        }
        output = "Incorrect Node Type";
        return output;
    }

    @Override
    public String implementPSQL() {
        String output = null;
        try {
            lock.writeLock().lock();
            postgreJDBC.populateAccount(Integer.toString(accountID),
                    Double.toString(balance));
            lock.writeLock().unlock();
        }catch(Exception e){
            output = e.getMessage();
        }finally{
            return output;
        }
    }

}
