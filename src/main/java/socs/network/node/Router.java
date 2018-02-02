package socs.network.node;

import socs.network.util.Configuration;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.InetAddress;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.LinkedList;
import java.util.List;

public class Router {

	protected LinkStateDatabase lsd;
	RouterDescription rd = new RouterDescription();
	public List<Link> ports = new LinkedList<Link>();

	public Router(Configuration config) {
		
		// get info from conf file
		rd.simulatedIPAddress = config.getString("socs.network.router.ip");
		rd.processPortNumber = Short.parseShort(config.getString("socs.network.router.port"));
		
		// get local host address
		InetAddress inetAddress = null;
		try {
			inetAddress = InetAddress.getLocalHost();
		} catch (UnknownHostException e) {
			e.printStackTrace();
		}
		rd.processIPAddress = inetAddress.getHostAddress();
		
		System.out.println("Started router " + rd.processIPAddress + ":" + rd.processPortNumber + " with simulatedIP (" + rd.simulatedIPAddress + ")");
		
		Server server = new Server(this, rd.processPortNumber);
		server.start();
		
		lsd = new LinkStateDatabase(rd);
	}

	/**
	 * output the shortest path to the given destination ip
	 * <p/>
	 * format: source ip address -> ip address -> ... -> destination ip
	 *
	 * @param destinationIP
	 *            the ip adderss of the destination simulated router
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

		// cannot attach to self
		if (rd.simulatedIPAddress.equals(simulatedIP)) {
			System.out.println("Error: Cannot establish link to myself");
			return;
		}
		
		// check to see if router is already attached
		for (Link currLink : ports) {
			// Attempting to attach to a router it is already attached to
			if (currLink.router2.simulatedIPAddress.equals(simulatedIP) ) {
				System.out.println("Error: Attach failed: link already exists between this router and " + processIP + ".");
			}
		}

		// check to see if there is space in ports list
		if (ports.size() < 4) {
			RouterDescription remoteRouter = new RouterDescription();
			remoteRouter.processIPAddress = processIP;
			remoteRouter.simulatedIPAddress = simulatedIP;
			remoteRouter.processPortNumber = processPort;

			ports.add(new Link(rd, remoteRouter));
			System.out.println("Attached (" + rd.simulatedIPAddress + ") to (" + simulatedIP +")");
		}
		// ports list is full
		else {
			System.out.println("Error: Attach failed: all ports are full.");
		}

	}

	/**
	 * broadcast Hello to neighbors
	 */
	private void processStart() {

		Socket clientSocket = null;

		// send HELLO message to all attached routers
		for (Link current : ports) {
			String hostName = current.router2.processIPAddress;
			short port = current.router2.processPortNumber;

			try {
				clientSocket = new Socket(hostName, port);
			} catch (Exception e) {
				e.printStackTrace();
			}

			// start client thread
			ClientThread myClientThread = new ClientThread(clientSocket, this);
			myClientThread.start();
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

		// iterate through all ports and print all IPAddress of routers whose RouterStatus is TWO_WAY
		for (Link currLink : ports) {
			if (currLink.router2.status == RouterStatus.TWO_WAY) {
				System.out.println(currLink.router2.simulatedIPAddress);
			}
		}

	}

	/**
	 * disconnect with all neighbors and quit the program
	 */
	private void processQuit() {
        System.exit(0);
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
