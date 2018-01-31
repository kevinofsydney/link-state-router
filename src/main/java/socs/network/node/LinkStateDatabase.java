package socs.network.node;

import socs.network.message.LSA;
import socs.network.message.LinkDescription;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Map.Entry;

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
		HashMap<String, String> edges = new HashMap<String, String>();

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
}

//weighted graph implementation
class Graph {

	private ArrayList<Node> nodes = new ArrayList<Node>();

	public void addNode(Node pNode) {
		nodes.add(pNode);
	}

	public void calculateShortestPathFromSource(Node source) {
		source.setDistance(0);

		HashSet<Node> closedSet = new HashSet<Node>();
		HashSet<Node> openSet = new HashSet<Node>();

		openSet.add(source);

		while (openSet.size() != 0) {
			// get first element in openSet
			Iterator<Node> iterator = openSet.iterator();
			Node currentNode = iterator.next();
			openSet.remove(currentNode);
			// evaluate all neighbours of currentNode
			for (Entry<Node, Integer> neighbours : currentNode.getNeighbours().entrySet()) {
				Node neighbour = neighbours.getKey();
				Integer weight = neighbours.getValue();
				if (!closedSet.contains(neighbour)) {
					calculateDistance(neighbour, weight, currentNode);
					openSet.add(neighbour);
				}
			}
			closedSet.add(currentNode);
		}
	}

	// evaluates or reevaluates distance to neighbours
	private void calculateDistance(Node target, Integer weight, Node source) {
		Integer sourceDistance = source.getDistance();
		if (sourceDistance + weight < target.getDistance()) {
			target.setDistance(sourceDistance + weight);
			LinkedList<Node> shortestPath = new LinkedList<Node>(source.getShortestPath());
			shortestPath.add(source);
			target.setShortestPath(shortestPath);
		}
	}
}

//Vertex of weighted graph
class Node {
	private String id;
	private LinkedList<Node> shortestPath = new LinkedList<Node>(); // shortest path from source node
	private Integer distance = Integer.MAX_VALUE; // distance from source node (infinity on init)
	private HashMap<Node, Integer> neighbours = new HashMap<Node, Integer>(); // map of neighbouring vertices with corresponding weights

	// constructor
	public Node(String id) {
		this.id = id;
	}

	// set neighbours
	public void addNeighbour(Node destination, int weight) {
		neighbours.put(destination, weight);
	}

	// getters and setters
	public String getId() {
		return id;
	}

	public LinkedList<Node> getShortestPath() {
		return shortestPath;
	}

	public void setShortestPath(LinkedList<Node> shortestPath) {
		this.shortestPath = shortestPath;
	}

	public Integer getDistance() {
		return distance;
	}

	public void setDistance(Integer distance) {
		this.distance = distance;
	}

	public HashMap<Node, Integer> getNeighbours() {
		return neighbours;
	}

	public String getShortestPathString() {
		String path = "";
		Iterator<Node> it = shortestPath.iterator();
		while (it.hasNext()) {
			path += it.next().id;
			if (it.hasNext()) {
				path += " -> ";
			}
		}
		path += " -> " + this.id + " [distance = " + this.distance + "]";
		return path;

	}
}
