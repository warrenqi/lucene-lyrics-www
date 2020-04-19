package com.distraction.lyrics;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.function.Function;

import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.LongPoint;
import org.apache.lucene.document.StringField;
import org.apache.lucene.document.TextField;
import org.apache.lucene.index.IndexWriter;

public class SongDoc {

  public static final String PATH = "path";
  public static final Function<String, StringField> PATH_FIELD =
      (filepath) -> new StringField(PATH, filepath, Field.Store.YES);

  public static final String TIMESTAMP = "timestamp";
  public static final Function<Long, LongPoint> TIMESTAMP_FIELD =
      (timestamp) -> new LongPoint(TIMESTAMP, timestamp);

  public static final String CONTENTS = "contents";
  public static final Function<Reader, TextField> CONTENTS_FIELD =
      (reader) -> new TextField(CONTENTS, reader);

  private Path filePath;
  private long timestamp;

  public SongDoc(Path filePath, long timestamp) {
    this.filePath = filePath;
    this.timestamp = timestamp;
  }

  public Path getFilePath() {
    return filePath;
  }

  public long getTimestamp() {
    return timestamp;
  }

  public Document index(IndexWriter writer) throws IOException {
    Document luceneDoc = new Document();

    try (BufferedReader reader = Files.newBufferedReader(this.filePath, StandardCharsets.UTF_8)) {

      luceneDoc.add(PATH_FIELD.apply(this.filePath.toString()));
      luceneDoc.add(TIMESTAMP_FIELD.apply(this.timestamp));
      TextField contents = CONTENTS_FIELD.apply(reader);
      luceneDoc.add(contents);
      writer.addDocument(luceneDoc);
    }
    System.out.println("indexed " + this.filePath.toString());

    return luceneDoc;
  }
}
