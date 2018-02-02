package socs.network.node;

import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;

import socs.network.message.SOSPFPacket;

public class ServerThread extends Thread {

	Socket mySocket;
	Router myRouter;

	ServerThread(Socket socket, Router router) {
		mySocket = socket;
		myRouter = router;
	}

	public void run() {

		ObjectInputStream inputStream = null;
		ObjectOutputStream outputStream = null;

		try {
			
			inputStream = new ObjectInputStream(mySocket.getInputStream()); //Throws Exceptions
			
			SOSPFPacket message = (SOSPFPacket) inputStream.readObject(); //Throws Exceptions
			
			// -----------------------------------------------------------------------------------------------------------------------------------------
			// check if message is HELLO message
			if (message.sospfType == 0) {
				
				System.out.println("received HELLO from " + message.neighborID + ";");
				
				// message.neighborID identifies sender of packet
				
				// add a neighbour
				String senderSimIPAddress = message.neighborID;
				boolean addNeighbour = true;
				
				// check to see if sender is already a neighbour
				for ( Link currLink : myRouter.ports) {
					// sender is already a neighbour (dont want to addNeighbour)
					if (currLink.router2.simulatedIPAddress.equals(senderSimIPAddress)) {
						addNeighbour = false;
					}
				}
				
				// if sender is not already a neighbour, then add as neighbour
				if ( addNeighbour ) {
					
					// add a new Neighbour to myRouter
					if (myRouter.ports.size() < 4) {
						// create new RouterDescription for Neighbour
						RouterDescription remoteRouter = new RouterDescription();
						remoteRouter.processIPAddress = message.srcProcessIP;
						remoteRouter.simulatedIPAddress = senderSimIPAddress;
						remoteRouter.processPortNumber = message.srcProcessPort;
						remoteRouter.status = RouterStatus.INIT;

						myRouter.ports.add(new Link(myRouter.rd, remoteRouter));
						
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
				outputStream = new ObjectOutputStream(mySocket.getOutputStream()); //Throws Exceptions
				outputStream.writeObject(response); //Throws Exceptions

				// get response from client
				response = (SOSPFPacket) inputStream.readObject(); //Throws Exceptions

				// confirm it is a HELLO response
				if (response.sospfType == 0) {
					System.out.println("received HELLO from " + response.neighborID + ";");
					// set myRouter.neighbourID.status to TWO_WAY
					for ( Link currLink : myRouter.ports) {
						// sender is already a neighbour (dont want to addNeighbour)
						if (currLink.router2.simulatedIPAddress.equals(senderSimIPAddress)) {
							currLink.router2.status = RouterStatus.TWO_WAY;
							System.out.println("set " + message.neighborID + " state to TWO_WAY;");
						// Error: sender not a neighbour
						} else {
							System.out.println("Error: Setting sender status to TWO_WAY, sender not a Neighbour");
						}
					}
				}
				
			// END Server HELLO Response 
			//-----------------------------------------------------------------------------------------------------------------------------------------
			} else {
				System.out.println("Wrong message");
			}
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
