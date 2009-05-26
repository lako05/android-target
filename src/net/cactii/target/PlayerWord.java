package net.cactii.target;

public class PlayerWord {
  public static final int RESULT_UNSCORED = 0;
  public static final int RESULT_OK = 1;
  public static final int RESULT_INVALID = 2;
  public static final int RESULT_MISSED = 3;
  public static final int RESULT_HEADER = 4;
  
  public String word;
  public int result;
  
  public PlayerWord(String word) {
    super();
    this.word = word;
    this.result = RESULT_UNSCORED;
  }
}
