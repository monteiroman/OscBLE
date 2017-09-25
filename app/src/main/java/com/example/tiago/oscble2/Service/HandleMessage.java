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
    int x=0;
    byte vDiv;
    byte tDiv;

    static final char SUP=255;
    static final char SDN=245;
    static final char SVD=243;
    static final char STD=247;

    char state;
    static final char SUP_READ=0;
    static final char SDN_READ=1;
    static final char SVD_READ=2;
    static final char STD_READ=3;
    static final char READ_HEADER=4;


    public int validateData (byte[] data) {
        // msg.arg1 = bytes from connect thread
        /*recDataString.append(data);

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
        return -1;*/





        for(int i=0; i<data.length; i++) {
            switch (state) {
                case READ_HEADER:

                    if (data[i] == SUP)
                        state = SUP_READ;
                    if (data[i] == SDN)
                        state = SDN_READ;
                    if (data[i] == SVD)
                        state = SVD_READ;
                    if (data[i] == STD)
                        state = STD_READ;

                    break;

                case SUP_READ:

                    if (data[i] < 241) {
                        values.add(new Entry(x, data[i]));
                    }
                    x++;
                    x %= 640;

                    state = READ_HEADER;

                    break;

                case SDN_READ:

                    if (data[i] < 241) {
                        values.add(new Entry(x, (-1) * data[i]));
                    }
                    x++;
                    x %= 640;

                    state = READ_HEADER;

                    break;

                case SVD_READ:

                    vDiv = data[i];

                    state = READ_HEADER;

                    break;

                case STD_READ:

                    tDiv = data[i];

                    state = READ_HEADER;

                    break;

                default:

                    state = READ_HEADER;

                    break;
            }
        }




                /*
                //Si corresponde a caracter de señalizacion
                case SUP:
                    //Vuelvo a leer el serie
                    b = Serial3.read();
                    //Evaluo si es valido y lo dibujo
                    if (b < 241) {
                        VGA.drawPixel(pos, MID - b, 1);
                        datach[pos] = b; //Lo meto en un buffer
                    }
                    //Marco que pasé por el serie
                    p = 1;
                    break;

                case SDN:
                    //Vuelvo a leer el serie
                    b = Serial3.read();
                    //Evaluo si es valido y lo dibujo
                    if (b < 241) {
                        VGA.drawPixel(pos, MID + b, 1);
                        datach[pos] = b; //Lo meto en un buffer
                    }
                    //Marco que pasé por el serie
                    p = 1;
                    break;

                //Si es informacion de tension la guardo
                case SVD:
                    b = Serial3.read();
                    Vdiv = b;
                    break;
                //Si es informacion de tiempo la guardo

                case STD:
                    b = Serial3.read();
                    Tdiv = b;
                    break;

                //Si era dato dejo pasar y busco señalziacion
                default:
                    break;
            }
            //reseteo el contador
            if (p) {
                pos++;
                if (pos == 640) pos = 0;
            }




        }*/




  /*      #define SUP 255
        #define SDN 245
        #define SVD 243
        #define STD 247




        void serie ()
        {

            while (Serial3.available())
            {
    //variables para la función
                unsigned char b;
                unsigned char p = 0;
                unsigned char a;

    //borrado de valores previos
                VGA.drawPixel(pos, MID + datach[pos], 0);
                VGA.drawPixel(pos, MID - datach[pos], 0);

    //Leo una vez el serie
                a = Serial3.read();


    //Evaluo que lei
                switch (a)
                {
      //Si corresponde a caracter de señalizacion
                    case SUP:
        //Vuelvo a leer el serie
                        b = Serial3.read();
        //Evaluo si es valido y lo dibujo
                        if (b < 241) {
                            VGA.drawPixel(pos, MID - b, 1);
                            datach[pos] = b; //Lo meto en un buffer
                        }
        //Marco que pasé por el serie
                        p = 1;
                        break;

                    case SDN:
        //Vuelvo a leer el serie
                        b = Serial3.read();
        //Evaluo si es valido y lo dibujo
                        if (b < 241) {
                            VGA.drawPixel(pos, MID + b, 1);
                            datach[pos] = b; //Lo meto en un buffer
                        }
        //Marco que pasé por el serie
                        p = 1;
                        break;

      //Si es informacion de tension la guardo
                    case SVD:
                        b = Serial3.read();
                        Vdiv = b;
                        break;
      //Si es informacion de tiempo la guardo

                    case STD:
                        b = Serial3.read();
                        Tdiv = b;
                        break;

      //Si era dato dejo pasar y busco señalziacion
                    default:
                        break;
                }
    //reseteo el contador
                if (p) {
                    pos++;
                    if (pos == 640) pos = 0;
                }

            }

        }*/



        return 0;
    }

    public String getStringToShow (){
        return stringToShow;
    }
    public ArrayList<Entry> getValues (){return values;}
    public byte getTDiv (){ return tDiv; }
    public byte getVDiv (){ return vDiv; }
}