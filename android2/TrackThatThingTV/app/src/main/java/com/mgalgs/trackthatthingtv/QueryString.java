package com.mgalgs.trackthatthingtv;

import android.util.Log;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;

public class QueryString {

	private String query = "";
	private boolean is_first_param = true;

	public QueryString(String base_url) {
		query = base_url;
	}

	public void add(String name, String value) {
		if (is_first_param) {
			query += "?";
			is_first_param = false;
		} else {
			query += "&";
		}
		encodeParamPlz(name, value);
	}

	private void encodeParamPlz(String name, String value) {
		try {
			query += URLEncoder.encode(name, "UTF-8");
			query += "=";
			query += URLEncoder.encode(value, "UTF-8");
		} catch (UnsupportedEncodingException ex) {
			Log.e(TrackThatThingTV.TAG, "Broken VM does not support UTF-8");
			ex.printStackTrace();
		}
	}

	public String getQuery() {
		return query;
	}

	public String toString() {
		return getQuery();
	}

}

