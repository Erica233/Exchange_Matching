package postgresHandler;

import org.w3c.dom.Node;

public class orderCancleHandler extends queryHandler{
    public orderCancleHandler(Node _n, String _accountID){
        super(_n, _accountID);
    }

    @Override
    public String implementPSQL() {
        String output = null;
        try {
            //transactions = postgreJDBC.cancelTransaction(this.transID);
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
