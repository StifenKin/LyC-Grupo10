package lyc.compiler.files;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import lyc.compiler.table.DataType;

public class IntermediateCodeGenerator {
  private static final List<Triplet> triplets = new ArrayList<>();
  private static int currentIndex = 1;
  private static int labelCounter = 1;

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

  // Obtener el índice actual (para referencias futuras)
  public static int getCurrentIndex() {
    return currentIndex;
  }

  // Generar un nuevo label único
  public static String generateLabel() {
    return "L" + labelCounter++;
  }

  // Crear un terceto de label
  public static Triplet createLabel(String labelName) {
    return add(Operator.LABEL, labelName, null);
  }

  // Obtener el operador de salto complementario (negado)
  public static Operator getNegatedJump(String comparator) {
    switch (comparator) {
      case "==": return Operator.BNE;  // si es ==, saltar si NOT equal
      case "!=": return Operator.BEQ;  // si es !=, saltar si equal
      case "<":  return Operator.BGE;  // si es <, saltar si >=
      case "<=": return Operator.BGT;  // si es <=, saltar si >
      case ">":  return Operator.BLE;  // si es >, saltar si <=
      case ">=": return Operator.BLT;  // si es >=, saltar si <
      default: throw new IllegalArgumentException("Operador desconocido: " + comparator);
    }
  }

  // Generar comparación completa: operandos + CMP + salto negado
  // Retorna el índice del terceto de salto (para poder completarlo después)
  public static int createComparison(int expr1Idx, int expr2Idx, String comparator, String targetLabel) {
    // CMP entre los dos índices de expresiones
    createTriplet(Operator.CMP, expr1Idx, expr2Idx);

    // Salto con el operador complementario
    Operator jumpOp = getNegatedJump(comparator);
    Triplet jumpTriplet = createTriplet(jumpOp, targetLabel, (String)null);

    return jumpTriplet.getIndex();
  }

  // Completar un terceto de salto con el label destino
  public static void updateJumpTarget(int tripletIndex, String targetLabel) {
    if (tripletIndex > 0 && tripletIndex <= triplets.size()) {
      Triplet t = triplets.get(tripletIndex - 1);
      // Crear un nuevo terceto con el label actualizado
      triplets.set(tripletIndex - 1,
        new Triplet(t.getIndex(), getOperatorFromString(t.getOperator()), targetLabel, null));
    }
  }

  // Método auxiliar para obtener Operator desde string
  private static Operator getOperatorFromString(String op) {
    for (Operator operator : Operator.values()) {
      if (operator.getDisplay().equals(op)) {
        return operator;
      }
    }
    throw new IllegalArgumentException("Operador no encontrado: " + op);
  }

  public static List<Triplet> getTriplets() {
    return Collections.unmodifiableList(triplets);
  }

  public static void reset() {
    triplets.clear();
    currentIndex = 1;
    labelCounter = 1;
  }

  public static void printAll() {
    for (Triplet t : triplets) {
      System.out.println(t);
    }
  }
}