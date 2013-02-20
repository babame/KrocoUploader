package com.beam.krocouploader;

import java.io.InputStream;
import java.util.HashMap;

import org.json.JSONException;
import org.json.JSONObject;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;

public class SingleSkinView extends Activity implements OnClickListener {
	private static final String URL = "";
	private ProgressDialog pDialog;
	private JsonParser jParser = new JsonParser();
	private ImageView img_image;
	private EditText txt_title, txt_desc, txt_author;
	private ImageButton btn_download;
	private Button btn_update, btn_delete;
	private String id = null;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		Intent i = getIntent();
		id = i.getStringExtra("id");
		setContentView(R.layout.skin_view);
		img_image = (ImageView) findViewById(R.id.img_skinView);
		txt_title = (EditText) findViewById(R.id.txt_skinViewTitle);
		txt_desc = (EditText) findViewById(R.id.txt_skinViewDesc);
		txt_author = (EditText) findViewById(R.id.txt_skinViewAuthor);
		btn_download = (ImageButton) findViewById(R.id.btn_downloadSkin);
		btn_update = (Button) findViewById(R.id.btn_update);
		btn_delete = (Button) findViewById(R.id.btn_delete);

		btn_download.setOnClickListener(this);
		btn_update.setOnClickListener(this);
		btn_delete.setOnClickListener(this);

		fillForm(id);
	}

	@Override
	public void onClick(View v) {
		switch (v.getId()) {
		case R.id.btn_update:
			// TODO implements
			break;
		case R.id.btn_delete:
			// TODO implements
			break;
		case R.id.btn_downloadSkin:
			// TODO implements
			break;
		default:
			break;
		}
	}

	private void fillForm(String skinId) {
		new GetSkin().execute(skinId);
		// show The Image
		new DownloadImageTask(img_image).execute(URL + skinId + "/picture");
	}

	private class GetSkin extends
			AsyncTask<String, Void, HashMap<String, String>> {

		@Override
		protected void onPreExecute() {
			super.onPreExecute();
			pDialog = new ProgressDialog(SingleSkinView.this);
			pDialog.setMessage("Fetching Data, Please Wait...");
			pDialog.setCancelable(false);
			pDialog.setIndeterminate(false);
			pDialog.show();
		}

		@Override
		protected HashMap<String, String> doInBackground(String... params) {
			HashMap<String, String> result = new HashMap<String, String>();
			JSONObject json = jParser.makeHttpRequest(URL + params[0], "GET",
					null);
			try {
				int success = json.getInt("success");
				if (success == 1) {
					JSONObject skin = json.getJSONObject("skin");
					result.put("id", skin.getString("id"));
					result.put("title", skin.getString("title"));
					result.put("description", skin.getString("description"));
					result.put("author", skin.getString("author"));
					result.put("apk_md5", skin.getString("apk_md5"));
				}
			} catch (JSONException e) {
				e.printStackTrace();
			}
			return result;
		}

		@Override
		protected void onPostExecute(final HashMap<String, String> result) {
			super.onPostExecute(result);
			if (pDialog.isShowing())
				pDialog.dismiss();
			txt_title.setText(result.get("title"));
			txt_desc.setText(result.get("description"));
			txt_author.setText(result.get("author"));
			runOnUiThread(new Runnable() {

				@Override
				public void run() {
					setTitle(result.get("title"));
				}
			});
		}
	}

	private class DownloadImageTask extends AsyncTask<String, Void, Bitmap> {
		ImageView bmImage;

		public DownloadImageTask(ImageView bmImage) {
			this.bmImage = bmImage;
		}

		protected Bitmap doInBackground(String... urls) {
			String urldisplay = urls[0];
			Bitmap mIcon11 = null;
			try {
				InputStream in = new java.net.URL(urldisplay).openStream();
				mIcon11 = BitmapFactory.decodeStream(in);
			} catch (Exception e) {
				Log.e("Error", e.getMessage());
				e.printStackTrace();
			}
			return mIcon11;
		}

		protected void onPostExecute(Bitmap result) {
			bmImage.setImageBitmap(result);
		}
	}
}
