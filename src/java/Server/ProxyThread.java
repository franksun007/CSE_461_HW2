package Server;

import Utils.Utilities;
import com.sun.security.ntlm.Server;

import java.io.*;
import java.net.Socket;
import java.io.OutputStream;
import java.io.InputStream;
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
        if (socket != null) {
            this.socket = socket;
        } else {
            OUTPUT.println("Socket is null");
        }
    }

    @Override
    public void run() {
        if (socket == null) {
            this.interrupt();
            return;
        }

        try {
            BufferedReader readIn = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            String firstLine = readIn.readLine();
            System.out.println(firstLine);

            if (firstLine.toLowerCase().startsWith("connect")) {

            } else {
                // Non connect branch
                nonConnect(readIn, firstLine);
            }

        } catch (Exception e) {
            closeSocket();
            OUTPUT.println("Unexpected exception");
            e.printStackTrace();
            this.interrupt();
        }
    }

    private void nonConnect(BufferedReader readIn, String firstLine) {
        String requestLine = firstLine;
        StringBuilder fullRequest = new StringBuilder();

        if (firstLine.toUpperCase().contains("HTTP/1.1")) {
            requestLine = firstLine.replaceAll("HTTP/1.1", "HTTP/1.0").replaceAll("http/1.1", "http/1.0");
        }
        fullRequest.append(requestLine + "\r\n");
        try {
            int port = 0;
            String serverAddr = "";
            requestLine = readIn.readLine();
            while (requestLine != null) {
                if (requestLine.toLowerCase().startsWith("host")) {
                    port = getPort(firstLine, requestLine);
                    serverAddr = requestLine.substring(6);
                } else if (requestLine.toLowerCase().startsWith("connection:")) {
                    requestLine = "Connection: close";
                } else if (requestLine.toLowerCase().startsWith("proxy-connection:")) {
                    requestLine = "Proxy-connection: close";
                } else if (requestLine.equals("")) {
                    break;
                }

                fullRequest.append(requestLine + "\r\n");
                requestLine = readIn.readLine();
            }
            //System.out.println("request is " + fullRequest.toString() + "end of request");
            System.out.println("server is " + serverAddr);
            System.out.println("port is " + port);
            OutputStream out = socket.getOutputStream();
            sendToServer(port, serverAddr, fullRequest.toString(), out);
        } catch (Exception e) {
                e.printStackTrace();
                return;
        }
    }

    private void sendToServer(int port, String serverAddr, String fullRequest, OutputStream out) {
        Socket socket2Server;
        InputStream fromServer;
        PrintWriter writeToServer;

        try {
            socket2Server = new Socket(serverAddr, port);
            fromServer = socket2Server.getInputStream();
            writeToServer = new PrintWriter(socket2Server.getOutputStream());
            writeToServer.write(fullRequest);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private int getPort(String firstLine, String hostLine) {
        int port = 80;
        if (firstLine.toLowerCase().contains("https")) {
            port = 443;
        }
        return port;
    }

    private void closeSocket() {
        try {
            this.socket.close();
        } catch (Exception e) {
            OUTPUT.println("Socket closure failed:");
            OUTPUT.println(socket);
            e.printStackTrace();
        }
    }
}
