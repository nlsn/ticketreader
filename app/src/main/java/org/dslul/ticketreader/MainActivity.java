package org.dslul.ticketreader;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.View;
import android.view.Menu;
import android.view.MenuItem;

import android.widget.Button;
import android.widget.TextView;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.concurrent.TimeUnit;
import android.content.ClipboardManager;
import android.content.ClipData;
import android.content.Context;

import android.nfc.NfcAdapter;
import android.nfc.tech.NfcA;

import android.widget.Toast;

import android.content.Intent;
import android.content.IntentFilter;

import android.app.PendingIntent;

import android.os.Handler;
import android.os.Message;

import android.app.AlertDialog;

import android.content.DialogInterface;



public class MainActivity extends AppCompatActivity {

    private NfcAdapter mNfcAdapter;
	private IntentFilter tech;
	private IntentFilter[] intentFiltersArray;
	private PendingIntent pendingIntent;
	private Intent intent;
	private AlertDialog alertDialog;

	private String pages = "ERROR";

	private TextView dataout;

    private static final int ACTION_NONE  = 0;
	private static final int ACTION_READ  = 1;
	private int scanAction;

    // list of NFC technologies detected:
	private final String[][] techListsArray = new String[][] {
		new String[] {
			//MifareUltralight.class.getName(),
			NfcA.class.getName()
		}
	};


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        mNfcAdapter = NfcAdapter.getDefaultAdapter(this);
		if (mNfcAdapter == null) {
			// Stop here, we definitely need NFC
			Toast.makeText(this, "Questo dispositivo non supporta la tecnologia NFC.", Toast.LENGTH_LONG).show();
			finish();
			return;
		}

		if (!mNfcAdapter.isEnabled()) {
			Toast.makeText(this, "NFC disabilitato. Attiva l'NFC e torna indietro.", Toast.LENGTH_LONG).show();
	        startActivity(new Intent(android.provider.Settings.ACTION_WIRELESS_SETTINGS));
		}

		tech = new IntentFilter(NfcAdapter.ACTION_TECH_DISCOVERED);
		intentFiltersArray = new IntentFilter[] {tech};
		intent = new Intent(this, getClass()).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);
		//FLAG_ACTIVITY_REORDER_TO_FRONT FLAG_RECEIVER_REPLACE_PENDING
		pendingIntent = PendingIntent.getActivity(this, 0, intent, 0);

		scanAction = ACTION_READ;

        dataout = new TextView(this);
    }

    @Override
	protected void onResume() {
		super.onResume();
		mNfcAdapter.enableForegroundDispatch(this, pendingIntent, intentFiltersArray, this.techListsArray);
	}

	@Override
	protected void onPause() {
		// disabling foreground dispatch:
		//NfcAdapter nfcAdapter = NfcAdapter.getDefaultAdapter(this);
		mNfcAdapter.disableForegroundDispatch(this);
		super.onPause();
	}

    @Override
    protected void onNewIntent(Intent intent) {
        if (intent.getAction().equals(NfcAdapter.ACTION_TECH_DISCOVERED)) {

            String mTextBufferText = "aa";

            NfcThread nfcThread = new NfcThread(intent, scanAction, mTextBufferText, mTextBufferHandler, mToastShortHandler, mToastLongHandler, mShowInfoDialogHandler);
            nfcThread.start();

            scanAction = ACTION_READ;
        }
    }

	private Handler mTextBufferHandler = new Handler() {
		public void handleMessage(Message msg) {
			pages = (String)msg.obj;
            if(pages != "ERROR") {
                dataout = (TextView) findViewById(R.id.outdata);
                Parser parser = new Parser(pages);
                dataout.setText("Data obliterazione: " + parser.getDate()
                                + System.getProperty("line.separator")
                                + "Corse residue: " + parser.getRemainingRides());
            }
		}
	};

	private Handler mToastShortHandler = new Handler() {
		public void handleMessage(Message msg) {
			String text = (String)msg.obj;
			Toast.makeText(MainActivity.this, text, Toast.LENGTH_SHORT).show();
		}
	};

	private Handler mToastLongHandler = new Handler() {
		public void handleMessage(Message msg) {
			String text = (String)msg.obj;
			Toast.makeText(MainActivity.this, text, Toast.LENGTH_LONG).show();
		}
	};

	private Handler mShowInfoDialogHandler = new Handler() {
		public void handleMessage(Message msg) {
			String text = (String)msg.obj;
			//infoDialog = showInfoDialog(text);
			//infoDialog.show();
		}
	};



    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_info) {
            alertDialog = showAlertDialog("Semplice applicazione opensource per visualizzare le " +
                    "corse rimanenti nei biglietti GTT.");
            alertDialog.show();
			return true;
        }

        return super.onOptionsItemSelected(item);
    }


	private AlertDialog showAlertDialog(String message) {
		DialogInterface.OnClickListener dialogInterfaceListener = new DialogInterface.OnClickListener() {
			@Override
			public void onClick(DialogInterface dialog, int which) {
				alertDialog.cancel();
				scanAction = ACTION_READ;
			}
		};

		alertDialog = new AlertDialog.Builder(this)
				.setTitle("Informazioni")
				//.setIcon(R.drawable.ic_launcher)
				.setMessage(message)
				.create();

		alertDialog.setOnCancelListener(new DialogInterface.OnCancelListener() {

			public void onCancel(DialogInterface dialog) {
				scanAction = ACTION_READ;
			}
		});

		return alertDialog;
	}

}
