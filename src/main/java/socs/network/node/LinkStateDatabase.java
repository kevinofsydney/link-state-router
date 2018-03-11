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
		
		WeightedGraph g = new WeightedGraph();
		// index integers
		int i = 0;
		int j = 0;
	
		// add all ip addresses to openSet and weighted graph
		for (String linkID : _store.keySet()) {
			g.myID[i++] = linkID;
			openSet.add(linkID);
			// set all distances to infinity as per Dijkstras algorithm
			distance.put(linkID, Integer.MAX_VALUE); 
			
		}

		i = 0; //reset
		
		// dijkstras algorithm
		while (openSet.size() != 0) {
			Iterator<String> iterator = openSet.iterator();
			String currentNode = iterator.next();
			openSet.remove(currentNode);

			for (LinkDescription neighbour : _store.get(currentNode).links) {
				int weight = neighbour.tosMetrics;
				String target = neighbour.linkID;
				g.edges[i][j] = weight;
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
				j++;
			}
			i++;
			closedSet.add(currentNode);
		}

		// Build and return the string with the shortest path
		LinkedList<String> pathToReturn = new LinkedList<String>(shortestPath.get(destinationIP));

		for (int k = 0; k < pathToReturn.size() + 1; k++) {
			// first string has no weight
			if ( k == 0) {
				toReturn += pathToReturn.get(k);
				// add arrow and weight to string
			} else {
				String nextNode = pathToReturn.get(k);
				// if done traversing back traces
				if ( k == pathToReturn.size() ) {
					nextNode = destinationIP;
				}
				
				// determine edge weight
				int edgeWeight;
				if ( k == 1) {
					edgeWeight = distance.get(nextNode);
				} else {
					edgeWeight = distance.get(nextNode) - distance.get(pathToReturn.get(k-1));
				}
			
				toReturn += "->(" + edgeWeight + ") " + nextNode;
			} 
		}
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

class WeightedGraph{
	int [][] edges; //index stands for the two nodes of the links, the value of edge is linkWeight
	String [] myID; // Store the simulatedIP for each vertex(router)
}
