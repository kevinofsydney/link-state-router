package socs.network.node;

public class RouterDescription {

    //used for socket communication
    String processIPAddress;

    //used to identify the router in the simulated network space
    String simulatedIPAddress;

    short processPortNumber;

    //status of the router
    RouterStatus status;
}
