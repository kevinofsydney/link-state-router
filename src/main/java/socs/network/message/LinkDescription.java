package socs.network.message;

import java.io.Serializable;

@SuppressWarnings("serial")
public class LinkDescription implements Serializable {
	public String linkID;
	public int portNum;
	public int tosMetrics;

	public String toString() {
		return linkID + "," + portNum + "," + tosMetrics;
	}

	public LinkDescription() {}

	public LinkDescription(String linkID, int portNum, int tosMetrics) {
		this.linkID = linkID;
		this.portNum = portNum;
		this.tosMetrics = tosMetrics; //the weight of the link
	}
}
