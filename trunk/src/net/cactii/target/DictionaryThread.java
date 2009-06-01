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
  public static final int MESSAGE_GET_SMH_NINELETTER = 6;
  public static final int MESSAGE_HAVE_SMH_NINELETTER = 7;
  public static final int MESSAGE_FAIL_SMH_NINELETTER = 8;

  private int nineLetterDictionary = 2;
  private int currentDictionary = 2;
  
  // List of all nine letter words
  private ArrayList<NineLetterWord> nineLetterWords;
  // List of valid words for current Nine letter
  public ArrayList<String> validWords;

  public NineLetterWord currentNineLetter;

  public static DictionaryThread currentInstance = null;

  private static void setCurrent(DictionaryThread current){
    DictionaryThread.currentInstance = current;
  }

  @Override
  public void run() {
    Looper.prepare();
    DictionaryThread.setCurrent(this);
    getDictionary();

    nineLetterWords = new ArrayList<NineLetterWord>();
    getNineLetterWords(R.raw.nineletterwords_common);	
    getNineLetterWords(nineLetterDictionary);
    
    Message message = Message.obtain();
    message.what = MESSAGE_DICTIONARY_READY;
    MainActivity.currentInstance.newWordReadyHandler.sendMessage(message);
    Looper.loop();
  }

  public Handler messageHandler = new Handler() {
    public void handleMessage(Message msg) {
      switch(msg.what) {
        case MESSAGE_GET_SMH_NINELETTER : {
          SmhImport smhThread = new SmhImport();
//        smhThread.currentPuzzleDate = "2009/05/23";
          new Thread(smhThread).start();
          break;
        }
        case MESSAGE_HAVE_SMH_NINELETTER : {
          currentNineLetter = new NineLetterWord((String) msg.obj);
          currentNineLetter.setShuffledWord((String) msg.obj);
          Message message = Message.obtain();
          if (currentNineLetter == null || currentNineLetter.word.length() != 9) {
            message.what = MESSAGE_FAIL_SMH_NINELETTER;
          } else {
            findNineLetterWord();
            // Send message back saying we have the word
            message.what = MESSAGE_HAVE_NINELETTER;
            message.obj = currentNineLetter.shuffled;
          }
          MainActivity.currentInstance.newWordReadyHandler.sendMessage(message);
          break;
        }
        case MESSAGE_GET_NINELETTER : {
          
          int minSize = msg.arg1;
          int maxSize = msg.arg2;
          do {
            currentNineLetter = nineLetterWords.get((int) (Math.random() * nineLetterWords.size()));
          } while (NineLetterWord.shuffleWithRange(currentNineLetter, minSize, maxSize) == false);
          // Send message back saying we have the word
          Message message = Message.obtain();
          message.what = MESSAGE_HAVE_NINELETTER;
          message.obj = currentNineLetter.shuffled;
          MainActivity.currentInstance.newWordReadyHandler.sendMessage(message);
          break;
        }
        case MESSAGE_GET_MATCHING_WORDS : {
          // Find words matching current nine letter (shuffled)
          validWords = new ArrayList<String>();
          getMatchingWords(R.raw.words_common);
          getMatchingWords(currentDictionary);
          
          // Send notification back to main thread
          Message message = Message.obtain();
          message.what = MESSAGE_HAVE_MATCHING_WORDS;
          MainActivity.currentInstance.newWordReadyHandler.sendMessage(message);
          break;
        }
        case MESSAGE_REREAD_DICTIONARY : {
          // Dictionary selection has changed, reload
          getDictionary();
          nineLetterWords.clear();
          getNineLetterWords(R.raw.nineletterwords_common); 
          getNineLetterWords(nineLetterDictionary);
        }
      }
    }
  };

  // Fetch all nine letter words from the dictionary, populates this.nineLetterWords.
  private void getNineLetterWords(int dictionary) {
    InputStream is = MainActivity.currentInstance.getResources().openRawResource(dictionary);
    BufferedReader rd = new BufferedReader(new InputStreamReader(is));
    String word;
    Integer wordCount;
    try {
      while((word = rd.readLine())!=null) {
        word = word.trim();
        nineLetterWords.add(new NineLetterWord(word));
      }
      is.close();
    } catch (IOException e) {
      //pass
    }
    Log.d("Target", "Read all 9 letter words.");
  }

  // From 'currentNineLetterWord', with an unknown word (ie only the
  // scrambled word), attempt to find the word.
  private void findNineLetterWord() {
    validWords = new ArrayList<String>();
    getMatchingWords(nineLetterDictionary);
    if (validWords.size() > 0) {
      currentNineLetter.word = validWords.get(0);
    } else {
      getMatchingWords(R.raw.nineletterwords_common);
      currentNineLetter.word = validWords.get(0);
    }
  }

  // Get all words matching currentNineLetter and magicLetter
  private void getMatchingWords(int dictionary) {
    InputStream is = MainActivity.currentInstance.getResources().openRawResource(dictionary);
    BufferedReader rd = new BufferedReader(new InputStreamReader(is));
    String word;
    try {
      while((word = rd.readLine())!=null) {
        word = word.trim();
        // Nineletter words have ':' for word counts...we just want the first part
        if (word.contains(":")) {
          word = word.substring(0, 9);
          if (isValidWord(word)) {
            validWords.add(word);
            break;
          }
        } else {
          if (isValidWord(word))
            validWords.add(word);
        }
      }
      is.close();
    } catch (IOException e) {
      //pass
    }
    Log.d("Target", "Found matches, " + validWords.size() + " words.");
  }

  // Determines if a given word matches the current nine letter
  private boolean isValidWord(String word) {
    if (!word.contains(currentNineLetter.magicLetter))
      return false;

    char checkingLetter;
    int i;
    int j;
    char[] wordArray = word.toCharArray();
    int wordLength = wordArray.length;
    currentNineLetter.array = currentNineLetter.word.toCharArray();

    for (i = 0 ; i < 9 ; i++) {
      checkingLetter = currentNineLetter.array[i];
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
  
  private void getDictionary() {
    String dict = PreferenceManager.getDefaultSharedPreferences(MainActivity.currentInstance).getString("dictpref", "2");
    if (dict.equals("2")) {
      Toast.makeText(MainActivity.currentInstance, "Select your dictionary in Options, defaulting to UK.", Toast.LENGTH_LONG).show();
      dict = "0";
    }
    if (dict.equals("0")) {
      this.nineLetterDictionary = R.raw.nineletterwords_uk;
      this.currentDictionary = R.raw.words_uk;
      Log.d("Target", "Reading British dictionary");
    }
    else if (dict.equals("1")) {
      this.nineLetterDictionary = R.raw.nineletterwords_us;
      this.currentDictionary = R.raw.words_us;
      Log.d("Target", "Reading American dictionary");
    }
  }

}
