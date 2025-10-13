package lyc.compiler.files;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import lyc.compiler.table.DataType;

public class IntermediateCodeGenerator {
  private static final List<Triplet> triplets = new ArrayList<>();
  private static int currentIndex = 1;

  private static String ref(int idx) { return "ref:" + idx; }

  private static Triplet add(Operator op, String a1, String a2) {
    Triplet t = new Triplet(currentIndex++, op, a1, a2);
    triplets.add(t);
    return t;
  }

  public static Triplet createTriplet(Operator op, String arg1, String arg2) {
    return add(op, arg1, arg2);
  }
  public static Triplet createTriplet(Operator op, String arg1) {
    return add(op, arg1, null);
  }
  public static Triplet createTriplet(Operator op, int index) {
    return add(op, ref(index), null);
  }
  public static Triplet createTriplet(Operator op, int index1, int index2) {
    return add(op, ref(index1), ref(index2));
  }

  public static Triplet createTriplet(Operator op, String arg, int index) {
    return add(op, arg, ref(index));
  }

  public static Triplet createTriplet(Operator op, String arg, DataType type) {
    return add(op, arg, type.getName());
  }

  public static List<Triplet> getTriplets() {
    return Collections.unmodifiableList(triplets);
  }

  public static void reset() {
    triplets.clear();
    currentIndex = 1;
  }

  public static void printAll() {
    for (Triplet t : triplets) {
      System.out.println(t);
    }
  }
}