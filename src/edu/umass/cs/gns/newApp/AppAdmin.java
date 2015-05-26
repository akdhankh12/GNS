/*
 * Copyright (C) 2014
 * University of Massachusetts
 * All Rights Reserved
 */
package edu.umass.cs.gns.newApp;

import edu.umass.cs.gns.util.Shutdownable;
import edu.umass.cs.gns.nsdesign.*;
import edu.umass.cs.gns.nodeconfig.GNSNodeConfig;
import edu.umass.cs.gns.newApp.clientCommandProcessor.commandSupport.AccountAccess;
import edu.umass.cs.gns.newApp.clientCommandProcessor.commandSupport.GuidInfo;
import edu.umass.cs.gns.database.AbstractRecordCursor;
import edu.umass.cs.gns.exceptions.FieldNotFoundException;
import edu.umass.cs.gns.main.GNS;
import edu.umass.cs.gns.newApp.packet.Packet;
import edu.umass.cs.gns.newApp.packet.admin.AdminRequestPacket;
import edu.umass.cs.gns.newApp.packet.admin.AdminResponsePacket;
import edu.umass.cs.gns.newApp.packet.admin.DumpRequestPacket;
import edu.umass.cs.gns.newApp.recordmap.NameRecord;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.logging.Level;

/**
 * A separate thread that runs in the NameServer that handles administrative (AKA non-data related, non-user)
 * type operations. All of the things in here are for server administration and debugging.
 */
@SuppressWarnings("unchecked")
public class AppAdmin extends Thread implements Shutdownable{

  /**
   * Socket over which active name server request arrive *
   */
  private ServerSocket serverSocket;

  private final GnsApplicationInterface app;

  private final GNSNodeConfig gnsNodeConfig;

  /**
   * Creates a new listener thread for handling response packet
   *
   * @throws IOException
   */
  
  public AppAdmin(GnsApplicationInterface app, GNSNodeConfig gnsNodeConfig) {
    super("NSListenerAdmin");
    this.app = app;
    this.gnsNodeConfig = gnsNodeConfig;
    try {
      this.serverSocket = new ServerSocket(gnsNodeConfig.getAdminPort(app.getNodeID()));
    } catch (IOException e) {
      GNS.getLogger().severe("Unable to create NSListenerAdmin server: " + e);
    }
  }

  /**
   * Start executing the thread.
   */
  @Override
  public void run() {
    int numRequest = 0;
    GNS.getLogger().info("NS Node " + app.getNodeID().toString() + " starting Admin Request Server on port " + serverSocket.getLocalPort());
    while (true) {
      try {
        Socket socket = serverSocket.accept();

        //Read the packet from the input stream
        JSONObject incomingJSON = Packet.getJSONObjectFrame(socket);
        switch (Packet.getPacketType(incomingJSON)) {

          case DUMP_REQUEST:

            DumpRequestPacket dumpRequestPacket = new DumpRequestPacket(incomingJSON, gnsNodeConfig);

            dumpRequestPacket.setPrimaryNameServer(app.getNodeID());
            JSONArray jsonArray = new JSONArray();
            // if there is an argument it is a TAGNAME we return all the records that have that tag
            if (dumpRequestPacket.getArgument() != null) {
              String tag = dumpRequestPacket.getArgument();
              AbstractRecordCursor cursor = NameRecord.getAllRowsIterator(app.getDB());
              while (cursor.hasNext()) {
                NameRecord nameRecord = null;
                JSONObject json = cursor.nextJSONObject();
                try {
                  nameRecord = new NameRecord(app.getDB(), json);
                } catch (JSONException e) {
                  GNS.getLogger().severe("Problem parsing json into NameRecord: " + e + " JSON is " + json.toString());
                }
                if (nameRecord != null) {
                  try {
                    if (nameRecord.containsKey(AccountAccess.GUID_INFO)) {
                      GuidInfo userInfo = new GuidInfo(nameRecord.getKeyAsArray(AccountAccess.GUID_INFO).toResultValueString());
                      if (userInfo.containsTag(tag)) {
                        jsonArray.put(nameRecord.toJSONObject());
                      }
                    }
                  } catch (FieldNotFoundException e) {
                    GNS.getLogger().severe("FieldNotFoundException. Field Name =  " + e.getMessage());
                    e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
                  }
                }
              }
              // OTHERWISE WE RETURN ALL THE RECORD
            } else {
              //for (NameRecord nameRecord : NameServer.getAllNameRecords()) {
              AbstractRecordCursor cursor = NameRecord.getAllRowsIterator(app.getDB());
              while (cursor.hasNext()) {
                NameRecord nameRecord = null;
                JSONObject json = cursor.nextJSONObject();
                try {
                  nameRecord = new NameRecord(app.getDB(), json);
                } catch (JSONException e) {
                  GNS.getLogger().severe("Problem parsing record cursor into NameRecord: " + e + " JSON is " + json.toString());
                }
                if (nameRecord != null) {
                  jsonArray.put(nameRecord.toJSONObject());
                }
              }
            }
            if (GNS.getLogger().isLoggable(Level.FINER)) {
              GNS.getLogger().finer("NSListenrAdmin for " + app.getNodeID() + " is " + jsonArray.toString());
            }
            dumpRequestPacket.setJsonArray(jsonArray);
            Packet.sendTCPPacket(dumpRequestPacket.toJSONObject(), 
                    dumpRequestPacket.getCCPAddress());
            
            GNS.getLogger().info("NSListenrAdmin: Response to id:" + dumpRequestPacket.getId() + " --> " + dumpRequestPacket.toString());
            break;
          case ADMIN_REQUEST:
            AdminRequestPacket adminRequestPacket = new AdminRequestPacket(incomingJSON);
            switch (adminRequestPacket.getOperation()) {
              case DELETEALLRECORDS:
                GNS.getLogger().fine("NSListenerAdmin (" + app.getNodeID() + ") : Handling DELETEALLRECORDS request");
                long startTime = System.currentTimeMillis();
                int cnt = 0;
                AbstractRecordCursor cursor = NameRecord.getAllRowsIterator(app.getDB());
                while (cursor.hasNext()) {
                  NameRecord nameRecord = new NameRecord(app.getDB(), cursor.nextJSONObject());
                  //for (NameRecord nameRecord : NameServer.getAllNameRecords()) {
                  try {
                    NameRecord.removeNameRecord(app.getDB(), nameRecord.getName());
                  } catch (FieldNotFoundException e) {
                    GNS.getLogger().severe("FieldNotFoundException. Field Name =  " + e.getMessage());
                    e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
                  }
                  //DBNameRecord.removeNameRecord(nameRecord.getName());
                  cnt++;
                }
                GNS.getLogger().fine("NSListenerAdmin (" + app.getNodeID() + ") : Deleting " + cnt + " records took "
                        + (System.currentTimeMillis() - startTime) + "ms");
                break;
              // Clears the database and reinitializes all indices
              case RESETDB:
                  // don't like this anyway
//                GNS.getLogger().fine("NSListenerAdmin (" + app.getNodeID() + ") : Handling RESETDB request");
//                replicaController.reset();
//                rcCoordinator.reset();
//                appCoordinator.reset();
//                app.reset();

                break;
              case PINGTABLE:
                Object node = adminRequestPacket.getArgument();
                if (node.equals(app.getNodeID())) {
                  JSONObject jsonResponse = new JSONObject();
                  jsonResponse.put("PINGTABLE", app.getPingManager().tableToString(app.getNodeID()));
                  AdminResponsePacket responsePacket = new AdminResponsePacket(adminRequestPacket.getId(), jsonResponse);
                  Packet.sendTCPPacket(responsePacket.toJSONObject(), adminRequestPacket.getCCPAddress());
                } else {
                  GNS.getLogger().warning("NSListenerAdmin wrong node for PINGTABLE!");
                }
                break;
              case PINGVALUE:
                Object node1 = adminRequestPacket.getArgument();
                Object node2 = adminRequestPacket.getArgument2();
                if (node1.equals(app.getNodeID())) {
                  JSONObject jsonResponse = new JSONObject();
                  jsonResponse.put("PINGVALUE", app.getPingManager().nodeAverage(node2));
                  AdminResponsePacket responsePacket = new AdminResponsePacket(adminRequestPacket.getId(), jsonResponse);
                  Packet.sendTCPPacket(responsePacket.toJSONObject(), adminRequestPacket.getCCPAddress());
                } else {
                  GNS.getLogger().warning("NSListenerAdmin wrong node for PINGVALUE!");
                }
                break;
              case CHANGELOGLEVEL:
                Level level = Level.parse(adminRequestPacket.getArgument());
                GNS.getLogger().info("Changing log level to " + level.getName());
                GNS.getLogger().setLevel(level);
                break;
              case CLEARCACHE:
                // shouldn't ever toString this
                GNS.getLogger().warning("NSListenerAdmin (" + app.getNodeID() + ") : Ignoring CLEARCACHE request");
                break;

            }
            break;
          case STATUS_INIT:
            break;
        }

        socket.close();
      } catch (Exception e) {
        if (serverSocket.isClosed())  {
          GNS.getLogger().warning("NS Admin shutting down.");
          return; // close this thread
        }
        e.printStackTrace();
      }
    }
  }

  /**
   * Closes the server socket in process of shutting down name server.
   * This unblocks the listening thread and and the listening thread shuts down.
   */
  @Override
  public void shutdown() {
    try {
      this.serverSocket.close();
    } catch (IOException e) {
      e.printStackTrace();
    }
  }
}
