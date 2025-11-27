package dev.murek.elixirij.lexer;

import com.intellij.lexer.FlexLexer;
import com.intellij.psi.tree.IElementType;
import static dev.murek.elixirij.psi.ExTypes.*;
import static com.intellij.psi.TokenType.WHITE_SPACE;
import static com.intellij.psi.TokenType.BAD_CHARACTER;

%%

%{
    public _ExLexer() {
        this((java.io.Reader)null);
    }
%}

%public
%class _ExLexer
%implements FlexLexer
%function advance
%type IElementType
%unicode

// Whitespace
HORIZONTAL_SPACE=[ \t\f]
EOL_CHAR=\r\n|\r|\n

// Numbers
DIGIT=[0-9]
DIGITS={DIGIT}(_?{DIGIT})*
HEX_DIGIT=[0-9a-fA-F]
HEX_DIGITS={HEX_DIGIT}(_?{HEX_DIGIT})*
OCTAL_DIGIT=[0-7]
OCTAL_DIGITS={OCTAL_DIGIT}(_?{OCTAL_DIGIT})*
BIN_DIGIT=[01]
BIN_DIGITS={BIN_DIGIT}(_?{BIN_DIGIT})*

DEC_INT={DIGITS}
HEX_INT=0[xX]{HEX_DIGITS}
OCTAL_INT=0[oO]{OCTAL_DIGITS}
BIN_INT=0[bB]{BIN_DIGITS}
INTEGER={DEC_INT}|{HEX_INT}|{OCTAL_INT}|{BIN_INT}

EXPONENT=[eE][+-]?{DIGITS}
FLOAT_LITERAL={DIGITS}\.{DIGITS}{EXPONENT}?|{DIGITS}{EXPONENT}

// Identifiers
LOWERCASE=[a-z_]
UPPERCASE=[A-Z]
ALNUM=[a-zA-Z0-9_]
IDENTIFIER_TAIL={ALNUM}*[!?]?

IDENTIFIER={LOWERCASE}{IDENTIFIER_TAIL}
ALIAS={UPPERCASE}{ALNUM}*

// Atoms
ATOM_HEAD={LOWERCASE}{IDENTIFIER_TAIL}

// Comments
COMMENT=#[^\r\n]*

// Sigil prefixes  
SIGIL_START=\~[a-zA-Z]

%%

<YYINITIAL> {
    // Whitespace
    {HORIZONTAL_SPACE}+                { return WHITE_SPACE; }
    {EOL_CHAR}                         { return EOL; }
    
    // Comments
    {COMMENT}                          { return COMMENT; }
    
    // Keywords (must come before identifiers)
    "true"                             { return TRUE; }
    "false"                            { return FALSE; }
    "nil"                              { return NIL; }
    "do"                               { return DO; }
    "end"                              { return END; }
    "fn"                               { return FN; }
    "after"                            { return AFTER; }
    "else"                             { return ELSE; }
    "catch"                            { return CATCH; }
    "rescue"                           { return RESCUE; }
    "when"                             { return WHEN; }
    "in"                               { return IN; }
    "not"                              { return NOT; }
    "and"                              { return AND; }
    "or"                               { return OR; }
    
    // Three-character operators (must come before two-character)
    "==="                              { return EQ_EQ_EQ; }
    "!=="                              { return NOT_EQ_EQ; }
    "<~>"                              { return LT_TILDE_GT; }
    "<<~"                              { return LT_LT_TILDE; }
    "~>>"                              { return TILDE_GT_GT; }
    "<<<"                              { return LT_LT_LT; }
    ">>>"                              { return GT_GT_GT; }
    "<|>"                              { return LT_PIPE_GT; }
    "..."                              { return DOT_DOT_DOT; }
    "+++"                              { return PLUS_PLUS_PLUS; }
    "---"                              { return MINUS_MINUS_MINUS; }
    "&&&"                              { return AMP_AMP_AMP; }
    "|||"                              { return PIPE_PIPE_PIPE; }
    "..//"                             { return DOT_DOT_SLASH_SLASH; }
    
    // Two-character operators
    "=="                               { return EQ_EQ; }
    "!="                               { return NOT_EQ; }
    "<="                               { return LT_EQ; }
    ">="                               { return GT_EQ; }
    "=~"                               { return EQ_TILDE; }
    "&&"                               { return AMP_AMP; }
    "||"                               { return PIPE_PIPE; }
    "->"                               { return ARROW; }
    "<-"                               { return LEFT_ARROW; }
    "=>"                               { return FAT_ARROW; }
    "|>"                               { return PIPE_GT; }
    "<~"                               { return LT_TILDE; }
    "~>"                               { return TILDE_GT; }
    ".."                               { return DOT_DOT; }
    "++"                               { return PLUS_PLUS; }
    "--"                               { return MINUS_MINUS; }
    "<>"                               { return LT_GT; }
    "**"                               { return STAR_STAR; }
    "::"                               { return COLON_COLON; }
    "\\\\"                             { return BACK_SLASH_BACK_SLASH; }
    "//"                               { return SLASH_SLASH; }
    "<<"                               { return LT_LT; }
    ">>"                               { return GT_GT; }
    "%{"                               { return PERCENT_LBRACE; }
    
    // Single-character operators
    "@"                                { return AT; }
    "!"                                { return EXCLAMATION; }
    "^"                                { return CARET; }
    "~"                                { return TILDE; }
    "&"                                { return AMPERSAND; }
    "+"                                { return PLUS; }
    "-"                                { return MINUS; }
    "*"                                { return STAR; }
    "/"                                { return SLASH; }
    "="                                { return EQ; }
    "<"                                { return LT; }
    ">"                                { return GT; }
    "|"                                { return PIPE; }
    
    // Delimiters
    "("                                { return LPAREN; }
    ")"                                { return RPAREN; }
    "["                                { return LBRACKET; }
    "]"                                { return RBRACKET; }
    "{"                                { return LBRACE; }
    "}"                                { return RBRACE; }
    
    // Punctuation
    "."                                { return DOT; }
    ","                                { return COMMA; }
    ":"                                { return COLON; }
    ";"                                { return SEMICOLON; }
    "%"                                { return PERCENT; }
    
    // Char literals - escape sequences
    "?\\n"                             { return CHAR; }
    "?\\r"                             { return CHAR; }
    "?\\t"                             { return CHAR; }
    "?\\s"                             { return CHAR; }
    "?\\0"                             { return CHAR; }
    "?\\\\"                            { return CHAR; }
    "?\\?"                             { return CHAR; }
    // Char literals - regular
    "?"[^\s]                           { return CHAR; }
    
    // Numbers
    {FLOAT_LITERAL}                    { return FLOAT; }
    {INTEGER}                          { return INTEGER; }
    
    // Atoms - operator atoms
    ":..."                             { return ATOM; }
    ":.."                              { return ATOM; }
    ":<<>>"                            { return ATOM; }
    ":%{}"                             { return ATOM; }
    ":{}"                              { return ATOM; }
    ":&"                               { return ATOM; }
    ":..//"                            { return ATOM; }
    ":+"                               { return ATOM; }
    ":-"                               { return ATOM; }
    ":*"                               { return ATOM; }
    ":/"                               { return ATOM; }
    ":!"                               { return ATOM; }
    ":^"                               { return ATOM; }
    ":|"                               { return ATOM; }
    ":@"                               { return ATOM; }
    ":<"                               { return ATOM; }
    ":>"                               { return ATOM; }
    ":="                               { return ATOM; }
    // Atoms - keyword atoms  
    ":" {ATOM_HEAD}                    { return ATOM; }
    // Atoms - quoted
    ":\"" [^\"]* "\""                  { return ATOM_QUOTED; }
    ":\'" [^\']* "\'"                  { return ATOM_QUOTED; }
    
    // Sigils - heredocs first
    {SIGIL_START} "\"\"\"" ~"\"\"\""   { return SIGIL; }
    {SIGIL_START} "\'\'\'" ~"\'\'\'"   { return SIGIL; }
    // Sigils - regular delimiters
    {SIGIL_START} "\"" [^\"]* "\""     { return SIGIL; }
    {SIGIL_START} "\'" [^\']* "\'"     { return SIGIL; }
    {SIGIL_START} "/" [^/]* "/"        { return SIGIL; }
    {SIGIL_START} "|" [^|]* "|"        { return SIGIL; }
    {SIGIL_START} "(" [^)]* ")"        { return SIGIL; }
    {SIGIL_START} "[" [^\]]* "]"       { return SIGIL; }
    {SIGIL_START} "{" [^}]* "}"        { return SIGIL; }
    {SIGIL_START} "<" [^>]* ">"        { return SIGIL; }
    
    // Strings - heredocs must be before regular strings
    "\"\"\"" ~"\"\"\""                 { return HEREDOC; }
    "\'\'\'" ~"\'\'\'"                 { return CHARLIST_HEREDOC; }
    
    // Strings - regular (simplified - handle escape sequences inside)
    "\"" ([^\\\"] | "\\" .)* "\""      { return STRING; }
    "\'" ([^\\\'] | "\\" .)* "\'"      { return CHARLIST; }
    
    // Identifiers and aliases
    {ALIAS}                            { return ALIAS; }
    {IDENTIFIER}                       { return IDENTIFIER; }
    
    // Catch all bad characters
    [^]                                { return BAD_CHARACTER; }
}
