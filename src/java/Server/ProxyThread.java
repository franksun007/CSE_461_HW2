package Server;

import java.io.*;
import java.net.Socket;
import java.net.URL;
import java.util.Scanner;
import java.util.concurrent.RecursiveAction;
import Utils.*;

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
        assert (socket != null);
        try {
            BufferedReader readIn = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            String firstLine = readIn.readLine();
            if (firstLine == null) {
                return;
            }

            OUTPUT.println(Utilities.getCurrentTime() + " - >>> "
                    + firstLine.substring(0, firstLine.indexOf("HTTP/1.1")));

            String requestLine = firstLine;
            StringBuilder fullRequest = new StringBuilder();
            String host = null;
            int port = 80;

            if (firstLine.contains("HTTP/1.1")) {
                requestLine = requestLine.replaceAll("HTTP/1.1", "HTTP/1.0");
            }
            if (firstLine.contains("connect")) {
                port = 443;
            }
            fullRequest.append(requestLine + "\r\n");

            requestLine = readIn.readLine();
            while (requestLine != null) {
                if (requestLine.toLowerCase().contains("host")) {
                    String[] contentSplit = requestLine.split(" ")[1].split(":[0-9]+");
                    host = contentSplit[0];
                    try {
                        port = Integer.parseInt(contentSplit[1]);
                    } catch (Exception e) {
                        // do nothing?
                    }
                } else if (requestLine.toLowerCase().contains("proxy-connection: keep-alive")) {
                    requestLine = requestLine.replaceAll("keep-alive", "close");
                } else if (requestLine.toLowerCase().contains("connection: keep-alive")) {
                    requestLine = requestLine.replaceAll("keep-alive", "close");
                } else if (requestLine.equals("") || requestLine.equals("\r\n")) {
                    break;
                }
                fullRequest.append(requestLine + "\r\n");
                requestLine = readIn.readLine();
            }
            fullRequest.append("\r\n");

            if (port == 443) {
                // connect(host, port, fullRequest);
            } else {
                // Non connect branch
                nonConnect(host, port, fullRequest.toString());
            }

        } catch (Exception e) {
            closeSocket(this.socket);
            OUTPUT.println("Unexpected exception");
            e.printStackTrace();
        }
    }
    /*
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

            System.out.println(fullRequest);
            assert(host != null);

            DataInputStream clientToProxy = new DataInputStream(socket.getInputStream());
            DataOutputStream proxyToClient = new DataOutputStream(socket.getOutputStream());
            proxyToClient.write("HTTP/1.0 200 OK\r\n\r\n".getBytes("ascii"));

            Socket proxySocket = null;
            try {
                proxySocket = new Socket(host, port);
            } catch (Exception pse) {
                socket.getOutputStream().write("HTTP/1.1 502 BAD GATEWAY\r\n\r\n".getBytes("ascii"));
                closeSocket();
                return;
            }

            DataOutputStream proxyToServer = new DataOutputStream(proxySocket.getOutputStream());
            DataInputStream serverToProxy = new DataInputStream(proxySocket.getInputStream());

//            proxyToServer.write(fullRequest.toString().getBytes("ascii"));
            System.out.println(proxySocket.isClosed());

            byte[] data = new byte[DEFAULT_PACKET_SIZE];


            while (true) {  // Need to be changed
                String response = "";
                while (clientToProxy.read(data) > 0) {
                    response += new String(data, "ascii");
                    proxyToServer.write(data);
                }
                System.out.println(response);

//                response = "";
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
            OUTPUT.println("Unexpected Exception: " + e.getMessage());

            closeSocket();
            e.printStackTrace();
        }
    }*/

    private void nonConnect(String host, int port, String fullRequest) {
        try {


            Socket proxySocket = new Socket(host, port);
            InputStream fromServerToProxy = proxySocket.getInputStream();

            DataOutputStream fromProxyToServer = new DataOutputStream(proxySocket.getOutputStream());

            DataInputStream fromClientToProxy = new DataInputStream(this.socket.getInputStream());


            DataOutputStream fromProxyToClient = new DataOutputStream(this.socket.getOutputStream());

            fromProxyToServer.write(fullRequest.getBytes("ascii"));
            fromProxyToServer.flush();

            byte[] data = new byte[DEFAULT_PACKET_SIZE];
            StringBuilder dataTrans = new StringBuilder();


//            fromServerToProxy.read(data);
//            dataTrans.append(new String(data, "ascii"));
//            System.out.println(dataTrans.toString());

            /*
            while (!proxySocket.isClosed()) {
                dataTrans.setLength(0);
                while (fromClientToProxy.available() > 0) {
                    dataTrans.append(new String(data, "ascii"));
                }
                if (dataTrans.length() > 0) {
                    fromProxyToServer.write(dataTrans.toString().getBytes("ascii"));
                    fromProxyToServer.flush();
                }
             */
//                System.out.println(dataTrans.toString());

//            while (fromServerToProxy.read(data) > 0) {
            int read = fromServerToProxy.read(data);
            while (read != -1) {
//                System.out.println(new String(data, "ascii"));
//                fromProxyToClient.write(new String(data, "ascii").getBytes("ascii"));
//                fromServerToPr
// oxy.read(data);
                fromProxyToClient.write(data, 0, read);
                fromProxyToClient.flush();
                read = fromServerToProxy.read(data);
            }


            closeSocket(this.socket);
            closeSocket(proxySocket);

            //}

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void closeSocket(Socket socket) {
        try {
            socket.close();
        } catch (Exception e) {
            OUTPUT.println("Socket closure failed:");
            OUTPUT.println(socket);
            e.printStackTrace();
        }
    }
}
