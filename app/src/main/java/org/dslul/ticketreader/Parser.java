package org.dslul.ticketreader;

import android.util.Log;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.concurrent.TimeUnit;


public class Parser {

    private String pages;
    private String date;
    private int remainingMins;

    public Parser(String data) {
        //if(data == null)

        this.pages = data;
        this.date = this.pages.substring(90, 96);
    }

    public String getDate() {
        String startingDate = "05/01/01 00:00:00";
        SimpleDateFormat format = new SimpleDateFormat("yy/MM/dd HH:mm:ss");
        Date date = null;
        try {
            date = format.parse(startingDate);
        } catch (ParseException e) {
            e.printStackTrace();
        }

        Date finalDate = addMinutesToDate(Long.parseLong(this.date, 16), date);

        //calcola minuti rimanenti
        Calendar c = Calendar.getInstance();
        long diff = (c.getTime().getTime() - finalDate.getTime()) / 60000;
        if(diff >= 90) {
            remainingMins = 0;
        } else {
            remainingMins = (int)(90 - diff);
        }


        return DateFormat.getDateTimeInstance(DateFormat.LONG, DateFormat.SHORT)
                .format(finalDate);
    }


    //TODO: corse in metropolitana (forse bit più significativo pag. 3)
    public int getRemainingRides() {
        int tickettype = (int)getBytesFromPage(5, 0, 1);
        int tickets;
            if(tickettype == 3) { //extraurbano
                tickets = (int) (~getBytesFromPage(3, 0, 4));
            } else {
                    tickets = (int)(~getBytesFromPage(3, 2, 2))
                                            & 0xFFFF;
            }
        return Integer.bitCount(tickets);
    }


    public int getRemainingMinutes() {
        return remainingMins;
    }



    private long getBytesFromPage(int page, int offset, int bytesnum) {
        return Long.parseLong(
                pages.substring(9 * page + offset * 2, 9 * page + offset * 2 + bytesnum * 2), 16);
    }



    private static Date addMinutesToDate(long minutes, Date beforeTime){
        final long ONE_MINUTE_IN_MILLIS = 60000;

        long curTimeInMs = beforeTime.getTime();
        Date afterAddingMins = new Date(curTimeInMs + (minutes * ONE_MINUTE_IN_MILLIS));
        return afterAddingMins;
    }


    private static byte[] hexStringToByteArray(String s) {
    int len = s.length();
    byte[] data = new byte[len / 2];
    for (int i = 0; i < len; i += 2) {
        data[i / 2] = (byte) ((Character.digit(s.charAt(i), 16) << 4)
                             + Character.digit(s.charAt(i+1), 16));
    }
    return data;
}

}
