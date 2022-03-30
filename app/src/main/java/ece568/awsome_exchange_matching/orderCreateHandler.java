package ece568.awsome_exchange_matching;

import org.w3c.dom.Element;
import org.w3c.dom.Node;

public class orderCreateHandler implements PostgresHandler{
    protected PostgreSQLJDBC postgreJDBC = null;
    Node n;
    protected String accountID;
    private String sym_name;
    private String amount;
    private String limit;
    private String transID;
    public orderCreateHandler(Node _n, String _accountID){
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

    public String getSym_name() {
        return sym_name;
    }

    public String getAmount(){
        return amount;
    }

    public String getLimit(){
        return limit;
    }

    public String getTransID(){
        return transID;
    }
    @Override
    public String reader(Node n) {
        String output = null;
        if (n.getNodeType() == Node.ELEMENT_NODE) {
            try {
                Element order = (Element) n;
                sym_name = order.getAttribute("sym");
                amount = order.getAttribute("amount");
                limit = order.getAttribute("limit");
                System.out.println("SYM: " + sym_name + ", AMT: " + amount + ", LMT：" + limit);
                return output;
            }catch(Exception e){
                output = e.getMessage();
            }finally{
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
            postgreJDBC.populateOrder(accountID,
                    sym_name,
                    amount,
                    limit);
        }catch(Exception e){
            output = e.getMessage();
        }finally{
            return output;
        }
    }
}
