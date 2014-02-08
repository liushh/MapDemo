package com.example.mapdemo;

public class AddressInfoItem {
	public String eastWest;
	public String southNorth;
	public String number;
	public int index1;
	public int index2;

	// Constructor
	public AddressInfoItem(String eastWest, String southNorth, String number, int index1, int index2) {
		this.eastWest = eastWest;
		this.southNorth = southNorth;
		this.number = number;
		this.index1 = index1;
		this.index2 = index2;
	}

	@Override
	public boolean equals(Object obj) {
		if (obj instanceof AddressInfoItem) {
			AddressInfoItem other = (AddressInfoItem)obj;
			return (this.eastWest == other.eastWest &&
					this.southNorth == other.southNorth);
		}	
		return false;
	}
	
	@Override
	public int hashCode() {
		return this.eastWest.hashCode() * this.southNorth.hashCode() * this.number.hashCode();
	}
}
