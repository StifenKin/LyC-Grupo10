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


%{
  private Symbol symbol(int type) {
    return new Symbol(type, yyline, yycolumn);
  }
  private Symbol symbol(int type, Object value) {
    return new Symbol(type, yyline, yycolumn, value);
  }
  StringBuffer sb;
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
ConvDate = "convDate"

FloatConstant = -?[0-9]+(\.[0-9]+)?



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

TraditionalComment = "#+" [^#]* "+#"
NestedComment = "#+" ([^#] | {TraditionalComment})* "+#"
Comment = {TraditionalComment} | {NestedComment}


WhiteSpace = {LineTerminator} | {Identation}
Identifier = {Letter} ({Letter}|{Digit})*
BooleanConstant = {True}|{False}
IntegerConstant = {Digit}+
InvalidIntegerConstant = 0+{Digit19}+
FloatConstant = (({Digit}|{Digit19}{Digit}+)?\.{Digit}+)
StringConstant = \"(([^\"\n]*)\")

%%

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


/* keywords */
<YYINITIAL> {

  /* Declaration */
  {Init}                                    { return symbol(ParserSym.INIT); }

  /* Logical */
  {If}                                     { return symbol(ParserSym.IF); }
  {Else}                                   { return symbol(ParserSym.ELSE); }
  {While}                                  { return symbol(ParserSym.WHILE); }

  /*Funciones especiales*/
  {IsZero}                              { return symbol(ParserSym.ISZERO); }
  {ConvDate}                          { return symbol(ParserSym.CONVDATE); }


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

                                              System.out.println("Token IDENTIFIER encontrado: " + yytext());
                                              if(!SymbolTableManager.existsInTable(yytext())){
                                                    SymbolEntry entry = new SymbolEntry(yytext());
                                                    SymbolTableManager.insertInTable(entry);
                                                     System.out.println("Token IDENTIFIER encontrado si no existe: " + yytext());
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

                                                System.out.println("Token FLOAT encontrado: " + text);

                                                String[] num = text.split("\\.");

                                                String exp = num[0].isEmpty() ? "0" : num[0];
                                                String mantissa = num.length > 1 ? num[1] : "0";

                                            if (exp.length() > 3 || Integer.parseInt(exp) > 256) {
                                                throw new InvalidFloatException("Exponent out of range: " + text);
                                            }

                                            if (mantissa.length() > 8 || Integer.parseInt(mantissa) > 16777216) {
                                                throw new InvalidFloatException("Mantissa out of range: " + text);
                                            }

                                            if (!SymbolTableManager.existsInTable(text)) {
                                                SymbolEntry entry = new SymbolEntry("_" + text, DataType.FLOAT_CONS, text);
                                                SymbolTableManager.insertInTable(entry);
                                            }

                                            return symbol(ParserSym.FLOAT_CONSTANT, text);
                                            }



  {StringConstant}                         {
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
  {Assig}                                   { System.out.println("Token ASSIG encontrado" + yytext()); return symbol(ParserSym.ASSIG); }
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
}


   /* Comments */
   {Comment}                                { /* ignore */ }


/* error fallback */
[^]                              { throw new UnknownCharacterException(yytext()); }
