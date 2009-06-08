package net.cactii.target;

import java.io.File;
import java.util.ArrayList;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageInfo;
import android.graphics.Typeface;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.SubMenu;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.view.View.OnClickListener;
import android.view.View.OnLongClickListener;
import android.view.animation.Animation;
import android.view.animation.AnimationSet;
import android.view.animation.AnimationUtils;
import android.view.animation.Animation.AnimationListener;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.AdapterView.OnItemClickListener;

public class MainActivity extends Activity {

  public static final int DIALOG_FETCHING = 0;
  public static final int DIALOG_DOWNLOADING = 1;
  public static final int CLEAR_TEXTBOX = 100;
  
  public static final int ACTIVITY_NEWGAME = 7;
  
  public static final int DOWNLOAD_STARTING = 0;
  public static final int DOWNLOAD_PROGRESS = 1;
  public static final int DOWNLOAD_COMPLETE = 2;
  public static final int COUNTDOWN_PING = 3;
  
  // Top area of the game
  public LinearLayout playArea;
  // The main grid object.
  public TargetGridView targetGrid;
  
  // Current word is displayed here.
  public TextView enteredWordBox;
  public AnimationSet animationSet;
  
  // 'Clear' button
  private Button clearWord;
  
  // 'Fetching words' dialog
  private ProgressDialog progressDialog;
  public ProgressBar countDownBar;
  public CountDown countDown;
  public TextView timeRemaining;
  
  // 'Submit' button.
  private Button submitWord;
  
  // Thread to handle the dictionary.
  public Thread dictionaryThread;
  
  // Display of 'Good/Very Good/Excellent'
  private TextView targetCounts;
  
  // Points to the active instance of the main activity.
  public static MainActivity currentInstance = null;
  
  // Filename where active games are saved to.
  public static final String saveFilename = "/data/data/net.cactii.target/savedgame";
  
  // List of the players current words.
  public ArrayList<PlayerWord> playerWords;
  
  // List view of the players current words.
  public ListView playerWordList;
  
  // Adapter to link playerWordList to playerWords
  public WordAdapter playerWordsAdapter = null;
  
  // Selected word in listitem popup
  public PlayerWord currentSelectedWord;
  
  // Appl preferences
  public SharedPreferences preferences;
  public SharedPreferences.Editor prefeditor = null;
  
  // Saved game
  public SavedGame savedGame = null;
  
  public TextView bottomText = null;
 
  private static void setCurrent(MainActivity current){
    MainActivity.currentInstance = current;
  }

  /** Called when the activity is first created. */
  @Override
  public void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    // For now, a specific layout (fullscreen portrait)
    this.requestWindowFeature(Window.FEATURE_NO_TITLE);
    this.getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
        WindowManager.LayoutParams.FLAG_FULLSCREEN);
    setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
    
    MainActivity.setCurrent(this);
    setContentView(R.layout.main);
    this.playArea = (LinearLayout)findViewById(R.id.playArea);
    this.targetGrid = (TargetGridView)findViewById(R.id.targetGrid);
    this.enteredWordBox = (TextView)findViewById(R.id.enteredWord);
    this.clearWord = (Button)findViewById(R.id.clearWord);
    this.submitWord = (Button)findViewById(R.id.submitWord);
    this.targetCounts = (TextView)findViewById(R.id.targetCounts);
    this.playerWordList = (ListView)findViewById(R.id.playerWordList);
    
    // Configure the countdown timer
    this.countDownBar = (ProgressBar)findViewById(R.id.gameTimer);
    this.timeRemaining = (TextView)findViewById(R.id.timeRemaining);
    this.countDown = new CountDown(this);
    this.countDown.setCountDownTimeExpiredListener(this.countDown.new CountDownTimeExpiredListener() {
      @Override
      public void OnCountDownTimeExpired() {
        Log.d("Target", "Countdown expired!!");
        setGameState(false);
        enteredWordBox.setText("TIME'S UP!");
        scoreAllWords();
      }
    });
   
    this.preferences = PreferenceManager.getDefaultSharedPreferences(this);
    this.prefeditor = preferences.edit();
    
    newVersionCheck();
    this.savedGame = new SavedGame(this);
    
    // This is the font for the current word box.
    // This is 'Purisa' for now (ubuntu ttf-thai-tlwg)
    Typeface face=Typeface.createFromAsset(getAssets(), "fonts/font.ttf");
    this.enteredWordBox.setTypeface(face);

    // When a letter on the grid is touched, update the current word box.
    this.targetGrid.setLetterTouchedListener(this.targetGrid.new LetterTouchedHandler() {
      public void handleLetterTouched(int index) {
        enteredWordBox.setText(targetGrid.getSelectedWord());
      }
    });

    // A click on 'clear' clears the most recent letter.
    this.clearWord.setOnClickListener(new OnClickListener() {
      public void onClick(View v) {
        targetGrid.clearLastLetter();
        enteredWordBox.setText(targetGrid.getSelectedWord());
      }
    });

    // But a long click clears the word entirely
    this.clearWord.setOnLongClickListener(new OnLongClickListener() {
      public boolean onLongClick(View v) {
        targetGrid.clearGrid();
        enteredWordBox.setText("");
        return true;
      }
    });

    // Clicking 'submit' verifies the word then adds it to the list.
    this.submitWord.setOnClickListener(new OnClickListener() {
      public void onClick(View v) {
        MainActivity.this.handleSubmitClicked();
      }
    });

    this.playerWordList.setOnItemClickListener(new OnItemClickListener() {
    	public void onItemClick(AdapterView adapterView, View view, int position, long id) {
    	  PlayerWord word = MainActivity.this.playerWords.get(position);
    	  MainActivity.this.currentSelectedWord = word;
    		String[] choices = new String[1];
    		choices[0] = new String("Find definition");
    		new AlertDialog.Builder(view.getContext())
        .setTitle("Selected: " + word.word)
        .setItems(choices, new DialogInterface.OnClickListener() {
          @Override
          public void onClick(DialogInterface dialog, int which) {
            PlayerWord word = MainActivity.this.currentSelectedWord;
            if (which == 0) {
              Intent myIntent = null;
              myIntent = new Intent("android.intent.action.VIEW", Uri.parse("http://www.google.com/" +
                  "search?q=define:" + word.word.toLowerCase()));
              // Start the activity
              startActivity(myIntent); 
            }
          }
        })
        .show();
    	}
    });
    
    // Initialise the playerWord list, and set the Adapter.
    this.playerWords = new ArrayList<PlayerWord>();
    this.InitPlayerWords();
    
    this.playerWordsAdapter = new WordAdapter(this, this.playerWords);
    this.playerWordList.setAdapter(this.playerWordsAdapter);

    // Dictionary thread is started, will fetch our words.
    this.dictionaryThread = new Thread(new DictionaryThread());
    this.dictionaryThread.start();
    
    this.setGameState(false);
  }
  
  public Handler newWordReadyHandler = new Handler() {
    public void handleMessage(Message msg) {
      switch (msg.what) {
      case DictionaryThread.MESSAGE_HAVE_NINELETTER : {
        // Dictionary thread sends message that a new 9letter is available.
        String nineLetterWord = (String)msg.obj;
        targetGrid.setLetters(nineLetterWord);
        playArea.setVisibility(View.VISIBLE);
        setGameState(true);

        // This must be posted here. If called in the get_nine_letter handler,
        // it blocks for some reason.
        Message message = Message.obtain();
        message.what = DictionaryThread.MESSAGE_GET_MATCHING_WORDS;
        DictionaryThread.currentInstance.messageHandler.sendMessage(message);
        break;
      }
      case DictionaryThread.MESSAGE_HAVE_MATCHING_WORDS :
        // Called when Dictionary thread has found all matching words.
        showWordCounts(0);
        MainActivity.this.dismissDialog(MainActivity.DIALOG_FETCHING);
        MainActivity.this.animateTargetGrid();
        MainActivity.this.countDown.begin(0, 0);
        break;
      case DictionaryThread.MESSAGE_DICTIONARY_READY :
        // Called after game is restored when dictionary is ready.
        ((TextView)findViewById(R.id.starting)).setVisibility(View.GONE);
        if (MainActivity.this.savedGame.Restore()) {
          playArea.setVisibility(View.VISIBLE);
          MainActivity.this.animateTargetGrid();
        } else {
          Intent i = new Intent(MainActivity.this, NewGameActivity.class);
          startActivityForResult(i, ACTIVITY_NEWGAME);
          // playArea.setVisibility(View.GONE);
        }
        break;
      case DictionaryThread.MESSAGE_FAIL_SMH_NINELETTER :
        MainActivity.this.dismissDialog(MainActivity.DIALOG_FETCHING);
        Toast.makeText(MainActivity.this, "Error fetching smh.com.au puzzle!",
                       Toast.LENGTH_LONG).show();
        break;
      }
    }
  };

  // Handles progress messages for the SMH download feature.
  public Handler progressHandler = new Handler() {
    public void handleMessage(Message msg) {
      switch (msg.what) {
      case DOWNLOAD_STARTING:
        progressDialog.setIndeterminate(false);
        break;
      case DOWNLOAD_PROGRESS:
        progressDialog.setProgress(msg.arg1);
        break;
      case DOWNLOAD_COMPLETE:
        MainActivity.this.dismissDialog(MainActivity.DIALOG_DOWNLOADING);
        showDialog(MainActivity.DIALOG_FETCHING);
        break;
      }
    }
  };
  
  public void handleSubmitClicked() {
    String word = targetGrid.getSelectedWord();
    String message = "";
    int addedSeconds = 0;
    if (word.length() < 4)
      message = "Must be at least 4 letters.";
    else if (!word.contains(DictionaryThread.currentInstance.currentNineLetter.magicLetter))
      message = "Must contain the middle letter: " + DictionaryThread.currentInstance.currentNineLetter.magicLetter;
    else if (this.playerHasWord(word))
      message = "You already have that word.";
    if (message != "") {
      Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
      return;
    }
    PlayerWord playerWord = new PlayerWord(word);
    this.playerWords.add(playerWord);
    targetGrid.clearGrid();
    this.scoreWord(playerWord);
    this.showWordCounts(this.countCorrectWords());
    this.playerWordList.setSelectionFromTop(this.playerWords.size()-1, 10);
    if (playerWord.result == PlayerWord.RESULT_OK)
      addedSeconds = this.countDown.addWord(playerWord.word.length());

    // Animate the word result text
    int animateColour;
    String animateText;
    if (playerWord.result == PlayerWord.RESULT_OK) {
      animateColour = 0xFF008000;
      animateText = "GOOD!";
      if (countDown.enabled)
        animateText += " +" + addedSeconds + "s";
    } else {
      animateColour = 0xFFF00000;
      animateText = "UNKNOWN!";
    }
    animateTextBox(animateText, animateColour, R.anim.textboxfade);
  }

  public void onPause() {
    this.savedGame.Save();
    this.countDown.pause();
    Log.d("Target", "Paused game");
    super.onPause();
  }
  
  public void onResume() {
    this.countDown.resume();
    Log.d("Target", "Resumed game");
    super.onResume();
  }
    
  @Override
  protected Dialog onCreateDialog(int id) {
    if (id == DIALOG_FETCHING) {
      progressDialog = new ProgressDialog(this);
      progressDialog.setTitle("Fetching words");
      progressDialog.setMessage("Please wait...");
      progressDialog.setIndeterminate(false);
      progressDialog.setCancelable(true);
      return progressDialog;
    } else if (id == DIALOG_DOWNLOADING) {
      progressDialog = new ProgressDialog(this);
      progressDialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
      progressDialog.setTitle("Downloading puzzle..");
      progressDialog.setMessage("Please wait...");
      progressDialog.setIndeterminate(true);
      progressDialog.setProgress(0);
      progressDialog.setMax(100);
      return progressDialog;
    }
    return null;
  }

  // Sets the game to active or inactive by disabling/enabling the controls
  public void setGameState(boolean state) {
    this.targetGrid.gameActive = state;
    this.submitWord.setEnabled(state);
    this.clearWord.setEnabled(state);
    this.playerWordList.setClickable(state);
  }

  // Updates the 'Good/Very Good/Excellent' display.
  public void showWordCounts(int playerWords) {
    int numWords = DictionaryThread.currentInstance.validWords.size();
    int good = numWords/2;
    int vgood = numWords*3/4;
    int excellent = numWords;
    String targets = "Good:          " + good +
    "\nVery good:  " + vgood +
    "\nExcellent:    " + excellent +
    "\n\nYou:             " + playerWords;
    if (numWords > 0 && this.targetGrid.gameActive == false) {
      if (playerWords >= excellent)
        showWordMessage("EXCELLENT!");
      else if (playerWords >= vgood)
        showWordMessage("VERY GOOD!");
      else if (playerWords >= good)
        showWordMessage("GOOD!");
    }
    this.targetCounts.setText(targets);
  }
  
  public void showWordMessage(String message) {
    TextView box = this.enteredWordBox;
    box.setText(message);
  }
  
  public void animateTargetGrid() {
    Animation animation = AnimationUtils.loadAnimation(this, R.anim.targetzoomin);
    this.targetGrid.setVisibility(View.VISIBLE);
    this.targetGrid.startAnimation(animation);
  }
  
  public void animateTextBox(String text, int colour, int animationResource) {

    Animation animation = AnimationUtils.loadAnimation(this, animationResource);

    animation.setAnimationListener(new AnimationListener() {
      @Override
      public void onAnimationEnd(Animation animation) {
        enteredWordBox.setTextColor(0xFF000000); 
        MainActivity.currentInstance.enteredWordBox.setText(""); 
        enteredWordBox.setGravity(Gravity.LEFT);
        playerWordsAdapter.notifyDataSetChanged();
      }
      @Override
      public void onAnimationRepeat(Animation animation) {}
      @Override
      public void onAnimationStart(Animation animation) {}
    });
    this.enteredWordBox.setGravity(Gravity.CENTER);
    this.enteredWordBox.setText(text);
    this.enteredWordBox.setTextColor(colour);
    this.enteredWordBox.startAnimation(animation);
  }

  /**
   * Menus are created here. There are 3 menu items, all are quite self
   * explanatory.
   */
  private static final int MENU_NEWWORD = 0;
  private static final int MENU_SCORE = 1;
  private static final int MENU_INSTRUCTIONS = 2;
  private static final int MENU_OPTIONS = 3;

  @Override
  public boolean onCreateOptionsMenu(Menu menu) {
    boolean supRetVal = super.onCreateOptionsMenu(menu);
    SubMenu menu_new = menu.addSubMenu(0, MENU_NEWWORD, 0, "New game");
    menu_new.setIcon(R.drawable.menu_new);
    SubMenu menu_score = menu.addSubMenu(0, MENU_SCORE, 0, "Score game");
    menu_score.setIcon(R.drawable.menu_score);
    SubMenu menu_help = menu.addSubMenu(0, MENU_INSTRUCTIONS, 0, "Help");
    menu_help.setIcon(R.drawable.menu_help);
    SubMenu menu_options = menu.addSubMenu(0, MENU_OPTIONS, 0, "Options");
    menu_options.setIcon(R.drawable.menu_options);
    return supRetVal;
  }   

  @Override
  public boolean onOptionsItemSelected(MenuItem menuItem) {
    boolean supRetVal = super.onOptionsItemSelected(menuItem);
    switch (menuItem.getItemId()) {
    case MENU_NEWWORD : {
      // selectGameType();
      Intent i = new Intent(this, NewGameActivity.class);
      startActivityForResult(i, ACTIVITY_NEWGAME);
      break;
    }
    case MENU_SCORE : {
      if (targetGrid.gameActive) {
        this.setGameState(false);
        this.scoreAllWords();
      }
      break;
    }
    case MENU_INSTRUCTIONS : {
      openHelpDialog();
      break;
    }
    case MENU_OPTIONS :
      startActivityForResult(new Intent(
          MainActivity.this, OptionsActivity.class), 0);
      break;
    }
    return supRetVal;
  }
  
  protected void onActivityResult(int requestCode, int resultCode,
      Intent data) {
    if (requestCode != ACTIVITY_NEWGAME || resultCode != Activity.RESULT_OK)
      return;
    Log.d("Target", "Got newgame result: request " + requestCode + " result "
        + resultCode);
    final Message msg = Message.obtain();
    Bundle extras = data.getExtras();
    if (extras.getBoolean("fromsmh")) {
      msg.what = DictionaryThread.MESSAGE_GET_SMH_NINELETTER;
    } else{
      msg.what = DictionaryThread.MESSAGE_GET_NINELETTER;
      switch (extras.getInt("wordcount")) {
      case R.id.newWordCount1 :
        msg.arg1 = 1;
        msg.arg2 = 30;
        break;
      case R.id.newWordCount30 :
        msg.arg1 = 30;
        msg.arg2 = 75;
        break;
      case R.id.newWordCount75 :
        msg.arg1 = 75;
        msg.arg2 = 500;
      default :
        return;
      }
    }
    showDialog(msg.what == DictionaryThread.MESSAGE_GET_SMH_NINELETTER ? 
        MainActivity.DIALOG_DOWNLOADING : MainActivity.DIALOG_FETCHING);

    this.playArea.setVisibility(View.VISIBLE);
    this.InitPlayerWords();
    this.playerWordsAdapter.notifyDataSetChanged();
    this.showWordMessage("");
    this.timeRemaining.setText("");
    this.targetCounts.setText("");
    this.targetGrid.setVisibility(View.INVISIBLE);
    this.countDown.enabled = extras.getBoolean("timed");
    new File(MainActivity.saveFilename).delete();
    new Thread(new Runnable() {
      @Override
      public void run() {
        // Sleep a fraction, allows main UI to catch up
        try {
          Thread.sleep(300);
        } catch (InterruptedException e) { }
        DictionaryThread.currentInstance.messageHandler.sendMessage(msg);
      }
    }).start();
  }

  private boolean playerHasWord(String word) {
    for (PlayerWord playerWord : this.playerWords) {
      if (playerWord.word.contentEquals(word))
        return true;
    }
    return false;
  }
  
  // Count the number of correct words the player has.
  public int countCorrectWords() {
	  int correct = 0;
	  for (PlayerWord playerWord : this.playerWords) {
		  if (playerWord.result != PlayerWord.RESULT_HEADER && scoreWord(playerWord))
			  correct++;
	  }
	  return correct;
  }

  public void InitPlayerWords() {
    this.playerWords.clear();
    PlayerWord header = new PlayerWord("YOUR WORDS");
    header.result = PlayerWord.RESULT_HEADER;
    this.playerWords.add(header);
  }
  
  // Returns a count of the player's words.
  public int CountPlayerWords() {
    int count = 0;
    for (PlayerWord word : this.playerWords) {
      if (word.result != PlayerWord.RESULT_HEADER &&
          word.result != PlayerWord.RESULT_MISSED)
        count++;
    }
    return count;
  }
  
  // Score an individual word
  // Returns boolean, if the word is valid/ok
  public boolean scoreWord(PlayerWord playerWord) {
	if (DictionaryThread.currentInstance.validWords.contains(playerWord.word) ||
	    playerWord.word.equals(DictionaryThread.currentInstance.currentNineLetter.word)) {
	  playerWord.result = PlayerWord.RESULT_OK;
	} else
	  playerWord.result = PlayerWord.RESULT_INVALID;
	this.playerWordsAdapter.notifyDataSetChanged();
	return playerWord.result == PlayerWord.RESULT_OK;
  }

  // Score the player's words.
  public void scoreAllWords() {
  	int correctUserWords;
  	
  	this.countDown.end();
  	correctUserWords = countCorrectWords();
    PlayerWord header = new PlayerWord("MISSED WORDS");
    header.result = PlayerWord.RESULT_HEADER;
    this.playerWords.add(header);
    // If the player missed it, show the 9 letter word first
    if (!playerHasWord(DictionaryThread.currentInstance.currentNineLetter.word)) {
      PlayerWord resultWord = new PlayerWord(DictionaryThread.currentInstance.currentNineLetter.word);
      resultWord.result = PlayerWord.RESULT_MISSED;
      this.playerWords.add(resultWord);
    }
  
    // Then show all other missed words
    for (String validWord : DictionaryThread.currentInstance.validWords) {
      if (playerHasWord(validWord) == false &&
          validWord != DictionaryThread.currentInstance.currentNineLetter.word) {
        PlayerWord resultWord = new PlayerWord(validWord);
        resultWord.result = PlayerWord.RESULT_MISSED;
        this.playerWords.add(resultWord);
      }
    }
    this.playerWordsAdapter.notifyDataSetChanged();
    showWordCounts(correctUserWords);
  }

  private void openHelpDialog() {
    LayoutInflater li = LayoutInflater.from(this);
    View view = li.inflate(R.layout.aboutview, null); 
    new AlertDialog.Builder(MainActivity.this)
    .setTitle("Target Help")
    .setIcon(R.drawable.about)
    .setView(view)
    .setNeutralButton("Changes", new DialogInterface.OnClickListener() {
      public void onClick(DialogInterface dialog, int whichButton) {
          MainActivity.currentInstance.openChangesDialog();
      }
    })
    .setNegativeButton("Close", new DialogInterface.OnClickListener() {
      public void onClick(DialogInterface dialog, int whichButton) {
          //
      }
    })
    .show();  
    // TextView versionLabel = (TextView)findViewById(R.id.aboutVersionCode);
    // versionLabel.setText(getVersionName());
  }
  
  private void openChangesDialog() {
    LayoutInflater li = LayoutInflater.from(this);
    View view = li.inflate(R.layout.changeview, null); 
    new AlertDialog.Builder(MainActivity.this)
    .setTitle("Changelog")
    .setIcon(R.drawable.about)
    .setView(view)
    .setNegativeButton("Close", new DialogInterface.OnClickListener() {
      public void onClick(DialogInterface dialog, int whichButton) {
          //
      }
    })
    .show();  
  }
  
  public void newVersionCheck() {
    int pref_version = preferences.getInt("currentversion", -1);
    int current_version = getVersionNumber();
    if (pref_version == -1 || pref_version != current_version) {
      this.prefeditor.putInt("currentversion", current_version);
      this.prefeditor.commit();
      new File(MainActivity.saveFilename).delete();
      Log.d("Target", "Version number bumped from " + pref_version + " to " + current_version);
      this.openChangesDialog();
      return;
    }
  }
  
  public int getVersionNumber() {
    int version = -1;
      try {
          PackageInfo pi = getPackageManager().getPackageInfo(getPackageName(), 0);
          version = pi.versionCode;
      } catch (Exception e) {
          Log.e("Target", "Package name not found", e);
      }
      return version;
  }
  public String getVersionName() {
    String version = "";
      try {
          PackageInfo pi = getPackageManager().getPackageInfo(getPackageName(), 0);
          version = pi.versionName;
      } catch (Exception e) {
          Log.e("Target", "Package name not found", e);
      }
      return version;
  }
}