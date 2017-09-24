package com.example.tiago.oscble2.Service;

/**
 * Created by tiago on 19/09/17.
 */

import android.util.Log;

import java.util.ArrayList;

import android.util.Log;
import android.widget.Toast;

import com.github.mikephil.charting.data.Entry;

import java.io.IOException;
import java.util.ArrayList;

/**
 * Created by tiago on 03/06/17.
 * this class is in charge of handling the message coming from BtService
 * and puts a string in Extras_fragmen edit text and an array of floats in LineChart_Fragment
 */

public class HandleMessage {

    private static final String TAG = "Tiago";

    StringBuilder recDataString = new StringBuilder();
    String stringToShow = null;
    ArrayList<Entry> values = new ArrayList<Entry>();
    ArrayList<Entry> valuesAux;



    public int validateData (String data) {
        // msg.arg1 = bytes from connect thread
        recDataString.append(data);

        int endOfLineIndex = recDataString.indexOf(">");                                // determine the end-of-line
        if (endOfLineIndex > 0) {
            int startOfLineIndex = recDataString.indexOf("<");                                // determine the start-of-line
            if (startOfLineIndex >= 0) {
                String auxRecDataString = recDataString.substring(startOfLineIndex + 1, endOfLineIndex);     // the +1 is for deleting the first character

                stringToShow = auxRecDataString;

                String[] strValues = auxRecDataString.split(";");
                for (int x = 0; x < strValues.length; x++) {
                    float y = 0;
                    try {
                        y = Float.parseFloat(strValues[x]);
                    } catch (Exception e) {
                        Log.e(TAG, "messageHandling exception", e);
                        recDataString.delete(0, recDataString.length());                            //clear all string data
                        //agregar aviso de mensaje no recibido
                    }

                    values.add(new Entry(x, y));
                }
                recDataString.delete(0, recDataString.length());                            //clear all string data
                valuesAux = new ArrayList<Entry>(values);                                   //copy values to valuesAux for values clean
                values.clear();
                return 0;
            }


        }
        return -1;
    }

    public String getStringToShow (){
        return stringToShow;
    }
    public ArrayList<Entry> getValues (){return valuesAux;}
}