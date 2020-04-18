package com.distraction.lyrics;

import java.util.function.Function;

import org.apache.lucene.document.Field;
import org.apache.lucene.document.StringField;

public class SongDoc {
  public static final String PATH = "path";
  public static final Function<String, StringField> PATH_FIELD =
      (filepath) -> new StringField(PATH, filepath, Field.Store.YES);
}
