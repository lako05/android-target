package net.cactii.target;

import android.app.Activity;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.os.Bundle;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.RadioGroup;
import android.widget.Toast;
import android.widget.CompoundButton.OnCheckedChangeListener;

public class NewGameActivity extends Activity {
  
  // Area for new game widgets
  private CheckBox newGameFromSMH;
  private RadioGroup newGameWordCount;
  private CheckBox newGameTimed;
  private Button newGameStart;
  
  /** Called when the activity is first created. */
  @Override
  public void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    // For now, a specific layout (fullscreen portrait)
    this.requestWindowFeature(Window.FEATURE_NO_TITLE);
    this.getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
        WindowManager.LayoutParams.FLAG_FULLSCREEN);
    setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
    setContentView(R.layout.newgame);
    this.newGameFromSMH = (CheckBox)findViewById(R.id.newGameFromSMH);
    this.newGameWordCount = (RadioGroup)findViewById(R.id.newGameWordCount);
    this.newGameTimed = (CheckBox)findViewById(R.id.newGameTimed);
    this.newGameStart = (Button)findViewById(R.id.newGameStart);
    this.newGameFromSMH.setOnCheckedChangeListener(new OnCheckedChangeListener() {
      @Override
      public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
        newGameWordCount.setClickable(!isChecked);
      }
    });
    this.newGameStart.setOnClickListener(new OnClickListener() {
      @Override
      public void onClick(View v) {
        if (newGameWordCount.getCheckedRadioButtonId() < 1 &&
            !newGameFromSMH.isChecked()) {
          Toast.makeText(NewGameActivity.this, "Please select word count.", Toast.LENGTH_SHORT).show();
          return;
        }
        Intent i = new Intent().putExtra("fromsmh", newGameFromSMH.isChecked()).
        putExtra("wordcount", newGameWordCount.getCheckedRadioButtonId()).
        putExtra("timed", newGameTimed.isChecked());
        setResult(Activity.RESULT_OK, i);
        finish();
      }
    });
  }
}
