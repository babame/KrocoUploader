package com.beam.krocouploader.chooser;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Stack;

import android.app.ListActivity;
import android.content.Intent;
import android.os.Bundle;
import android.os.Environment;
import android.view.View;
import android.widget.ListView;

import com.beam.krocouploader.R;
import com.beam.krocouploader.utils.C;

public class FileChooser extends ListActivity {
	private File currentDir;
	private FileArrayAdapter adapter;
	
	private Stack<File> dirStack = new Stack<File>();
	
	private String[] filetype = null;
	
	private Intent resultIntent;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		Bundle extras = getIntent().getExtras();
		if (null != extras) {
			filetype = extras.getStringArray(C.FILETYPE);
		}
		currentDir = new File(Environment.getExternalStorageDirectory()
				.getAbsolutePath());
		fill(currentDir);
	}

	private void fill(File f) {
		File[] dirs = f.listFiles();
		this.setTitle("Current Dir: " + f.getName());
		List<Option> dir = new ArrayList<Option>();
		List<Option> files = new ArrayList<Option>();
		try {
			for (File ff : dirs) {
				if (ff.isDirectory() && !ff.getName().startsWith("."))
					dir.add(new Option(ff.getName(), "Folder", ff
							.getAbsolutePath()));
				else {
					if (null != filetype) {
						for (String type : filetype) {
							if (ff.getName().endsWith("." + type)) {
								files.add(new Option(ff.getName(), "File", ff
										.getAbsolutePath()));
							}
						}
					} else {
						files.add(new Option(ff.getName(), "File", ff
								.getAbsolutePath()));
					}
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		Collections.sort(dir);
		Collections.sort(files);
		dir.addAll(files);
		if (!f.getName().equalsIgnoreCase("sdcard"))
			dir.add(0, new Option("..", "parent directory", f.getParent()));

		adapter = new FileArrayAdapter(FileChooser.this, R.layout.file_view,
				dir);
		this.setListAdapter(adapter);
	}

	@Override
	protected void onListItemClick(ListView l, View v, int position, long id) {
		super.onListItemClick(l, v, position, id);
		Option o = adapter.getItem(position);
		if (o.getData().equalsIgnoreCase("folder")) {
			dirStack.push(currentDir);
			currentDir = new File(o.getPath());
			fill(currentDir);
		} else if (o.getData().equalsIgnoreCase("parent directory")) {
			currentDir = dirStack.pop();
			fill(currentDir);
		} else {
			onFileClick(o);
		}
	}

	private void onFileClick(Option o) {
		resultIntent = new Intent();
		resultIntent.putExtra("result", o.getPath());
		setResult(RESULT_OK, resultIntent);
		finish();
	}

	@Override
	public void onBackPressed() {
		if (dirStack.empty()) {
			resultIntent = new Intent();
			setResult(RESULT_CANCELED);
			finish();
			return;
		}
		currentDir = dirStack.pop();
		fill(currentDir);
	}
}
