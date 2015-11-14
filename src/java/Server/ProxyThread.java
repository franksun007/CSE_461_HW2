package Server;

import java.io.*;
import java.net.Socket;
import java.net.URL;
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
                connect(readIn, firstLine);
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

    private void connect(BufferedReader reader, String firstLine) {

        String requestLine = firstLine;
        StringBuilder fullRequest = new StringBuilder();
        String host = null;
        int port = -1;

        try {
            if (firstLine.contains("HTTP/1.1")) {
                requestLine = requestLine.replaceAll("HTTP/1.1", "HTTP/1.0");
            }
            fullRequest.append(requestLine);

            requestLine = reader.readLine();
            while (requestLine != null) {
                if (requestLine.toLowerCase().contains("host")) {
                    String[] contentSplit = requestLine.split(" ")[1].split(":[0-9]+");
                    host = contentSplit[0];
                    try {
                        port = Integer.parseInt(contentSplit[1]);
                    } catch (Exception e) {
                        port = 443;
                    }
                    System.out.println(host);
                } else if (requestLine.toLowerCase().contains("proxy-connection: keep-alive")) {
                    requestLine = requestLine.replaceAll("keep-alive", "close");
                } else if (requestLine.toLowerCase().contains("connection: keep-alive")) {
                    requestLine = requestLine.replaceAll("keep-alive", "close");
                } else if (requestLine.equals("")) {
                    break;
                }
                fullRequest.append(requestLine + "\r\n");
                requestLine = reader.readLine();
            }
            fullRequest.append("\r\n");

            assert(host != null);
            Socket proxySocket = new Socket(host, port);

            InputStream clientToProxy = socket.getInputStream();
            OutputStream proxyToClient = socket.getOutputStream();
            proxyToClient.write("HTTP/1.1 200 OK\r\n\r\n".getBytes("ascii"));

            OutputStream proxyToServer = proxySocket.getOutputStream();
            InputStream serverToProxy = proxySocket.getInputStream();

            proxyToServer.write(fullRequest.toString().getBytes("ascii"));
            System.out.println(proxySocket.isClosed());

            byte[] data = new byte[DEFAULT_PACKET_SIZE];

            String response = "";

            while (clientToProxy.read(data) > 0) {
                response += new String(data, "ascii");
                proxyToClient.write(data);
            }
            System.out.println(response);

            if (!proxySocket.isClosed()) {
                response = "";
                while (serverToProxy.read(data) > 0) {
                    response += new String(data, "ascii");
                    proxyToClient.write(data);
                }
                System.out.println(response);
            }



//            String line = reader.readLine();
//            while (line != null) {
//                System.out.println(line);
//                line = reader.readLine();
//            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void nonConnect(BufferedReader readIn, String firstLine) {
        String requestLine = firstLine;
        StringBuilder fullRequest = new StringBuilder();
        int port = 0;
        if (firstLine.contains("HTTP/1.1")) {
            requestLine = firstLine.replaceAll("HTTP/1.1", "HTTP/1.0");
        }
        fullRequest.append(requestLine + "\r\n");
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

            // sendToServer(port, serverAddr);
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
