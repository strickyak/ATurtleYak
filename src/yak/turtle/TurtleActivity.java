package yak.turtle;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.PrintStream;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;

import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;

import yak.turtle.Views.AListView;
import yak.turtle.Views.AListViewOfActivityUris;
import yak.turtle.Views.ATextView;
import yak.turtle.Views.AnEditView;
import yak.turtle.Views.DrawView;
import yak.turtle.Views.DrawView.AKeyboard;
import android.net.Uri;
import android.os.Bundle;
import android.app.Activity;
import android.app.LauncherActivity;
import android.content.Context;
import android.content.Intent;
import android.content.res.Resources.Theme;
import android.graphics.Color;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.Toast;

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

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		// Handle item selection
		switch (item.getItemId()) {
		case R.id.top_menu:
			Views.launchActivity(TurtleActivity.this, TurtleActivity.class,
					"/", "");
			return true;
		default:
			return super.onOptionsItemSelected(item);
		}
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
		try {
			Log.v("yak", "DISPATCH {" + Show(path) + "}" + path.length);
			if (path.length == 0 || path[0].length() == 0) {
				doRoot();
			} else if (path.length >= 1) {
				doCommand(path, query);
			} else {
				Barf("How did we get here");
			}
		} catch (Throwable e) {
			e.printStackTrace();
			final StringBuilder sb = new StringBuilder();
			OutputStream out = new OutputStream() {
				@Override
				public void write(int arg0) throws IOException {
					sb.append((char) arg0);
				}
			};
			PrintStream ps = new PrintStream(out);
			e.printStackTrace(ps);
			String s = /* e.toString() + "\n" + e.getMessage() + "\n" + */sb
					.toString();
			ATextView tv = new ATextView(this, s);
			tv.setTextColor(Color.CYAN);
			SetContentViewWithHomeButtonAndScroll(tv);
		}
	}

	void doRoot() {
		String[] commands = { "/push to web", "/save to file",
				"/pull from web", "/load from file", "/clear all", "/HELP",
				"/test_keyboard" };
		String[] labels = new String[3 * 26 + commands.length];
		for (int i = 0; i < commands.length; i++) {
			labels[i] = commands[i];
		}
		for (int i = 0; i < 26; i++) {
			String k = "" + (char) ('A' + i);
			int j = commands.length + i;
			String v = boxes.get(k);
			labels[j] = "/box/" + k + " == "
					+ TranslateControlsToSpaces(v == null ? "" : v);
		}
		for (int i = 0; i < 26; i++) {
			String k = "" + (char) ('A' + i) + (char) ('A' + i);
			int j = commands.length + i + 26;
			String v = boxes.get(k);
			labels[j] = "/box/" + k + " == "
					+ TranslateControlsToSpaces(v == null ? "" : v);
		}
		for (int i = 0; i < 26; i++) {
			String k = "" + (char) ('A' + i) + (char) ('A' + i)
					+ (char) ('A' + i);
			int j = commands.length + i + 26 + 26;
			String v = boxes.get(k);
			labels[j] = "/box/" + k + " == "
					+ TranslateControlsToSpaces(v == null ? "" : v);
		}
		AListView lv = new AListViewOfActivityUris(this, labels,
				TurtleActivity.class);
		setContentView(lv);
	}

	void doBox(final String box) {
		String text = boxes.get(box);
		if (text == null) {
			text = "";
		}
		AnEditView e = new AnEditView(this, text, "Save and Run") {
			@Override
			void onSave(String newText) {
				boxes.put(box, newText);
				Log.v("onSave", Fmt("[%s] <- %s", box, CurlyEncode(newText)));
				Views.launchActivity(TurtleActivity.this, TurtleActivity.class,
						"/run/" + box, "");
			}
		};
		setContentView(e);
	}

	void doCommand(String[] path, HashMap<String, String> query) {
		String message = "? replace this message !";

		if (path[0].equals("run")) {
			doRun(path[1]);
			return;

		} else if (path[0].equals("test_keyboard")) {
			doTestKeyboard();
			return;

		} else if (path[0].equals("box")) {
			doBox(path[1]);
			return;

		} else if (path[0].equals("push")) {
			long now = System.currentTimeMillis() / 1000;
			String filename = Fmt("logo_%d", now);
			pushToWeb(filename, pickle());
			message = Fmt("Pushed to %s", filename);

		} else if (path[0].equals("save")) {
			long now = System.currentTimeMillis() / 1000;
			String filename = Fmt("logo_%d", now);
			writeFile(filename, pickle());
			message = Fmt("Saved to %s", filename);

		} else if (path[0].equals("pull")) {
			if (path.length == 1) {

				ArrayList<String> list = listOfWebFiles();
				ArrayList<String> members = new ArrayList<String>();
				for (String s : list) {
					String[] words = s.split(" ");
					String w0 = words[0];
					if (w0.charAt(0) == 'l' && w0.endsWith(".txt")) {
						String member = Fmt("%s  len=%s",
								w0.substring(0, w0.length() - 4),
								(words.length > 2 ? words[2] : "?"));
						members.add(w0.substring(0, w0.length() - 4)); // remove
																		// .txt
					}
				}
				String[] labels = new String[members.size()];
				for (int i = 0; i < labels.length; i++) {
					labels[i] = "/pull/" + members.get(i);
				}
				ReverseSort(labels);
				AListView lv = new AListViewOfActivityUris(this, labels,
						TurtleActivity.class);
				setContentView(lv);
				return;
			} else {
				String guts = pullFromWeb(path[1]);
				unpickle(guts);
				message = Fmt("Pulled from web %s\n-----\n%s", path[1], guts);
			}

		} else if (path[0].equals("clear")) {
			for (String s : boxes.keySet()) {
				boxes.put(s, "");
			}
			message = "All Cleared.";

		} else if (path[0].equals("load")) {
			if (path.length == 1) {
				File dir = getFilesDir();
				ArrayList<String> members = new ArrayList<String>();
				for (File f : dir.listFiles()) {
					String name = f.getName();
					if (name.charAt(0) != 'l') {
						break;
					}
					members.add(name);
				}
				String[] labels = new String[members.size()];
				for (int i = 0; i < labels.length; i++) {
					labels[i] = "/load/" + members.get(i);
				}
				ReverseSort(labels);
				AListView lv = new AListViewOfActivityUris(this, labels,
						TurtleActivity.class);
				setContentView(lv);
				return;

			} else {
				String filename = path[1];
				String guts = readFile(filename);
				unpickle(guts);
				message = Fmt("Loaded from file %s\n-----\n%s", path[1], guts);
			}

		} else if (path[0].equals("HELP")) {
			// @formatter:off
			message = "This is a Logo-like language with Turtle Graphics.\n"
					+ "(Use Google & Wikipedia.)\n"
					+ "\n"
					+ "Code is stored in named boxes (A, B, C, ... AA, BB, ...), "
					+ "which can be run or called as subroutines from other code.\n"
					+ "\n" + "f N = move Forward distance N.\n"
					+ "l N = turn Left N degrees.\n"
					+ "r N = turn Right N degrees.\n"
					+ "u = pen Up (stop drawing).\n"
					+ "d = pen Down (draw again).\n" + "c N = use Color N\n"
					+ "     900 = red\n" + "     90 = green\n"
					+ "     9 = blue\n" + "     0 = black\n"
					+ "     999 = white.\n"
					+ "( commands... ) N = repeat commands N times.\n"
					+ "/x = call box /X as a subroutine\n"
					+ "; = end commands and begin comment thru EOF.\n" + "\n"
					+ "There are no variables, no conditionals, and "
					+ "so recursion is a bad idea, since you can't stop it.\n"
					+ "\n" + "You may Save and Load from private files.\n"
					+ "You may Push and Pull from public web files.\n" + "";
			// @formatter:on

		} else {
			message = Fmt("Bad command: %s", path[0]);
		}
		// If you did not return, we display message.
		SetContentViewWithHomeButtonAndScroll(new ATextView(this, message));
	}

	void doRun(final String box) {
		final String text = boxes.get(box);
		String[] main = splitLogoWords(text);
		ArrayList<Float> v = new LogoMachine(main).runLogo();
		DrawView dv = new DrawView(this, v);
		setContentView(dv);
	}

	class LogoMachine {
		double color = 999;
		double x = 50; // turtle position
		double y = 50;
		double a = 0; // angle
		boolean down = true;
		final ArrayList<Float> v = new ArrayList<Float>();
		final ArrayList<Float> loopVars = new ArrayList<Float>();
		Block main;

		LogoMachine(String[] words) {
			this.main = new Block(words);
		}

		ArrayList<Float> runLogo() {
			this.main.runLogo();
			return v;
		}

		class Block {
			final String[] words;
			int i;
			final int n;
			String w;

			Block(String[] words) {
				this.words = words;
				this.n = words.length;
			}

			void runLogo() {
				Log.v("runLogo", Show(words));
				for (i = 0; i < n; i++) {
					w = words[i];
					if (w.charAt(0) == 'f') { // Forward
						if (w.length() > 1) {
							w = w.substring(1); // allow omit space
						} else {
							++i;
							w = (i < n) ? words[i] : "<EOF>";
						}
						if (i < n) {
							double d = parseExpr();
							double xx = x + d * Math.cos(a / 180.0 * Math.PI);
							double yy = y + d * Math.sin(a / 180.0 * Math.PI);
							if (down) {
								v.add((float) x);
								v.add((float) y);
								v.add((float) xx);
								v.add((float) yy);
								v.add((float) color);
								// Log.v("COLOR", ""+color);
							}
							x = xx;
							y = yy;
						}
					} else if (w.charAt(0) == 'l') { // Left
						if (w.length() > 1) {
							w = w.substring(1); // allow omit space
						} else {
							++i;
							w = (i < n) ? words[i] : "<EOF>";
						}
						if (i < n) {
							double d = parseExpr();
							a -= d;
						}
					} else if (w.charAt(0) == 'r') { // right
						if (w.length() > 1) {
							w = w.substring(1); // allow omit space
						} else {
							++i;
							w = (i < n) ? words[i] : "<EOF>";
						}
						if (i < n) {
							double d = parseExpr();
							a += d;
						}
					} else if (w.charAt(0) == 'c') { // color
						if (w.length() > 1) {
							w = w.substring(1); // allow omit space
						} else {
							++i;
							w = (i < n) ? words[i] : "<EOF>";
						}
						if (i < n) {
							double d = parseExpr();
							color = ((int) d % 1000);
						}
					} else if (w.equals("u")) { // pen up
						down = false;
					} else if (w.equals("d")) { // pen down
						down = true;
					} else if (w.charAt(0) == '/') { // subroutine
						String key = w.substring(1).toUpperCase();
						String code = boxes.get(key);
						if (code != null) {
							new Block(splitLogoWords(code)).runLogo();
						}
					} else if (w.equals("!") || w.equals("(")) { // block
						++i;
						w = (i < n) ? words[i] : "<EOF>";
						int level = 0;
						ArrayList<String> t = new ArrayList<String>();

						while (i < n
								&& (level > 0 || !(w.equals("?") || w
										.equals(")")))) {
							t.add(w);
							if (w.equals("!") || w.equals("(")) {
								level++;
							}
							if (w.equals("?") || w.equals(")")) {
								level--;
							}
							++i;
							w = (i < n) ? words[i] : "<EOF>";
						}
						i++; // Advance past '?' or ')'
						w = (i < n) ? words[i] : "<EOF>";

						Log.v("endBlock", (i < n) ? words[i] : "<EOF>");

						// Copy into array.
						String[] blockWords = new String[t.size()];
						for (int j = 0; j < blockWords.length; j++) {
							blockWords[j] = t.get(j);
						}
						if (i < n) {
							double d = parseExpr();
							// Add a new final slot to loopVars, for the
							// upcoming loop.
							final int last = loopVars.size();
							loopVars.add(0.0f);
							try {
								for (int k = 0; k < d - 0.5; k++) {
									loopVars.set(last, (float) k);
									new Block(blockWords).runLogo();
								}
							} finally {
								loopVars.remove(last);
							}
						}
					} else {
						throw new RuntimeException("Unknown Command: {" + w
								+ "}");
					}
				}
			}
			
			double parseExpr() {
				double x = parseDouble();
				while (true) {
					// Don't just check that the next i is valid,
					// but also the one after that.
					String peek = (i + 2 < n) ? words[i + 1] : "";
					if (false) {
						continue;
					} else if (peek.equals("!=")) {
						i += 2;
						w = words[i];
						x = x != parseDouble() ? 1.0 : 0.0;
					} else if (peek.equals("<")) {
						i += 2;
						w = words[i];
						x = x < parseDouble() ? 1.0 : 0.0;
					} else if (peek.equals("<=")) {
						i += 2;
						w = words[i];
						x = x <= parseDouble() ? 1.0 : 0.0;
					} else if (peek.equals("==")) {
						i += 2;
						w = words[i];
						x = x == parseDouble() ? 1.0 : 0.0;
					} else if (peek.equals(">")) {
						i += 2;
						w = words[i];
						x = x > parseDouble() ? 1.0 : 0.0;
					} else if (peek.equals(">=")) {
						i += 2;
						w = words[i];
						x = x >= parseDouble() ? 1.0 : 0.0;
					} else if (peek.equals("+")) {
						i += 2;
						w = words[i];
						x = x + parseDouble();
					} else if (peek.equals("-")) {
						i += 2;
						w = words[i];
						x = x - parseDouble();
					} else if (peek.equals("*")) {
						i += 2;
						w = words[i];
						x = x * parseDouble();
					} else if (peek.equals("/")) {
						i += 2;
						w = words[i];
						double d = parseDouble();
						x = (d == 0) ? 0.0 : x / parseDouble();
					} else if (peek.equals("%")) {
						i += 2;
						w = words[i];
						double d = parseDouble();
						x = (d == 0) ? 0.0 : x % parseDouble();
					} else {
						return x;
					}
				}
			}
			
			double parseDouble() {
				final int s = loopVars.size();
				if (w.equals("i") && s > 0) {
					return loopVars.get(s - 1);
				}
				if (w.equals("j") && s > 1) {
					return loopVars.get(s - 2);
				}
				if (w.equals("k") && s > 2) {
					return loopVars.get(s - 3);
				}
				if (w.equals("l") && s > 3) {
					return loopVars.get(s - 4);
				}
				if (w.equals("m") && s > 4) {
					return loopVars.get(s - 5);
				}
				// Not an index variable, try number.
				final int n = w.length();
				double roman = 0;
				int level = 0;
				for (int i = n - 1; i >= 0; i--) {
					char c = w.charAt(i);
					switch (c) {
					case 'i':
						if (level > 1) {
							roman -= 1;
						} else {
							roman += 1;
							level = 1;
						}
						break;
					case 'v':
						if (level > 5) {
							roman -= 5;
						} else {
							roman += 5;
							level = 5;
						}
						break;
					case 'x':
						if (level > 10) {
							roman -= 10;
						} else {
							roman += 10;
							level = 10;
						}
						break;
					case 'l':
						if (level > 50) {
							roman -= 50;
						} else {
							roman += 50;
							level = 50;
						}
						break;
					case 'c':
						if (level > 100) {
							roman -= 100;
						} else {
							roman += 100;
							level = 100;
						}
						break;
					default:
						return Double.parseDouble(w);
					}
				}
				return roman;
			}

		}

	}

	String pickle() {
		StringBuilder sb = new StringBuilder();
		for (String k : boxes.keySet()) {
			String v = boxes.get(k).trim();
			if (v != null && v.length() > 0) {
				sb.append(Fmt("box %s %s\n", k, CurlyEncode(v)));
			}
		}
		return sb.toString();
	}

	void unpickle(String s) {
		String[] lines = s.split("\n");
		for (String line : lines) {
			line = line.trim();
			if (line.length() == 0) {
				continue;
			}
			String[] words = line.split(" ");
			if (words.length == 3 && words[0].equals("box")) {
				boxes.put(words[1], CurlyDecode(words[2]));
			}
		}
	}

	String[] splitLogoWords(String a) {
		a = a.split(";")[0]; // Comment is ';' thru EOF.
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

	public FileInputStream openFileRead(String filename)
			throws FileNotFoundException {
		return openFileInput(filename);
	}

	public FileOutputStream openFileWrite(String filename)
			throws FileNotFoundException {
		return openFileOutput(filename, Context.MODE_WORLD_READABLE
				| Context.MODE_WORLD_WRITEABLE);
	}

	public FileOutputStream openFileAppend(String filename)
			throws FileNotFoundException {
		return openFileOutput(filename, Context.MODE_WORLD_READABLE
				| Context.MODE_WORLD_WRITEABLE | Context.MODE_APPEND);
	}

	public String readFile(String filename) {
		StringBuilder sb = new StringBuilder();
		try {
			FileInputStream fis = openFileRead(filename);
			InputStreamReader isr = new InputStreamReader(fis);
			BufferedReader br = new BufferedReader(isr);
			while (true) {
				String line = br.readLine();
				if (line == null)
					break;
				sb.append(line);
				sb.append('\n');
			}
			fis.close();
			return sb.toString();
		} catch (IOException e) {
			throw Barf(e, "Cannot readFile:", filename);
		}
	}

	public void writeFile(String filename, String content) {
		StringBuilder sb = new StringBuilder();
		try {
			FileOutputStream fos = openFileWrite(filename);
			PrintStream ps = new PrintStream(fos);
			ps.print(content);
		} catch (IOException e) {
			throw Barf(e, "Cannot writeFile:", filename);
		}
	}

	public void appendFile(String filename, String content) {
		StringBuilder sb = new StringBuilder();
		try {
			FileOutputStream fos = openFileAppend(filename);
			PrintStream ps = new PrintStream(fos);
			ps.print(content);
		} catch (IOException e) {
			throw Barf(e, "Cannot appendFile:", filename);
		}
	}

	public void pushToWeb(String filename, String content) {
		DefaultHttpClient client = new DefaultHttpClient();
		HttpPost post = new HttpPost(Fmt("%s.push.%s", YAK_WEB_PAGE, filename));

		try {
			List<NameValuePair> nameValuePairs = new ArrayList<NameValuePair>();
			nameValuePairs.add(new BasicNameValuePair("content", content));
			post.setEntity(new UrlEncodedFormEntity(nameValuePairs));

			HttpResponse response = client.execute(post);
			BufferedReader rd = new BufferedReader(new InputStreamReader(
					response.getEntity().getContent()));
			String line = "";
			while ((line = rd.readLine()) != null) {
				line.length(); // Ignore it for now.
			}
		} catch (IOException e) {
			e.printStackTrace();
			throw Barf(e, "Error during pushToWeb", filename);
		}
	}

	public String pullFromWeb(String filename) {
		DefaultHttpClient client = new DefaultHttpClient();
		HttpGet post = new HttpGet(Fmt("%s.pull.%s", YAK_WEB_PAGE, filename));

		StringBuilder sb = new StringBuilder();
		try {
			HttpResponse response = client.execute(post);
			BufferedReader rd = new BufferedReader(new InputStreamReader(
					response.getEntity().getContent()));
			while (true) {
				String line = rd.readLine();
				if (line == null)
					break;
				sb.append(line);
				sb.append('\n');
			}
		} catch (IOException e) {
			e.printStackTrace();
			throw Barf(e, "Error during pullFromWeb", filename);
		}
		return sb.toString();
	}

	public ArrayList<String> listOfWebFiles() {
		DefaultHttpClient client = new DefaultHttpClient();
		HttpGet post = new HttpGet(Fmt("%s.dir", YAK_WEB_PAGE));

		ArrayList<String> z = new ArrayList<String>();
		try {
			HttpResponse response = client.execute(post);
			BufferedReader rd = new BufferedReader(new InputStreamReader(
					response.getEntity().getContent()));
			while (true) {
				String line = rd.readLine();
				if (line == null) {
					break;
				}
				if (line.startsWith("<li>")) {
					line = line.substring(4);
					if (line.startsWith("<tt>")) {
						line = line.substring(4);
					}
					if (line.endsWith("</tt>")) {
						line = line.substring(0, line.length() - 5);
					}
					// String[] words = line.split(" ");

					z.add(line.trim());
				}
			}
		} catch (IOException e) {
			e.printStackTrace();
			throw Barf(e, "Error during lostOfWebFiles");
		}
		return z;
	}

	public void doTestKeyboard() {
		AKeyboard k = new AKeyboard(this);
		k.addButton("dup", new Toaster("dup"));
		k.addButton("pop", new Toaster("pop"));
		k.addButton("swap", new Toaster("swap"));
		k.addButton("roll", new Toaster("roll"));
		k.addButton("***", new Toaster("***"));
		k.nextRow();
		k.addButton("dup", new Toaster("dup"));
		k.addButton("dup", new Toaster("dup"));
		k.addButton("dup", new Toaster("dup"));
		k.nextRow();
		k.addButton("dup", new Toaster("dup"));
		k.addButton("dup", new Toaster("dup"));
		k.addButton("dup", new Toaster("dup"));
		k.addButton("dup", new Toaster("dup"));
		k.nextRow();
		k.addButton("5", new Toaster("5"));
		k.addButton("6", new Toaster("6"));
		k.addButton("7", new Toaster("7"));
		k.addButton("8", new Toaster("8"));
		k.addButton("***", new Toaster("***"));
		k.nextRow();
		k.addButton("1", new Toaster("1"));
		k.addButton("2", new Toaster("2"));
		k.addButton("3", new Toaster("3"));
		k.addButton("4", new Toaster("4"));
		k.addButton("***", new Toaster("***"));
		setContentView(k);
	}

	public class Toaster implements Runnable {
		String text;

		public Toaster(String text) {
			this.text = text;
		}

		@Override
		public void run() {
			Toast.makeText(TurtleActivity.this, "Toast [" + text + "]",
					Toast.LENGTH_SHORT).show();
		}
	}

	public static String CurlyEncode(String s) {
		final int n = s.length();
		if (n == 0) {
			return "{}"; // Special Case.
		}
		StringBuffer sb = new StringBuffer();
		for (int i = 0; i < n; i++) {
			char c = s.charAt(i);
			if ('"' < c && c < '{') {
				sb.append(c);
			} else {
				sb.append("{" + (int) c + "}"); // {%d}
			}
		}
		return sb.toString();
	}

	public static String CurlyDecode(String s) {
		final int n = s.length();
		if (s.equals("{}")) {
			return ""; // Special Case.
		}
		StringBuffer sb = new StringBuffer();
		for (int i = 0; i < n; i++) {
			char c = s.charAt(i);
			if (c == '{') {
				i++;
				c = s.charAt(i);
				int x = 0;
				while ('0' <= c && c <= '9') {
					x = x * 10 + (c - '0');
					i++;
					c = s.charAt(i);
				}
				if (c != '}') {
					throw Barf("Bad closing curly: ", "" + (int) c);
				}
				sb.append((char) x);
			} else {
				sb.append(c);
			}
		}
		return sb.toString();
	}

	public static String TranslateControlsToSpaces(String s) {
		final int n = s.length();
		StringBuffer sb = new StringBuffer();
		for (int i = 0; i < n; i++) {
			char c = s.charAt(i);
			if (c < ' ') {
				sb.append(' ');
			} else {
				sb.append(c);
			}
		}
		return sb.toString();
	}

	void SetContentViewWithHomeButtonAndScroll(View v) {
		Button btn = new Button(this);
		btn.setText("[ TOP ]");
		// btn.setTextSize(15);
		// btn.setHeight(25);
		// btn.setMaxHeight(25);
		btn.setOnClickListener(new OnClickListener() {
			public void onClick(View v) {
				// Perform action on clicks
				Views.launchActivity(TurtleActivity.this, TurtleActivity.class,
						"/", "");
			};
		});

		LinearLayout linear = new LinearLayout(this);
		linear.setOrientation(LinearLayout.VERTICAL);
		linear.addView(btn);
		linear.addView(v);

		ScrollView scrollv = new ScrollView(this);
		scrollv.addView(linear);
		setContentView(scrollv);
	}

	protected static String YAK_WEB_PAGE = "http://wiki.yak.net/1017";

	public static RuntimeException Barf(Exception e, String... stuff) {
		throw new RuntimeException(e.toString() + " :: " + Show(stuff));
	}

	public static RuntimeException Barf(String s, String... stuff) {
		throw new RuntimeException(s + " :: " + Show(stuff));
	}

	public static String Fmt(String s, Object... args) {
		return String.format(s, args);
	}

	public static void ReverseSort(String[] a) {
		Arrays.sort(a, new Comparator<String>() {
			@Override
			public int compare(String lhs, String rhs) {
				return rhs.compareTo(lhs); // Reverse them.
			}
		});
	}
}
