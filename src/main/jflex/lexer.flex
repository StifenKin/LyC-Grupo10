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
       return symbol(ParserSym.AND_COND);
   }
   {OrCond}  {
       System.out.println("Token OR_COND encontrado: " + yytext());
       return symbol(ParserSym.OR_COND);
   }
   {NotCond} {
       System.out.println("Token NOT_COND encontrado: " + yytext());
       return symbol(ParserSym.NOT_COND);
   }

  /* Declaration */
  {Init}                                    { return symbol(ParserSym.INIT); }

  /* Logical */
  {If}                                     { return symbol(ParserSym.IF); }
  {Else}                                   { return symbol(ParserSym.ELSE); }
  {While}                                  { return symbol(ParserSym.WHILE); }

  /*Funciones especiales*/
  {IsZero}                                 { return symbol(ParserSym.ISZERO); }
  {DateConverted}                          { return symbol(ParserSym.DATECONVERTED); }
  {ConvDate}                               { return symbol(ParserSym.CONVDATE); }
  {Date}                                   { return symbol(ParserSym.DATE_LITERAL, yytext()); }


  /* Data types */
  {Int}                                     { return symbol(ParserSym.INT); }
  {Float}                                   { return symbol(ParserSym.FLOAT); }
  {String}                                  { return symbol(ParserSym.STRING); }

  /* I/O */
  {Write}                                  { return symbol(ParserSym.WRITE); }
  {Read}                                   { return symbol(ParserSym.READ); }


  /* Identifiers */
  {BooleanConstant}                         { return symbol(ParserSym.BOOLEAN_CONSTANT); }
  {Identifier}                             {
                                              if(yytext().length() > 15) {
                                                  throw new InvalidLengthException("Identifier length not allowed: " + yytext());
                                              }
                                              return symbol(ParserSym.IDENTIFIER, yytext());
                                          }
  /* Constants */



{IntegerConstant}                        {
                                                if(yytext().length() > 5 || Integer.valueOf(yytext()) > 65535) {
                                                    throw new InvalidIntegerException("Integer out of range: " + yytext());
                                                }

                                                if(!SymbolTableManager.existsInTable(yytext())){
                                                      SymbolEntry entry = new SymbolEntry("_"+yytext(), DataType.INTEGER_CONS, yytext());
                                                      SymbolTableManager.insertInTable(entry);
                                                }

                                                return symbol(ParserSym.INTEGER_CONSTANT, yytext());
                                            }

  {FloatConstant}                           { 
                                                String text = yytext();
                                                float value = Float.parseFloat(text);

                                                if (!Float.isFinite(value)) {
                                                  throw new InvalidFloatException("Float out of range: " + text);
                                                }

                                                if (!SymbolTableManager.existsInTable(text)) {
                                                    SymbolEntry entry = new SymbolEntry("_" + text, DataType.FLOAT_CONS, text);
                                                    SymbolTableManager.insertInTable(entry);
                                                }

                                                return symbol(ParserSym.FLOAT_CONSTANT, text);
                                            }



  {StringConstant}                         {    
                                                StringBuffer sb;
                                                sb = new StringBuffer(yytext());
                                                if(sb.length() > 52) //quotes add 2 to max length
                                                    throw new InvalidLengthException("String out of range: " + yytext());

                                                sb.replace(0,1,"");
                                                sb.replace(sb.length()-1,sb.length(),""); //trim extra quotes

                                                if(!SymbolTableManager.existsInTable(yytext())){
                                                      SymbolEntry entry = new SymbolEntry("_"+sb.toString(), DataType.STRING_CONS, sb.toString(), Integer.toString(sb.length()));
                                                      SymbolTableManager.insertInTable(entry);
                                                }

                                                return symbol(ParserSym.STRING_CONSTANT, yytext());
                                            }
  /*Declaration*/
  {Init}                                    { return symbol(ParserSym.INIT); }

  /* Operators */
  {Plus}                                    { return symbol(ParserSym.PLUS); }
  {Sub}                                     { return symbol(ParserSym.SUB); }
  {Mult}                                    { return symbol(ParserSym.MULT); }
  {Div}                                     { return symbol(ParserSym.DIV); }
  {Assig}                                   { return symbol(ParserSym.ASSIG); }
  {OpenBracket}                             { return symbol(ParserSym.OPEN_BRACKET); }
  {CloseBracket}                            { return symbol(ParserSym.CLOSE_BRACKET); }
  {OpenCurlyBrace}                          { return symbol(ParserSym.OPEN_CURLY_BRACKET); }
  {CloseCurlyBrace}                         { return symbol(ParserSym.CLOSE_CURLY_BRACKET); }
  {OpenSquareBracket}                       { return symbol(ParserSym.OPEN_SQUARE_BRACKET); }
  {CloseSquareBracket}                      { return symbol(ParserSym.CLOSE_SQUARE_BRACKET); }

  /* Comparators */
  {Mayor}                                  { return symbol(ParserSym.MAYOR); }
  {Lower}                                  { return symbol(ParserSym.LOWER); }
  {MayorI}                                 { return symbol(ParserSym.MAYOR_I); }
  {LowerI}                                 { return symbol(ParserSym.LOWER_I); }
  {Equal}                                  { return symbol(ParserSym.EQUAL); }
  {NotEqual}                               { return symbol(ParserSym.NOT_EQUAL); }

  /* Misc */

  {Comma}                                  { return symbol(ParserSym.COMMA); }
  {SemiColon}                              { return symbol(ParserSym.SEMI_COLON); }
  {Dot}                                    { return symbol(ParserSym.DOT); }
  {DoubleDot}                              { return symbol(ParserSym.DOUBLE_DOT); }

    /* whitespace */
    {WhiteSpace}                   { /* ignore */ }

    /* error fallback */
    [^]                              { throw new UnknownCharacterException(yytext()); }


}



