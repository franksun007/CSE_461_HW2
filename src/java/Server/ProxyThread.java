package Server;

import Utils.Utilities;

import java.io.DataInput;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.PrintStream;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Scanner;

/**
 * Created by chenfs on 11/8/15.
 */
public class ProxyThread extends Thread {

    private Socket socket;
    public static final int DEFAULT_PACKET_SIZE = 1024;
    public static final PrintStream OUTPUT = Proxy.OUTPUT;

    public ProxyThread(Socket socket) {
        this.socket = socket;
    }

    @Override
    public void run() {
        try {
            byte[] data = new byte[DEFAULT_PACKET_SIZE];
            DataInputStream fromClient = new DataInputStream(socket.getInputStream());

            StringBuilder request = new StringBuilder();

            int actualLength = fromClient.read(data);
            request.append(new String(data, "ascii"));

            while (fromClient.available() > 0) {
                actualLength = fromClient.read(data);
                request.append(new String(data, "ascii"));
            }

            List<String> content = new ArrayList<String>(Arrays.asList(request.toString().split("\n")));

            OUTPUT.println(Utilities.getCurrentTime() + " - >>> " + content.get(0));

        } catch (Exception e) {
            closeSocket();
            OUTPUT.println("Unexpected exception");
            e.printStackTrace();
            this.interrupt();
        }
    }

    private void closeSocket() {
        try {
            socket.close();
        } catch (Exception e) {
            OUTPUT.println("Socket closure failed:");
            OUTPUT.println(socket);
            e.printStackTrace();
        }
    }
}
