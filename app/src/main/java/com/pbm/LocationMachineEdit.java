package com.pbm;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
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
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.concurrent.ExecutionException;

public class LocationMachineEdit extends PinballMapActivity {
	private Location location;
	private LocationMachineXref lmx;
	private ConditionsArrayAdapter conditionsAdapter;
	private ScoresArrayAdapter scoresAdapter;
	private View.OnClickListener removeHandler;
	private final int NUMBER_OF_CONDITIONS_TO_SHOW = 5;
	private final int NUMBER_OF_SCORES_TO_SHOW = 5;

	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.location_machine_edit);

		logAnalyticsHit("com.pbm.LocationMachineEdit");

		lmx = (LocationMachineXref) getIntent().getExtras().get("lmx");

		location = lmx.getLocation(this);
		final Machine machine = getPBMApplication().getMachine(lmx.machineID);

		setTitle(machine.name + " @ " + location.name);
		removeHandler = new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				removeMachineDialog();
			}
		};
		Button removeMachine = (Button) findViewById(R.id.remove_machine_button);
		removeMachine.setOnClickListener(removeHandler);

		Button addMachine = (Button) findViewById(R.id.add_condition_button);
		addMachine.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				Intent myIntent = new Intent();
				myIntent.setClassName("com.pbm", "com.pbm.ConditionEdit");
				myIntent.putExtra("lmx", lmx);
				startActivityForResult(myIntent, QUIT_RESULT);
			}
		});

		TextView pintips = (TextView) findViewById(R.id.pintips);
		pintips.setMovementMethod(LinkMovementMethod.getInstance());
		pintips.setText(Html.fromHtml("<a href=\"http://pintips.net/pinmap/machine/"+ Integer.toString(machine.id) + "\">View playing tips on pintips.net</a>"));

		Button otherLocations = (Button) findViewById(R.id.other_locations);
		otherLocations.setText("Lookup Other Locations With " + machine.name);

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
										app.requestWithAuthDetails(regionlessBase + "location_machine_xrefs/" + Integer.toString(lmx.id) + ".json"), "DELETE"
									).get();
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

	@Override
	protected void onResume() {
		super.onResume();
		loadConditions();
		loadScores();
	}

	private void loadScores() {
		Log.d("com.pbm", "msx edit resume");

		NonScrollListView listView = (NonScrollListView) findViewById(R.id.score_list);
		View emptyView = findViewById(R.id.empty_score);
		listView.setEmptyView(emptyView);

		final LayoutInflater inflater = LayoutInflater.from(getApplicationContext());

		ArrayList scores = getPBMApplication().getMachineScoresByLMXId(lmx.id);

		int scoreCount = scores.size() < NUMBER_OF_SCORES_TO_SHOW ? scores.size() : NUMBER_OF_SCORES_TO_SHOW;

		scoresAdapter = new ScoresArrayAdapter(this, inflater, new ArrayList(scores.subList(0, scoreCount)));
		listView.setAdapter(scoresAdapter);
		conditionsAdapter.sort(new Comparator<Condition>() {
			@Override
			public int compare(Condition lhs, Condition rhs) {
				return rhs.getDate().compareTo(lhs.getDate());
			}
		});

	}

	private void loadConditions() {
		Log.d("com.pbm", "location Machine edit resume");
		NonScrollListView listView = (NonScrollListView) findViewById(android.R.id.list);
		View emptyView = findViewById(android.R.id.empty);
		listView.setEmptyView(emptyView);

		final LayoutInflater inflater = LayoutInflater.from(getApplicationContext());

		ArrayList conditions = getPBMApplication().getLmxConditionsByID(lmx.id).getConditions();

		int conditionCount = conditions.size() < NUMBER_OF_CONDITIONS_TO_SHOW ? conditions.size() : NUMBER_OF_CONDITIONS_TO_SHOW;

		conditionsAdapter = new ConditionsArrayAdapter(this, inflater, new ArrayList(conditions.subList(0, conditionCount)));
		listView.setAdapter(conditionsAdapter);
		conditionsAdapter.sort(new Comparator<Condition>() {
			@Override
			public int compare(Condition lhs, Condition rhs) {
				return rhs.getDate().compareTo(lhs.getDate());
			}
		});
	}

	public void clickHandler(View view) {
		switch (view.getId()) {
			case R.id.condition:
				Intent myIntent = new Intent();
				myIntent.setClassName("com.pbm", "com.pbm.ConditionEdit");
				myIntent.putExtra("lmx", lmx);
				startActivityForResult(myIntent, QUIT_RESULT);

				break;
		}
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