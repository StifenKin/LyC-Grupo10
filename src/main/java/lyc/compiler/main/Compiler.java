package lyc.compiler.main;

import java.io.IOException;
import java.io.Reader;
import lyc.compiler.Parser;
import lyc.compiler.factories.FileFactory;
import lyc.compiler.factories.ParserFactory;
import lyc.compiler.files.FileOutputWriter;
import lyc.compiler.files.IntermediateCodeGenerator;
import lyc.compiler.files.IntermediateCodeFileGenerator;
import lyc.compiler.files.SymbolTableGenerator;
import lyc.compiler.files.TypeTable;
import lyc.compiler.table.SymbolTableManager;

public final class Compiler {

  private Compiler() {}

  public static void main(String[] args) {
    if (args.length != 1) {
      System.out.println("Filename must be provided as argument.");
      System.exit(0);
    }

    try (Reader reader = FileFactory.create(args[0])) {
      // Resetear las tablas antes de compilar
      IntermediateCodeGenerator.reset();
      TypeTable.reset();

      Parser parser = ParserFactory.create(reader);
      parser.parse();

      // Generar los archivos requeridos para la segunda entrega
      FileOutputWriter.writeOutput("symbol-table.txt", new SymbolTableGenerator());
      FileOutputWriter.writeOutput("intermediate-code.txt", new IntermediateCodeFileGenerator());

      System.out.println("Archivos generados:");
      System.out.println("- symbol-table.txt: Tabla de símbolos");
      System.out.println("- intermediate-code.txt: Código intermedio (tercetos)");

    } catch (IOException e) {
      System.err.println("There was an error trying to read input file " + e.getMessage());
      System.exit(0);
    } catch (Exception e) {
      System.err.println("Compilation error: " + e.getMessage());
      e.printStackTrace();
      System.exit(0);
    }

    System.out.println("Compilation Successful");
  }
}
