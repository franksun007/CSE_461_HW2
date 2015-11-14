package Server;

import Utils.Utilities;

import java.io.*;
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
        int port = 0;
        if (firstLine.contains("HTTP/1.1")) {
            requestLine = firstLine.replaceAll("HTTP/1.1", "HTTP/1.0");
        }
        fullRequest.append(requestLine);
        try {
            requestLine = readIn.readLine();
            while (requestLine != null) {
                if (requestLine.toLowerCase().startsWith("host")) {
                    Scanner scan = new Scanner(firstLine);
                    scan.next();
                    String url = scan.next();
                    URL aUrl = new URL(url);
                    String serverAddr = requestLine.substring(6).trim();
                    int portIdx = serverAddr.indexOf(":");
                    if (portIdx > 0) {
                        port = Integer.parseInt(serverAddr.substring(portIdx + 1));
                    } else {
                        if (aUrl.getPort() != -1) {
                            port = aUrl.getPort();
                        } else {
                            if (firstLine.toLowerCase().contains("https://")) {
                                port = 443;
                            } else {
                                port = 80;
                            }
                        }
                    }
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
            System.out.println("request is " + fullRequest.toString() + "end of request");

            sendToServer(port, serverAddr);
        } catch (Exception e) {
                e.printStackTrace();
                return;
        }
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
