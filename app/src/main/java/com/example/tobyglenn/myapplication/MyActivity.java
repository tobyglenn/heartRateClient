package com.example.tobyglenn.myapplication;

import android.app.Activity;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.TextView;
import android.widget.Toast;

import com.microsoft.band.BandClient;
import com.microsoft.band.BandClientManager;
import com.microsoft.band.BandDeviceInfo;
import com.microsoft.band.BandException;
import com.microsoft.band.BandIOException;
import com.microsoft.band.ConnectionResult;
import com.microsoft.band.sensors.BandHeartRateEvent;
import com.microsoft.band.sensors.BandHeartRateEventListener;
import com.microsoft.band.sensors.BandSensorException;
import com.microsoft.band.sensors.BandSensorManager;
import com.squareup.okhttp.MediaType;
import com.squareup.okhttp.OkHttpClient;
import com.squareup.okhttp.Request;
import com.squareup.okhttp.Response;

import java.io.IOException;

import sdksample.Model;


public class MyActivity extends Activity {

    public static final MediaType JSON
            = MediaType.parse("application/json; charset=utf-8");

    private TextView textData;

    private BandDeviceInfo[] mPairedBands;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_my);
        textData = (TextView) findViewById(R.id.textData);
        mPairedBands = BandClientManager.getInstance().getPairedBands();
        if (mPairedBands.length > 0){
            BandClient client = BandClientManager.getInstance().create(this, mPairedBands[0]);
            Model.getInstance().setClient(client);
            // Connect must be called on a background thread.
            new ConnectTask().execute(Model.getInstance().getClient());
        }else {
            Toast.makeText(getApplicationContext(), "You don't have a band bro come back when you get one", Toast.LENGTH_LONG).show();
        }

    }

    //
    // The connect call must be done on a background thread because it
    // involves a callback that must be handled on the UI thread.
    //
    private class ConnectTask extends AsyncTask<BandClient, Void, ConnectionResult> {
        @Override
        protected ConnectionResult doInBackground(BandClient... clientParams) {
            try {
                return clientParams[0].connect().await();
            } catch (InterruptedException e) {
                return ConnectionResult.TIMEOUT;
            } catch (BandException e) {
                return ConnectionResult.INTERNAL_ERROR;
            }
        }

        protected void onPostExecute(ConnectionResult result) {
            if (result != ConnectionResult.OK) {
                Toast.makeText(getApplicationContext(), "Connect Connection failed: result=" + result.toString(), Toast.LENGTH_LONG).show();
            }else {
                BandSensorManager sensorMgr = Model.getInstance().getClient().getSensorManager();
                textData.setText("connected sending data to the cloud");
                try {
                    sensorMgr.registerHeartRateEventListener(mHeartRateEventListener);
                } catch (BandSensorException e) {
                    e.printStackTrace();
                } catch (BandIOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    OkHttpClient client = new OkHttpClient();

    private BandHeartRateEventListener mHeartRateEventListener = new BandHeartRateEventListener() {
        @Override
        public void onBandHeartRateChanged(final BandHeartRateEvent event) {
            Request request = new Request.Builder()
                    .url("http://10.165.14.202:3000/heartrate/"+ event.getHeartRate())
                    .build();
            try {
                Response response = client.newCall(request).execute();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    };


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.my, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();
        if (id == R.id.action_settings) {
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
}
