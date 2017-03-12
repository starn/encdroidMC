/*
 * Copyright 2009-2011 Jon Stevens et al.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package de.aflx.sardine.impl.handler;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;


import ch.boye.httpclientandroidlib.HttpEntity;
import ch.boye.httpclientandroidlib.HttpResponse;
import ch.boye.httpclientandroidlib.StatusLine;
import de.aflx.sardine.impl.SardineException;
import de.aflx.sardine.model.Multistatus;

/**
 * {@link org.apache.http.client.ResponseHandler} which returns the {@link Multistatus} response of
 * a {@link de.aflx.sardine.impl.methods.HttpPropFind} request.
 *
 * @author mirko
 * @version $Id: MultiStatusResponseHandler.java 276 2011-06-28 08:13:28Z dkocher@sudo.ch $
 */
public class MultiStatusResponseHandler extends ValidatingResponseHandler<Multistatus>
{
	public Multistatus handleResponse(HttpResponse response) throws SardineException, IOException
	{
		super.validateResponse(response);

		// Process the response from the server.
		HttpEntity entity = response.getEntity();
		StatusLine statusLine = response.getStatusLine();
		if (entity == null)
		{
			throw new SardineException("No entity found in response", statusLine.getStatusCode(),
					statusLine.getReasonPhrase());
		}
		
//		boolean debug=false;
//		//starn: for debug purpose, display the stream on the console
//		if (debug){
//			String result = convertStreamToString(entity.getContent());
//			System.out.println(result);
//			InputStream in = new ByteArrayInputStream(result.getBytes()); 
//			return this.getMultistatus(in);
//		}
//		else {
//			return this.getMultistatus(entity.getContent());
//		}
		Multistatus m = new Multistatus();
		new XmlMapper(entity.getContent(),m);
		return m;
	}

	
	public static String convertStreamToString(java.io.InputStream is) {
	    java.util.Scanner s = new java.util.Scanner(is).useDelimiter("\\A");
	    return s.hasNext() ? s.next() : "";
	}	
	

}