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
    case MOD:
      // Solo números: int y float (MOD típicamente solo con enteros, pero permitimos flexibilidad)
      return t1.isNumeric() && t2.isNumeric();
    case NEG:
      // unario → solo uno debe ser numérico
      return t1 != null && t1.isNumeric();
    case ASSIGN:
      // Para asignaciones: mismo tipo o conversión implícita permitida
      if (t1 == t2) return true;
      // Int se puede asignar a Float (conversión implícita)
      if (t1 == DataType.FLOAT_TYPE && t2 == DataType.INTEGER_TYPE) return true;
      return false;
    case LT:
    case GT:
    case EQ:
    case NEQ:
    case LTEQ:
    case GTEQ:
      // Comparaciones: mismo tipo o ambos numéricos
      if (t1 == t2) return true;
      return t1.isNumeric() && t2.isNumeric();
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

    case MOD:
      // Módulo típicamente devuelve entero, pero si hay float involucrado, devolvemos float
      if (t1 == DataType.FLOAT_TYPE || t2 == DataType.FLOAT_TYPE)
        return DataType.FLOAT_TYPE;
      return DataType.INTEGER_TYPE;

    case NEG:
      return t1;

    case ASSIGN:
      return t1; // El tipo destino

    case LT:
    case GT:
    case EQ:
    case NEQ:
    case LTEQ:
    case GTEQ:
      // Las comparaciones devuelven boolean (pero no lo tenemos definido, usaremos INT)
      return DataType.INTEGER_TYPE;

    default:
      // Si no se define, devolvemos null o lanzamos excepción
      throw new IllegalArgumentException("Tipo no definido para " + op);
    }
  }

  public static void reset() {
    typeMap.clear();
  }
}