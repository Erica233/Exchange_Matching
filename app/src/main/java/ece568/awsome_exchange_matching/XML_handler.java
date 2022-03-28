package ece568.awsome_exchange_matching;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.w3c.dom.traversal.DocumentTraversal;
import org.w3c.dom.traversal.NodeFilter;
import org.w3c.dom.traversal.TreeWalker;
import org.xml.sax.SAXException;

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
    private DocumentBuilder create_builder;
    private Document create_doc;
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
            create_builder = factory.newDocumentBuilder();
            //create document
            create_doc = create_builder.newDocument();
            //create root node
            Element results = create_doc.createElement("results");
            create_doc.appendChild(results);

            if (doc.getDocumentElement().getNodeName() == "create") {
                parseCreate(doc, bufferedWriter, results);
            }
            else if (doc.getDocumentElement().getNodeName() == "transactions"){
                parseTransaction(doc, bufferedWriter);
            }
            //generate XML file
            StringWriter writer = new StringWriter();
            StreamResult result = new StreamResult(writer);
            TransformerFactory tff = TransformerFactory.newInstance();
            Transformer tf = tff.newTransformer();
            //set change line
            tf.setOutputProperty(OutputKeys.INDENT, "yes");
            tf.transform(new DOMSource(create_doc), result);
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
                    readAccount(n, bufferedWriter, results);
                    break;
                case "symbol":
                    readSys(n, bufferedWriter, results);
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
    public void parseTransaction(Document doc, BufferedWriter bufferedWriter){
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
                    readOrder(n);
                    break;
                case "query":
                    readQuery(n);
                    break;
                case "cancel":
                    readCancel(n);
                    break;
                default:
                    throw new IllegalArgumentException("Invalid node name: " + n.getNodeName());
            }
        }
    }
    //readers:
    public void readAccount(Node p, BufferedWriter bufferedWriter, Element results ) throws IOException {
        accountCreateHandler myHandler = new accountCreateHandler(p);
        String isReadValid = myHandler.reader(p);
        if (isReadValid == null){
            String isImplementSuccess = myHandler.implementPSQL();
            if (isImplementSuccess == null){
                //TODO: display on success
                Element create_account = create_doc.createElement("created");
                create_account.setAttribute("id", myHandler.getAccountID());
                results.appendChild(create_account);
                return;
            }
            //TODO: display on failure

            Element create_account_fail = create_doc.createElement("error");
            create_account_fail.setAttribute("id", myHandler.getAccountID());
            String msg = "fail to insert element into table account: " +isImplementSuccess;
            create_account_fail.setTextContent(msg);
            results.appendChild(create_account_fail);
        } else{
            //TODO: display on failure
            Element create_account_fail = create_doc.createElement("error");
            create_account_fail.setAttribute("id", myHandler.getAccountID());
            String msg = "fail to insert element into table account: " +isReadValid;
            create_account_fail.setTextContent(msg);
            results.appendChild(create_account_fail);
        }
        return;
    }

    public void readSys(Node p, BufferedWriter bufferedWriter, Element results ) throws IOException {
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
                                Element create_position = create_doc.createElement("created");
                                create_position.setAttribute("sym", myHandler.getSym_name());
                                create_position.setAttribute("id", myHandler.getAccountID());
                                results.appendChild(create_position);
                            } else {
                                //TODO: display on failure
                                Element create_position_fail = create_doc.createElement("error");
                                create_position_fail.setAttribute("sym", myHandler.getSym_name());
                                create_position_fail.setAttribute("id", myHandler.getAccountID());
                                String msg = "fail to insert element into table position: " + isImplementSuccess;
                                create_position_fail.setTextContent(msg);
                                results.appendChild(create_position_fail);
                            }
                        } else {
                            //TODO: display on failure
                            Element create_position_fail = create_doc.createElement("error");
                            create_position_fail.setAttribute("sym", myHandler.getSym_name());
                            create_position_fail.setAttribute("id", myHandler.getAccountID());
                            String msg = "fail to insert element into table position: " + isReadValid;
                            create_position_fail.setTextContent(msg);
                            results.appendChild(create_position_fail);
                        }
                    }
                }
            }
        }
    }

    public void readOrder(Node p){
        if (p.getNodeType() == Node.ELEMENT_NODE) {
            Element order = (Element) p;
            String SYM = order.getAttribute("sym");
            String AMT = order.getAttribute("amount");
            String LMT = order.getAttribute("limit");
            System.out.println("SYM: "+ SYM + ", AMT: " + AMT + ", LMT：" + LMT);
        }
    }

    public void readQuery(Node p){
        if (p.getNodeType() == Node.ELEMENT_NODE) {
            Element query = (Element) p;
            String transID = query.getAttribute("id");
            System.out.println("TransID: " + transID);
        }
    }

    public void readCancel(Node p){
        if (p.getNodeType() == Node.ELEMENT_NODE) {
            Element cancel = (Element) p;
            String transID = cancel.getAttribute("id");
            System.out.println("TransID: " + transID);
        }
    }

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
                if (isMatch_transaction || isMatch_create){
                    break;
                }
                line++;
            }

            System.out.printf(" Sent from the client: %s\n",
                    content);
            bufferedWriter.write(content + "\n");
            bufferedWriter.flush();
            String xml = filterBlankXMl(content);
            inStream = new ByteArrayInputStream(
                    xml.getBytes(StandardCharsets.UTF_8));
            getXML(inStream, bufferedWriter );
            inStream.close();

            /*
            while ((line = in.readLine()) != null) {
                // writing the received message from
                // client
                System.out.printf(" Sent from the client: %s\n",
                        line);
                out.println(line);
            }
            */
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
