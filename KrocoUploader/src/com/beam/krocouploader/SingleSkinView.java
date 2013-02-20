package com.beam.krocouploader;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;

import org.apache.commons.io.FileUtils;
import org.json.JSONException;
import org.json.JSONObject;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.AlertDialog.Builder;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Base64;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.Toast;

public class SingleSkinView extends Activity implements OnClickListener,
		android.content.DialogInterface.OnClickListener {
	private static final String TAG = SingleSkinView.class.getSimpleName();
	private static final String URL = "";
	private String pictUri = null, apkUri = null;
	private ProgressDialog pDialog;
	private JsonParser jParser = new JsonParser();
	private EditText txt_title, txt_desc, txt_author;
	private ImageButton btn_image, btn_download;
	private Button btn_update, btn_delete;
	private String id = null;

	HashMap<String, String> data;

	private static final int FETCH = 0;
	private static final int DELETE = 1;
	private static final int UPDATE = 2;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		// get extras
		Intent i = getIntent();
		id = i.getStringExtra("id");
		setContentView(R.layout.skin_view);

		// find view
		btn_image = (ImageButton) findViewById(R.id.img_skinView);
		txt_title = (EditText) findViewById(R.id.txt_skinViewTitle);
		txt_desc = (EditText) findViewById(R.id.txt_skinViewDesc);
		txt_author = (EditText) findViewById(R.id.txt_skinViewAuthor);
		btn_download = (ImageButton) findViewById(R.id.btn_downloadSkin);
		btn_update = (Button) findViewById(R.id.btn_update);
		btn_delete = (Button) findViewById(R.id.btn_delete);

		btn_image.setOnClickListener(this);
		btn_download.setOnClickListener(this);
		btn_update.setOnClickListener(this);
		btn_delete.setOnClickListener(this);

		// fill form
		fillForm(id);
	}

	@Override
	public void onClick(View v) {
		switch (v.getId()) {
		case R.id.btn_update:
			new ProcessingTask().execute(UPDATE);
			break;
		case R.id.btn_delete:
			new ProcessingTask().execute(DELETE);
			break;
		case R.id.btn_downloadSkin:
			Builder builder = new Builder(SingleSkinView.this);
			builder.setTitle("What you want to do?");
			builder.setPositiveButton("Download Apk", SingleSkinView.this);
			builder.setNegativeButton("Cancel", SingleSkinView.this);
			builder.setNeutralButton("Change Apk", SingleSkinView.this);
			AlertDialog dialog = builder.create();
			dialog.show();
			break;
		case R.id.img_skinView:
			Intent i = new Intent(this, FileChooser.class);
			i.putExtra("filetype", new String[] { "png", "jpg", "bmp" });
			startActivityForResult(i, DELETE);
		default:
			break;
		}
	}

	@Override
	public void onClick(DialogInterface dialog, int which) {
		switch (which) {
		case AlertDialog.BUTTON_POSITIVE:
			// TODO implements
			break;
		case AlertDialog.BUTTON_NEGATIVE:
			dialog.dismiss();
			break;
		case AlertDialog.BUTTON_NEUTRAL:
			Intent i = new Intent(this, FileChooser.class);
			i.putExtra("filetype", new String[] { "apk" });
			startActivityForResult(i, UPDATE);
			break;
		default:
			break;
		}
	}

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		super.onActivityResult(requestCode, resultCode, data);
		switch (requestCode) {
		case DELETE: // too lazy ;)
			if (resultCode == RESULT_OK) {
				Bundle extras = data.getExtras();
				if (null != extras.getString("result")) {
					pictUri = extras.getString("result");
					Bitmap b = BitmapFactory.decodeFile(pictUri);
					btn_image.setImageBitmap(b);
				}
			}
			break;
		case UPDATE:
			if (resultCode == RESULT_OK) {
				Bundle extras = data.getExtras();
				if (null != extras.getString("result")) {
					apkUri = extras.getString("result");
					PackageManager pm = getPackageManager();
					PackageInfo pi = pm.getPackageArchiveInfo(apkUri, 0);

					pi.applicationInfo.sourceDir = apkUri;
					pi.applicationInfo.publicSourceDir = apkUri;

					pi.applicationInfo.loadIcon(pm);
					btn_download.setImageDrawable(pi.applicationInfo
							.loadIcon(pm));
					txt_title.setText(pi.applicationInfo.loadLabel(pm));
					if (null != pi.applicationInfo.loadDescription(pm))
						txt_desc.setText(pi.applicationInfo.loadDescription(pm));
				}
			}
			break;
		default:
			break;
		}
	}

	private void fillForm(String skinId) {
		// show The Image
		new ProcessingTask().execute(FETCH);
		new DownloadImageTask(btn_image).execute(URL + skinId + "/picture");
	}

	private void setProgressDialog(String message) {
		pDialog = new ProgressDialog(SingleSkinView.this);
		pDialog.setMessage(message);
		pDialog.setCancelable(false);
		pDialog.setIndeterminate(false);
		pDialog.show();
	}

	private class ProcessingTask extends
			AsyncTask<Integer, Void, HashMap<String, Integer>> {

		@Override
		protected void onPreExecute() {
			super.onPreExecute();
			setProgressDialog("Processing, Please Wait...");
		}

		@Override
		protected HashMap<String, Integer> doInBackground(Integer... type) {
			HashMap<String, Integer> result = new HashMap<String, Integer>();
			result.put("type", type[0]);

			switch (type[0]) {
			case FETCH:
				data = new HashMap<String, String>();
				JSONObject fetch = jParser.makeHttpRequest(URL + id, "GET",
						null);
				try {
					int success = fetch.getInt("success");
					if (success == 1) {
						JSONObject skin = fetch.getJSONObject("skin");
						data.put("id", skin.getString("id"));
						data.put("title", skin.getString("title"));
						data.put("description", skin.getString("description"));
						data.put("author", skin.getString("author"));
						data.put("apk_md5", skin.getString("apk_md5"));
					}
					result.put("success", success);
					return result;
				} catch (JSONException e) {
					e.printStackTrace();
					result.put("success", 0);
					return result;
				}
			case DELETE:
				JSONObject delete = jParser.makeHttpRequest(URL + "skin/" + id,
						"DELETE", null);
				try {
					int success = delete.getInt("success");
					result.put("success", success);
					return result;
				} catch (JSONException e) {
					e.printStackTrace();
					result.put("success", 0);
					return result;
				}
			case UPDATE:
				String title = txt_title.getText().toString(),
				desc = txt_desc.getText().toString(),
				author = txt_author.getText().toString();
				File filepict = null,
				fileApk = null;
				try {
					JSONObject params = new JSONObject();
					if (null != pictUri) {
						filepict = new File(pictUri);
						byte[] pictByteArray = FileUtils
								.readFileToByteArray(filepict);
						String pictEnc = Base64
								.encodeToString(pictByteArray, 0);
						params.put("img", pictEnc);
					}
					if (null != apkUri) {
						fileApk = new File(apkUri);
						byte[] apkByteArray = FileUtils
								.readFileToByteArray(fileApk);
						String apkEnc = Base64.encodeToString(apkByteArray, 0);
						params.put("apk", apkEnc);
					}
					params.put("title", title);
					params.put("author", author);
					params.put("description", desc);
					Log.d(TAG, String.format("title: %s\nauthor: %s\ndescription: %s", title, author, desc));
					JSONObject update = jParser.makeHttpRequest(URL + "skin/"
							+ id, "PUT", params);
					int success = update.getInt("success");
					result.put("success", success);
					return result;
				} catch (JSONException e) {
					e.printStackTrace();
					result.put("success", 0);
					return result;
				} catch (IOException e) {
					e.printStackTrace();
					result.put("success", 0);
					return result;
				}
			default:
				result.put("success", 0);
				return result;
			}
		}

		@Override
		protected void onPostExecute(HashMap<String, Integer> result) {
			super.onPostExecute(result);
			if (pDialog.isShowing())
				pDialog.dismiss();
			if (result.get("success") == 1) {
				switch (result.get("type")) {
				case FETCH:
					txt_title.setText(data.get("title"));
					txt_desc.setText(data.get("description"));
					txt_author.setText(data.get("author"));
					runOnUiThread(new Runnable() {

						@Override
						public void run() {
							setTitle(data.get("title"));
						}
					});
					break;
				case DELETE:
					Toast.makeText(SingleSkinView.this, "Delete Success..",
							Toast.LENGTH_SHORT).show();
					finish();
					break;
				case UPDATE:
					Toast.makeText(SingleSkinView.this, "Update Success..",
							Toast.LENGTH_SHORT).show();
					finish();
					break;
				default:
					break;
				}
			} else {
				Toast.makeText(SingleSkinView.this, "An Error occured",
						Toast.LENGTH_SHORT).show();
			}
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
