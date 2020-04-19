package com.distraction.lyrics;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.PriorityQueue;

import org.apache.commons.lang3.tuple.Triple;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.LeafReader;
import org.apache.lucene.index.LeafReaderContext;
import org.apache.lucene.index.Terms;
import org.apache.lucene.index.TermsEnum;
import org.apache.lucene.queryparser.classic.ParseException;
import org.apache.lucene.queryparser.classic.QueryParser;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.TopDocs;
import org.apache.lucene.store.FSDirectory;
import org.apache.lucene.util.BytesRef;

public class TopWords {

  public static void main(String[] args) {

    Triple<String, String, Path> setup = IndexLyrics.checkAndSetArgs(args);
    TopWords tw = new TopWords(setup.getLeft(), setup.getRight(), 10, 3);
    System.out.println("Top words by TFIDF \n" + tw.getTopWords());

    for (String word : tw.getTopWords()) {
      System.out.println("topword = " + word + "\t links = " + tw.getTopWordToPath().get(word));
    }
  }

  private final Map<String, List<String>> topWordToPath = new HashMap<>();
  private final List<String> topWords = new ArrayList<>();

  public TopWords(String indexDirectory, Path documents, int topWordsCount, int linksPerWord) {
    try (IndexWriter writer = IndexLyrics.createIndex(indexDirectory, documents)) {
      writer.commit();

      IndexReader reader = DirectoryReader.open(FSDirectory.open(Paths.get(indexDirectory)));
      this.topWords.addAll(topWordsTfidf(reader, topWordsCount));

      IndexSearcher searcher = new IndexSearcher(reader);
      Analyzer analyzer = new StandardAnalyzer();
      QueryParser parser = new QueryParser(SongDoc.CONTENTS, analyzer);
      for (String topword : this.topWords) {

        List<String> searchResults = searchFiles(searcher, parser, topword, linksPerWord);
        this.topWordToPath.put(topword, searchResults);
      }

    } catch (IOException e) {
      e.printStackTrace();
    }
  }

  public List<String> getTopWords() {
    return this.topWords;
  }

  public Map<String, List<String>> getTopWordToPath() {
    return this.topWordToPath;
  }

  static List<String> searchFiles(
      IndexSearcher searcher, QueryParser parser, String keyword, int limit) {

    List<String> resultPaths = new ArrayList<>();
    try {

      Query query = parser.parse(keyword);
      TopDocs results = searcher.search(query, limit);

      for (ScoreDoc hit : results.scoreDocs) {
        Document luceneDoc = searcher.doc(hit.doc);
        resultPaths.add(luceneDoc.get(SongDoc.PATH));
      }

    } catch (IOException e) {
      e.printStackTrace();
    } catch (ParseException e) {
      e.printStackTrace();
    }

    return resultPaths;
  }

  static List<String> topWordsTfidf(IndexReader reader, int k) throws IOException {
    List<Terms> allTermsFromReaders = new ArrayList<Terms>();
    List<LeafReaderContext> contexts = reader.leaves();
    int sumDocCount = 0;
    long sumTermOccurenceCount = 0L;

    // collect corpus counts
    for (LeafReaderContext leafCtx : contexts) {

      LeafReader leafReader = leafCtx.reader();
      Terms contentTerms = leafReader.terms(SongDoc.CONTENTS);
      allTermsFromReaders.add(contentTerms);
      sumDocCount += leafReader.getDocCount(SongDoc.CONTENTS);
      sumTermOccurenceCount += leafReader.getSumTotalTermFreq(SongDoc.CONTENTS);
    }
    System.out.println("total number of leafReaders = " + contexts.size());
    System.out.println("total number of docs = " + sumDocCount);
    System.out.println("total TermOccurence count (word count) = " + sumTermOccurenceCount);

    Map<String, Integer> countOfDocsWithTerm = new HashMap<>();
    Map<String, Long> termOccurenceCount = new HashMap<>();

    // collect stats for each term, shove into Maps
    for (Terms terms : allTermsFromReaders) {

      TermsEnum termIter = terms.iterator();
      BytesRef text;

      while ((text = termIter.next()) != null) {

        countOfDocsWithTerm.merge(text.utf8ToString(), termIter.docFreq(), Integer::sum);
        termOccurenceCount.merge(text.utf8ToString(), termIter.totalTermFreq(), Long::sum);
      }
    }
    Map<String, Float> tfidf = new HashMap<>();

    // finally calculate
    for (String term : termOccurenceCount.keySet()) {

      float termFreq = (float) termOccurenceCount.get(term) / sumTermOccurenceCount;
      float docFreq = (float) countOfDocsWithTerm.get(term) / sumDocCount;
      float idf = (float) Math.log10(1.0F / docFreq);
      tfidf.put(term, termFreq * idf);
      // System.out.println("term:" + term + "\t tf=" + termFreq + "\t idf=" + idf );
    }

    PriorityQueue<Entry<String, Float>> queue =
        new PriorityQueue<Entry<String, Float>>(
            new Comparator<Entry<String, Float>>() {
              @Override
              public int compare(Entry<String, Float> o1, Entry<String, Float> o2) {
                return (-1) * Float.compare(o1.getValue(), o2.getValue());
              }
            });
    queue.addAll(tfidf.entrySet());

    List<String> result = new ArrayList<>();
    int count = 0;
    while (queue.peek() != null && count < k) {
      result.add(queue.remove().getKey());
      count++;
    }
    return result;
  }
}
