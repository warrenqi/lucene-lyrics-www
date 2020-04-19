package com.distraction.lyrics;

import java.nio.file.Path;

import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang3.tuple.Triple;

public class TopWordsJekyllBlog {

  public static String getHref(int resultDocId, String resultFilePath) {

    String PRE = "<a href=\"{{ site.baseurl }}{% post_url ";
    String CLOSE = " %}\">[";
    String POST = "]</a>";

    return PRE + FilenameUtils.getBaseName(resultFilePath) + CLOSE + resultDocId + POST;
  }

  public static String boldText(String word) {
    String PRE = "<b>";
    String POST = ":</b>";
    return PRE + word + POST;
  }

  public static void main(String[] args) {
    Triple<String, String, Path> setup = IndexLyrics.checkAndSetArgs(args);
    TopWords tw = new TopWords(setup.getLeft(), setup.getRight(), 7, 3);

    for (String word : tw.getTopWords()) {

      System.out.println(boldText(word));
      int i = 1;
      for (String fullPath : tw.getTopWordToPath().get(word)) {

        System.out.println(getHref(i, fullPath));
        i++;
      }
    }
  }
}
