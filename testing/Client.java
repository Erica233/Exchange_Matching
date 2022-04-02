package client;

import java.io.*;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.Scanner;

public class Client {
    // driver code
    public static void main(String[] args) throws IOException {
        // establish a connection by providing host and port
        // number
        int counter = 0;
        long start2 = System.currentTimeMillis();
        while (counter < 500) {
            ClientRunnable clientRunnable = new ClientRunnable();
            try {
                clientRunnable.id = counter;
                //vcm-24065.vm.duke.edu
                //vcm-26335.vm.duke.edu
                clientRunnable.socket = new Socket("vcm-26335.vm.duke.edu", 12345);
                new Thread(clientRunnable).start();
            }catch(IOException e){
                System.out.println("Thread-" + counter + ": cannot connect!");
            }
            counter++;
        }
        long end2 = System.currentTimeMillis();
        System.out.println("Elapsed Time of thread "+ counter+" in milli seconds: "+ (end2-start2));
    }
}

class ClientRunnable implements Runnable{
    public int id;
    public Socket socket;
    @Override
    public void run() {
        try {
            File inputFile;
            if (id <=1){
                inputFile = new File("scripts/input2-1.txt");
            }
            else {
                inputFile = new File("scripts/input2.txt");
            }
            // writing to server
            PrintWriter out = new PrintWriter(
                    socket.getOutputStream(), true);

            // reading from server
            BufferedReader in
                    = new BufferedReader(new InputStreamReader(
                    socket.getInputStream()));

            // object of scanner class
            Scanner myReader = new Scanner(inputFile);
            String line = null;
            // sending the user input to server
            while (myReader.hasNextLine()) {
                line= myReader.nextLine();
                out.println(line);
                out.flush();
            }
            //FileWriter myWriter = new FileWriter("scripts/output.txt");
            StringBuilder sb = new StringBuilder();
            String temp = in.readLine();
            while(temp!= null) {
                // displaying server reply
                sb.append(temp + "\n");
                temp = in.readLine();
                if (temp.contains("</results>")){
                    sb.append(temp);
                    break;
                }
                /*
                try {
                    myWriter.write(in.readLine());
                } catch (IOException e) {
                    System.out.println("An error occurred.");
                    e.printStackTrace();
                }
                if (in.readLine()!= null && in.readLine().equals("</results>")){
                    break;
                }

                 */
            }
            String str = sb.toString();
            //System.out.println("Therad-"+ id + "\n" + str);
            // closing the scanner object
            myReader.close();
            in.close();
            //myWriter.close();
            socket.close();
        }
        catch (UnknownHostException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}


