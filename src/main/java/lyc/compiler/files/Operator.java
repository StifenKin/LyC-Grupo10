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
  MOD("%"),

  // --- Asignación ---
  ASSIGN(":="),

  // --- Declaración ---
  DECLARE("DECLARE"),

  // --- Comparaciones ---
  CMP("CMP"),
  LT("<"),
  GT(">"),
  EQ("=="),
  NEQ("!="),
  LTEQ("<="),
  GTEQ(">="),

  // --- Saltos condicionales ---
  BLT("BLT"),    // Branch if Less Than
  BGE("BGE"),    // Branch if Greater or Equal
  BLE("BLE"),    // Branch if Less or Equal
  BGT("BGT"),    // Branch if Greater Than
  BEQ("BEQ"),    // Branch if Equal
  BNE("BNE"),    // Branch if Not Equal
  BI("BI"),      // Branch Inconditional

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
  LABEL("LABEL"),

  // --- I/O ---
  READ("READ"),
  WRITE("WRITE"),

  // --- Operador Unario ---
  NEG("-");

  private final String display;

  Operator(String display) { this.display = display; }

  public String getDisplay() { return display; }
}
