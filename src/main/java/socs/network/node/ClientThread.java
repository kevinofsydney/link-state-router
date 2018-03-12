package socs.network.node;

import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;

import socs.network.message.LSA;

import socs.network.message.SOSPFPacket;

public class ClientThread extends Thread {

	private Socket mySocket;
	private Router myRouter;
	private int srcWeight;

	public ClientThread(Socket socket, Router router, int weight) {
		mySocket = socket;
		myRouter = router;
		srcWeight = weight;
	}

	public void run() {

		ObjectInputStream inputStream = null;
		ObjectOutputStream outputStream = null;

		try {

			// create new SOSPF packet with HELLO message
			SOSPFPacket message = new SOSPFPacket();
			message.sospfType = 0;
			message.neighborID = myRouter.rd.simulatedIPAddress;
			message.srcProcessIP = myRouter.rd.processIPAddress;
			message.srcProcessPort = myRouter.rd.processPortNumber;
			message.neighborID = myRouter.rd.simulatedIPAddress;
			message.srcWeight = srcWeight;

			// send packet	
			outputStream = new ObjectOutputStream(mySocket.getOutputStream());
			outputStream.writeObject(message); //Throws Exceptions

			// get response
			inputStream = new ObjectInputStream(mySocket.getInputStream());
			SOSPFPacket response = (SOSPFPacket) inputStream.readObject();

			// check response is of sospfType HELLO
			if (response.sospfType == 0) {
				System.out.println("\nreceived HELLO from " + response.neighborID);

				// set server status to TWO_WAY
				for (Link currLink : myRouter.ports) {
					// sender is already a neighbour (dont want to addNeighbour)
					if (currLink.router2.simulatedIPAddress.equals(response.neighborID)) {
						currLink.router2.status = RouterStatus.TWO_WAY;
						System.out.println("set " + response.neighborID + " state to TWO_WAY;");
					}
				}
			}

			// send HELLO again
			outputStream = new ObjectOutputStream(mySocket.getOutputStream());
			outputStream.writeObject(message);

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

			// For each active TWO_WAY connection, create a link state packet (LSP) and send it
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
				outputStream = new ObjectOutputStream(clientSocket.getOutputStream());
				outputStream.writeObject(LSP);
				clientSocket.close();

			}
			// sent Link State Packet ----------------------------------------------------------------------------

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
