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

    private SocketChannel socket;
    public static final int DEFAULT_PACKET_SIZE = 1024;
    public static final PrintStream OUTPUT = Proxy.OUTPUT;

    public ProxyThread(SocketChannel socket) {
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


            BufferedReader readIn = new BufferedReader(new InputStreamReader(socket.socket().getInputStream()));
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
        SocketChannel browserChannel = this.socket;
        Selector selector;
        boolean isConnected = false;
        ByteBuffer buffer = ByteBuffer.allocate(DEFAULT_PACKET_SIZE);
        String msg502 = "HTTP/1.0 502 BAD GATEWAY\r\n\r\n";

        try {


            proxyChannel = SocketChannel.open();
            isConnected = proxyChannel.connect(new InetSocketAddress(host, port));

            System.out.println("Fuckt the shit");
           // buffer.put(msg502.getBytes());

            System.out.println(isConnected);

            if (!isConnected) {
                browserChannel.write(buffer);
            }


            (new DataOutputStream(socket.socket().getOutputStream())).write("HTTP/1.1 200 OK\r\n\r\n".getBytes("ascii"));
            browserChannel.configureBlocking(false);
            proxyChannel.configureBlocking(false);
            selector = Selector.open();
            SelectionKey selectKeyProxy = proxyChannel.register(selector, SelectionKey.OP_READ);
            SelectionKey selectKeyBrowser = browserChannel.register(selector, SelectionKey.OP_READ);    System.out.println("Fuckt the shit 200");





            System.out.println("Fuckt the shit");
            assert (proxySocket != null);


            // Send 200 message to client
    //        String msg200 = "200 OK\r\n\r\n";
            //browserChannel.write(buffer.put(msg200.getBytes("ascii"), 0, msg200.length()));
            //(new DataOutputStream(browserChannel.socket().getOutputStream())).write(msg200.getBytes("ascii"));
            //(new DataOutputStream(browserChannel.socket().getOutputStream())).flush();

          //  buffer.clear();
         //   buffer.put(msg200.getBytes(), 0, msg200.length());
         //   buffer.flip();
//
        //    while (buffer.hasRemaining()) {
         //       browserChannel.write(buffer);
         //   }

       //     System.out.println("Sleeping");

            assert (proxyChannel.isConnected() && browserChannel.isConnected());

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
                        SocketChannel writeToChannel;
                        if (readFromChannel.equals(proxyChannel)) {
                            // Reading from proxy and write to client
                            writeToChannel = browserChannel;
                        } else {
                            // Reading from client and write to proxy
                            writeToChannel = proxyChannel;
                        }
                        //buffer.clear();
                        int readCount = readFromChannel.read(buffer);
                        //System.out.println("readcount is " + readCount);
                        while (readCount > 0) {
                            String shit = writeToChannel == proxyChannel ? "proxyChannel" : "writeToChannel";
                            System.out.println(shit);
                            System.out.println("Writing: " + new String(buffer.array(), "ASCII"));
                            writeToChannel.write(buffer);
                            //buffer.clear();
                            readCount = readFromChannel.read(buffer);
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

            DataOutputStream fromProxyToClient = new DataOutputStream(this.socket.socket().getOutputStream());

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
            proxySocket.close();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void closeSocket(SocketChannel socket) {
        try {
            socket.close();
        } catch (Exception e) {
            OUTPUT.println("Socket closure failed:");
            OUTPUT.println(socket);
            e.printStackTrace();
        }
    }
}
