package socs.network.message;

import java.io.Serializable;
import java.util.LinkedList;

@SuppressWarnings("serial")
public class LSA implements Serializable {

    //IP address of the router originate this LSA
    public String linkStateID;
    public int lsaSeqNumber = Integer.MIN_VALUE;

    public LinkedList<LinkDescription> links = new LinkedList<LinkDescription>();

    @Override
    public String toString() {
        StringBuffer sb = new StringBuffer();
        sb.append(linkStateID + ":").append(lsaSeqNumber + "\n");
        for (LinkDescription ld : links) {
            sb.append(ld);
        }
        sb.append("\n");
        return sb.toString();
    }

    public LSA addLink(LinkDescription ld) {
        links.add(ld);
        return this;
    }

    public LSA addLink(String r2_simIP, int r2_portNum, int r2_weight) {
        links.add(new LinkDescription(r2_simIP, r2_portNum, r2_weight));
        return this;
    }
}
