package Server;


import java.io.*;
import java.net.Socket;
import java.io.OutputStream;
import java.io.InputStream;

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

            if (firstLine != null && firstLine.contains("http")) {
                if (firstLine.toLowerCase().startsWith("connect")) {

                } else {
                    // Non connect branch
                    nonConnect(readIn, firstLine);
                }
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

        try {
            int port = 0;
            String host = "";
            requestLine = readIn.readLine();
            while (requestLine != null) {
                String temp = requestLine.toLowerCase().trim();
                if (temp.startsWith("host")) {
                    port = getPort(firstLine, requestLine);
                    host = requestLine.substring(6);
                    System.out.println("Host in nonConnect is " + host);
                } else if (temp.startsWith("connection:")) {
                    requestLine = "Connection: close";
                } else if (temp.startsWith("proxy-connection:")) {
                    requestLine = "Proxy-connection: close";
                } else if (temp.contains("http/1.1")) {
                    requestLine = firstLine.replaceAll("HTTP/1.1", "HTTP/1.0").replaceAll("http/1.1", "http/1.0");
                } else if (requestLine.equals("")) {
                    break;
                }

                fullRequest.append(requestLine + "\r\n");
                requestLine = readIn.readLine();
            }
            System.out.println("First line is " + firstLine);
            System.out.println("host is " + host);
            System.out.println("port is " + port);
            OutputStream out = socket.getOutputStream();
            sendToServer(port, host, fullRequest.toString(), out);
        } catch (Exception e) {
                e.printStackTrace();
                return;
        }
    }

    private void sendToServer(int port, String host, String fullRequest, OutputStream out) {
        Socket socket2Server;
        InputStream fromServer;
        PrintWriter writeToServer;
        System.out.println("request is \n" + fullRequest);
        System.out.println("Sending to host..........");
        try {
            socket2Server = new Socket(host, port);
            fromServer = new BufferedInputStream(socket2Server.getInputStream());

            writeToServer = new PrintWriter(socket2Server.getOutputStream());
            writeToServer.write(fullRequest);
            writeToServer.flush();

            System.out.println("Reading from host..........");
            byte[] buffer = new byte[1024 * 6];
            int res = fromServer.read(buffer);
            System.out.println("read is " + res);

            while (res > 0 || res != -1) {
                System.out.println("Writing to client......");
                out.write(buffer, 0, res);
                out.flush();

                res = fromServer.read(buffer);
            }
            System.out.println("Done writing");
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
