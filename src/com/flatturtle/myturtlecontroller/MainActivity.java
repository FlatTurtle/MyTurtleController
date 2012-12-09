/**
 * Main Activity for the application
 * @author Michiel Vancoillie
 */

package com.flatturtle.myturtlecontroller;

import java.lang.Thread.UncaughtExceptionHandler;
import java.util.ArrayList;

import android.R.color;
import android.app.Activity;
import android.app.AlarmManager;
import android.app.AlertDialog;
import android.app.PendingIntent;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.graphics.Color;
import android.os.Bundle;
import android.text.Editable;
import android.util.Log;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.View;
import android.view.View.OnKeyListener;
import android.view.Window;
import android.view.WindowManager;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.RelativeLayout.LayoutParams;
import android.widget.TextView;

import com.flatturtle.myturtlecontroller.ui.MainButton;

public class MainActivity extends Activity {
	private PendingIntent intent;
	private LinearLayout containerButtons;
	private View viewStart;
	private View viewLocation;
	private View viewSettings;
	private TextView lblMode;
	private RelativeLayout btnBack;
	private RelativeLayout btnGo;
	private RelativeLayout btnSaveSettings;
	private LinearLayout btnSwitchPane;
	private ArrayList<RelativeLayout> listButtons;

	private TextView txtFrom;
	private TextView txtTo;
	private TextView txtPin;
	private TextView txtPass;
	
	private SharedPreferences settings;
    public static final String PREFS_NAME = "MyTurtleController";
    public static final String SETTING_PIN = "PIN";
    public static final String SETTING_PASSWORD = "PASSWORD";
    private OnKeyListener enterKeyListener;

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
		containerButtons = (LinearLayout) findViewById(R.id.containerButtons);
		viewStart = findViewById(R.id.viewStart);
		viewLocation = findViewById(R.id.viewLocation);
		viewLocation.setVisibility(View.INVISIBLE);
		viewSettings = findViewById(R.id.viewSettings);
		viewSettings.setVisibility(View.INVISIBLE);
		lblMode = (TextView) findViewById(R.id.lblMode);
		btnBack = (RelativeLayout) findViewById(R.id.btnBack);
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
		

		// Create buttons
		ArrayList<MainButton> btns = new ArrayList<MainButton>();

		ImageView icon = new ImageView(this);
		icon.setImageResource(R.drawable.icon_walk);
		btns.add(new MainButton(icon, getString(R.string.otp_mode_walk),
				getString(R.string.btn_walk), 1));

		icon = new ImageView(this);
		icon.setImageResource(R.drawable.icon_bike);
		btns.add(new MainButton(icon, getString(R.string.otp_mode_bike),
				getString(R.string.btn_bike), 1));

		icon = new ImageView(this);
		icon.setImageResource(R.drawable.icon_public);
		btns.add(new MainButton(icon, getString(R.string.otp_mode_transit),
				getString(R.string.btn_public_transport), 2));

		icon = new ImageView(this);
		icon.setImageResource(R.drawable.icon_car);
		btns.add(new MainButton(icon, getString(R.string.otp_mode_car),
				getString(R.string.btn_car), 1));

		int count = 0;
		for (MainButton btn : btns) {
			// Make layout for button
			RelativeLayout btnContainer = new RelativeLayout(this);
			if (count != btns.size() - 1)
				btnContainer.setPadding(0, 0, 20, 0);
			RelativeLayout.LayoutParams layoutParams = new RelativeLayout.LayoutParams(
					RelativeLayout.LayoutParams.WRAP_CONTENT,
					RelativeLayout.LayoutParams.MATCH_PARENT);
			btnContainer.setLayoutParams(layoutParams);

			RelativeLayout newButton = new RelativeLayout(this);
			layoutParams = new RelativeLayout.LayoutParams(
					RelativeLayout.LayoutParams.WRAP_CONTENT,
					RelativeLayout.LayoutParams.MATCH_PARENT);
			newButton.setLayoutParams(layoutParams);
			newButton.setTag(btn.tag);

			// Add background
			int backgroundColor = (count % 2 == 0) ? Color
					.parseColor("#2357a5") : Color.parseColor("#0478bd");
			View background = new View(this);
			newButton.addView(background);
			background.setBackgroundColor(backgroundColor);
			layoutParams = new RelativeLayout.LayoutParams(228,
					RelativeLayout.LayoutParams.FILL_PARENT);
			background.setLayoutParams(layoutParams);

			// Make textView
			TextView label = new TextView(this);
			label.setText(btn.title);
			label.setTextSize(40);
			label.setTextColor(Color.parseColor("#ffffff"));
			label.setLines(btn.lines);
			label.setGravity(Gravity.CENTER);
			label.setWidth(200);
			newButton.addView(label);
			newButton.addView(btn.icon);

			// Set textView position
			LayoutParams lp = (LayoutParams) label.getLayoutParams();
			lp.addRule(RelativeLayout.ALIGN_PARENT_BOTTOM);
			lp.addRule(RelativeLayout.CENTER_HORIZONTAL);
			lp.setMargins(0, 0, 0, 20);
			label.setLayoutParams(lp);

			// Set icon position
			lp = (LayoutParams) btn.icon.getLayoutParams();
			lp.addRule(RelativeLayout.CENTER_HORIZONTAL);
			lp.addRule(RelativeLayout.CENTER_VERTICAL);
			btn.icon.setLayoutParams(lp);

			// Big button listeners
			newButton.setOnClickListener(new View.OnClickListener() {
				@Override
				public void onClick(View v) {
					String btnTag = v.getTag().toString();
					Log.i("Button", btnTag);
					modeSelected(btnTag);
				}
			});

			count++;
			btnContainer.addView(newButton);
			containerButtons.addView(btnContainer);
		}

		// Back button listener
		btnBack.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				backToTheStart();
			}
		});
		
		// Switch pane listener
		btnSwitchPane.setOnClickListener(new View.OnClickListener() {	
			@Override
			public void onClick(View v) {
				Log.i("Panes", "Switch");
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
	 * Go back to the start screen
	 */
	public void backToTheStart() {
		Log.i("View", "Back to the start view");
		viewLocation.setVisibility(View.INVISIBLE);
		viewStart.setVisibility(View.VISIBLE);
	}

	/**
	 * One of the big buttons was clicked
	 * 
	 * @param mode
	 */
	public void modeSelected(String mode) {
		Log.i("View", "To the form view");
		if (mode.equals("WALK")) {
			lblMode.setText(getString(R.string.btn_walk));
		} else if (mode.equals("BICYCLE")) {
			lblMode.setText(getString(R.string.btn_bike));
		} else if (mode.equals("TRANSIT")) {
			lblMode.setText(getString(R.string.btn_public_transport));
		} else if (mode.equals("CAR")) {
			lblMode.setText(getString(R.string.btn_car));
		}

		viewStart.setVisibility(View.INVISIBLE);
		viewLocation.setVisibility(View.VISIBLE);
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
		viewStart.setVisibility(View.INVISIBLE);
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
		viewStart.setVisibility(View.VISIBLE);
		viewSettings.setVisibility(View.INVISIBLE);	
	}
}