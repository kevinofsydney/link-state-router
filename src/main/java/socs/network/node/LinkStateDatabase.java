package socs.network.node;

import socs.network.message.LSA;
import socs.network.message.LinkDescription;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;


public class LinkStateDatabase {

	// linkID => LSAInstance
	HashMap<String, LSA> _store = new HashMap<String, LSA>();

	private RouterDescription rd = null;

	public LinkStateDatabase(RouterDescription routerDescription) {
		rd = routerDescription;
		LSA l = initLinkStateDatabase();
		_store.put(l.linkStateID, l);
	}

	/**
	 * output the shortest path from this router to the destination with the
	 * given IP address
	 */
	String getShortestPath(String destinationIP) {

		String toReturn = "";
		HashSet<String> openSet = new HashSet<String>(); //nodes that still need to be evaluated
		HashSet<String> closedSet = new HashSet<String>(); //nodes already evaluated
		HashMap<String, LinkedList<String>> shortestPath = new HashMap<String, LinkedList<String>>(); //structure keeps track of shortestPath for each node
		HashMap<String, Integer> distance = new HashMap<String, Integer>(); //structure keeps track of distance to each node
		
		distance.put(rd.simulatedIPAddress, 0); // set source node distance to 0

		// add all ip addresses to openSet
		for (String linkID : _store.keySet()) {
			openSet.add(linkID);
			// set all distances to infinity as per Dijkstras algorithm
			distance.put(linkID, Integer.MAX_VALUE); 
			
		}

		// dijkstras algorithm
		while (openSet.size() != 0) {
			Iterator<String> iterator = openSet.iterator();
			String currentNode = iterator.next();
			openSet.remove(currentNode);

			for (LinkDescription neighbour : _store.get(currentNode).links) {
				int weight = neighbour.tosMetrics;
				String target = neighbour.linkID;
				if (!closedSet.contains(target)) {
					Integer distanceFromSource = distance.get(currentNode);
					Integer distanceToNeighbour = distance.get(target);
					if (distanceFromSource + weight < distanceToNeighbour) {
						distance.put(target, distanceFromSource + weight);
						LinkedList<String> path = new LinkedList<String>(shortestPath.get(currentNode));
						path.add(currentNode);
						shortestPath.put(target, path);
					}
					openSet.add(target);
				}
			}
			closedSet.add(currentNode);
		}

		// Build and return the string with the shortest path
		LinkedList<String> pathToReturn = new LinkedList<String>(shortestPath.get(destinationIP));

		Iterator<String> it = pathToReturn.iterator();
		while (it.hasNext()) {
			toReturn += it.next();
		    if (it.hasNext()) {
		    		toReturn += " -> ";
		    }
		}
		toReturn += " -> "  + destinationIP + " [distance = " + distance.get(destinationIP) + "]";
		return toReturn;
	}

	// initialize the linkstate database by adding an entry about the router itself
	private LSA initLinkStateDatabase() {
		LSA lsa = new LSA();
		lsa.linkStateID = rd.simulatedIPAddress;
		lsa.lsaSeqNumber = Integer.MIN_VALUE;
		LinkDescription ld = new LinkDescription();
		ld.linkID = rd.simulatedIPAddress;
		ld.portNum = -1;
		ld.tosMetrics = 0;
		lsa.links.add(ld);
		return lsa;
	}

	public String toString() {
		StringBuilder sb = new StringBuilder();
		for (LSA lsa : _store.values()) {
			sb.append(lsa.linkStateID).append("(" + lsa.lsaSeqNumber + ")").append(":\t");
			for (LinkDescription ld : lsa.links) {
				sb.append(ld.linkID).append(",").append(ld.portNum).append(",").append(ld.tosMetrics).append("\t");
			}
			sb.append("\n");
		}
		return sb.toString();
	}

	public boolean addLSA(LSA newLSA) {

	    // Check if the LSD already contains a link with the same ID
	    if (_store.containsKey(newLSA.linkStateID)) {

	        // Check if the newLSA is more recent than the one stored
	        if (_store.get(newLSA.linkStateID).lsaSeqNumber >= newLSA.lsaSeqNumber)
	            return false;
        }

        // Store the LSA
        _store.put(newLSA.linkStateID, newLSA);
	    return true;
    }
}
