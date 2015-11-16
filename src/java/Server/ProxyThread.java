package Server;

import java.io.*;
import java.net.InetSocketAddress;
import java.net.ProxySelector;
import java.net.Socket;
import java.net.URL;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.Iterator;
import java.util.Scanner;
import java.util.Set;
import java.util.concurrent.RecursiveAction;
import Utils.*;

/**
 * Created by chenfs on 11/8/15.
 */
public class ProxyThread extends Thread {

    private Socket socket;
    private int serverPort;
    public static final int DEFAULT_PACKET_SIZE = 1024;
    public static final PrintStream OUTPUT = Proxy.OUTPUT;

    public ProxyThread(Socket socket, int serverPort) {
        if (socket != null) {
            this.socket = socket;
            this.serverPort = serverPort;
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
            if (firstLine.toLowerCase().contains("connect")) {
                port = 443;
            }

            fullRequest.append(requestLine + "\r\n");

            requestLine = readIn.readLine();
            while (requestLine != null) {
                if (requestLine.toLowerCase().contains("host")) {
                    String[] contentSplit = requestLine.split(" ")[1].split(":[0-9]+");
                    host = contentSplit[0];
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
                connect(host, port);
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

    private void connect(String host, int port) {


        Socket proxySocket = null;
        SocketChannel proxyChannel;
        ServerSocketChannel clientChannel;
        Selector selector;
        boolean isConnected = false;
        String msg502 = "HTTP/1.0 502 BAD GATEWAY\r\n\r\n";


        try {
            clientChannel = ServerSocketChannel.open();
            clientChannel = clientChannel.bind(new InetSocketAddress(serverPort));

            proxyChannel = SocketChannel.open();
            isConnected = proxyChannel.connect(new InetSocketAddress(host, port));
            if (!isConnected) {

            }

            selector = Selector.open();

        } catch (Exception e) {
            clientChannel.write(
        }

            assert (proxySocket != null);

            // Send 200 message to client
            String msg = "HTTP/1.0 200 OK\r\n\r\n";
            fromProxyToClient.write(msg.getBytes("ascii"), 0, msg.length());
            fromProxyToClient.flush();


            proxyChannel.configureBlocking(false);
            clientChannel.configureBlocking(false);

            int proxyOps = proxyChannel.validOps();
            int clientOps = clientChannel.validOps();
            SelectionKey selectKeyProxy = proxyChannel.register(selector, proxyOps);
            SelectionKey selectKeyClient = clientChannel.register(selector, SelectionKey.OP_READ);


            assert (proxyChannel.isConnected() && clientChannel.isConnected());
            // Start tunneling
            while (true) {
                int readyChannel = selector.select();
                if (readyChannel == 0) continue;
                Set<SelectionKey> selectedKeys = selector.selectedKeys();
                Iterator<SelectionKey> iter = selectedKeys.iterator();

                while (iter.hasNext()) {
                    SelectionKey key = iter.next();

                    if (key.isReadable()) {
                        SocketChannel readFromChannel = (SocketChannel) key.channel();
                        ByteBuffer buffer = ByteBuffer.allocate(DEFAULT_PACKET_SIZE);
                        SocketChannel writeToChannel;
                        if (readFromChannel.equals(proxyChannel)) {
                            // Reading from proxy and write to client
                            writeToChannel = clientChannel;
                        } else {
                            // Reading from client and write to proxy
                            writeToChannel = proxyChannel;
                        }
                        int readCount = readFromChannel.read(buffer);
                        System.out.println("readcount is " + readCount);
                        while (readCount > 0 || readCount != -1) {
                            //System.out.println("Writing: " + new String(buffer.array(), "ASCII"));
                            writeToChannel.write(buffer);
                            readCount = readFromChannel.read(buffer);
                        }
                    } else if (key.isWritable()) {
                        SocketChannel writeChannel = (SocketChannel) key.channel();
                        ByteBuffer buffer = ByteBuffer.allocate(DEFAULT_PACKET_SIZE);
                        SocketChannel readChannel;
                        if (writeChannel.equals(proxyChannel)) {
                            // Reading from client and write to proxy
                            readChannel = clientChannel;
                            System.out.println("proxy writable");
                        } else {
                            // Reading from proxy and write to client
                            readChannel = proxyChannel;
                            System.out.println("client writable");
                        }
                        int readCount = readChannel.read(buffer);
                        System.out.println("readCount + " + readCount);
                        while (readCount > 0 || readCount != -1) {
                            //System.out.println("Writing: " + new String(buffer.array(), "ASCII"));
                            writeChannel.write(buffer);
                            readCount = readChannel.read(buffer);
                        }
                    }

                    iter.remove();
                }
            }


        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void nonConnect(String host, int port, String fullRequest) {
        try {


            Socket proxySocket = new Socket(host, port);
            InputStream fromServerToProxy = proxySocket.getInputStream();

            DataOutputStream fromProxyToServer = new DataOutputStream(proxySocket.getOutputStream());

            DataOutputStream fromProxyToClient = new DataOutputStream(this.socket.getOutputStream());

            fromProxyToServer.write(fullRequest.getBytes("ascii"));
            fromProxyToServer.flush();

            byte[] data = new byte[DEFAULT_PACKET_SIZE];

            int read = fromServerToProxy.read(data);
            while (read != -1) {
                fromProxyToClient.write(data, 0, read);
                fromProxyToClient.flush();
                read = fromServerToProxy.read(data);
            }


            closeSocket(this.socket);
            closeSocket(proxySocket);

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
