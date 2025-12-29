package dev.murek.elixirij.lang.lexer;

import com.intellij.lexer.FlexLexer;
import com.intellij.psi.tree.IElementType;
import static dev.murek.elixirij.lang.ElementTypes.*;

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
SIGIL_MODIFIERS=[a-zA-Z]*

%%

<YYINITIAL> {
    // Whitespace
    {HORIZONTAL_SPACE}+                { return WHITE_SPACE; }
    {EOL_CHAR}                         { return EX_EOL; }

    // Comments
    {COMMENT}                          { return EX_COMMENT; }

    // Keywords (must come before identifiers)
    "true"                             { return EX_TRUE; }
    "false"                            { return EX_FALSE; }
    "nil"                              { return EX_NIL; }
    "do"                               { return EX_DO; }
    "end"                              { return EX_END; }
    "fn"                               { return EX_FN; }
    "case"                             { return EX_CASE; }
    "cond"                             { return EX_COND; }
    "with"                             { return EX_WITH; }
    "try"                              { return EX_TRY; }
    "receive"                          { return EX_RECEIVE; }
    "defmodule"                        { return EX_DEFMODULE; }
    "def"                              { return EX_DEF; }
    "defp"                             { return EX_DEFP; }
    "defmacro"                         { return EX_DEFMACRO; }
    "defmacrop"                        { return EX_DEFMACROP; }
    "defguard"                         { return EX_DEFGUARD; }
    "defguardp"                        { return EX_DEFGUARDP; }
    "defstruct"                        { return EX_DEFSTRUCT; }
    "defexception"                     { return EX_DEFEXCEPTION; }
    "defprotocol"                      { return EX_DEFPROTOCOL; }
    "defimpl"                          { return EX_DEFIMPL; }
    "for" ":" / {HORIZONTAL_SPACE}     { return EX_FOR_COLON; }
    "import"                           { return EX_IMPORT; }
    "require"                          { return EX_REQUIRE; }
    "use"                              { return EX_USE; }
    "alias"                            { return EX_ALIAS_KW; }
    "as" ":" / {HORIZONTAL_SPACE}      { return EX_AS_COLON; }
    "for"                              { return EX_FOR; }
    "into" ":" / {HORIZONTAL_SPACE}    { return EX_INTO_COLON; }
    "uniq" ":" / {HORIZONTAL_SPACE}    { return EX_UNIQ_COLON; }
    "reduce" ":" / {HORIZONTAL_SPACE}  { return EX_REDUCE_COLON; }
    "quote"                            { return EX_QUOTE; }
    "unquote_splicing"                 { return EX_UNQUOTE_SPLICING; }
    "unquote"                          { return EX_UNQUOTE; }
    "raise"                            { return EX_RAISE; }
    "throw"                            { return EX_THROW; }
    "send"                             { return EX_SEND; }
    "super"                            { return EX_SUPER; }
    "after"                            { return EX_AFTER; }
    "else"                             { return EX_ELSE; }
    "catch"                            { return EX_CATCH; }
    "rescue"                           { return EX_RESCUE; }
    "when"                             { return EX_WHEN; }
    "in"                               { return EX_IN; }
    "not"                              { return EX_NOT; }
    "and"                              { return EX_AND; }
    "or"                               { return EX_OR; }

    // Three-character operators (must come before two-character)
    "==="                              { return EX_EQ_EQ_EQ; }
    "!=="                              { return EX_NOT_EQ_EQ; }
    "<~>"                              { return EX_LT_TILDE_GT; }
    "<<~"                              { return EX_LT_LT_TILDE; }
    "~>>"                              { return EX_TILDE_GT_GT; }
    "<<<"                              { return EX_LT_LT_LT; }
    ">>>"                              { return EX_GT_GT_GT; }
    "<|>"                              { return EX_LT_PIPE_GT; }
    "..."                              { return EX_DOT_DOT_DOT; }
    "+++"                              { return EX_PLUS_PLUS_PLUS; }
    "---"                              { return EX_MINUS_MINUS_MINUS; }
    "&&&"                              { return EX_AMP_AMP_AMP; }
    "|||"                              { return EX_PIPE_PIPE_PIPE; }
    "..//"                             { return EX_DOT_DOT_SLASH_SLASH; }

    // Two-character operators
    "=="                               { return EX_EQ_EQ; }
    "!="                               { return EX_NOT_EQ; }
    "<="                               { return EX_LT_EQ; }
    ">="                               { return EX_GT_EQ; }
    "=~"                               { return EX_EQ_TILDE; }
    "&&"                               { return EX_AMP_AMP; }
    "||"                               { return EX_PIPE_PIPE; }
    "->"                               { return EX_ARROW; }
    "<-"                               { return EX_LEFT_ARROW; }
    "=>"                               { return EX_FAT_ARROW; }
    "|>"                               { return EX_PIPE_GT; }
    "<~"                               { return EX_LT_TILDE; }
    "~>"                               { return EX_TILDE_GT; }
    ".."                               { return EX_DOT_DOT; }
    "++"                               { return EX_PLUS_PLUS; }
    "--"                               { return EX_MINUS_MINUS; }
    "<>"                               { return EX_LT_GT; }
    "**"                               { return EX_STAR_STAR; }
    "::"                               { return EX_COLON_COLON; }
    "\\\\"                             { return EX_BACK_SLASH_BACK_SLASH; }
    "//"                               { return EX_SLASH_SLASH; }
    "<<"                               { return EX_LT_LT; }
    ">>"                               { return EX_GT_GT; }
    "%{"                               { return EX_PERCENT_LBRACE; }

    // Single-character operators
    "@"                                { return EX_AT; }
    "!"                                { return EX_EXCLAMATION; }
    "^"                                { return EX_CARET; }
    "~"                                { return EX_TILDE; }
    "&"                                { return EX_AMPERSAND; }
    "+"                                { return EX_PLUS; }
    "-"                                { return EX_MINUS; }
    "*"                                { return EX_STAR; }
    "/"                                { return EX_SLASH; }
    "="                                { return EX_EQ; }
    "<"                                { return EX_LT; }
    ">"                                { return EX_GT; }
    "|"                                { return EX_PIPE; }

    // Delimiters
    "("                                { return EX_LPAREN; }
    ")"                                { return EX_RPAREN; }
    "["                                { return EX_LBRACKET; }
    "]"                                { return EX_RBRACKET; }
    "{"                                { return EX_LBRACE; }
    "}"                                { return EX_RBRACE; }

    // Punctuation
    "."                                { return EX_DOT; }
    ","                                { return EX_COMMA; }
    ":"                                { return EX_COLON; }
    ";"                                { return EX_SEMICOLON; }
    "%"                                { return EX_PERCENT; }

    // Char literals - escape sequences
    "?\\n"                             { return EX_CHAR; }
    "?\\r"                             { return EX_CHAR; }
    "?\\t"                             { return EX_CHAR; }
    "?\\s"                             { return EX_CHAR; }
    "?\\0"                             { return EX_CHAR; }
    "?\\\\"                            { return EX_CHAR; }
    "?\\?"                             { return EX_CHAR; }
    // Char literals - regular
    "?"[^\s]                           { return EX_CHAR; }

    // Numbers
    {FLOAT_LITERAL}                    { return EX_FLOAT; }
    {INTEGER}                          { return EX_INTEGER; }

    // Atoms - operator atoms
    ":..."                             { return EX_ATOM; }
    ":.."                              { return EX_ATOM; }
    ":<<>>"                            { return EX_ATOM; }
    ":%{}"                             { return EX_ATOM; }
    ":{}"                              { return EX_ATOM; }
    ":&&&"                             { return EX_ATOM; }
    ":&"                               { return EX_ATOM; }
    ":..//"                            { return EX_ATOM; }
    ":+"                               { return EX_ATOM; }
    ":-"                               { return EX_ATOM; }
    ":*"                               { return EX_ATOM; }
    ":/"                               { return EX_ATOM; }
    ":!"                               { return EX_ATOM; }
    ":^"                               { return EX_ATOM; }
    ":|||"                             { return EX_ATOM; }
    ":||"                              { return EX_ATOM; }
    ":|"                               { return EX_ATOM; }
    ":@"                               { return EX_ATOM; }
    ":<"                               { return EX_ATOM; }
    ":>"                               { return EX_ATOM; }
    ":="                               { return EX_ATOM; }
    ":&&"                              { return EX_ATOM; }
    // Atoms - keyword atoms
    ":" {ATOM_HEAD}                    { return EX_ATOM; }
    // Atoms - quoted
    ":\"" [^\"]* "\""                  { return EX_ATOM_QUOTED; }
    ":\'" [^\']* "\'"                  { return EX_ATOM_QUOTED; }

    // Sigils - heredocs first
    {SIGIL_START} "\"\"\"" ~"\"\"\"" {SIGIL_MODIFIERS}   { return EX_SIGIL; }
    {SIGIL_START} "\'\'\'" ~"\'\'\'" {SIGIL_MODIFIERS}   { return EX_SIGIL; }
    // Sigils - regular delimiters
    {SIGIL_START} "\"" [^\"]* "\"" {SIGIL_MODIFIERS}     { return EX_SIGIL; }
    {SIGIL_START} "\'" [^\']* "\'" {SIGIL_MODIFIERS}     { return EX_SIGIL; }
    {SIGIL_START} "/" [^/]* "/" {SIGIL_MODIFIERS}        { return EX_SIGIL; }
    {SIGIL_START} "|" [^|]* "|" {SIGIL_MODIFIERS}        { return EX_SIGIL; }
    {SIGIL_START} "(" [^)]* ")" {SIGIL_MODIFIERS}        { return EX_SIGIL; }
    {SIGIL_START} "[" [^\]]* "]" {SIGIL_MODIFIERS}       { return EX_SIGIL; }
    {SIGIL_START} "{" [^}]* "}" {SIGIL_MODIFIERS}        { return EX_SIGIL; }
    {SIGIL_START} "<" [^>]* ">" {SIGIL_MODIFIERS}        { return EX_SIGIL; }

    // Strings - heredocs must be before regular strings
    "\"\"\"" ~"\"\"\""                 { return EX_HEREDOC; }
    "\'\'\'" ~"\'\'\'"                 { return EX_CHARLIST_HEREDOC; }

    // Strings - regular (simplified - handle escape sequences inside)
    "\"" ([^\\\"] | "\\" .)* "\""      { return EX_STRING; }
    "\'" ([^\\\'] | "\\" .)* "\'"      { return EX_CHARLIST; }

    // Identifiers and aliases
    {IDENTIFIER} ":" / {HORIZONTAL_SPACE} { return EX_KW_IDENTIFIER; }
    {ALIAS}                            { return EX_ALIAS; }
    {IDENTIFIER}                       { return EX_IDENTIFIER; }

    // Catch all bad characters
    [^]                                { return BAD_CHARACTER; }
}
