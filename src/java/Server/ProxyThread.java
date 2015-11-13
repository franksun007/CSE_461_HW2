package Server;

import Utils.Utilities;

import java.io.DataInput;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.PrintStream;
import java.net.Socket;
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

            fromClient.read(data);
            request.append(new String(data, "ascii"));

            while (fromClient.available() > 0) {
                fromClient.read(data);
                request.append(new String(data, "ascii"));
            }

            String[] content = request.toString().split("\r?\n");

            OUTPUT.println(Utilities.getCurrentTime() + " - >>> "
                    + content[0].substring(0, content[0].indexOf("HTTP/1.1")));

            String host = "";
            int port = -1;

            for (int i = 0; i < content.length; i++) {
                if (content[i].toLowerCase().contains("host")) {
                    host = content[i].substring(content[i].indexOf(':') + 2);
                    int indexOfColon = host.indexOf(':');
                    port = indexOfColon != -1 ? Integer.parseInt(host.substring(indexOfColon + 1)) : 80;
                    host = indexOfColon != -1 ? host.substring(0, indexOfColon) : host;
                }
                if (content[i].contains("HTTP/1.1")) {
                    content[i] = content[i].substring(0, content[i].indexOf("HTTP/1.1")) + "HTTP/1.0";
                }
                if (content[i].contains("Connection: keep-alive")) {
                    content[i] = "Connection: close";
                }
            }

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

            DataOutputStream toServerProxy = new DataOutputStream(talkToServer.getOutputStream());
            toServerProxy.write(reqh.array(), 0, reqh.array().length);
            toServerProxy.flush();


            DataInputStream toClientProxy = new DataInputStream(talkToServer.getInputStream());
            data = new byte[DEFAULT_PACKET_SIZE];
            toClientProxy.read(data);


            DataOutputStream toClient = new DataOutputStream(socket.getOutputStream());
            toClient.write(data);
            System.out.println(new String(data, "ascii"));
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
