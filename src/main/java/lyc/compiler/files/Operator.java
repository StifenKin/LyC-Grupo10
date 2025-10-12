package lyc.compiler.files;

public enum Operator {
  // --- Cargas ---
  ID("ID"),
  CTE("CTE"),

  // --- Operadores aritméticos ---
  ADD("+"),
  SUB("-"),
  MUL("*"),
  DIV("/"),

  // --- Asignación ---
  ASSIGN(":="),

  // --- Declaración ---
  DECLARE("DECLARE"),

  // --- Comparaciones ---
  LT("<"),
  GT(">"),
  EQ("=="),
  NEQ("!="),
  LTEQ("<="),
  GTEQ(">="),

  // --- Lógicos ---
  AND("AND"),
  OR("OR"),
  NOT("NOT"),

  // --- Control ---
  IF("IF"),
  ELSE("ELSE"),
  ENDIF("ENDIF"),
  WHILE("WHILE"),
  ENDWHILE("ENDWHILE"),
  BEGIN("BEGIN"),
  END("END"),

  // --- I/O ---
  READ("READ"),
  WRITE("WRITE"),

  // --- Operador Unario ---
  NEG("-");

  private final String display;

  Operator(String display) { this.display = display; }

  public String getDisplay() { return display; }
}
