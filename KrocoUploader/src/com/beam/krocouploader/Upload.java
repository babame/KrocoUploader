package com.beam.krocouploader;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

import org.apache.commons.io.FileUtils;
import org.json.JSONException;
import org.json.JSONObject;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Base64;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.Toast;

public class Upload extends Activity {
	private static final String URL = "http://ntahahase.zapto.org/Krocolizer/api.php/skin";
	private ProgressDialog pDialog;
	private EditText txt_title, txt_desc, txt_author;
	private ImageButton img_pict, img_apk;
	private Button btn_upload;
	private String pictUri, apkUri;
	private static final int PICT_REQUEST = 1;
	private static final int APK_REQUEST = 2;

	private JsonParser jParser = new JsonParser();

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_upload);

		txt_title = (EditText) findViewById(R.id.txt_skinTitle);
		txt_desc = (EditText) findViewById(R.id.txt_skinDesc);
		txt_author = (EditText) findViewById(R.id.txt_skinAuthor);
		img_pict = (ImageButton) findViewById(R.id.img_skinPict);
		img_pict.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				Intent i = new Intent(Upload.this, FileChooser.class);
				i.putExtra("filetype", new String[] { "png", "jpg", "bmp" });
				startActivityForResult(i, PICT_REQUEST);
			}
		});
		img_apk = (ImageButton) findViewById(R.id.img_skinApk);
		img_apk.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				Intent i = new Intent(Upload.this, FileChooser.class);
				i.putExtra("filetype", new String[] { "apk" });
				startActivityForResult(i, APK_REQUEST);
			}
		});
		btn_upload = (Button) findViewById(R.id.btn_skinUpload);
		btn_upload.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				if (null == pictUri)
					Toast.makeText(Upload.this, "No Picture selected",
							Toast.LENGTH_LONG).show();
				else
					new UploadSkin().execute();
			}
		});
	}

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		super.onActivityResult(requestCode, resultCode, data);
		switch (requestCode) {
		case PICT_REQUEST:
			if (resultCode == RESULT_OK) {
				Bundle extras = data.getExtras();
				if (null != extras.getString("result")) {
					pictUri = extras.getString("result");
					Bitmap b = BitmapFactory.decodeFile(pictUri);
					img_pict.setImageBitmap(b);
				}
			}
			break;
		case APK_REQUEST:
			if (resultCode == RESULT_OK) {
				Bundle extras = data.getExtras();
				if (null != extras.getString("result")) {
					apkUri = extras.getString("result");
					PackageManager pm = getPackageManager();
					PackageInfo pi = pm.getPackageArchiveInfo(apkUri, 0);

					pi.applicationInfo.sourceDir = apkUri;
					pi.applicationInfo.publicSourceDir = apkUri;

					pi.applicationInfo.loadIcon(pm);
					img_apk.setImageDrawable(pi.applicationInfo.loadIcon(pm));
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

	/**
	 * 
	 * @param f
	 *            File
	 * @return String md5 hash of File f
	 * @throws NoSuchAlgorithmException
	 *             MD5 instance not found?
	 * @throws IOException
	 *             more like file not found error
	 */
	private String generateMd5(File f) throws NoSuchAlgorithmException,
			IOException {
		MessageDigest md = MessageDigest.getInstance("MD5");
		FileInputStream fis = new FileInputStream(f);
		byte[] dataBytes = new byte[1024];
		int nread = 0;
		while ((nread = fis.read(dataBytes)) != -1) {
			md.update(dataBytes, 0, nread);
		}
		byte[] mdBytes = md.digest();
		StringBuffer sbMd5 = new StringBuffer("");
		for (int i = 0; i < mdBytes.length; i++) {
			sbMd5.append(Integer.toString((mdBytes[i] & 0xff) + 0x100, 16)
					.substring(1));
		}
		fis.close();
		return sbMd5.toString();
	}

	private class UploadSkin extends AsyncTask<String, String, Integer> {

		@Override
		protected void onPreExecute() {
			super.onPreExecute();
			pDialog = new ProgressDialog(Upload.this);
			pDialog.setMessage("Uploading. Please wait...");
			pDialog.setIndeterminate(false);
			pDialog.setCancelable(false);
			pDialog.show();
		}

		@Override
		protected Integer doInBackground(String... args) {
			String title = txt_title.getText().toString(), desc = txt_desc
					.getText().toString(), author = txt_author.getText()
					.toString();
			int success = 0;
			File filepict = null, fileApk = null;
			if (null != pictUri)
				filepict = new File(pictUri);
			if (null != apkUri)
				fileApk = new File(apkUri);
			try {
				byte[] pictByteArray = FileUtils.readFileToByteArray(filepict);
				byte[] apkByteArray = FileUtils.readFileToByteArray(fileApk);
				String pictEnc = Base64.encodeToString(pictByteArray, 0);
				String apkEnc = Base64.encodeToString(apkByteArray, 0);
				JSONObject params = new JSONObject();
				params.put("title", title);
				params.put("author", author);
				params.put("description", desc);
				params.put("img", pictEnc);
				params.put("apk", apkEnc);
				params.put("apk_name", fileApk.getName());
				JSONObject json = jParser.makeHttpRequest(URL, "POST", params);
				try {
					int result = json.getInt("success");
					String md5 = json.getString("md5");
					String localMd5 = generateMd5(fileApk);
					if (result == 1 && md5.equals(localMd5))
						success = 1;
					else if (result == 1 && !md5.equals(localMd5))
						success = 2;
					else
						success = 0;
				} catch (JSONException e) {
					e.printStackTrace();
				} catch (NoSuchAlgorithmException e) {
					e.printStackTrace();
				}
			} catch (IOException e) {
				e.printStackTrace();
			} catch (JSONException e1) {
				e1.printStackTrace();
			}
			return success;
		}

		@Override
		protected void onPostExecute(Integer result) {
			super.onPostExecute(result);
			if (pDialog.isShowing())
				pDialog.dismiss();
			if (result == 0)
				Toast.makeText(Upload.this, "Error while uploading occured!",
						Toast.LENGTH_LONG).show();
			else if (result == 2)
				Toast.makeText(Upload.this, "MD5 missmatch!", Toast.LENGTH_LONG)
						.show();
			else
				Toast.makeText(Upload.this, "Yeah...Upload Success",
						Toast.LENGTH_LONG).show();
			Upload.this.finish();
		}
	}
}
