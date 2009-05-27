package net.cactii.target;

import java.util.ArrayList;

import android.app.Activity;
import android.graphics.Typeface;
import android.text.Spannable;
import android.text.style.StrikethroughSpan;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.LinearLayout;
import android.widget.TextView;

public class WordAdapter extends BaseAdapter {

  private LayoutInflater inflater;
  private ArrayList<PlayerWord> playerWords = new ArrayList<PlayerWord>();
  private Typeface face;
  
  public WordAdapter(Activity context, ArrayList<PlayerWord>wordList) {
    super();
    this.inflater = LayoutInflater.from(context);
    this.playerWords = wordList;
    this.face=Typeface.createFromAsset(MainActivity.currentInstance.getAssets(), "fonts/font.ttf");
  }

  @Override
  public int getCount() {
    return playerWords.size();
  }

  @Override
  public Object getItem(int item) {
    return playerWords.get(item);
  }

  @Override
  public long getItemId(int position) {
    return position;
  }

  public boolean AreAllItemsEnabled() {
    return false;
  }
  
  public boolean isEnabled(int position) {
    int itemType = this.playerWords.get(position).result;
    return itemType != PlayerWord.RESULT_HEADER;
  }

  @Override
  public View getView(int position, View returnView, ViewGroup parent) {
    returnView = inflater.inflate(R.layout.playerlistitem, null);
    PlayerWord playerWord = this.playerWords.get(position);
    TextView playerWordItem = (TextView)returnView.findViewById(R.id.playerListItem);
    TextView playerWordItemResult = (TextView)returnView.findViewById(R.id.playerListItemResult);
    LinearLayout wordRow = (LinearLayout)returnView.findViewById(R.id.wordRow);
    playerWordItem.setTypeface(this.face);
    playerWordItem.setText(playerWord.word);
    switch(playerWord.result) {
      case PlayerWord.RESULT_INVALID :
        playerWordItemResult.setText("INVALID");
        Spannable text = (Spannable) playerWordItem.getText();
        text.setSpan(new StrikethroughSpan(), 0, text.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
        playerWordItem.setTextColor(0xFFFF0000);
        playerWordItemResult.setTextColor(0xFFFF0000);
        // wordRow.setBackgroundColor(0x90FFB0B0);
        break;
      case PlayerWord.RESULT_MISSED :
        break;
      case PlayerWord.RESULT_OK :
        playerWordItem.setTextColor(0xFF007F00);
        // playerWordItemResult.setText("OK");
        break;
      case PlayerWord.RESULT_HEADER :
        wordRow.setBackgroundColor(0x8FFFFFFF);
        playerWordItem.setText("");
        playerWordItemResult.setText(playerWord.word);
        break;
      default :
        playerWordItemResult.setText("");
      break;
    }
    return returnView;
  }
}
