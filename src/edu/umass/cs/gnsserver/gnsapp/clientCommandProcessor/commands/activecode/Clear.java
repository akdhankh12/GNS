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
 *  Initial developer(s): Westy
 *
 */
package edu.umass.cs.gnsserver.gnsapp.clientCommandProcessor.commands.activecode;

import static edu.umass.cs.gnscommon.GNSCommandProtocol.*;
import edu.umass.cs.gnscommon.utils.Format;

import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.SignatureException;
import java.security.spec.InvalidKeySpecException;

import org.json.JSONException;
import org.json.JSONObject;

import edu.umass.cs.gnsserver.gnsapp.clientCommandProcessor.ClientRequestHandlerInterface;
import edu.umass.cs.gnsserver.gnsapp.clientCommandProcessor.commandSupport.ActiveCode;
import edu.umass.cs.gnsserver.gnsapp.clientCommandProcessor.commandSupport.CommandResponse;
import edu.umass.cs.gnsserver.gnsapp.clientCommandProcessor.commands.CommandModule;
import edu.umass.cs.gnscommon.CommandType;
import edu.umass.cs.gnscommon.GNSResponseCode;
import edu.umass.cs.gnsserver.gnsapp.clientCommandProcessor.commands.BasicCommand;

import java.text.ParseException;
import java.util.Date;

/**
 * The command to clear the active code for the specified GUID and action.
 *
 */
public class Clear extends BasicCommand {

  /**
   * Creates a Clear instance.
   *
   * @param module
   */
  public Clear(CommandModule module) {
    super(module);
  }

  @Override
  public CommandType getCommandType() {
    return CommandType.ClearActiveCode;
  }

  @Override
  public String[] getCommandParameters() {
    return new String[]{GUID, WRITER, AC_ACTION, SIGNATURE, SIGNATUREFULLMESSAGE};
  }

//  @Override
//  public String getCommandName() {
//    return AC_CLEAR;
//  }

  @Override
  public CommandResponse execute(JSONObject json,
          ClientRequestHandlerInterface handler) throws InvalidKeyException,
          InvalidKeySpecException, JSONException, NoSuchAlgorithmException,
          SignatureException, ParseException {
    String accountGuid = json.getString(GUID);
    String writer = json.getString(WRITER);
    String action = json.getString(AC_ACTION);
    String signature = json.getString(SIGNATURE);
    String message = json.getString(SIGNATUREFULLMESSAGE);
    Date timestamp = json.has(TIMESTAMP) ? Format.parseDateISO8601UTC(json.getString(TIMESTAMP)) : null; // can be null on older client
    GNSResponseCode response = ActiveCode.clearCode(accountGuid, action,
            writer, signature, message, timestamp, handler);

    if (response.isExceptionOrError()) {
      return new CommandResponse(response, BAD_RESPONSE + " " + response.getProtocolCode());
    } else {
      return new CommandResponse(GNSResponseCode.NO_ERROR, OK_RESPONSE);
    }
  }

  @Override
  public String getCommandDescription() {
    return "Clears the active code for the specified GUID and action,"
            + "ensuring the writer has permission";
  }
}
