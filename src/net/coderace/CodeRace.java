package net.coderace;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.List;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.annotation.TargetApi;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.drawable.Drawable;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.app.FragmentActivity;
import android.telephony.TelephonyManager;
import android.text.format.DateUtils;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.view.animation.LinearInterpolator;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ToggleButton;

import com.google.android.gcm.GCMRegistrar;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesUtil;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.GoogleMap.OnMarkerClickListener;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;

public class CodeRace extends FragmentActivity {
	
	// CONSTANTS #############################
	
	private static final String	TAG			= "CodeRace";		// Used for logCat messages
	private static final int	LOGIN		= 0;				// Used to identify activity results
	private static final String	SENDER_ID	= "565338773147";	// GCM sender id
	private static final int    POLL_SHORT	= 20;				// Poll quickly when tracking positions
	private static final int	POLL_LONG	= 60;				// Poll slowly ordinarily
	private static final int	NONE		= 0;				// OK dialog action
	private static final int	DIE			= 1;				// OK dialog action
	private static final int	TICKPERIOD	= 1000;				// Tick period in milliseconds
	private static final int	CLEAR		= 0;				// State of poll
	private static final int	NOW			= 1;				// State of poll
	private static final int	POLLING		= 2;				// State of poll
	private static final int	PRE			= 0;				// Flag to hideGame()
	private static final int	POST		= 1;				// Flag to hideGame()

	// Required packages
	private static final String GooglePlayStorePackageNameOld = "com.google.market";
	private static final String GooglePlayStorePackageNameNew = "com.android.vending";
	private static final String GoogleMaps = "com.google.android.apps.maps";

	// FLAGS #################################
	
	private Boolean googlePlayStoreInstalled = false;
	private Boolean googleMapsInstalled = false;
   	private Boolean showPlayers = false;
   	//private Boolean canChat = false;
    	
	// Timers ################################
   	
	int timeToGo = 0;
    protected Handler tickHandler;
    protected Boolean isPollActive = false;
    protected Boolean isDestroyed = false;
    int pollPeriod = POLL_LONG;
    int pollState  = CLEAR;
    private Runnable tick = new Runnable() {
        public void run()
        {
            runTickTask();
            if (!isDestroyed)
            	tickHandler.postDelayed( this, TICKPERIOD );
        }
    };

    // Location management ##############################
    
    LocationManager mlocManager;
   	LocationListener mlocListener;
   	double latitude=90;				// Default to North Pole
   	double longitude=0;
   	
   	// Graphics #########################################
   	
   	Bitmap teamDot;
   	Bitmap otherDot;
   	Bitmap teamPlayer;
   	Bitmap otherPlayer;
   	
	// Device screen characteristics ####################
   	
	DisplayMetrics metrics = null;
	float imageScale = 1;
	
	// Async task #######################################
	
	AsyncTask<String, Void, JSONObject> asyncFetchTask = null;
	
	// UI references ####################################
	
	View			mLogoutStatusView	= null;
	View			mContent			= null;
	View			mMessages			= null;
	ImageButton		mStatus				= null;
	ToggleButton	mPlayers			= null;
	ImageButton		mChat				= null;
	ImageButton		mHelp				= null;
	TextView		mCountdown			= null;
	TextView		bigCountdown		= null;
	TextView		mMessageHistory		= null;
	EditText		mMessage			= null;
	RelativeLayout	preGame				= null;
	SupportMapFragment		mFragment	= null;
	String			countdownText		= "00:00";
	int				portraitPadding		= 0;		// Actually set in the InputField style - different in landscape
	
	// Intent communication variables ###################
	
	ReceiveMessages updateReceiver = null;
	Boolean updateReceiverIsRegistered = false;
	
	// STATE VARIABLES ##################################
	
	private Game		g = null;
	private GoogleMap	mMap = null;
	private String		device = "";
	private int			me = -3;		// Used as index to find own details; -1 and -2 are free and total in server response
	private int			myTeam = -1;
	private String		markerId = "";	// Populated from the google.maps.marker.snippet
	private int			apiLevel = 17;
	
	// METHODS ##########################################

    @TargetApi(Build.VERSION_CODES.HONEYCOMB) 
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Find the current API level
		apiLevel = Build.VERSION.SDK_INT;
        
        // Hide app action bar (style in layout did not seem to work)
		if (apiLevel > 10)
			getActionBar().hide();
        
        setContentView(R.layout.activity_game); 
        addButtonListeners(this);
        
        // Find UI objects
        mContent		= (RelativeLayout) findViewById(R.id.main_content);
        mMessages		= (LinearLayout) findViewById(R.id.main_message);
        mMessageHistory = (TextView) findViewById(R.id.messageHistory);
        mMessage		= (EditText) findViewById(R.id.message);
        mCountdown		= (TextView) findViewById(R.id.countdown);
        bigCountdown	= (TextView) findViewById(R.id.big_countdown);
        preGame			= (RelativeLayout) findViewById(R.id.pre_game);
		mFragment		= (SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map);
		mStatus			= (ImageButton) findViewById(R.id.statusButton);
		mPlayers		= (ToggleButton) findViewById(R.id.playerButton);
		mChat			= (ImageButton) findViewById(R.id.chatButton);
		mHelp			= (ImageButton) findViewById(R.id.helpButton);
		portraitPadding	= findViewById(R.id.answer).getPaddingLeft();	// Restored after orientation change
/*
        AnimatorSet set = (AnimatorSet) AnimatorInflater.loadAnimator(this,
        	    R.anim.blink);
        	set.setTarget(mChat);xx
        	set.start();
*/
		
        // Prepare to receive updates via GCM
        updateReceiver = new ReceiveMessages();

        // Find device id
        TelephonyManager tm = (TelephonyManager) getSystemService(TELEPHONY_SERVICE);
        if (tm != null) {
    		device = tm.getDeviceId();
        }
        
		// Get device screen density
		metrics = new DisplayMetrics();
		getWindowManager().getDefaultDisplay().getMetrics(metrics);
		imageScale = metrics.scaledDensity;
        
        // Prepare bitmaps from XML shapes
		teamDot = fromDrawable(R.drawable.blue_dot, 12);
		otherDot = fromDrawable(R.drawable.red_dot, 12);
        
        // Check for Google Play Store
        PackageManager packageManager = getApplication().getPackageManager();
        List<PackageInfo> packages = packageManager.getInstalledPackages(PackageManager.GET_UNINSTALLED_PACKAGES);
        for (PackageInfo packageInfo : packages) {
            if (packageInfo.packageName.equals(GooglePlayStorePackageNameOld) ||
                packageInfo.packageName.equals(GooglePlayStorePackageNameNew)) {
                googlePlayStoreInstalled = true;
            }
            else if (packageInfo.packageName.equals(GoogleMaps))
                googleMapsInstalled = true;
        }
    	
        if (googlePlayStoreInstalled) {
        	// Check for Google Play services
        	int resultCode = GooglePlayServicesUtil.isGooglePlayServicesAvailable(this);

        	if(resultCode != ConnectionResult.SUCCESS)
        		Log.e(TAG, "Google Play Services not found: " + resultCode);
        	else {
        		// Check for Google Maps application
        		if (! googleMapsInstalled)
            	   okDialog("The Google Maps application is required. Please install and then restart CodeRace", DIE);
        	}
        }
        else
        	okDialog("Google Play Store is required. Please install and then restart CodeRace", DIE);
        
        // Start polling
        tickHandler = new Handler();
        tickHandler.postDelayed( tick, TICKPERIOD );
        
        // Start login activity
		this.startActivityForResult(new Intent(this, LoginActivity.class), LOGIN);
    }
   
    // Handle activity result 
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
    	Log.i(TAG,requestCode + " result: " + resultCode);
        if (resultCode != RESULT_CANCELED ) {
        	switch(requestCode) {
        	case LOGIN:		doLogin(data.getDataString());
        					break;
			default:		Log.e(TAG,"Unrecognised activity result: " + requestCode);
        	}
        }
    }
    
    public class ReceiveMessages extends BroadcastReceiver 
    {
    @Override
    	public void onReceive(Context context, Intent intent) 
    	{    
       		String message = "";
       		String sender = "";
       		
    		String action = intent.getAction();
       		Log.d(TAG,"Update received: " + action);
       		
       		message     = intent.getStringExtra("message");
       		sender      = intent.getStringExtra("sender");
       		
   			mMessageHistory.append("\n" + sender + ": \n" + message + " \n");
   			
   			pulse(mChat);
    	}
    }

    @Override
    protected void onResume() {
    	super.onResume();

    	// Hide logout canvas
		mLogoutStatusView = findViewById(R.id.logout_status);
		mLogoutStatusView.setVisibility(View.GONE);
		
		// Activate polling and GPS
		isPollActive = true;
		startLocationListener();
	}
    
    @Override
    protected void onPause() {
    	super.onPause();
		isPollActive = false;
    	stopLocationListener();
    }
    
    @Override
    protected void onDestroy() {
    	super.onPause();
    	GCMRegistrar.unregister(this);
    	
    	if (updateReceiverIsRegistered) {
    	    unregisterReceiver(updateReceiver);
    	    updateReceiverIsRegistered = false;
    	}
    	isDestroyed = true;
    }
    
    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        
        // Checks the orientation of the screen
        if (newConfig.orientation == Configuration.ORIENTATION_LANDSCAPE) {
        	findViewById(R.id.messageHistory).setVisibility(View.GONE);
        	
        	// Adjust padding on InputFields
        	findViewById(R.id.answer).setPadding(4,4,4,4);
        	findViewById(R.id.message).setPadding(4,4,4,4);
        	
        } else if (newConfig.orientation == Configuration.ORIENTATION_PORTRAIT){
        	findViewById(R.id.messageHistory).setVisibility(View.VISIBLE);
        	
        	// Adjust padding on InputFields
        	findViewById(R.id.answer).setPadding(portraitPadding,portraitPadding,portraitPadding,portraitPadding);
        	findViewById(R.id.message).setPadding(portraitPadding,portraitPadding,portraitPadding,portraitPadding);
        	
        }
    }
    
    // Handle Async task results #########################
    
    private void doLogin(String data) {
    	initialiseGame(data);

    	if (googlePlayStoreInstalled) {
    		if (g != null) {
    			// Register for GCM
    			GCMRegistrar.checkDevice(this);
    			GCMRegistrar.checkManifest(this);
   				GCMRegistrar.register(this, SENDER_ID);
    		}
    	}
    	
    	if (!updateReceiverIsRegistered) {
    	    registerReceiver(updateReceiver, new IntentFilter("net.coderace.GCM"));
    	    updateReceiverIsRegistered = true;
    	}

        // Check that GPS is enabled
        mlocManager = (LocationManager) getSystemService(LOCATION_SERVICE);
        boolean enabledGPS = mlocManager
                .isProviderEnabled(LocationManager.GPS_PROVIDER);
        if (!enabledGPS) {
        	okDialog("GPS is not enabled. Your location cannot be reported to the game. Please enable GPS in your settings.", NONE);
        }
    }
    
    private void doPopulate(JSONObject json) {
    	Bitmap drawable;
    	String title="";
    	String snippet="";
    	
        try {
            if (json.getString("message").length() > 0)
            	Toast.makeText(this, json.getString("message"), Toast.LENGTH_LONG).show();
            
            if (json.getInt("status") == 0) {
            	mMap.clear();
            	// Add data to the game object
            	g.populate(json);
            
            	// Add locations to the map for those locations that are visible
            	for (GameLocation loc : g.locations) {
            		if(loc.visible) {
            			if (loc.team == 0) {
            				drawable = BitmapFactory.decodeResource(getResources(),R.drawable.free);
            				title = loc.clue;
            				snippet = Integer.toString(loc.id);
            			}
            			else if (loc.team == myTeam) {
            				drawable = BitmapFactory.decodeResource(getResources(),R.drawable.blue_cross);
            			}
            			else {
            				drawable = BitmapFactory.decodeResource(getResources(),R.drawable.red_cross);
            			}
            				
           				loc.marker = 
           						mMap.addMarker(new MarkerOptions()
           						.position(new LatLng(loc.latitude, loc.longitude))
           						.icon(BitmapDescriptorFactory.fromBitmap(drawable))
            					.anchor((float)0.5,(float)0.5)
           						.title(title)
           						.snippet(snippet));
            		}
            	}
            }
        }

        catch(JSONException e) {
        	Log.e(TAG, "Could not process JSON: " + json.toString());
        }
        hidePopupMessage();

        if (g.getState() == Game.STARTING)
        	g.start();
        else if (g.getState() == Game.ENDING)
        	endGame();
    }

    // Game methods #####################################
    
    private void initialiseGame(String s) {
        JSONObject j = null;
        // String	message
        // Int		playerId
        // Int		gameId
        // String	gameName
        // double	longitude
        // double	latitude
        // Int		zoom
        // Long		serverTime
        // Long		gameStart
        // Long		gameEnd
        // int		teamId
        
        try {
            j = new JSONObject(s);
            me = j.getInt("playerId");
            Log.d(TAG,"I am player id " + Integer.toString(me));
            myTeam = j.getInt("teamId");
            Long gameStart = j.getLong("gameStart");
            Long gameEnd = j.getLong("gameEnd");
            Long serverTime = j.getLong("serverTime");
            int state = Game.PENDING;
            if(gameEnd < serverTime) {
            	state = Game.ENDING;
            	timeToGo = 0;
            	showGame();
            }
            else if(gameStart > serverTime) {
            	timeToGo = (int)(gameStart - serverTime);
            	hideGame(PRE);
            }
            else {
            	state = Game.STARTING;
            	timeToGo = (int)(gameEnd - serverTime);
            	showGame();
            }

        	g = new Game(this, 									// ctx
        		j.getInt("gameId"),								// id
        		j.getString("gameName"),						// name
        		j.getDouble("latitude"),						// startLat
        		j.getDouble("longitude"),						// startLong
        		j.getInt("zoom"),								// startZoom
        		gameStart,										// startTime
        		gameEnd,										// endTime
        		serverTime - System.currentTimeMillis()/1000,	// timeOffset
        		state);											// state 
        	
        	// Do a null check to confirm that we have not already instantiated the map.
            if (mMap == null) {
                mMap = mFragment.getMap();
                mMap.setOnMarkerClickListener(new OnMarkerClickListener() {

                    @Override
                    public boolean onMarkerClick(final Marker marker) {
                    	Log.d(TAG, "Clicked: " + marker.getTitle());
                    	
                    	if (g.getState() != Game.STARTED)
                    		return true;

                    	String title = marker.getTitle();
                    	TextView txtTitle = ((TextView) findViewById(R.id.clue));
               	 
                    	if (title != null) {
                    		txtTitle.setText(title);
                    		((EditText) findViewById(R.id.answer)).setText("");
                    		markerId = marker.getSnippet();
                    		View mDialog = findViewById(R.id.claim_dialog);
                    		mDialog.setVisibility(View.VISIBLE);
                    	}
                        return true;
                    }
                });

                // Check if we were successful in obtaining the map.
                if (mMap != null) {
                    // The Map is verified. It is now safe to manipulate the map.
                	mMap.setMapType(GoogleMap.MAP_TYPE_NORMAL);
                	LatLng mapCentre = new LatLng(g.startLat,g.startLong);
                	mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(mapCentre, g.startZoom));
                	mMap.setMyLocationEnabled(true);
                	
                   	String url = getString(R.string.server_url) + "getGameState.php" +
       				"?playerId=" + me +
       				"&gameId=" + g.id +
       				"&device=" + device;

       				Log.d(TAG,url);
       				asyncFetchTask = new AsyncFetchTask(AsyncFetchTask.POPULATE).execute(url);
                }
            }        	
        }
		catch (JSONException e) {
			Log.e(TAG, "JSON Error: " + s);
		}
    }
    
    private String claim(int playerId, String locationIdString, String claimCode) {
		String message = "Claim error";
		
        Log.i(TAG, "Starting claim activity");
		try {
			String url =	getString(R.string.server_url) + "claim.php?gameId=" +
							g.id +
							"&playerId=" +
							me +
							"&locationId=" +
							locationIdString +
							"&code=" + 
							URLEncoder.encode(claimCode.trim(), "utf-8");
			Log.d(TAG,url);
			asyncFetchTask = new AsyncFetchTask(AsyncFetchTask.CLAIM).execute(url);

		} catch (UnsupportedEncodingException e1) {
			message = "Unsupported coding error";
			e1.printStackTrace();
		}
        return message;
    }
	
	private void doClaim(JSONObject json) {
        try {
            if (json.getString("message").length() > 0)
            	Toast.makeText(this, json.getString("message"), Toast.LENGTH_LONG).show();
            
            if(json.getInt("status") == 0) {
            	GameLocation cLoc = g.getLocation(json.getInt("locationId"));
            	cLoc.setClaimed(myTeam,
            		mMap.addMarker(new MarkerOptions()
            			.position(new LatLng(cLoc.latitude, cLoc.longitude))
            			.icon(BitmapDescriptorFactory.fromResource(R.drawable.blue_cross))
    					.anchor((float)0.5,(float)0.5)));
            	g.getTeam(myTeam).incrementScore();
            	g.getTeam(Team.FREE).decrementScore();
            	pollState = NOW;	// Picked up in runTickTask
            }
        }

        catch(JSONException e) {
        	Log.e(TAG, "Could not process JSON: " + json.toString());
        }

	}

    private void sendMessage() {
    	TextView m = (TextView) findViewById(R.id.message);
        Log.i(TAG, "Sending message");
        if (m.getText().length() > 0) {
        	try {
        		String url =	getString(R.string.server_url) + "message.php?gameId=" +
        						g.id +
        						"&playerId=" +
        						me +
        						"&message=" +
        						URLEncoder.encode( m.getText().toString(), "utf-8");
        		Log.d(TAG,url);
        		asyncFetchTask = new AsyncFetchTask(AsyncFetchTask.MESSAGE).execute(url);

        	} catch (UnsupportedEncodingException e1) {
        		e1.printStackTrace();
        	}
        }
    }
    
    private void doMessage() {
    	String m = ((TextView)findViewById(R.id.message)).getText().toString();
    	EditText h = (EditText)findViewById(R.id.messageHistory);
    	((EditText)findViewById(R.id.message)).setText("");
    	h.append("\n" + g.getPlayer(me).getUsername());
    	h.append(":\n" + m + "\n");
    }
    
    private void logout(int playerId) {
    	Log.i(TAG, "Starting logout activity");
    	
		try {
			String url =	getString(R.string.server_url) + "logout.php?playerId=" + me;
			Log.d(TAG,url);
			asyncFetchTask = new AsyncFetchTask(AsyncFetchTask.LOGOUT).execute(url);

		} catch (Exception e1) {
			e1.printStackTrace();
		}
    }
	
	private void doLogout() {
		Log.i(TAG, "Logging out");
		// Stop timers
		// Remove team scores
		// Reset variables
		// Stop GPS
		isPollActive = false;
    	stopLocationListener();
    	me = -3;
    	g = null;
    	mMap = null;
    	this.startActivityForResult(new Intent(this, LoginActivity.class), LOGIN);
	}

	// Graphics
	
	private Bitmap fromDrawable(int drawable, int size) {
		int dim = (int) (size * imageScale);
		Bitmap b = Bitmap.createBitmap(dim,dim,Bitmap.Config.ARGB_8888);
		Canvas c = new Canvas(b);
		Drawable d = getResources().getDrawable(drawable);
		d.setBounds(0,0,dim,dim);
		d.draw(c);
		return(b);
	}

	@SuppressWarnings("deprecation")
	@TargetApi(Build.VERSION_CODES.JELLY_BEAN) 
	private void pulse(View v) {
	    final Animation animation = new AlphaAnimation(1, 0); // Change alpha from fully visible to invisible
	    animation.setDuration(500); // duration - half a second
	    animation.setInterpolator(new LinearInterpolator()); // do not alter animation rate
	    animation.setRepeatCount(Animation.INFINITE); // Repeat animation infinitely
	    animation.setRepeatMode(Animation.REVERSE); // Reverse animation at the end so the button will fade back in
	    v.startAnimation(animation);
	    if (v.getId() != R.id.playerButton) {
	    	if (apiLevel >= Build.VERSION_CODES.JELLY_BEAN)
	    		v.setBackground(getResources().getDrawable(R.drawable.control_checked_background));
	    	else
	    		v.setBackgroundDrawable(getResources().getDrawable(R.drawable.control_checked_background));
	    }
	}

	@SuppressWarnings("deprecation")
	@TargetApi(Build.VERSION_CODES.JELLY_BEAN) 
	private void noPulse(View v) {
	    v.clearAnimation();
	    if (v.getId() != R.id.playerButton) {
	    	if (apiLevel >= Build.VERSION_CODES.JELLY_BEAN)
	    		v.setBackground(getResources().getDrawable(R.drawable.control_background));
	    	else
	    		v.setBackgroundDrawable(getResources().getDrawable(R.drawable.control_background));
	    }
	}

    // Controls

    protected void okDialog(String message, final int action) {
		AlertDialog.Builder alert = new AlertDialog.Builder(this);

		alert.setTitle("");
		alert.setMessage(message);
		alert.setNeutralButton("OK", new DialogInterface.OnClickListener() {
			
			@Override
			public void onClick(DialogInterface dialog, int which) {
				if (action == DIE)
					finish();
			}
		});
		alert.show();
    }
    
    protected void showPopupMessage(String message) {
		View mPopupView = findViewById(R.id.popup_message_canvas);
		mPopupView.setVisibility(View.VISIBLE);
		TextView mPopupMessageView = (TextView) findViewById(R.id.popup_message);
		mPopupMessageView.setText(message);
    }
    
    protected void hidePopupMessage() {
		View mPopupView = findViewById(R.id.popup_message_canvas);
		mPopupView.setVisibility(View.GONE);
    }
    
    public void addButtonListeners(final CodeRace c) {
    	 
		ImageButton imageButton = (ImageButton) findViewById(R.id.logoutButton);
		imageButton.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View arg0) {
				View mLogoutStatusView = findViewById(R.id.logout_status);
				mLogoutStatusView.setVisibility(View.VISIBLE);
				TextView mLogoutStatusMessageView = (TextView) findViewById(R.id.logout_status_message);
				mLogoutStatusMessageView.setText(R.string.logout_progress);
				c.logout(c.me);
			}
		});
   	 
		imageButton = (ImageButton) findViewById(R.id.statusButton);
		imageButton.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View arg0) {
				if (g.teams == null) {
					Toast.makeText(c, "Game not initialised - try again!", Toast.LENGTH_SHORT).show();
				}
				else if (g.getState() != Game.STARTED)
					return;
				else {
					int[]    teamIds    = new int[g.teams.length];
					String[] teamNames  = new String[g.teams.length];
					int[]    teamScores = new int[g.teams.length];
				
					for(int i=0;i<g.teams.length;i++) {
						teamIds[i] = g.teams[i].id;
						teamNames[i] = g.teams[i].name;
						teamScores[i] = g.teams[i].score;
					}

					Intent intent = new Intent(getBaseContext(), StatusActivity.class);
					intent.putExtra("t", myTeam);
					intent.putExtra("ids", teamIds);
					intent.putExtra("names", teamNames);
					intent.putExtra("scores", teamScores);
				
					startActivity(intent);
				}
			}
		});
	   	 
			imageButton = (ImageButton) findViewById(R.id.chatButton);
			imageButton.setOnClickListener(new OnClickListener() {
				@Override
				public void onClick(View arg0) {
					if (g == null) {
						Toast.makeText(c, "Game not initialised - try again!", Toast.LENGTH_SHORT).show();
					}
					else {
				        showChat();
					}
				}
			});

			imageButton = (ImageButton) findViewById(R.id.helpButton);
			imageButton.setOnClickListener(new OnClickListener() {
				@Override
				public void onClick(View arg0) {
					Intent intent = new Intent(getBaseContext(), HelpActivity.class);
					startActivity(intent);
				}
			});

			findViewById(R.id.exit_chat_button).setOnClickListener(
					new View.OnClickListener() {
						@Override
						public void onClick(View view) {
							hideChat();
						}
					});

			findViewById(R.id.send_button).setOnClickListener(
					new View.OnClickListener() {
						@Override
						public void onClick(View view) {
							sendMessage();
						}
					});
			
    		findViewById(R.id.cancel_button).setOnClickListener(new View.OnClickListener() {
    			public void onClick(View v) {
    				Log.d(TAG,"Hiding dialog");
    		    	InputMethodManager imm = (InputMethodManager) getSystemService(Activity.INPUT_METHOD_SERVICE);
    		       	imm.hideSoftInputFromWindow(mContent.getWindowToken(), 0);
    				findViewById(R.id.claim_dialog).setVisibility(View.GONE);
    			}
    		});

    		findViewById(R.id.claim_button).setOnClickListener(new View.OnClickListener() {
    			public void onClick(View v) {
    				Log.d(TAG,"Sending claim");
    				EditText ansText = ((EditText) findViewById(R.id.answer));
    		    	InputMethodManager imm = (InputMethodManager) getSystemService(Activity.INPUT_METHOD_SERVICE);
    		       	imm.hideSoftInputFromWindow(mContent.getWindowToken(), 0);
    				claim(me, markerId, ansText.getText().toString());
    				findViewById(R.id.claim_dialog).setVisibility(View.GONE);
   				}
    		});

			
			((EditText) findViewById(R.id.answer))
				.setOnEditorActionListener(new TextView.OnEditorActionListener() {
					@Override
					public boolean onEditorAction(TextView textView, int id, KeyEvent keyEvent) {
						if (id == EditorInfo.IME_NULL) {
		    				EditText ansText = ((EditText) findViewById(R.id.answer));
		    		    	InputMethodManager imm = (InputMethodManager) getSystemService(Activity.INPUT_METHOD_SERVICE);
		    		       	imm.hideSoftInputFromWindow(mContent.getWindowToken(), 0);
		    				claim(me, markerId, ansText.getText().toString());
		    				findViewById(R.id.claim_dialog).setVisibility(View.GONE);
							return true;
						}
						return false;
					}
				});


    }
    
    public void togglePlayers(View v) {
    	Bitmap icon;
    	
		if (showPlayers) {
			showPlayers = false;
			noPulse(mPlayers);
			pollPeriod = POLL_LONG;
			for(Player p: g.players)
				p.hide();
		}
		else {
			if (g.players == null) {
				Toast.makeText(this, "Game not initialised - try again!", Toast.LENGTH_SHORT).show();
			}
			else if (g.getState() != Game.STARTED)
				return;
			else {
				showPlayers = true;
				pulse(mPlayers);
				pollPeriod = POLL_SHORT;
				for(Player p: g.players) {
					if(p.team == myTeam)
						icon = teamDot;
					else
						icon = otherDot;
					p.show(mMap.addMarker(new MarkerOptions()
					.position(new LatLng(p.getLatitude(), p.getLongitude()))
					.icon(BitmapDescriptorFactory.fromBitmap(icon))
					.anchor((float)0.5,(float)0.5)));
				}
			}
		}
    }
    
	// Timed operations
	
    protected void runTickTask()
    {
    	if( g != null && 
    		g.getState() != Game.ENDING && 
    		g.getState() != Game.OVER && 
    		timeToGo > 0) {
    		timeToGo--;
    		if(timeToGo == 0) {
    			if(g.getState() == Game.PENDING) {
    				showGame();
    				g.start();
    				timeToGo = g.duration();
    			}
    			else if(g.getState() == Game.STARTED) {
    				endGame();
    			}
    		}
    		else if (timeToGo == 6000)		// One minute to go!
    			pulse(mCountdown);
    		
    		mCountdown.setText(DateUtils.formatElapsedTime(timeToGo));
    		if (g.getState() == Game.PENDING)
        		bigCountdown.setText(DateUtils.formatElapsedTime(timeToGo));
        	if ((pollState == NOW || timeToGo % pollPeriod == 0)
        			&& isPollActive
        			&& g.getState() == Game.STARTED
        			&& pollState != POLLING) {
        		pollState = POLLING;
        		try {
        			String url =	getString(R.string.server_url) + "poll.php?playerId=" + me;
        			url += "&gameId=" + g.id;
        			url += "&latitude=" + latitude;
        			url += "&longitude=" + longitude;
        			url += "&device=" + device;
        			url += "&players=" + (showPlayers ? 1 : 0);
        			Log.d(TAG,url);
       				asyncFetchTask = new AsyncFetchTask(AsyncFetchTask.POLL).execute(url);
        		} catch (Exception e1) {
        			e1.printStackTrace();
        		}
            }
    	}
    }	
    
    private void endGame() {
		isPollActive = false;
    	stopLocationListener();
    	hideGame(POST);

		String finalString = g.end();
		LayoutInflater inflater = LayoutInflater.from(this);
        View v = inflater.inflate(R.layout.game_over, null, false);
        FrameLayout fl = (FrameLayout) findViewById(R.id.top);
        fl.addView(v, new RelativeLayout.LayoutParams(fl.getLayoutParams().width, fl.getLayoutParams().height));
        Log.d(TAG,finalString);
        ((TextView) findViewById(R.id.winner)).setText(finalString);
        
		ImageButton imageButton = (ImageButton) findViewById(R.id.logoutButton2);
		imageButton.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View arg0) {
				View mLogoutStatusView = findViewById(R.id.logout_status);
				mLogoutStatusView.setVisibility(View.VISIBLE);
				View mPostGame = findViewById(R.id.post_game);
				mPostGame.setVisibility(View.GONE);
				TextView mLogoutStatusMessageView = (TextView) findViewById(R.id.logout_status_message);
				mLogoutStatusMessageView.setText(R.string.logout_progress);
				logout(me);
			}
		});

    }
    
    public void showGame() {
		preGame.setVisibility(View.GONE);
		mCountdown.setVisibility(View.VISIBLE);
		mStatus.setVisibility(View.VISIBLE);
		mPlayers.setVisibility(View.VISIBLE);
		mChat.setVisibility(View.VISIBLE);
		mFragment.getView().setEnabled(true);
    }
    
    public void hideGame(int f) {
    	if (f == PRE)
    		preGame.setVisibility(View.VISIBLE);
    	if (f == POST)
    		mHelp.setVisibility(View.GONE);
		mCountdown.setVisibility(View.GONE);
		mStatus.setVisibility(View.GONE);
		mPlayers.setVisibility(View.GONE);
		mChat.setVisibility(View.GONE);
		mFragment.getView().setEnabled(false);
    }
    
    public void showChat() {
    	noPulse(mChat);
    	mContent.setVisibility(View.INVISIBLE);
    	mMessages.setVisibility(View.VISIBLE);
    }
    
    public void hideChat() {
    	InputMethodManager imm = (InputMethodManager) getSystemService(Activity.INPUT_METHOD_SERVICE);
       	imm.hideSoftInputFromWindow(mMessages.getWindowToken(), 0);
       	mMessage.setText("");
    	mContent.setVisibility(View.VISIBLE);
    	mMessages.setVisibility(View.GONE);
    }
    
	public class MyLocationListener implements LocationListener {

		public void onLocationChanged(Location location) {
			latitude = location.getLatitude();
			longitude = location.getLongitude();
/*			
			String Text = "My current location is: " +
			"Latitude = " + location.getLatitude() +
			"Longitude = " + location.getLongitude();
			Toast.makeText( getApplicationContext(), Text, Toast.LENGTH_SHORT).show();
//			Toast.makeText( getApplicationContext(),"Sending GPS location",Toast.LENGTH_SHORT).show();
*/		}

		public void onProviderDisabled(String provider) {
			// TODO Auto-generated method stub
			
		}

		public void onProviderEnabled(String provider) {
			// TODO Auto-generated method stub
			
		}

		public void onStatusChanged(String provider, int status, Bundle extras) {
			// TODO Auto-generated method stub
			
		}
	}

	public void startLocationListener() {
	    mlocManager = (LocationManager)getSystemService(LOCATION_SERVICE);
	   	mlocListener = new MyLocationListener();
	   	// Attach listener - mintime is 60s, no mindist
	   	mlocManager.requestLocationUpdates( LocationManager.GPS_PROVIDER, 60000, 0, mlocListener);
	}
	
	public void stopLocationListener() {
		if (mlocManager != null) {
			mlocManager.removeUpdates(mlocListener);
			mlocManager=null;
		}
	}
	
	private class AsyncFetchTask extends AsyncTask<String, Void, JSONObject> {
		private static final String TAG = "AsyncFetchTask";
		public static final int POPULATE	= 1;
		public static final int CLAIM		= 2;
		public static final int POLL		= 3;
		public static final int	LOGOUT		= 4;
		public static final int MESSAGE		= 5;
		int fetchTask = 0;
		
		public AsyncFetchTask(int f) {
			super();
			fetchTask = f;
		}
	
		protected JSONObject doInBackground(String... arg) {
			GameJSON gameJSON = new GameJSON(arg[0]);
			JSONObject json = null;
			try {
				json = new JSONObject(gameJSON.body);
			}
			catch(JSONException e) {
				Log.e(TAG,"JSON parse error: " + gameJSON.body);
			}
    		return json;
		}

		protected void onPostExecute(final JSONObject json) {
			switch(fetchTask) {
			case POPULATE:	doPopulate(json);
							break;
			case CLAIM:		doClaim(json);
							break;
			case POLL:		updateGame(json);
							break;
        	case LOGOUT:	doLogout();
							break;
        	case MESSAGE:	doMessage();
        					break;
        	default:		Log.e(TAG,"Unrecognised fetch result: " + fetchTask);}			
		} 
	}
	
	// Updates game data with 1) newly claimed locations 2) newly visible locations
	// 3) player positions
	private void updateGame(JSONObject json) {
		GameLocation loc = null;
		Bitmap icon;
		int locationId = 0;
		int teamId = 0;
		int count;
		int newLocations=0;
		
		// Avoid timing errors
		if (g == null) {
			pollState = CLEAR;
			return;
		}
		
		try {
			// Get free array
			JSONArray jFree = null;
			if (json.has("free")) {
				jFree = json.getJSONArray("free");
				newLocations = jFree.length();
			}
			
			// get visible count
			count = json.getInt("visible");
			if (count != g.visibleLocations() + newLocations) {
   				Toast.makeText(this, "Refreshing game data", Toast.LENGTH_LONG).show();

   				String url = getString(R.string.server_url) + 
   					"getGameState.php" +
        			"?playerId=" + me +
        			"&gameId=" + g.id +
       				"&device=" + device;

   				Log.d(TAG,url);
   				asyncFetchTask = new AsyncFetchTask(AsyncFetchTask.POPULATE).execute(url);
			}
			else {
				if(json.has("claimed")) {
					// Get claimed array
					JSONArray jLocations = json.getJSONArray("claimed");
					for (int i=0; i<jLocations.length(); i++) {
						locationId  = jLocations.getJSONObject(i).getInt("id");
						teamId		= jLocations.getJSONObject(i).getInt("team");

						// update game and map
						loc = g.getLocation(locationId);
						loc.setVisible(true);
						if(teamId == myTeam)
							icon = BitmapFactory.decodeResource(getResources(),R.drawable.blue_cross);
						else
							icon = BitmapFactory.decodeResource(getResources(),R.drawable.red_cross);

						loc.setClaimed(teamId,
								mMap.addMarker(new MarkerOptions()
    								.position(new LatLng(loc.latitude, loc.longitude))
    								.icon(BitmapDescriptorFactory.fromBitmap(icon))
    								.anchor((float)0.5,(float)0.5)
    								.title("")
    								.snippet("")));
			
						// update scores
						g.getTeam(teamId).incrementScore();
						g.getTeam(Team.FREE).decrementScore();
					}
				}
				if (jFree != null) {
					// Update map with free locations
					for (int i=0; i<jFree.length(); i++) {
						locationId  = jFree.getInt(i);
						loc = g.getLocation(locationId);
						loc.setVisible(true);
						loc.marker = 
								mMap.addMarker(new MarkerOptions()
									.position(new LatLng(loc.latitude, loc.longitude))
									.icon(BitmapDescriptorFactory.fromResource(R.drawable.free))
									.anchor((float)0.5,(float)0.5)
									.title(loc.clue)
									.snippet(Integer.toString(loc.id)));
					}
				}
				// Get player locations
				if (showPlayers && json.has("players")) {
					JSONArray jPlayers = json.getJSONArray("players");
					JSONObject jPlayer;
					Player p = null;
					double jLat = 90;
					double jLong = 0;
				
					if (jPlayers != null) {
						Log.d(TAG,"jPlayers: " + jPlayers.length());
						for (int i=0; i<jPlayers.length();i++) {
							jPlayer = jPlayers.getJSONObject(i);
							p = g.getPlayer(jPlayer.getInt("id"));
							jLat = jPlayer.getDouble("la");
							jLong = jPlayer.getDouble("lo");
						
							Log.d(TAG,"Adding marker at: " + jLat + ", " + jLong);
							if (jLat != p.getLatitude() || jLong != p.getLongitude()) {
								if(p.team == myTeam)
									icon = teamDot;
								else
									icon = otherDot;
								p.show(jLat, jLong, mMap.addMarker(new MarkerOptions()
                					.position(new LatLng(jLat, jLong))
                					.icon(BitmapDescriptorFactory.fromBitmap(icon))
                					.anchor((float)0.5,(float)0.5)));
							}
						}
					}
				}
			}
		} catch (JSONException e) {
			e.printStackTrace();
			Log.e(TAG, "Could not process JSON: " + json.toString());
		}
		pollState = CLEAR;
	}
/*	
	private void restoreSavedInstanceState(Bundle s) {
        if (s == null) {
        	return;
        }
        me				= s.getInt("myId");
        myTeam			= s.getInt("myTeam");

            if (g == null) {
            	g = new Game(this, 					// ctx
            			s.getInt("gameId"),			// id
                		s.getString("gameName"),	// name
                		s.getDouble("latitude"),	// startLat
                		s.getDouble("longitude"),	// startLong
                		s.getInt("zoom"),			// startZoom
                		s.getLong("startTime"),		// startTime
                		s.getLong("endTime"),		// endTime
                		s.getLong("timeOffset"),	// timeOffset
                		s.getInt("state"));			// state 
                	
            	String url = getString(R.string.server_url) + "getGameState.php" +
   				"?playerId=" + me +
   				"&gameId=" + g.id +
   				"&device=" + device;

   				Log.d(TAG,url);
   				asyncFetchTask = new AsyncFetchTask(AsyncFetchTask.POPULATE).execute(url);

   				private GoogleMap	mMap = null;
            	
            
            Long serverTime = j.getLong("serverTime");
            int state = Game.PENDING;
            if(gameEnd < serverTime) {
            	state = Game.OVER;
            	timeToGo = 0;
            	showGame();
            }
            else if(gameStart > serverTime) {
            	timeToGo = (int)(gameStart - serverTime);
            	hideGame();
            }
            else {
            	state = Game.STARTING;
            	timeToGo = (int)(gameEnd - serverTime);
            	showGame();
            }
	}
*/
}
