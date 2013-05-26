package yak.turtle;

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.HashMap;

import yak.turtle.Views.AListView;
import yak.turtle.Views.AListViewOfActivityUris;
import yak.turtle.Views.ATextView;
import yak.turtle.Views.AnEditView;
import yak.turtle.Views.DrawView;
import android.net.Uri;
import android.os.Bundle;
import android.app.Activity;
import android.app.LauncherActivity;
import android.content.Intent;
import android.util.Log;
import android.view.Menu;

public class TurtleActivity extends Activity {
	static HashMap<String, String> boxes = new HashMap<String, String>();

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		// setContentView(R.layout.activity_turtle);

		Intent intent = getIntent();
		Uri uri = intent.getData();
		Bundle extras = intent.getExtras();
		String pathStr = uri == null ? "/" : uri.getPath();
		String queryStr = uri == null ? "" : uri.getQuery();

		String[] path = pathStr.substring(1).split("/");
		HashMap<String, String> query = parseQueryStr(queryStr);

		dispatch(path, query);

	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.turtle, menu);
		return true;
	}

	HashMap<String, String> parseQueryStr(String queryStr) {
		HashMap<String, String> z = new HashMap<String, String>();
		queryStr = queryStr == null ? "" : queryStr; // Don't be null.

		String[] parts = queryStr.split("&");
		for (String part : parts) {
			String[] kv = part.split("=", 2);
			if (kv.length == 2)
				try {
					z.put(kv[0], URLDecoder.decode(kv[1], "UTF-8"));
				} catch (UnsupportedEncodingException e) {
					e.printStackTrace();
					throw new RuntimeException(e);
				}
		}
		return z;
	}

	void dispatch(String[] path, HashMap<String, String> query) {
		Log.v("yak", "DISPATCH {" + Show(path) + "}" + path.length);
		if (path.length == 0 || path[0].length() == 0) {
			doRoot();
		} else if (path.length == 1 && query.size() == 0) {
			doBox(path[0]);
		} else if (path.length == 2 && query.size() == 0
				&& path[1].equals("RUN")) {
			doRun(path[0]);
		} else {
			String text = "PATH " + path + " QUERY " + query;
			ATextView tv = new ATextView(this, text);
			setContentView(tv);
		}
	}

	void doRoot() {
		String[] labels = { "/A", "/B", "/C", "/D", "/E", "/F", "/G" };
		AListView lv = new AListViewOfActivityUris(this, labels,
				TurtleActivity.class);
		setContentView(lv);
	}

	void doBox(final String box) {
		String text = boxes.get(box);
		if (text == null) {
			text = "?" + box;
		}
		AnEditView e = new AnEditView(this, text) {
			@Override
			void onSave(String newText) {
				boxes.put(box, newText);
				Views.launchActivity(TurtleActivity.this, TurtleActivity.class,
						"/" + box + "/" + "RUN", "");
			}
		};
		setContentView(e);
	}

	void doRun(final String box) {
		final String text = boxes.get(box);
		ArrayList<Float> v = new ArrayList<Float>();
		// for (int i = 0; i < 30; i++) {
		// v.add((float)(i*4));
		// v.add((float)(i*8));
		// v.add((float)(100 - i*3));
		// v.add((float)(100 - i*7));
		// }
		v = runLogo(parseLogo(text));
		DrawView dv = new DrawView(this, v);
		setContentView(dv);
	}

	ArrayList<Float> runLogo(String[] words) {
		final int n = words.length;
		ArrayList<Float> v = new ArrayList<Float>();
		double x = 50; // turtle position
		double y = 50;
		double a = 0; // angle
		for (int i = 0; i < n; i++) {
			String w = words[i];
			x = (x + 1000000) % 100.0;
			y = (y + 1000000) % 100.0;
			if (w.equals("f")) { // Forward
				double d = parseDouble(words[i + 1]);
				++i;
				double xx = x + d * Math.cos(a / 180.0 * Math.PI);
				double yy = y + d * Math.sin(a / 180.0 * Math.PI);
				v.add((float) x);
				v.add((float) y);
				v.add((float) xx);
				v.add((float) yy);
				x = xx;
				y = yy;
			} else if (w.equals("l")) { // Left
				double d = parseDouble(words[i + 1]);
				++i;
				a -= d;
			} else if (w.equals("r")) { // right
				double d = parseDouble(words[i + 1]);
				++i;
				a += d;
			} else {
				throw new RuntimeException("Unknown Command: {" + w + "}");
			}
		}
		return v;
	}

	double parseDouble(String s) {
		if (s.length() == 1 && 'a' <= s.charAt(0) && s.charAt(0) <= 'z') {
			// Shortcut 'a' thru 'z' are 1 thru 26.
			return s.charAt(0) - 'a' + 1;
		}
		return Double.parseDouble(s);
	}

	String[] parseLogo(String a) {
		return removeEmptyStrings(a.split("[\\t\\n\\r ]+"));
	}

	String[] removeEmptyStrings(String[] a) {
		final int n = a.length;
		int c = 0;
		for (int i = 0; i < n; i++) {
			if (a[i].length() > 0) {
				c++;
			}
		}
		String[] z = new String[c];
		int j = 0;
		for (int i = 0; i < n; i++) {
			if (a[i].length() > 0) {
				z[j] = a[i];
				++j;
			}
		}
		return z;
	}

	public static String Show(String[] ss) {
		StringBuffer sb = new StringBuffer();
		sb.append("{arr ");
		for (int i = 0; i < ss.length; i++) {
			sb.append("[" + i + "]= " + ss[i] + " ");
		}
		sb.append("}");
		return sb.toString();
	}
}
