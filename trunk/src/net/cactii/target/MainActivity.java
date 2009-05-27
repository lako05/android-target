package net.cactii.target;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.graphics.Typeface;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.ContextMenu;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.SubMenu;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.View.OnClickListener;
import android.view.View.OnCreateContextMenuListener;
import android.view.View.OnLongClickListener;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.AdapterView.OnItemClickListener;

public class MainActivity extends Activity {

  // The main grid object.
  private TargetGridView targetGrid;
  // Current word is displayed here.
  private TextView enteredWordBox;
  // 'Clear' button
  private Button clearWord;
  // 'Submit' button.
  private Button submitWord;
  // Thread to handle the dictionary.
  private Thread dictionaryThread;
  // Display of 'Good/Very Good/Excellent'
  private TextView targetCounts;
  // Points to the active instance of the main activity.
  public static MainActivity currentInstance = null;
  // Filename where active games are saved to.
  public static final String saveFilename = "/data/data/net.cactii.target/savedgame";
  // List of the players current words.
  private ArrayList<PlayerWord> playerWords;
  // List view of the players current words.
  private ListView playerWordList;
  // Adapter to link playerWordList to playerWords
  private WordAdapter playerWordsAdapter = null;
  
  private TextView bottomText = null;
  
  public static final int CONTEXT_DELETE = 0;
  public static final int CONTEXT_DEFINE = 1;

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
    this.targetGrid = (TargetGridView)findViewById(R.id.targetGrid);
    this.enteredWordBox = (TextView)findViewById(R.id.enteredWord);
    this.clearWord = (Button)findViewById(R.id.clearWord);
    this.submitWord = (Button)findViewById(R.id.submitWord);
    this.targetCounts = (TextView)findViewById(R.id.targetCounts);
    this.playerWordList = (ListView)findViewById(R.id.playerWordList);
    this.bottomText = (TextView)findViewById(R.id.bottomText);

    // This is the font for the current word box.
    // This is 'Purisa' for now (ubuntu ttf-thai-tlwg)
    Typeface face=Typeface.createFromAsset(getAssets(), "fonts/font.ttf");
    this.enteredWordBox.setTypeface(face);

    // When a letter on the grid is touched, update the current word box.
    this.targetGrid.setLetterTouchedListener(new LetterTouchedHandler() {
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
        String word = targetGrid.getSelectedWord();
        String message = "";
        if (word.length() < 4)
          message = "Must be at least 4 letters.";
        else if (!word.contains(DictionaryThread.currentInstance.magicLetter))
          message = "Must contain the middle letter.";
        else if (MainActivity.this.playerHasWord(word))
          message = "You already have that word.";
        if (message != "") {
          Toast.makeText(MainActivity.this, message, Toast.LENGTH_SHORT).show();
          return;
        }
        PlayerWord playerWord = new PlayerWord(word);
        MainActivity.this.playerWords.add(playerWord);
        MainActivity.this.playerWordsAdapter.notifyDataSetChanged();
        MainActivity.this.playerWordList.setSelectionFromTop(MainActivity.this.playerWords.size()-1, 10);
        enteredWordBox.setText("");
        targetGrid.clearGrid();
        if (PreferenceManager.getDefaultSharedPreferences(
        		MainActivity.currentInstance).getBoolean("livescoring", false)) {
        	MainActivity.this.scoreWord(playerWord);
        }
        MainActivity.this.showWordCounts(MainActivity.this.countCorrectWords());
      }
    });
    /*
    this.playerWordList.setOnItemClickListener(new OnItemClickListener() {
    	public void onItemClick(AdapterView adapterView, View view, int arg2, long arg3) {
    		int selectedPosition = adapterView.getSelectedItemPosition();
    	}
    });
    */
    
    /* Add Context-Menu listener to the ListView. */
    this.playerWordList.setOnCreateContextMenuListener(new OnCreateContextMenuListener() {

      @Override
      public void onCreateContextMenu(ContextMenu menu, View v,
          ContextMenuInfo menuInfo) {
        menu.setHeaderTitle("Selected word");
        menu.add(ContextMenu.NONE, CONTEXT_DELETE, ContextMenu.NONE, "Delete word");
        menu.add(ContextMenu.NONE, CONTEXT_DEFINE, ContextMenu.NONE, "Define word");
        // Log.d("Target", "Selected: " + menu.);
      }
    });

    // Initialise the playerWord list, and set the Adapter.
    this.playerWords = new ArrayList<PlayerWord>();
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
        showWordCounts(MainActivity.this.playerWords.size());
        break;
      case DictionaryThread.MESSAGE_DICTIONARY_READY :
        // Called after game is restored when dictionary is ready.
        restoreGame();
        break;
      }
    }
  };

  public void onDestroy() {
    if (this.targetGrid.gameActive)
      this.saveGame();
    super.onDestroy();
  }

  // Sets the game to active or inactive by disabling/enabling the controls
  private void setGameState(boolean state) {
    this.targetGrid.gameActive = state;
    this.submitWord.setEnabled(state);
    this.clearWord.setEnabled(state);
    this.playerWordList.setClickable(state);
//    this.playerWordList.setLongClickable(state);
  }

  // Save the current game state
  // Simple text file..
  // First line is nine letter word
  // Second line is shuffled version
  // Other lines are valid words, until @@PLAYERWORDS@@
  // Then the rest are the player's words
  private void saveGame() {
    BufferedWriter writer = null;
    try {
      writer = new BufferedWriter(new FileWriter(MainActivity.saveFilename));
      writer.write(DictionaryThread.currentInstance.currentNineLetter + "\n");
      writer.write(DictionaryThread.currentInstance.currentShuffled + "\n");
      for (String word : DictionaryThread.currentInstance.validWords)
        writer.write(word + "\n");
      writer.write("@@PLAYERWORDS@@\n");
      for (PlayerWord word : this.playerWords)
        writer.write(word.word + "\n");
    } catch (IOException e) {
      Log.d("Target", "Error saving game: "+e.getMessage());
    }
    finally {
      try {
        if (writer != null)
          writer.close();
      } catch (IOException e) {
        //pass
      }
    }
    Log.d("Target", "Successfully saved game.");
  }

  // restore from the above saved file
  public void restoreGame() {
    String line = null;
    BufferedReader br = null;
    InputStream ins = null;

    try {
      ins = new FileInputStream(new File(MainActivity.saveFilename));
      br = new BufferedReader(new InputStreamReader(ins), 8192);
      this.playerWords.clear();
      DictionaryThread.currentInstance.currentNineLetter = br.readLine();
      DictionaryThread.currentInstance.currentNineLetterArray = DictionaryThread.currentInstance.currentNineLetter.toCharArray();
      DictionaryThread.currentInstance.currentShuffled = br.readLine();
      this.targetGrid.setLetters(DictionaryThread.currentInstance.currentShuffled);
      DictionaryThread.currentInstance.magicLetter = DictionaryThread.currentInstance.currentShuffled.substring(4, 5);
      boolean fetchingPlayerWords = false;
      DictionaryThread.currentInstance.validWords = new ArrayList<String>();
      while((line = br.readLine())!=null) {
        String word = line.trim();
        if (fetchingPlayerWords) {
          PlayerWord playerWord = new PlayerWord(word);
          this.playerWords.add(playerWord);
        } else {
          if (word.equals("@@PLAYERWORDS@@")) {
            fetchingPlayerWords = true;
            continue;
          }
          DictionaryThread.currentInstance.validWords.add(word);
        }
      }
      this.bottomText.setVisibility(View.GONE);
      this.playerWordList.setVisibility(View.VISIBLE);
      this.playerWordsAdapter.notifyDataSetChanged();
      this.setGameState(true);
      showWordCounts(this.playerWords.size());
      Log.d("Target", "Restored game successfully.");
    } catch (FileNotFoundException e) {
      Log.d("Target", "FNF Error restoring game: " + e.getMessage());
    } catch (IOException e) {
      Log.d("Target", "IO Error restoring game: " + e.getMessage());
    }
    finally {
      try {
        ins.close();
        br.close();
      } catch (Exception e) {
        // Nothing.
      }
    }
  }

  // Updates the 'Good/Very Good/Excellent' display.
  private void showWordCounts(int playerWords) {
    int numWords = DictionaryThread.currentInstance.validWords.size();
    int good = numWords/2;
    int vgood = numWords*3/4;
    int excellent = numWords;
    String targets = "Good:            " + good +
    "\nVery good:    " + vgood +
    "\nExcellent:      " + excellent +
    "\n\nYou:              " + playerWords;
    if (numWords > 0 && this.targetGrid.gameActive == false) {
      if (playerWords >= excellent)
        targets += "\n EXCELLENT!";
      else if (playerWords >= vgood)
        targets += "\n VERY GOOD!";
      else if (playerWords >= good)
        targets += "\n   GOOD!";
    }
    this.targetCounts.setText(targets);
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
      enteredWordBox.setText("");
      this.bottomText.setVisibility(View.GONE);
      this.playerWordList.setVisibility(View.VISIBLE);
      this.setGameState(false);
      this.targetCounts.setText("Getting words..");
      Message msg = Message.obtain();
      msg.what = DictionaryThread.MESSAGE_GET_NINELETTER;
      DictionaryThread.currentInstance.messageHandler.sendMessage(msg);

      this.playerWords.clear();
      this.playerWordsAdapter.notifyDataSetChanged();
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

  // Called on a long press of a word, asks player to delete the word.
  public boolean onContextItemSelected(MenuItem item) {
    AdapterView.AdapterContextMenuInfo menuInfo = (AdapterView.AdapterContextMenuInfo)item.getMenuInfo();
    /* Switch on the ID of the item, to get what the user selected. */
    switch (item.getItemId()) {
      case CONTEXT_DELETE:
        if (this.targetGrid.gameActive == false)
          return true;
        this.playerWords.remove((int)menuInfo.id);
        this.playerWordsAdapter.notifyDataSetChanged();
        this.showWordCounts(this.playerWords.size());
        return true; /* true means: "we handled the event". */
      case CONTEXT_DEFINE:
        String word = this.playerWords.get((int)menuInfo.id).word;
        Intent myIntent = null;
        myIntent = new Intent("android.intent.action.VIEW", Uri.parse("http://www.google.com/" +
            "search?q=define:" + word.toLowerCase()));
        // Start the activity
        startActivity(myIntent); 
        return true;
    }
    return false;
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
		  if (scoreWord(playerWord))
			  correct++;
	  }
	  return correct;
  }
  
  // Score an individual word
  // Returns boolean, if the word is valid/ok
  public boolean scoreWord(PlayerWord playerWord) {
	if (DictionaryThread.currentInstance.validWords.contains(playerWord.word) ||
	    playerWord.word.equals(DictionaryThread.currentInstance.currentNineLetter)) {
	  playerWord.result = PlayerWord.RESULT_OK;
	} else
	  playerWord.result = PlayerWord.RESULT_INVALID;
	this.playerWordsAdapter.notifyDataSetChanged();
	return playerWord.result == PlayerWord.RESULT_OK;
  }

  // Score the player's words.
  public void scoreAllWords() {
	int correctUserWords;
	
	correctUserWords = countCorrectWords();
    PlayerWord header = new PlayerWord("MISSED WORDS");
    header.result = PlayerWord.RESULT_HEADER;
    this.playerWords.add(header);
    // If the player missed it, show the 9 letter word first
    if (!playerHasWord(DictionaryThread.currentInstance.currentNineLetter)) {
      PlayerWord resultWord = new PlayerWord(DictionaryThread.currentInstance.currentNineLetter);
      resultWord.result = PlayerWord.RESULT_MISSED;
      this.playerWords.add(resultWord);
    }

    // Then show all other missed words
    for (String validWord : DictionaryThread.currentInstance.validWords) {
      if (playerHasWord(validWord) == false &&
          validWord != DictionaryThread.currentInstance.currentNineLetter) {
        PlayerWord resultWord = new PlayerWord(validWord);
        resultWord.result = PlayerWord.RESULT_MISSED;
        this.playerWords.add(resultWord);
      }
    }
    this.playerWordsAdapter.notifyDataSetChanged();
    showWordCounts(correctUserWords);
    new File(MainActivity.saveFilename).delete();
  }

  private void openHelpDialog() {
    LayoutInflater li = LayoutInflater.from(this);
    View view = li.inflate(R.layout.aboutview, null); 
    new AlertDialog.Builder(MainActivity.this)
    .setTitle("Target Help")
    .setIcon(R.drawable.about)
    .setView(view)
    .setNegativeButton("Close", new DialogInterface.OnClickListener() {
      public void onClick(DialogInterface dialog, int whichButton) {
          //
      }
    })
    .show();  		
  }
}