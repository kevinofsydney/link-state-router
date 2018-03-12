package socs.network.node;

import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.net.Socket;

import socs.network.message.LSA;
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
				System.out.println("\nreceived HELLO from " + response.neighborID);
				
				// set server status to TWO_WAY
				for ( Link currLink : myRouter.ports) {
					// sender is already a neighbour (dont want to addNeighbour)
					if (currLink.router2.simulatedIPAddress.equals(response.neighborID)) {
						currLink.router2.status = RouterStatus.TWO_WAY;
						System.out.println("set " + response.neighborID + " state to TWO_WAY;");
					}
				}
			}
			
			// send HELLO again
			outputStream.writeObject(message);

            // Called when a connection status becomes TWO_WAY
            // Create the link description (LD), create a link state advertisement (LSA), and add the LD to the LSA
            LSA initLSA = new LSA();
            initLSA.lsaSeqNumber = myRouter.lsaSeqNum_counter++;
            initLSA.linkStateID = myRouter.rd.simulatedIPAddress;

            // Add all of the router's links to the LSA
            for (Link link : myRouter.ports) {
                initLSA.addLink(link.router2.simulatedIPAddress, link.router2.processPortNumber, link.weight);
            }

            // Add the link state advertisement to the link state database
            myRouter.lsd.addLSA(initLSA);

            // For each active TWO_WAY connection, create a link state packet (LSP) and send it
            for (Link link : myRouter.ports) {
                if (link.router2.status == RouterStatus.TWO_WAY) {
                    SOSPFPacket LSP = new SOSPFPacket();
                    LSP.srcProcessIP = link.router1.processIPAddress;
                    LSP.srcProcessPort = link.router1.processPortNumber;
                    LSP.srcIP = link.router1.simulatedIPAddress;
                    LSP.dstIP = link.router2.simulatedIPAddress;
                    LSP.sospfType = 1;
                    LSP.routerID = link.router1.simulatedIPAddress;
                    //LSP.neighborID = link.router2.simulatedIPAddress;
                    LSP.lsaArray.add(initLSA);

                    // Send the packet to each neighbour
                    // outputStream.writeObject(LSP); I think you know more about this one that I do Toni, sorry bro
                }
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
