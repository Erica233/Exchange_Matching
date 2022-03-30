package postgresHandler;

import ece568.awsome_exchange_matching.PostgreSQLJDBC;
import ece568.awsome_exchange_matching.Transaction;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

import java.util.ArrayList;

public class queryHandler implements PostgresHandler{
    protected PostgreSQLJDBC postgreJDBC = null;
    protected Node n;
    protected String transID;
    protected String accountID;
    public ArrayList<Transaction> transactions;

    public queryHandler(Node _n, String _accountID){
        try {
            postgreJDBC = postgreJDBC.getInstance("postgres", "postgres",
                    "jdbc:postgresql://localhost:4444/postgres");
            //jdbc:postgresql://database:5432/postgres
        } catch (Exception e) {
            e.printStackTrace();
            System.err.println(e.getClass().getName() + ": " + e.getMessage());
            System.exit(0);
        }
        n = _n;
        accountID = _accountID;
    }

    @Override
    public String reader(Node n) {
        String output = null;
        if (n.getNodeType() == Node.ELEMENT_NODE) {
            try {
                if (n.getNodeType() == Node.ELEMENT_NODE) {
                    Element query = (Element) n;
                    transID = query.getAttribute("id");
                    System.out.println("TransID: " + transID);
                }
            } catch (Exception e) {
                output = e.getMessage();
            } finally {
                return output;
            }
        }
        else{
            output = "Incorrect Node Type";
            return output;
        }
    }

    @Override
    public String implementPSQL() {
        String output = null;
        try {
            transactions = postgreJDBC.queryTransaction(transID);
        }catch(Exception e){
            output = e.getMessage();
        }finally{
            return output;
        }
    }

    public String getTransID() {
        return transID;
    }

}
