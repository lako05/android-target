package net.cactii.target;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Typeface;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnTouchListener;

public class TargetGridView extends View implements OnTouchListener {

  public LetterTouchedHandler _letterTouchedHandler = null;

  // Initialise some colours
  private static final int backgroundColor = 0x00FFFFFF;
  private static final int gridColor = Color.BLACK;
  private static final int centerBackgroundColor = Color.BLACK;
  private static final int centerLetterColor = Color.WHITE;
  private static final int letterColor = Color.BLACK;
  private static final int letterHighlightColor = 0x90FFFF00;

  // Set paint objects
  private Paint backgroundPaint;		// Overall background
  private Paint gridPaint;			// Grid lines
  private Paint letterPaint;			// Outside letters
  private Paint centerLetterPaint;	// Center letter
  private Paint centerPaint;			// Center background
  private Paint highlightPaint;		// Square highlight

  // An array to indicate which letters are displayed as highlighted
  private boolean[] highlights = {
      false, false, false, false, false,
      false, false, false, false};

  private int[] selectedword = new int[] {-1, -1, -1, -1, -1, -1, -1, -1, -1};

  // Variable to indicate the letters on the grid.
  private String letters = "";

  public boolean gameActive = false;

  public TargetGridView(Context context) {
    super(context);
    initTargetView();
  }
  public TargetGridView(Context context, AttributeSet attrs) {
    super(context, attrs);
    initTargetView();
  }
  public TargetGridView(Context context, AttributeSet attrs, int defStyle) {
    super(context, attrs, defStyle);
    initTargetView();
  }
  protected void initTargetView() {
    setFocusable(true);

    letters = "";
    gridPaint = new Paint();
    gridPaint.setColor(gridColor);
    gridPaint.setStrokeWidth(5);

    backgroundPaint = new Paint();
    backgroundPaint.setColor(backgroundColor);

    letterPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    letterPaint.setColor(letterColor);
    letterPaint.setTextSize(32);
    letterPaint.setTypeface(Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD));

    centerPaint = new Paint();
    centerPaint.setColor(centerBackgroundColor);

    centerLetterPaint = new Paint();
    centerLetterPaint.setColor(centerLetterColor);
    centerLetterPaint.setTextSize(32);
    centerLetterPaint.setTypeface(Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD));

    highlightPaint = new Paint();
    highlightPaint.setColor(letterHighlightColor);

    this.setOnTouchListener((OnTouchListener) this);
    this.gameActive = false;
  }
  @Override
  protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
    // Our target grid is a square, measuring 80% of the minimum dimension
    int measuredWidth = measure(widthMeasureSpec);
    int measuredHeight = measure(heightMeasureSpec);

    int dim = Math.min(measuredWidth, measuredHeight);

    setMeasuredDimension(dim, dim);
  }
  private int measure(int measureSpec) {

    int specMode = MeasureSpec.getMode(measureSpec);
    int specSize = MeasureSpec.getSize(measureSpec);

    if (specMode == MeasureSpec.UNSPECIFIED)
      return 180;
    else
      return (int)(specSize * 0.8);
  }

  @Override
  protected void onDraw(Canvas canvas) {
    int width = getMeasuredWidth();
    int height = getMeasuredHeight();

    canvas.drawARGB(0, 255, 255, 255);

    if (letters != "")
      for (int index = 0 ; index < 9 ; index++)
        drawLetter(canvas, index, highlights[index]);

    for (float x = 0 ; x <= width ; x += width/3)
      canvas.drawLine(x, 0, x, height, gridPaint);

    for (float y = 0 ; y <= height ; y += height/3)
      canvas.drawLine(0, y, width, y, gridPaint);

  }

  // Draws a single letter in the grid, with appropriate highlight
  protected void drawLetter(Canvas canvas, int index, boolean highlighted) {
    Paint textPaint;
    Paint squarePaint;

    int size = getMeasuredWidth(); // Measure one as its a square

    String letter = letters.substring(index, index+1);
    float squareLeft = (index % 3) * size/3;
    float squareTop = (float) Math.floor(index/3) * size/3;
    float squareSize = size/3;
    float letterWidth = letterPaint.measureText(letter);
    float letterHeight = letterPaint.ascent();

    if (!highlighted) {
      if (index == 4) {
        textPaint = centerLetterPaint;
        squarePaint = centerPaint;
      } else {
        textPaint = letterPaint;
        squarePaint = backgroundPaint;
      }
    } else {
      squarePaint = highlightPaint;
      textPaint = letterPaint;
    }

    canvas.drawRect(squareLeft, squareTop,
        squareLeft + squareSize, squareTop + squareSize, squarePaint);
    canvas.drawText(letter,
        squareLeft + squareSize/2 - letterWidth/2,
        squareTop + squareSize/2 - letterHeight/2, textPaint);
  }

  // Supplies a new word to the grid.
  // Words that arent 9 letters are ignored.
  public void setLetters(String word) {
    if (word.length() == 9) {
      letters = word;
      clearGrid(); // Calls invalidate() for us.
    }
  }

  // Clears (unhighlights) the most recently selected letter from the grid.
  public void clearLastLetter() {
    int gridIndex;
    for (int i = 8 ; i >= 0 ; i--) {
      gridIndex = selectedword[i];
      if (gridIndex != -1) {
        highlights[gridIndex] = false;
        selectedword[i] = -1;
        invalidate();
        return;
      }
    }
  }

  // Unhighlights the entire grid
  public void clearGrid() {
    highlights = new boolean[] {false, false, false, false,
        false, false, false, false, false};
    selectedword = new int[] {-1, -1, -1, -1, -1, -1, -1, -1, -1};
    invalidate();
  }

  // Returns the string of the currently tapped out word
  public String getSelectedWord() {
    String word = "";
    int gridIndex;
    for (int i = 0 ; i < 9 ; i++) {
      gridIndex = selectedword[i];
      if (gridIndex > -1)
        word += letters.substring(gridIndex, gridIndex+1);
      else
        return word;
    }

    return word;
  }

  public void setLetterTouchedListener(LetterTouchedHandler handler) {
    _letterTouchedHandler = handler;
  }

  // Handles touch events to the grid.
  //
  // Marks the letter to be highlighted, and updates the
  // ordered list of selected letters.
  //
  // Finally calls the LetterTouchedHandler for further actions.
  public boolean onTouch(View v, MotionEvent event) {
    boolean handled = false;
    int index = 0;
    if (gameActive == false)
      return true;
    switch(event.getAction()) {
    case MotionEvent.ACTION_DOWN : {
      index = eventToLetterIndex(event);
      if (highlights[index])
        return true; // return if letter already highlighted
      highlights[index] = true;
      handled = true;
      invalidate();
      for (int i = 0 ; i < 9 ; i++) {
        if (selectedword[i] == -1) {
          selectedword[i] = index;
          break;
        }
      }
      // Log.d("Target", "Selectedword: " + selectedword);
      if (_letterTouchedHandler != null)
        _letterTouchedHandler.handleLetterTouched(index);
    }
    }
    return handled;
  }

  // Takes an onTouch event and returns the grid index of the touched letter.
  private int eventToLetterIndex(MotionEvent event) {
    float x = event.getX();
    float y = event.getY();
    int size = getMeasuredWidth(); // Measure one side only as its a square

    int row = (int)((size - (size-y))/(size/3));
    if (row > 2) row = 2;
    if (row < 0) row = 0;

    int col = (int)((size - (size-x))/(size/3));
    if (col > 2) col = 2;
    if (col < 0) col = 0;

    int index = row*3 + col;

    // Log.d("Target", "Row " + row + ", col " + col + ", index " + index);
    return index;
  }
}
