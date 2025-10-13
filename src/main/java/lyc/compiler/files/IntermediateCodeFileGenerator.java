package lyc.compiler.files;

import java.io.FileWriter;
import java.io.IOException;
import java.util.List;

public class IntermediateCodeFileGenerator implements FileGenerator {

  @Override
  public void generate(FileWriter fileWriter) throws IOException {
    List<Triplet> triplets = IntermediateCodeGenerator.getTriplets();

    fileWriter.write("CODIGO INTERMEDIO - TERCETOS\n");
    fileWriter.write("===============================\n\n");

    for (Triplet triplet : triplets) {
      fileWriter.write(triplet.toString() + "\n");
    }

    if (triplets.isEmpty()) {
      fileWriter.write("No se generó código intermedio.\n");
    }
  }
}
