package com.example.mapdemo;

import com.google.android.gms.maps.model.LatLng;

public class Location {
	
	public LatLng location; 
	public double latitude;
	public double longitude;
	
	public Location(LatLng location) {
		this.location = location;
		this.latitude = location.latitude;
		this.longitude = location.longitude;
	}
	
	@Override
	public boolean equals(Object obj) {
		if (obj instanceof LatLng) {
			LatLng other = (LatLng)obj;
			return (other.latitude == this.latitude && other.longitude == this.longitude);
		}	
		return false;
	}
}
