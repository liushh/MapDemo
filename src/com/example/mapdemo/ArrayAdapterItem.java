package com.example.mapdemo;

import java.util.List;


import android.app.Activity;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

public class ArrayAdapterItem extends ArrayAdapter<AddressInfoItem> {
	
	Context mContext;
	int layoutResourceId;
	List<AddressInfoItem> addressList = null;
	
	public ArrayAdapterItem(Context mContext, int layoutResourceId,
			List<AddressInfoItem> addressList) {
		super(mContext, layoutResourceId, addressList);
		// TODO Auto-generated constructor stub

		this.layoutResourceId = layoutResourceId;
		this.mContext = mContext;
		this.addressList = addressList;
	}
	
	public View getView(int position, View convertView, ViewGroup parent) {
		if (convertView == null) {
			// inflate the layout
			LayoutInflater inflater = ((Activity) mContext).getLayoutInflater();
			convertView = inflater.inflate(layoutResourceId, parent, false);
		}
		
		AddressInfoItem addressInfoItem = addressList.get(position);
		
		TextView textViewItem = (TextView) convertView.findViewById(R.id.textViewItem);
		textViewItem.setText(addressInfoItem.number + ": " + addressInfoItem.eastWest + " & " + addressInfoItem.southNorth);
		return convertView;
	}
}
