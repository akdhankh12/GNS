/*
 *
 *  Copyright (c) 2015 University of Massachusetts
 *
 *  Licensed under the Apache License, Version 2.0 (the "License"); you
 *  may not use this file except in compliance with the License. You
 *  may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or
 *  implied. See the License for the specific language governing
 *  permissions and limitations under the License.
 *
 *  Initial developer(s): Westy, arun
 *
 */
package edu.umass.cs.gnsserver.gnsapp;

import edu.umass.cs.contextservice.integration.ContextServiceGNSClient;
import edu.umass.cs.contextservice.integration.ContextServiceGNSInterface;
import edu.umass.cs.gigapaxos.interfaces.AppRequestParserBytes;
import edu.umass.cs.gigapaxos.interfaces.ClientMessenger;
import edu.umass.cs.gigapaxos.interfaces.ClientRequest;
import edu.umass.cs.gigapaxos.interfaces.Replicable;
import edu.umass.cs.gigapaxos.interfaces.Request;
import edu.umass.cs.gigapaxos.interfaces.RequestIdentifier;
import edu.umass.cs.gnscommon.exceptions.client.ClientException;
import edu.umass.cs.gnsserver.activecode.ActiveCodeHandler;
import edu.umass.cs.gnsserver.database.ColumnField;
import edu.umass.cs.gnsserver.database.MongoRecords;
import edu.umass.cs.gnscommon.exceptions.server.FailedDBOperationException;
import edu.umass.cs.gnscommon.exceptions.server.FieldNotFoundException;
import edu.umass.cs.gnscommon.exceptions.server.RecordExistsException;
import edu.umass.cs.gnscommon.exceptions.server.RecordNotFoundException;
import edu.umass.cs.gnsserver.database.NoSQLRecords;
import edu.umass.cs.gnsserver.main.GNSConfig;
import edu.umass.cs.gnsserver.main.GNSConfig.GNSC;
import edu.umass.cs.gnsserver.gnsapp.clientCommandProcessor.ListenerAdmin;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import org.json.JSONException;
import org.json.JSONObject;
import edu.umass.cs.gnsserver.nodeconfig.GNSConsistentReconfigurableNodeConfig;
import edu.umass.cs.gnsserver.nodeconfig.GNSNodeConfig;
import edu.umass.cs.gnsserver.gnsapp.clientCommandProcessor.ClientRequestHandler;
import edu.umass.cs.gnsserver.gnsapp.clientCommandProcessor.ClientRequestHandlerInterface;
import edu.umass.cs.gnsserver.gnsapp.clientCommandProcessor.commandSupport.Admintercessor;
import edu.umass.cs.gnsserver.gnsapp.clientCommandProcessor.commandSupport.CommandHandler;
import edu.umass.cs.gnsserver.gnsapp.packet.BasicPacketWithClientAddress;
import edu.umass.cs.gnsserver.gnsapp.packet.CommandPacket;
import edu.umass.cs.gnscommon.CommandValueReturnPacket;
import edu.umass.cs.gnsserver.gnsapp.packet.NoopPacket;
import edu.umass.cs.gnsserver.gnsapp.packet.Packet;
import edu.umass.cs.gnsserver.gnsapp.packet.SelectRequestPacket;
import edu.umass.cs.gnsserver.gnsapp.packet.SelectResponsePacket;
import edu.umass.cs.gnsserver.gnsapp.packet.StopPacket;
import edu.umass.cs.gnsserver.gnsapp.packet.Packet.PacketType;
import edu.umass.cs.gnsserver.gnsapp.recordmap.BasicRecordMap;
import edu.umass.cs.gnsserver.gnsapp.recordmap.GNSRecordMap;
import edu.umass.cs.gnsserver.gnsapp.recordmap.NameRecord;
import edu.umass.cs.gnsserver.httpserver.GNSHttpServer;
import edu.umass.cs.gnsserver.utils.ValuesMap;
import edu.umass.cs.nio.JSONMessenger;
import edu.umass.cs.nio.JSONPacket;
import edu.umass.cs.nio.MessageExtractor;
import edu.umass.cs.nio.interfaces.IntegerPacketType;
import edu.umass.cs.nio.interfaces.SSLMessenger;
import edu.umass.cs.nio.interfaces.Stringifiable;
import edu.umass.cs.nio.nioutils.NIOHeader;
import edu.umass.cs.reconfiguration.examples.AbstractReconfigurablePaxosApp;
import edu.umass.cs.reconfiguration.interfaces.Reconfigurable;
import edu.umass.cs.reconfiguration.interfaces.ReconfigurableNodeConfig;
import edu.umass.cs.reconfiguration.interfaces.ReconfigurableRequest;
import edu.umass.cs.reconfiguration.reconfigurationutils.RequestParseException;
import edu.umass.cs.utils.Config;
import edu.umass.cs.utils.DelayProfiler;
import edu.umass.cs.utils.GCConcurrentHashMap;
import edu.umass.cs.utils.GCConcurrentHashMapCallback;
import edu.umass.cs.utils.Util;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.logging.Level;

/**
 * @author Westy, arun
 */
public class GNSApp extends AbstractReconfigurablePaxosApp<String>
        implements GNSApplicationInterface<String>, Replicable, Reconfigurable,
        ClientMessenger, AppRequestParserBytes {

  private String nodeID;
  private GNSConsistentReconfigurableNodeConfig<String> nodeConfig;
  private boolean constructed = false;
  /**
   * Object provides interface to the database table storing name records
   */
  private BasicRecordMap nameRecordDB;
  /**
   * The Nio server
   */
  private SSLMessenger<String, JSONObject> messenger;
  private ClientRequestHandlerInterface requestHandler;

  // Keep track of commands that are coming in
  /**
   * arun: I am not sure {@link #outStandingQueries} is really needed. It is
   * only being used in CommandHandler.handleCommandReturnValuePacketForApp
   * that invokes sendClient, but sendClient internally uses
   * {@link #outstanding} below that only needs the response, nothing else. If
   * you need to preserve the !DELEGATE_CLIENT_MESSAGING options, you could
   * read from {@link #outstanding} instead; it is designed to
   * auto-garbage-collect.
   */
//  @Deprecated
//  public final ConcurrentMap<Long, CommandRequestInfo> outStandingQueries
//          = new ConcurrentHashMap<>(10, 0.75f, 3);
  private static final long DEFAULT_REQUEST_TIMEOUT = 8000;
  private final GCConcurrentHashMap<Long, Request> outstanding = new GCConcurrentHashMap<>(
          new GCConcurrentHashMapCallback() {
    @Override
    public void callbackGC(Object key, Object value) {
    }
  }, DEFAULT_REQUEST_TIMEOUT);
  /**
   * Active code handler
   */
  private ActiveCodeHandler activeCodeHandler;

  /**
   * context service interface
   */
  private ContextServiceGNSInterface contextServiceGNSClient;

  /**
   * Constructor invoked via reflection by gigapaxos.
   *
   * @param args
   * @throws IOException
   */
  public GNSApp(String[] args) throws IOException {
    AppReconfigurableNode.initOptions(args);
  }

  /**
   * Actually creates the application. This strange way of constructing the application
   * is because of legacy code that used the createAppCoordinator interface.
   *
   * @param messenger
   * @throws java.io.IOException
   */
  private void GnsAppConstructor(JSONMessenger<String> messenger) throws IOException {
    this.nodeID = messenger.getMyID();
    GNSNodeConfig<String> gnsNodeConfig = new GNSNodeConfig<>();
    this.nodeConfig = new GNSConsistentReconfigurableNodeConfig<>(gnsNodeConfig);

    NoSQLRecords noSqlRecords;
    try {
      Class<?> clazz = AppReconfigurableNodeOptions.getNoSqlRecordsClass();
      Constructor<?> constructor = clazz.getConstructor(String.class, int.class);
      noSqlRecords = (NoSQLRecords) constructor.newInstance(nodeID, AppReconfigurableNodeOptions.mongoPort);
      GNSConfig.getLogger().info("Created noSqlRecords class: " + clazz.getName());
    } catch (NoSuchMethodException | SecurityException | IllegalAccessException | IllegalArgumentException | InstantiationException | InvocationTargetException e) {
      // fallback plan
      GNSConfig.getLogger().warning("Problem creating noSqlRecords from config:" + e);
      noSqlRecords = new MongoRecords(nodeID, AppReconfigurableNodeOptions.mongoPort);
    }
    this.nameRecordDB = new GNSRecordMap<>(noSqlRecords, MongoRecords.DBNAMERECORD);
    GNSConfig.getLogger().log(Level.FINE, "App {0} created {1}",
            new Object[]{nodeID, nameRecordDB});
    this.messenger = messenger;
    // Create the admin object
    Admintercessor admintercessor = new Admintercessor();
    // Create the request handler
    this.requestHandler = new ClientRequestHandler(
            admintercessor,
            new InetSocketAddress(nodeConfig.getBindAddress(this.nodeID),
                    this.nodeConfig.getCcpPort(this.nodeID)),
            nodeID, this,
            gnsNodeConfig);
    // Finish admin setup
    ListenerAdmin ccpListenerAdmin = new ListenerAdmin(requestHandler);
    ccpListenerAdmin.start();
    admintercessor.setListenerAdmin(ccpListenerAdmin);
    new AppAdmin(this, gnsNodeConfig).start();
    GNSConfig.getLogger().log(Level.INFO,
            "{0} Admin thread initialized", nodeID);
    // Should add this to the shutdown method - do we have a shutdown method?
    GNSHttpServer httpServer = new GNSHttpServer(requestHandler);
    this.activeCodeHandler = AppReconfigurableNodeOptions.enableActiveCode ? new ActiveCodeHandler(this,
            AppReconfigurableNodeOptions.activeCodeWorkerCount,
            AppReconfigurableNodeOptions.activeCodeBlacklistSeconds) : null;

    // context service init
    if (AppReconfigurableNodeOptions.enableContextService) {
      String[] parsed = AppReconfigurableNodeOptions.contextServiceIpPort.split(":");
      String host = parsed[0];
      int port = Integer.parseInt(parsed[1]);
      GNSConfig.getLogger().fine("ContextServiceGNSClient initialization started");
      contextServiceGNSClient = new ContextServiceGNSClient(host, port);
      GNSConfig.getLogger().fine("ContextServiceGNSClient initialization completed");
    }

    constructed = true;
  }

  @Override
  @SuppressWarnings("unchecked")
  public void setClientMessenger(SSLMessenger<?, JSONObject> messenger) {
    this.messenger = (SSLMessenger<String, JSONObject>) messenger;
    this.nodeID = messenger.getMyID().toString();
    try {
      if (!constructed) {
        this.GnsAppConstructor((JSONMessenger<String>) messenger);
      }
    } catch (IOException e) {
      e.printStackTrace();
      GNSConfig.getLogger().severe("Unable to create app: exiting");
      System.exit(1);
    }
  }

  private static PacketType[] types = {
    PacketType.SELECT_REQUEST,
    PacketType.SELECT_RESPONSE,
    PacketType.STOP,
    PacketType.NOOP,
    PacketType.COMMAND,
    PacketType.COMMAND_RETURN_VALUE};

  /**
   * arun: The code below {@link #incrResponseCount(ClientRequest)} and
   * {@link #executeNoop(Request)} is for instrumentation only and will go
   * away soon.
   */
  private static final int RESPONSE_COUNT_THRESHOLD = 100;
  private static boolean doneOnce = false;

  private synchronized void incrResponseCount(ClientRequest response) {
    if (responseCount++ > RESPONSE_COUNT_THRESHOLD
            && response instanceof CommandValueReturnPacket
            && (!doneOnce && (doneOnce = true))) {
      try {
        this.cachedResponse = ((CommandValueReturnPacket) response)
                .toJSONObject();
      } catch (JSONException e) {
        // TODO Auto-generated catch block
        e.printStackTrace();
      }
    }
  }

  private static final boolean EXECUTE_NOOP_ENABLED = Config.getGlobalBoolean(GNSC.EXECUTE_NOOP_ENABLED);
  private int responseCount = 0;
  private JSONObject cachedResponse = null;

  private boolean executeNoop(Request request) {
    if (!EXECUTE_NOOP_ENABLED) {
      return false;
    }
    if (cachedResponse != null
            && request instanceof CommandPacket
            && ((CommandPacket) request).getCommandType().isRead() //&& ((CommandPacket) request).getCommandName().equals(GNSCommandProtocol.READ)
            ) {
      try {
        ((BasicPacketWithClientAddress) request)
                .setResponse(new CommandValueReturnPacket(cachedResponse)
                        .setClientRequestAndLNSIds(((ClientRequest) request)
                                .getRequestID()));
      } catch (JSONException e) {
        e.printStackTrace();
      }
      return true;
    }
    return false;
  }

  @SuppressWarnings("unchecked")
  // we explicitly check type
  @Override
  public boolean execute(Request request, boolean doNotReplyToClient) {
    boolean executed = false;
    if (executeNoop(request)) {
      return true;
    }
    try {
      Packet.PacketType packetType = request.getRequestType() instanceof Packet.PacketType
              ? (Packet.PacketType) request.getRequestType()
              : null;
      GNSConfig.getLogger().log(Level.FINE, "{0} &&&&&&& handling {1} ",
              new Object[]{this, request.getSummary()});
      Request prev = null;
      // arun: enqueue request, dequeue before returning
      if (request instanceof RequestIdentifier) {
        prev = this.outstanding.putIfAbsent(((RequestIdentifier) request).getRequestID(),
                request);
      } else {
        assert (false);
      }

      switch (packetType) {
        case SELECT_REQUEST:
          /* FIXED: arun: this needs to be a blocking call, otherwise you are violating
        	 * execute(.)'s semantics.
           */
          Select.handleSelectRequest(
                  (SelectRequestPacket<String>) request, this);
          break;
        case SELECT_RESPONSE:
          Select.handleSelectResponse(
                  (SelectResponsePacket<String>) request, this);
          break;
        // Keeping STOP and NOOP here because we have to return true below
        // because returning false might cause reexecution
        case STOP:
          break;
        case NOOP:
          break;
        case COMMAND:
          CommandHandler.handleCommandPacketForApp(
                  (CommandPacket) request, doNotReplyToClient, this);
          break;
        case COMMAND_RETURN_VALUE:
          CommandHandler.handleCommandReturnValuePacketForApp(
                  (CommandValueReturnPacket) request, doNotReplyToClient, this);
          break;
        default:
          GNSConfig.getLogger().log(Level.SEVERE,
                  " Packet type not found: {0}", request.getSummary());
          return false;
      }
      executed = true;

      // arun: always clean up all created state upon exiting
      if (request instanceof RequestIdentifier && prev == null) {
				GNSConfig
						.getLogger()
						.log(Level.FINE,
								"{0} dequeueing request {1}; response={2}",
								new Object[] {
										this,
										request.getSummary(),
										request instanceof ClientRequest
												&& ((ClientRequest) request)
														.getResponse() != null ? ((ClientRequest) request)
												.getResponse().getSummary()
												: null });
        this.outstanding.remove(request);
      }

    } catch (JSONException | IOException | ClientException e) {
      e.printStackTrace();
    } catch (FailedDBOperationException e) {
      // all database operations throw this exception, therefore we keep
      // throwing this exception upwards and catch this
      // here.
      // A database operation error would imply that the application
      // hasn't been able to successfully execute
      // the request. therefore, this method returns 'false', hoping that
      // whoever calls handleDecision would retry
      // the request.
      GNSConfig.getLogger().log(Level.SEVERE,
              "Error handling request: {0}", request.toString());
      e.printStackTrace();
    }

    return executed;
  }

  // For InterfaceApplication
  @Override
  public Request getRequest(String string)
          throws RequestParseException {
    GNSConfig.getLogger().log(Level.FINE,
            ">>>>>>>>>>>>>>> GET REQUEST: {0}", string);
    // Special case handling of NoopPacket packets
    if (Request.NO_OP.equals(string)) {
      return new NoopPacket();
    }
    try {
      long t = System.nanoTime();
      JSONObject json = new JSONObject(string);
      if (Util.oneIn(1000)) {
        DelayProfiler.updateDelayNano("jsonificationApp", t);
      }
      Request request = (Request) Packet.createInstance(json, nodeConfig);
      return request;
    } catch (JSONException e) {
      throw new RequestParseException(e);
    }
  }

  /**
   * This method avoids an unnecessary restringification (as is the case with
   * {@link #getRequest(String)} above) by decoding the JSON, stamping it with
   * the sender information, and then creating a packet out of it.
   *
   * @param msgBytes
   * @param header
   * @param unstringer
   * @return Request constructed from msgBytes.
   */
  public static Request getRequestStatic(byte[] msgBytes, NIOHeader header,
          Stringifiable<String> unstringer) {
    Request request = null;
    try {
      long t = System.nanoTime();
      JSONObject json = new JSONObject(
              JSONPacket.couldBeJSON(msgBytes) ? new String(msgBytes,
              NIOHeader.CHARSET) : new String(msgBytes,
              Integer.BYTES, msgBytes.length - Integer.BYTES,
              NIOHeader.CHARSET));
      MessageExtractor.stampAddressIntoJSONObject(header.sndr,
              header.rcvr, json);
      request = (Request) Packet.createInstance(json, unstringer);
      if (Util.oneIn(100)) {
        DelayProfiler.updateDelayNano("getRequest." + request.getClass().getSimpleName(), t);
      }
    } catch (JSONException | UnsupportedEncodingException e) {
      e.printStackTrace();
    }
    return request;
  }

  @Override
  public Request getRequest(byte[] msgBytes, NIOHeader header) {
    return getRequestStatic(msgBytes, header, nodeConfig);
  }

  @Override
  public Set<IntegerPacketType> getRequestTypes() {
    return new HashSet<>(Arrays.asList(types));
  }

  @Override
  public boolean execute(Request request) {
    return this.execute(request, false);
  }

  private final static ArrayList<ColumnField> curValueRequestFields = new ArrayList<>();

  static {
    curValueRequestFields.add(NameRecord.VALUES_MAP);
    //curValueRequestFields.add(NameRecord.TIME_TO_LIVE);
  }

  @Override
  public String checkpoint(String name) {
    try {
      NameRecord nameRecord = NameRecord.getNameRecord(nameRecordDB, name);
      GNSConfig.getLogger().log(Level.FINE,
              "&&&&&&& {0} getting state for {1} : {2} ",
              new Object[]{this, name, nameRecord.getValuesMap().getSummary()});
      return nameRecord.getValuesMap().toString();
    } catch (RecordNotFoundException e) {
      // normal result
    } catch (FieldNotFoundException e) {
      GNSConfig.getLogger().log(Level.SEVERE, "Field not found exception: {0}", e.getMessage());
      e.printStackTrace();
    } catch (FailedDBOperationException e) {
      GNSConfig.getLogger().log(Level.SEVERE, "State not read from DB: {0}", e.getMessage());
      e.printStackTrace();
    }
    return null;
  }

  /**
   * Updates the state for the given named record.
   *
   * @param name
   * @param state
   * @return true if we were able to updateEntireRecord the state
   */
  @Override
  public boolean restore(String name, String state) {
    GNSConfig.getLogger().log(Level.FINE,
            "&&&&&&& {0} updating {1} with state [{2}]",
            new Object[]{this, name, Util.truncate(state, 32, 32)});
    try {
      if (state == null) {
        // If state is null the only thing it means is that we need to delete 
        // the record. If the record does not exists this is just a noop.
        NameRecord.removeNameRecord(nameRecordDB, name);
      } else //state does not equal null so we either create a new record or update the existing one
      {
        if (!NameRecord.containsRecord(nameRecordDB, name)) {
          // create a new record
          try {
            ValuesMap valuesMap = new ValuesMap(new JSONObject(state));
            NameRecord nameRecord = new NameRecord(nameRecordDB, name, valuesMap);
            NameRecord.addNameRecord(nameRecordDB, nameRecord);
          } catch (RecordExistsException | JSONException e) {
            GNSConfig.getLogger().log(Level.SEVERE, "Problem updating state: {0}", e.getMessage());
          }
        } else { // update the existing record
          try {
            NameRecord nameRecord = NameRecord.getNameRecord(nameRecordDB, name);
            nameRecord.updateState(new ValuesMap(new JSONObject(state)));
          } catch (JSONException | FieldNotFoundException | RecordNotFoundException | FailedDBOperationException e) {
            GNSConfig.getLogger().log(Level.SEVERE, "Problem updating state: {0}", e.getMessage());
          }
        }
      }
      return true;
    } catch (FailedDBOperationException e) {
      GNSConfig.getLogger().log(Level.SEVERE, "Failed update exception: {0}", e.getMessage());
      e.printStackTrace();
    }
    return false;
  }

  /**
   * Returns a stop request packet.
   *
   * @param name
   * @param epoch
   * @return the stop request packet
   */
  @Override
  public ReconfigurableRequest getStopRequest(String name, int epoch) {
    return new StopPacket(name, epoch);
  }

  @Override
  public String getFinalState(String name, int epoch) {
    throw new RuntimeException("This method should not have been called");
  }

  @Override
  public void putInitialState(String name, int epoch, String state) {
    throw new RuntimeException("This method should not have been called");
  }

  @Override
  public boolean deleteFinalState(String name, int epoch) {
    throw new RuntimeException("This method should not have been called");
  }

  @Override
  public Integer getEpoch(String name) {
    throw new RuntimeException("This method should not have been called");
  }

  //
  // GnsApplicationInterface implementation
  //
  @Override
  public String getNodeID() {
    return nodeID;
  }

  @Override
  public BasicRecordMap getDB() {
    return nameRecordDB;
  }

  @Override
  public ReconfigurableNodeConfig<String> getGNSNodeConfig() {
    return nodeConfig;
  }

  protected static final boolean DELEGATE_CLIENT_MESSAGING = true;

  /**
   * Delegates client messaging to gigapaxos.
   *
   * @param responseJSON
   * @throws java.io.IOException
   */
  @Override
  public void sendToClient(Request response,
          JSONObject responseJSON)
          throws IOException {

    if (DELEGATE_CLIENT_MESSAGING) {
      assert (response instanceof ClientRequest);
      Request originalRequest = this.outstanding
              .remove(((RequestIdentifier) response).getRequestID());
      assert (originalRequest != null && originalRequest instanceof BasicPacketWithClientAddress) : ((ClientRequest) response).getSummary();
      if (originalRequest != null && originalRequest instanceof BasicPacketWithClientAddress) {
        ((BasicPacketWithClientAddress) originalRequest).setResponse((ClientRequest) response);
        incrResponseCount((ClientRequest) response);
      }
      GNSConfig.getLogger().log(Level.FINE,
              "{0} set response {1} for requesting client {2} for request {3}",
              new Object[]{
                this,
                response.getSummary(),
                ((BasicPacketWithClientAddress) originalRequest)
                .getClientAddress(), originalRequest.getSummary()});
      return;
    } // else

    /* arun: FIXED: You have just ignored the doNotReplyToClient flag
		 * here, which is against the spec of the implementation of the
		 * Replicable.execute(.) method. Alternatively, you could just delegate 
		 * client messaging to gigapaxos, but you are not doing that either.
		 * The current implementation will unnecessarily incur 3x client
		 * messaging overhead and can potentially cause bugs if clients rapidly 
		 * open and close connections.
		 * 
		 * 
		 * Alternatively, delegate client messaging to gigapaxos and you don't
		 * have to worry about keeping track of sender or listening addresses.
		 * For that we need the corresponding request here so that we can invoke
		 * request.setResponse(response) here. */
//    if (!disableSSL) {
//      GNSConfig.getLogger().log(Level.FINE,
//              "{0} sending back response to client {1} -> {2}",
//              new Object[]{this, response.getSummary(), isa});
//      messenger.getSSLClientMessenger().sendToAddress(isa, responseJSON);
//    } else {
//      messenger.getClientMessenger().sendToAddress(isa, responseJSON);
//    }
  }

  @Override
  public String toString() {
    return this.getClass().getSimpleName() + ":" + this.nodeID;
  }

  @Override
  // Currently only used by Select
  public void sendToID(String id, JSONObject msg) throws IOException {
    messenger.sendToID(id, msg);
  }

  @Override
  public ActiveCodeHandler getActiveCodeHandler() {
    return activeCodeHandler;
  }

  @Override
  public ClientRequestHandlerInterface getRequestHandler() {
    return requestHandler;
  }

  public ContextServiceGNSInterface getContextServiceGNSClient() {
    if (contextServiceGNSClient != null) {
      GNSConfig.getLogger().fine("getContextServiceClient non null ");
    } else if (AppReconfigurableNodeOptions.enableContextService) {
      GNSConfig.getLogger().fine("getContextServiceClient NULL ");
      assert (false);
    }
    return contextServiceGNSClient;
  }
}
