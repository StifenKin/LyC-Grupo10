package lyc.compiler.files;

import java.util.HashMap;
import java.util.Map;
import lyc.compiler.table.DataType;

public class TypeTable {

  private static Map<Integer, DataType> typeMap = new HashMap<>();

  public static DataType getType(int index) { return typeMap.get(index); }

  public static void putType(int index, DataType type) {
    typeMap.put(index, type);
  }

  // Verifica si dos tipos son compatibles para una operación dada
  public static boolean areCompatible(DataType t1, DataType t2, Operator op) {
    switch (op) {
    case ADD:
    case SUB:
    case MUL:
    case DIV:
      // Solo números: int y float
      return t1.isNumeric() && t2.isNumeric();
    case NEG:
      // unario → solo uno debe ser numérico
      return t1 != null && t1.isNumeric();

    default:
      return false;
    }
  }

  // Devuelve el tipo resultante de una operación binaria
  public static DataType resultType(DataType t1, DataType t2, Operator op) {
    switch (op) {
    case ADD:
    case SUB:
    case MUL:
    case DIV:
      // float + int → float
      if (t1 == DataType.FLOAT_TYPE || t2 == DataType.FLOAT_TYPE)
        return DataType.FLOAT_TYPE;
      return DataType.INTEGER_TYPE;

    case NEG:
      return t1;

    default:
      // Si no se define, devolvemos null o lanzamos excepción
      throw new IllegalArgumentException("Tipo no definido para " + op);
    }
  }
}