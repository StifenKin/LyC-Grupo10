package lyc.compiler;

import lyc.compiler.factories.ParserFactory;
import lyc.compiler.files.AsmCodeGenerator;
import lyc.compiler.files.IntermediateCodeGenerator;
import lyc.compiler.files.TypeTable;
import lyc.compiler.table.SymbolTableManager;
import org.junit.jupiter.api.*;

import java.io.*;

import static org.junit.jupiter.api.Assertions.*;

public class AsmCodeGeneratorTest {

    @BeforeEach
    public void setUp() {
        IntermediateCodeGenerator.reset();
        TypeTable.reset();
        SymbolTableManager.getSymbolTable().clear();
    }

    @AfterEach
    public void tearDown() {
        IntermediateCodeGenerator.reset();
        TypeTable.reset();
        SymbolTableManager.getSymbolTable().clear();
    }

    @Test
    @DisplayName("Test generación ASM con tercetos - Asignaciones simples")
    public void testSimpleAssignments() throws Exception {
        String input = "init{\n" +
                "  a:Int\n" +
                "  b:Int\n" +
                "}\n" +
                "a := 10\n" +
                "b := 20";

        parseInput(input);
        String asmCode = generateAsm();

        // Verificar que contiene las variables declaradas
        assertTrue(asmCode.contains("a DD 0"), "Debe declarar variable 'a'");
        assertTrue(asmCode.contains("b DD 0"), "Debe declarar variable 'b'");

        // Verificar que contiene las constantes literales (con prefijo _)
        assertTrue(asmCode.contains("__10") || asmCode.contains("_10"), "Debe declarar literal 10");
        assertTrue(asmCode.contains("__20") || asmCode.contains("_20"), "Debe declarar literal 20");

        // Verificar que contiene las asignaciones
        assertTrue(asmCode.contains("FSTP [a]"), "Debe asignar a 'a'");
        assertTrue(asmCode.contains("FSTP [b]"), "Debe asignar a 'b'");
    }

    @Test
    @DisplayName("Test generación ASM con tercetos - Operaciones aritméticas")
    public void testArithmeticOperations() throws Exception {
        String input = "init{\n" +
                "  x:Float\n" +
                "  y:Float\n" +
                "  z:Float\n" +
                "}\n" +
                "x := 5.5\n" +
                "y := 3.2\n" +
                "z := x + y";

        parseInput(input);
        String asmCode = generateAsm();

        // Verificar operación de suma
        assertTrue(asmCode.contains("FADD"), "Debe contener instrucción de suma");
        assertTrue(asmCode.contains("@tmp"), "Debe usar variables temporales");
    }

    @Test
    @DisplayName("Test generación ASM con tercetos - Comparaciones con mismo tipo")
    public void testComparisonsWithSameType() throws Exception {
        String input = "init{\n" +
                "  a:Int\n" +
                "  b:Int\n" +
                "}\n" +
                "a := 10\n" +
                "b := 20\n" +
                "if (a < b) {\n" +
                "  a := 30\n" +
                "}";

        parseInput(input);
        String asmCode = generateAsm();

        // Verificar que contiene comparación
        assertTrue(asmCode.contains("FCOMP"), "Debe contener instrucción de comparación");
        assertTrue(asmCode.contains("FSTSW AX"), "Debe guardar flags de FPU");
        assertTrue(asmCode.contains("SAHF"), "Debe cargar flags a CPU");

        // Verificar saltos condicionales
        assertTrue(asmCode.matches("(?s).*J[A-Z]+.*"), "Debe contener instrucciones de salto");
        assertTrue(asmCode.contains("L"), "Debe contener etiquetas");
    }

    @Test
    @DisplayName("Test validación de tipos incompatibles en comparación - String vs Int")
    public void testIncompatibleTypeComparison() {
        String input = "init{\n" +
                "  a:Int\n" +
                "  b:String\n" +
                "}\n" +
                "a := 10\n" +
                "b := \"hola\"\n" +
                "if (a < b) {\n" +
                "  a := 30\n" +
                "}";

        assertThrows(Exception.class, () -> parseInput(input),
                "Debe lanzar excepción al comparar tipos incompatibles (Int vs String)");
    }

    @Test
    @DisplayName("Test validación de tipos compatibles - Int vs Float en comparación")
    public void testCompatibleNumericTypeComparison() throws Exception {
        String input = "init{\n" +
                "  a:Int\n" +
                "  b:Float\n" +
                "}\n" +
                "a := 10\n" +
                "b := 20.5\n" +
                "if (a < b) {\n" +
                "  a := 30\n" +
                "}";

        // No debe lanzar excepción - Int y Float son compatibles en comparaciones
        assertDoesNotThrow(() -> parseInput(input),
                "Int y Float deben ser compatibles en comparaciones");

        String asmCode = generateAsm();
        assertTrue(asmCode.contains("FCOMP"), "Debe generar código de comparación");
    }

    @Test
    @DisplayName("Test generación ASM - Operador módulo")
    public void testModuloOperation() throws Exception {
        String input = "init{\n" +
                "  a:Int\n" +
                "  b:Int\n" +
                "  c:Int\n" +
                "}\n" +
                "a := 10\n" +
                "b := 3\n" +
                "c := a % b";

        parseInput(input);
        String asmCode = generateAsm();

        // Verificar que contiene operación de módulo
        assertTrue(asmCode.contains("FDIV"), "Módulo debe usar división");
        assertTrue(asmCode.contains("FRNDINT"), "Módulo debe redondear");
        assertTrue(asmCode.contains("FSUBR"), "Módulo debe usar resta reversa");
    }

    @Test
    @DisplayName("Test generación ASM - Operador unario negativo")
    public void testUnaryNegation() throws Exception {
        String input = "init{\n" +
                "  a:Int\n" +
                "  b:Int\n" +
                "}\n" +
                "a := 10\n" +
                "b := -a";

        parseInput(input);
        String asmCode = generateAsm();

        // Verificar que contiene operación de negación
        assertTrue(asmCode.contains("FCHS"), "Debe usar FCHS para cambiar signo");
    }

    @Test
    @DisplayName("Test generación ASM - Estructura while")
    public void testWhileLoop() throws Exception {
        String input = "init{\n" +
                "  i:Int\n" +
                "}\n" +
                "i := 0\n" +
                "while (i < 10) {\n" +
                "  i := i + 1\n" +
                "}";

        parseInput(input);
        String asmCode = generateAsm();

        // Verificar etiquetas y saltos
        assertTrue(asmCode.contains("L"), "Debe contener etiquetas para while");
        assertTrue(asmCode.contains("JMP"), "Debe contener salto incondicional al inicio");
    }

    @Test
    @DisplayName("Test generación ASM - Estructura if-else")
    public void testIfElseStructure() throws Exception {
        String input = "init{\n" +
                "  a:Int\n" +
                "  b:Int\n" +
                "}\n" +
                "a := 10\n" +
                "if (a > 5) {\n" +
                "  b := 20\n" +
                "} else {\n" +
                "  b := 30\n" +
                "}";

        parseInput(input);
        String asmCode = generateAsm();

        // Verificar estructura if-else
        assertTrue(asmCode.contains("JMP"), "Debe contener salto para evitar el else");

        // Contar etiquetas (debe haber al menos 2 para if-else)
        long labelCount = asmCode.lines().filter(line -> line.trim().matches("L\\d+:")).count();
        assertTrue(labelCount >= 2, "Debe tener al menos 2 etiquetas para if-else");
    }

    @Test
    @DisplayName("Test validación tipos - Comparación de dos expresiones")
    public void testExpressionComparison() throws Exception {
        String input = "init{\n" +
                "  a:Int\n" +
                "  b:Int\n" +
                "  c:Int\n" +
                "}\n" +
                "a := 5\n" +
                "b := 3\n" +
                "c := 2\n" +
                "if (a + b > c * 2) {\n" +
                "  a := 100\n" +
                "}";

        // No debe lanzar excepción - todas son expresiones Int
        assertDoesNotThrow(() -> parseInput(input),
                "Expresiones del mismo tipo deben ser comparables");

        String asmCode = generateAsm();

        // Verificar que se generan operaciones y comparación
        assertTrue(asmCode.contains("FADD"), "Debe contener suma");
        assertTrue(asmCode.contains("FMUL"), "Debe contener multiplicación");
        assertTrue(asmCode.contains("FCOMP"), "Debe contener comparación");
    }

    // Métodos auxiliares
    private void parseInput(String input) throws Exception {
        ParserFactory.create(input).parse();
    }

    private String generateAsm() throws IOException {
        // Crear un archivo temporal
        File tempFile = File.createTempFile("test_asm_", ".asm");
        tempFile.deleteOnExit();

        try (FileWriter fileWriter = new FileWriter(tempFile)) {
            AsmCodeGenerator generator = new AsmCodeGenerator();
            generator.generate(fileWriter);
        }

        // Leer el contenido del archivo
        StringBuilder content = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(new FileReader(tempFile))) {
            String line;
            while ((line = reader.readLine()) != null) {
                content.append(line).append("\n");
            }
        }

        // Eliminar el archivo temporal
        tempFile.delete();

        return content.toString();
    }
}
