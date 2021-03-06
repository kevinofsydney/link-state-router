package socs.network.node;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;

import org.omg.PortableInterceptor.SYSTEM_EXCEPTION;
import socs.network.message.LSA;
import socs.network.message.LinkDescription;
import socs.network.message.SOSPFPacket;

public class ServerThread implements Runnable {

	protected Socket socket;
	protected Router router;

	// constructor
	public ServerThread(Socket socket, Router router) {
		this.socket = socket;
		this.router = router;
	}

	// create and send new LSP ----------------------------------------------------------------------------
	protected void sendLSP(LSA lsa) throws IOException {

		// For each link create a LSP and send it
		for (Link link : router.ports) {

			SOSPFPacket LSP = new SOSPFPacket();
			LSP.srcProcessIP = router.rd.processIPAddress;
			LSP.srcProcessPort = router.rd.processPortNumber;
			LSP.srcIP = router.rd.simulatedIPAddress;
			LSP.dstIP = link.router2.simulatedIPAddress;
			LSP.sospfType = 1;
			LSP.neighborID = router.rd.simulatedIPAddress;
			LSP.lsaArray.add(lsa);

			// Send the packet to each neighbour
			Socket clientSocket = new Socket(link.router2.processIPAddress, link.router2.processPortNumber);
			ObjectOutputStream outputStreamLSP = new ObjectOutputStream(clientSocket.getOutputStream());
			outputStreamLSP.writeObject(LSP);
			clientSocket.close();
			outputStreamLSP.close();

		}

	}

	// update existing LSA ----------------------------------------------------------------------------
	protected LSA updateLSA(Link link, int ACTION_FLAG) {

		// create a link state advertisement (LSA) with all Link Description (LD) for each occupied port
		LSA tempLSA = null;

		// retrieve this router's own LSA
		if (router.lsd._store.get(router.rd.simulatedIPAddress) != null) {
			tempLSA = router.lsd._store.get(router.rd.simulatedIPAddress);
			tempLSA.lsaSeqNumber++;
		}

		// If link is null, then a disconnect occurred
        // If link is non-null, then a new connection occurred
		if (ACTION_FLAG == 0) {
            // Add new link to the LSA
            tempLSA.addLinkDescription(link.router2.simulatedIPAddress, link.router2.processPortNumber, link.weight);
            System.out.println("INFO: Added " + link.router2.simulatedIPAddress + " to my LSA.");
		} else {
            // Remove the LSA associated with the disconnected device
		    LSA deadLSA = router.lsd._store.remove(link.router2.simulatedIPAddress);

		    // Retrieve this router's LSA
		    tempLSA = router.lsd._store.get(router.rd.simulatedIPAddress);

            // Remove any links to the disconnected device from this router's LSA
            for (int i = 0; i < tempLSA.links.size(); i++)  {
                if (tempLSA.links.get(i).linkID.equals(link.router2.simulatedIPAddress)) {
                    tempLSA.links.remove(i);
                    tempLSA.lsaSeqNumber++;
                    i--;
                    System.out.println("INFO: Removed " + link.router2.simulatedIPAddress + " from my LSA.");
                }
            }
        }

		// Add the updated link state advertisement to the link state database
		router.lsd._store.put(tempLSA.linkStateID, tempLSA);
//		System.out.println("LSA: " + tempLSA.toString());

		return tempLSA;
	}

	//TODO: Implement a function to handle receiving an exit packet
	public void run() {

		ObjectInputStream inputStream = null;
		ObjectOutputStream outputStream = null;

		try {

			// get packet from InputStream
			inputStream = new ObjectInputStream(this.socket.getInputStream());
			SOSPFPacket message = (SOSPFPacket) inputStream.readObject();

			// -----------------------------------------------------------------------------------------------------------------------------------------
			// check if message is HELLO message
			if (message.sospfType == 0) {

				System.out.println("\nreceived HELLO from " + message.neighborID + ";");

				// message.neighborID identifies sender of packet

				// add a neighbour
				boolean addNeighbour = true;

				// check to see if sender is already a neighbour
				for (Link currLink : router.ports) {
					// sender is already a neighbour (dont want to addNeighbour)
					if (currLink.router2.simulatedIPAddress.equals(message.neighborID)) {
						addNeighbour = false;
					}
				}

				// if sender is not already a neighbour, then add as neighbour
				if (addNeighbour) {

					// add a new Neighbour to myRouter
					if (router.ports.size() < 4) {
						// create new RouterDescription for Neighbour
						RouterDescription remoteRouter = new RouterDescription();
						remoteRouter.processIPAddress = message.srcProcessIP;
						remoteRouter.simulatedIPAddress = message.neighborID;
						remoteRouter.processPortNumber = message.srcProcessPort;
						remoteRouter.status = RouterStatus.INIT;

						router.ports.add(new Link(router.rd, remoteRouter, message.srcWeight));

						System.out.println("set " + message.neighborID + " state to INIT;");

                    // cannot add new Neighbour because myRouter.ports are full
					} else {
						System.out.println("Error: Cannot add new Neighbour, ports are full");
					}

				}
				// Prepare response ---------------------------------------------------------------------

				// create new SOSPF packet with HELLO message
				SOSPFPacket response = new SOSPFPacket();
				response.sospfType = 0;
				response.neighborID = router.rd.simulatedIPAddress;
				response.srcProcessIP = router.rd.processIPAddress;
				response.srcProcessPort = router.rd.processPortNumber;

				// send response to client
				outputStream = new ObjectOutputStream(this.socket.getOutputStream());
				outputStream.writeObject(response);

				// get response from client
				response = (SOSPFPacket) inputStream.readObject();

				// confirm it is a HELLO response
				if (response.sospfType == 0) {
					System.out.println("received HELLO from " + response.neighborID + ";");

					// set myRouter.neighbourID.status to TWO_WAY
					for (Link currLink : router.ports) {
						// check if already TWO WAY
						if (currLink.router2.simulatedIPAddress.equals(response.neighborID)) {

							currLink.router2.status = RouterStatus.TWO_WAY;
							System.out.println("set " + response.neighborID + " state to TWO_WAY;");

							sendLSP(updateLSA(currLink, 0));
						}
					}
				}

				// message is Link State Packet
				//-----------------------------------------------------------------------------------------------------------------------------------------
			} else if (message.sospfType == 1) {
//				System.out.println(" -- Received LSP -- ");

				for (LSA receivedLSA : message.lsaArray) {
					LSA currentLSA = router.lsd._store.get(receivedLSA.linkStateID);

					//if the incoming LSA is newer than current or new router
					if (!router.lsd._store.containsKey(receivedLSA.linkStateID) || (receivedLSA.lsaSeqNumber > currentLSA.lsaSeqNumber)) {
						//System.out.println("NLSA : " + message.lsaArray.lastElement().toString());
						router.lsd._store.put(receivedLSA.linkStateID, receivedLSA);

						sendLSP(receivedLSA);
					}
				}

                // message is Exit Packet
                //-----------------------------------------------------------------------------------------------------------------------------------------
			} else if (message.sospfType == 2) {
//			    System.out.println("INFO: Received a deletion request from " + message.srcIP + ".");

			    // Create the response packet
                SOSPFPacket confirmDisconnect = new SOSPFPacket();
                confirmDisconnect.sospfType = 2;
                confirmDisconnect.srcIP = router.rd.simulatedIPAddress;
                confirmDisconnect.srcProcessPort = router.rd.processPortNumber;
                confirmDisconnect.dstIP = message.srcIP;
                confirmDisconnect.srcProcessIP = router.rd.processIPAddress;

                // Send the packet
                outputStream = new ObjectOutputStream(this.socket.getOutputStream());
                outputStream.writeObject(confirmDisconnect);
                outputStream.close();

                // Find the port that it belongs to
                int i = 0;
                Link deadLink = null;
                for (Link l : router.ports) {
                    if (message.srcIP.equals(l.router2.simulatedIPAddress)) {
                        deadLink = l;
                        break;
                    }
                    else {
                        i++;
                    }
                }

                if (deadLink == null) {
                    System.out.println("ERROR: Could not find the port to delete.");
                    throw new RuntimeException();
                }

                Link confirmDead = router.ports.remove(i);
                System.out.println("INFO: Removed " + confirmDead.router2.simulatedIPAddress + " from ports." );

                // updateLSA actually updates the LSD and LSA. This must always run, so don't nest this
                //      inside sendLSP.
                LSA freshLSA = updateLSA(confirmDead, 1);
                sendLSP(freshLSA);
//                System.out.println(router.lsd._store.keySet());
            }

		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			try {
				inputStream.close();
				//outputStream.close();
				this.socket.close();
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	}
}
