package lyc.compiler.files;

import java.io.FileWriter;
import java.io.IOException;
import java.util.Map;
import lyc.compiler.table.SymbolEntry;
import lyc.compiler.table.SymbolTableManager;

public class SymbolTableGenerator implements FileGenerator {

  @Override
  public void generate(FileWriter fileWriter) throws IOException {
    fileWriter.write(String.format("%-20s %-15s %-40s %-6s%n", "LEXEME", "TYPE",
                                   "VALUE", "LENGTH"));

    for (Map.Entry<String, SymbolEntry> e :
         SymbolTableManager.getSymbolTable().entrySet()) {
      String lexeme = e.getKey();
      SymbolEntry entry = e.getValue();

      String type = safe(entry.getDataType());
      String value = safe(entry.getValue());
      String length = safe(entry.getLength());

      fileWriter.write(String.format("%-20s %-15s %-40s %-6s%n", lexeme, type,
                                     value, length));
    }
  }

  private String safe(Object o) { return o == null ? "-" : o.toString(); }
}