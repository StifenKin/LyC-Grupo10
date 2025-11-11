package lyc.compiler.table;

import java.util.HashMap;

public class SymbolTableManager {

  private static HashMap<String, SymbolEntry> symbolTable = new HashMap<>();

  public static HashMap<String, SymbolEntry> getSymbolTable() {
    return symbolTable;
  }

  public static void insertInTable(SymbolEntry entry) {
    // Validar que el nombre no sea nulo ni vacío
    if (entry == null || entry.getName() == null || entry.getName().trim().isEmpty()) {
      System.out.println("Warning: Attempting to insert symbol with null or empty name - ignored");
      return;
    }
    symbolTable.put(entry.getName().replace(" ", "_"), entry);
  }

  public static SymbolEntry get(String entryName) {
    return symbolTable.get(entryName);
  }

  public static void setDataTypeInTable(String key, DataType dataType) {
    SymbolEntry entry = symbolTable.get(key);
    entry.setDataType(dataType);
  }

  public static void removeFromTable(String entryName) {
    // Reemplazar espacios por guiones bajos para mantener coherencia con
    // insertInTable

    if (symbolTable.containsKey(entryName)) {
      symbolTable.remove(entryName);
      System.out.println("Removed symbol: " + entryName);
    } else {
      System.out.println("Symbol not found: " + entryName);
    }
  }

  public static boolean existsInTable(String entryName) {
    return symbolTable.containsKey(entryName);
  }
}
