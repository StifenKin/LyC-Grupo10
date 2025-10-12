package lyc.compiler.files;

public class Triplet {
  private final int index;
  private final Operator operator;
  private final String arg1;
  private final String arg2;

  public Triplet(int index, Operator operator, String arg1, String arg2) {
    this.index = index;
    this.operator = operator;
    this.arg1 = arg1;
    this.arg2 = arg2;
  }

  public int getIndex() { return index; }

  public String getOperator() { return operator.getDisplay(); }

  public String getArg1() { return arg1; }

  public String getArg2() { return arg2; }

  private static String formatArg(String arg) {

    if (arg == null)
      return "-";
    if (arg.startsWith("ref:"))
      return "[" + arg.substring(4) + "]";
    return arg;
  }

  @Override
  public String toString() {
    return "[" + index + "] (" + operator + ", " + formatArg(arg1) + ", " +
        formatArg(arg2) + ")";
  }
}