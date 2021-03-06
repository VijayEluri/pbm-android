package com.pbm;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.Html;
import android.text.method.LinkMovementMethod;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Date;
import java.util.Locale;
import java.util.concurrent.ExecutionException;

public class LocationMachineEdit extends PinballMapActivity {
	private Location location;
	private Machine machine;
	private LocationMachineXref lmx;

	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.location_machine_edit);

		logAnalyticsHit("com.pbm.LocationMachineEdit");

		lmx = (LocationMachineXref) getIntent().getExtras().get("lmx");

		if (lmx != null) {
			location = lmx.getLocation(this);
			machine = getPBMApplication().getMachine(lmx.getMachineID());
		}

		if (location != null && machine != null){
			setTitle(machine.getName() + " @ " + location.getName());

			initializeRemoveMachineButton();
			initializeAddMachineConditionButton();
			initializePintipsButton();
			initializeAddScoreButton();
			initializeOtherLocationsButton();

			try {
				loadConditions();
			} catch (ParseException e) {
				e.printStackTrace();
			}
			loadScores();
		}
	}

	public void initializeRemoveMachineButton() {
		Button removeMachine = (Button) findViewById(R.id.remove_machine_button);
		if (!getPBMApplication().userIsAuthenticated()) {
			removeMachine.setText(R.string.login_to_remove_machine);
		}

		View.OnClickListener removeHandler = new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				Intent myIntent = new Intent();

				if (!getPBMApplication().userIsAuthenticated()) {
					myIntent.setClassName("com.pbm", "com.pbm.Login");
                    startActivityForResult(myIntent, QUIT_RESULT);
				} else {
					removeMachineDialog();
				}

			}
		};

		removeMachine.setOnClickListener(removeHandler);
	}
	public void initializeAddMachineConditionButton() {
		Button addMachineCondition = (Button) findViewById(R.id.add_condition_button);
		if (!getPBMApplication().userIsAuthenticated()) {
			addMachineCondition.setText(R.string.login_to_add_condition);
		}

		addMachineCondition.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
			Intent myIntent = new Intent();

			if (!getPBMApplication().userIsAuthenticated()) {
				myIntent.setClassName("com.pbm", "com.pbm.Login");
			} else {
				myIntent.setClassName("com.pbm", "com.pbm.ConditionEdit");
				myIntent.putExtra("lmx", lmx);
			}

			startActivityForResult(myIntent, QUIT_RESULT);
			}
		});
	}

	public void initializePintipsButton() {
		Button pintips = (Button) findViewById(R.id.pintips);
		pintips.setMovementMethod(LinkMovementMethod.getInstance());

		String urlLookupTypeData = urlLookupTypeData = "machine/" + Integer.toString(machine.getId());
		pintips.setText(Html.fromHtml("<a href=\"http://pintips.net/pinmap/" + urlLookupTypeData + "\">View playing tips on pintips.net</a>"));
	}

	public void initializeAddScoreButton() {
		Button addScore = (Button) findViewById(R.id.add_new_score);
		if (!getPBMApplication().userIsAuthenticated()) {
			addScore.setText(R.string.login_to_add_score);
		}
		addScore.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
			Intent myScoreIntent = new Intent();

			if (!getPBMApplication().userIsAuthenticated()) {
				myScoreIntent.setClassName("com.pbm", "com.pbm.Login");
			} else {
				myScoreIntent.setClassName("com.pbm", "com.pbm.EnterScore");
				myScoreIntent.putExtra("lmx", lmx);
			}

			startActivityForResult(myScoreIntent, QUIT_RESULT);
			}
		});
	}

	public void initializeOtherLocationsButton() {
		Button otherLocations = (Button) findViewById(R.id.other_locations);
		otherLocations.setText(String.format("Lookup Other Locations With %s", machine.getName()));
		otherLocations.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				Intent myIntent = new Intent();
				myIntent.putExtra("Machine", machine);
				myIntent.setClassName("com.pbm", "com.pbm.MachineLookupDetail");

				startActivityForResult(myIntent, QUIT_RESULT);
			}
		});
	}

	private void removeMachineDialog() {
		new AlertDialog.Builder(LocationMachineEdit.this)
			.setIcon(android.R.drawable.ic_dialog_alert).setTitle("Remove this machine?").setMessage("Are you sure?")
			.setPositiveButton("Yes", new DialogInterface.OnClickListener() {
				public void onClick(DialogInterface dialog, int which) {
				new Thread(new Runnable() {
					public void run() {
						try {
							PBMApplication app = getPBMApplication();

							location.removeMachine(LocationMachineEdit.this, lmx);
							new RetrieveJsonTask().execute(
								app.requestWithAuthDetails(regionlessBase + "location_machine_xrefs/" + Integer.toString(lmx.getId()) + ".json"),
								"DELETE"
							).get();

							SharedPreferences settings = getBaseContext().getSharedPreferences(PinballMapActivity.PREFS_NAME, 0);
							location.setDateLastUpdated(new SimpleDateFormat("MM/dd/yyyy", Locale.getDefault()).format(new Date()));
							location.setLastUpdatedByUsername(settings.getString("username", ""));
							app.updateLocation(location);
						} catch (InterruptedException | ExecutionException e) {
							e.printStackTrace();
						}

						LocationMachineEdit.super.runOnUiThread(new Runnable() {
							public void run() {
								Toast.makeText(getBaseContext(), "OK, machine deleted.", Toast.LENGTH_LONG).show();

								setResult(REFRESH_RESULT);
								LocationMachineEdit.this.finish();
							}
						});
					}
				}).start();
				}
			})
			.setNegativeButton("No", null)
			.show();
	}

	public boolean onCreateOptionsMenu(Menu menu) {
		MenuInflater inflater = getMenuInflater();
		inflater.inflate(R.menu.location_machine_edit_menu, menu);

		if (!getPBMApplication().userIsAuthenticated()) {
			menu.removeItem(R.id.remove_button);
		}

		return super.onCreateOptionsMenu(menu);
	}

	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
			case R.id.remove_button:
				removeMachineDialog();
				return true;
			default:
				return super.onOptionsItemSelected(item);
		}
	}

	private void loadScores() {
		Log.d("com.pbm", "msx edit resume");

		NonScrollListView listView = (NonScrollListView) findViewById(R.id.score_list);
		View emptyView = findViewById(R.id.empty_score);
		listView.setEmptyView(emptyView);

		final LayoutInflater inflater = LayoutInflater.from(getApplicationContext());

		ArrayList scores = getPBMApplication().getMachineScoresByLMXId(lmx.getId());

		int NUMBER_OF_SCORES_TO_SHOW = 5;
		int scoreCount = scores.size() < NUMBER_OF_SCORES_TO_SHOW ? scores.size() : NUMBER_OF_SCORES_TO_SHOW;

		ScoresArrayAdapter scoresAdapter = new ScoresArrayAdapter(this, inflater, new ArrayList(scores.subList(scores.size() - scoreCount, scores.size())));
		listView.setAdapter(scoresAdapter);
		scoresAdapter.sort(new Comparator<MachineScore>() {
			public int compare(MachineScore lhs, MachineScore rhs) {
				DateFormat f = new SimpleDateFormat("MM/dd/yyyy");
				try {
					return f.parse(rhs.getDate()).compareTo(f.parse(lhs.getDate()));
				} catch (ParseException e) {
					throw new IllegalArgumentException(e);
				}
			}
		});
	}

	private void loadConditions() throws ParseException {
		Log.d("com.pbm", "location Machine edit resume");
		NonScrollListView listView = (NonScrollListView) findViewById(android.R.id.list);
		View emptyView = findViewById(android.R.id.empty);
		listView.setEmptyView(emptyView);

		final LayoutInflater inflater = LayoutInflater.from(getApplicationContext());

		ArrayList conditions = getPBMApplication().getLmxConditionsByID(lmx.getId()).getConditions();

		int NUMBER_OF_CONDITIONS_TO_SHOW = 5;
		int conditionCount = conditions.size() < NUMBER_OF_CONDITIONS_TO_SHOW ? conditions.size() : NUMBER_OF_CONDITIONS_TO_SHOW;

		ConditionsArrayAdapter conditionsAdapter = new ConditionsArrayAdapter(this, inflater, new ArrayList(conditions.subList(conditions.size() - conditionCount, conditions.size())));
		listView.setAdapter(conditionsAdapter);
		conditionsAdapter.sort(new Comparator<Condition>() {
			@Override
			public int compare(Condition lhs, Condition rhs) {
				DateFormat f = new SimpleDateFormat("MM/dd/yyyy");
				try {
					return f.parse(rhs.getDate()).compareTo(f.parse(lhs.getDate()));
				} catch (ParseException e) {
					throw new IllegalArgumentException(e);
				}
			}
		});
	}

	public void activityRefreshResult() {
		LocationMachineEdit.super.runOnUiThread(new Runnable() {
			public void run() {
				Toast.makeText(getBaseContext(), "Thanks for updating that machine.", Toast.LENGTH_LONG).show();

				setResult(REFRESH_RESULT);
				LocationMachineEdit.this.finish();
			}
		});
	}
}
