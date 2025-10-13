package lyc.compiler;

import java_cup.runtime.Symbol;
import lyc.compiler.factories.ParserFactory;
import lyc.compiler.table.SymbolTableManager;
import lyc.compiler.files.IntermediateCodeGenerator;
import lyc.compiler.files.TypeTable;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;

import static com.google.common.truth.Truth.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class ParserTest {

    @BeforeEach
    void setUp() {
        // Limpiar tablas antes de cada test
        SymbolTableManager.getSymbolTable().clear();
        IntermediateCodeGenerator.reset();
        TypeTable.reset();
    }

    @AfterEach
    void tearDown() {
        // Limpiar tablas despues de cada test
        SymbolTableManager.getSymbolTable().clear();
        IntermediateCodeGenerator.reset();
        TypeTable.reset();
    }

    @Test
    public void assignmentWithExpression() throws Exception {
        compilationSuccessful("init { c, d, e : Float } c:=d*(e-21)/4");
    }

    @Test
    public void syntaxError() {
        compilationError("1234");
    }

    @Test
    void assignments() throws Exception {
        compilationSuccessful(readFromFile("assignments.txt"));
    }

    @Test
    void write() throws Exception {
        compilationSuccessful(readFromFile("write.txt"));
    }

    @Test
    void read() throws Exception {
        compilationSuccessful(readFromFile("read.txt"));
    }

    @Test
    void comment() throws Exception {
        compilationSuccessful(readFromFile("comment.txt"));
    }

    @Test
    void init() throws Exception {
        compilationSuccessful(readFromFile("init.txt"));
    }

    @Test
    void and() throws Exception {
        compilationSuccessful(readFromFile("and.txt"));
    }

    @Test
    void or() throws Exception {
        compilationSuccessful(readFromFile("or.txt"));
    }

    @Test
    void not() throws Exception {
        compilationSuccessful(readFromFile("not.txt"));
    }

    @Test
    void ifStatement() throws Exception {
        compilationSuccessful(readFromFile("if.txt"));
    }

    @Test
    void whileStatement() throws Exception {
        compilationSuccessful(readFromFile("while.txt"));
    }

    @Test
    void isZeroStatement() throws Exception {
        compilationSuccessful(readFromFile("iszero.txt"));
    }

    @Test
    void convDateStatement() throws Exception {
        compilationSuccessful(readFromFile("convdate.txt"));
    }

    private void compilationSuccessful(String input) throws Exception {
        assertThat(scan(input).sym).isEqualTo(ParserSym.EOF);
    }

    private void compilationError(String input){
        assertThrows(Exception.class, () -> scan(input));
    }

    private Symbol scan(String input) throws Exception {
        return ParserFactory.create(input).parse();
    }

    private String readFromFile(String fileName) throws IOException {
        File file = new File("src/test/java/resources/" + fileName);

        if (!file.exists()) {
            throw new IOException("El archivo no existe: " + file.getAbsolutePath());
        }
        return Files.readString(file.toPath(), StandardCharsets.UTF_8);
    }


}
