package com.beam.krocouploader;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.UnsupportedEncodingException;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.methods.HttpDelete;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.DefaultHttpClient;
import org.json.JSONException;
import org.json.JSONObject;

import android.os.Environment;
import android.util.Log;

public class JsonParser {
	boolean DEBUG = false;

	static InputStream is = null;
	static JSONObject jObj = null;
	static String json = "";

	// constructor
	public JsonParser() {

	}

	// function get json from url
	// by making HTTP POST or GET mehtod
	public JSONObject makeHttpRequest(String url, String method,
			JSONObject params) throws IOException {
		// initialize entity
		StringEntity se = null;
		// set entity
		try {
			if (null != params)
				se = new StringEntity(params.toString());
		} catch (UnsupportedEncodingException e1) {
			e1.printStackTrace();
		}
		// Making HTTP request
		try {
			DefaultHttpClient httpClient = new DefaultHttpClient();
			// check for request method
			if (method == "POST") {
				HttpPost httpPost = new HttpPost(url);
				httpPost.setEntity(se);
				httpPost.setHeader("Accept", "application/json");
				httpPost.setHeader("Content-type", "application/json");
				HttpResponse httpResponse = httpClient.execute(httpPost);
				HttpEntity httpEntity = httpResponse.getEntity();
				is = httpEntity.getContent();
			} else if (method == "GET") {
				HttpGet httpGet = new HttpGet(url);
				HttpResponse httpResponse = httpClient.execute(httpGet);
				HttpEntity httpEntitiy = httpResponse.getEntity();
				is = httpEntitiy.getContent();
			} else if (method == "PUT") {
				HttpPut httpPut = new HttpPut(url);
				httpPut.setEntity(se);
				httpPut.setHeader("Accept", "application/json");
				httpPut.setHeader("Content-type", "application/json");
				HttpResponse httpResponse = httpClient.execute(httpPut);
				HttpEntity httpEntity = httpResponse.getEntity();
				is = httpEntity.getContent();
			} else if (method == "DELETE") {
				HttpDelete httpDelete = new HttpDelete(url);
				HttpResponse httpResponse = httpClient.execute(httpDelete);
				HttpEntity httpEntitiy = httpResponse.getEntity();
				is = httpEntitiy.getContent();
			}
		} catch (UnsupportedEncodingException e) {
			e.printStackTrace();
		} catch (ClientProtocolException e) {
			e.printStackTrace();
		}

		try {
			BufferedReader reader = new BufferedReader(new InputStreamReader(
					is, "iso-8859-1"), 8);
			StringBuilder sb = new StringBuilder();
			String line = null;
			while ((line = reader.readLine()) != null) {
				sb.append(line + "\n");
			}
			is.close();
			json = sb.toString();
			if (DEBUG) {
				File myFile = new File(Environment
						.getExternalStorageDirectory().getAbsolutePath()
						+ File.separator + "respond.html");
				myFile.createNewFile();
				FileOutputStream fOut = new FileOutputStream(myFile);
				OutputStreamWriter myOutWriter = new OutputStreamWriter(fOut);
				myOutWriter.append(json);
				myOutWriter.close();
				fOut.close();
			}
		} catch (Exception e) {
			Log.e("Buffer Error", "Error converting result " + e.toString());
		}
		// try parse the string to a JSON object
		try {
			jObj = new JSONObject(json);
		} catch (JSONException e) {
			Log.e("JSON Parser", "Error parsing data " + e.toString());
		}

		// return JSON String
		return jObj;

	}
}