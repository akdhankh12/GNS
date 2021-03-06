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
 *  Initial developer(s): Westy, arun, Emmanuel Cecchet
 *
 */
package edu.umass.cs.gnsclient.client;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import org.json.JSONException;
import org.json.JSONObject;

import edu.umass.cs.gigapaxos.interfaces.Request;
import edu.umass.cs.utils.Util;
import edu.umass.cs.gnsclient.client.GNSClientConfig.GNSCC;
import edu.umass.cs.gnsclient.client.android.AndroidNIOTask;
import edu.umass.cs.gnsserver.gnsapp.packet.CommandPacket;
import edu.umass.cs.gnscommon.CommandValueReturnPacket;
import edu.umass.cs.utils.Config;
import edu.umass.cs.utils.DelayProfiler;
import edu.umass.cs.gnscommon.exceptions.client.ClientException;
import edu.umass.cs.reconfiguration.reconfigurationpackets.ActiveReplicaError;
import edu.umass.cs.gnscommon.GNSCommandProtocol;
import edu.umass.cs.gnscommon.GNSResponseCode;
import edu.umass.cs.gnscommon.utils.CanonicalJSON;
import edu.umass.cs.gnscommon.utils.Format;
import edu.umass.cs.gnscommon.CommandType;

import java.io.UnsupportedEncodingException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.SignatureException;
import java.util.Date;
import java.util.concurrent.ExecutionException;
import java.util.logging.Level;

/**
 * The base for all GNS clients.
 *
 */
public abstract class AbstractGNSClient {

  /**
   * Indicates whether we are on an Android platform or not
   */
  public static final boolean IS_ANDROID
          = System.getProperty("java.vm.name").equalsIgnoreCase("Dalvik");

  /**
   * A string representing the GNS Server that we are connecting to.
   * NOTE THAT THIS STRING SHOULD BE DIFFERENT FOR DIFFERENT SERVERS (say
   * a local test server vs the one on EC2 otherwise the key pair storage
   * code overwrite keys with the same name that are being used for
   * different servers. This generally is only a problem during testing
   * when private keys might be created, deleted and recreated for the same
   * names used on multiple servers.
   */
  public final String GNSInstance;

  /**
   * The length of time we will wait for a command response from the server
   * before giving up.
   */
  // FIXME: We might need a separate timeout just for certain ops like 
  // gui creation that sometimes take a while
  // 10 seconds is too short on EC2 
  protected int readTimeout = 8000; // 20 seconds... was 40 seconds

  /* Keeps track of requests that are sent out and the reponses to them */
  private final ConcurrentMap<Long, Request> resultMap = new ConcurrentHashMap<Long, Request>(
          10, 0.75f, 3);
  /* Instrumentation: Keeps track of transmission start times */
  private final ConcurrentMap<Long, Long> queryTimeStamp = new ConcurrentHashMap<Long, Long>(10,
          0.75f, 3);
  /* Used to generate unique ids */
  private final Random randomID = new Random();
  /* Used by the wait/notify calls */
  private final Object monitor = new Object();

  // instrumentation
  private double movingAvgLatency;
  //private long lastLatency;
  private int totalAsynchErrors;

  private static boolean forceCoordinatedReads = false;

  private static final String DEFAULT_INSTANCE = "server.gns.name";

  /**
   */
  @Deprecated
  public AbstractGNSClient() {
    this(null);
    GNSClientConfig.getLogger().warning(
            "Initializing a GNS client without a reconfigurator address is deprecated");
  }

  /**
   * Creates a new <code>BasicUniversalTcpClient</code> object
   * Optionally disables SSL if disableSSL is true.
   *
   * @param anyReconfigurator
   */
  public AbstractGNSClient(InetSocketAddress anyReconfigurator) {
    this.GNSInstance = DEFAULT_INSTANCE;
    resetInstrumentation();
  }

  /**
   * Creates a command object from the given CommandType and a variable
   * number of key and value pairs with a signature parameter. The signature is
   * generated from the query signed by the given guid.
   *
   * @param commandType
   * @param privateKey
   * @param keysAndValues
   * @return the query string
   * @throws ClientException
   * 
   * arun: commented out because it is redundant with CommandUtils
   */
//  public static JSONObject createAndSignCommand(CommandType commandType,
//          PrivateKey privateKey, Object... keysAndValues)
//          throws ClientException {
//    try {
//      JSONObject result = createCommand(commandType, keysAndValues);
//      long t = System.nanoTime();
//      result.put(GNSCommandProtocol.TIMESTAMP, Format.formatDateISO8601UTC(new Date()));
//      result.put(GNSCommandProtocol.SEQUENCE_NUMBER, CommandUtils.getRandomRequestId());
//
//      String canonicalJSON = CanonicalJSON.getCanonicalForm(result);
//      String signatureString = CommandUtils.signDigestOfMessage(privateKey, canonicalJSON);
//      result.put(GNSCommandProtocol.SIGNATURE, signatureString);
//      if (edu.umass.cs.utils.Util.oneIn(10)) {
//        DelayProfiler.updateDelayNano("signing", t);
//      }
//      return result;
//    } catch (JSONException | NoSuchAlgorithmException | InvalidKeyException | SignatureException | UnsupportedEncodingException e) {
//      throw new ClientException("Error encoding message", e);
//    }
//  }

  /**
   * Creates a command object from the given command type and a variable
   * number of key and value pairs.
   *
   * @param commandType
   * @param keysAndValues
   * @return the query string
   * @throws edu.umass.cs.gnscommon.exceptions.client.ClientException
   */
  public static JSONObject createCommand(CommandType commandType, Object... keysAndValues)
          throws ClientException {
    try {
      long t = System.nanoTime();
      JSONObject result = new JSONObject();
      String key;
      Object value;
      result.put(GNSCommandProtocol.COMMAND_INT, commandType.getInt());
      for (int i = 0; i < keysAndValues.length; i = i + 2) {
        key = (String) keysAndValues[i];
        value = keysAndValues[i + 1];
        result.put(key, value);
      }
//      if (forceCoordinatedReads) {
//        result.put(GNSCommandProtocol.COORDINATE_READS, true);
//      }
      DelayProfiler.updateDelayNano("createCommand", t);
      return result;
    } catch (JSONException e) {
      throw new ClientException("Error encoding message", e);
    }
  }
  
	protected JSONObject setForceCoordinatedReads(JSONObject command) {
		try {
			return forceCoordinatedReads ? command.put(
					GNSCommandProtocol.COORDINATE_READS, true) : command;
		} catch (JSONException e) {
			e.printStackTrace();
			// return command; this exception will never happen anyway
			return command;
		}
	}

  /**
   * Check that the connectivity with the host:port can be established
   *
   * @throws IOException throws exception if a communication error occurs
   */
  public abstract void checkConnectivity() throws IOException;

  /**
   * Closes the underlying messenger.
   */
  public abstract void close();

  /**
   * Sends a command to the server and returns a response.
   *
   * @param command
   * @return Result as CommandValueReturnPacket; or String if Android (FIXME)
   * @throws IOException if an error occurs
   */
  public CommandValueReturnPacket sendCommandAndWait(JSONObject command) throws IOException {
    if (IS_ANDROID) {
      return androidSendCommandAndWait(command);
    } else {
      return desktopSendCommmandAndWait(command);
    }
  }

  /**
   * Sends a command to the server asychronously.
   *
   * @param command
   * @return CommandPacket sent.
   * @throws IOException if an error occurs
   */
  public CommandPacket sendCommandNoWait(JSONObject command) throws IOException {
    if (IS_ANDROID) {
      return androidSendCommandNoWait(command).getCommandPacket();
    } else {
      return desktopSendCommmandNoWait(command);
    }
  }

  private static final boolean USE_GLOBAL_MONITOR = Config.getGlobalBoolean(GNSCC.USE_GLOBAL_MONITOR);
  /**
   * Sends a command to the server.
   * Waits for the response packet to come back.
   *
   */
  private ConcurrentHashMap<Long, Object> monitorMap = new ConcurrentHashMap<Long, Object>();

  // arun: changed this to return CommandValueReturnPacket
  private CommandValueReturnPacket desktopSendCommmandAndWait(JSONObject command) throws IOException {
    Object myMonitor = new Object();
    long id;
    monitorMap.put(id = this.generateNextRequestID(), myMonitor);

    CommandPacket commandPacket = desktopSendCommmandNoWait(command, id);
    // now we wait until the correct packet comes back
    try {
      GNSClientConfig.getLogger().log(Level.FINE,
              "{0} waiting for query {1}",
              new Object[]{this, id + ""});

      long monitorStartTime = System.currentTimeMillis();
      if (!USE_GLOBAL_MONITOR) {
        synchronized (myMonitor) {
          while (monitorMap.containsKey(id) && (readTimeout == 0 || System.currentTimeMillis()
                  - monitorStartTime < readTimeout)) {
            myMonitor.wait(readTimeout);
          }
        }
      } else {
        synchronized (monitor) {
          while (!resultMap.containsKey(id)
                  && (readTimeout == 0 || System.currentTimeMillis()
                  - monitorStartTime < readTimeout)) {
            monitor.wait(readTimeout);
          }
        }
      }

      if (readTimeout != 0 && System.currentTimeMillis() - monitorStartTime >= readTimeout) {
    	  return this.getTimeoutResponse(commandPacket);
      }
      GNSClientConfig.getLogger().log(Level.FINE,
              "Response received for query {0}", new Object[]{id + ""});
    } catch (InterruptedException x) {
      GNSClientConfig.getLogger().severe("Wait for return packet was interrupted " + x);
    }
    //CommandResult 
    Request result = resultMap.remove(id);
    GNSClientConfig.getLogger().log(Level.FINE,
            "{0} received response {1} ",
            new Object[]{this, result.getSummary()
            });


    return result instanceof CommandValueReturnPacket ? ((CommandValueReturnPacket) result)
    		: new CommandValueReturnPacket(result.getServiceName(), id,
    				GNSResponseCode.ACTIVE_REPLICA_EXCEPTION,
    				((ActiveReplicaError) result).getResponseMessage());
  }
  
  protected CommandValueReturnPacket getTimeoutResponse(CommandPacket commandPacket) {
      GNSClientConfig.getLogger().log(Level.INFO,
              "{0} timed out after {1}ms on {2}: {3}",
              new Object[]{this, readTimeout, commandPacket.getClientRequestId() + "", commandPacket.getSummary()});
      /* FIXME: Remove use of string reponse codes */
      return new CommandValueReturnPacket(commandPacket.getServiceName(), commandPacket.getClientRequestId(), GNSResponseCode.TIMEOUT, 
                      GNSCommandProtocol.BAD_RESPONSE + " " + GNSCommandProtocol.TIMEOUT + " for command " + commandPacket.getSummary());

  }

  private CommandPacket desktopSendCommmandNoWait(JSONObject command) throws IOException {
	  return this.desktopSendCommmandNoWait(command, generateNextRequestID());
  }

  private CommandPacket desktopSendCommmandNoWait(JSONObject command, long id) throws IOException {
    long startTime = System.currentTimeMillis();
    CommandPacket packet = new CommandPacket(id, command);
		/* arun: moved this here from createCommand. This is the right place to
		 * put it because it is not easy to change "command" once it has been
		 * signed, and the command creation methods are and should be static. */
    packet.setForceCoordinatedReads(forceCoordinatedReads);
    GNSClientConfig.getLogger().log(Level.FINE, "{0} sending {1}:{2}",
            new Object[]{this, id + "", packet.getSummary()});
    queryTimeStamp.put(id, startTime);
    sendCommandPacket(packet);
    DelayProfiler.updateDelay("desktopSendCommmandNoWait", startTime);
    return packet;
  }

  private CommandValueReturnPacket androidSendCommandAndWait(JSONObject command) throws IOException {
    final AndroidNIOTask sendTask = androidSendCommandNoWait(command);
    try {
      return new CommandValueReturnPacket(new JSONObject(sendTask.get()));
    } catch (InterruptedException | ExecutionException | JSONException e) {
      throw new IOException(e);
    }
  }

  private AndroidNIOTask androidSendCommandNoWait(JSONObject command) throws IOException {
    final AndroidNIOTask sendTask = new AndroidNIOTask();
    sendTask.setId(generateNextRequestID()); // so we can get it back from the task later
    sendTask.execute(command, sendTask.getId(), monitor,
            queryTimeStamp, resultMap, readTimeout);
    return sendTask;
  }

  @Override
  public String toString() {
    return this.getClass().getSimpleName();
  }

  /**
   * arun: Handles both command return values and active replica error
   * messages.
   *
   * @param response
   * @param receivedTime
   * @throws JSONException
   */
  protected void handleCommandValueReturnPacket(Request response,
          long receivedTime) throws JSONException {
    long methodStartTime = System.currentTimeMillis();
    CommandValueReturnPacket packet = response instanceof CommandValueReturnPacket ? (CommandValueReturnPacket) response
            : null;
    ActiveReplicaError error = response instanceof ActiveReplicaError ? (ActiveReplicaError) response
            : null;
    assert (packet != null || error != null);

    long id = packet != null ? packet.getClientRequestId() : error.getRequestID();
    GNSClientConfig.getLogger().log(
            Level.FINE,
            "{0} received response {1}:{2} from {3}",
            new Object[]{this, id + "", response.getSummary(),
              packet != null
                      ? "unknown"//packet.getResponder() 
                      : error.getSender()});
    long queryStartTime = queryTimeStamp.remove(id);
    long latency = receivedTime - queryStartTime;
    movingAvgLatency = Util.movingAverage(latency, movingAvgLatency);
    // store the response away
    if (packet != null) {
      resultMap.put(id, packet);
    } else {
      resultMap.put(id, error);
    }

    // differentiates between synchronusly and asynchronusly sent
    if (!pendingAsynchPackets.containsKey(id)) {
      Object myMonitor = monitorMap.remove(id);
      assert (myMonitor != null) : Util.suicide("No monitor entry found for request " + id);
      if (!USE_GLOBAL_MONITOR && myMonitor != null) {
        synchronized (myMonitor) {
          myMonitor.notify();
        }
      }
      /* for synchronous sends we notify waiting threads.
       * arun: Needed now only for Android if at all.
       */
      synchronized (monitor) {
        monitor.notifyAll();
      }
    } else {
      // Handle the asynchronus packets
      // note that we have recieved the reponse
      pendingAsynchPackets.remove(id);
      // Record errors
      if (packet.getErrorCode().isExceptionOrError()) {
        totalAsynchErrors++;
      }
    }
    DelayProfiler.updateDelay("handleCommandValueReturnPacket", methodStartTime);
  }

  /**
 * @return random long not in map
 */
public synchronized long generateNextRequestID() {
    long id;
    do {
      id = randomID.nextLong();
      // this is actually wrong because we can still generate duplicate keys
      // because the resultMap doesn't contain pending requests until they come back
    } while (resultMap.containsKey(id));
    return id;
  }

  /**
   * Returns true if a response has been received.
   *
   * @param id
   * @return true if response has been received for request id
   */
  public boolean isAsynchResponseReceived(long id) {
    return resultMap.containsKey(id);
  }

  /**
   * Removes and returns the command result.
   *
   * @param id
   * @return {@link ConcurrentMap#remove(Object)}
   */
  public Request removeAsynchResponse(long id) {
    return resultMap.remove(id);
  }

// ASYNCHRONUS OPERATIONS
  /**
   * This contains all the command packets sent out asynchronously that have
   * not been acknowledged yet.
   */
  private final ConcurrentHashMap<Long, CommandPacket> pendingAsynchPackets
          = new ConcurrentHashMap<>();

  /**
 * @return number of outstanding async command packets
 */
public int outstandingAsynchPacketCount() {
    return pendingAsynchPackets.size();
  }

  /**
   * Sends a command packet without waiting for a response.
   * Performs bookkeeping so we can retrieve the response.
   *
   * @param packet
   * @throws IOException
   */
  public void sendCommandPacketAsynch(CommandPacket packet) throws IOException {
    long startTime = System.currentTimeMillis();
    long id = packet.getClientRequestId();
    pendingAsynchPackets.put(id, packet);
    GNSClientConfig.getLogger().log(Level.FINE, "{0} sending request {1}:{2}",
            new Object[]{this, id + "", packet.getSummary()});
    queryTimeStamp.put(id, System.currentTimeMillis());
    sendCommandPacket(packet);
    DelayProfiler.updateDelay("sendAsynchTestCommand", startTime);
  }

  /**
   * Updates the field in the targetGuid without waiting for a response.
   * The writer is the guid
   * of the user attempting access. Signs the query using
   * the private key of the writer guid.
   *
   * @param targetGuid
   * @param field
   * @param value
   * @param writer
   * @throws IOException
   * @throws ClientException
   * @throws JSONException
   */
  public void fieldUpdateAsynch(String targetGuid, String field, Object value, GuidEntry writer) throws ClientException, IOException, JSONException {
    JSONObject json = new JSONObject();
    json.put(field, value);
    JSONObject command = CommandUtils.createAndSignCommand(CommandType.ReplaceUserJSON,
            writer.getPrivateKey(), GNSCommandProtocol.GUID,
            targetGuid, GNSCommandProtocol.USER_JSON, json.toString(), GNSCommandProtocol.WRITER, writer.getGuid());
    sendCommandNoWait(command);
  }

  /**
   * Updates the field in the targetGuid without waiting for a response.
   * Signs the query using the private key of the given guid.
   *
   * @param targetGuid
   * @param field
   * @param value
   * @throws IOException
   * @throws ClientException
   * @throws JSONException
   */
  public void fieldUpdateAsynch(GuidEntry targetGuid, String field, Object value) throws ClientException, IOException, JSONException {
    fieldUpdateAsynch(targetGuid.getGuid(), field, value, targetGuid);
  }

  protected abstract void sendCommandPacket(CommandPacket packet) throws IOException;

  /**
 * 
 */
public final void resetInstrumentation() {
    movingAvgLatency = 0;
  }

  /**
   * Instrumentation. Returns the moving average of request latency
   * as seen by the client.
   *
   * @return moving average of request latency
   */
  public double getMovingAvgLatency() {
    return movingAvgLatency;
  }

  /**
   * Instrumentation. Currently only valid when asynch testing.
   *
   * @return total number of errors
   */
  public int getTotalAsynchErrors() {
    return totalAsynchErrors;
  }

  /**
   * Return a string representing the GNS server that we are connecting to.
   *
   * @return GNS server instance
   */
  public String getGNSInstance() {
    return GNSInstance;
  }

  /**
   * Returns true if the client is forcing read operations to be coordinated.
   *
   * @return true if the client is forcing read operations to be coordinated
   */
  public static boolean isForceCoordinatedReads() {
    return forceCoordinatedReads;
  }

  /**
   * Sets the value of forcing read operations to be coordinated.
   *
   * @param forceCoordinatedReads
   */
  public static void setForceCoordinatedReads(boolean forceCoordinatedReads) {
    AbstractGNSClient.forceCoordinatedReads = forceCoordinatedReads;
  }

  /**
   * Returns the timeout value (milliseconds) used when sending commands to the
   * server.
   *
   * @return value in milliseconds
   */
  public int getReadTimeout() {
    return readTimeout;
  }

  /**
   * Sets the timeout value (milliseconds) used when sending commands to the
   * server.
   *
   * @param readTimeout in milliseconds
   */
  public void setReadTimeout(int readTimeout) {
    this.readTimeout = readTimeout;
  }
}
