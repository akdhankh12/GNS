/*
 * Copyright (C) 2013
 * University of Massachusetts
 * All Rights Reserved 
 */
package edu.umass.cs.gns.localnameserver;

import edu.umass.cs.gns.nameserver.NameRecordKey;
import edu.umass.cs.gns.packet.DNSPacket;

/**************************************************************
 * This class represents a data structure to store information
 * about queries (name lookup) transmitted by the local name
 * server
 * 
 *************************************************************/
public class DNSRequestInfo {


  /** Unique ID for each query **/
  private int id;
  /** Host/domain name in the query **/
  private String qName;
  /** System time when query was transmitted from the local name server **/
  private long lookupRecvdTime;
  /** System time when a response for this query was received at a local name server.**/
  private long recvTime = -1;

  private DNSPacket incomingPacket;
  public int numRestarts;

  private int nameserverID;

  /**************************************************************
   * Constructs a QueryInfo object with the following parameters
   * @param id Query id
   * @param name Host/Domain name
   * @param time System time when query was transmitted
   * @param nameserverID Response name server ID
   * @param queryStatus Query Status
   **************************************************************/
  public DNSRequestInfo(int id, String name, NameRecordKey recordKey, long time,
          int nameserverID, String queryStatus, int lookupNumber,
          DNSPacket incomingPacket, int numRestarts) {
    this.id = id;
    this.qName = name;
    this.lookupRecvdTime = time;

    this.incomingPacket = incomingPacket;
    this.numRestarts = numRestarts;
    this.nameserverID = nameserverID;
  }


  public synchronized void setNameserverID(int nameserverID1) {
    nameserverID = nameserverID1;

  }

  /**
   *  
   * @return
   */
  public long getLookupRecvdTime() {
    return lookupRecvdTime;
  }

  /**************************************************************
   * Returns a String representation of QueryInfo
   **************************************************************/
  @Override
  public String toString() {
    StringBuilder str = new StringBuilder();
    str.append("ID:" + getId());
    str.append(" Name:" + getqName());
    str.append(" Key:NA");// + qRecordKey.getName());str
    str.append(" Time:" + lookupRecvdTime);
    str.append(" Transmission:NA");
    return str.toString();
  }

  public synchronized String getLookupStats() {
    //Response Information: Time(ms) ActiveNS Ping(ms) Name NumTransmission LNS Timestamp(systime)
    StringBuilder str = new StringBuilder();
    str.append("0\t");
    str.append(incomingPacket.getKey().getName() + "\t");
    str.append(getqName());
    str.append("\t" + (getRecvTime() - lookupRecvdTime));
    str.append("\t0");
    str.append("\t0");
    str.append("\t0");
    str.append("\t0");
    str.append("\t" + nameserverID);
    str.append("\t" + LocalNameServer.getNodeID());
    str.append("\t" + lookupRecvdTime);
    str.append("\t" + numRestarts);
    str.append("\t[]");
    str.append("\t[]");

    //save response time
    String stats = str.toString();
    return stats;
  }

  /**
   * 
   * @param receiveTIme
   * @return
   */
  public synchronized void setRecvTime(long receiveTIme) {
    if (this.getRecvTime() == -1) {
      this.recvTime = receiveTIme;
    }
  }

  public synchronized long getResponseTime() {
    if (this.getRecvTime() == -1) {
      return -1L;
    }
    return (this.getRecvTime() - this.lookupRecvdTime);
  }

  /**
   * @return the id
   */
  public int getId() {
    return id;
  }

  /**
   * @return the qName
   */
  public String getqName() {
    return qName;
  }

  /**
   * @return the recvTime
   */
  public long getRecvTime() {
    return recvTime;
  }

  /**
   * @return the incomingPacket
   */
  public DNSPacket getIncomingPacket() {
    return incomingPacket;
  }
}