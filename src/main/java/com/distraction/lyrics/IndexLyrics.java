package com.distraction.lyrics;

import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.concurrent.TimeUnit;

import org.apache.commons.lang3.tuple.ImmutableTriple;
import org.apache.commons.lang3.tuple.Triple;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.index.IndexWriterConfig.OpenMode;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;

/** Simple modifications from Lucene demo */
public class IndexLyrics {

  private IndexLyrics() {}

  /**
   * @param args
   * @return left = indexDirectory string <br>
   *     middle = documentsDirectory string <br>
   *     right = documents as Path <br>
   */
  public static Triple<String, String, Path> checkAndSetArgs(String[] args) {
    String usage =
        "java org.apache.lucene.demo.IndexFiles"
            + " [-index INDEX_PATH] [-docs DOCS_PATH] \n\n"
            + "This indexes the documents in DOCS_PATH, creating a Lucene index"
            + "in INDEX_PATH that can be searched with SearchFiles";

    String docsDir = null;
    String indexDir = "index";

    for (int i = 0; i < args.length; i++) {

      if ("-index".equals(args[i])) {
        indexDir = args[i + 1];
        i++;
      } else if ("-docs".equals(args[i])) {
        docsDir = args[i + 1];
        i++;
      }
    }
    if (docsDir == null) {
      System.err.println("Usage: " + usage);
      System.exit(1);
    }
    Path docsPath = Paths.get(docsDir);
    if (!Files.isReadable(docsPath)) {
      System.out.println(
          "Document directory '"
              + docsPath.toAbsolutePath()
              + "' does not exist or is not readable, please check the path");
      System.exit(1);
    }
    return new ImmutableTriple<String, String, Path>(indexDir, docsDir, docsPath);
  }

  /** Index all text files under a directory. */
  public static void main(String[] args) {
    Triple<String, String, Path> setup = checkAndSetArgs(args);

    try (IndexWriter writer = createIndex(setup.getLeft(), setup.getRight())) {

      int numDocs = writer.getDocStats().numDocs;
      System.out.println("numDocs = " + numDocs);

    } catch (IOException e) {
      System.out.println(" caught a " + e.getClass() + "\n with message: " + e.getMessage());
    }
  }

  public static IndexWriter createIndex(String indexDir, Path docsPath) throws IOException {

    System.out.println("Indexing to directory '" + indexDir + "'...");
    Directory dir = FSDirectory.open(Paths.get(indexDir));
    Analyzer analyzer = new StandardAnalyzer();
    IndexWriterConfig iwConfig = new IndexWriterConfig(analyzer);

    // removes previous index
    iwConfig.setOpenMode(OpenMode.CREATE);
    IndexWriter writer = new IndexWriter(dir, iwConfig);
    indexDocs(writer, docsPath);
    return writer;
  }

  /**
   * Indexes the given file using the given writer, or if a directory is given, recurses over files
   * and directories found under the given directory.
   *
   * @param writer Writer to the index where the given file/dir info will be stored
   * @param path The file to index, or the directory to recurse into to find files to index
   * @throws IOException If there is a low-level I/O error
   */
  static void indexDocs(final IndexWriter writer, Path path) throws IOException {

    if (Files.isDirectory(path)) {

      Files.walkFileTree(
          path,
          new SimpleFileVisitor<Path>() {
            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs)
                throws IOException {

              try {
                SongDoc doc = new SongDoc(file, attrs.lastModifiedTime().to(TimeUnit.SECONDS));
                doc.index(writer);

              } catch (IOException ignore) {
                System.out.println("exception indexing " + file.toString());
              }
              return FileVisitResult.CONTINUE;
            }
          });
    } else {

      SongDoc doc = new SongDoc(path, Files.getLastModifiedTime(path).to(TimeUnit.SECONDS));
      doc.index(writer);
    }
  }
}
