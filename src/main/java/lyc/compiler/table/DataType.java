package lyc.compiler.table;

public enum DataType {

  INTEGER_TYPE("Int"),
  FLOAT_TYPE("Float"),
  STRING_TYPE("String"),
  DATECONVERTED_TYPE("DateConverted"),
  INTEGER_CONS("INT_CTE"),
  FLOAT_CONS("FLOAT_CTE"),
  STRING_CONS("STRING_CTE"),
  ID("IDENTIFIER");

  private String name;

  DataType(String name) { this.name = name; }

  public String getName() { return name; }

  public boolean isNumeric() {
    return this == INTEGER_TYPE || this == FLOAT_TYPE;
  }
}
