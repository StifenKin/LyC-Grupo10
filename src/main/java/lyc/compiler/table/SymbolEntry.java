package lyc.compiler.table;

public class SymbolEntry {

  private String name;
  private DataType dataType;
  private String value;
  private String length;

  public SymbolEntry(String name, DataType dataType, String value,
                     String length) {
    this.name = name;
    this.dataType = dataType;
    this.value = value;
    this.length = length;
  }

  public SymbolEntry(String name, DataType dataType) {
    this.name = name;
    this.dataType = dataType;
    this.value = "-";
    this.length = "-";
  }

  public SymbolEntry(String name, DataType dataType, String value) {
    this.name = name;
    this.value = value;
    this.dataType = dataType;
    this.length = "-";
  }

  public SymbolEntry(String name) {
    this.name = name;
    this.value = "-";
    this.length = "-";
  }

  public String getName() { return name; }

  public void setName(String name) { this.name = name; }

  public DataType getDataType() { return dataType; }

  public void setDataType(DataType dataType) { this.dataType = dataType; }

  public String getValue() { return value; }

  public void setValue(String value) { this.value = value; }

  public String getLength() { return length; }

  public void setLength(String length) { this.length = length; }
}
