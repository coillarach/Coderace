package net.coderace;

import android.annotation.TargetApi;
import android.app.Activity;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.webkit.WebView;

public class HelpActivity extends Activity{

    @TargetApi(Build.VERSION_CODES.HONEYCOMB) 
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Log.d("HELP","Creating...");
		if (Build.VERSION.SDK_INT > 10)
			getActionBar().hide();
        setContentView(R.layout.activity_help); 

        WebView helpView = (WebView) findViewById(R.id.helpView);
        String url = getString(R.string.server_url) + "help.html";
        helpView.loadUrl(url);
        
        Log.d("HELP","Adding listener");
		findViewById(R.id.exit_help_button).setOnClickListener(
				new View.OnClickListener() {
					@Override
					public void onClick(View view) {
						finish();
					}
		});
    }
}
