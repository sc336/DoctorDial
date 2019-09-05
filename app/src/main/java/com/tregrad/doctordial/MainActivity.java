package com.tregrad.doctordial;

import android.Manifest;
import android.app.Activity;
import android.app.PendingIntent;
import android.content.ComponentName;
import android.content.ContentResolver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.database.Cursor;
import android.media.AudioManager;
import android.media.Image;
import android.net.Uri;
import android.os.Handler;
import android.os.IBinder;
import android.provider.ContactsContract;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.telephony.TelephonyManager;
import android.util.Log;
import android.util.Patterns;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.Adapter;
import android.widget.AdapterView;
import android.widget.AutoCompleteTextView;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.SimpleAdapter;
import android.widget.Toast;


import com.android.vending.billing.IInAppBillingService;
import com.google.android.gms.appindexing.Action;
import com.google.android.gms.appindexing.AppIndex;
import com.google.android.gms.common.api.GoogleApiClient;

import org.json.JSONObject;

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.prefs.Preferences;

public class MainActivity extends AppCompatActivity {

    //private ArrayAdapter<String> contactListAdapter;
    public final int REQUEST_AUTOCOMPLETE = 1;
    public final int REQUEST_BILLING = 2;

    MainActivity thisActivity = this;
    HashMap<String, String> tipOptions = new HashMap<>();
    int delay_millis = 15000;
    //TODO: consider going back to sdkTarget 22, with old permissions model

    private Runnable dial = new Runnable() {
        @Override
        public void run() {
            int callState = ((TelephonyManager) getBaseContext().getSystemService(Context.TELEPHONY_SERVICE)).getCallState();
            Log.d("dial", "" + callState);
            switch (callState) {
                case TelephonyManager.CALL_STATE_IDLE:
                    Intent intent = new Intent(Intent.ACTION_CALL);

                    intent.setData(Uri.parse("tel:" + ((EditText) findViewById(R.id.numberEdit)).getText()));

                    if (ActivityCompat.checkSelfPermission(getBaseContext(), Manifest.permission.CALL_PHONE) != PackageManager.PERMISSION_GRANTED) {
                        throw new RuntimeException("Need to permission make calls");
                    }
                    intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    getBaseContext().startActivity(intent);
                    spamHandler.postDelayed(this, delay_millis);
                    break;
                case TelephonyManager.CALL_STATE_OFFHOOK:
                    Log.d("dial", "Activating speaker");
                    FsmDial.getInstance().halt();
                    AudioManager audio = (AudioManager) getBaseContext().getSystemService(Context.AUDIO_SERVICE);
                    audio.setMode(AudioManager.MODE_IN_CALL);
                    audio.setSpeakerphoneOn(true);
                    findViewById(R.id.stopButton).setBackgroundColor(ContextCompat.getColor(getBaseContext(), R.color.abortGray));
            }
        }
    };

    private Handler spamHandler = new Handler();
    private boolean goToggle = true;

    private static int NEEDS_PHONE = 1;
    private static int NEEDS_CONTACTS = 2;
    /**
     * ATTENTION: This was auto-generated to implement the App Indexing API.
     * See https://g.co/AppIndexing/AndroidStudio for more information.
     */
    private GoogleApiClient client;

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        //TODO: Maybe actually handle this
        actual_init();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        if(getPreferences(MODE_PRIVATE).getBoolean("SHOW_TUTORIAL", true)) {
            Log.i("Startup", "Tutorial activity launching");
            Intent intent = new Intent(getBaseContext(), TutorialActivity.class);
            startActivity(intent);
        }

        super.onCreate(savedInstanceState);

        // Permissions

        boolean has_phone = ContextCompat.checkSelfPermission(thisActivity, Manifest.permission.CALL_PHONE) == PackageManager.PERMISSION_GRANTED;
        boolean has_contacts = ContextCompat.checkSelfPermission(thisActivity, Manifest.permission.READ_CONTACTS) == PackageManager.PERMISSION_GRANTED;

        Log.i("", "Phone permissions: " + has_phone);
        Log.i("", "Contact permissions: " + has_contacts);

        if (has_phone && has_contacts) {
            Log.i("", "Permissions granted");
            actual_init();
        } else if (!has_contacts && !has_phone) {
            Log.i("", "Call & Contacts permission denied, requesting");
            //Ask permission
            ActivityCompat.requestPermissions(thisActivity, new String[]{Manifest.permission.CALL_PHONE, Manifest.permission.READ_CONTACTS}, NEEDS_PHONE & NEEDS_CONTACTS);
        } else if (!has_phone) {
            Log.i("", "Call permission denied, requesting");
            //Ask permission
            ActivityCompat.requestPermissions(thisActivity, new String[]{Manifest.permission.CALL_PHONE}, NEEDS_PHONE);
        } else if (!has_contacts) {
            Log.i("", "Contacts permission denied, requesting");
            //Ask permission
            ActivityCompat.requestPermissions(thisActivity, new String[]{Manifest.permission.READ_CONTACTS}, NEEDS_CONTACTS);
        } else throw new RuntimeException("This should be logically impossible");

        billingInit();

    }

    private IInAppBillingService[] mServiceWrapper;
    private ServiceConnection mServiceConn;

    //Method created for tidiness, this is all effectively inline in onCreate
    private void billingInit() {
        //TODO: consider making a separate object to handle billing
        //reference: https://developer.android.com/google/play/billing/billing_integrate.html
        // Connect billing for tips
        mServiceWrapper = new IInAppBillingService[]{null};
        mServiceConn = (new ServiceConnection() {
            private IInAppBillingService[] mServiceWrapper = new IInAppBillingService[]{null};

            @Override
            public void onServiceDisconnected(ComponentName name) {
                mServiceWrapper[0] = null;
                Log.i("mServiceConn", "Service disconnected");
            }

            @Override
            public void onServiceConnected(ComponentName name,
                                           IBinder service) {
                mServiceWrapper[0] = IInAppBillingService.Stub.asInterface(service);
                Log.i("mServiceConn", "Service connected");
            }

            private ServiceConnection initialise(IInAppBillingService[] msw) {
                mServiceWrapper = msw;
                return this;
            }
        }).initialise(mServiceWrapper);

        // bindService here
        Intent serviceIntent = new Intent("com.android.vending.billing.InAppBillingService.BIND");
        serviceIntent.setPackage("com.android.vending");
        //TODO: consider using startService so the service doesn't get killed while it's in the background
        bindService(serviceIntent, mServiceConn, Context.BIND_AUTO_CREATE | Context.BIND_NOT_FOREGROUND);

        // ATTENTION: This was auto-generated to implement the App Indexing API.
        // See https://g.co/AppIndexing/AndroidStudio for more information.
        client = new GoogleApiClient.Builder(this).addApi(AppIndex.API).build();

    }

    private View.OnClickListener goListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            //TODO: add code to stop multiple scheduling
            if (FsmDial.getInstance().getState() == FsmDial.State.IDLE) {
                String number = ((EditText) findViewById(R.id.numberEdit)).getText().toString();
                number = number.trim();
                ((EditText) findViewById(R.id.numberEdit)).setText(number);
                Log.i("goButton", "Attempting to call " + number);
                if (Patterns.PHONE.matcher(number).matches()) {
                    spamHandler.post(dial);
                    FsmDial.getInstance().activate();
                    //findViewById(R.id.stopButton).setBackgroundColor(ContextCompat.getColor(getBaseContext(), R.color.abortRed));
                    ((ImageButton) findViewById(R.id.stopButton)).setImageResource(R.drawable.abort);
                } else {
                    Toast.makeText(getApplicationContext(), "Not a valid phone number", Toast.LENGTH_SHORT).show();
                }
            }

        }
    };

    private View.OnClickListener stopListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            Toast.makeText(getApplicationContext(), "Dialing aborted", Toast.LENGTH_SHORT).show();
            FsmDial.getInstance().halt();
            spamHandler.removeCallbacks(dial);
            //findViewById(R.id.stopButton).setBackgroundColor(ContextCompat.getColor(getBaseContext(), R.color.abortGray));
            ((ImageButton) findViewById(R.id.stopButton)).setImageResource(R.drawable.abort_gray);
        }
    };

    private void actual_init() {
        Log.i("actual_init", "");
        setContentView(R.layout.activity_main);

        getSupportActionBar().setDisplayShowHomeEnabled(true);
        getSupportActionBar().setLogo(R.drawable.drdial_logo);
        getSupportActionBar().setDisplayUseLogoEnabled(true);

        findViewById(R.id.goButton).setOnClickListener(goListener);
        findViewById(R.id.stopButton).setOnClickListener(stopListener);
        findViewById(R.id.contactsButton).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //Intent intent = new Intent(Intent.ACTION_PICK, ContactsContract.Contacts.CONTENT_URI);
                Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
                intent.setType(ContactsContract.CommonDataKinds.Phone.CONTENT_ITEM_TYPE);
                intent.setPackage("com.google.android.contacts");
                List<ResolveInfo> rInfos = getPackageManager().queryIntentActivities(intent, PackageManager.MATCH_ALL);
                //Log.i("Rinfos:", Integer.toString(rInfos.size()));
                if (rInfos.size() < 1) intent.setPackage(null);
                startActivityForResult(intent, 1); //1 signals picking contact from list
            }
        });
        findViewById(R.id.tipButton).setOnClickListener(new View.OnClickListener() {
            //TODO: Refactor this into its own class
            @Override
            public void onClick(View v) {
                //Confirm SKU for billing
                //TODO: take this code, shove it in an AsyncTask, and start it off once the connection is established... maybe?
                // probably not

                ArrayList<String> skuList = new ArrayList<>();
                skuList.add("tip1");
                Bundle querySkus = new Bundle();
                querySkus.putStringArrayList("ITEM_ID_LIST", skuList);
                Bundle skuDetails = new Bundle();
                JSONObject jo;
                try {
                    Bundle ownedItems = mServiceWrapper[0].getPurchases(3, getPackageName(), "inapp", null);
                    if (ownedItems.getInt("RESPONSE_CODE") == 0)  //0 is RESPONSE_OK - not sure why this isn't recognised here
                    {
                        ArrayList<String>  purchaseDataList = ownedItems.getStringArrayList("INAPP_PURCHASE_DATA_LIST");
                        Iterator<String> i = purchaseDataList.iterator();
                        while (i.hasNext()) {
                            String item = i.next();
                            jo = new JSONObject(item);
                            Log.i("PurchaseData", jo.toString());
                            //There's only one type of purchase, and it doesn't provision anything to the user, so just consume here
                            mServiceWrapper[0].consumePurchase(3, getPackageName(), jo.getString("purchaseToken"));
                        }


                    }
                }
                catch (Exception e) {
                    Log.e("billing sweep", e.getMessage());
                }

                try {
                    skuDetails = mServiceWrapper[0].getSkuDetails(3, getPackageName(), "inapp", querySkus);

                    int response = skuDetails.getInt("RESPONSE_CODE");
                    if (response == 0) {
                        ArrayList<String> responseList = skuDetails.getStringArrayList("DETAILS_LIST");
                        String sku = "ERROR", price = "ERROR";
                        //TODO:Process responseList here
                        for (String thisResponse : responseList) {
                            jo = new JSONObject(thisResponse);
                            sku = jo.getString("productId");
                            price = jo.getString("price");
                            //Do stuff with the pricing here
                        }
                        tipOptions.put(sku, price);
                    }
                } catch (Exception e) {
                    Log.e("Query skus", "foo", e);
                }
                Log.i("Query skus", tipOptions.toString());
                Toast.makeText(getApplicationContext(), tipOptions.toString(), Toast.LENGTH_LONG);
                DialogInterface.OnClickListener dialogListener = new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        //FIXME: Purchases not working here - possibly because purchase not correctly consumed in response listener?
                        //This probably requires using the Helper classes
                        switch (which) {
                            case DialogInterface.BUTTON_POSITIVE:
                                try {
                                    //NB: developerPayload is left blank ("") here.  It's a way of passing information through the whole process
                                    Bundle buyIntentBundle = mServiceWrapper[0].getBuyIntent(3, getPackageName(), "tip1", "inapp", "");
                                    PendingIntent pendingIntent = buyIntentBundle.getParcelable("BUY_INTENT");
                                    startIntentSenderForResult(pendingIntent.getIntentSender(), REQUEST_BILLING, null, 0,0,0);
                                    Log.i("TipButton", "Tip billing request sent");
                                } catch (Exception e) {
                                    Toast.makeText(getApplicationContext(), "Something went wrong with your billing", Toast.LENGTH_LONG).show();
                                    Log.d("Tip clicked", e.getMessage());
                                }
                                break;
                        }
                    }
                };
                AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(thisActivity);
                //TODO: check this works

                dialogBuilder.setMessage("Thanks for your support!  Just tap 'Yes' to confirm tipping the developer £1.")
                        .setPositiveButton("Yes! Have £1 on me", dialogListener)
                        .setNegativeButton("No!  I will not give you anything", dialogListener)
                        .show();
            }

        });


        AutoCompleteTextView autoTest = ((AutoCompleteTextView) findViewById(R.id.numberEdit));

        autoTest.setAdapter((SimpleAdapter) getAutoCompleteAdapter());
        autoTest.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> av, View arg1, int index, long arg3) {
                Map<String, String> map = (Map<String, String>) av.getItemAtPosition(index);
                String number = map.get("Phone");
                ((AutoCompleteTextView) findViewById(R.id.numberEdit)).setText(number);
            }
        });
    }

    @Override
    public void onActivityResult(int reqCode, int resultCode, Intent data) {
        //TODO: Needs cleaning up
        super.onActivityResult(reqCode, resultCode, data);
        if (reqCode == REQUEST_AUTOCOMPLETE) {
            if (resultCode == Activity.RESULT_OK) {
                Uri contactData = data.getData();
                Cursor c = getContentResolver().query(contactData, new String[]{
                        ContactsContract.CommonDataKinds.Phone.NUMBER,
                        ContactsContract.CommonDataKinds.Phone.TYPE}, null, null, null);
                if (c != null && c.moveToFirst()) {
                    String number = c.getString(0);
                    ((EditText) findViewById(R.id.numberEdit)).setText(number);
                }
            }
        } else if(reqCode == REQUEST_BILLING) {
            try {
                final JSONObject jo = new JSONObject(data.getStringExtra("INAPP_PURCHASE_DATA"));
                Log.d("INAPP_PURCHASE_DATA", jo.toString());
                Log.i("Billing result", "Billing acknowledgement intent on order " + jo.getString("orderId") + " received");
                Log.i("Billing result", "with resultCode " + resultCode);
                try {
                    if(mServiceWrapper[0].consumePurchase(3,"com.tregrad.doctordial", jo.getString("purchaseToken")) == RESULT_OK) {
                        Toast.makeText(getApplicationContext(), "Thanks for your support!", Toast.LENGTH_SHORT).show();
                        Log.i("consumePurchase", "Purchase consumed: " + jo.getString("purchaseToken"));
                    }
                } catch (Exception e) {
                    Log.d("consumePurchase", e.getMessage());
                    Toast.makeText(getApplicationContext(), "-", Toast.LENGTH_LONG).show();
                }
            } catch (Exception e) {
                Log.d("Billing result", e.getMessage());
            }
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.activity_main, menu);
        delay_millis = getPreferences(MODE_PRIVATE).getInt("DELAY_MILLIS", 15000);
        return true;
    }

    boolean menuInit = true;

    @Override
    public boolean onMenuOpened(int featureId, Menu menu) {

        if(menu==null)
            return super.onMenuOpened(featureId, menu);


        if(menuInit) {
            switch (delay_millis) {
                case 10000:
                    menu.findItem(R.id.delay10).setChecked(true);
                    break;
                case 15000:
                    menu.findItem(R.id.delay15).setChecked(true);
                    break;
                case 20000:
                    menu.findItem(R.id.delay20).setChecked(true);
                    break;
                case 30000:
                    menu.findItem(R.id.delay30).setChecked(true);
                    break;
            }

            menu.findItem(R.id.toggle_tutorial).setChecked(getPreferences(MODE_PRIVATE).getBoolean("SHOW_TUTORIAL", true));
            menuInit = false;
        }
        return super.onMenuOpened(featureId, menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        SharedPreferences pref = getPreferences(MODE_PRIVATE);
        // Delay items
        switch (item.getItemId()) {
            case R.id.delay10:
                delay_millis = 10000;
                pref.edit().putInt("DELAY_MILLIS", 10000).apply();
                break;
            case R.id.delay15:
                delay_millis = 15000;
                pref.edit().putInt("DELAY_MILLIS", 15000).apply();
                break;
            case R.id.delay20:
                delay_millis = 20000;
                pref.edit().putInt("DELAY_MILLIS", 20000).apply();
                break;
            case R.id.delay30:
                delay_millis = 30000;
                pref.edit().putInt("DELAY_MILLIS", 30000).apply();
                break;
            case R.id.show_tutorial:
                Intent intent = new Intent(getBaseContext(), TutorialActivity.class);
                startActivity(intent);
                return true;
            case R.id.toggle_tutorial:
                pref.edit().putBoolean("SHOW_TUTORIAL", !item.isChecked()).apply();
                item.setChecked(!item.isChecked());
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
        //for delay options only
        item.setChecked(true);
        Toast.makeText(getApplicationContext(), "Delay set to ~" + Integer.toString(delay_millis / 1000) + "s", Toast.LENGTH_SHORT).show();
        return true;
    }

    private Adapter getAutoCompleteAdapter() {
        //TODO: figure out how to deal with it if the user revokes contact permissions
        Log.i("getAutoCompleteAdapter", "");
        //Fill with contacts
        ContentResolver cr = getContentResolver();
        Cursor cursor = cr.query(ContactsContract.Contacts.CONTENT_URI, null, null, null, null);

        ArrayList<String> nameList = new ArrayList<String>();
        ArrayList<Map<String, String>> contactList = new ArrayList<Map<String, String>>();

        if (cursor.getCount() > 0) {
            if (cursor.moveToFirst()) {
                do {
                    if (Integer.parseInt(cursor.getString(cursor.getColumnIndex(ContactsContract.Contacts.HAS_PHONE_NUMBER))) > 0) {
                        String id = cursor.getString(cursor.getColumnIndex(ContactsContract.Contacts._ID));
                        String name = cursor.getString(cursor.getColumnIndex(ContactsContract.Contacts.DISPLAY_NAME));
                        Cursor phones = getContentResolver().query(ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
                                null,
                                ContactsContract.CommonDataKinds.Phone.CONTACT_ID + " = " + id,
                                null, null);
                        while (phones.moveToNext()) {
                            String number = phones.getString(phones.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER));
                            String type = phones.getString(phones.getColumnIndex(ContactsContract.CommonDataKinds.Phone.TYPE));
                            Map<String, String> NamePhoneType = new HashMap<String, String>();

                            NamePhoneType.put("Name", name);
                            NamePhoneType.put("Phone", number);
                            switch (type) {
                                case "0":
                                    NamePhoneType.put("Type", "Work");
                                    break;
                                case "1":
                                    NamePhoneType.put("Type", "Home");
                                    break;
                                case "2":
                                    NamePhoneType.put("Type", "Mobile");
                                    break;
                                default:
                                    NamePhoneType.put("Type", "Other");
                            }

                            contactList.add(NamePhoneType);
                        }
                        phones.close();
                        nameList.add(name);

                        //String phone = cursor.getString(cursor.getColumnIndex(ContactsContract.PhoneLookup.NUMBER));
                    }
                } while (cursor.moveToNext());
            }
        } else Log.i("getAutoCompleteAdapter", "No contacts found");
        cursor.close();

        return new SimpleAdapter(this, contactList, R.layout.contact, new String[]{"Name", "Phone", "Type"}, new int[]{R.id.contactName, R.id.contactPhone, R.id.contactType});

    }

    @Override
    public void onStart() {
        super.onStart();

        // ATTENTION: This was auto-generated to implement the App Indexing API.
        // See https://g.co/AppIndexing/AndroidStudio for more information.
        client.connect();
        Action viewAction = Action.newAction(
                Action.TYPE_VIEW, // TODO: choose an action type.
                "Main Page", // TODO: Define a title for the content shown.
                // TODO: If you have web page content that matches this app activity's content,
                // make sure this auto-generated web page URL is correct.
                // Otherwise, set the URL to null.
                Uri.parse("http://host/path"),
                // TODO: Make sure this auto-generated app URL is correct.
                Uri.parse("android-app://com.tregrad.doctordial/http/host/path")
        );
        AppIndex.AppIndexApi.start(client, viewAction);
    }

    @Override
    public void onStop() {
        super.onStop();

        // ATTENTION: This was auto-generated to implement the App Indexing API.
        // See https://g.co/AppIndexing/AndroidStudio for more information.
        Action viewAction = Action.newAction(
                Action.TYPE_VIEW, // TODO: choose an action type.
                "Main Page", // TODO: Define a title for the content shown.
                // TODO: If you have web page content that matches this app activity's content,
                // make sure this auto-generated web page URL is correct.
                // Otherwise, set the URL to null.
                Uri.parse("http://host/path"),
                // TODO: Make sure this auto-generated app URL is correct.
                Uri.parse("android-app://com.tregrad.doctordial/http/host/path")
        );
        AppIndex.AppIndexApi.end(client, viewAction);
        client.disconnect();
    }
}
