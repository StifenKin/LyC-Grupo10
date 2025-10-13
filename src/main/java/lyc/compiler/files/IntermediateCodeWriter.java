package lyc.compiler.files;

import java.io.FileWriter;
import java.io.IOException;

public class IntermediateCodeWriter implements FileGenerator {

  @Override
  public void generate(FileWriter fileWriter) throws IOException {
    fileWriter.write(
        String.format("%-6s %-20s %-20s %-20s%n", "IDX", "OP", "ARG1", "ARG2"));
    System.out.println(IntermediateCodeGenerator.getTriplets());
    for (Triplet t : IntermediateCodeGenerator.getTriplets()) {
      fileWriter.write(String.format("%-6s %-20s %-20s %-20s%n",
                                     "[" + t.getIndex() + "]", t.getOperator(),
                                     t.getArg1(), t.getArg2()));
    }
  }
}
