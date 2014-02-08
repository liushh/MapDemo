package com.example.mapdemo;

import java.util.ArrayList;

import android.os.Parcel;
import android.os.Parcelable;

import com.google.android.gms.maps.model.LatLng;

/**
 * A basic object that can be parcelled to
 * transfer between objects.
 */

public class ParcelableRouteMessage implements Parcelable
{
    private ArrayList<LatLng> routeMessage;

    /**
     * Standard basic constructor for non-parcel
     * object creation.
     */

    public ParcelableRouteMessage()
    {
    }

    /**
     *
     * Constructor to use when re-constructing object
     * from a parcel.
     *
     * @param in a parcel from which to read this object.
     */

    public ParcelableRouteMessage(Parcel in)
    {
        readFromParcel(in);
    }

    /**
     * Standard getter
     *
     * @return strValue
     */
    public ArrayList<LatLng> getStrValue()
    {
        return this.routeMessage;
    }

    /**
     * Standard setter
     *
     * @param strValue
     */

    public void setStrValue(ArrayList<LatLng> routeMessage) {
        this.routeMessage = routeMessage;
    }


    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        // We just need to write each field into the
        // parcel. When we read from parcel, they
        // will come back in the same order

        dest.writeArray(this.routeMessage.toArray());
    }

    /**
     *
     * Called from the constructor to create this
     * object from a parcel.
     *
     * @param in parcel from which to re-create object.
     */
    public void readFromParcel(Parcel in)
    {
        // We just need to read back each
        // field in the order that it was
        // written to the parcel
    }

    /**
    *
    * This field is needed for Android to be able to
    * create new objects, individually or as arrays.
    *
    * This also means that you can use use the default
    * constructor to create the object and use another
    * method to hyrdate it as necessary.
    */
    /*@SuppressWarnings("unchecked")
    public static final Parcelable.Creator CREATOR = new Parcelable.Creator()
    {
        @Override
        public ObjectA createFromParcel(Parcel in)
        {
            return new ObjectA(in);
        }

        @Override
        public Object[] newArray(int size)
        {
            return new ObjectA[size];
        }
    };*/
}