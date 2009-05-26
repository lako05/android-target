package net.cactii.target;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;

import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.preference.PreferenceManager;
import android.util.Log;
import android.widget.Toast;

public class DictionaryThread implements Runnable {

  public static final int MESSAGE_GET_NINELETTER = 0;
  public static final int MESSAGE_HAVE_NINELETTER = 1;
  public static final int MESSAGE_GET_MATCHING_WORDS= 2;
  public static final int MESSAGE_HAVE_MATCHING_WORDS= 3;
  public static final int MESSAGE_DICTIONARY_READY= 4;
  public static final int MESSAGE_REREAD_DICTIONARY = 5;

  private int nineLetterDictionary = 2;
  private int currentDictionary = 2;
  
  private ArrayList<String> nineLetterWords;
  public ArrayList<String> validWords;

  public String currentNineLetter;
  public char[] currentNineLetterArray;
  public String currentShuffled;
  public String magicLetter;

  public static DictionaryThread currentInstance = null;

  private static void setCurrent(DictionaryThread current){
    DictionaryThread.currentInstance = current;
  }

  @Override
  public void run() {
    Looper.prepare();
    DictionaryThread.setCurrent(this);
    getDictionary();
    getNineLetterWords();	
    Message message = Message.obtain();
    message.what = MESSAGE_DICTIONARY_READY;
    MainActivity.currentInstance.newWordReadyHandler.sendMessage(message);
    Looper.loop();
  }

  public Handler messageHandler = new Handler() {
    public void handleMessage(Message msg) {
      switch(msg.what) {
        case MESSAGE_GET_NINELETTER : {
          // Fetch a random word from the 9-letter array.
          currentNineLetter = nineLetterWords.get((int) (Math.random() * nineLetterWords.size()));
          // Shuffle the letters, assign variables
          currentShuffled = shuffle(currentNineLetter);
          magicLetter = currentShuffled.substring(4, 5);
          currentNineLetterArray = currentNineLetter.toCharArray();
  
          // Send message back saying we have the word
          Message message = Message.obtain();
          message.what = MESSAGE_HAVE_NINELETTER;
          message.obj = currentShuffled;
          // Log.d("Target", "Mixing '" + currentNineLetter +
          //		"' into '" + currentShuffled + "'. (" + magicLetter + ")");
          MainActivity.currentInstance.newWordReadyHandler.sendMessage(message);
          break;
        }
        case MESSAGE_GET_MATCHING_WORDS : {
          // Find words matching current nine letter (shuffled)
          getMatchingWords();
          // Send notification back to main thread
          Message message = Message.obtain();
          message.what = MESSAGE_HAVE_MATCHING_WORDS;
          MainActivity.currentInstance.newWordReadyHandler.sendMessage(message);
          break;
        }
        case MESSAGE_REREAD_DICTIONARY : {
          // Dictionary selection has changed, reload
          getDictionary();
          getNineLetterWords();
        }
      }
    }
  };

  // Fetch all nine letter words from the dictionary, populates this.nineLetterWords.
  private void getNineLetterWords() {
    InputStream is = MainActivity.currentInstance.getResources().openRawResource(this.nineLetterDictionary);
    BufferedReader rd = new BufferedReader(new InputStreamReader(is));
    nineLetterWords = new ArrayList<String>();
    String word;
    try {
      while((word = rd.readLine())!=null) {
        word = word.trim();
        nineLetterWords.add(word);
      }
      is.close();
    } catch (IOException e) {
      //pass
    }
    Log.d("Target", "Read all 9 letter words.");
  }

  // Get all words matching currentNineLetter and magicLetter
  private void getMatchingWords() {
    InputStream is = MainActivity.currentInstance.getResources().openRawResource(this.currentDictionary);
    BufferedReader rd = new BufferedReader(new InputStreamReader(is));
    validWords = new ArrayList<String>();
    String word;
    try {
      while((word = rd.readLine())!=null) {
        word = word.trim();
        if (isValidWord(word))
          validWords.add(word);
      }
      is.close();
    } catch (IOException e) {
      //pass
    }
    Log.d("Target", "Found matches, " + validWords.size() + " words.");
  }

  // Determines if a given word matches the current nine letter
  private boolean isValidWord(String word) {
    if (!word.contains(magicLetter))
      return false;

    char checkingLetter;
    int i;
    int j;
    char[] wordArray = word.toCharArray();
    int wordLength = wordArray.length;

    for (i = 0 ; i < 9 ; i++) {
      checkingLetter = currentNineLetterArray[i];
      for (j = 0 ; j < wordLength ; j++) {
        if (wordArray[j] == checkingLetter) {
          wordArray[j] = '0';
          break;
        }
      }
    }
    for (i = 0 ; i < wordLength ; i++) {
      if (wordArray[i] != '0')
        return false;
    }
    // Log.d("Target", "Found word: " + word);
    return true;
  }

  // Shuffles the letters in a word, returns the shuffled result
  private String shuffle(String word){
    if (word.length()<=1)
      return word;

    int split=word.length()/2;

    String temp1=shuffle(word.substring(0,split));
    String temp2=shuffle(word.substring(split));

    if (Math.random() > 0.5) 
      return temp1 + temp2;
    else 
      return temp2 + temp1;
  }
  
  private void getDictionary() {
    String dict = PreferenceManager.getDefaultSharedPreferences(MainActivity.currentInstance).getString("dictpref", "2");
    if (dict.equals("2")) {
      Toast.makeText(MainActivity.currentInstance, "Select your dictionary in Options", Toast.LENGTH_LONG).show();
      dict = "0";
    }
    if (dict.equals("0")) {
      this.nineLetterDictionary = R.raw.nineletterwords;
      this.currentDictionary = R.raw.words;
      Log.d("Target", "Reading British dictionary");
    }
    else if (dict.equals("1")) {
      this.nineLetterDictionary = R.raw.nineletterwords_us;
      this.currentDictionary = R.raw.words_us;
      Log.d("Target", "Reading American dictionary");
    }
  }

}
