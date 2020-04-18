package com.distraction.lyrics;

import java.io.IOException;
import java.nio.file.Path;

import org.apache.commons.lang3.tuple.Triple;
import org.apache.lucene.index.IndexWriter;

public class TopWords {
  public static void main(String[] args) {
    Triple<String, String, Path> triple = IndexLyrics.checkAndSetArgs(args);
    try (IndexWriter writer = IndexLyrics.openIndex(triple.getLeft(), triple.getRight())) {
    } catch (IOException e) {
      e.printStackTrace();
    }
  }
}
