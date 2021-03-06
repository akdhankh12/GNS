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
 *  Initial developer(s): Westy, Emmanuel Cecchet
 *
 */
package edu.umass.cs.gnsclient.client.util;

import edu.umass.cs.utils.Config;
import edu.umass.cs.utils.Util;
import edu.umass.cs.gnsclient.client.CommandUtils;
import edu.umass.cs.gnsclient.client.GNSClientCommands;
import edu.umass.cs.gnsclient.client.GNSClientConfig;
import edu.umass.cs.gnsclient.client.GNSClientInterface;
import edu.umass.cs.gnscommon.utils.ThreadUtils;
import edu.umass.cs.gnscommon.utils.ByteUtils;
import edu.umass.cs.gnscommon.GNSCommandProtocol;
import edu.umass.cs.gnscommon.GNSResponseCode;
import edu.umass.cs.gnsclient.client.GuidEntry;
import edu.umass.cs.gnsclient.client.http.UniversalHttpClient;
import edu.umass.cs.gnscommon.SharedGuidUtils;
import edu.umass.cs.gnscommon.exceptions.client.ClientException;
import edu.umass.cs.gnscommon.exceptions.client.DuplicateNameException;
import edu.umass.cs.gnscommon.exceptions.client.InvalidGuidException;
import edu.umass.cs.utils.DelayProfiler;

import java.io.IOException;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;

/**
 *
 * @author westy
 */
public class GuidUtils {

  public static final String JUNIT_TEST_TAG = "JUNIT";
  // this is so we can mimic the verification code the server is generting
  // AKA we're cheating... if the SECRET changes on the server side
  // you'll need to change it here as well
  private static final String SECRET = 
		  Config.getGlobalString(GNSClientConfig.GNSCC.VERIFICATION_SECRET);
  private static final int VERIFICATION_CODE_LENGTH = 3; // Six hex characters

  private static boolean guidExists(GNSClientInterface client, GuidEntry guid) throws IOException {
    try {
      client.lookupGuidRecord(guid.getGuid());
    } catch (ClientException e) {
      return false;
    }
    return true;
  }

  public static GuidEntry registerGuidWithTestTag(GNSClientInterface client, GuidEntry masterGuid, String entityName) throws Exception {
    return registerGuidWithTag(client, masterGuid, entityName, JUNIT_TEST_TAG);
  }

  /**
   * Creates and verifies an account GUID. Yes it cheats on verification.
   *
   * @param client
   * @param name
   * @param password
   * @return
   * @throws Exception
   */
  public static GuidEntry lookupOrCreateAccountGuid(GNSClientInterface client, String name,
          String password) throws Exception {
    return lookupOrCreateAccountGuid(client, name, password, false);
  }

  public static final String ACCOUNT_ALREADY_VERIFIED = "Account already verified";

  private static final int NUM_VERIFICATION_ATTEMPTS = 3;

  public static GuidEntry lookupOrCreateAccountGuid(GNSClientInterface client, String name, String password,
          boolean verbose) throws Exception {
    GuidEntry guid = lookupGuidEntryFromDatabase(client, name);
    // If we didn't find the guid or the entry in the database is obsolete we
    // create a new guid.
    if (guid == null || !guidExists(client, guid)) {
      if (verbose) {
        if (guid == null) {
          System.out.println("  Creating a new account GUID for " + name);
        } else {
          System.out.println("  Old account GUID found locally for " + name + " is invalid, creating a new one.");
        }
      }
      try {
      guid = client.accountGuidCreate(name, password);
      } catch(DuplicateNameException e) {
    	  // ignore as it is most likely because of a seemingly failed creation operation that actually succeeded.
      }
      int attempts = 0;
      // Since we're cheating here we're going to catch already verified errors which means
      // someone on the server probably turned off verification for testing purposes
      // but we'll rethrow everything else
      while (true) {
        try {
          client.accountGuidVerify(guid, createVerificationCode(name));
        } catch (ClientException e) {
          // a bit of a hack here that depends on someone not changing
          // that error message
          if (!e.getMessage().contains(GNSCommandProtocol.ALREADY_VERIFIED_EXCEPTION)) {
            if (attempts++ < NUM_VERIFICATION_ATTEMPTS) {
              // do nothing
            } else {
              e.printStackTrace();
              throw e;
            }
          } else {
            System.out.println("  Caught and ignored \"Account already verified\" error for " + name);
            break;
          }
        }
      }
      if (verbose) {
        System.out.println("  Created and verified account GUID " + guid);
      }
      return guid;
    } else {
      if (verbose) {
        System.out.println("Found account guid for " + guid.getEntityName() + " (" + guid.getGuid() + ")");
      }
      return guid;
    }
  }

  private static String createVerificationCode(String name) {
    return ByteUtils.toHex(Arrays.copyOf(SHA1HashFunction.getInstance().hash(name + SECRET), VERIFICATION_CODE_LENGTH));
  }

  /**
   * Creates and verifies an account GUID. Yes it cheats on verification.
   *
   * @param client
   * @param accountGuid
   * @param name
   * @return
   * @throws Exception
   */
  public static GuidEntry lookupOrCreateGuid(GNSClientInterface client, GuidEntry accountGuid, String name) throws Exception {
    return lookupOrCreateGuid(client, accountGuid, name, false);
  }

  /**
   * Creates and verifies an account GUID. Yes it cheats on verification.
   *
   * @param client
   * @param accountGuid
   * @param name
   * @param verbose
   * @return
   * @throws Exception
   */
  public static GuidEntry lookupOrCreateGuid(GNSClientInterface client, GuidEntry accountGuid, String name, boolean verbose) throws Exception {
    GuidEntry guid = lookupGuidEntryFromDatabase(client, name);
    // If we didn't find the guid or the entry in the database is obsolete we
    // create a new guid.
    if (guid == null || !guidExists(client, guid)) {
      if (verbose) {
        if (guid == null) {
          System.out.println("Creating a new guid for " + name);
        } else {
          System.out.println("Old guid for " + name + " is invalid. Creating a new one.");
        }
      }
      //Util.suicide("here");
      guid = client.guidCreate(accountGuid, name);
      return guid;
    } else {
      if (verbose) {
        System.out.println("Found guid for " + guid.getEntityName() + " (" + guid.getGuid() + ")");
      }
      return guid;
    }
  }

  private static GuidEntry registerGuidWithTag(GNSClientInterface client, GuidEntry masterGuid, String entityName, String tagName) throws Exception {
    GuidEntry entry = client.guidCreate(masterGuid, entityName);
    /*
     * arun: replace this block with code below.
    try {
      client.addTag(entry, tagName);
    } catch (InvalidGuidException e) {
      ThreadUtils.sleep(100);
      client.addTag(entry, tagName);
    }
    return entry;
    */
		/* arun: This gymnastics below is to hide ugly methods like addTag from public
		 * view. 
		 * 
		 * I am disabling addTag because it is incorrect. There is no reason for 
		 * a local (as it happens to be) addTag operation to succeed after entityName's
		 * creation done as a remote transaction (that is also poor to 
		 * begin with).
		 * */
//		if (client instanceof GNSClientCommands)
//			return GNSClientCommandsTest.addTag(client, entry, tagName);
//		else if (client instanceof UniversalHttpClient)
//			return UniversalHttpClientTest.addTag(client, entry, tagName);
//		else throw new RuntimeException("Unimplemented");
    return entry;
  }

  /**
   * Looks up the guid information associated with alias that is stored in the preferences.
   *
   * @param client
   * @param name
   * @return
   */
  public static GuidEntry lookupGuidEntryFromDatabase(GNSClientInterface client, String name) {
    return GuidUtils.lookupGuidEntryFromDatabase(client.getGNSInstance(), name);
  }

  public static GuidEntry lookupGuidEntryFromDatabase(String gnsInstance, String name) {
    return KeyPairUtils.getGuidEntry(gnsInstance, name);
  }

  /**
   * Creates a GuidEntry which associates an alias with a new guid and key pair. Stores the
   * whole thing in the local preferences.
   *
   * @param alias
   * @param hostport
   * @return
   * @throws NoSuchAlgorithmException
   */
  public static GuidEntry createAndSaveGuidEntry(String alias, String hostport) throws NoSuchAlgorithmException {
    long keyPairStart = System.currentTimeMillis();
    KeyPair keyPair = KeyPairGenerator.getInstance(GNSCommandProtocol.RSA_ALGORITHM).generateKeyPair();
    DelayProfiler.updateDelay("createKeyPair", keyPairStart);
    String guid = SharedGuidUtils.createGuidStringFromPublicKey(keyPair.getPublic().getEncoded());
    long saveStart = System.currentTimeMillis();
    KeyPairUtils.saveKeyPair(hostport, alias, guid, keyPair);
    DelayProfiler.updateDelay("saveKeyPair", saveStart);
    return new GuidEntry(alias, guid, keyPair.getPublic(), keyPair.getPrivate());
  }

  /**
   * Finds a GuidEntry which associated with an alias or creates and stores it in the local preferences.
   *
   * @param alias
   * @param gnsInstance
   * @return
   * @throws NoSuchAlgorithmException
   */
  public static GuidEntry lookupOrCreateGuidEntry(String alias, String gnsInstance) throws NoSuchAlgorithmException {
    GuidEntry entry;
    if ((entry = GuidUtils.lookupGuidEntryFromDatabase(gnsInstance, alias)) != null) {
      return entry;
    } else {
      return createAndSaveGuidEntry(alias, gnsInstance);
    }
  }

//  /**
//   * Uses a hash function to generate a GUID from a public key.
//   * This code is duplicated in server so if you
//   * change it you should change it there as well.
//   *
//   * @param keyBytes
//   * @return
//   */
//  public static String createGuidFromPublicKey(byte[] keyBytes) {
//    byte[] publicKeyDigest = SHA1HashFunction.getInstance().hash(keyBytes);
//    return ByteUtils.toHex(publicKeyDigest);
//  }
}
