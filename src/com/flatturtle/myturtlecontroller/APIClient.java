/**
 * APIClient
 * Handles API requests
 * @author Michiel Vancoillie
 */

package com.flatturtle.myturtlecontroller;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Observable;

import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.HttpResponseException;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpDelete;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.client.methods.HttpRequestBase;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.protocol.HTTP;
import org.apache.http.util.EntityUtils;

import android.accounts.NetworkErrorException;
import android.app.Activity;
import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.util.Log;

public class APIClient extends Observable {
	private HttpClient http;
	private String API_URL;
	private HttpResponse response;
	private String token;
	private ConnectivityManager conMgr;
	
	public String pin;

	private static final String TAG = "APICLient";
	private static final String URI_AUTH = "auth/mobile";
	private static final String URI_SWITCHER_ROTATE = "tablet/plugins/switcher/rotate";
	private static final String URI_ROUTE_NMBS = "tablet/plugins/route/nmbs";
	private static final String URI_ROUTE_NMBS_BOARD = "tablet/plugins/route/board";
	private static final String URI_TAXI_SHOW = "tablet/plugins/taxi/show";
	
	@SuppressWarnings("unused")
	private static final String METHOD_GET = "GET";
	private static final String METHOD_POST = "POST";
	private static final String METHOD_PUT = "PUT";
	private static final String METHOD_DELETE = "DELETE";
	

	public APIClient(String apiURL, Activity act) {
		this.API_URL = apiURL;

		conMgr = (ConnectivityManager) act.getSystemService(Context.CONNECTIVITY_SERVICE);
	}

	/**
	 * Authenticate with API
	 */
	public Boolean authenticate() throws NetworkErrorException{
		ArrayList<NameValuePair> params = new ArrayList<NameValuePair>();
		params.add(new BasicNameValuePair("pin", pin));
		
		String response = this.call(METHOD_POST, URI_AUTH, params);
		return ((response != null && response.equals("true")) ? true : false);
	}
	
	/**
	 * Rotate pane
	 */
	public void rotatePane(){
		final ArrayList<NameValuePair> params = new ArrayList<NameValuePair>();
		params.add(new BasicNameValuePair("type", "widget"));
		
		new Thread(new Runnable() {
	        public void run() {
	    		try {
					call(METHOD_POST, URI_SWITCHER_ROTATE, params);
				} catch (NetworkErrorException e) {
					setChanged();
					notifyObservers(e);
				}
	        }
	    }).start();
	}
	
	/**
	 * Route call
	 */
	public void route(String from, String to){
		final ArrayList<NameValuePair> params = new ArrayList<NameValuePair>();
		params.add(new BasicNameValuePair("from", from));
		params.add(new BasicNameValuePair("to", to));
		
		new Thread(new Runnable() {
	        public void run() {
	    		try {
	    			call(METHOD_POST, URI_ROUTE_NMBS, params);
				} catch (NetworkErrorException e) {
					setChanged();
					notifyObservers(e);
				}
	        }
	    }).start();
	}
	
	/**
	 * Board call
	 */
	public void board(String type, String station){
		final ArrayList<NameValuePair> params = new ArrayList<NameValuePair>();
		params.add(new BasicNameValuePair("type", type.toLowerCase()));
		params.add(new BasicNameValuePair("station", station));
		
		new Thread(new Runnable() {
	        public void run() {
	    		try {
	    			call(METHOD_POST, URI_ROUTE_NMBS_BOARD, params);
				} catch (NetworkErrorException e) {
					setChanged();
					notifyObservers(e);
				}
	        }
	    }).start();
	}
	
	/**
	 * Show taxi turtle
	 */
	public void taxi(){	
		new Thread(new Runnable() {
	        public void run() {
	    		try {
	    			call(METHOD_POST, URI_TAXI_SHOW, null);
				} catch (NetworkErrorException e) {
					setChanged();
					notifyObservers(e);
				}
	        }
	    }).start();
	}
	
	/**
	 * General API call method
	 */
	public String call(String method, String uri, ArrayList<NameValuePair> params) throws NetworkErrorException{
		// Check internet connection
		NetworkInfo infos[] = conMgr.getAllNetworkInfo(); 
		boolean internet = false;
		for(NetworkInfo info : infos){
			if (info.getState() == NetworkInfo.State.CONNECTED || info.getState() == NetworkInfo.State.CONNECTING) {
				internet = true;
				break;
			}
		}
		if(!internet){
			throw new NetworkErrorException("No internet connection.");
		}
		
		
		http = new DefaultHttpClient();
		
		HttpRequestBase request;
		
		// Switch methods
		if(method == METHOD_POST){
			request = new HttpPost(API_URL + '/' + uri);
		}else if(method == METHOD_PUT){
			request = new HttpPut(API_URL + '/' + uri);
		}else if(method == METHOD_DELETE){
			request = new HttpDelete(API_URL + '/' + uri);
		}else{
			request = new HttpGet(API_URL + '/' + uri);
		}

		if (uri != URI_AUTH) {
			if(token != null)
				request.addHeader("Authorization", token);
			else
				this.authenticate();
		}
		
		String body = null;
		try {
			if(params != null && request instanceof HttpPost)
				((HttpPost) request).setEntity(new UrlEncodedFormEntity(params, HTTP.UTF_8));
			response = http.execute(request);
			int statusCode = response.getStatusLine().getStatusCode();
			body = EntityUtils.toString(response.getEntity());

			Log.i(request.getURI().toString(), statusCode + " - " + body);

			if (uri == URI_AUTH) {
				token = null;

				switch (statusCode) {
				case 200:
					// Get token
					token = body.split("\"")[1];
					Log.i(TAG, "Got token: " + token);
					return "true";
				case 403:
					// Invalid PIN
					return null;
				default:
					return null;
				}
			} else {
				switch (statusCode) {
				case 403:
					if (token != null) {
						// Invalid token
						token = null;
						authenticate();
						call(method, uri, params);
					}
					break;

				default:
					break;
				}
			}

			http.getConnectionManager().shutdown();
		} catch (HttpResponseException e) {
			Log.e(request.getURI().toString(),
					e.getStatusCode() + " - " + e.getMessage());
		} catch (ClientProtocolException e) {
			Log.e(TAG, e.getMessage());
		} catch (IOException e) {
			Log.e(TAG, e.getMessage());
		}

		return body;
	}
}
