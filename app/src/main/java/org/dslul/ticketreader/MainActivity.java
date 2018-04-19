package org.dslul.ticketreader;

import android.os.Bundle;
import android.os.CountDownTimer;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.CardView;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.View;
import android.view.Menu;
import android.view.MenuItem;

import android.widget.Button;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TableLayout;
import android.widget.TextView;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
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

import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdView;
import com.google.android.gms.ads.MobileAds;


public class MainActivity extends AppCompatActivity {

    private NfcAdapter mNfcAdapter;
	private IntentFilter tech;
	private IntentFilter[] intentFiltersArray;
	private PendingIntent pendingIntent;
	private Intent intent;
	private AlertDialog alertDialog;

	private Toast currentToast;

	private String pages = "ERROR";

    private AdView adview;
    private ImageView imageNfc;
    private CardView ticketCard;
    private CardView statusCard;
    private ImageView statusImg;
    private TextView statoBiglietto;
    private TextView infoLabel;
	private TableLayout infoTable;
	private TextView dataObliterazione;
	private TextView corseRimanenti;

	private CountDownTimer timer;

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

        adview = (AdView) findViewById(R.id.adView);
        imageNfc = (ImageView) findViewById(R.id.imagenfcView);
        ticketCard = (CardView) findViewById(R.id.ticketCardView);
        statusCard = (CardView) findViewById(R.id.statusCardView);
        statusImg = (ImageView) findViewById(R.id.statusImg);
        statoBiglietto = (TextView) findViewById(R.id.stato_biglietto);
        infoLabel = (TextView) findViewById(R.id.infolabel);
        infoTable = (TableLayout) findViewById(R.id.info_table);
        dataObliterazione = (TextView) findViewById(R.id.data_obliterazione);
        corseRimanenti = (TextView) findViewById(R.id.corse_rimaste);

        MobileAds.initialize(this, "ca-app-pub-3940256099942544/6300978111");
        AdRequest adRequest = new AdRequest.Builder().build();
        adview.loadAd(adRequest);

        mNfcAdapter = NfcAdapter.getDefaultAdapter(this);
		if (mNfcAdapter == null) {
			Toast.makeText(this, R.string.nfc_not_supported, Toast.LENGTH_LONG).show();
			finish();
			return;
		}

		if (!mNfcAdapter.isEnabled()) {
			Toast.makeText(this, R.string.nfc_disabled, Toast.LENGTH_LONG).show();
	        startActivity(new Intent(android.provider.Settings.ACTION_WIRELESS_SETTINGS));
		}

		tech = new IntentFilter(NfcAdapter.ACTION_TECH_DISCOVERED);
		intentFiltersArray = new IntentFilter[] {tech};
		intent = new Intent(this, getClass()).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);
		//FLAG_ACTIVITY_REORDER_TO_FRONT FLAG_RECEIVER_REPLACE_PENDING
		pendingIntent = PendingIntent.getActivity(this, 0, intent, 0);

		scanAction = ACTION_READ;

        onNewIntent(getIntent());

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

			NfcThread nfcThread = new NfcThread(getBaseContext(), intent, scanAction, mTextBufferText, mTextBufferHandler, mToastShortHandler, mToastLongHandler, mShowInfoDialogHandler);
			nfcThread.start();

			scanAction = ACTION_READ;
		}
    }

	private Handler mTextBufferHandler = new Handler() {
		public void handleMessage(Message msg) {
			pages = (String)msg.obj;
            if(timer != null)
                timer.cancel();
            if(pages != "ERROR") {
                Parser parser = new Parser(pages);
				dataObliterazione.setText(parser.getDate());
				corseRimanenti.setText(Integer.toString(parser.getRemainingRides()));

                if(parser.getRemainingMinutes() != 0) {
                    statoBiglietto.setText(R.string.in_corso);
                    statusImg.setImageResource(R.drawable.ic_restore_grey_800_36dp);
                    statusCard.setCardBackgroundColor(0xFF90CAF9);
                    Calendar calendar = Calendar.getInstance();
                    int sec = calendar.get(Calendar.SECOND);
                    timer = new CountDownTimer((parser.getRemainingMinutes()*60 - sec)*1000, 1000) {

                        public void onTick(long millis) {
                            statoBiglietto.setText(String.format(getResources().getString(R.string.in_corso),
                                    TimeUnit.MILLISECONDS.toMinutes(millis),
                                    TimeUnit.MILLISECONDS.toSeconds(millis) -
                                    TimeUnit.MINUTES.toSeconds(TimeUnit.MILLISECONDS.toMinutes(millis))));
                        }

                        public void onFinish() {
                            statoBiglietto.setText(R.string.corse_esaurite);
                            statusImg.setImageResource(R.drawable.ic_error_grey_800_36dp);
                            statusCard.setCardBackgroundColor(0xFFEF9A9A);
            				if(timer != null)
                            	timer.cancel();
                        }

                        }.start();
                } else if(parser.getRemainingRides() == 0 && parser.getRemainingMinutes() == 0) {
                    statoBiglietto.setText(R.string.corse_esaurite);
                    statusImg.setImageResource(R.drawable.ic_error_grey_800_36dp);
                    statusCard.setCardBackgroundColor(0xFFEF9A9A);
                } else if(parser.getRemainingRides() != 0 && parser.getRemainingMinutes() == 0) {
                    statoBiglietto.setText(String.format(getResources().getString(R.string.corse_disponibili), parser.getRemainingRides()));
                    statusImg.setImageResource(R.drawable.ic_check_circle_grey_800_36dp);
                    statusCard.setCardBackgroundColor(0xFFA5D6A7);
                }

                statusCard.setVisibility(View.VISIBLE);
                ticketCard.setVisibility(View.VISIBLE);
				infoLabel.setText(R.string.read_another_ticket);
                imageNfc.setVisibility(View.GONE);


            } else {
                statusCard.setVisibility(View.GONE);
                ticketCard.setVisibility(View.GONE);
                infoLabel.setText(R.string.info_instructions);
                imageNfc.setVisibility(View.VISIBLE);
			}
		}
	};

	private Handler mToastShortHandler = new Handler() {
		public void handleMessage(Message msg) {
			String text = (String)msg.obj;
            if(currentToast != null)
			    currentToast.cancel();
			currentToast = Toast.makeText(MainActivity.this, text, Toast.LENGTH_SHORT);
			currentToast.show();
		}
	};

	private Handler mToastLongHandler = new Handler() {
		public void handleMessage(Message msg) {
			String text = (String)msg.obj;
			if(currentToast != null)
			    currentToast.cancel();
			currentToast = Toast.makeText(MainActivity.this, text, Toast.LENGTH_LONG);
			currentToast.show();
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
            alertDialog = showAlertDialog(getString(R.string.info_message));
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
				.setTitle(R.string.information)
				.setIcon(android.R.drawable.ic_dialog_info)
				.setMessage(message)
   				.setPositiveButton(R.string.close_dialog, null)
				.create();

		alertDialog.setOnCancelListener(new DialogInterface.OnCancelListener() {

			public void onCancel(DialogInterface dialog) {
				scanAction = ACTION_READ;
			}
		});

		return alertDialog;
	}

}
