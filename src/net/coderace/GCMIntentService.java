package net.coderace;

import org.json.JSONException;
import org.json.JSONObject;

import com.google.android.gcm.GCMBaseIntentService;
import android.content.Context;
import android.content.Intent;
import android.telephony.TelephonyManager;
import android.util.Log;

public class GCMIntentService extends GCMBaseIntentService {
	
	private static final String TAG = "GCMIntentService";
	
	public GCMIntentService() {
	}

	@Override
	protected void onError(Context arg0, String arg1) {
		Log.i(TAG, "GCM error: " + arg1);
		
		// TODO Auto-generated method stub
		/*
		 * Problem registering or de-registering
		 */
		
	}

	@Override
	protected void onMessage(Context context, Intent intent) {
		Log.i(TAG, "GCM message received");
		
		Intent update = new Intent("net.coderace.GCM");
		for (String s: intent.getExtras().keySet()) {
			update.putExtra(s, intent.getExtras().getString(s));
		}

		Log.d(TAG, "Sending update to CodeRace");
		try {
			sendBroadcast(update);
		}
		catch(Exception e) {
			Log.e(TAG,"Intent error: " + e.getLocalizedMessage());
		}
	}

	@Override
	protected void onRegistered(Context arg0, String arg1) {
		gcmUpdate(arg0, arg1);
	}

	@Override
	protected void onUnregistered(Context arg0, String arg1) {
		gcmUpdate(arg0, "");
	}

	private void gcmUpdate(Context arg0, String arg1) {
//		Log.i(TAG, "GCM update: " + arg1);
		String device = "ERROR";
		JSONObject json = null;
		
        TelephonyManager tm = (TelephonyManager) getSystemService(TELEPHONY_SERVICE);
        if (tm != null) {
			device =  tm.getDeviceId();
        }

        Log.i(TAG, "Starting GCM update activity");
       	String url = getString(R.string.server_url) + "gcm_update.php" +
			"?device=" + device +
			"&gcm=" + arg1 ;
		
		GameJSON gameJSON = new GameJSON(url);
		int status = 1000;
		try {
			json = new JSONObject(gameJSON.body);
			status = json.getInt("status");
		}
		catch(JSONException e) {
			Log.e(TAG,"JSON parse error: " + gameJSON);
		}

		if (status == 0) {
			Log.i(TAG, "GCM update sent to server");
		}
		else {
			Log.i(TAG, "Failed to send GCM update to server");
		}
	}
}
