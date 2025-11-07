package lyc.compiler.files;

import lyc.compiler.table.SymbolEntry;
import lyc.compiler.table.SymbolTableManager;
import lyc.compiler.table.DataType;

import java.io.FileWriter;
import java.io.IOException;
import java.util.*;

public class AsmCodeGenerator implements FileGenerator {
    private static StringBuilder dataSection = new StringBuilder();
    private static StringBuilder codeSection = new StringBuilder();

    private static int tempCount = 0;
    private static final Map<String, Boolean> declaredTemps = new HashMap<>();
    private static final Map<Integer, String> tripletResults = new HashMap<>();

    @Override
    public void generate(FileWriter fileWriter) throws IOException {
        List<Triplet> triplets = IntermediateCodeGenerator.getTriplets();

        dataSection.setLength(0);
        codeSection.setLength(0);
        declaredTemps.clear();
        tempCount = 0;
        tripletResults.clear();

        genDataHeader();
        genUserVars();

        genCodeHeader();

        // Procesar todos los tercetos
        for (Triplet triplet : triplets) {
            processTriplet(triplet);
        }

        genCodeFooter();

        StringBuilder finalAsm = new StringBuilder();
        finalAsm.append(dataSection);
        finalAsm.append(codeSection);

        fileWriter.write(finalAsm.toString());
    }

    private static void genDataHeader() {
        dataSection.append("; *************** SECCION DE DATOS ***************\n");
        dataSection.append(".MODEL LARGE\n");
        dataSection.append(".386\n");
        dataSection.append(".STACK 200h\n");
        dataSection.append(".DATA\n");
    }

    private static void genUserVars() {
        for (Map.Entry<String, SymbolEntry> entry : SymbolTableManager.getSymbolTable().entrySet()) {
            String nombre = entry.getKey();
            SymbolEntry sym = entry.getValue();
            if (sym == null || sym.getDataType() == null)
                sym.setDataType(DataType.FLOAT_TYPE);
            switch (sym.getDataType()) {
                case INTEGER_TYPE:
                    dataSection.append(nombre).append(" DD 0\n");
                    break;
                case FLOAT_TYPE:
                    dataSection.append(nombre).append(" DD 0.0\n");
                    break;
                case STRING_TYPE:
                    dataSection.append(nombre).append(" DB 256 DUP (?)\n");
                    break;
                default:
                    dataSection.append(nombre).append(" DD 0.0\n");
                    break;
            }
        }
        dataSection.append("\n");
    }

    private static void genCodeHeader() {
        codeSection.append("; *************** SECCION DE CODIGO ***************\n");
        codeSection.append(".CODE\n");
        codeSection.append("START:\n");
        codeSection.append("mov AX,@DATA\n");
        codeSection.append("mov DS,AX\n");
        codeSection.append("mov ES,AX\n\n");
    }

    private static void genCodeFooter() {
        codeSection.append("\n; Fin del programa\n");
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
                String litName = defineLiteral(arg1);
                tripletResults.put(idx, litName);
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

        codeSection.append("FLD [").append(src).append("]\n");
        codeSection.append("FSTP [").append(dest).append("]\n");
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
        String name = "@tmp" + tempCount;
        if (!declaredTemps.containsKey(name)) {
            dataSection.append(name).append(" DD 0.0\n");
            declaredTemps.put(name, true);
        }
        return name;
    }

    private static String defineLiteral(String val) {
        String cleanVal = val.replace("\"", "");

        if (esNumero(cleanVal)) {
            String name = "_" + cleanVal.replace(".", "_").replace("-", "neg");
            if (!declaredTemps.containsKey(name)) {
                dataSection.append(name).append(" DD ").append(cleanVal).append("\n");
                declaredTemps.put(name, true);
            }
            return name;
        } else {
            String name = "_str" + tempCount++;
            if (!declaredTemps.containsKey(name)) {
                dataSection.append(name).append(" DB \"").append(cleanVal).append("\", 0\n");
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
}
