/**
 * DataAdapter
 * Adapter for autocompletes
 * @author Michiel Vancoillie
 */

package com.flatturtle.myturtlecontroller;

import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.Set;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.content.Context;
import android.util.Log;
import android.widget.ArrayAdapter;
import android.widget.Filter;
import android.widget.Filterable;

public class DataAdapter extends ArrayAdapter<String> implements Filterable {
	private ArrayList<String> resultList;
	private static final String LOG_TAG = "Autocomplete";

	private static final String PLACES_API_BASE = "http://data.irail.be/";
	private static final String OUT_JSON = ".json";

	private Object[] resultsArray;
	private int numberOfCompletes = 0;
	private String type = "NMBS";

	public DataAdapter(Context context, int textViewResourceId, String type) {
		super(context, textViewResourceId);
		
		this.type = type;
		fetchData(type);
	}
	
	public void fetchData(String type){
		HttpURLConnection conn = null;
		StringBuilder jsonResults = new StringBuilder();
		try {
			StringBuilder sb = new StringBuilder(PLACES_API_BASE + type + "/Stations" + OUT_JSON);
			// sb.append("&input=" + URLEncoder.encode(input, "utf8"));

			URL url = new URL(sb.toString());
			conn = (HttpURLConnection) url.openConnection();
			InputStreamReader in = new InputStreamReader(conn.getInputStream());

			// Load the results into a StringBuilder
			int read;
			char[] buff = new char[1024];
			while ((read = in.read(buff)) != -1) {
				jsonResults.append(buff, 0, read);
			}
		} catch (MalformedURLException e) {
			Log.e(LOG_TAG, "Error processing Stations API URL", e);
		} catch (IOException e) {
			Log.e(LOG_TAG, "Error connecting to Stations API", e);
		} finally {
			if (conn != null) {
				conn.disconnect();
			}
		}

		try {
			// Create a JSON object hierarchy from the results
			JSONObject jsonObj = new JSONObject(jsonResults.toString());
			JSONArray predsJsonArray = jsonObj.getJSONArray("Stations");
			
			Set<String> temp = new LinkedHashSet<String>();
			  
			if (predsJsonArray != null) { 
			   int len = predsJsonArray.length();
			   for (int i=0;i<len;i++){ 
				   temp.add(predsJsonArray.getJSONObject(i).getString("name"));
			   } 
			}
			Log.i("qsdf", "qsdfq" + temp.size());
			resultsArray = temp.toArray();
		
		} catch (JSONException e) {
			Log.e(LOG_TAG, "Cannot process JSON results", e);
		}
	}

	@Override
	public int getCount() {
		return resultList.size();
	}

	@Override
	public String getItem(int index) {
		return resultList.get(index);
	}

	@Override
	public Filter getFilter() {
		Filter filter = new Filter() {
			@Override
			protected FilterResults performFiltering(CharSequence constraint) {
				FilterResults filterResults = new FilterResults();
				if (constraint != null) {
					// Retrieve the autocomplete results.
					resultList = autocomplete(constraint.toString());

					if(resultList != null){
						// Assign the data to the FilterResults
						filterResults.values = resultList;
						filterResults.count = resultList.size();
					}else{
						filterResults.values = null;
						filterResults.count = 0;
					}
				}
				return filterResults;
			}

			@Override
			protected void publishResults(CharSequence constraint,
					FilterResults results) {
				if (results != null && results.count > 0) {
					notifyDataSetChanged();
				} else {
					notifyDataSetInvalidated();
				}
			}
		};
		return filter;
	}

	private ArrayList<String> autocomplete(String input) {
		ArrayList<String> resultList = null;

		try {
			// Extract the station from the results
			resultList = new ArrayList<String>(resultsArray.length);
			for (int i = 0; i < resultsArray.length; i++) {
				String station = resultsArray[i].toString();
				if (station.toLowerCase().contains(input.toLowerCase()))
					resultList.add(station);
			}
		} catch (Exception e) {
			Log.e(LOG_TAG, "Cannot process JSON results", e);
		}
		
		// Refetch data after 10000 autocompletes
		numberOfCompletes++;
		if(numberOfCompletes % 10000 == 0)
			fetchData(this.type);
		return resultList;
	}
}