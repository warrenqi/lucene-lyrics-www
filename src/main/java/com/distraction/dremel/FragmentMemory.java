package com.distraction.dremel;

import java.util.concurrent.CountDownLatch;

/** Run with -Xms7g -Xmx7g */
public class FragmentMemory {

  public static void main(String[] args) throws Exception {
    int SIZE = 1_000_000;
    // allocate 4GB
    int[][] mem = alloc(SIZE);

    Thread.sleep(60_000);
    // free up 2GB, but without GC, heap usage should stay 4GB
    fragment(mem);
    Thread.sleep(5_000);
    // allocate 4GB interleaved within mem array for 6GB total
    reallocate(mem);
    CountDownLatch l = new CountDownLatch(1);
    l.await();
  }

  /** Allocate len * 4 KB of memory */
  public static int[][] alloc(int len) {
    long startMs = System.currentTimeMillis();
    int[][] mem = new int[len][];
    int x = 0;
    int PER_SLOT = 1000;
    for (int i = 0; i < len; i++) {
      int[] fourKB = new int[PER_SLOT];
      for (int j = 0; j < PER_SLOT; j++) {
        fourKB[j] = x;
        x++;
      }
      mem[i] = fourKB;
    }
    System.out.println("allocation done in " + (System.currentTimeMillis() - startMs));
    return mem;
  }

  /** Free half of mem */
  public static int[][] fragment(int[][] mem) {
    long startMs = System.currentTimeMillis();
    for (int i = 0; i < mem.length; i += 2) {
      mem[i] = null;
    }
    System.out.println("fragmenting done in " + (System.currentTimeMillis() - startMs));
    return mem;
  }

  /** Reallocate half of mem array with 8 KB per slot */
  public static int[][] reallocate(int[][] mem) {
    long startMs = System.currentTimeMillis();
    int PER_SLOT = 2000;
    int x = 0;
    for (int i = 0; i < mem.length; i += 2) {
      int[] eightKB = new int[PER_SLOT];
      for (int j = 0; j < PER_SLOT; j++) {
        eightKB[j] = x;
        x++;
      }
      mem[i] = eightKB;
    }
    System.out.println("reallocate done in " + (System.currentTimeMillis() - startMs));
    return mem;
  }
}
