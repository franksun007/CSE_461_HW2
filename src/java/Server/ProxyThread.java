package Server;

import Utils.Utilities;

import java.io.DataInput;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.PrintStream;
import java.net.Socket;
import java.net.URL;
import java.nio.ByteBuffer;
import java.nio.charset.Charset;
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

            String[] content = request.toString().split("\r?\n");

            OUTPUT.println(Utilities.getCurrentTime() + " - >>> "
                    + content[0].substring(0, content[0].indexOf("HTTP/1.1")));

            String host = "";

            String url = content[0].split("[ \t]+")[1];
            String[] urlComponent = url.split(":");
            int port = -1;

            if (urlComponent[0].equals("http")) {
                port = 80;
            } else {
                port = 443;
            }

            System.out.println(host + ":" + port);

            System.out.println(host);
            System.out.println(host);
            System.out.println(host);

            System.out.println(port);

            request.setLength(0);
            for (int i = 0; i < content.length; i++) {
                request.append(content[i]).append("\r\n");
            }
            ByteBuffer reqh = ByteBuffer.allocate(request.length());
            reqh.put(request.toString().getBytes(Charset.forName("ascii")));

            Socket talkToServer = new Socket(host, port);

            DataOutputStream toServer = new DataOutputStream(talkToServer.getOutputStream());
            toServer.write(reqh.array(), 0, reqh.array().length);
            toServer.flush();

            System.out.println("Check point");

            DataInputStream toClient = new DataInputStream(talkToServer.getInputStream());
            data = new byte[DEFAULT_PACKET_SIZE];
<<<<<<< HEAD
            DataOutputStream toClient = new DataOutputStream(socket.getOutputStream());
//            toClientProxy.read(data);
//            toClient.write(data);
//            System.out.println(new String(data, "ascii"));

            while (toClientProxy.read(data) > 0) {
                toClient.write(data);
            }


=======
            toClient.read(data);

            System.out.println(new String(data, "ascii"));
>>>>>>> parent of 40b4e7b... what the fuck is happending
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
