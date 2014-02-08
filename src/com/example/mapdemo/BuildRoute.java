package com.example.mapdemo;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.json.JSONObject;

import android.content.Intent;
import android.graphics.Color;
import android.location.Address;
import android.location.Geocoder;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import android.util.Log;
import android.widget.Toast;

import com.google.android.gms.maps.CameraUpdate;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.PolylineOptions;

/**
 * This shows how to create a simple activity with a map and a marker on the map.
 * <p>
 * Notice how we deal with the possibility that the Google Play services APK is not
 * installed/enabled/updated on a user's device.
 */
public class BuildRoute extends FragmentActivity {
    /**
     * Note that this may be null if the Google Play services APK is not available.
     */
    private GoogleMap mMap;
    
    private Map<StartEndPair, List<LatLng>> pairToWaypoints;
    private List<LatLng> locationList;
    private Map<LatLng, Double> locationToNumber;
    
    private Map<StartEndPair, Double> pairToDistance; // the distance here should consider the number of people who want to get to this location
    												// => Real DISTANCE = distance / number (to be optimized)
    
    private double shortestDistance = 10000.0; // any big number is fine
    
    private List<LatLng> optimalRoute; 
    
    private List<LatLng> tempRoute;
    
    private Map<LatLng, List<LatLng>> startToEnds;
    
    //private LatLng currentOrig;
    
    //private LatLng currentDest; 
    
    private ArrayList<LatLng> routeMessage;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_build_route);
        setUpMapIfNeeded();
        
        // Setup data structure
        pairToWaypoints = new HashMap<StartEndPair, List<LatLng>>();
        locationList = new ArrayList<LatLng>();
        locationToNumber = new HashMap<LatLng, Double>();
        pairToDistance = new HashMap<StartEndPair, Double>();
        optimalRoute = new ArrayList<LatLng>();
        tempRoute = new ArrayList<LatLng>();
        startToEnds = new HashMap<LatLng, List<LatLng>>();
        
        Intent intent = getIntent();
       	routeMessage = intent.getParcelableArrayListExtra(MainActivity.EXTRA_MESSAGE);
       	Log.w("size of routeMessage", "@@@@@@@@@@@@@@@@@@@   " + routeMessage.size());
       	drawPolyline();
       	/*
       	LatLng originalStart = new LatLng(47.657289,-122.309669);
       	locationList.add(originalStart);
       	locationToNumber.put(originalStart, 1.0);
       	for (int i = 0; i < addressList.length; i++) {
       		LatLng location = getLocation(addressList, i);
       		locationList.add(location);
       	}
       	
       	int calculationCounter = 0;
       	for (int i = 0; i < locationList.size(); i++) {
       		LatLng currentOrig = locationList.get(i);
       		List<LatLng> ends = new ArrayList<LatLng>();
       		for (int j = 0; j < locationList.size(); j++) {
       			if (j != i) {
       				calculationCounter++;
       				LatLng currentDest = locationList.get(j);
       				if (calculationCounter == 8) {
       					try {
							Thread.sleep(6000);
						} catch (InterruptedException e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
						}
       				}
       				Log.w("calculationCounter = ", currentOrig.toString() + " to " + currentDest.toString());
	       			ends.add(currentDest);
	       			// Getting URL to the Google Directions API
	       	       	String url = getDirectionsUrl(currentOrig, currentDest);
	       	       	DownloadTask downloadTask = new DownloadTask(false, currentOrig, currentDest, calculationCounter);
	       	       	// Start downloading json data from Google Directions API
	       	       	downloadTask.execute(url);
       			}
       		}
       		startToEnds.put(currentOrig, ends);
       	}
       	
       	Log.w("calculationCounter : ", "" + calculationCounter);
       	*/
    }
    
    private void optimizeRoute() {
    	LatLng originalStart = locationList.get(0);
    	tempRoute.add(originalStart);
    	optimizeRouteHelper(0, 0.0, originalStart);
    }
    
    private void optimizeRouteHelper(int count, double tempDistance, LatLng currentStart) {
    	Log.w("count and size: ", count + " $$$$$$$$$$$$$$$$$$$$$ " + locationList.size());
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
	    		Log.w("pair: ", pair.start.toString() + " " + pair.end.toString());
	    		if (pairToDistance == null) {
	    			Log.w("pairToDistance: ", "NULL");
	    		} else if (pairToDistance.get(pair) == null) {
	    			Log.w("pairToDistance.get(pair) ", "NULL");
	    		}
	    		tempDistance += pairToDistance.get(pair); // override equal in StartEndPair
	    		tempRoute.add(count, currentEnd);
	    		
	    		optimizeRouteHelper(count, tempDistance, currentEnd);
	    		
	    		tempDistance -= pairToDistance.get(pair);
	    		tempRoute.remove(count);
    		}
    	}
    	
    }
    
    

    @Override
    protected void onResume() {
        super.onResume();
        setUpMapIfNeeded();
    }

    /**
     * Sets up the map if it is possible to do so (i.e., the Google Play services APK is correctly
     * installed) and the map has not already been instantiated.. This will ensure that we only ever
     * call {@link #setUpMap()} once when {@link #mMap} is not null.
     * <p>
     * If it isn't installed {@link SupportMapFragment} (and
     * {@link com.google.android.gms.maps.MapView MapView}) will show a prompt for the user to
     * install/update the Google Play services APK on their device.
     * <p>
     * A user can return to this FragmentActivity after following the prompt and correctly
     * installing/updating/enabling the Google Play services. Since the FragmentActivity may not
     * have been completely destroyed during this process (it is likely that it would only be
     * stopped or paused), {@link #onCreate(Bundle)} may not be called again so we should call this
     * method in {@link #onResume()} to guarantee that it will be called.
     */
    private void setUpMapIfNeeded() {
        // Do a null check to confirm that we have not already instantiated the map.
        if (mMap == null) {
            // Try to obtain the map from the SupportMapFragment.
            mMap = ((SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map))
                    .getMap();
            
            // Check if we were successful in obtaining the map.
            if (mMap != null) {
                //setUpMap();
            	CameraUpdate center=
            	        CameraUpdateFactory.newLatLng(new LatLng(47.65918, -122.31194));
        	    CameraUpdate zoom=CameraUpdateFactory.zoomTo(15);

        	    mMap.moveCamera(center);
        	    mMap.animateCamera(zoom);
            }
        }
    }

    /**
     * This is where we can add markers or lines, add listeners or move the camera. In this case, we
     * just add a marker near Africa.
     * <p>
     * This should only be called once and when we are sure that {@link #mMap} is not null.
     */
    private void setUpMap() {
        mMap.addMarker(new MarkerOptions().position(new LatLng(0, 0)).title("Marker"));
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
    
    private LatLng getLocation(String[] addressInfoList, int index) {
    		String[] temp = addressInfoList[index].split(":=:");
    		String eastWest = temp[0];
    		String southNorth = temp[1];
    		String number = temp[2];
    		Log.w("getLocation called!!!!", "................");
    		try {
    			LatLng location = getIntersectionLocation(eastWest, southNorth);
    			locationToNumber.put(location, Double.valueOf(number));
    			Log.w("points to number: @@@@@@@@@@",  location.toString() + " " + number);
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
				data = BuildRoute.this.downloadUrl(url[0]);

				Log.w("data!!!!! = ", this.calculationCounter + " " + data);
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

				Log.w("calculationCounter = ", this.calculationCounter + " " + currentOrig.toString() + " to " + currentDest.toString());
				Toast.makeText(BuildRoute.this.getBaseContext(), "No Points", Toast.LENGTH_SHORT).show();
				
				return;
			}
			Log.w("in ParserTask ", calculationCounter + " &********************");
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
					Toast.makeText(BuildRoute.this.getBaseContext(), "too many points!!!!!!!!!", Toast.LENGTH_SHORT).show();
				}
				StartEndPair pair = new StartEndPair(currentOrig, currentDest);
				pairToWaypoints.put(pair, points);
				
				Log.w("pair: ", pair.start.toString() + " " + pair.end.toString());
				// The number should choose the number of people for the end point not the start point
				//if (pairToDistance != null && locationToNumber.get(currentDest) != null) {
					Log.w("GOOD!!!", "PASSSSSSSSSSSSSSSSSSSSSS");
					pairToDistance.put(pair, Double.valueOf(distance)/locationToNumber.get(currentDest));
				//}
				//Log.w("locationList size  ", locationList.size() + " !!!!!!!!!!!!!!");
				Log.w("pairToWaypoints size  ", pairToWaypoints.size() + " !!!!!!!!!!!!!!");
				if (pairToWaypoints.size() == locationList.size() * (locationList.size() - 1)) {
					Log.w("finish calculate ", pairToWaypoints.size() + " times###################33");
					optimizeRoute();
	       	   		drawPolyline();
				}
			}
			
			//BuildRoute.this.tvDistanceDuration.setText("Distance: " + distance + ", Duration:" + duration);
			
			
		}
    	
    }
    
    private void drawPolyline() {
    	Log.w("drawPolyling: ", "!!!!!!!!!!!!!!!!!!!!!!!!!!!!!! " + optimalRoute.size());
    	PolylineOptions lineOptions = new PolylineOptions();
		// Adding all the points in the route to LineOptions
		lineOptions.addAll(routeMessage);
		lineOptions.width(8);
		lineOptions.color(Color.RED);

		// Drawing polyline in the Google Map for the i-th route
		BuildRoute.this.mMap.addPolyline(lineOptions);
    } 
    
    private ArrayList<LatLng> addWayPoints() {
    	ArrayList<LatLng> points = new ArrayList<LatLng>();
    	Log.w("optimalRoute size: ", optimalRoute.size() + "");
    	for (int i = 0; i < optimalRoute.size() - 1; i++) {
    		LatLng start = optimalRoute.get(i);
    		LatLng end = optimalRoute.get(i + 1);
    		Log.w("final route = ", start.toString() + " to" + end.toString());
    		
    		points.addAll(pairToWaypoints.get(new StartEndPair(start, end)));
    		for (LatLng point : pairToWaypoints.get(new StartEndPair(start, end))) {
    			Log.w("waypoints: ", point.toString() + "YYYYYYYYYYYYYYYy");
    		}
    	}
    	return points;
    }
}





















































