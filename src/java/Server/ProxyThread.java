package Server;

import java.io.*;
import java.net.ProxySelector;
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

        DataOutputStream fromProxyToClient;
        Socket proxySocket = null;
        try {
            fromProxyToClient = new DataOutputStream(this.socket.getOutputStream());

            try {
                proxySocket = new Socket(host, port);
            } catch (Exception e) {
                fromProxyToClient.write("HTTP/1.0 502 BAD GATEWAY\r\n\r\n".getBytes("ascii"));
            }

            System.out.println("Socket established");
            assert (proxySocket != null);

            fromProxyToClient.write("HTTP/1.0 200\r\n\r\n".getBytes("ascii"),
                    0, "HTTP/1.0 200\r\n\r\n".length());
            fromProxyToClient.flush();

//            DataInputStream fromClientToProxy = new DataInputStream(this.socket.getInputStream());
//            InputStream fromClientToProxy = this.socket.getInputStream();
//            BufferedReader fromClientToProxy = new BufferedReader(new InputStreamReader(this.socket.getInputStream()));
            BufferedInputStream fromClientToProxy = new BufferedInputStream(socket.getInputStream());




            byte[] data = new byte[10 * DEFAULT_PACKET_SIZE];
            DataOutputStream fromProxyToServer = new DataOutputStream(proxySocket.getOutputStream());
//            DataInputStream fromServerToProxy = new DataInputStream(proxySocket.getInputStream());
//            InputStream fromServerToProxy = proxySocket.getInputStream();
//            BufferedReader fromServerToProxy = new BufferedReader(new InputStreamReader(proxySocket.getInputStream()));

            BufferedInputStream fromServerToProxy = new BufferedInputStream(proxySocket.getInputStream());

            String line;
            StringBuilder everything = new StringBuilder();
            while (true) {
//                System.out.println("shit");
//                everything.setLength(0);
//
//                while (!(line = fromClientToProxy.readLine()).equals("")) {
//                    System.out.println(line);
//                    everything.append(line);
//                }
//                if (everything.length() > 0) {
//                    fromProxyToServer.write(everything.toString().getBytes("ascii"));
//                    fromProxyToServer.flush();
//                }
//                System.out.println("shit");
//
//                while (!(line = fromServerToProxy.readLine()).equals("")) {
//                    fromProxyToClient.write(line.getBytes("ascii"));
//                    fromProxyToClient.flush();
//                }

                StringBuilder req = new StringBuilder();
                int read = fromClientToProxy.available();
                if (read > 0) {
                    read = fromClientToProxy.read(data, 0, read);
                    req.append(new String(data, 0, read, "ascii"));
                }
                if (req.length() > 0) {
                    fromProxyToServer.write(req.toString().getBytes("ascii"));
                    fromProxyToServer.flush();
                }

                read = fromServerToProxy.available();
                if (read > 0) {
                    read = fromServerToProxy.read(data);
                    fromProxyToClient.write(data, 0, read);
                    fromProxyToClient.flush();
                }


            }

            /*
            while (!proxySocket.isClosed()) {
                int read = fromClientToProxy.read(data);
                req.setLength(0);
                while (read != -1) {
                    System.out.println(read);
                    req.append(new String(data, 0, read, "ascii"));
                    if (fromClientToProxy.available() > 0)
                        read = fromClientToProxy.read(data);
                    else
                        break;
                }
                System.out.println(req.toString());

                if (req.length() != 0) {
                    fromProxyToServer.write(req.toString().getBytes("ascii"));
                    fromProxyToServer.flush();
                }

                read = fromClientToProxy.read(data);

                while (read != -1) {
                    fromProxyToClient.write(data, 0, read);
                    fromProxyToClient.flush();
                    if (fromServerToProxy.available() > 0)
                        read = fromServerToProxy.read(data);
                    else
                        break;
                }
            }
                InputStream clientReader = socket.getInputStream();
				InputStream serverReader = proxySocket.getInputStream();
				DataOutputStream clientStreamer = new DataOutputStream(socket.getOutputStream());
				DataOutputStream serverStreamer = new DataOutputStream(proxySocket.getOutputStream());

				while (true)
				{
//					System.out.println("start");
					if (clientReader.available() != 0)
					{
						byte[] buf = new byte[100];
						int read = clientReader.read(buf);
						while (read != -1)
						{
							serverStreamer.write(buf, 0, read);
							serverStreamer.flush();
							read = serverReader.read(buf);
						}
					}

					if (serverReader.available() != 0)
					{
						byte[] buf2 = new byte[100];
						int read2 = serverReader.read(buf2);
						while (read2 != -1)
						{
							clientStreamer.write(buf2, 0, read2);
							clientStreamer.flush();
							read2 = serverReader.read(buf2);
						}
					}
//					System.out.println("end");
				}
*/

        } catch (Exception e) {
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
