package net.coderace;

import android.annotation.TargetApi;
import android.app.Activity;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.util.TypedValue;
import android.view.View;
import android.widget.ImageView;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TextView;

public class StatusActivity extends Activity {
	
	private int myTeam = 0;
	private TableLayout mTableView;

	@TargetApi(Build.VERSION_CODES.HONEYCOMB) @Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		if (Build.VERSION.SDK_INT > 10)
			getActionBar().hide();
		setContentView(R.layout.activity_status);

		// Recover data from intent
		Intent intent = getIntent();
		myTeam = intent.getIntExtra("t",0);
		int[]    teamId    = intent.getIntArrayExtra("ids");
		String[] teamName  = intent.getStringArrayExtra("names");
		int[]    teamScore = intent.getIntArrayExtra("scores");
		
		// Set up the status table
		mTableView = (TableLayout) findViewById(R.id.content);

		for(int i=0;i<teamId.length;i++) {

			TableRow newRow = new TableRow(this);
			
			ImageView marker = new ImageView(this);
			if (teamId[i] == myTeam)	// My team is blue
		        marker.setImageDrawable(getResources().getDrawable(R.drawable.blue_dot));
			else if (teamId[i] > 500)			// free and total are white
		        marker.setImageDrawable(null);
			else						// Everyone else is red
		        marker.setImageDrawable(getResources().getDrawable(R.drawable.red_dot));
			marker.setPadding(0, (int) (getResources().getDimension( R.dimen.mediumFont)/2), 0, 0);
			
			TextView label = new TextView(this);
			label.setText(teamName[i]);
			label.setLayoutParams(new TableRow.LayoutParams(1));
			label.setTextColor(getResources().getColor(R.color.dark_text));
			label.setTextSize(TypedValue.COMPLEX_UNIT_PX, getResources().getDimension(R.dimen.mediumFont));
			label.setPadding(4,0,(int)getResources().getDimension(R.dimen.mediumFont),0);

			TextView scoreField = new TextView(this);
			scoreField.setText(Integer.toString(teamScore[i]));
			scoreField.setTextColor(getResources().getColor(R.color.dark_text));
			scoreField.setTextSize(TypedValue.COMPLEX_UNIT_PX, getResources().getDimension(R.dimen.mediumFont));
			
			newRow.addView(marker);
			newRow.addView(label);
			newRow.addView(scoreField);
			
			mTableView.addView(newRow);
		}
		
		findViewById(R.id.status_button).setOnClickListener(
				new View.OnClickListener() {
					@Override
					public void onClick(View view) {
						finish();
					}
		});
	}
}
