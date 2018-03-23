package socs.network.node;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;


import socs.network.message.SOSPFPacket;

public class Client extends ServerThread {

	private Link link;

	public Client(Socket socket, Router router, Link link) {
		super(socket, router);
		this.link = link;
	}

	public void run() {

		ObjectInputStream inputStream = null;
		ObjectOutputStream outputStream = null;

		try {

			// create new SOSPF packet with HELLO message
			SOSPFPacket message = new SOSPFPacket();
			message.sospfType = 0;
			message.neighborID = router.rd.simulatedIPAddress;
			message.srcProcessIP = router.rd.processIPAddress;
			message.srcProcessPort = router.rd.processPortNumber;
			message.neighborID = router.rd.simulatedIPAddress;
			message.srcWeight = link.weight;

			// send packet	
			outputStream = new ObjectOutputStream(socket.getOutputStream());
			outputStream.writeObject(message); //Throws Exceptions

			// get response
			inputStream = new ObjectInputStream(socket.getInputStream());
			SOSPFPacket response = (SOSPFPacket) inputStream.readObject();

			// check response is of sospfType HELLO
			if (response.sospfType == 0) {
				System.out.println("\nreceived HELLO from " + response.neighborID + ";");
				
				// set myRouter.neighbourID.status to TWO_WAY
				for (Link currLink : router.ports) {
					if (currLink.router2.simulatedIPAddress.equals(response.neighborID)) {

						currLink.router2.status = RouterStatus.TWO_WAY;
						System.out.println("set " + response.neighborID + " state to TWO_WAY;");
					}
				}

				// send HELLO again
				outputStream.writeObject(message);

				// send LSP
				sendLSP(updateLSA(link, 0));
			}
			
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			try {
				inputStream.close();
				outputStream.close();
				socket.close();
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	}

}
