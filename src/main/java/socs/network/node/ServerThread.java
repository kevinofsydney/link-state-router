package socs.network.node;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;

import socs.network.message.LSA;
import socs.network.message.LinkDescription;
import socs.network.message.SOSPFPacket;

public class ServerThread extends Thread {

	Socket mySocket;
	Router myRouter;

	ServerThread(Socket socket, Router router) {
		mySocket = socket;
		myRouter = router;
	}

	private void broadcastLSP() throws IOException {
		// send new Link State Packet ----------------------------------------------------------------------------

		// create a link state advertisement (LSA) with all Link Description (LD) for each occupied port
		LSA tempLSA = new LSA();
		tempLSA.lsaSeqNumber = myRouter.lsd._store.get(myRouter.rd.simulatedIPAddress).lsaSeqNumber++;
		tempLSA.linkStateID = myRouter.rd.simulatedIPAddress;

		// Add all of the router's links to the LSA
		for (Link link : myRouter.ports) {
			tempLSA.addLink(link.router2.simulatedIPAddress, link.router2.processPortNumber, link.weight);
		}

		// Add the link state advertisement to the link state database
		myRouter.lsd.addLSA(tempLSA);

		// For each connection, create a link state packet (LSP) and send it
		for (Link link : myRouter.ports) {

			SOSPFPacket LSP = new SOSPFPacket();
			LSP.srcProcessIP = myRouter.rd.processIPAddress;
			LSP.srcProcessPort = myRouter.rd.processPortNumber;
			LSP.srcIP = myRouter.rd.simulatedIPAddress;
			LSP.dstIP = link.router2.simulatedIPAddress;
			LSP.sospfType = 1;
			LSP.routerID = myRouter.rd.simulatedIPAddress;
			LSP.neighborID = myRouter.rd.simulatedIPAddress;
			LSP.lsaArray.add(tempLSA);

			// Send the packet to each neighbour
			Socket clientSocket = new Socket(link.router2.processIPAddress, link.router2.processPortNumber);
			ObjectOutputStream outputStream = new ObjectOutputStream(clientSocket.getOutputStream());
			outputStream.writeObject(LSP);
			clientSocket.close();
			outputStream.close();

		}
		// sent Link State Packet ----------------------------------------------------------------------------

	}

	public void run() {

		ObjectInputStream inputStream = null;
		ObjectOutputStream outputStream = null;

		try {

			// get packet from InputStream
			inputStream = new ObjectInputStream(mySocket.getInputStream());
			SOSPFPacket message = (SOSPFPacket) inputStream.readObject();

			// -----------------------------------------------------------------------------------------------------------------------------------------
			// check if message is HELLO message
			if (message.sospfType == 0) {

				System.out.println("\nreceived HELLO from " + message.neighborID + ";");

				// message.neighborID identifies sender of packet

				// add a neighbour
				String senderSimIPAddress = message.neighborID;
				boolean addNeighbour = true;

				// check to see if sender is already a neighbour
				for (Link currLink : myRouter.ports) {
					// sender is already a neighbour (dont want to addNeighbour)
					if (currLink.router2.simulatedIPAddress.equals(senderSimIPAddress)) {
						addNeighbour = false;
					}
				}

				// if sender is not already a neighbour, then add as neighbour
				if (addNeighbour) {

					// add a new Neighbour to myRouter
					if (myRouter.ports.size() < 4) {
						// create new RouterDescription for Neighbour
						RouterDescription remoteRouter = new RouterDescription();
						remoteRouter.processIPAddress = message.srcProcessIP;
						remoteRouter.simulatedIPAddress = senderSimIPAddress;
						remoteRouter.processPortNumber = message.srcProcessPort;
						remoteRouter.status = RouterStatus.INIT;

						myRouter.ports.add(new Link(myRouter.rd, remoteRouter, message.srcWeight));

						System.out.println("set " + senderSimIPAddress + " state to INIT;");

						// cannot add new Neighbour because myRouter.ports are full
					} else {
						System.out.println("Error: Cannot add new Neighbour, ports are full");
					}

				}
				// Prepare response ---------------------------------------------------------------------

				// create new SOSPF packet with HELLO message
				SOSPFPacket response = new SOSPFPacket();
				response.sospfType = 0;
				response.neighborID = myRouter.rd.simulatedIPAddress;
				response.srcProcessIP = myRouter.rd.processIPAddress;
				response.srcProcessPort = myRouter.rd.processPortNumber;

				// send response to client
				outputStream = new ObjectOutputStream(mySocket.getOutputStream());
				outputStream.writeObject(response);

				// get response from client
				response = (SOSPFPacket) inputStream.readObject();

				// confirm it is a HELLO response
				if (response.sospfType == 0) {
					System.out.println("received HELLO from " + response.neighborID + ";");
					// set myRouter.neighbourID.status to TWO_WAY
					for (Link currLink : myRouter.ports) {
						// sender is already a neighbour (dont want to addNeighbour)
						if (currLink.router2.simulatedIPAddress.equals(senderSimIPAddress)) {

							currLink.router2.status = RouterStatus.TWO_WAY;
							System.out.println("set " + message.neighborID + " state to TWO_WAY;");

							broadcastLSP();

							// Error: sender not a neighbour
						} else {
							//System.out.println("Error: Setting sender status to TWO_WAY, sender not a Neighbour");
						}
					}
				}

				// END Server HELLO Response 
				//-----------------------------------------------------------------------------------------------------------------------------------------

				// message is Link State Packet
				//-----------------------------------------------------------------------------------------------------------------------------------------
			} else if (message.sospfType == 1) {
				//If packet is a LSA Update

				//check to see if sequenceNumber is greater than current by getting the most recent LSA from the requested source IP
				LSA lsa = myRouter.lsd._store.get(message.srcIP);

				//if the incoming LSA is newer than current or new router
				if (lsa == null || (message.lsaArray.lastElement().lsaSeqNumber > lsa.lsaSeqNumber)) {

					//check if the link between this router and the sender exists (direct neighbor)            
					for (Link link : myRouter.ports) {
						//if link exists, update link in port 
						if (link.router2.simulatedIPAddress.equals(message.srcIP)) {
							LinkDescription linkDescription = null;

							//find the link description of current router in the LSA from message
							for (LinkDescription ld : message.lsaArray.lastElement().links) {
								if (ld.linkID.equals(myRouter.rd.simulatedIPAddress)) {
									linkDescription = ld;
									break;
								}
							}

							if (linkDescription != null) {
								//if the LSA is saying that there is an outdated weight, then update weight in port and broadcast
								if (linkDescription.tosMetrics != link.weight && linkDescription.tosMetrics > -1) {
									//update link weight
									link.weight = linkDescription.tosMetrics;
								}
							}
						}
					}

					broadcastLSP();
				}
			}

			//System.out.println("Wrong message");

		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			try {
				inputStream.close();
				outputStream.close();
				mySocket.close();
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	}
}
