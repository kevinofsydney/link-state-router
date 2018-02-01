package socs.network.node;

import socs.network.message.SOSPFPacket;
import socs.network.util.Configuration;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.LinkedList;
import java.util.List;

public class Router {

	protected LinkStateDatabase lsd;
    private Server server;
	private RouterDescription rd = new RouterDescription();

	//assuming that all routers are with 4 ports
	public List<Link> ports = new LinkedList<Link>();

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
        //Note: The 'name' of the router is it's simulatedIP
	    for (Link currLink : ports)
        {
            // Attempting to attach to a router it is already attached to
            if (currLink.router2.processIPAddress.equals(processIP) &&
                    currLink.router2.processPortNumber == processPort)
            {
                System.out.println("Error: Attach failed: link already exists between this router and " + processIP + ".");
            }
            //Attempting to attach to itself
            else if (currLink.router2.simulatedIPAddress.equals(simulatedIP))
            {
                System.out.println("Error: Attach failed: router cannot attach to itself.");
            }
        }

        if (ports.size() < 4)
        {
            RouterDescription remoteRouter = new RouterDescription();
            remoteRouter.processIPAddress = processIP;
            remoteRouter.simulatedIPAddress = simulatedIP;
            remoteRouter.processPortNumber = processPort;
            remoteRouter.status = RouterStatus.TWO_WAY;

            Link link = new Link(rd, remoteRouter);
            ports.add(link);
        }
        else
            System.out.println("Error: Attach failed: all ports are full.");

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
