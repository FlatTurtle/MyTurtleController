/**
 * Main Activity for the application
 * @author Michiel Vancoillie
 */

package com.flatturtle.myturtlecontroller;

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
import android.os.StrictMode;
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
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.TextView;

import org.apache.http.NameValuePair;
import org.apache.http.message.BasicNameValuePair;
import org.json.JSONException;
import org.json.JSONObject;

import java.lang.Thread.UncaughtExceptionHandler;
import java.util.ArrayList;
import java.util.Observable;
import java.util.Observer;

public class MainActivity extends Activity implements Observer {
    private PendingIntent intent;
    private View viewStart;
    private View viewNMBS;
    private View viewStation;
    private View viewRoute;
    private View viewPlanning;
    private View viewSettings;
    private RelativeLayout containerNavItems;
    private Button btnHome;
    private TextView lblNavWhere;
    protected RelativeLayout btnNMBS;
    protected RelativeLayout btnDeLijn;
    protected RelativeLayout btnMIVB;
    protected TextView btnToggleKeyboardRoute;
    protected TextView btnToggleKeyboardBoard;
    protected TextView btnToggleKeyboardSettings;
    protected RelativeLayout btnShowRoute;
    protected RelativeLayout btnShowDepartures;
    protected RelativeLayout btnShowArrivals;
    protected RelativeLayout btnClearFrom;
    protected RelativeLayout btnClearTo;
    protected RelativeLayout btnClearStation;
    protected RelativeLayout btnGoRoute;
    protected RelativeLayout btnGoStation;
    protected RelativeLayout btnBackRoute;
    protected RelativeLayout btnBackStation;
    protected RelativeLayout btnBack;
    protected RelativeLayout btnSaveSettings;
    protected RelativeLayout btnExitToSettings;
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
    public static double latitude = 51.2;
    public static double longitude = 4.3;

    protected OnKeyListener enterKeyListener;

    private APIClient api;
    private Boolean bootFase = true;

    protected DataAdapter AUTOCOMPLETE_NMBS;
    protected DataAdapter AUTOCOMPLETE_DELIJN;
    protected DataAdapter AUTOCOMPLETE_MIVB;

    private String route_type = "NMBS";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        final MainActivity self = this;

        // Hide the title bar
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        // Fullscreen (> 4)
        this.getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
        this.getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_HIDE_NAVIGATION);

        // Select layout
        setContentView(R.layout.activity_main);

        // Thread policy
        StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder().permitAll().build();
        StrictMode.setThreadPolicy(policy);

        // Restart application on crash
        intent = PendingIntent.getActivity(this.getApplication()
                .getBaseContext(), 0, new Intent(getIntent()), getIntent().getFlags());
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
        viewNMBS = findViewById(R.id.viewNMBS);
        viewStation = findViewById(R.id.viewStation);
        viewRoute = findViewById(R.id.viewRoute);
        viewPlanning = findViewById(R.id.viewPlanning);
        viewSettings = findViewById(R.id.viewSettings);
        containerNavItems = (RelativeLayout) findViewById(R.id.containerNavItems);
        btnHome = (Button) findViewById(R.id.btnHome);
        lblNavWhere = (TextView) findViewById(R.id.lblNavWhere);

        btnNMBS = (RelativeLayout) findViewById(R.id.btnNMBS);
        btnDeLijn = (RelativeLayout) findViewById(R.id.btnDeLijn);
        btnMIVB = (RelativeLayout) findViewById(R.id.btnMIVB);

        btnToggleKeyboardRoute = (TextView) findViewById(R.id.btnToggleKeyboardRoute);
        btnToggleKeyboardBoard = (TextView) findViewById(R.id.btnToggleKeyboardBoard);
        btnToggleKeyboardSettings = (TextView) findViewById(R.id.btnToggleKeyboardSettings);

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

        this.startScreen();

        // Create APIClient with API URL from strings
        api = new APIClient(getString(R.string.api), this);
        api.addObserver(this);
        this.authenticate();


        paneSwitchHandler = new Handler();
        backToStartHandler = new Handler();


        // Soft keyboard toggle
        View.OnClickListener keyboardToggle = new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                InputMethodManager inputMgr = (InputMethodManager)getSystemService(Context.INPUT_METHOD_SERVICE);
                inputMgr.toggleSoftInput(0, 0);
            }
        };
        btnToggleKeyboardRoute.setOnClickListener(keyboardToggle);
        btnToggleKeyboardBoard.setOnClickListener(keyboardToggle);
        btnToggleKeyboardSettings.setOnClickListener(keyboardToggle);


        // Start screen switching
        btnNMBS.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                hideViews();
                lblNavWhere.setText("NMBS");
                viewNMBS.setVisibility(View.VISIBLE);
                txtStation.setAdapter(AUTOCOMPLETE_NMBS);
            }
        });
        btnDeLijn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                hideViews();
                lblNavWhere.setText("De Lijn");
                self.route_type = "DeLijn";
                viewStation.setVisibility(View.VISIBLE);
                txtStation.setAdapter(AUTOCOMPLETE_DELIJN);
            }
        });
        btnMIVB.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                hideViews();
                lblNavWhere.setText("MIVB");
                self.route_type = "MIVB";
                viewStation.setVisibility(View.VISIBLE);
                txtStation.setAdapter(AUTOCOMPLETE_MIVB);
            }
        });

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
        btnExitToSettings.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(
                        android.provider.Settings.ACTION_SETTINGS));
            }
        });

        // Close keyboard on ENTER key for some fields
        this.getWindow().setSoftInputMode(
                WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN
        );
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

        // Home button
        btnHome.setOnClickListener(new Button.OnClickListener() {
            @Override
            public void onClick(View v) {
                startScreen();
            }
        });

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
                hideViews();
                lblNavWhere.setText("NMBS/Route");
                viewRoute.setVisibility(View.VISIBLE);
            }
        });
        btnShowDepartures.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                lblStation.setText(R.string.departures);
                hideViews();
                lblNavWhere.setText("NMBS/Departures");
                viewStation.setVisibility(View.VISIBLE);
            }
        });
        btnShowArrivals.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                lblStation.setText(R.string.arrivals);
                hideViews();
                lblNavWhere.setText("NMBS/Arrivals");
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
                    api.route(txtFrom.getText().toString(), txtTo.getText().toString());
                    hideViews();
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
                    api.board(lblStation.getText().toString(), txtStation.getText().toString(), self.route_type);
                    hideViews();
                    showPlanning();
                }
            }
        });

        // Back to start buttons
        OnClickListener backToStartListener = new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                backToStartHandler.removeCallbacks(backToStartRunnable);
                startScreen();
            }
        };
        btnBack.setOnClickListener(backToStartListener);
        btnBackRoute.setOnClickListener(backToStartListener);
        btnBackStation.setOnClickListener(backToStartListener);
    }

    /**
     * Show first view and hide the others
     */
    protected void startScreen(){
        this.hideViews();

        // Reset fields
        this.route_type = "NMBS";
        txtStation.setText("");

        // Navbar settings
        containerNavItems.setVisibility(View.INVISIBLE);
        btnHome.setVisibility(Button.INVISIBLE);
        btnHome.setEnabled(false);

        viewStart.setVisibility(View.VISIBLE);
    }

    /**
     * Hide all views
     */
    protected void hideViews(){
        containerNavItems.setVisibility(View.VISIBLE);
        btnHome.setVisibility(Button.VISIBLE);
        btnHome.setEnabled(true);

        viewStart.setVisibility(View.INVISIBLE);
        viewNMBS.setVisibility(View.INVISIBLE);
        viewStation.setVisibility(View.INVISIBLE);
        viewRoute.setVisibility(View.INVISIBLE);
        viewPlanning.setVisibility(View.INVISIBLE);
        viewSettings.setVisibility(View.INVISIBLE);
    }

    /**
     * Show alert if there is no Internet connection
     */
    public void noInternetAlert() {
        AlertDialog.Builder alert = new AlertDialog.Builder(this);
        alert.setTitle("No internet connection!");
        alert.setMessage("Do you have internet connection? Is the WIFI turned on and configured correctly?");
        alert.setPositiveButton("Ok", null);
        alert.show();
    }

    /**
     * Load autocomplete data
     */
    public void loadAutocompletes() {
        Log.i("QSDFQSDFQSDFQSDFQDSFQSDf", "startauto");

        final MainActivity self = this;

        // Autocomplete fields
        new Thread(new Runnable() {
            public void run() {
                AUTOCOMPLETE_NMBS = new DataAdapter(self, R.layout.list_item, "NMBS");
                AUTOCOMPLETE_MIVB = new DataAdapter(self, R.layout.list_item, "MIVBSTIB");
                AUTOCOMPLETE_DELIJN = new DataAdapter(self, R.layout.list_item, "DeLijn");
                // TODO: leak restore
               /* txtStation.setAdapter(AUTOCOMPLETE_MIVB);
                txtFrom.setAdapter(AUTOCOMPLETE_NMBS);
                txtTo.setAdapter(AUTOCOMPLETE_NMBS);*/
            }
        }).start();

        Log.i("QSDFQSDFQSDFQSDFQDSFQSDf", "auto");
    }

    /**
     * Authenticate with API
     */
    public void authenticate() {
        api.pin = txtPin.getText().toString();
        api.pin = "13979880";
        api.authenticate();
    }

    public void authenticateResponse(boolean res){
        if (!res) {
            AlertDialog.Builder alert = new AlertDialog.Builder(this);
            alert.setTitle("Authentication failed!");
            alert.setMessage("No token received, is the PIN-code set/correct? Do you have internet connection?");
            alert.setPositiveButton("Ok", null);
            alert.show();
        }

        // Only load screen information on initial authentication
        if(bootFase){
            bootFase = false;

            // Get screen information
            new Thread(new Runnable() {
                public void run() {
                    try {
                        ArrayList<NameValuePair> params = new ArrayList<NameValuePair>();
                        params.add(new BasicNameValuePair("pin", api.pin));

                        String response = api.call("POST", "auth/alias", params);
                        String alias = response.split("\"")[1];

                        String screen_json = api.call("GET", alias + ".json", null);
                        JSONObject screen = new JSONObject(screen_json);
                        JSONObject screen_interface = screen.getJSONObject("interface");

                        float latitude = Float.parseFloat(screen_interface.getString("latitude"));
                        float longitude = Float.parseFloat(screen_interface.getString("longitude"));

                        if(longitude > 0 && latitude > 0){
                            MainActivity.longitude = longitude;
                            MainActivity.latitude = latitude;
                        }

                        loadAutocompletes();
                    } catch (NetworkErrorException e) {} catch (JSONException e) {
                        noInternetAlert();
                    }
                }
            }).start();
        }
    }

    /**
     * Show planning screen
     */
    public void showPlanning() {
        btnBack.setEnabled(false);
        lblNavWhere.setText("Notification");
        viewPlanning.setVisibility(View.VISIBLE);

        backToStartHandler.removeCallbacks(doubleClickRunnable);
        backToStartHandler.removeCallbacks(backToStartRunnable);

        // Delay back button
        backToStartHandler.postDelayed(doubleClickRunnable, 1000);
        // Back to start after 20 seconds
        backToStartHandler.postDelayed(backToStartRunnable, 30000);
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
        this.hideViews();
        lblNavWhere.setText("Settings");
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

        // Authenticate to check settings
        this.authenticate();

        // Back to the start screen
        this.startScreen();
    }

    /**
     * Callback for AutoUpdateApk
     */
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
        }
    }
}