package socs.network.message;

import java.io.*;
import java.util.Vector;
import java.util.concurrent.atomic.AtomicInteger;

@SuppressWarnings({ "serial", "unused" })
public class SOSPFPacket implements Serializable {

  //for inter-process communication
  public String srcProcessIP;
  public short srcProcessPort;
  public int srcWeight;

  //simulated IP address
  public String srcIP;
  public String dstIP;

  //common header
  public short sospfType; //0 - HELLO, 1 - LinkState Update, 2 - Quit packet
  public String routerID;

  //used by HELLO message to identify the sender of the message
  //e.g. when router A sends HELLO to its neighbor, it has to fill this field with its own
  //simulated IP address
  public String neighborID; //neighbor's simulated IP address

  //used by LSAUPDATE
  public Vector<LSA> lsaArray = new Vector<LSA>();

}
