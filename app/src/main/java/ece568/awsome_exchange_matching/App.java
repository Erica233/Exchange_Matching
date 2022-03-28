/*
 * This Java source file was generated by the Gradle 'init' task.
 */
package ece568.awsome_exchange_matching;

import java.io.IOException;
import java.net.Socket;

public class App {
    public static void main(String[] args) {

        //TCP socket connection
        Server myServer;
        try {
            myServer = new Server(12345);//12345
            // running infinite loop for getting
            // client request
            try {
                Socket client;
                while (true) {
                    client = myServer.acceptClient();
                    System.out.println("New client connected"
                            + client.getInetAddress()
                            .getHostAddress());
                    XML_handler clientSock
                            = new XML_handler(client);
                    // This thread will handle the client
                    // separately
                    new Thread(clientSock).start();
                }
            } catch (IOException e) {
                e.printStackTrace();
            } finally {
                myServer.closeServer();
            }
        } catch (Exception i) {
            System.err.println(i.getClass().getName() + ": " + i.getMessage());

        }

    }
}
