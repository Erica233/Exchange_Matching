package ece568.awsome_exchange_matching;

import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import java.util.ArrayList;
import java.util.concurrent.locks.ReentrantReadWriteLock;

public class positionCreateHandler extends accountCreateHandler{
    private String sym_name;
    private ArrayList<Integer> accountIDs;
    private ArrayList<Double> amounts;
    private static final ReentrantReadWriteLock lock = new ReentrantReadWriteLock(true);

    public positionCreateHandler(Node _n) {
        super(_n);
        accountIDs = new ArrayList<>();
        amounts = new ArrayList<>();
    }

    @Override
    public String reader(Node n) {
        String output = null;
        if (n.getNodeType() == Node.ELEMENT_NODE) {
            Element sym = (Element) n;
            sym_name = sym.getAttribute("sym");
            System.out.print("sym: "+sym_name + ", ");
            NodeList Nums = sym.getChildNodes();
            if (Nums != null && Nums.getLength() > 0){
                for (int j = 0; j < Nums.getLength(); j++){
                    try {
                        Node a = Nums.item(j);
                        if (a.getNodeType() == Node.ELEMENT_NODE){
                                Element account = (Element) a;
                                String ID = account.getAttribute("id");
                                accountIDs.add(Integer.parseInt(ID));
                                String NUM = account.getTextContent();
                                amounts.add(Double.parseDouble(NUM));
                                System.out.println("Account ID: " + ID + ", NUM: " + NUM);
                        }
                    }catch(Exception e){
                        output = e.getMessage();
                        break;
                    }
                }
                return output;
            }
            output = "Empty Account";
            return output;
        }
        output = "Incorrect Node Type";
        return output;
    }

    @Override
    public String implementPSQL() {
        //populatePosition(String Symbol, String accountID, String amt)
        String output = null;
        for(int i = 0; i < accountIDs.size(); i++) {
            try {
                lock.writeLock().lock();
                postgreJDBC.populatePosition(sym_name,
                        Integer.toString(accountIDs.get(i)),
                        Double.toString(amounts.get(i)));
                lock.writeLock().unlock();
            } catch (Exception e) {
                output = e.getMessage();
                break;
            }
        }
        return output;
    }
}
