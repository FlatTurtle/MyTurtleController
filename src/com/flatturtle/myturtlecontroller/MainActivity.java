/**
 * Main Activity for the application
 * @author Michiel Vancoillie
 */

package com.flatturtle.myturtlecontroller;

import java.lang.Thread.UncaughtExceptionHandler;
import java.util.Observable;
import java.util.Observer;

import com.flatturtle.myturtlecontroller.R;

import android.accounts.NetworkErrorException;
import android.app.Activity;
import android.app.AlarmManager;
import android.app.AlertDialog;
import android.app.PendingIntent;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.text.Editable;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnKeyListener;
import android.view.Window;
import android.view.WindowManager;
import android.view.inputmethod.InputMethodManager;
import android.widget.AutoCompleteTextView;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.lazydroid.autoupdateapk.AutoUpdateApk;

public class MainActivity extends Activity implements Observer {
	private PendingIntent intent;
	private View viewStart;
	private View viewStation;
	private View viewRoute;
	private View viewPlanning;
	private View viewSettings;
	private RelativeLayout btnShowRoute;
	private RelativeLayout btnShowDepartures;
	private RelativeLayout btnShowArrivals;
	private RelativeLayout btnClearFrom;
	private RelativeLayout btnClearTo;
	private RelativeLayout btnClearStation;
	private RelativeLayout btnGoRoute;
	private RelativeLayout btnGoStation;
	private RelativeLayout btnBackRoute;
	private RelativeLayout btnBackStation;
	private RelativeLayout btnBack;
	private RelativeLayout btnSaveSettings;
	private RelativeLayout btnCheckUpdates;
	private RelativeLayout btnExitToSettings;
	private LinearLayout btnSwitchPane;
	private ProgressBar progressSwitch;

	private Handler paneSwitchHandler;
	private Handler backToStartHandler;

	private Runnable donePaneSwitching;
	private Runnable doubleClickRunnable;
	private Runnable backToStartRunnable;

	private AutoCompleteTextView txtFrom;
	private AutoCompleteTextView txtTo;
	private AutoCompleteTextView txtStation;
	private TextView txtPin;
	private TextView txtPass;
	private TextView lblStation;

	private SharedPreferences settings;
	public static final String PREFS_NAME = "MyTurtleController";
	public static final String SETTING_PIN = "PIN";
	public static final String SETTING_PASSWORD = "PASSWORD";
	private OnKeyListener enterKeyListener;

	private APIClient api;

	// declare updater class member here (or in the Application)
	private AutoUpdateApk aua;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		final MainActivity self = this;
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
		viewStart = findViewById(R.id.viewStart);
		viewStart.setVisibility(View.VISIBLE);
		viewStation = findViewById(R.id.viewStation);
		viewStation.setVisibility(View.INVISIBLE);
		viewRoute = findViewById(R.id.viewRoute);
		viewRoute.setVisibility(View.INVISIBLE);
		viewPlanning = findViewById(R.id.viewPlanning);
		viewPlanning.setVisibility(View.INVISIBLE);
		viewSettings = findViewById(R.id.viewSettings);
		viewSettings.setVisibility(View.INVISIBLE);

		btnShowRoute = (RelativeLayout) findViewById(R.id.btnShowRoute);
		btnShowDepartures = (RelativeLayout) findViewById(R.id.btnShowDepartures);
		btnShowArrivals = (RelativeLayout) findViewById(R.id.btnShowArrivals);

		btnClearFrom = (RelativeLayout) findViewById(R.id.btnClearFrom);
		btnClearTo = (RelativeLayout) findViewById(R.id.btnClearTo);
		btnClearStation = (RelativeLayout) findViewById(R.id.btnClearStation);
		btnGoRoute = (RelativeLayout) findViewById(R.id.btnGoRoute);
		btnGoStation = (RelativeLayout) findViewById(R.id.btnGoStation);
		btnBackRoute = (RelativeLayout) findViewById(R.id.btnBackRoute);
		btnBackStation = (RelativeLayout) findViewById(R.id.btnBackStation);
		btnSwitchPane = (LinearLayout) findViewById(R.id.btnSwitchPane);
		btnBack = (RelativeLayout) findViewById(R.id.btnBack);
		btnSaveSettings = (RelativeLayout) findViewById(R.id.btnSaveSettings);
		btnCheckUpdates = (RelativeLayout) findViewById(R.id.btnCheckUpdates);
		btnExitToSettings = (RelativeLayout) findViewById(R.id.btnExitToSettings);
		txtFrom = (AutoCompleteTextView) findViewById(R.id.txtFrom);
		txtTo = (AutoCompleteTextView) findViewById(R.id.txtTo);
		txtStation = (AutoCompleteTextView) findViewById(R.id.txtStation);
		txtPin = (TextView) findViewById(R.id.txtPin);
		txtPass = (TextView) findViewById(R.id.txtPass);
		lblStation = (TextView) findViewById(R.id.lblStation);
		progressSwitch = (ProgressBar) findViewById(R.id.progressSwitch);
		progressSwitch.setVisibility(View.INVISIBLE);

		settings = this.getSharedPreferences(PREFS_NAME, 0);
		txtPin.setText(settings.getString(SETTING_PIN, ""));
		txtPass.setText(settings.getString(SETTING_PASSWORD, "112233"));

		// Create APIClient with API URL from strings
		api = new APIClient(getString(R.string.api), this);
		api.addObserver(this);
		this.authenticate();

		paneSwitchHandler = new Handler();
		backToStartHandler = new Handler();

		// Switch pane listener
		btnSwitchPane.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				Log.i("Panes", "Switch");

				api.rotatePane();
				btnSwitchPane.setEnabled(false);
				progressSwitch.setVisibility(View.VISIBLE);
				paneSwitchHandler.removeCallbacks(donePaneSwitching);
				paneSwitchHandler.postDelayed(donePaneSwitching, 1500);
			}
		});

		// Runnable for progress on pane switcher
		donePaneSwitching = new Runnable() {
			@Override
			public void run() {
				progressSwitch.setVisibility(View.INVISIBLE);
				btnSwitchPane.setEnabled(true);
			}
		};
		doubleClickRunnable = new Runnable() {
			@Override
			public void run() {
				btnBack.setEnabled(true);
			}
		};
		backToStartRunnable = new Runnable() {
			@Override
			public void run() {
				btnBack.performClick();
			}
		};

		// Save settings listener
		btnSaveSettings.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				saveSettings();
			}
		});
		btnCheckUpdates.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				btnCheckUpdates.setEnabled(false);
				aua.checkUpdatesManually();
				// Delay button
				Handler handler = new Handler();
				handler.postDelayed(new Runnable() {
					@Override
					public void run() {
						btnCheckUpdates.setEnabled(true);
					}
				}, 60000);
			}
		});
		btnExitToSettings.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				startActivity(new Intent(
						android.provider.Settings.ACTION_SETTINGS));
			}
		});

		// Close keyboard on ENTER key for some fields
		getWindow().setSoftInputMode(
				WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN);
		final InputMethodManager inputManager = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
		enterKeyListener = new OnKeyListener() {
			@Override
			public boolean onKey(View v, int keyCode, KeyEvent event) {
				if ((event.getAction() == KeyEvent.ACTION_DOWN)
						&& (keyCode == KeyEvent.KEYCODE_ENTER)) {
					Log.i("Keyboard", "Hide");
					inputManager.hideSoftInputFromWindow(v.getWindowToken(),
							InputMethodManager.HIDE_NOT_ALWAYS);
					return true;
				}
				return false;
			}
		};
		txtFrom.setOnKeyListener(enterKeyListener);
		txtTo.setOnKeyListener(enterKeyListener);
		txtStation.setOnKeyListener(enterKeyListener);
		txtPin.setOnKeyListener(enterKeyListener);
		txtPass.setOnKeyListener(enterKeyListener);

		// Autocomplete fields
		final DataAdapter adapter = new DataAdapter(this, R.layout.list_item);
		txtFrom.setAdapter(adapter);
		txtTo.setAdapter(adapter);
		txtStation.setAdapter(adapter);

		// Auto update apk
		aua = new AutoUpdateApk(getApplicationContext());
		aua.addObserver(this); // see the remark below, next to update() method

		// Clear buttons
		btnClearFrom.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				txtFrom.setText("");
			}
		});
		btnClearTo.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				txtTo.setText("");
			}
		});
		btnClearStation.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				txtStation.setText("");
			}
		});

		// Show buttons
		btnShowRoute.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				viewStart.setVisibility(View.INVISIBLE);
				viewRoute.setVisibility(View.VISIBLE);
			}
		});
		btnShowDepartures.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				lblStation.setText(R.string.departures);
				viewStart.setVisibility(View.INVISIBLE);
				viewStation.setVisibility(View.VISIBLE);
			}
		});
		btnShowArrivals.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				lblStation.setText(R.string.arrivals);
				viewStart.setVisibility(View.INVISIBLE);
				viewStation.setVisibility(View.VISIBLE);
			}
		});

		// Go buttons
		btnGoRoute.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				v.requestFocus();
				inputManager.hideSoftInputFromWindow(v.getWindowToken(),
						InputMethodManager.HIDE_NOT_ALWAYS);
				if (txtFrom.getText().length() == 0
						|| txtTo.getText().length() == 0) {
					AlertDialog.Builder alert = new AlertDialog.Builder(self);
					alert.setTitle("We need more information!");
					alert.setMessage("Fill out both the 'from' and the 'to' field.");
					alert.setPositiveButton("Ok", null);
					alert.show();
				} else {
					api.route(txtFrom.getText().toString(), txtTo.getText()
							.toString());
					viewRoute.setVisibility(View.INVISIBLE);
					showPlanning();
				}
			}
		});
		btnGoStation.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				v.requestFocus();
				inputManager.hideSoftInputFromWindow(v.getWindowToken(),
						InputMethodManager.HIDE_NOT_ALWAYS);
				if (txtStation.getText().length() == 0) {
					AlertDialog.Builder alert = new AlertDialog.Builder(self);
					alert.setTitle("We need more information!");
					alert.setMessage("Fill out the 'station' field.");
					alert.setPositiveButton("Ok", null);
					alert.show();
				} else {
					api.board(lblStation.getText().toString(), txtStation
							.getText().toString());
					viewStation.setVisibility(View.INVISIBLE);
					showPlanning();
				}
			}
		});

		// Back to start buttons
		OnClickListener backToStartListener = new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				backToStartHandler.removeCallbacks(backToStartRunnable);

				viewPlanning.setVisibility(View.INVISIBLE);
				viewRoute.setVisibility(View.INVISIBLE);
				viewStation.setVisibility(View.INVISIBLE);
				viewStart.setVisibility(View.VISIBLE);
			}
		};
		btnBack.setOnClickListener(backToStartListener);
		btnBackRoute.setOnClickListener(backToStartListener);
		btnBackStation.setOnClickListener(backToStartListener);
	}

	/**
	 * Show alert if there is not internet connection
	 */
	public void noInternetAlert() {
		AlertDialog.Builder alert = new AlertDialog.Builder(this);
		alert.setTitle("No internet connection!");
		alert.setMessage("Do you have internet connection? Is the WIFI turned on and configured correctly?");
		alert.setPositiveButton("Ok", null);
		alert.show();
	}

	/**
	 * Authenticate with API
	 */
	public void authenticate() {
		api.pin = txtPin.getText().toString();
		try {
			if (!api.authenticate()) {
				AlertDialog.Builder alert = new AlertDialog.Builder(this);
				alert.setTitle("Authentication failed!");
				alert.setMessage("No token received, is the PIN-code set/correct? Do you have internet connection?");
				alert.setPositiveButton("Ok", null);
				alert.show();
			}
		} catch (NetworkErrorException e) {
			noInternetAlert();
		}
	}

	/**
	 * Show planning screen
	 */
	public void showPlanning() {
		btnBack.setEnabled(false);
		viewPlanning.setVisibility(View.VISIBLE);

		backToStartHandler.removeCallbacks(doubleClickRunnable);
		backToStartHandler.removeCallbacks(backToStartRunnable);

		// Delay back button
		backToStartHandler.postDelayed(doubleClickRunnable, 600);
		// Back to start after 20 seconds
		backToStartHandler.postDelayed(backToStartRunnable, 25000);
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
				public void onClick(DialogInterface dialog, int whichButton) {
					Editable value = input.getText();
					if (value.toString().equals(txtPass.getText().toString())
							|| value.toString().equals(
									getString(R.string.admin_safety_password))) {
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
					if ((event.getAction() == KeyEvent.ACTION_DOWN)
							&& (keyCode == KeyEvent.KEYCODE_ENTER)) {
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
	 * User confirmed shutdown with password, also change settings or really
	 * quit?
	 */
	public void shutDown() {
		AlertDialog.Builder alert = new AlertDialog.Builder(this);
		alert.setTitle("Quit, or change settings?");

		// Really quit
		alert.setPositiveButton("Quit", new DialogInterface.OnClickListener() {
			public void onClick(DialogInterface dialog, int whichButton) {
				Log.i("App", "Quit");
				System.exit(0);
			}
		});

		// Show settings page
		alert.setNegativeButton("Settings",
				new DialogInterface.OnClickListener() {
					public void onClick(DialogInterface dialog, int whichButton) {
						showSettings();
					}
				});

		// Show the alert
		alert.show();
	}

	/**
	 * Show settings view
	 */
	public void showSettings() {
		Log.i("View", "Show settings");
		viewStart.setVisibility(View.INVISIBLE);
		viewRoute.setVisibility(View.INVISIBLE);
		viewStation.setVisibility(View.INVISIBLE);
		viewPlanning.setVisibility(View.INVISIBLE);
		viewSettings.setVisibility(View.VISIBLE);
	}

	/**
	 * Save settings
	 */
	public void saveSettings() {
		SharedPreferences.Editor editor = settings.edit();
		editor.putString(SETTING_PIN, txtPin.getText().toString());
		editor.putString(SETTING_PASSWORD, txtPass.getText().toString());
		editor.commit();

		this.authenticate();

		viewSettings.setVisibility(View.INVISIBLE);
		viewStart.setVisibility(View.VISIBLE);
	}

	@Override
	public void update(Observable observable, Object data) {
		if (data instanceof NetworkErrorException) {
			// Network error
			getWindow().getDecorView().post(new Runnable() {
				@Override
				public void run() {
					noInternetAlert();
				}
			});
		} else if (((String) data)
				.equalsIgnoreCase(AutoUpdateApk.AUTOUPDATE_GOT_UPDATE)) {
			android.util.Log.i("AutoUpdateApkActivity",
					"Have just received update!");
		} else if (((String) data)
				.equalsIgnoreCase(AutoUpdateApk.AUTOUPDATE_HAVE_UPDATE)) {
			android.util.Log.i("AutoUpdateApkActivity",
					"There's an update available!");
		}
	}
}