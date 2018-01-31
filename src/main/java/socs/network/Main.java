package socs.network;

import socs.network.node.Router;
import socs.network.util.Configuration;

public class Main {

    public static void main(String[] args) {
        if (args.length != 1) {
            System.out.println("usage: program conf_path");
            System.exit(1);
        }
        Router r = new Router(new Configuration(args[0]));

        // Configuration files look like:
        //   socs.network.router.ip="192.168.1.1"
        //   socs.network.router.port=100
        r.terminal();
    }
}