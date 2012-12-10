/**
 * Main Activity for the application
 * @author Michiel Vancoillie
 */

package com.flatturtle.myturtlecontroller;

import java.lang.Thread.UncaughtExceptionHandler;

import android.app.Activity;
import android.app.AlarmManager;
import android.app.AlertDialog;
import android.app.PendingIntent;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.Editable;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.view.View.OnKeyListener;
import android.view.Window;
import android.view.WindowManager;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;

public class MainActivity extends Activity {
	private PendingIntent intent;
	private View viewLocation;
	private View viewSettings;
	private RelativeLayout btnGo;
	private RelativeLayout btnSaveSettings;
	private LinearLayout btnSwitchPane;
	
	private TextView txtFrom;
	private TextView txtTo;
	private TextView txtPin;
	private TextView txtPass;
	
	private SharedPreferences settings;
    public static final String PREFS_NAME = "MyTurtleController";
    public static final String SETTING_PIN = "PIN";
    public static final String SETTING_PASSWORD = "PASSWORD";
    private OnKeyListener enterKeyListener;
    
    private APIClient api;
   
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		// Hide the title bar
		requestWindowFeature(Window.FEATURE_NO_TITLE);
		// Go fullscreen
		this.getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
				WindowManager.LayoutParams.FLAG_FULLSCREEN);

		// Select layout
		setContentView(R.layout.activity_main);

		// Restart application on crash
		intent = PendingIntent.getActivity(this.getApplication()
				.getBaseContext(), 0, new Intent(getIntent()), getIntent()
				.getFlags());
		Thread.setDefaultUncaughtExceptionHandler(new UncaughtExceptionHandler() {
			public void uncaughtException(Thread thread, Throwable ex) {
				AlarmManager mgr = (AlarmManager) getSystemService(Context.ALARM_SERVICE);
				mgr.set(AlarmManager.RTC, System.currentTimeMillis() + 2000,
						intent);
				System.exit(2);
			}
		});
		
		// Link necessary views
		viewLocation = findViewById(R.id.viewLocation);
		viewLocation.setVisibility(View.VISIBLE);
		viewSettings = findViewById(R.id.viewSettings);
		viewSettings.setVisibility(View.INVISIBLE);
		btnGo = (RelativeLayout) findViewById(R.id.btnGo);
		btnSwitchPane = (LinearLayout) findViewById(R.id.btnSwitchPane);
		btnSaveSettings = (RelativeLayout) findViewById(R.id.btnSaveSettings);
		txtFrom = (TextView) findViewById(R.id.txtFrom);
		txtTo = (TextView) findViewById(R.id.txtTo);
		txtPin = (TextView) findViewById(R.id.txtPin);
		txtPass = (TextView) findViewById(R.id.txtPass);
		
		settings = this.getSharedPreferences(PREFS_NAME, 0);
		txtPin.setText(settings.getString(SETTING_PIN, ""));
		txtPass.setText(settings.getString(SETTING_PASSWORD, "112233"));
		
		// Create APIClient with API URL from strings	
		api = new APIClient(getString(R.string.api));
		this.authenticate();

		
		// Switch pane listener
		btnSwitchPane.setOnClickListener(new View.OnClickListener() {	
			@Override
			public void onClick(View v) {
				Log.i("Panes", "Switch");
				api.rotatePane();
			}
		});
		
		// Save settings listener
		btnSaveSettings.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				saveSettings();
			}
		});
		
		// Close keyboard on ENTER key for some fields
		getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN);
		final InputMethodManager inputManager = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
		enterKeyListener = new OnKeyListener() {
			@Override
			public boolean onKey(View v, int keyCode, KeyEvent event) {
				if((event.getAction() == KeyEvent.ACTION_DOWN) &&
			            (keyCode == KeyEvent.KEYCODE_ENTER)){
					Log.i("Keyboard","Hide");
					inputManager.hideSoftInputFromWindow(v.getWindowToken(), InputMethodManager.HIDE_NOT_ALWAYS);
					return true;
				}
				return false;
			}
		};
		txtFrom.setOnKeyListener(enterKeyListener);
		txtTo.setOnKeyListener(enterKeyListener);
		txtPin.setOnKeyListener(enterKeyListener);
		txtPass.setOnKeyListener(enterKeyListener);
	}
	
	/**
	 * Authenticate with API
	 */
	public void authenticate(){
		api.pin = txtPin.getText().toString();
		if(!api.authenticate()){
			AlertDialog.Builder alert = new AlertDialog.Builder(this);
			alert.setTitle("Authentication failed!");
			alert.setMessage("No token receive, is the PIN-code correct?");
			alert.show();
		}
	}

	/**
	 * Prevent leaving the application without password
	 */
	@Override
	public boolean onKeyDown(int keyCode, KeyEvent event) {
		switch (keyCode) {
		case KeyEvent.KEYCODE_POWER:
		case KeyEvent.KEYCODE_HOME:
		case KeyEvent.KEYCODE_BACK:
			// Create an alert to prompt password
			AlertDialog.Builder alert = new AlertDialog.Builder(this);
			alert.setTitle("A password is needed to quit the application");
			alert.setMessage("Password:");

			final EditText input = new EditText(this);
			alert.setView(input);

			// Check password
			final DialogInterface.OnClickListener submitListener = new DialogInterface.OnClickListener() {
				public void onClick(DialogInterface dialog,
						int whichButton) {
					Editable value = input.getText();
					if(value.toString().equals(txtPass.getText().toString())){
						shutDown();
					}
				}
			};
			alert.setPositiveButton("Ok", submitListener);
			
			// Cancel exit
			alert.setNegativeButton("Cancel",
					new DialogInterface.OnClickListener() {
						public void onClick(DialogInterface dialog,
								int whichButton) {
							// Canceled.
						}
					});
			final AlertDialog alertDialog = alert.create(); 
			
			// ENTER key to submit
			input.setOnKeyListener(new OnKeyListener() {
				@Override
				public boolean onKey(View v, int keyCode, KeyEvent event) {
					if((event.getAction() == KeyEvent.ACTION_DOWN) &&
				            (keyCode == KeyEvent.KEYCODE_ENTER)){
						submitListener.onClick(null, 0);
						alertDialog.dismiss();
						return true;
					}
					return false;
				}
			});

			// Show the alert
			alertDialog.show();
			return true;
		default:
			return false;
		}
	}
	
	/**
	 * User confirmed shutdown with password, also change settings or really quit?
	 */
	public void shutDown(){
		AlertDialog.Builder alert = new AlertDialog.Builder(this);
		alert.setTitle("Quit, or change settings?");
		
		// Really quit
		alert.setPositiveButton("Quit",
				new DialogInterface.OnClickListener() {
					public void onClick(DialogInterface dialog,
							int whichButton) {
						Log.i("App","Quit");
						System.exit(0);
					}
				});

		// Show settings page
		alert.setNegativeButton("Settings",
				new DialogInterface.OnClickListener() {
					public void onClick(DialogInterface dialog,
							int whichButton) {
							showSettings();
					}
				});

		// Show the alert
		alert.show();
	}
	
	/**
	 * Show settings view
	 */
	public void showSettings(){
		Log.i("View","Show settings");
		viewLocation.setVisibility(View.INVISIBLE);
		viewSettings.setVisibility(View.VISIBLE);
	}
	
	/**
	 * Save settings
	 */
	public void saveSettings(){
		SharedPreferences.Editor editor = settings.edit();
		editor.putString(SETTING_PIN, txtPin.getText().toString());
		editor.putString(SETTING_PASSWORD, txtPass.getText().toString());
		editor.commit();
		
		this.authenticate();
		
		viewSettings.setVisibility(View.INVISIBLE);	
		viewLocation.setVisibility(View.VISIBLE);
	}
}