/*
 * 2. database to store the current status in case of the app crash
 * 4. deal with wrong address for example if there is not intersection of 7th and 41st
 * 6. according to "5" add a button in edit pop window just for add one more people 
 * 7. set the bounds by fecthing the four angel LatLng of bounds
 * 8. after modify the existing addressinfo remember to update the entire datastruction info
 * 9. make sure the entire route has been update when the number of an address has been updated
 * 10. if the start direction for next location is oppsite to the current one than the route just show overlap without considering if we can do U turn at that point
 * 11. make sure pop-up for both clearList and delete address button 
 * 
 * Next time should start with 11
 */

/*
 * Bug list:
 * 
 * 1. after edit the existing address everything has been deleted because the wrong implementation of deleteLocation mehtod
 * 2. some location with different number should be considered as the same addressInfoItem
 */

package com.example.mapdemo;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import org.json.JSONObject;

import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.location.Address;
import android.location.Geocoder;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.Button;
import android.widget.ListView;
import android.widget.RelativeLayout;
import android.widget.Spinner;
import android.widget.Toast;

import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;



public class MainActivity extends FragmentActivity {
	public final static String EXTRA_MESSAGE = "com.example.mapdemo.route_message";
	public final static String TAG = "MapDemo";

	public Spinner address1;
	public Spinner address2;
	public Spinner number;

	private Button buildRouteButton;
	private Button clearListButton;
	private Button addToListButton;
	private ProgressDialog pdialog;
	
	private int editPosition;

	private List<AddressInfoItem> addressInfoList;

	private AddressInfoItem selectedItem;

	private ArrayAdapterItem adapter;

	private Map<StartEndPair, List<LatLng>> pairToWaypoints; // map from start and end points to all the waypoints between them
	private List<LatLng> locationList;
	private Map<LatLng, Double> locationToNumber;

	private Map<StartEndPair, Double> pairToDistance; // the distance here should consider the number of people who want to get to this location
	// => Real DISTANCE = distance / number (to be optimized)
	private double shortestDistance = 10000.0; // any big number is fine

	private List<LatLng> optimalRoute; 

	private List<LatLng> tempRoute;

	private Map<LatLng, List<LatLng>> startToEnds;

	private Map<String, LatLng> locationToLatLng; // for fetching LatLng when we want to edit the address

	private ArrayList<LatLng> routeMessage; // all locations for the final decision for entire route


	private Map<LatLng, String> locationToStreetIntersection; // This is only for debug purpose.
	
	SharedPreferences locationInfoPref;
	
	SharedPreferences savedListInfoPref;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.map_demo_main);
		address1 = (Spinner) findViewById(R.id.spinner_address1);
		address2 = (Spinner) findViewById(R.id.spinner_address2);
		number = (Spinner) findViewById(R.id.spinner_numbers);
				
		buildRouteButton = (Button)findViewById(R.id.build_route);
		clearListButton = (Button)findViewById(R.id.clear_list);
		addToListButton = (Button)findViewById(R.id.add_to_list);
		locationInfoPref = this.getSharedPreferences("LocationInfo", Context.MODE_PRIVATE);

		// Setup data structure
		addressInfoList = new ArrayList<AddressInfoItem>();
		pairToWaypoints = new HashMap<StartEndPair, List<LatLng>>();
		locationList = new ArrayList<LatLng>();
		locationToNumber = new HashMap<LatLng, Double>();
		pairToDistance = new HashMap<StartEndPair, Double>();
		optimalRoute = new ArrayList<LatLng>();
		tempRoute = new ArrayList<LatLng>();
		startToEnds = new HashMap<LatLng, List<LatLng>>();   // change the List of end to Set. maybe better for remove location
		routeMessage = new ArrayList<LatLng>();
		locationToStreetIntersection = new HashMap<LatLng, String>();
		locationToLatLng = new HashMap<String, LatLng>();
		
		showAddressList();

		// Setup the original points oddegaard stop
		LatLng originalStart = new LatLng(47.657289,-122.309669);
		List<LatLng> ends = new ArrayList<LatLng>();
		locationList.add(originalStart);
		startToEnds.put(originalStart, ends);
		locationToNumber.put(originalStart, 1.0); // default 1 maybe need to be changed later!!
		

		
		pdialog = new ProgressDialog(this);
		pdialog.setCancelable(true);
		pdialog.setMessage("Loading address..");
		

		// Load existing address if exist
		savedListInfoPref = this.getSharedPreferences("SavedList", Context.MODE_PRIVATE);
		int key = 0;
		while (savedListInfoPref.contains(String.valueOf(key))) {
			String[] addressInfo = savedListInfoPref.getString(String.valueOf(key), "address not found in preference").split(":=:");
			
			Log.d("SAVEDLISTINFO", addressInfo[0] + " " + addressInfo[1] + " " + addressInfo[2] +"Saved Address!!!!!!!!!!!!!!!!!!!!!!!");
			try {
				addToListHelper(addressInfo[0], addressInfo[1], addressInfo[2]);
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			key++;
			
		}
		if (key == 0) {
			Log.d("SAVEDLISTINFO", "NO Address!!!!!!!!!!!!!!!!!!!!!!!");
		}
	}




	/**
	 * Called when the user press the "Add to list" button to add a new address
	 * @throws InterruptedException 
	 */
	public void addToList(View view) throws InterruptedException {
		
		String eastWest = address1.getSelectedItem().toString();
		String southNorth = address2.getSelectedItem().toString();
		String n2 = number.getSelectedItem().toString();
		
		Log.w("DownloadTask", "Put new address into sharedpreferences");
		addToListHelper(eastWest, southNorth, n2);
	}
	
	public void addToListHelper(String eastWest, String southNorth, String n2) throws InterruptedException {
		AddressInfoItem newAddress = new AddressInfoItem(eastWest, southNorth, n2, address1.getSelectedItemPosition(), address2.getSelectedItemPosition());
		if (addressInfoList.contains(newAddress)) {
			Toast.makeText(getBaseContext(), "Exsiting Address", Toast.LENGTH_SHORT).show();
		} else {
			buildRouteButton.setEnabled(false);
			clearListButton.setEnabled(false);
			addToListButton.setEnabled(false);
			pdialog.show();
			addressInfoList.add(newAddress);
			adapter.notifyDataSetChanged();
			

			SharedPreferences.Editor editor = savedListInfoPref.edit();
			editor.putString(String.valueOf((addressInfoList.size() - 1)), eastWest + ":=:" + southNorth + ":=:" + n2);
			Log.d("", "key for this new address in pref is " + (addressInfoList.size() - 1));
			editor.commit();
	
			LatLng location = getLocation(eastWest, southNorth, n2);
	
			// reset shortestDistance for next round calculation
			shortestDistance = 10000.0;
	
			// Calculated the distance from the new address to every other address
			updateRouteDistance(location);
			locationToLatLng.put(eastWest + ":=:" + southNorth + ":=:" + n2, location);
		}
	}

	public void updateRouteDistance(LatLng newLocation) throws InterruptedException {
		int rateCounter = 0;
		List<LatLng> ends = new ArrayList<LatLng>();
		for (LatLng existLocation : locationList) {
			ends.add(existLocation);

			startToEnds.get(existLocation).add(newLocation);
			if (locationInfoPref.contains(existLocation.toString() + ":=:" + newLocation.toString())) {
				Log.w("calculated start and end info!!!!!!!!", "%%%%%%%%%%%%%%%%%%");
				Log.w("from ", "" + existLocation.toString() + " to " + newLocation.toString());
				Log.w(" and from ", "" + newLocation.toString() + " to " + existLocation.toString());
				parseLocations(existLocation, newLocation, locationInfoPref.getString(existLocation.toString() + ":=:" + newLocation.toString(), "not such startEndPair found ToT"));
				parseLocations(newLocation, existLocation, locationInfoPref.getString(newLocation.toString() + ":=:" + existLocation.toString(), "not such startEndPair found ToT"));
				
			} else {
				Log.w("NEW ADDRESS", "@@@@@@@@@@@@@@@@@@@@@@");
				Log.w("from ", "" + existLocation.toString() + " to " + newLocation.toString());
				Log.w(" and from ", "" + newLocation.toString() + " to " + existLocation.toString());
				// Getting URL to the Google Directions API
				String url1 = getDirectionsUrl(existLocation, newLocation);
				DownloadTask downloadTask1 = new DownloadTask(false, existLocation, newLocation, rateCounter);
				// Start downloading json data from Google Directions API
				downloadTask1.execute(url1);

				// Switch start and end point
				String url2 = getDirectionsUrl(newLocation, existLocation); 
				DownloadTask downloadTask2 = new DownloadTask(false, newLocation, existLocation, rateCounter);
				// Start downloading json data from Google Directions API
				downloadTask2.execute(url2);
				rateCounter += 2;
				
				if (rateCounter % 10 == 0) {
					//Thread.sleep(6000);
				}
			}
		}	

		//Log.w("rateCounter = ", rateCounter + "******************8");
		locationList.add(newLocation);
		startToEnds.put(newLocation, ends);
	}

	private void parseLocations(LatLng start, LatLng end, String routeInfo) {
		ParserTask parserTask = new ParserTask(false, start, end, -1);
		// Invokes the thread for parsing the JSON data
		//Log.w("routeInfo is ", routeInfo);
		parserTask.execute(routeInfo);
	}


	public void showAddressList() {
		// our adapter instance
		adapter = new ArrayAdapterItem(this, R.layout.address_info_item, addressInfoList);

		// Create a new ListView, set the adapteritem click listener
		ListView addressList = new ListView(this);
		addressList.setAdapter(adapter);

		RelativeLayout layout = (RelativeLayout) findViewById(R.id.spinner_address1).getParent();
		RelativeLayout.LayoutParams params = new RelativeLayout.LayoutParams(RelativeLayout.LayoutParams.WRAP_CONTENT, RelativeLayout.LayoutParams.WRAP_CONTENT);
		params.addRule(RelativeLayout.BELOW, R.id.spinner_address1);
		params.addRule(RelativeLayout.ABOVE, R.id.add_to_list);
		addressList.setLayoutParams(params);
		layout.addView(addressList);


		addressList.setOnItemClickListener(new OnItemClickListener() {

			@Override
			public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
				// TODO Auto-generated method stub
				View editAddressFragment = findViewById(R.id.edit_fragment);
				editAddressFragment.setVisibility(View.VISIBLE);
				editAddressFragment.bringToFront();

				selectedItem = addressInfoList.get(position);
				
				((Spinner)findViewById(R.id.spinner_address1_fragment)).setSelection(selectedItem.index1, false);
				((Spinner)findViewById(R.id.spinner_address2_fragment)).setSelection(selectedItem.index2, false);
				((Spinner)findViewById(R.id.spinner_numbers_fragment)).setSelection(Integer.parseInt(selectedItem.number) - 1, false);

				LatLng location = locationToLatLng.get(selectedItem.eastWest + ":=:" + selectedItem.southNorth + ":=:" + selectedItem.number);
				deleteHelper(location);
				editPosition = position;

			}

		});
	}

	private void deleteHelper(LatLng location) {
		if (location != null) {
			// delete all the old records about this location
			locationList.remove(location);
			locationToNumber.remove(location);
			
			startToEnds.remove(location); // remove all the pair start in this location
			
			// remove all the pair end in this location
			for (LatLng start : startToEnds.keySet()) {
				startToEnds.get(start).remove(location);
				
				StartEndPair pair1 = new StartEndPair(start, location);
				StartEndPair pair2 = new StartEndPair(location, start);
				
				pairToDistance.keySet().remove(pair1);
				pairToDistance.keySet().remove(pair2);
				
				pairToWaypoints.keySet().remove(pair1);
				pairToWaypoints.keySet().remove(pair2);
			}

			// Change saved address info
			SharedPreferences.Editor editor = savedListInfoPref.edit();
			editor.remove(String.valueOf(editPosition));
			Log.d(TAG, "Replace the saved address in postion " + editPosition);
			editor.commit();
		}
	}

	public void deleteLocationInfo(View view) {
		final Dialog dialog = new Dialog(this);

        dialog.setContentView(R.layout.alert_message);
        dialog.setTitle("Custom Alert Dialog");

        Button delete=(Button)dialog.findViewById(R.id.delete_after_alert);
        Button cancel=(Button)dialog.findViewById(R.id.cancel);
        dialog.show();
	}

	public void buildRoute(View view) {
		/*Intent intent = new Intent(this, BuildRoute.class);
    	String[] addressList = new String[addressInfoList.size()];
    	for (int i = 0; i < addressList.length; i++) {
    		String eastWest = addressInfoList.get(i).eastWest;
    		String southNorth = addressInfoList.get(i).southNorth;
    		String number = addressInfoList.get(i).number;
    		addressList[i] = eastWest + ":=:" + southNorth + ":=:" + number;
    	}
    	intent.putExtra(EXTRA_MESSAGE, addressList);
    	startActivity(intent);*/
		optimizeRoute();
		//Log.w("optimalRoute size = ", optimalRoute.size() + "");
		for (LatLng point : optimalRoute) {
			//Log.w("optimalRoute: ", point.toString());
		}
		addWayPoints();
		Intent intent = new Intent(this, BuildRoute.class);
		intent.putParcelableArrayListExtra(EXTRA_MESSAGE, routeMessage);
		startActivity(intent);
	}

	public void doneEdit(View view) throws IOException, InterruptedException {
		View editAddressFragment = findViewById(R.id.edit_fragment);
		editAddressFragment.setVisibility(View.INVISIBLE);

		Spinner address1Edit = (Spinner)findViewById(R.id.spinner_address1_fragment);
		Spinner address2Edit = (Spinner)findViewById(R.id.spinner_address2_fragment);
		
		String eastWest = address1Edit.getSelectedItem().toString();
		String southNorth = address2Edit.getSelectedItem().toString();
		String n = ((Spinner)findViewById(R.id.spinner_numbers_fragment)).getSelectedItem().toString();

		AddressInfoItem newAddress = new AddressInfoItem(eastWest, southNorth, n, address1Edit.getSelectedItemPosition(), address2Edit.getSelectedItemPosition());
		addressInfoList.set(editPosition, newAddress);
		adapter.notifyDataSetChanged();

		LatLng location = getLocation(eastWest, southNorth, n);
		updateRouteDistance(location);
		Log.d(TAG, "Replace the orginal address info with " + eastWest + ", " + southNorth + ", " + n);

		// Change saved address info
		SharedPreferences.Editor editor = savedListInfoPref.edit();
		editor.putString(String.valueOf(editPosition), eastWest + ":=:" + southNorth + ":=:" + n);
		Log.d(TAG, "Replace the saved address in postion " + editPosition);
		editor.commit();
	}

	private LatLng getIntersectionLocation(String eastWest, String southNorth) throws IOException {
		String selectedLat = "";
		String selectedLng = "";
		Geocoder geocoder = new Geocoder(this, Locale.getDefault());
		List<Address> eastWestList = geocoder.getFromLocationName(eastWest + " Seattle WA 98105", 5);
		List<Address> southNorthList = geocoder.getFromLocationName(southNorth + " Seattle WA 98105", 5);

		if (eastWestList.size() > 0 && southNorthList.size() > 0) {
			Address address1 = southNorthList.get(0);
			if(address1.hasLatitude() && address1.hasLongitude()){
				selectedLng = "" + address1.getLongitude();
			}

			Address address2 = eastWestList.get(0);
			if(address2.hasLatitude() && address2.hasLongitude()){
				selectedLat = "" + address2.getLatitude();
			}
			//Toast.makeText(getApplicationContext(), "Lat: " + selectedLat + "\n Lng: " + selectedLng, Toast.LENGTH_LONG).show();
		}

		//return selectedLat + ":=:" + selectedLng;
		return new LatLng(Double.parseDouble(selectedLat), Double.parseDouble(selectedLng));
	}

	private LatLng getLocation(String eastWest, String southNorth, String number) {
		try {
			LatLng location = getIntersectionLocation(eastWest, southNorth);
			locationToNumber.put(location, Double.valueOf(number));
			//Log.w("points to number: @@@@@@@@@@",  location.toString() + " " + number);
			//return getIntersectionLocation(eastWest, southNorth) + ":=:" + number;
			return location;
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			//return "cannot get location";
			return null;
		}
	}


	private String getDirectionsUrl(LatLng origin, LatLng dest) {
		// Origin of route

		String str_origin = "origin=" + origin.latitude + "," + origin.longitude;

		// Destination of route
		String str_dest = "destination=" + dest.latitude + "," + dest.longitude;

		// Sensor enabled
		String sensor = "sensor=false";

		// Building the parameters to the web service
		String parameters = str_origin + "&" + str_dest + "&" + sensor;

		// Output format
		String output = "json";

		// Building the url to the web service
		String url = "https://maps.googleapis.com/maps/api/directions/" + output + "?" + parameters;

		return url;
	}

	/**
	 * A method to download json data from url
	 */
	private String downloadUrl(String strUrl) throws IOException {
		String data = "";
		InputStream iStream = null;
		HttpURLConnection urlConnection = null;
		try {
			URL url = new URL(strUrl);

			// Create an http connection to communicate with url
			urlConnection = (HttpURLConnection)url.openConnection();

			// Connection to url
			urlConnection.connect();

			// Reading data from url
			iStream = urlConnection.getInputStream();

			BufferedReader br = new BufferedReader(new InputStreamReader(iStream));

			StringBuffer sb = new StringBuffer();

			String line = "";
			while ((line = br.readLine()) != null) {
				sb.append(line);
			}

			data = sb.toString();

			br.close();
		} catch (Exception e) {
			Log.d("Exception while downloading url", e.toString());
		} finally {
			iStream.close();
			urlConnection.disconnect();
		}
		return data;
	}

	// Fetches data from url passed
	private class DownloadTask extends AsyncTask<String, Void, String> {
		private LatLng currentOrig;
		private LatLng currentDest;

		private int calculationCounter;

		public DownloadTask(boolean showLoading, LatLng currentOrig, LatLng currentDest, int calculationCounter) {
			super();
			this.currentOrig = currentOrig;
			this.currentDest = currentDest;
			this.calculationCounter = calculationCounter;
			// do stuff
		}

		@Override
		protected String doInBackground(String... url) {

			// For storing data from web service
			String data = "";

			Log.w("in DownloadTask ", calculationCounter + "****************");
			try {
				// Fetching the data from web service
				data = MainActivity.this.downloadUrl(url[0]);

				//Log.w("data!!!!! = ", this.calculationCounter + " " + data);
			} catch (Exception e) {
				Log.d("Background Task", e.toString());
			}
			return data;
		}

		//Executes in UI thread, after the parsing process
		// doInBackground()
		@Override
		protected void onPostExecute(String result) {
			super.onPostExecute(result);
			ParserTask parserTask = new ParserTask(false, currentOrig, currentDest, calculationCounter);

			// Save the new calculated startEnd route info
			SharedPreferences sharedPref = MainActivity.this.getSharedPreferences("LocationInfo", Context.MODE_PRIVATE);
			SharedPreferences.Editor editor = sharedPref.edit();
			editor.putString(currentOrig.toString() + ":=:" + currentDest.toString(), result);
			editor.commit();
			Log.w("DownloadTask", "Put new address into sharedpreferences");
			// Invokes the thread for parsing the JSON data
			parserTask.execute(result);
		}

	}


	/*
	 * A class to parse the Google Places in JSON format
	 */

	private class ParserTask extends AsyncTask<String, Integer, List<List<HashMap<String, String>>>> {

		private LatLng currentOrig;
		private LatLng currentDest;
		private int calculationCounter;

		public ParserTask(boolean showLoading, LatLng currentOrig, LatLng currentDest, int calculationCounter) {
			super();
			this.currentOrig = currentOrig;
			this.currentDest = currentDest;
			this.calculationCounter = calculationCounter;
			// do stuff
		}

		@Override
		protected List<List<HashMap<String, String>>> doInBackground(
				String... jsonData) {
			JSONObject jObject;
			List<List<HashMap<String, String>>> routes = null;

			try {

				Log.w("in ParserTask ", calculationCounter + " &********************");
				jObject = new JSONObject(jsonData[0]);
				DirectionsJSONParser parser = new DirectionsJSONParser();

				// Starts parsing data
				routes = parser.parse(jObject);

			} catch (Exception e) {
				e.printStackTrace();
				return null;
			}
			return routes;
		}

		// Executes in UI thread, after the parsing process
		@Override
		protected void onPostExecute(List<List<HashMap<String, String>>> result) {
			ArrayList<LatLng> points = null;

			MarkerOptions markerOptions = new MarkerOptions();

			String distance = "";
			String duration = "";
			if (result.size() < 1) {

				//Log.w("calculationCounter = ", this.calculationCounter + " " + currentOrig.toString() + " to " + currentDest.toString());
				Toast.makeText(getBaseContext(), "No Points", Toast.LENGTH_SHORT).show();

				return;
			}
			
			Log.w("Before parse the route info from server", "Before parse the route info from server");	
			// Traversing through all the routes 
			// In this app this outer for loop should only execute once
			for (int i = 0; i < result.size(); i++) {
				points = new ArrayList<LatLng>();

				//Fetching i-th route
				List<HashMap<String, String>> path = result.get(i);

				// Fetching all the points in i-th route
				for (int j = 0; j < path.size(); j++) {
					HashMap<String, String> point = path.get(j);
					if (j == 0) {
						// Get distance from the list
						distance = point.get("distance").split(" ")[0];
						continue;
					} else if (j == 1) {
						// Get duration from the list
						duration = point.get("duration");
						continue;
					}
					double lat = Double.parseDouble(point.get("lat"));
					double lng = Double.parseDouble(point.get("lng"));
					LatLng position = new LatLng(lat, lng);
					points.add(position);
				}
				if (i >= 1) {
					Toast.makeText(MainActivity.this.getBaseContext(), "too many points!!!!!!!!!", Toast.LENGTH_SHORT).show();
				}
				StartEndPair pair = new StartEndPair(currentOrig, currentDest);
				pairToWaypoints.put(pair, points);

				// Save the start end pair and the relevant 

				Log.w("inserted pair: ", pair.start.toString() + " " + pair.end.toString() + "$$$$$$$$$$$$$$$$");
				// The number should choose the number of people for the end point not the start point
				//if (pairToDistance != null && locationToNumber.get(currentDest) != null) {
				//Log.w("GOOD!!!", "PASSSSSSSSSSSSSSSSSSSSSS");
				pairToDistance.put(pair, Double.valueOf(distance));
				//}
				Log.w("locationList size  ", locationList.size() + " !!!!!!!!!!!!!!");
				Log.w("pairToWaypoints size  ", pairToWaypoints.size() + " !!!!!!!!!!!!!!");
				if (pairToWaypoints.size() == locationList.size() * (locationList.size() - 1)) {
					pdialog.hide();
					buildRouteButton.setEnabled(true);
					clearListButton.setEnabled(true);
					addToListButton.setEnabled(true);
					//Log.w("finish calculate ", pairToWaypoints.size() + " times###################33");

					//drawPolyline();
				}
			}

			//BuildRoute.this.tvDistanceDuration.setText("Distance: " + distance + ", Duration:" + duration);


		}

	}

	private void optimizeRoute() {
		optimalRoute.clear();
		tempRoute.clear();
		LatLng originalStart = locationList.get(0);
		tempRoute.add(originalStart);
		optimizeRouteHelper(0, 0.0, originalStart);
	}

	private void optimizeRouteHelper(int count, double tempDistance, LatLng currentStart) {
		//Log.w("count and size: ", count + " $$$$$$$$$$$$$$$$$$$$$ " + locationList.size());
		if (count == locationList.size() - 1) {
			if (tempDistance < shortestDistance) {
				shortestDistance = tempDistance;
				optimalRoute.clear();
				optimalRoute = new ArrayList<LatLng>(tempRoute);
			}
			return;
		}
		count++;
		List<LatLng> ends = startToEnds.get(currentStart);
		for (LatLng currentEnd : ends) {
			if (!tempRoute.contains(currentEnd)) {
				StartEndPair pair = new StartEndPair(currentStart, currentEnd);
				//Log.w("pair: ", pair.start.toString() + " " + pair.end.toString());
				if (pairToDistance == null) {
					//Log.w("pairToDistance: ", "NULL");
				} else if (pairToDistance.get(pair) == null) {
					Log.w("pairToDistance.get(pair) ", "NULL");
				}
				Log.w("the pair is ", pair.start.toString() + " to " + pair.end.toString());
				//Log.w("number is ", locationToNumber.get(currentEnd).toString());
				//Log.w("distance is ", pairToDistance.get(pair).toString());
				tempDistance += pairToDistance.get(pair)/locationToNumber.get(currentEnd); // override equal in StartEndPair
				tempRoute.add(count, currentEnd);

				optimizeRouteHelper(count, tempDistance, currentEnd);

				tempDistance -= pairToDistance.get(pair)/locationToNumber.get(currentEnd);
				tempRoute.remove(count);
			}
		}

	}

	private void addWayPoints() {
		//Log.w("optimalRoute size: ", optimalRoute.size() + "");
		for (int i = 0; i < optimalRoute.size() - 1; i++) {
			LatLng start = optimalRoute.get(i);
			LatLng end = optimalRoute.get(i + 1);
			Log.w("final route = ", start.toString() + " to" + end.toString());

			routeMessage.addAll(pairToWaypoints.get(new StartEndPair(start, end)));
			for (LatLng point : pairToWaypoints.get(new StartEndPair(start, end))) {
				//Log.w("waypoints: ", point.toString() + "YYYYYYYYYYYYYYYy");
			}
		}
	}

	public void clearList(View view) {
		// clear all the data structure	
		addressInfoList.clear();
		pairToWaypoints.clear();
		locationList.clear();
		locationToNumber.clear();
		pairToDistance.clear();
		optimalRoute.clear();
		tempRoute.clear();
		startToEnds.clear();
		routeMessage.clear();
		
		adapter.notifyDataSetChanged();
		
		SharedPreferences.Editor editor = savedListInfoPref.edit();
		editor.clear();
		editor.commit();
	}
}
