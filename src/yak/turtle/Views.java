package yak.turtle;

import java.net.URI;
import java.util.ArrayList;

import android.annotation.TargetApi;
import android.content.Context;
import android.content.Intent;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.net.Uri;
import android.os.Build;
import android.text.InputType;
import android.util.Log;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.LinearLayout.LayoutParams;

public final class Views {

	public static abstract class AListView extends ListView {
		protected Context theContext;

		String[] labels;

		public AListView(Context cx, final String[] labels) {
			super(cx);
			this.theContext = cx;
			this.labels = labels;
			Log.i("AListView", "=== CTOR");

			this.setAdapter(new ArrayAdapter<String>(theContext,
					R.layout.list_item, labels));

			this.setLayoutParams(FILL);
			this.setTextFilterEnabled(true);

			this.setOnItemClickListener(new OnItemClickListener() {

				@Override
				public void onItemClick(AdapterView<?> arg0, View arg1,
						int index, long arg3) {
					final String label = labels[index];
					handleItemClick(label);
				}

			});
		}

		public abstract void handleItemClick(String label);
	}

	public static class AListViewOfActivityUris extends AListView {
		Class theClass;

		public AListViewOfActivityUris(Context cx, String[] labels,
				Class theClass) {
			super(cx, labels);
			this.theClass = theClass;
		}

		@Override
		public void handleItemClick(String label) {
			// Only first word before " " is the URI.
			String[] words = label.split(" ");
			launchActivity(theContext, theClass, words[0], "");
		}

	}

	public static abstract class AWebView extends WebView {
		private Context theContext;

		@TargetApi(Build.VERSION_CODES.ECLAIR_MR1)
		public AWebView(Context cx, String html) {
			super(cx);
			this.theContext = cx;
			Log.i("AWebView", "=== CTOR");

			this.loadDataWithBaseURL("terse://terse", html, "text/html",
					"UTF-8", null);

			// this.setWebChromeClient(new WebChromeClient());
			this.getSettings().setBuiltInZoomControls(true);
			// this.getSettings().setJavaScriptEnabled(true);
			this.getSettings().setDefaultFontSize(18);
			this.getSettings().setNeedInitialFocus(true);
			this.getSettings().setSupportZoom(true);
			this.getSettings().setSaveFormData(true);

			this.setWebViewClient(new WebViewClient() {
				@Override
				public boolean shouldOverrideUrlLoading(WebView view, String url) {
					return onClickLink(url);
				}
			});
		}

		protected boolean onClickLink(String url) {
			URI uri = URI.create("" + url);
			String path = uri.getPath();
			String query = uri.getQuery();

			handleClick(path, query);

			return true;
		}

		protected abstract void handleClick(String path, String query);
	}

	public static class ATextView extends TextView {
		private Context theContext;

		public ATextView(Context cx, String text) {
			super(cx);
			this.theContext = cx;

			this.setText(text);
			this.setBackgroundColor(Color.BLACK);
			this.setTextSize(TypedValue.COMPLEX_UNIT_SP, 24);
			this.setTextColor(Color.YELLOW);
			this.setVerticalScrollBarEnabled(true);
		}
	}

	public static class AVerticalView extends LinearLayout {
		private Context theContext;

		public AVerticalView(Context cx) {
			super(cx);
			this.theContext = cx;
			Log.i("VerticalView", theContext.toString() + "=== CTOR");
			this.setOrientation(LinearLayout.VERTICAL);
		}

		@Override
		public void addView(View view) {
			Log.i("VerticalView", theContext.toString() + "=== addView: "
					+ view);
			super.addView(view);
		}
	}

	public abstract static class AnEditView extends LinearLayout {

		final private Context theContext;

		final Button btn;
		final EditText ed;
		String text;

		AnEditView(Context cx, String text, String buttonText) {
			super(cx);
			this.theContext = cx;
			this.text = text;

			ed = new EditText(theContext);

			ed.setText(text);

			ed.setInputType(InputType.TYPE_CLASS_TEXT
					| InputType.TYPE_TEXT_FLAG_MULTI_LINE
					| InputType.TYPE_TEXT_VARIATION_LONG_MESSAGE
					| InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS);
			ed.setLayoutParams(FILL_1);
			// ed.setTextSize(TypedValue.COMPLEX_UNIT_SP, 22);
			ed.setTextAppearance(theContext, R.style.teletype);
			ed.setBackgroundColor(Color.BLACK);
			ed.setGravity(Gravity.TOP);
			ed.setScrollBarStyle(View.SCROLLBARS_OUTSIDE_INSET);
			ed.setVerticalFadingEdgeEnabled(true);
			ed.setVerticalScrollBarEnabled(true);
			ed.setOnKeyListener(new OnKeyListener() {
				public boolean onKey(View v, int keyCode, KeyEvent event) {
					return false;
				}
			});

			btn = new Button(theContext);
			btn.setText(buttonText);
			btn.setOnClickListener(new OnClickListener() {
				public void onClick(View v) {
					onSave(ed.getText().toString());
				}
			});

			this.setOrientation(LinearLayout.VERTICAL);
			this.addView(btn);
			this.addView(ed);
		}

		abstract void onSave(String text);
	}

	public static class DrawView extends View {
		ArrayList<Float> lines;

		public DrawView(Context context, ArrayList<Float> lines) {
			super(context);
			this.lines = lines;
		}

		@Override
		protected void onDraw(Canvas canvas) {
			Paint black = new Paint();
			black.setColor(Color.BLACK);
			canvas.drawPaint(black);

			Paint green = new Paint();
			green.setColor(Color.GREEN);
			green.setStrokeWidth(4);

			int w = canvas.getWidth();
			int h = canvas.getHeight();
			int m = (w < h) ? w : h;
			float ww = m / 100.0f;
			float hh = m / 100.0f;
//			float ww = w / 100.0f;
//			float hh = h / 100.0f;

			final int n = lines.size() / 4;
			for (int i = 0; i < n; i++) {
				float x1 = ww * lines.get(4 * i + 0);
				float y1 = hh * lines.get(4 * i + 1);
				float x2 = ww * lines.get(4 * i + 2);
				float y2 = hh * lines.get(4 * i + 3);
				canvas.drawLine(x1, y1, x2, y2, green);
			}
		}
	}

	public static LayoutParams FILL = new LayoutParams(
			LayoutParams.FILL_PARENT, LayoutParams.FILL_PARENT);

	public static LayoutParams FILL_1 = new LayoutParams(
			ViewGroup.LayoutParams.FILL_PARENT,
			ViewGroup.LayoutParams.FILL_PARENT, 1.0f);

	public static void launchActivity(Context theContext, Class theClass,
			String path, String query) {
		Log.v("yak", "LAUNCHING {" + path + "}");
		Uri uri = new Uri.Builder().scheme("yak").path(path).query(query)
				.build();
		Intent intent = new Intent("android.intent.action.MAIN", uri);
		intent.setClass(theContext.getApplicationContext(), theClass);
		theContext.startActivity(intent);
	}
}
