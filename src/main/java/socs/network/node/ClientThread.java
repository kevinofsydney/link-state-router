package socs.network.node;

import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;

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
			inputStream = new ObjectInputStream(mySocket.getInputStream());
			outputStream = new ObjectOutputStream(mySocket.getOutputStream());

		} catch (Exception e) {
			e.printStackTrace();
		}
	}

}
