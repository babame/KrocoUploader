package com.beam.krocouploader;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.Typeface;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.Window;
import android.widget.FrameLayout;
import android.widget.FrameLayout.LayoutParams;
import android.widget.TextView;

import com.beam.krocouploader.glcoverflu.CoverFluGL;
import com.beam.krocouploader.glcoverflu.CoverFluGL.CoverFluListener;
import com.beam.krocouploader.utils.C;
import com.beam.krocouploader.utils.FileCache;
import com.beam.krocouploader.utils.SpannableBuilder;
import com.beam.krocouploader.utils.Utils;

public class KrocoList extends Activity {

	private TextView txt_info;
	private FrameLayout parentLayout;

	private ProgressDialog pDialog;
	private JsonParser jParser = new JsonParser();
	private JSONArray skins = null;
	private ArrayList<HashMap<String, String>> skinsList;
	private CoverFluGL mCoverFlu;
	private String[] IMAGE_ADDR = new String[] {};
	private FileCache fileCache;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		requestWindowFeature(Window.FEATURE_NO_TITLE);
		parentLayout = new FrameLayout(this);
		parentLayout.setLayoutParams(new LayoutParams(
				LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT));
		txt_info = new TextView(this);
		txt_info.setTextColor(Color.WHITE);
		txt_info.setTextSize(15f);
		fileCache = new FileCache(this);
		getSkin();
	}

	private void getSkin() {
		new GetKrocoSkin(new KrocoAsyncListener()).execute();
	}

	private class KrocoAsyncListener implements ProcessingListener {

		@Override
		public void onComplete() {
			mCoverFlu = new CoverFluGL(KrocoList.this);
			parentLayout.addView(mCoverFlu, new LayoutParams(
					LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT));
			LayoutParams params = new LayoutParams(LayoutParams.WRAP_CONTENT,
					LayoutParams.WRAP_CONTENT);
			params.setMargins(0, 20, 0, 0);
			parentLayout.addView(txt_info, params);
			mCoverFlu.setCoverFluListener(new KrocoGLCoverGluListener());
			mCoverFlu.setSelection(0);
			mCoverFlu.setSensitivity(1.5f);
			setContentView(parentLayout);
		}
	}

	private class KrocoGLCoverGluListener implements CoverFluListener {

		@Override
		public int getCount(CoverFluGL view) {
			return IMAGE_ADDR.length;
		}

		@Override
		public Bitmap getImage(CoverFluGL anotherCoverFlow, int position) {
			return BitmapFactory.decodeFile(IMAGE_ADDR[position]);
		}

		@Override
		public void tileOnTop(CoverFluGL view, final int position) {
			SpannableBuilder builder = new SpannableBuilder(KrocoList.this);
			builder.append(skinsList.get(position).get("title"), Typeface.BOLD)
					.appendLine()
					.append("By: " + skinsList.get(position).get("author"),
							Typeface.ITALIC).appendLine()
					.append(skinsList.get(position).get("description"));
			final CharSequence info = builder.toSpannableString();
			runOnUiThread(new Runnable() {

				@Override
				public void run() {
					txt_info.setText(info);
				}
			});
		}

		@Override
		public void topTileClicked(CoverFluGL view, int position) {
			Intent i = new Intent(KrocoList.this, SingleSkinView.class);
			i.putExtra("id", skinsList.get(position).get("id"));
			startActivity(i);
			finish();
		}

	}

	/**
	 * Listener for GetKrocoSkinAsyncTask
	 * 
	 * @author adrianbabame
	 * 
	 */
	private static interface ProcessingListener {
		/**
		 * Yeah just onComplete
		 */
		public void onComplete();
	}

	private class GetKrocoSkin extends AsyncTask<Void, Void, Void> {
		private ProcessingListener listener;

		public GetKrocoSkin(ProcessingListener listener) {
			this.listener = listener;
		}

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
			try {
				JSONObject json = jParser.makeHttpRequest(C.URL + "skins", "GET",
						null);
				int success = json.getInt("success");
				if (success == 1) {
					ArrayList<String> stringArrayList = new ArrayList<String>();
					skinsList = new ArrayList<HashMap<String, String>>();
					skins = json.getJSONArray("skins");
					for (int i = 0; i < skins.length(); i++) {
						JSONObject c = skins.getJSONObject(i);

						HashMap<String, String> map = new HashMap<String, String>();
						map.put("id", c.getString("id"));
						map.put("title", c.getString("title"));
						map.put("description", c.getString("description"));
						map.put("author", c.getString("author"));
						skinsList.add(map);
						File f = fileCache.getFile(C.URL + c.getString("id")
								+ "/picture");
						if (!f.exists()) {
							try {
								URL imageUrl = new URL(C.URL
										+ c.getString("id") + "/picture");
								HttpURLConnection conn = (HttpURLConnection) imageUrl
										.openConnection();
								conn.setConnectTimeout(30000);
								conn.setReadTimeout(30000);
								conn.setInstanceFollowRedirects(true);
								InputStream is = conn.getInputStream();
								OutputStream os = new FileOutputStream(f);
								Utils.CopyStream(is, os);
								os.close();
							} catch (Exception ex) {
								ex.printStackTrace();
								return null;
							}
						}
						stringArrayList.add(f.getAbsolutePath());
					}
					IMAGE_ADDR = stringArrayList
							.toArray(new String[stringArrayList.size()]);
				} else {
					// no skins found
				}
			} catch (JSONException e) {
				e.printStackTrace();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			return null;
		}

		@Override
		protected void onPostExecute(Void result) {
			super.onPostExecute(result);
			listener.onComplete();
			if (pDialog.isShowing())
				pDialog.dismiss();
		}
	}
}
