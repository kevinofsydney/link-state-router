package socs.network.node;

import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;

import socs.network.message.SOSPFPacket;

public class ClientThread extends Thread {

	private Socket mySocket;
	private Router myRouter;

	public ClientThread(Socket socket, Router router) {
		mySocket = socket;
		myRouter = router;
	}

	public void run() {

		ObjectInputStream inputStream = null;
		ObjectOutputStream outputStream = null;

		try {
	
			outputStream = new ObjectOutputStream(mySocket.getOutputStream());
			
			// create new SOSPF packet with HELLO message
			SOSPFPacket message = new SOSPFPacket();
			message.sospfType = 0;
			message.neighborID = myRouter.rd.simulatedIPAddress;
			message.srcProcessIP = myRouter.rd.processIPAddress;
			message.srcProcessPort = myRouter.rd.processPortNumber;
			message.neighborID = myRouter.rd.simulatedIPAddress;
			
			// send packet	
			outputStream.writeObject(message); //Throws Exceptions

			// get response
			inputStream = new ObjectInputStream(mySocket.getInputStream()); 
			SOSPFPacket response = (SOSPFPacket) inputStream.readObject();
			
			// check response is of sospfType HELLO
			if (response.sospfType == 0) {
				System.out.println("received HELLO from " + response.neighborID);
				
				// set server status to TWO_WAY
				for ( Link currLink : myRouter.ports) {
					// sender is already a neighbour (dont want to addNeighbour)
					if (currLink.router2.simulatedIPAddress.equals(response.neighborID)) {
						currLink.router2.status = RouterStatus.TWO_WAY;
						System.out.println("set " + message.neighborID + " state to TWO_WAY;");
					// Error: sender not a neighbour
					} else {
						System.out.println("Error: Setting sender status to TWO_WAY, sender not a Neighbour");
					}
				}
			}
			
			// send HELLO again
			outputStream.writeObject(message);	
			
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
