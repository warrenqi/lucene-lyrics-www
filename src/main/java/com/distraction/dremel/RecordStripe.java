package com.distraction.dremel;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

import com.google.common.collect.ImmutableList;

public class RecordStripe {

  public static void main(String[] args) {
    System.out.println("run 0 ==== ");
    preProcess(recZero());
    System.out.println("run 1 ==== ");
    Record recOne = recOne();
    preProcess(recOne);
    List<SplitRecord> writer = new ArrayList<RecordStripe.SplitRecord>();
    Set<String> seenFields = new HashSet<>();
    recOne.writeDefLevel(0, 0, 0, "", seenFields, writer);
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

  public static Record recOne() {
    Record level3_enus = new Record(Multi.REQUIRED, "name.lang.code", "en-us", null);
    Record level3_en = new Record(Multi.REQUIRED, "name.lang.code", "en", null);
    Record level3_engb = new Record(Multi.REQUIRED, "name.lang.code", "en-gb", null);
    Record level3_cc_us = new Record(Multi.OPTIONAL, "name.lang.country", "us", null);
    Record level3_code_null =
        new Record(Multi.REQUIRED, "name.lang.code", null, null); // treat as OPTIONAL
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

    // assume: no special treatment of required fields
    public void writeDefLevel(
        int curDef,
        int nonNullDefPlusOne,
        int curRep,
        String parentField,
        Set<String> seenFields,
        List<SplitRecord> writerToOutput) {

      int myRep = this.type == Multi.REPEATED ? curRep + 1 : curRep;

      if (this.children != null && !this.children.isEmpty()) {
        // a parent node with subgroups

        if (this.value == null) {
          // this field is not specified and the children are nulls. record current level as last
          // non-null level
          for (Record child : this.children) {
            child.writeDefLevel(
                1 + curDef,
                curDef,
                myRep,
                parentField + "_" + this.field,
                seenFields,
                writerToOutput);
          }
        } else {
          for (Record child : this.children) {
            child.writeDefLevel(
                1 + curDef,
                1 + curDef,
                myRep,
                parentField + "_" + this.field,
                seenFields,
                writerToOutput);
          }
        }

      } else {
        // a value node
        String val = "NULL";
        int defLevel = curDef;
        String fullField = parentField + "_" + this.field;
        int repLevelToWrite = seenFields.contains(fullField) ? myRep : 0;
        seenFields.add(fullField);
        if (this.value == null) {
          // if the parent level had value != null, it passed 1+curDef
          // else, the parent level was null and its curDef was already 1 more than the actual
          // nonNull level
          defLevel = nonNullDefPlusOne - 1;
          repLevelToWrite = defLevel;
        } else {
          val = this.value;
        }

        writerToOutput.add(new SplitRecord(fullField, val, repLevelToWrite, defLevel));
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
      return "[r=" + rep + ", d=" + def + " : " + field + " = " + value + "]\n";
    }

    String field;
    String value;
    int rep;
    int def;
  }
}
