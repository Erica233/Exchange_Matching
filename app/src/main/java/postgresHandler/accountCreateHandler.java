package postgresHandler;

import ece568.awsome_exchange_matching.PostgreSQLJDBC;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

public class accountCreateHandler implements PostgresHandler{
    protected PostgreSQLJDBC postgreJDBC = null;
    Node n;
    protected String accountID;
    private String balance;

    public accountCreateHandler(Node _n){
        n = _n;
        try {
            postgreJDBC = postgreJDBC.getInstance("postgres", "postgres",
                    "jdbc:postgresql://localhost:4444/postgres");
            //jdbc:postgresql://database:5432/postgres
        } catch (Exception e) {
            e.printStackTrace();
            System.err.println(e.getClass().getName() + ": " + e.getMessage());
            System.exit(0);
        }
    }

    public String getAccountID() {
        return accountID;
    }

    @Override
    public String reader(Node node) {
        String output = null;
        if (node.getNodeType() == Node.ELEMENT_NODE) {
            try {
                Element account = (Element) node;
                this.accountID = account.getAttribute("id");
                this.balance = account.getAttribute("balance");
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
            postgreJDBC.populateAccount(accountID,
                    balance);
        }catch(Exception e){
            output = e.getMessage();
        }finally{
            return output;
        }
    }
}
