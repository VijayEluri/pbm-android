package com.pbm;

import android.os.Bundle;
import android.text.Html;
import android.text.Spanned;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.ProgressBar;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.text.DateFormat;
import java.text.DecimalFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ExecutionException;

public class RecentScores extends PinballMapActivity {
	private ProgressBar progressBar;
	private List<Spanned> recentScores = new ArrayList<>();
	final private static int NUM_RECENT_SCORES_TO_SHOW = 20;

	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		setContentView(R.layout.recent_scores);

		logAnalyticsHit("com.pbm.RecentScores");

		enableLoadingSpinnerForView(R.id.scoreRelativeLayout);

		new Thread(new Runnable() {
			public void run() {
				while (!getPBMApplication().getIsDataInitialized()) {
					try {
						Thread.sleep(1000);
					} catch (InterruptedException e) {
						e.printStackTrace();
					}
				}

				try {
					getLocationData();
				} catch (InterruptedException | ExecutionException | JSONException | ParseException | IOException e) {
					e.printStackTrace();
				}

				RecentScores.super.runOnUiThread(new Runnable() {
					@Override
					public void run() {
						disableLoadingSpinner();
						ListView recentScoresTable = (ListView)findViewById(R.id.recentscorestable);
						recentScoresTable.setAdapter(new ArrayAdapter<>(RecentScores.this, R.layout.custom_list_item_1, recentScores));					}
				});
			}
		}).start();
	}

    @SuppressWarnings("deprecation")
	public void getLocationData() throws IOException, InterruptedException, ExecutionException, JSONException, ParseException {
		PBMApplication app = getPBMApplication();

		String json = new RetrieveJsonTask().execute(
			app.requestWithAuthDetails(regionBase + "machine_score_xrefs.json?limit=" + NUM_RECENT_SCORES_TO_SHOW),
			"GET"
		).get();

		if (json == null) {
			return;
		}
		
		DecimalFormat formatter = new DecimalFormat("#,###");
		JSONObject jsonObject = new JSONObject(json);
		JSONArray scores = jsonObject.getJSONArray("machine_score_xrefs");
		for (int i=0; i < scores.length(); i++) {
			JSONObject msx = scores.getJSONObject(i);
			
			int lmxID = msx.getInt("location_machine_xref_id");
			try {
				app.loadLmx(lmxID);
			} catch (ExecutionException | InterruptedException | ParseException | JSONException | IOException e) {
				e.printStackTrace();
			}
			if (msx.get("score") != null) {
				LocationMachineXref lmx = app.getLmx(lmxID);
				long score = msx.getLong("score");
				String username = msx.getString("username");
				String rawScoreDate = msx.getString("created_at").split("T")[0];
				Location location = lmx.getLocation(this);
				Machine machine = lmx.getMachine(this);

				DateFormat inputDF = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
				DateFormat outputDF = new SimpleDateFormat("MM/dd/yyyy", Locale.getDefault());
				Date dateCreatedAt = inputDF.parse(rawScoreDate);
				String scoreDate = outputDF.format(dateCreatedAt);

				String title = "<u>" + machine.getName() + "</u><br />" +
					formatter.format(score) + " by <b>" + username + "</b>" + "<br />" +
					"<small>at " + location.getName() + " on " + scoreDate + "</small>";

				recentScores.add(Html.fromHtml(title));
			}
		}
	}
}
