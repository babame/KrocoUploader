package com.beam.krocouploader;

import java.util.ArrayList;
import java.util.HashMap;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.app.ListActivity;
import android.app.ProgressDialog;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.View;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.SimpleAdapter;
import android.widget.TextView;

public class KrocoList extends ListActivity {
	private static final String URL = "http://ntahahase.zapto.org/Krocolizer/api.php/skins";
	private ProgressDialog pDialog;
	private JsonParser jParser = new JsonParser();
	private JSONArray skins = null;
	private ArrayList<HashMap<String, String>> skinsList;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		getSkin();
	}

	@Override
	protected void onListItemClick(ListView l, View v, int position, long id) {
		super.onListItemClick(l, v, position, id);
		TextView txt_id = (TextView) v.findViewById(R.id.txt_id);
		String skinId = txt_id.getText().toString();
		Intent i = new Intent(this, SingleSkinView.class);
		i.putExtra("id", skinId);
		startActivity(i);
	}

	private void getSkin() {
		new GetKrocoSkin().execute();
	}

	private class GetKrocoSkin extends AsyncTask<Void, Void, Void> {

		@Override
		protected void onPreExecute() {
			super.onPreExecute();
			pDialog = new ProgressDialog(KrocoList.this);
			pDialog.setMessage("Fetching Data. Please wait...");
			pDialog.setIndeterminate(false);
			pDialog.setCancelable(false);
			pDialog.show();
		}

		@Override
		protected Void doInBackground(Void... params) {
			JSONObject json = jParser.makeHttpRequest(URL, "GET", null);
			try {
				int success = json.getInt("success");
				if (success == 1) {
					skinsList = new ArrayList<HashMap<String,String>>();
					skins = json.getJSONArray("skins");
					for (int i = 0; i < skins.length(); i++) {
						JSONObject c = skins.getJSONObject(i);

						HashMap<String, String> map = new HashMap<String, String>();
						map.put("id", c.getString("id"));
						map.put("title", c.getString("title"));
						map.put("description", c.getString("description"));
						map.put("author", c.getString("author"));
						skinsList.add(map);
					}
				} else {
					// no skins found
				}
			} catch (JSONException e) {
				e.printStackTrace();
			}
			return null;
		}

		@Override
		protected void onPostExecute(Void result) {
			super.onPostExecute(result);
			if (pDialog.isShowing())
				pDialog.dismiss();

			runOnUiThread(new Runnable() {

				@Override
				public void run() {
					ListAdapter adapter = new SimpleAdapter(KrocoList.this,
							skinsList, R.layout.list_item, new String[] { "id",
									"title", "description", "author" },
							new int[] { R.id.txt_id, R.id.txt_title,
									R.id.txt_desc, R.id.txt_author });
					setListAdapter(adapter);
				}
			});
		}
	}
}
