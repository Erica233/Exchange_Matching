package ece568.awsome_exchange_matching;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.w3c.dom.traversal.DocumentTraversal;
import org.w3c.dom.traversal.NodeFilter;
import org.w3c.dom.traversal.TreeWalker;
import org.xml.sax.SAXException;
import postgresHandler.*;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.*;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import java.io.*;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class XML_handler implements Runnable{
    private final Socket clientSocket;
    public DocumentBuilderFactory factory;
    private DocumentBuilder result_builder;
    private Document result_doc;
    public XML_handler(Socket c) {
        clientSocket = c;
        factory = DocumentBuilderFactory.newInstance();
    }

    public void getXML(InputStream is, BufferedWriter bufferedWriter ){
        try{
            DocumentBuilder builder = factory.newDocumentBuilder();
            Document doc = builder.parse(is); //"scripts/testXML.xml"
            doc.getDocumentElement().normalize();
            System.out.println("Root element:" + doc.getDocumentElement().getNodeName());
            //create result XML
            //create documentBuilder
            result_builder = factory.newDocumentBuilder();
            //create document
            result_doc = result_builder.newDocument();
            //create root node
            Element results = result_doc.createElement("results");
            result_doc.appendChild(results);

            if (doc.getDocumentElement().getNodeName() == "create") {
                parseCreate(doc, bufferedWriter, results);
            }
            else if (doc.getDocumentElement().getNodeName() == "transactions"){
                parseTransaction(doc, bufferedWriter, results);
            }
            //generate XML file
            StringWriter writer = new StringWriter();
            StreamResult result = new StreamResult(writer);
            TransformerFactory tff = TransformerFactory.newInstance();
            Transformer tf = tff.newTransformer();
            //set change line
            tf.setOutputProperty(OutputKeys.INDENT, "yes");
            tf.transform(new DOMSource(result_doc), result);
            //convert XML to string, send to client
            bufferedWriter.write(writer.toString());
            bufferedWriter.flush();

        }catch(ParserConfigurationException e){
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } catch (SAXException e) {
            e.printStackTrace();
        } catch (TransformerConfigurationException e) {
            e.printStackTrace();
        } catch (TransformerException e) {
            e.printStackTrace();
        }
    }

    /**
     * parse create XML
     * @param doc
     */
    public void parseCreate(Document doc, BufferedWriter bufferedWriter, Element results ) throws IOException, ParserConfigurationException {

        DocumentTraversal traversal = (DocumentTraversal) doc;
        TreeWalker walker = traversal.createTreeWalker(
                doc.getDocumentElement(),
                NodeFilter.SHOW_ELEMENT | NodeFilter.SHOW_TEXT, null, true);
        //traverse children
        for (Node n = walker.firstChild(); n != null; n = walker.nextSibling()){
            System.out.println(n.getNodeName());
            switch(n.getNodeName()){
                case "#text":
                    break;
                case "account":
                    readAccount(n, results);
                    break;
                case "symbol":
                    readSys(n, results);
                    break;
                default:
                    throw new IllegalArgumentException("Invalid node name: " + n.getNodeName());
            }
        }
    }

    /**
     * parse transaction XML
     * @param doc
     */
    public void parseTransaction(Document doc, BufferedWriter bufferedWriter, Element results){
        String id = doc.getDocumentElement().getAttribute("id");
        System.out.println("id: "+id);
        DocumentTraversal traversal = (DocumentTraversal) doc;

        TreeWalker walker = traversal.createTreeWalker(
                doc.getDocumentElement(),
                NodeFilter.SHOW_ELEMENT | NodeFilter.SHOW_TEXT, null, true);
        //traverse children
        for (Node n = walker.firstChild(); n != null; n = walker.nextSibling()){

            String name = n.getNodeName();
            System.out.println(name);
            switch(name){
                case "#text":
                    break;
                case "order":
                    readOrder(n, results, id);
                    break;
                case "query":
                    readQuery(n, results, id);
                    break;
                case "cancel":
                    readCancel(n, results, id);
                    break;
                default:
                    throw new IllegalArgumentException("Invalid node name: " + n.getNodeName());
            }
        }
    }
    //readers:

    /**
     * read XML node "create Account"
     * @param p
     * @param results
     */
    public void readAccount(Node p, Element results ) {
        accountCreateHandler myHandler = new accountCreateHandler(p);
        String isReadValid = myHandler.reader(p);
        if (isReadValid == null){
            String isImplementSuccess = myHandler.implementPSQL();
            if (isImplementSuccess == null){
                //TODO: display on success
                Element create_account = result_doc.createElement("created");
                create_account.setAttribute("id", myHandler.getAccountID());
                results.appendChild(create_account);
                return;
            }
            //TODO: display on failure
            handleCreateAccErr(myHandler, isImplementSuccess, results);

        } else{
            //TODO: display on failure
            handleCreateAccErr(myHandler, isReadValid, results);
        }
        return;
    }

    /**
     * read XML node "create Symbol"
     * @param p
     * @param results
     */
    public void readSys(Node p, Element results ){
        if (p.getNodeType() == Node.ELEMENT_NODE) {
            Element sym = (Element) p;
            String sym_name = sym.getAttribute("sym");
            System.out.print("sym: " + sym_name + ", ");
            NodeList Nums = sym.getChildNodes();
            if (Nums != null && Nums.getLength() > 0) {
                for (int j = 0; j < Nums.getLength(); j++) {
                    Node a = Nums.item(j);
                    if (a.getNodeName().equals("account")) {
                        positionCreateHandler myHandler = new positionCreateHandler(a, sym_name);
                        String isReadValid = myHandler.reader(a);
                        if (isReadValid == null) {
                            String isImplementSuccess = myHandler.implementPSQL();
                            if (isImplementSuccess == null) {
                                //TODO: display on success
                                Element create_position = result_doc.createElement("created");
                                create_position.setAttribute("sym", myHandler.getSym_name());
                                create_position.setAttribute("id", myHandler.getAccountID());
                                results.appendChild(create_position);
                            } else {
                                //TODO: display on failure
                                handleCreatePositErr(myHandler, isImplementSuccess, results);
                            }
                        } else {
                            //TODO: display on failure
                            handleCreatePositErr(myHandler, isReadValid, results);
                        }
                    }
                }
            }
        }
    }

    /**
     * read XML node "open order"
     * @param p
     * @param results
     * @param id
     */
    public void readOrder(Node p, Element results, String id){
        orderCreateHandler myHandler = new orderCreateHandler(p, id);
        String isReadValid = myHandler.reader(p);
        if (isReadValid == null){
            String isImplementSuccess = myHandler.implementPSQL();
            if (isImplementSuccess == null){
                //TODO: display on success
                Element open_order = result_doc.createElement("opened");
                open_order.setAttribute("sym", myHandler.getSym_name());
                open_order.setAttribute("amount", myHandler.getAmount());
                open_order.setAttribute("limit", myHandler.getLimit());
                open_order.setAttribute("id", myHandler.getTransID());
                results.appendChild(open_order);
                return;
            }
            //TODO: display on failure
            handleOpenOrderErr(myHandler, isImplementSuccess, results);

        } else{
            //TODO: display on failure
            handleOpenOrderErr(myHandler, isReadValid, results);
        }
        return;
    }

    /**
     * read XML node "query order"
     * @param p
     * @param results
     */
    public void readQuery(Node p, Element results, String accountID){
        queryHandler myHandler = new queryHandler(p, accountID);
        String isReadValid = myHandler.reader(p);
        if (isReadValid == null){
            String isImplementSuccess = myHandler.implementPSQL();
            if (isImplementSuccess == null){
                //TODO: display on success
                handleQueryOrderSuccess(myHandler, "status", results);
                return;
            }
            //TODO: display on failure
            handleQueryOrderErr(myHandler, isImplementSuccess, results);

        } else{
            //TODO: display on failure
            handleQueryOrderErr(myHandler, isReadValid, results);
        }
        return;
    }

    /**
     * read XML node "cancel order"
     * @param p
     * @param results
     */
    public void readCancel(Node p, Element results, String accountID){
        orderCancleHandler myHandler = new orderCancleHandler(p, accountID);
        String isReadValid = myHandler.reader(p);
        if (isReadValid == null){
            String isImplementSuccess = myHandler.implementPSQL();
            if (isImplementSuccess == null){
                //TODO: display on success
                handleQueryOrderSuccess(myHandler, "canceled", results);
                return;
            }
            //TODO: display on failure
            handleQueryOrderErr(myHandler, isImplementSuccess, results);

        } else{
            //TODO: display on failure
            handleQueryOrderErr(myHandler, isReadValid, results);
        }
        return;
    }

    /**
     * filter the input string into a well-formatted xml string
     * @param xml
     * @return
     */
    public static String filterBlankXMl(String xml) {
        //remove space and new lines
        Pattern p = Pattern.compile(">\\s{1,}|\t|\r|\n");
        Matcher m = p.matcher(xml);
        xml = m.replaceAll(">");
        //remove non-xml header
        Pattern p2 = Pattern.compile("^(.|s)*<\\?xml version=");
        Matcher m2 = p2.matcher(xml);
        xml = m2.replaceFirst("<\\?xml version=");
        //remove non-xml tail
        Pattern p3 = Pattern.compile("</transactions>(.|\\s)+$");
        Matcher m3 = p3.matcher(xml);
        xml = m3.replaceFirst("</transactions>");
        Pattern p4 = Pattern.compile("</create>(.|\\s)+$");
        Matcher m4 = p4.matcher(xml);
        xml = m4.replaceFirst("</create>");
        System.out.println("received XML:\n" + xml);
        return xml;
    }

    /**
     * helper functions
     * @param myHandler
     * @param err_msg
     * @param results
     */
    private void handleCreatePositErr(positionCreateHandler myHandler, String err_msg, Element results){
        Element create_position_fail = result_doc.createElement("error");
        create_position_fail.setAttribute("sym", myHandler.getSym_name());
        create_position_fail.setAttribute("id", myHandler.getAccountID());
        String msg = "fail to insert element into table position: " + err_msg;
        create_position_fail.setTextContent(msg);
        results.appendChild(create_position_fail);
    }

    private void handleCreateAccErr(accountCreateHandler myHandler, String err_msg, Element results){
        Element create_account_fail = result_doc.createElement("error");
        create_account_fail.setAttribute("id", myHandler.getAccountID());
        String msg = "fail to insert element into table position: " + err_msg;
        create_account_fail.setTextContent(msg);
        results.appendChild(create_account_fail);
    }

    private void handleOpenOrderErr(orderCreateHandler myHandler, String err_msg, Element results){
        Element open_order_fail = result_doc.createElement("error");
        open_order_fail.setAttribute("sym", myHandler.getSym_name());
        open_order_fail.setAttribute("amount", myHandler.getAmount());
        open_order_fail.setAttribute("limit", myHandler.getLimit());
        String msg = "fail to open order: " + err_msg;
        open_order_fail.setTextContent(msg);
        results.appendChild(open_order_fail);
    }

    private void handleQueryOrderErr(queryHandler myHandler, String err_msg, Element results){
        Element query_order_fail = result_doc.createElement("error");
        query_order_fail.setAttribute("id", myHandler.getTransID());
        String msg = "fail to query or cancel order: " + err_msg;
        query_order_fail.setTextContent(msg);
        results.appendChild(query_order_fail);
    }

    private void handleQueryOrderSuccess(queryHandler myHandler, String tag, Element results){
        Element query_order = result_doc.createElement(tag);
        query_order.setAttribute("id", myHandler.getTransID());
        for(int i = 0; i < myHandler.transactions.size(); i++){
            Transaction t = myHandler.transactions.get(i);
            switch(t.getStatus()){
                case "open":
                    Element open_order = result_doc.createElement("open");
                    open_order.setAttribute("shares", Double.toString(t.getAmount()));
                    query_order.appendChild(open_order);
                    break;
                case "canceled":
                    Element canceled_order = result_doc.createElement("canceled");
                    canceled_order.setAttribute("shares", Double.toString(t.getAmount()));
                    canceled_order.setAttribute("time", Long.toString(t.getTime()));
                    query_order.appendChild(canceled_order);
                    break;
                case "executed":
                    Element executed_order = result_doc.createElement("executed");
                    executed_order.setAttribute("shares", Double.toString(t.getAmount()));
                    executed_order.setAttribute("price", Double.toString(t.getPrice()));
                    executed_order.setAttribute("time", Long.toString(t.getTime()));
                    query_order.appendChild(executed_order);
                    break;
            }
        }
        results.appendChild(query_order);
        return;
    }
    ///////////////////////////////////////////////////////////////////////////////////////////

    /**
     * implement XMLhandler for each client node in separate thread
     */
    @Override
    public void run() {
        PrintWriter out = null;
        BufferedReader in = null;
        InputStream inStream;
        String content = "";
        try {
            // get the outputstream of client
            //out = new PrintWriter(clientSocket.getOutputStream(), true);
            while(true) {
                OutputStream outputStream = clientSocket.getOutputStream();
                OutputStreamWriter outputStreamWriter = new OutputStreamWriter(outputStream);
                BufferedWriter bufferedWriter = new BufferedWriter(outputStreamWriter);

                // get the inputstream of client
                in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
                String tempString = null;
                int line = 1;
                // read by line until null
                while ((tempString = in.readLine()) != null) {
                    // display line index
                    System.out.println("line " + line + ": " + tempString);
                    boolean isMatch_transaction = Pattern.matches("</transactions>(.|\\s)*$", tempString);
                    boolean isMatch_create = Pattern.matches("</create>(.|\\s)*$", tempString);
                    content = content + tempString;
                    if (isMatch_transaction || isMatch_create) {
                        break;
                    }
                    line++;
                }

                System.out.printf(" Sent from the client: %s\n",
                        content);

                String xml = filterBlankXMl(content);
                inStream = new ByteArrayInputStream(
                        xml.getBytes(StandardCharsets.UTF_8));
                getXML(inStream, bufferedWriter);
                inStream.close();
            }
        }
        catch (IOException e) {
            e.printStackTrace();
        }
        finally {
            //close socket
            try {
                if (out != null) {
                    out.close();
                }
                if (in != null) {
                    in.close();
                    clientSocket.close();
                }
            }
            catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}
