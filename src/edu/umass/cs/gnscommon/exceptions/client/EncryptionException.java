/* Copyright (c) 2015 University of Massachusetts
 * 
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 * 
 * Initial developer(s): Westy, Emmanuel Cecchet */
package edu.umass.cs.gnscommon.exceptions.client;

import edu.umass.cs.gnscommon.GNSResponseCode;

/**
 * This class defines a EncryptionException
 * 
 * @author arun
 * @version 1.0
 */
public class EncryptionException extends ClientException {
	private static final long serialVersionUID = 1721392537222462554L;

	/**
	 * @param code
	 * @param message
	 */
	public EncryptionException(GNSResponseCode code, String message) {
		super(code, message);
	}

	/**
	 * @param message
	 */
	public EncryptionException(String message) {
		super(GNSResponseCode.SIGNATURE_ERROR, message);
	}

	/**
	 * @param message
	 * @param cause
	 */
	public EncryptionException(String message, Throwable cause) {
		super(GNSResponseCode.SIGNATURE_ERROR, message, cause);
	}

}
