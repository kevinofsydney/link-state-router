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

	public LinkStateDatabase lsd;
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
	 * attach the link to the remote router, which is identified by the given
	 * simulated ip; to establish the connection via socket, you need to
	 * identify the process IP and process Port; additionally, weight is the
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

			ports.add(new Link(rd, remoteRouter, weight));
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
			Thread myClientThread = new Thread(new Client(clientSocket, this, current));
			myClientThread.start();
		}
	}

	/**
	 * attach the link to the remote router, which is identified by the given
	 * simulated ip; to establish the connection via socket, you need to
	 * identify the process IP and process Port; additionally, weight is the
	 * cost to transmitting data through the link
	 * <p/>
	 * NOTE: This command DOES trigger the link database synchronization

     * Start() is similar to attach command, but it directly triggers the database synchronization without the
     *   need to run start. This command can only be run after start ).
     **/
    private void processConnect(String processIP, short processPort, String simulatedIP, short weight) {

	}

    /**
     * disconnect with the router identified by the given destination ip address
     * NOTE: This command DOES trigger the link database synchronization
     *
     * @param portNumber
     *        the port number which the link attaches to
     *
     * remove the link between this router and the remote one which is connected at port [Port Number]
     * (port number is between 0 - 3, i.e. four links in the router). Through this command, you are triggering
     * the synchronization of Link State Database by sending LSAUPDATE (Link State Advertisement Update)
     * message to all neighbors in the topology. This process will also be illustrated in the next section.
     **/
    private void processDisconnect(short portNumber) {
		Link deadLink = null;

		for (int i = 0; i < ports.size(); i++)
        {
            if (ports.get(i).router2.status == RouterStatus.TWO_WAY)
            {

            }
        }
    }

    /**
     * disconnect with all neighbors and quit the program
     * NOTE: This DOES trigger link database synchronization
     */
    private void processQuit() {
        System.exit(0);
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
