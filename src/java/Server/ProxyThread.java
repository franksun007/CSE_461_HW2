package Server;

import Utils.Utilities;

import java.io.*;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;
import java.util.Iterator;
import java.util.Set;

/**
 * This class is used by the proxy class so that the proxy class can handle several requests concurrently.
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
            this.interrupt();
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
                    + firstLine.substring(0, firstLine.indexOf("HTTP/1.")));

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
                } else if (requestLine.toLowerCase().contains("proxy-connection: keep-alive")) {
                    requestLine = requestLine.replaceAll("keep-alive", "close");
                } else if (requestLine.isEmpty() || requestLine.equals("\r\n")) {
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
            closeSocket(this.socket.socket());
            OUTPUT.println("Unexpected exception");
            OUTPUT.println(e.getMessage());
            this.interrupt();
        }
    }

    private void connect(String host, int port) {
        SocketChannel proxyChannel;
        SocketChannel browserChannel = this.socket;
        Selector selector;
        ByteBuffer buffer = ByteBuffer.allocate(DEFAULT_PACKET_SIZE);
        String msg502 = "HTTP/1.0 502 BAD GATEWAY\r\n\r\n";
        String msg200 = "HTTP/1.1 200 OK\r\n\r\n";
        try {
            proxyChannel = SocketChannel.open();
            if (!proxyChannel.connect(new InetSocketAddress(host, port))) {
                browserChannel.write(buffer.put(msg502.getBytes("ascii")));
                buffer.flip();
                return;
            }
            browserChannel.configureBlocking(false);
            proxyChannel.configureBlocking(false);
            selector = Selector.open();
            proxyChannel.register(selector, SelectionKey.OP_READ);
            browserChannel.register(selector, SelectionKey.OP_READ);

            // Sends the 200 msg to browser
            browserChannel.write(buffer.put(msg200.getBytes("ascii")));
            buffer.flip();
            assert (proxyChannel.isConnected() && browserChannel.isConnected());
            // Start tunneling
            try {
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
                            int readCount = readFromChannel.read(buffer);
                            while (readCount > 0) {
                                buffer.flip();
                                while (buffer.hasRemaining()) {
                                    writeToChannel.write(buffer);
                                }
                                buffer.flip();
                                readCount = readFromChannel.read(buffer);
                            }
                        }
                        iter.remove();
                    }
                }
            } catch(Exception e) {
                closeSocket(proxyChannel.socket());
                closeSocket(socket.socket());
                proxyChannel.close();
                socket.close();
            }
        } catch (Exception e) {
            closeSocket(this.socket.socket());
            OUTPUT.println(e.getMessage());
        } finally {
            this.interrupt();
        }
    }

    private void nonConnect(String host, int port, String fullRequest) {
        Socket proxySocket = null;
        try {
            proxySocket = new Socket(host, port);

            InputStream fromServerToProxy = proxySocket.getInputStream();
            DataOutputStream fromProxyToServer =
                    new DataOutputStream(proxySocket.getOutputStream());
            DataOutputStream fromProxyToClient =
                    new DataOutputStream(this.socket.socket().getOutputStream());
            fromProxyToServer.write(fullRequest.getBytes("ascii"));
            fromProxyToServer.flush();

            byte[] data = new byte[DEFAULT_PACKET_SIZE];
            int read = fromServerToProxy.read(data);
            while (read != -1) {
                fromProxyToClient.write(data, 0, read);
                fromProxyToClient.flush();
                read = fromServerToProxy.read(data);
            }
            closeSocket(this.socket.socket());
            proxySocket.close();
            closeSocket(proxySocket);
        } catch (Exception e) {
            closeSocket(this.socket.socket());
            closeSocket(proxySocket);
            OUTPUT.println(e.getMessage());
        } finally {
            this.interrupt();
        }
    }

    private void closeSocket(Socket socket) {
        try {
            socket.close();
        } catch (Exception e) {
            OUTPUT.print("Socket closure failed: ");
            OUTPUT.println(socket);
            OUTPUT.println(e.getMessage());
        }
    }
}
