package com.distraction.dremel;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

import com.google.common.collect.ImmutableList;

public class RecordStripe {

  public static void main(String[] args) {
    System.out.println("per-field depth level treats REQUIRED fields as OPTIONAL");
    System.out.println("record 0 maxes ==== ");
    preProcess(recZero());
    System.out.println("record 1 maxes ==== ");
    Record recOne = recOne();
    preProcess(recOne);
    List<SplitRecord> writer = new ArrayList<RecordStripe.SplitRecord>();
    Set<String> seenFields = new HashSet<>();
    recOne.writeRepDefLevels(0, 0, 0, 0, "", seenFields, writer);
    System.out.println(writer);
  }

  public static Record recZero() {
    Record level2_a = new Record(Multi.OPTIONAL, "level2_a", "value", null);
    Record level2_b = new Record(Multi.OPTIONAL, "level2_b", "value", null);
    Record level1 =
        new Record(Multi.REPEATED, "level1", "value", ImmutableList.of(level2_a, level2_b));
    Record root = new Record(Multi.OPTIONAL, "level0", "value", ImmutableList.of(level1));
    return root;
  }

  // record 1 from the paper
  public static Record recOne() {
    Record level3_enus = new Record(Multi.REQUIRED, "name.lang.code", "en-us", null);
    Record level3_en = new Record(Multi.REQUIRED, "name.lang.code", "en", null);
    Record level3_engb = new Record(Multi.REQUIRED, "name.lang.code", "en-gb", null);
    Record level3_cc_us = new Record(Multi.OPTIONAL, "name.lang.country", "us", null);
    Record level3_code_null = new Record(Multi.REQUIRED, "name.lang.code", null, null);
    Record level3_cc_null = new Record(Multi.OPTIONAL, "name.lang.country", null, null);
    Record level3_cc_gb = new Record(Multi.OPTIONAL, "name.lang.country", "gb", null);

    Record level2_lang1 =
        new Record(
            Multi.REPEATED, "name.lang", "lang1", ImmutableList.of(level3_enus, level3_cc_us));
    Record level2_lang2 =
        new Record(
            Multi.REPEATED, "name.lang", "lang2", ImmutableList.of(level3_en, level3_cc_null));

    Record level2_lang_null =
        new Record(
            Multi.REPEATED,
            "name.lang",
            null,
            ImmutableList.of(level3_code_null, level3_cc_null)); // empty repetition
    Record level2_lang3 =
        new Record(
            Multi.REPEATED, "name.lang", "lang3", ImmutableList.of(level3_engb, level3_cc_gb));

    Record level2_url1 = new Record(Multi.OPTIONAL, "name.url", "http://a", null);
    Record level2_url2 = new Record(Multi.OPTIONAL, "name.url", "http://b", null);
    Record level2_url_null = new Record(Multi.OPTIONAL, "name.url", null, null);

    Record level2_forward1 = new Record(Multi.REPEATED, "links.forward", "20", null);
    Record level2_forward2 = new Record(Multi.REPEATED, "links.forward", "40", null);
    Record level2_forward3 = new Record(Multi.REPEATED, "links.forward", "60", null);

    Record level1_link =
        new Record(
            Multi.OPTIONAL,
            "links",
            "link1",
            ImmutableList.of(level2_forward1, level2_forward2, level2_forward3));
    Record level1_name1 =
        new Record(
            Multi.REPEATED,
            "name",
            "name1",
            ImmutableList.of(level2_lang1, level2_lang2, level2_url1));
    Record level1_name2 =
        new Record(
            Multi.REPEATED, "name", "name2", ImmutableList.of(level2_lang_null, level2_url2));
    Record level1_name3 =
        new Record(
            Multi.REPEATED, "name", "name3", ImmutableList.of(level2_lang3, level2_url_null));

    Record root =
        new Record(
            Multi.OPTIONAL,
            "root",
            "root",
            ImmutableList.of(level1_link, level1_name1, level1_name2, level1_name3));
    return root;
  }

  public static Record recTwo() {
    Record root = new Record(Multi.OPTIONAL, "root", "root", null);
    return root;
  }

  public static void preProcess(Record r) {
    AtomicInteger maxRep = new AtomicInteger();
    AtomicInteger maxDef = new AtomicInteger();

    getMaxes(r, 0, maxDef, 0, maxRep);
    System.out.println("maxDef = " + maxDef);
    System.out.println("maxRep = " + maxRep);
  }

  public static void getMaxes(
      Record r, int defSoFar, AtomicInteger maxDef, int repSoFar, AtomicInteger maxRep) {
    if (r.children != null && !r.children.isEmpty()) {
      for (Record child : r.children) {

        int nextDef = child.type == Multi.REQUIRED ? defSoFar : defSoFar + 1;
        int nextRep = child.type == Multi.REPEATED ? repSoFar + 1 : repSoFar;
        getMaxes(child, nextDef, maxDef, nextRep, maxRep);
      }
    } else {
      maxDef.set(Math.max(maxDef.intValue(), defSoFar));
      maxRep.set(Math.max(maxRep.intValue(), repSoFar));
    }
  }

  // simple representation of proto record
  public static class Record {
    Multi type;
    String field;
    String value;
    List<Record> children;

    public Record(Multi type, String field, String value, List<Record> children) {
      this.type = type;
      this.field = field;
      this.value = value;
      this.children = children;
    }

    /**
     * Traverse a record by DFS
     *
     * <p>Repetition Level:
     *
     * <p>At every depth level: if it's a REPEAT type, increment the rawRep level by 1. If this node
     * is never seen, use the parent level's writtenRepLevel. Otherwise, this node is a repeat at
     * this level, so write the rawRep level.
     *
     * <p>Definition Level:
     *
     * <p>At a group node with children: if this field is not specified and the children are nulls,
     * record current level as last non-null level
     */
    public void writeRepDefLevels(
        int curDef,
        int nonNullDefPlusOne,
        int parentRawRep,
        int parentWrittenRep,
        String parentField,
        Set<String> seenFields,
        List<SplitRecord> writerToOutput) {

      String fullField = parentField + " / " + this.field;
      int rawRep = this.type == Multi.REPEATED ? parentRawRep + 1 : parentRawRep;
      int repToWrite = seenFields.contains(fullField) ? rawRep : parentWrittenRep;
      seenFields.add(fullField);

      if (this.children != null && !this.children.isEmpty()) {
        // a parent node with subgroups
        Set<String> nextLevelSeenFields = new HashSet<>(seenFields);

        // if this field is not specified & children are nulls,
        // then curDef is the last non-null level
        int nextDefLevel = this.value == null ? curDef : 1 + curDef;
        for (Record child : this.children) {
          child.writeRepDefLevels(
              1 + curDef,
              nextDefLevel,
              rawRep,
              repToWrite,
              parentField + " / " + this.field,
              nextLevelSeenFields,
              writerToOutput);
        }
      } else {
        // a value node
        String val = "NULL";
        int defLevel = curDef;
        if (this.value == null) {
          /**
           * if the parent level had value != null, it passed 1+curDef else, the parent level was
           * null and its curDef was already 1 more than the actual nonNull level
           */
          defLevel = nonNullDefPlusOne - 1;
        } else {
          val = this.value;
        }

        writerToOutput.add(new SplitRecord(fullField, val, repToWrite, defLevel));
      }
    }
  }

  public static enum Multi {
    REQUIRED, // not used in proto3 but included here for completeness
    OPTIONAL,
    REPEATED;
  }

  public static class SplitRecord {
    public SplitRecord(String field, String value, int rep, int def) {
      this.field = field;
      this.value = value;
      this.rep = rep;
      this.def = def;
    }

    @Override
    public String toString() {
      return "\n[r=" + rep + ", d=" + def + " : " + field + " = " + value + "]";
    }

    String field;
    String value;
    int rep;
    int def;
  }
}
