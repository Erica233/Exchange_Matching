package ece568.awsome_exchange_matching;
// A Java program for a Server

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;

public class Server {
    private ServerSocket    server;
    int portNum;

    // constructor with port
    public Server(int port) throws IOException {
        // starts server and waits for a connection
        this.portNum = port;
        this.server = new ServerSocket(this.portNum);
        System.out.println("Server started");
    }
    /**
     * Accept connection from a client node
     */
    public Socket acceptClient() throws IOException {
        // socket object to receive incoming client
        // requests
        System.out.println("Waiting for a client ...");
        Socket client = server.accept();
        return client;
    }

    /**
     * close server socket
     */
    public void closeServer(){
        if (server != null) {
            try {
                server.close();
            }
            catch (IOException e) {
                e.printStackTrace();
            }
        }
    }


}

