package ece568.awsome_exchange_matching;

import org.w3c.dom.Element;
import org.w3c.dom.Node;

import java.util.ArrayList;

public class orderCancleHandler extends queryHandler{
    public orderCancleHandler(Node _n){
        super(_n);
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
