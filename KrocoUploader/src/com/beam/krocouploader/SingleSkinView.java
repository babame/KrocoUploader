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
import android.telephony.TelephonyManager;
import android.util.Base64;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.Toast;

import com.beam.krocouploader.chooser.FileChooser;
import com.beam.krocouploader.utils.C;

public class SingleSkinView extends Activity implements OnClickListener,
		android.content.DialogInterface.OnClickListener {
	private String id = null, pictUri = null, apkUri = null;

	/* View */
	private ProgressDialog pDialog;
	private EditText txt_title, txt_desc, txt_author;
	private ImageButton btn_image, btn_download;
	private Button btn_update, btn_delete;

	/* Instantiate JsonParser */
	private JsonParser jParser = new JsonParser();
	/* Defining data here, workaround for AsyncTask data-passing limitation */
	private HashMap<String, String> data;

	/* Defining integer for visual reference and code completion only */
	/* Yea integer, because its the easiest way to deal with switch case */
	private static final int FETCH = 0;
	private static final int DELETE = 1;
	private static final int UPDATE = 2;
	
	private String imei;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		/* Get extras we've passed before */
		Intent i = getIntent();
		id = i.getStringExtra("id");

		setContentView(R.layout.skin_view);

		/* Find view */
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

		TelephonyManager tel = (TelephonyManager) this.getSystemService(TELEPHONY_SERVICE);
		imei = tel.getDeviceId();
		/* Fill form */
		fillForm(id);
	}

	@Override
	public void onClick(View v) {
		switch (v.getId()) {
		case R.id.btn_update:
			/* Update server database with the new data */
			new ProcessingTask().execute(UPDATE);
			break;
		case R.id.btn_delete:
			/* Delete skin in the server database */
			new ProcessingTask().execute(DELETE);
			break;
		case R.id.btn_downloadSkin:
			/* Instantiate builder */
			Builder builder = new Builder(SingleSkinView.this);
			/* Set dialog box title */
			builder.setTitle("What you want to do?");
			/* Set dialog box message */
			builder.setMessage("Isn't that obvious?");
			/* Set Positive, Negative, and Neutral Button text */
			builder.setPositiveButton("Download Apk", SingleSkinView.this);
			builder.setNegativeButton("Cancel", SingleSkinView.this);
			builder.setNeutralButton("Change Apk", SingleSkinView.this);
			/* Instantiate dialog */
			AlertDialog dialog = builder.create();
			/* Finally Show dialog */
			dialog.show();
			break;
		case R.id.img_skinView:
			/*
			 * Choose new picture for skin Instantiate new FileChooser intent,
			 * set filter to image
			 */
			Intent i = new Intent(this, FileChooser.class);
			i.putExtra(C.FILETYPE, C.IMAGE);
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
			/* Cancel Button, just dismiss the dialog */
			dialog.dismiss();
			break;
		case AlertDialog.BUTTON_NEUTRAL:
			/* Instantiate FileChooser intent, then set filter to apk */
			Intent i = new Intent(this, FileChooser.class);
			i.putExtra(C.FILETYPE, C.APK);
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
		case C.PICT_REQUEST: /* Used predefined int value for 1 */
			if (resultCode == RESULT_OK) {
				Bundle extras = data.getExtras();
				if (null != extras.getString("result")) {
					pictUri = extras.getString("result");
					Bitmap b = BitmapFactory.decodeFile(pictUri);
					btn_image.setImageBitmap(b);
				}
			}
			break;
		case C.APK_REQUEST: /* 2 */
			if (resultCode == RESULT_OK) {
				Bundle extras = data.getExtras();
				if (null != extras.getString("result")) {
					apkUri = extras.getString("result");
					/* Get package manager service, package manager info service */
					PackageManager pm = getPackageManager();
					PackageInfo pi = pm.getPackageArchiveInfo(apkUri, 0);
					/* Workaround to read non-installed apk */
					pi.applicationInfo.sourceDir = apkUri;
					pi.applicationInfo.publicSourceDir = apkUri;
					/*
					 * Load icon from apk file, then set it as btn_download
					 * image source
					 */
					pi.applicationInfo.loadIcon(pm);
					btn_download.setImageDrawable(pi.applicationInfo
							.loadIcon(pm));
					/*
					 * Read apk label(see: AndroidManifest.xml in the
					 * <Application> tag) then set it as txt_title text
					 */
					txt_title.setText(pi.applicationInfo.loadLabel(pm));
					/*
					 * Read apk description (which is rarely being set, so check
					 * if it exist first then set as txt_desc text
					 */
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
		/*
		 * Fetch data from web server and set the value to local edit text see:
		 * ProcessingTask case:FETCH
		 */
		new ProcessingTask().execute(FETCH);
		/* Download image from web server */
		new DownloadImageTask(btn_image).execute(C.URL + skinId + "/picture");
	}

	/**
	 * Instantiate new Progress Dialog
	 * 
	 * @param message
	 *            Progress Dialog message to show
	 */
	private void setProgressDialog(String message) {
		pDialog = new ProgressDialog(SingleSkinView.this);
		pDialog.setMessage(message);
		pDialog.setCancelable(false);
		pDialog.setIndeterminate(false);
		pDialog.show();
	}

	/**
	 * Asynchronously get, delete, put data to web server
	 * 
	 * @author adrianbabame
	 * 
	 */
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
			/*
			 * type[0] because we only passing 1 data and it should be at the
			 * first index (0)
			 */
			switch (type[0]) {
			case FETCH:
				/* Instantiate new data */
				data = new HashMap<String, String>();
				/* Make http request */
				try {
					JSONObject fetch = jParser.makeHttpRequest(C.URL + id,
							"GET", null);
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
				} catch (IOException e) {
					e.printStackTrace();
				}
			case DELETE:
				try {
					JSONObject delete = jParser.makeHttpRequest(C.URL + "skin/"
							+ id, "DELETE", null);
					int success = delete.getInt("success");
					result.put("success", success);
					return result;
				} catch (JSONException e) {
					e.printStackTrace();
					result.put("success", 0);
					return result;
				} catch (IOException e) {
					e.printStackTrace();
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
					params.put("imei", imei);
					params.put("title", title);
					params.put("author", author);
					params.put("description", desc);
					JSONObject update = jParser.makeHttpRequest(C.URL + "skin/"
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
			/* Must be at last line */
			if (pDialog.isShowing())
				pDialog.dismiss();
		}
	}

	private class DownloadImageTask extends AsyncTask<String, Void, Bitmap> {
		ImageView bmImage;

		/**
		 * Constructor We don't need it anyway, because we can interact with
		 * parents UI in onPostExecute function...But I just copying this from
		 * random citizen of the browserland and to lazy to edit it. Credits
		 * goes to original author as always :)
		 * 
		 * @param bmImage
		 *            ImageView that we used to change its image source
		 */
		public DownloadImageTask(ImageView bmImage) {
			this.bmImage = bmImage;
		}

		@Override
		protected Bitmap doInBackground(String... urls) {
			String urldisplay = urls[0];
			Bitmap mIcon11 = null;
			try {
				InputStream in = new java.net.URL(urldisplay).openStream();
				mIcon11 = BitmapFactory.decodeStream(in);
			} catch (Exception e) {
				e.printStackTrace();
			}
			return mIcon11;
		}

		@Override
		protected void onPostExecute(Bitmap result) {
			bmImage.setImageBitmap(result);
		}
	}
}
