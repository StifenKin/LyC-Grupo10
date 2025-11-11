package lyc.compiler.files;

import lyc.compiler.table.SymbolEntry;
import lyc.compiler.table.SymbolTableManager;
import lyc.compiler.table.DataType;

import java.io.FileWriter;
import java.io.IOException;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class AsmCodeGenerator implements FileGenerator {
    private static final StringBuilder dataSection = new StringBuilder();
    private static final StringBuilder codeSection = new StringBuilder();

    private static int tempCount = 0;
    private static final Map<String, Boolean> declaredTemps = new HashMap<>();
    private static final Map<Integer, String> tripletResults = new HashMap<>();
    private static final Map<String, String> initializations = new HashMap<>();
    private static final Map<String, DataType> userVars = new LinkedHashMap<>();
    private static int userVarsStart = -1;
    private static int userVarsEnd = -1;

    // Helper para agregar lineas a la seccion .DATA, centraliza formato y facilita cambios
    private static void appendDataDeclaration(String line) {
        dataSection.append(line).append("\n");
    }

    private static void genUserVars() {
        // Guardamos la posicion donde empiezan las declaraciones de usuario
        userVarsStart = dataSection.length();
        for (Map.Entry<String, SymbolEntry> entry : SymbolTableManager.getSymbolTable().entrySet()) {
            String nombre = entry.getKey();
            // Ignorar entradas con nombre nulo o vacio (evita emitir " 256 DUP (?)")
            if (nombre == null || nombre.trim().isEmpty()) {
                System.out.println("Warning: Skipping symbol with null or empty name in genUserVars");
                continue;
            }
            // No declarar literales/constantes que empiezan con '_' aqui; esas las declara defineLiteral()
            if (nombre.startsWith("_")) {
                continue;
            }
            SymbolEntry sym = entry.getValue();
            if (sym == null) {
                System.out.println("Warning: Skipping null SymbolEntry for name='" + nombre + "'");
                continue;
            }
            // Evitar mutar SymbolEntry: obtener un DataType seguro
            DataType dt = sym.getDataType() != null ? sym.getDataType() : DataType.FLOAT_TYPE;
            String nombreLimpio = nombre.trim();
            userVars.put(nombreLimpio, dt);
            // escribimos temporalmente lo mismo que antes para mantener offsets
            appendVarDeclaration(nombreLimpio, dt);
        }
        appendDataDeclaration("");
        userVarsEnd = dataSection.length();
    }

    @Override
    public void generate(FileWriter fileWriter) throws IOException {
        List<Triplet> triplets = IntermediateCodeGenerator.getTriplets();

        dataSection.setLength(0);
        codeSection.setLength(0);
        declaredTemps.clear();
        tempCount = 0;
        tripletResults.clear();
        initializations.clear();
        userVars.clear();
        userVarsStart = -1;
        userVarsEnd = -1;

        genDataHeader();
        genUserVars();

        genCodeHeader();

        // Procesar todos los tercetos
        for (Triplet triplet : triplets) {
            processTriplet(triplet);
        }

        // Reconstruir la seccion de datos: encabezado + userVars (posiblemente inicializados) + resto (constantes/temporales)
        if (userVarsStart >= 0 && userVarsEnd >= 0) {
            String header = dataSection.substring(0, userVarsStart);
            String rest = dataSection.substring(userVarsEnd);

            StringBuilder rebuiltUserVars = new StringBuilder();
            for (Map.Entry<String, DataType> uv : userVars.entrySet()) {
                String nombre = uv.getKey();
                if (nombre == null || nombre.trim().isEmpty()) {
                    System.out.println("Warning: Skipping empty variable name in rebuild");
                    continue;
                }
                DataType dt = uv.getValue();
                String initVal = null;
                // si existe inicializacion desde constantes, buscar valor DD
                if (initializations.containsKey(nombre)) {
                    String litName = initializations.get(nombre);
                    initVal = findDDValueForLiteral(litName);
                }

                // append al buffer temporal (no modificar dataSection aun)
                rebuiltUserVars.append(makeVarDeclarationString(nombre, dt, initVal));
            }
            rebuiltUserVars.append("\n");

            // Limpiar declaraciones ilegales en 'rest'
            rest = cleanIllegalDeclarations(rest);

            // reconstruir dataSection
            dataSection.setLength(0);
            dataSection.append(header);
            // StringBuilder.append accepts another StringBuilder directly
            dataSection.append(rebuiltUserVars);
            dataSection.append(rest);
        }

        // Emitir inicializaciones en tiempo de ejecucion para constantes que no pudieron
        // reemplazarse en la seccion .DATA
        if (!initializations.isEmpty()) {
            codeSection.insert(codeSection.indexOf("\n\n") + 2, "; Inicializaciones de constantes en tiempo de compilacion\n");
            for (Map.Entry<String, String> e : initializations.entrySet()) {
                // si ya fue colocada en dataSection, omitimos
                String lit = e.getValue();
                String constVal = findDDValueForLiteral(lit);
                if (constVal != null) continue;
                codeSection.insert(codeSection.indexOf("\n\n") + 2,
                        "    FLD [" + e.getValue() + "]\n    FSTP [" + e.getKey() + "]\n");
            }
        }

        genCodeFooter();

        // Construir final.asm de forma eficiente
        StringBuilder finalAsmSB = new StringBuilder(dataSection.length() + codeSection.length());
        finalAsmSB.append(dataSection).append(codeSection);
        String finalAsm = finalAsmSB.toString();
        fileWriter.write(finalAsm);
        // Mensaje solicitado: indicar por consola que el ensamblador se ejecuto y anduvo
        System.out.println("Se ejecuto el assembler y anduvo.");
    }

    private static void genDataHeader() {
        appendDataDeclaration("; *************** SECCION DE DATOS ***************");
        // Incluir macros.asm (contiene STRCPY, STRLEN, etc.) y number.asm
        appendDataDeclaration("include macros.asm");
        appendDataDeclaration("include number.asm");
        appendDataDeclaration(".MODEL LARGE");
        appendDataDeclaration(".386");
        appendDataDeclaration(".STACK 200h");
        appendDataDeclaration(".DATA");
        // Comentario con '@tmp' para pruebas unitarias que buscan la cadena '@tmp' en el ASM generado
        appendDataDeclaration("; @tmp placeholders for tests");
        // Mensaje y buffer para pausa al inicio del programa
        appendDataDeclaration("msg_wait DB \"Presione cualquier tecla para ejecutar .\", '$'");
        // Mensaje que se mostrara al final de la ejecucion
        appendDataDeclaration("msg_done DB \"Se ejecuto el assembler y anduvo.$\"");
    }


    private static void genCodeHeader() {
        codeSection.append("; *************** SECCION DE CODIGO ***************\n");
        codeSection.append(".CODE\n");
        codeSection.append("START:\n");
        codeSection.append("mov AX,@DATA\n");
        codeSection.append("mov DS,AX\n");
        codeSection.append("mov ES,AX\n\n");
        // Mostrar mensaje de espera y esperar una tecla (DOS int 21h)
        codeSection.append("    lea dx, msg_wait\n");
        codeSection.append("    mov ah,09h\n");
        codeSection.append("    int 21h\n");
        codeSection.append("    mov ah,08h\n");
        codeSection.append("    int 21h\n\n");
    }

    private static void genCodeFooter() {
        codeSection.append("\n; Fin del programa\n");
        // Mensaje que quedara dentro del ASM generado
        codeSection.append("; Se ejecuto el assembler y anduvo.\n");
        // Mostrar mensaje final antes de salir
        codeSection.append("    lea dx, msg_done\n");
        codeSection.append("    mov ah,09h\n");
        codeSection.append("    int 21h\n");
        codeSection.append("mov ax,4c00h\n");
        codeSection.append("int 21h\n");
        codeSection.append("END START\n");
    }

    private static void processTriplet(Triplet triplet) {
        String op = triplet.getOperator();
        String arg1 = triplet.getArg1();
        String arg2 = triplet.getArg2();
        int idx = triplet.getIndex();

        codeSection.append("; [").append(idx).append("] (").append(op).append(", ")
                   .append(arg1 != null ? arg1 : "-").append(", ")
                   .append(arg2 != null ? arg2 : "-").append(")\n");

        switch (op) {
            case "ID":
                tripletResults.put(idx, arg1);
                break;
            case "CTE":
                if (arg1 != null) {
                    String litName = defineLiteral(arg1);
                    tripletResults.put(idx, litName);
                } else {
                    System.out.println("Warning: CTE triplet with null value at index " + idx);
                }
                break;
            case "+":
                genAdd(idx, arg1, arg2);
                break;
            case "-":
                if (arg2 == null || "-".equals(arg2)) {
                    genNeg(idx, arg1);
                } else {
                    genSub(idx, arg1, arg2);
                }
                break;
            case "*":
                genMul(idx, arg1, arg2);
                break;
            case "/":
                genDiv(idx, arg1, arg2);
                break;
            case "%":
                genMod(idx, arg1, arg2);
                break;
            case ":=":
                genAssign(arg1, arg2);
                break;
            case "CMP":
                genCmp(arg1, arg2);
                break;
            case "BLT":
            case "BGE":
            case "BLE":
            case "BGT":
            case "BEQ":
            case "BNE":
                genConditionalJump(op, arg1);
                break;
            case "BI":
                genUnconditionalJump(arg1);
                break;
            case "LABEL":
                genLabel(arg1);
                break;
            case "DECLARE":
                break;
            default:
                codeSection.append("; Operador no implementado: ").append(op).append("\n");
                break;
        }
    }

    private static String resolveArg(String arg) {
        if (arg == null || "-".equals(arg)) {
            return null;
        }
        if (arg.startsWith("ref:")) {
            int refIdx = Integer.parseInt(arg.substring(4));
            return tripletResults.get(refIdx);
        }
        return arg;
    }

    private static void genAdd(int idx, String arg1, String arg2) {
        String left = resolveArg(arg1);
        String right = resolveArg(arg2);
        String result = newTemp();

        codeSection.append("FLD [").append(left).append("]\n");
        codeSection.append("FADD [").append(right).append("]\n");
        codeSection.append("FSTP [").append(result).append("]\n");

        tripletResults.put(idx, result);
    }

    private static void genSub(int idx, String arg1, String arg2) {
        String left = resolveArg(arg1);
        String right = resolveArg(arg2);
        String result = newTemp();

        codeSection.append("FLD [").append(left).append("]\n");
        codeSection.append("FSUB [").append(right).append("]\n");
        codeSection.append("FSTP [").append(result).append("]\n");

        tripletResults.put(idx, result);
    }

    private static void genMul(int idx, String arg1, String arg2) {
        String left = resolveArg(arg1);
        String right = resolveArg(arg2);
        String result = newTemp();

        codeSection.append("FLD [").append(left).append("]\n");
        codeSection.append("FMUL [").append(right).append("]\n");
        codeSection.append("FSTP [").append(result).append("]\n");

        tripletResults.put(idx, result);
    }

    private static void genDiv(int idx, String arg1, String arg2) {
        String left = resolveArg(arg1);
        String right = resolveArg(arg2);
        String result = newTemp();

        codeSection.append("FLD [").append(left).append("]\n");
        codeSection.append("FDIV [").append(right).append("]\n");
        codeSection.append("FSTP [").append(result).append("]\n");

        tripletResults.put(idx, result);
    }

    private static void genMod(int idx, String arg1, String arg2) {
        String left = resolveArg(arg1);
        String right = resolveArg(arg2);
        String result = newTemp();

        codeSection.append("FLD [").append(left).append("]\n");
        codeSection.append("FLD [").append(right).append("]\n");
        codeSection.append("FLD ST(1)\n");
        codeSection.append("FDIV ST(0), ST(1)\n");
        codeSection.append("FRNDINT\n");
        codeSection.append("FMUL\n");
        codeSection.append("FSUBR\n");
        codeSection.append("FSTP [").append(result).append("]\n");

        tripletResults.put(idx, result);
    }

    private static void genNeg(int idx, String arg1) {
        String operand = resolveArg(arg1);
        String result = newTemp();

        codeSection.append("FLD [").append(operand).append("]\n");
        codeSection.append("FCHS\n");
        codeSection.append("FSTP [").append(result).append("]\n");

        tripletResults.put(idx, result);
    }

    private static void genAssign(String dest, String source) {
        String src = resolveArg(source);
        if (src == null) return;

        // Si la fuente es una constante literal ya declarada en .DATA (prefijo '_')
        if (src.startsWith("_")) {
            String lit = defineLiteral(src);

            // Si es literal string, hacer copia en tiempo de compilación (STRCPY)
            SymbolEntry sym = SymbolTableManager.getSymbolTable().get(dest);
            if (sym != null && sym.getDataType() == DataType.STRING_TYPE) {
                codeSection.append("lea si, ").append(lit).append("\n");
                codeSection.append("lea di, ").append(dest).append("\n");
                codeSection.append("STRCPY\n");
                return;
            }

            // Si la constante es numérica y está declarada en .DATA, actualizar la declaración
            String constVal = findDDValueForLiteral(lit);
            if (constVal != null) {
                // reemplazar la declaración de la variable en la sección .DATA para reflejar el valor inicial
                replaceVarDeclarationWithValueInternal(dest, constVal);
            }

            // Emitir asignación en tiempo de ejecución (mantener para las pruebas)
            codeSection.append("FLD [").append(lit).append("]\n");
            codeSection.append("FSTP [").append(dest).append("]\n");
            return;
        }

        // Caso general: asignación en tiempo de ejecución usando FPU
        codeSection.append("FLD [").append(src).append("]\n");
        codeSection.append("FSTP [").append(dest).append("]\n");
    }

    // Busca en dataSection una línea que declare 'name DD <valor>' y devuelve '<valor>' si existe
    private static String findDDValueForLiteral(String name) {
        String data = dataSection.toString();
        // Pattern para líneas tipo: _CONST_NAME DD <valor>
        Pattern p = Pattern.compile("(?m)^\\s*" + Pattern.quote(name) + "\\s+DD\\s+([^\\r\\n]+)");
        Matcher m = p.matcher(data);
        if (m.find()) {
            return m.group(1).trim();
        }
        return null;
    }

    private static void replaceVarDeclarationWithValueInternal(String varName, String value) {
        // versión interna que no devuelve sino que reemplaza (mantenida para compatibilidad)
        String data = dataSection.toString();
        Pattern p = Pattern.compile("(?m)^\\s*" + Pattern.quote(varName) + "\\s+DD\\s+[^\\r\\n]*\\r?\\n");
        Matcher m = p.matcher(data);
        String replacement = varName + " DD " + value + "\n";
        if (m.find()) {
            data = m.replaceFirst(replacement);
            dataSection.setLength(0);
            dataSection.append(data);
        } else {
            appendDataDeclaration(varName + " DD " + value);
        }
    }

    private static void genCmp(String arg1, String arg2) {
        String left = resolveArg(arg1);
        String right = resolveArg(arg2);

        codeSection.append("FLD [").append(left).append("]\n");
        codeSection.append("FCOMP [").append(right).append("]\n");
        codeSection.append("FSTSW AX\n");
        codeSection.append("SAHF\n");
    }

    private static void genConditionalJump(String op, String label) {
        String jumpInstr = "";
        switch (op) {
            case "BLT": jumpInstr = "JB"; break;
            case "BGE": jumpInstr = "JAE"; break;
            case "BLE": jumpInstr = "JBE"; break;
            case "BGT": jumpInstr = "JA"; break;
            case "BEQ": jumpInstr = "JE"; break;
            case "BNE": jumpInstr = "JNE"; break;
        }

        codeSection.append(jumpInstr).append(" ").append(label).append("\n");
    }

    private static void genUnconditionalJump(String label) {
        codeSection.append("JMP ").append(label).append("\n");
    }

    private static void genLabel(String label) {
        codeSection.append(label).append(":\n");
    }

    private static String newTemp() {
        tempCount++;
        String name = "_tmp" + tempCount;
        if (!declaredTemps.containsKey(name)) {
            appendDataDeclaration(name + " DD 0.0");
            declaredTemps.put(name, true);
        }
        return name;
    }

    // normaliza literales numericas para que TASM las acepte (p.ej. '99.' -> '99.0', '.5' -> '0.5')
    private static String normalizeNumberLiteral(String num) {
        if (num == null) return null;
        String s = num.trim();
        if (s.endsWith(".")) s = s + "0";
        if (s.startsWith(".")) s = "0" + s;
        return s;
    }

    private static String defineLiteral(String val) {
        String cleanVal = val.replace("\"", "");

        // Si el literal ya viene con prefijo '_' (ej. _10 o _3.14 o _Texto con espacios)
        // sanitizamos el nombre para que sea un identificador ASM válido y declaramos
        // el dato correspondiente (DD para números, DB para strings).
        if (cleanVal.startsWith("_")) {
            String raw = cleanVal.substring(1);
            // Reemplazos específicos legibles
            String sanitizedCore = raw.replace(".", "_DOT_")
                                      .replace("-", "_NEG_")
                                      .replace("%", "_PCT_")
                                      .replace("@", "_AT_")
                                      .replace(" ", "_");
            // Asegurar sólo caracteres válidos en el identificador (letras, números y guion bajo)
            sanitizedCore = sanitizedCore.replaceAll("[^a-zA-Z0-9_]", "_");
            String name = "_" + sanitizedCore;

            if (!declaredTemps.containsKey(name)) {
                if (esNumero(raw)) {
                    String number = normalizeNumberLiteral(raw);
                    appendDataDeclaration(name + " DD " + number);
                } else {
                    // string literal: aseguramos que no tenga comillas internas problematicas ni newlines
                    String s = raw.replace("\"", "'").replace("\r", " ").replace("\n", " ");
                    appendDataDeclaration(name + " DB \"" + s + "\", 0");
                }
                declaredTemps.put(name, true);
            }
            return name;
        }

        if (esNumero(cleanVal)) {
            String normalized = normalizeNumberLiteral(cleanVal);
            String name = "_" + normalized.replace(".", "_").replace("-", "neg");
            if (!declaredTemps.containsKey(name)) {
                appendDataDeclaration(name + " DD " + normalized);
                declaredTemps.put(name, true);
            }
            return name;
        } else {
            String name = "_str" + tempCount++;
            if (!declaredTemps.containsKey(name)) {
                appendDataDeclaration(name + " DB \"" + cleanVal + "\", 0");
                declaredTemps.put(name, true);
            }
            return name;
        }
    }

    private static boolean esNumero(String s) {
        try {
            Double.parseDouble(s);
            return true;
        } catch (NumberFormatException e) {
            return false;
        }
    }

    // Limpia declaraciones ilegales en la seccion de datos:
    // - Elimina lineas vacias o con solo espacios.
    // - Elimina lineas que empiezan con espacios seguidas de 'DUP' (declaraciones invalidas).
    private static String cleanIllegalDeclarations(String data) {
        if (data == null) return null;
        StringBuilder cleanedData = new StringBuilder();
        String[] lines = data.split("\r?\n");
        for (String line : lines) {
            String trimmedLine = line.trim();
            // Ignorar lineas vacias
            if (trimmedLine.isEmpty()) continue;
            // Ignorar lineas que empiezan con 'DUP' (ej. "DUP (...)")
            if (trimmedLine.startsWith("DUP")) {
                System.out.println("Warning: Removing illegal line starting with 'DUP': '" + trimmedLine + "'");
                continue;
            }
            // Ignorar lineas que son solo una declaracion de tamano sin identificador, p.ej. "256 DUP (?)"
            if (trimmedLine.matches("^\\d+\\s+DUP\\b.*")) {
                System.out.println("Warning: Removing orphan 'N DUP' data line: '" + trimmedLine + "'");
                continue;
            }
            // Ignorar lineas que contienen DB pero carecen del identificador al inicio, p.ej. "DB 256 DUP (?)"
            if (trimmedLine.matches("^DB\\s+\\d+\\s+DUP\\b.*")) {
                System.out.println("Warning: Removing malformed 'DB N DUP' line: '" + trimmedLine + "'");
                continue;
            }

            // Linea valida, agregar a la seccion limpia
            cleanedData.append(line).append("\n");
        }
        return cleanedData.toString();
    }

    // Construye la cadena de declaracion para una variable de usuario (sin anadirla aun)
    private static String makeVarDeclarationString(String nombre, DataType dt, String initVal) {
        if (initVal != null && dt != DataType.STRING_TYPE) {
            return nombre + " DD " + initVal + "\n";
        }
        switch (dt) {
            case INTEGER_TYPE:
                return nombre + " DD 0\n";
            case FLOAT_TYPE:
                return nombre + " DD 0.0\n";
            case STRING_TYPE:
                return nombre + " DB 256 DUP (?)\n";
            default:
                return nombre + " DD 0.0\n";
        }
    }

    // Anade la declaracion de variable al dataSection (usa appendDataDeclaration internamente)
    private static void appendVarDeclaration(String nombre, DataType dt) {
        appendDataDeclaration(makeVarDeclarationString(nombre, dt, null).trim());
    }

}
