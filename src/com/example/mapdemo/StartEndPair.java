package com.example.mapdemo;

import android.util.Log;

import com.google.android.gms.maps.model.LatLng;

public class StartEndPair {
	
	public LatLng start;
	public LatLng end;
	
	public StartEndPair(LatLng start, LatLng end) {
		this.start = start;
		this.end = end;
	}
	
	@Override
	public boolean equals(Object obj) {
		if (obj instanceof StartEndPair) {
			StartEndPair other = (StartEndPair)obj;
			return (other.start.latitude == this.start.latitude && 
					other.start.longitude == this.start.longitude &&
					other.end.latitude == this.end.latitude && 
					other.end.longitude == this.end.longitude);
		}	
		return false;
	}
	
	@Override
	public int hashCode() {
		return start.hashCode() * end.hashCode();
	}
}

