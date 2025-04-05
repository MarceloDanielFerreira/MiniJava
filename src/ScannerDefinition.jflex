/*package de.jflex.example.standalone;*/
import java_cup.runtime.*;
%%

%public
%class Scanner
%cup
%unicode
%line
%column

%{
  private Symbol symbol(int type, Object value) {
    return new Symbol(type, yyline+1, yycolumn+1, value);
  }
%}

LineTerminator = \r|\n|\r\n
WhiteSpace     = {LineTerminator} | [ \t\f]

Digit          = [0-9]
Integer        = {Digit}+
Identifier     = [a-zA-Z][a-zA-Z0-9_]*
BlockComment   = "/*" [^*] ~"*/" | "/*" "*"+ "/"

%%

/* keywords */
"public"       { return symbol(sym.PUBLIC, yytext()); }
"static"       { return symbol(sym.STATIC, yytext()); }
"void"         { return symbol(sym.VOID, yytext()); }
"main"         { return symbol(sym.MAIN, yytext()); }
"class"        { return symbol(sym.CLASS, yytext()); }
"extends"      { return symbol(sym.EXTENDS, yytext()); }
"int"          { return symbol(sym.INT, yytext()); }
"if"           { return symbol(sym.IF, yytext()); }
"else"         { return symbol(sym.ELSE, yytext()); }
"while"        { return symbol(sym.WHILE, yytext()); }
"System.out.println" { return symbol(sym.PRINT, yytext()); }
"return"       { return symbol(sym.RETURN, yytext()); }
"this"         { return symbol(sym.THIS, yytext()); }
"new"          { return symbol(sym.NEW, yytext()); }
"String"       { return symbol(sym.STRING, yytext()); }
"length" { return symbol(sym.LENGTH, yytext()); }

/* operators */
"+"            { return symbol(sym.PLUS, yytext()); }
"-"            { return symbol(sym.MINUS, yytext()); }
"*"            { return symbol(sym.MULT, yytext()); }
"/"            { return symbol(sym.DIV, yytext()); }
"="            { return symbol(sym.EQ, yytext()); }
"!="           { return symbol(sym.NOTEQ, yytext()); }
"<"            { return symbol(sym.LT, yytext()); }
">"            { return symbol(sym.GT, yytext()); }
"&&"           { return symbol(sym.AND, yytext()); }
"||"          { return symbol(sym.OR, yytext()); }
"=="          { return symbol(sym.EQUAL, yytext()); }

	


/* delimiters */
"("            { return symbol(sym.O_PAREN, yytext()); }
")"            { return symbol(sym.C_PAREN, yytext()); }
"{"            { return symbol(sym.O_CBRACKET, yytext()); }
"}"            { return symbol(sym.C_CBRACKET, yytext()); }
"["            { return symbol(sym.O_SBRACKET, yytext()); }
"]"            { return symbol(sym.C_SBRACKET, yytext()); }
";"            { return symbol(sym.SEMICOLON, yytext()); }
","            { return symbol(sym.COMMA, yytext()); }
"."            { return symbol(sym.DOT, yytext()); }

/* literals */
{Integer}      { return symbol(sym.INTEGER_LITERAL, new Integer(yytext())); }
{Identifier}   { return symbol(sym.IDENTIFIER, yytext()); }

{WhiteSpace}   { /* ignore */ }

/* comments */
{BlockComment} { /* ignore block comments */ }
"//".*         { /* ignore line comments */ }

.              { System.err.println(
                   "\nunexpected character in input: '" + yytext() + "' at line " +
                   (yyline+1) + " column " + (yycolumn+1));
                }