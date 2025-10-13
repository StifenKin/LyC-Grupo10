package lyc.compiler;

import java_cup.runtime.Symbol;
import lyc.compiler.ParserSym;
import lyc.compiler.model.*;import lyc.compiler.table.DataType;import lyc.compiler.table.SymbolEntry;import lyc.compiler.table.SymbolTableManager;
import static lyc.compiler.constants.Constants.*;

%%

%public
%class Lexer
%unicode
%cup
%line
%column
%throws CompilerException
%eofval{
  return symbol(ParserSym.EOF);
%eofval}
%state COMMENT


%{
  private Symbol symbol(int type) {
    return new Symbol(type, yyline, yycolumn);
  }
  private Symbol symbol(int type, Object value) {
    return new Symbol(type, yyline, yycolumn, value);
  }
  
  // Para manejar comentarios anidados
  private int commentDepth = 0;

  // Indica si el siguiente '-' debe interpretarse como signo unario (permitir -123)
  private boolean lastTokenAllowsUnary = true; // al inicio del archivo se permite unario

  // Actualiza la bandera según el token emitido
  private void updateUnaryFlagAfter(int tokenType) {
    switch(tokenType) {
      // tokens después de los cuales puede venir un signo unario
      case ParserSym.ASSIG:
      case ParserSym.OPEN_BRACKET:
      case ParserSym.OPEN_CURLY_BRACKET:
      case ParserSym.OPEN_SQUARE_BRACKET:
      case ParserSym.COMMA:
      case ParserSym.PLUS:
      case ParserSym.MULT:
      case ParserSym.DIV:
      case ParserSym.MAYOR:
      case ParserSym.LOWER:
      case ParserSym.MAYOR_I:
      case ParserSym.LOWER_I:
      case ParserSym.EQUAL:
      case ParserSym.NOT_EQUAL:
      case ParserSym.AND_COND:
      case ParserSym.OR_COND:
      case ParserSym.NOT_COND:
        lastTokenAllowsUnary = true;
        break;
      default:
        // después de valores, identificadores o cierres, NO se espera unario
        lastTokenAllowsUnary = false;
    }
  }
%}

LineTerminator = \r|\n|\r\n
InputCharacter = [^\r\n]
Identation =  [ \t\f]


Init = "init"

Int = "Int"
Float = "Float"
String = "String"

If = "if"
While = "while"
Else = "else"

Write = "write"
Read = "read"

/* Funciones especiales */

IsZero = "isZero"
DateConverted = "DateConverted"
ConvDate = "convDate"


Day   = (0?[1-9]|[12][0-9]|3[01])
Month = (0?[1-9]|1[0-2])
Year  = [0-9]{4}
Date  = {Day}-{Month}-{Year}


Plus = "+"
Mult = "*"
Sub = "-"
Div = "/"
Mod = "%"
Assig = ":="

Mayor = ">"
Lower = "<"
MayorI = ">="
LowerI = "<="
Equal = "=="
NotEqual = "!="

AndCond = "AND"
OrCond = "OR"
NotCond = "NOT"

OpenBracket = "("
CloseBracket = ")"
OpenCurlyBrace = "{"
CloseCurlyBrace = "}"
OpenSquareBracket = "["
CloseSquareBracket = "]"

Comma = ","
SemiColon = ";"
Dot = "."
DoubleDot = ":"

Letter = [a-zA-Z]
Digit = [0-9]
Digit19 = [1-9]
True = "true"
False = "false"
InvalidCharacter = [^a-zA-z0-9<>:,@/\%\+\*\-\.\[\];\(\)=?!]

OpenComment  = "#\+"
CloseComment = "\+#"



WhiteSpace = {LineTerminator}|{Identation}
Identifier = {Letter}({Letter}|{Digit})*
BooleanConstant = {True}|{False}
IntegerConstant = {Digit}|{Digit19}{Digit}+

FloatLeft = {Digit} | {Digit19}{Digit}+
FloatRight = {Digit}+

FloatConstant = {FloatLeft}\.{FloatRight}|{FloatLeft}\.|\.{FloatRight}

StringConstant = \"(([^\"\n]*)\")

%%

  /* Comments */
  <YYINITIAL>{OpenComment} {
    commentDepth = 1;
    yybegin(COMMENT);
    //System.out.println(">> ENTER COMMENT at line " + (yyline + 1) + ", col " + (yycolumn + 1));
    //No se devuelve token.
  }
  <COMMENT> {
    {OpenComment} {
      if (commentDepth == 1) {
          commentDepth = 2;
      } else {
          throw new InvalidCommentException("Comentario anidado a más de un nivel (line " + yyline + ", col " + yycolumn + ")");
        }
      }
    
    {CloseComment} {
        if (commentDepth == 2) {
          commentDepth = 1;
        } else if (commentDepth == 1) {
          commentDepth = 0;
          yybegin(YYINITIAL);
        } else {
          throw new InvalidCommentException("cierre de comentario sin apertura (line " + yyline + ", col " + yycolumn + ")");
        }
      }
    <<EOF>> {
        throw new InvalidCommentException("EOF dentro de un comentario sin cerrar (line " + yyline + ", col " + yycolumn + ")");
      }

    [^] { /* ignorar*/ }
  }

/* keywords */
<YYINITIAL> {

  /* Conditionals */
  {AndCond}  {
       System.out.println("Token AND_COND encontrado: " + yytext());
       updateUnaryFlagAfter(ParserSym.AND_COND);
       return symbol(ParserSym.AND_COND);
   }
   {OrCond}  {
       System.out.println("Token OR_COND encontrado: " + yytext());
       updateUnaryFlagAfter(ParserSym.OR_COND);
       return symbol(ParserSym.OR_COND);
   }
   {NotCond} {
       System.out.println("Token NOT_COND encontrado: " + yytext());
       updateUnaryFlagAfter(ParserSym.NOT_COND);
       return symbol(ParserSym.NOT_COND);
   }

  /* Declaration */
  {Init}                                    { updateUnaryFlagAfter(ParserSym.INIT); return symbol(ParserSym.INIT); }

  /* Logical */
  {If}                                     { updateUnaryFlagAfter(ParserSym.IF); return symbol(ParserSym.IF); }
  {Else}                                   { updateUnaryFlagAfter(ParserSym.ELSE); return symbol(ParserSym.ELSE); }
  {While}                                  { updateUnaryFlagAfter(ParserSym.WHILE); return symbol(ParserSym.WHILE); }

  /*Funciones especiales*/
  {IsZero}                                 { updateUnaryFlagAfter(ParserSym.ISZERO); return symbol(ParserSym.ISZERO); }
  {DateConverted}                          { updateUnaryFlagAfter(ParserSym.DATECONVERTED); return symbol(ParserSym.DATECONVERTED); }
  {ConvDate}                               { updateUnaryFlagAfter(ParserSym.CONVDATE); return symbol(ParserSym.CONVDATE); }
  {Date}                                   { updateUnaryFlagAfter(ParserSym.DATE_LITERAL); return symbol(ParserSym.DATE_LITERAL, yytext()); }


  /* Data types */
  {Int}                                     { updateUnaryFlagAfter(ParserSym.INT); return symbol(ParserSym.INT); }
  {Float}                                   { updateUnaryFlagAfter(ParserSym.FLOAT); return symbol(ParserSym.FLOAT); }
  {String}                                  { updateUnaryFlagAfter(ParserSym.STRING); return symbol(ParserSym.STRING); }

  /* I/O */
  {Write}                                  { updateUnaryFlagAfter(ParserSym.WRITE); return symbol(ParserSym.WRITE); }
  {Read}                                   { updateUnaryFlagAfter(ParserSym.READ); return symbol(ParserSym.READ); }


  /* Identifiers */
  {BooleanConstant}                         { updateUnaryFlagAfter(ParserSym.BOOLEAN_CONSTANT); return symbol(ParserSym.BOOLEAN_CONSTANT); }
  {Identifier}                             {
                                              String id = yytext();
                                              if(yytext().length() > 15) {
                                                  throw new InvalidLengthException("Identifier length not allowed: " + id);
                                              }
                                              // Insertar en tabla como ID (marca) para que el parser pueda validar uso/decl.
                                              if (!SymbolTableManager.existsInTable(id)) {
                                                SymbolEntry entry = new SymbolEntry(id, DataType.ID);
                                                SymbolTableManager.insertInTable(entry);
                                              }
                                              updateUnaryFlagAfter(ParserSym.IDENTIFIER);
                                              return symbol(ParserSym.IDENTIFIER, id);
                                          }
  /* Constants */



{IntegerConstant}                        {
                                                // validar como entero con signo de 16 bits
                                                int value;
                                                try {
                                                  value = Integer.parseInt(yytext());
                                                } catch (NumberFormatException ex) {
                                                  throw new InvalidIntegerException("Integer out of range: " + yytext());
                                                }
                                                if (value < -32768 || value > 32767) {
                                                    throw new InvalidIntegerException("Integer out of range: " + yytext());
                                                }

                                                if(!SymbolTableManager.existsInTable(yytext())){
                                                      SymbolEntry entry = new SymbolEntry("_"+yytext(), DataType.INTEGER_TYPE, yytext());
                                                      SymbolTableManager.insertInTable(entry);
                                                }

                                                updateUnaryFlagAfter(ParserSym.INTEGER_CONSTANT);
                                                return symbol(ParserSym.INTEGER_CONSTANT, "_"+yytext());
                                            }

  /* Regla para enteros negativos ligados ("-123") que solo se aceptan si vienen en contexto unario */
  {Sub}{IntegerConstant} {
      String txt = yytext(); // ejemplo: "-21"
      if (lastTokenAllowsUnary) {
          int value = Integer.parseInt(txt);
          if (value < -32768 || value > 32767) {
              throw new InvalidIntegerException("Integer out of range: " + txt);
          }

          if(!SymbolTableManager.existsInTable(txt)){
                SymbolEntry entry = new SymbolEntry("_"+txt, DataType.INTEGER_TYPE, txt);
                SymbolTableManager.insertInTable(entry);
          }

          updateUnaryFlagAfter(ParserSym.INTEGER_CONSTANT);
          return symbol(ParserSym.INTEGER_CONSTANT, "_"+txt);
      } else {
          // No es ununario: devolver token SUB y "empujar" de vuelta el resto del texto (sin '-'),
          // para que el siguiente escaneo detecte el número por separado.
          yypushback(yytext().length() - 1);
          updateUnaryFlagAfter(ParserSym.SUB);
          return symbol(ParserSym.SUB);
      }
  }

  {FloatConstant}                           {
                                                float value = Float.parseFloat(yytext());

                                                if (!Float.isFinite(value)) {
                                                  throw new InvalidFloatException("Float out of range: " + yytext());
                                                }

                                                if (!SymbolTableManager.existsInTable(yytext())) {
                                                    SymbolEntry entry = new SymbolEntry("_"+yytext(), DataType.FLOAT_TYPE, yytext());
                                                    SymbolTableManager.insertInTable(entry);
                                                }

                                                updateUnaryFlagAfter(ParserSym.FLOAT_CONSTANT);
                                                return symbol(ParserSym.FLOAT_CONSTANT, "_"+yytext());
                                            }



  {StringConstant}                         {    
                                                StringBuffer sb;
                                                sb = new StringBuffer(yytext());
                                                if(sb.length() > 52) //quotes add 2 to max length
                                                    throw new InvalidLengthException("String out of range: " + yytext());

                                                sb.replace(0,1,"");
                                                sb.replace(sb.length()-1,sb.length(),""); //trim extra quotes
                                              
                                                if(!SymbolTableManager.existsInTable(yytext())){
                                                      SymbolEntry entry = new SymbolEntry("_"+sb.toString(), DataType.STRING_TYPE, sb.toString(), Integer.toString(sb.length()));
                                                      SymbolTableManager.insertInTable(entry);
                                                }

                                                updateUnaryFlagAfter(ParserSym.STRING_CONSTANT);
                                                return symbol(ParserSym.STRING_CONSTANT, "_"+sb.toString());
                                            }
  /*Declaration*/
  {Init}                                    { updateUnaryFlagAfter(ParserSym.INIT); return symbol(ParserSym.INIT); }

  /* Operators */
  {Plus}                                    { updateUnaryFlagAfter(ParserSym.PLUS); return symbol(ParserSym.PLUS); }
  {Sub}                                     { updateUnaryFlagAfter(ParserSym.SUB); return symbol(ParserSym.SUB); }
  {Mult}                                    { updateUnaryFlagAfter(ParserSym.MULT); return symbol(ParserSym.MULT); }
  {Div}                                     { updateUnaryFlagAfter(ParserSym.DIV); return symbol(ParserSym.DIV); }
  {Mod}                                     { updateUnaryFlagAfter(ParserSym.MOD); return symbol(ParserSym.MOD); }
  {Assig}                                   { updateUnaryFlagAfter(ParserSym.ASSIG); return symbol(ParserSym.ASSIG); }
  {OpenBracket}                             { updateUnaryFlagAfter(ParserSym.OPEN_BRACKET); return symbol(ParserSym.OPEN_BRACKET); }
  {CloseBracket}                            { updateUnaryFlagAfter(ParserSym.CLOSE_BRACKET); return symbol(ParserSym.CLOSE_BRACKET); }
  {OpenCurlyBrace}                          { updateUnaryFlagAfter(ParserSym.OPEN_CURLY_BRACKET); return symbol(ParserSym.OPEN_CURLY_BRACKET); }
  {CloseCurlyBrace}                         { updateUnaryFlagAfter(ParserSym.CLOSE_CURLY_BRACKET); return symbol(ParserSym.CLOSE_CURLY_BRACKET); }
  {OpenSquareBracket}                       { updateUnaryFlagAfter(ParserSym.OPEN_SQUARE_BRACKET); return symbol(ParserSym.OPEN_SQUARE_BRACKET); }
  {CloseSquareBracket}                      { updateUnaryFlagAfter(ParserSym.CLOSE_SQUARE_BRACKET); return symbol(ParserSym.CLOSE_SQUARE_BRACKET); }

  /* Comparators */
  {Mayor}                                  { updateUnaryFlagAfter(ParserSym.MAYOR); return symbol(ParserSym.MAYOR); }
  {Lower}                                  { updateUnaryFlagAfter(ParserSym.LOWER); return symbol(ParserSym.LOWER); }
  {MayorI}                                 { updateUnaryFlagAfter(ParserSym.MAYOR_I); return symbol(ParserSym.MAYOR_I); }
  {LowerI}                                 { updateUnaryFlagAfter(ParserSym.LOWER_I); return symbol(ParserSym.LOWER_I); }
  {Equal}                                  { updateUnaryFlagAfter(ParserSym.EQUAL); return symbol(ParserSym.EQUAL); }
  {NotEqual}                               { updateUnaryFlagAfter(ParserSym.NOT_EQUAL); return symbol(ParserSym.NOT_EQUAL); }

  /* Misc */

  {Comma}                                  { updateUnaryFlagAfter(ParserSym.COMMA); return symbol(ParserSym.COMMA); }
  {SemiColon}                              { updateUnaryFlagAfter(ParserSym.SEMI_COLON); return symbol(ParserSym.SEMI_COLON); }
  {Dot}                                    { updateUnaryFlagAfter(ParserSym.DOT); return symbol(ParserSym.DOT); }
  {DoubleDot}                              { updateUnaryFlagAfter(ParserSym.DOUBLE_DOT); return symbol(ParserSym.DOUBLE_DOT); }

    /* whitespace */
    {WhiteSpace}                   { /* ignore */ }

    /* error fallback */
    [^]                              { throw new UnknownCharacterException(yytext()); }


}
