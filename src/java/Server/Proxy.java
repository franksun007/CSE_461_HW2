package Server;

import java.util.*;

public class Proxy {



    public static void main(String[] args) {
        if (args.length != 1) {
            Usage();
        }

        int serverPort = Integer.parseInt(args[0]);
        System.out.println("Server port is " + serverPort);
    }

    private static void Usage() {
        System.err.println("No port specified");
        System.exit(1);
    }
}