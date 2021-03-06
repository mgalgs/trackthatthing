package com.mgalgs.trackthatthingtv;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

public class RestClient {

	private static String convertStreamToString(InputStream is) {
		/*
		 * To convert the InputStream to String we use the
		 * BufferedReader.readLine() method. We iterate until the BufferedReader
		 * return null which means there's no more data to read. Each line will
		 * appended to a StringBuilder and returned as String.
		 */
		BufferedReader reader = new BufferedReader(new InputStreamReader(is));
		StringBuilder sb = new StringBuilder();

		String line = null;
		try {
			while ((line = reader.readLine()) != null) {
				sb.append(line + "\n");
			}
		} catch (IOException e) {
			e.printStackTrace();
		} finally {
			try {
				is.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		return sb.toString();
	}

	/**
	 * This is a test function which will connects to a given rest service and
	 * prints it's response to Android Log with labels TrackThatThing.TAG. Returns the
	 * JSONObject or null on error.
	 */
	public static JSONObject connect(String url) {
		JSONObject json = null;
		HttpClient httpclient = new DefaultHttpClient();

		// Prepare a request object
		HttpGet httpget = new HttpGet(url);

		// Execute the request
		HttpResponse response;
		try {
			response = httpclient.execute(httpget);
			// Examine the response status
			//Log.i(TrackThatThing.TAG, "here is the response status line we got:\n" + response.getStatusLine().toString());

			// Get hold of the response entity
			HttpEntity entity = response.getEntity();
			// If the response does not enclose an entity, there is no need
			// to worry about connection release

			if (entity != null) {

				// A Simple JSON Response Read
				InputStream instream = entity.getContent();
				String result = convertStreamToString(instream);
				//Log.i(TrackThatThing.TAG, "here is the result:\n" + result);

				// A Simple JSONObject Creation
				json = new JSONObject(result);
				//Log.i(TrackThatThing.TAG, "Here's the json object:\n" + json.toString());

				// A Simple JSONObject Parsing
//				JSONArray nameArray = json.names();
//				JSONArray valArray = json.toJSONArray(nameArray);

				
				// A Simple JSONObject Value Pushing
//				json.put("sample key", "sample value");
//				Log.i(TrackThatThing.TAG, "<jsonobject>\n" + json.toString()
//						+ "\n</jsonobject>");

				// Closing the input stream will trigger connection release
				instream.close();
			}

		} catch (ClientProtocolException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (JSONException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (Exception e) {
			e.printStackTrace();
		}
		
		return json;
	}
}
