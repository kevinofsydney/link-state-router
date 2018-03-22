package socs.network.node;

import socs.network.message.LSA;
import socs.network.message.LinkDescription;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;

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
		HashMap<String, String> predecessor = new HashMap<String, String>(); //structure keeps track of shortestPath for each node
		HashMap<String, Integer> distance = new HashMap<String, Integer>(); //structure keeps track of distance to each node

		// add all ip addresses to openSet and weighted graph
		for (String linkID : _store.keySet()) {
			// set all distances to infinity as per Dijkstras algorithm
			distance.put(linkID, Integer.MAX_VALUE);

		}
		
		openSet.add(rd.simulatedIPAddress);
		distance.put(rd.simulatedIPAddress, 0);
		predecessor.put(rd.simulatedIPAddress, null);

		// dijkstras algorithm
		while (openSet.size() != 0) {
			String currentNode = getMinimum(openSet, distance);
			System.out.print("\nCURRENT " + currentNode);
			openSet.remove(currentNode);

			System.out.println(" LINKS " + _store.get(currentNode).links.toString());
			for (LinkDescription neighbour : _store.get(currentNode).links) {
				if (neighbour.portNum != -1) {
					int weight = neighbour.tosMetrics;
					String target = neighbour.linkID;
					if (!closedSet.contains(target)) {
						Integer distanceFromSource = distance.get(currentNode);
						Integer distanceToNeighbour = distance.get(target);
						if (distanceFromSource + weight < distanceToNeighbour) {
							System.out.println("Adding " + neighbour + " ");
							distance.put(target, distanceFromSource + weight);
							predecessor.put(target, currentNode);
						}
						openSet.add(target);
					}
				}
			}
			closedSet.add(currentNode);

		}

		// Build and return the string with the shortest path
		String current = destinationIP;
		List<String> shortestPath = new ArrayList<String>();
		while (current != null) {
			shortestPath.add(current);
			current = predecessor.get(current);
		}
		Collections.reverse(shortestPath);
		System.out.println(shortestPath.toString());

		for (int k = 0; k < shortestPath.size(); k++) {
			// first string has no weight
			if (k == 0) {
				toReturn += shortestPath.get(k);
				// add arrow and weight to string
			} else {
				String nextNode = shortestPath.get(k);

				// determine edge weight
				int edgeWeight;
				if (k == 1) {
					edgeWeight = distance.get(nextNode);
				} else {
					edgeWeight = distance.get(nextNode) - distance.get(shortestPath.get(k - 1));
				}

				toReturn += " ->(" + edgeWeight + ") " + nextNode;
			}
		}
		return toReturn;
	}

	// get minimum from openSet
    private String getMinimum(HashSet<String> openSet, HashMap<String, Integer> distance) {
        String minimum = null;
        
        for (String router : openSet) {

            //if minimum value has not been set yet then set first router to minimum
            minimum = (minimum == null || distance.get(router) < distance.get(minimum )) ? router : minimum;
        }
        return minimum;
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
				sb.append(ld.linkID).append(", ").append(ld.portNum).append(", ").append(ld.tosMetrics).append("\t");
			}
		}
		return sb.toString();
	}
}
