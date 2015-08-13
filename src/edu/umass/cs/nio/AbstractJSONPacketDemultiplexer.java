/*
 * Copyright (c) 2015 University of Massachusetts
 * 
 * Licensed under the Apache License, Version 2.0 (the "License"); you
 * may not use this file except in compliance with the License. You
 * may obtain a copy of the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or
 * implied. See the License for the specific language governing
 * permissions and limitations under the License.
 * 
 * Initial developer(s): V. Arun
 */
package edu.umass.cs.nio;

import org.json.JSONException;
import org.json.JSONObject;

import edu.umass.cs.nio.nioutils.NIOInstrumenter;

/**
 * @author V. Arun
 */
public abstract class AbstractJSONPacketDemultiplexer extends
		AbstractPacketDemultiplexer<JSONObject> implements
		InterfacePacketDemultiplexer<JSONObject> {

	/**
	 * 
	 * @param threadPoolSize
	 *            Refer documentation for {@link AbstractPacketDemultiplexer#setThreadPoolSize(int)
	 *            setThreadPoolsize(int)}.
	 */
	protected AbstractJSONPacketDemultiplexer(int threadPoolSize) {
		super(threadPoolSize);
	}

	protected AbstractJSONPacketDemultiplexer() {
		super();
	}
	
	protected Integer getPacketType(JSONObject json) {
		try {
			return JSONPacket.getPacketType(json);
		} catch (JSONException e) {
			log.severe("Unable to decode JSON packet type for: " + json);
			e.printStackTrace();
		}
		return null;
	}
	
	protected JSONObject getMessage(String msg) {
		return MessageExtractor.parseJSON(msg);
	}

	// This method will be invoked by NIO
	protected boolean handleMessageSuper(JSONObject jsonObject)
			throws JSONException {
		NIOInstrumenter.rcvdJSONPacket(jsonObject);
		return super.handleMessageSuper(jsonObject);
	}
	
}
