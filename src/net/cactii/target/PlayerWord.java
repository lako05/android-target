package net.cactii.target;

public class PlayerWord {
  public static final int RESULT_UNSCORED = 0; // Word yet to be scored
  public static final int RESULT_OK = 1;       // Word is valid
  public static final int RESULT_INVALID = 2;  // Word not found in dict
  public static final int RESULT_MISSED = 3;   // A word the player didnt get
  public static final int RESULT_HEADER = 4;   // Just a list header line
  
  public String word; // Text of the player's word
  public int result;  // Word correctness
  
  public PlayerWord(String word) {
    super();
    this.word = word;
    this.result = RESULT_UNSCORED;
  }
}
