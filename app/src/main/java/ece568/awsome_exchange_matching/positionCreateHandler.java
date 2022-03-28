package ece568.awsome_exchange_matching;

import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import java.sql.Array;
import java.util.ArrayList;
import java.util.concurrent.locks.ReentrantReadWriteLock;

public class positionCreateHandler extends accountCreateHandler{
    private String sym_name;
    private double amount;
    private static final ReentrantReadWriteLock lock = new ReentrantReadWriteLock(true);

    public positionCreateHandler(Node _n, String _sym_name) {
        super(_n);
        sym_name = _sym_name;
    }

    public String getSym_name() {
        return sym_name;
    }

    @Override
    public String reader(Node n) {
        String output = null;
        if (n.getNodeType() == Node.ELEMENT_NODE){
            try {
                Element account = (Element) n;
                String ID = account.getAttribute("id");
                accountID = Integer.parseInt(ID);
                String NUM = account.getTextContent();
                amount = Double.parseDouble(NUM);
                System.out.println("Account ID: " + ID + ", NUM: " + NUM);
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
        //populatePosition(String Symbol, String accountID, String amt)
        String output = null;
        try {
            lock.writeLock().lock();
            postgreJDBC.populatePosition(sym_name,
                    Integer.toString(accountID),
                    Double.toString(amount));
            lock.writeLock().unlock();
        }catch(Exception e){
            output = e.getMessage();
        }finally{
            return output;
        }
    }
}
