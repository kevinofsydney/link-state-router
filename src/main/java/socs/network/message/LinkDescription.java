package socs.network.message;

import java.io.Serializable;
import java.util.LinkedList;

public class LinkDescription implements Serializable {
	public String linkID;
	public int portNum;
	public int tosMetrics;
	public LinkedList<String> shortestPath;

	public String toString() {
		return linkID + "," + portNum + "," + tosMetrics;
	}
}
