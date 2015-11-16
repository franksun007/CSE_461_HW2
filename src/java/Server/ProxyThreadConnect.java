package Server;


import java.io.InputStream;
import java.io.OutputStream;

/**
 * Created by chenfs on 11/15/15.
 */
public class ProxyThreadConnect extends Thread {
    private InputStream in;
    private OutputStream out;

    private String shit;

    public ProxyThreadConnect (InputStream in, OutputStream out, String shit) {
        this.in = in;
        this.out = out;
        this.shit = shit;
    }

    @Override
    public void run() {
        try {
            byte[] data = new byte[10 * ProxyThread.DEFAULT_PACKET_SIZE];
           // int read;
            //req.setLength(0);
            //while ((read = in.read()) != 0) {
              //  System.out.println((char)(read));
              //  out.write(read);
              //  out.flush();
                //req.append(new String(data, 0, read, "ascii"));
                // fromProxyToServer.write(req.toString().getBytes("ascii"));
                // fromProxyToServer.flush();
            //}

            int read = in.read(data);
            in.read();

            while (read != -1) {
                System.out.println(new String(data, "ascii"));
                out.write(data);
                out.flush();
                read = in.read(data);
            }

            //System.out.println(req);
        } catch (Exception e) {
            System.out.print("From " + shit + " Socket: ");
            System.out.println(e);
        }
    }
}
