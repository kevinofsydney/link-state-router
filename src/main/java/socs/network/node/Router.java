package socs.network.node;
import socs.network.message.LinkDescription;
import socs.network.message.SOSPFPacket;
import socs.network.util.Configuration;
import socs.network.message.LSA;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.InetAddress;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.LinkedList;
import java.util.List;

public class Router {

	public LinkStateDatabase lsd;
	RouterDescription rd = new RouterDescription();
	public List<Link> ports = new LinkedList<Link>();
    private static boolean ROUTER_STARTED = false;

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
    // Should you be able to call attach if the router has already started?
    private int processAttach(String processIP, short processPort, String simulatedIP, short weight) {

		// cannot attach to self
		if (rd.simulatedIPAddress.equals(simulatedIP)) {
			System.out.println("ERROR: Cannot establish link to myself");
			return 1;
		}
		
		// check to see if router is already attached
		for (Link currLink : ports) {
			// Attempting to attach to a router it is already attached to
			if (currLink.router2.simulatedIPAddress.equals(simulatedIP) ) {
				System.out.println("ERROR: Attach failed: link already exists between this router and " + processIP + ".");
			}
		}

		// check to see if there is space in ports list
		if (ports.size() < 4) {
			RouterDescription remoteRouter = new RouterDescription();
			remoteRouter.processIPAddress = processIP;
			remoteRouter.simulatedIPAddress = simulatedIP;
			remoteRouter.processPortNumber = processPort;

			ports.add(new Link(rd, remoteRouter, weight));
			System.out.println("INFO: Attached (" + rd.simulatedIPAddress + ") to (" + simulatedIP +")");
		}
		// ports list is full
		else {
			System.out.println("ERROR: Attach failed: all ports are full.");
		}
        return 0;
	}

	/**
	 * broadcast Hello to neighbors
	 */
	private void processStart() {

		if (ROUTER_STARTED) {
		    System.out.println("INFO: Router has already been started.");
		    return;
        }

	    Socket clientSocket = null;
        ROUTER_STARTED = true;
        System.out.println("INFO: Router started with no connections.");

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

     * Start() is similar to attach command, but it directly triggers the database synchronization.
     * This command can only be run after start ).
     **/
    private void processConnect(String processIP, short processPort, String simulatedIP, short weight) {
        if (ROUTER_STARTED) {
            // Attempt attaching to the remote router
            if (processAttach(processIP, processPort, simulatedIP, weight) == 1)
                return;         // attach failed

            Link freshLink = null;

            // Go through the ports and find the port it was attached to
            for (Link l : ports) {
                if (l.router2.simulatedIPAddress == simulatedIP && l.router2.processPortNumber == processPort){ // processPort check probably redundant, idk
                    freshLink = l;
                    break;
                }
            }

            // Run the HELLO message exchange and broadcast the LSA updates
            Socket clientSocket = null;
			String hostName = freshLink.router2.processIPAddress;
			short port = freshLink.router2.processPortNumber;

			try {
				clientSocket = new Socket(hostName, port);
			} catch (Exception e) {
				e.printStackTrace();
			}

            Thread myClientThread = new Thread(new Client(clientSocket, this, freshLink));
            myClientThread.start();
        } else {
            System.out.println("INFO: Start() must be called first before calling connect().");
        }
	}

    /**
     * disconnect with the router identified by the given destination ip address
     * NOTE: This command DOES trigger the link database synchronization
     *
     * @param portNumber
     *        the port number which the link attaches to
     *
     * Remove the link between this router and the remote one which is connected at port [Port Number]
     * NOTE: This DOES trigger link state database synchronization by sending LSAUPDATE (Link State Advertisement Update)
     * message to all neighbors in the topology.
     **/
    private void processDisconnect(short portNumber) {
        if (portNumber > ports.size()) {
            System.out.println("ERROR: Invalid port number.");
            return;
        }

        // Remove the disconnected device's link from this router's ports
        Link deadLink = ports.remove(portNumber);
        System.out.println("INFO: IP of Link removed from ports[]: " + deadLink.router2.simulatedIPAddress);

        // Remove the disconnected device's entry from this router's LSD
        LSA deadRouterLSA = lsd._store.remove(deadLink.router2.simulatedIPAddress);
        System.out.println("INFO: Identity of removed LSA: " + deadRouterLSA.linkStateID);

        // Retrieve the LSA of this router
        LSA myLSA = this.lsd._store.get(rd.simulatedIPAddress);

        // Remove any links to the disconnect device from this router's LSA
        for (LinkDescription linkDesc : myLSA.links) {
            if (linkDesc.linkID.equals(deadLink.router2.simulatedIPAddress)) {
                myLSA.links.remove(linkDesc);
                myLSA.lsaSeqNumber++;
                System.out.println("INFO: Identity of LD removed from my LSA: " + linkDesc.linkID);
                break;
            }
        }

        // Store this router's LSA inside it's LSD
        lsd._store.put(rd.simulatedIPAddress, myLSA);

        // Open up a channel to the neighbor
        try {
            Socket clientSocket = new Socket(deadLink.router2.processIPAddress, deadLink.router2.processPortNumber);
            ObjectOutputStream output = new ObjectOutputStream(clientSocket.getOutputStream());

            // Create a deletion packet
            SOSPFPacket disconnectRequest = new SOSPFPacket();
            disconnectRequest.dstIP = deadLink.router2.simulatedIPAddress;
            disconnectRequest.srcIP = disconnectRequest.srcProcessIP = this.rd.simulatedIPAddress;
            disconnectRequest.srcProcessPort = this.rd.processPortNumber;
            disconnectRequest.sospfType = 2;

            // Send the deletion packet
            output.writeObject(disconnectRequest);

            System.out.println("INFO: Disconnect packet sent. Awaiting confirmation from remote router.");
            try {
                ObjectInputStream input = new ObjectInputStream(clientSocket.getInputStream());

                SOSPFPacket inbound = (SOSPFPacket) input.readObject();
                input.close();
                output.close();
                if (inbound.sospfType == 2)
                {
                    System.out.println("INFO: Disconnect from the remote router was successful.");
                    clientSocket.close();
                }
                else
                    System.out.println("ERROR: Disconnect from the remote router failed.");
            } catch (Exception e) {
                e.printStackTrace();
            }

        } catch (Exception e) {
            e.printStackTrace();
        }

        try {
            for (Link link : this.ports) {

                SOSPFPacket LSP = new SOSPFPacket();
                LSP.srcProcessIP = rd.processIPAddress;
                LSP.srcProcessPort = rd.processPortNumber;
                LSP.srcIP = rd.simulatedIPAddress;
                LSP.dstIP = link.router2.simulatedIPAddress;
                LSP.sospfType = 1;
                LSP.neighborID = rd.simulatedIPAddress;
                LSP.lsaArray.add(myLSA);        // send out the updated LSP

                // Send the packet to each neighbour
                Socket clientSocket = new Socket(link.router2.processIPAddress, link.router2.processPortNumber);
                ObjectOutputStream outputStreamLSP = new ObjectOutputStream(clientSocket.getOutputStream());
                outputStreamLSP.writeObject(LSP);
                clientSocket.close();
                outputStreamLSP.close();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        System.out.println("INFO: Port " + portNumber + " (" + deadLink.router2.simulatedIPAddress + ") has been disconnected from remote router.");

        // Broadcast to all neighbors that you have disconnected with that remote router
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

	    if (ports.size() == 0)
        {
            System.out.println("INFO: No neighbors connected.");
            return;
        }

		// iterate through all ports and print all IPAddress of routers whose RouterStatus is TWO_WAY
		for (Link currLink : ports) {
			if (currLink.router2.status == RouterStatus.TWO_WAY) {
				System.out.println(currLink.router2.simulatedIPAddress);
			}
		}

	}

	// Helper function taken from https://github.com/Shabirmean/simulatedNetwork
	private void printLSD() {
        for (String lsaEntry : this.lsd._store.keySet()) {
            LSA lsa = this.lsd._store.get(lsaEntry);
            System.out.println("--------------------------------------------------");
            System.out.println("       RouterIP      :   " + lsaEntry);
            System.out.println("       OriginatorIP  :   " + lsa.linkStateID);
            System.out.println("..................................................");

            for (LinkDescription linkDes : lsa.links) {
                System.out.println("LinkID [" + linkDes.linkID + "] - " +
                        "Port [" + linkDes.portNum + "] - WEIGHT [" + linkDes.tosMetrics + "]");
            }
            System.out.println("--------------------------------------------------");
        }
    }

    // Helper function taken from https://github.com/Shabirmean/simulatedNetwork
    private void printPortInfo() {
        int i = 0;
	    for (Link linkOnPort : ports) {
            if (linkOnPort != null) {
                RouterDescription linkedRouter = linkOnPort.router2;
                String processIPAddress = linkedRouter.processIPAddress;
                short processPortNumber = linkedRouter.processPortNumber;
                String simulatedIPAddress = linkedRouter.simulatedIPAddress;
                RouterStatus status = linkedRouter.status;
                String statusString = "NULL";

                if (status == RouterStatus.INIT) {
                    statusString = "INIT";
                } else if (status == RouterStatus.TWO_WAY) {
                    statusString = "TWO_WAY";
                }
                int linkWeight = linkOnPort.weight;

                System.out.println("-------------------------------------------");
                System.out.println("    ON ROUTER PORT: " + i);
                System.out.println("    PROCESS IP: " + processIPAddress);
                System.out.println("    PROCESS PORT: " + processPortNumber);
                System.out.println("    SIMULATED IP: " + simulatedIPAddress);
                System.out.println("    STATUS: " + statusString);
                System.out.println("    LINK WEIGHT: " + linkWeight);
                System.out.println("-------------------------------------------");
            } else {
                System.out.println("PORT " + i + " is free.");
            }
            i++;
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
				    if (cmdLine.length < 2) {
				        System.out.println("ERROR: Please enter a router to detect.");
                    } else {
                        processDetect(cmdLine[1]);
                    }
				} else if (command.startsWith("disconnect ")) {
					String[] cmdLine = command.split(" ");
					processDisconnect(Short.parseShort(cmdLine[1]));
				} else if (command.startsWith("quit")) {
					processQuit();
                    System.out.println("INFO: Exiting program. Goodbye!");
                    System.exit(0);
                    break;      // Unreachable code, but it's just here to keep the compiler happy
				} else if (command.startsWith("attach ")) {
					String[] cmdLine = command.split(" ");
					processAttach(cmdLine[1], Short.parseShort(cmdLine[2]), cmdLine[3], Short.parseShort(cmdLine[4]));
				} else if (command.trim().equals("start")) {
					processStart();
				} else if (command.startsWith("connect ")) {
					String[] cmdLine = command.split(" ");
					processConnect(cmdLine[1], Short.parseShort(cmdLine[2]), cmdLine[3], Short.parseShort(cmdLine[4]));
				} else if (command.trim().equals("neighbors")) {
                    // output neighbors
                    processNeighbors();
                } else if (command.trim().equals("lsd")) {
                    // print the LSD
                    printLSD();
                } else if (command.trim().equals("ports")) {
                    // print the status of all ports
				    printPortInfo();
				} else {
					//invalid command
                    System.out.println("ERROR: Invalid command. Please try again.");
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
