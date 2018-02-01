package socs.network.node;

import socs.network.message.SOSPFPacket;
import socs.network.util.Configuration;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;

public class Router {

	protected LinkStateDatabase lsd;
    private Server server;
	RouterDescription rd = new RouterDescription();

	//assuming that all routers are with 4 ports
	Link[] ports = new Link[4];

	public Router(Configuration config) {
		rd.simulatedIPAddress = config.getString("socs.network.router.ip");
		rd.processPortNumber = config.getShort("socs.network.router.port");
		lsd = new LinkStateDatabase(rd);
        Server server = new Server(this, rd.processPortNumber);

		System.out.println("Router with IP address " + rd.simulatedIPAddress + " started.");
	}

	/**
	 * output the shortest path to the given destination ip
	 * <p/>
	 * format: source ip address -> ip address -> ... -> destination ip
	 *
	 * @param destinationIP
	 *            the ip address of the destination simulated router
	 */
	private void processDetect(String destinationIP) {
		System.out.println(lsd.getShortestPath(destinationIP));
	}

	/**
	 * disconnect with the router identified by the given destination ip address
	 * Notice: this command should trigger the synchronization of database
	 *
	 * @param portNumber
	 *            the port number which the link attaches at
	 */
	private void processDisconnect(short portNumber) {
		
	}

	/**
	 * attach the link to the remote router, which is identified by the given
	 * simulated ip; to establish the connection via socket, you need to
	 * indentify the process IP and process Port; additionally, weight is the
	 * cost to transmitting data through the link
	 * <p/>
	 * NOTE: this command should not trigger link database synchronization
	 */
	private void processAttach(String processIP, short processPort, String simulatedIP, short weight) {

		// Cannot attach to itself
		if(rd.simulatedIPAddress.equals(simulatedIP)) {
			System.out.println("Error: Cannot establish link to myself");
			return;
		}
		
		// Check to make sure it isn't already attached to the given IP
		for (int j = 0 ; j < ports.length; j++) {
			if ( ports[j].router2.simulatedIPAddress.equals(simulatedIP)) {
				System.out.println("Error: Cannot establish link that is already existing");
				return;
			}
		}

		//Attempt to find a free port
        for (int i = 0 ; i < ports.length; i++) {
			if(ports[i] == null ) {
				// create the new router description
				RouterDescription remoteRouterDescription = new RouterDescription();
				remoteRouterDescription.processIPAddress = processIP;
				remoteRouterDescription.simulatedIPAddress = simulatedIP;
				remoteRouterDescription.processPortNumber = processPort;
				
				ports[i] = new Link(rd, remoteRouterDescription);
				break;
			} else {
				System.out.println("Error: Cannot establish link to remote router, ports are full");
			}
		}
	}

	/**
	 * broadcast Hello to neighbors
	 */
	private void processStart() {

        Socket clientSocket;
		
		for(int i = 0 ; i < ports.length; i++) {
            if (ports[i] != null) {
                if (ports[i].router2.status == null) {
                	
                    String serverIPAddress = ports[i].router2.processIPAddress;
                    short port = ports[i].router2.processPortNumber;

                    // I don't think we need this packet thing, we just need
                    // input and output streams
                    SOSPFPacket packet = new SOSPFPacket();
                    packet.sospfType = 0;
                    packet.neighborID = rd.simulatedIPAddress;
                    packet.srcProcessIP = rd.processIPAddress;
                    packet.srcProcessPort = rd.processPortNumber;
                    
                    	// act as a client send HELLO via SOSPFPacket
                }
            }
		}
		
	}

	/**
	 * attach the link to the remote router, which is identified by the given
	 * simulated ip; to establish the connection via socket, you need to
	 * indentify the process IP and process Port; additionally, weight is the
	 * cost to transmitting data through the link
	 * <p/>
	 * This command does trigger the link database synchronization
	 */
	private void processConnect(String processIP, short processPort, String simulatedIP, short weight) {

	}

	/**
	 * output the neighbors of the routers
	 */
	private void processNeighbors() {

	}

	/**
	 * disconnect with all neighbors and quit the program
	 */
	private void processQuit() {

	}

	public void terminal() {
		try {
			InputStreamReader isReader = new InputStreamReader(System.in);
			BufferedReader br = new BufferedReader(isReader);
			System.out.print(">> ");
			String command = br.readLine();
			while (true) {
				if (command.startsWith("detect ")) {
					String[] cmdLine = command.split(" ");
					processDetect(cmdLine[1]);
				} else if (command.startsWith("disconnect ")) {
					String[] cmdLine = command.split(" ");
					processDisconnect(Short.parseShort(cmdLine[1]));
				} else if (command.startsWith("quit")) {
					processQuit();
				} else if (command.startsWith("attach ")) {
					String[] cmdLine = command.split(" ");
					processAttach(cmdLine[1], Short.parseShort(cmdLine[2]), cmdLine[3], Short.parseShort(cmdLine[4]));
				} else if (command.equals("start")) {
					processStart();
				} else if (command.equals("connect ")) {
					String[] cmdLine = command.split(" ");
					processConnect(cmdLine[1], Short.parseShort(cmdLine[2]), cmdLine[3], Short.parseShort(cmdLine[4]));
				} else if (command.equals("neighbors")) {
					//output neighbors
					processNeighbors();
				} else {
					//invalid command
					break;
				}
				System.out.print(">> ");
				command = br.readLine();
			}
			isReader.close();
			br.close();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

}
