package yak.turtle;

import java.net.URI;

import android.annotation.TargetApi;
import android.content.Context;
import android.graphics.Color;
import android.os.Build;
import android.util.Log;
import android.util.TypedValue;
import android.view.View;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.LinearLayout.LayoutParams;

public final class Views {
	public static abstract class AListView extends ListView {
		private Context theContext;

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
			Log.i("VerticalView", theContext.toString() + "=== addView: " + view);
			super.addView(view);
		}
	}

	public static LayoutParams FILL = new LayoutParams(LayoutParams.FILL_PARENT,
			LayoutParams.FILL_PARENT);
}
