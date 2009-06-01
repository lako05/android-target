/*
 * SMH Import
 * 
 * Imports target games from the Sydney Morning Herald website.
 * 
 * http://www.smh.com.au/entertainment/puzzles/target.html
 */
package net.cactii.target;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.StatusLine;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;

import android.os.Message;
import android.util.Log;

public class SmhImport implements Runnable {
  // URL for the puzzle
  private static final String SMH_PUZZLE_URL = "http://www.smh.com.au/entertainment/puzzles/";
  private static final String SMH_TARGET_URL = "/target.html";
  
  // Contains URLS of previous puzzles (Strings are dates, eg "2009/05/19")
  public ArrayList<String> pastPuzzleUrls = null;
  // Contains the letters of the current puzzle, L-R T-B.
  public String currentPuzzleLetters = null;
  // Fill this in with a 'pastPuzzleUrl' to fetch other than todays
  public String fetchPuzzleDate = "";
  
  public void run() {
    GetSmhPuzzle();
    Message msg = Message.obtain();
    msg.what = DictionaryThread.MESSAGE_HAVE_SMH_NINELETTER;
    msg.obj = this.currentPuzzleLetters;
    DictionaryThread.currentInstance.messageHandler.sendMessage(msg);
  }

  private void GetSmhPuzzle() {
    this.pastPuzzleUrls = new ArrayList<String>();
    this.currentPuzzleLetters = new String();
    String pageContent;
    pageContent = FetchPage(SMH_PUZZLE_URL + this.fetchPuzzleDate + SMH_TARGET_URL);

    Log.d("TargetSMH", "Fetched " + pageContent.length() + " bytes.");
    for (String line : pageContent.split("\n")) {
      if (line.contains("size=\"+4\"")) {
        int sizeIndex = line.indexOf("size=\"+4\"");
        this.currentPuzzleLetters = this.currentPuzzleLetters.concat(
          line.substring(sizeIndex+10, sizeIndex+11));
      }
      if (line.contains("entertainment/puzzles/") &&
          line.contains("/target.html")) {
        int hrefIndex = line.indexOf("a href=\"/entertainment");
        this.pastPuzzleUrls.add(line.substring(hrefIndex+31, hrefIndex+41));
      }
    }
    Log.d("TargetSMH", "Todays puzzle: " + this.currentPuzzleLetters);
    for (String pastPuzzle : this.pastPuzzleUrls)
      Log.d("TargetSMH", "Past puzzle: " + pastPuzzle);
  }
  
  private String FetchPage(String url) {
    HttpClient client = new DefaultHttpClient();
    HttpGet request = new HttpGet(String.format(url));
    String pageContent = "";
    HttpResponse response;
    try {
      response = client.execute(request);
      StatusLine status = response.getStatusLine();
      Log.d("Target", "Request returned status " + status);
      if (status.getStatusCode() == 200) {
        HttpEntity entity = response.getEntity();
        InputStream instream = entity.getContent();
        byte buf[] = new byte[8192];
        while(instream.read(buf) > 0) {
          pageContent = pageContent.concat(new String(buf));
        }
      }
    } catch (ClientProtocolException e) {
      e.printStackTrace();
    } catch (IOException e) {
      e.printStackTrace();
    }
    return pageContent;
  }
}
