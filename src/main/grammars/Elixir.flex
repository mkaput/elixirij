package dev.murek.elixirij.lang.lexer;

import com.intellij.lexer.FlexLexer;
import com.intellij.psi.StringEscapesTokenTypes;
import com.intellij.psi.tree.IElementType;
import static dev.murek.elixirij.lang.ElementTypes.*;

%%

%{
    private int interpolationBalance = 0;
    private int interpolationReturnState = YYINITIAL;
    private int stringReturnState = YYINITIAL;
    private int[] interpolationStateStack = new int[8];
    private int[] interpolationBalanceStack = new int[8];
    private int interpolationStackTop = 0;

    public _ExLexer() {
        this((java.io.Reader)null);
    }

    private IElementType beginInterpolation(int returnState) {
        if (interpolationBalance > 0) {
            if (interpolationStackTop == interpolationStateStack.length) {
                int newSize = interpolationStateStack.length * 2;
                int[] newStateStack = new int[newSize];
                int[] newBalanceStack = new int[newSize];
                System.arraycopy(interpolationStateStack, 0, newStateStack, 0, interpolationStateStack.length);
                System.arraycopy(interpolationBalanceStack, 0, newBalanceStack, 0, interpolationBalanceStack.length);
                interpolationStateStack = newStateStack;
                interpolationBalanceStack = newBalanceStack;
            }
            interpolationStateStack[interpolationStackTop] = interpolationReturnState;
            interpolationBalanceStack[interpolationStackTop] = interpolationBalance;
            interpolationStackTop++;
        }
        interpolationBalance = 1;
        interpolationReturnState = returnState;
        yybegin(IN_INTERPOLATION);
        return EX_INTERPOLATION_START;
    }
%}

%public
%class _ExLexer
%implements FlexLexer
%function advance
%type IElementType
%unicode
%state IN_STRING IN_HEREDOC IN_CHARLIST IN_CHARLIST_HEREDOC IN_INTERPOLATION
%state IN_SIGIL_PAREN IN_SIGIL_BRACKET IN_SIGIL_BRACE IN_SIGIL_ANGLE IN_SIGIL_SLASH IN_SIGIL_PIPE IN_SIGIL_DQUOTE IN_SIGIL_SQUOTE
%state IN_SIGIL_HEREDOC_DQUOTE IN_SIGIL_HEREDOC_SQUOTE

// Whitespace
HORIZONTAL_SPACE=[ \t\f]
EOL_CHAR=\r\n|\r|\n

// Numbers
DIGIT=[0-9]
DIGITS={DIGIT}(_?{DIGIT})*
HEX_DIGIT=[0-9a-fA-F]
HEX_DIGITS={HEX_DIGIT}(_?{HEX_DIGIT})*
UNICODE_SHORT=\\u{HEX_DIGIT}{4}
UNICODE_BRACED=\\u\{{HEX_DIGIT}{1,6}\}
HEX_ESC=\\x{HEX_DIGIT}{2}
SIMPLE_ESC=\\[0abdefnrstv\\\"']
ESCAPED_NEWLINE=\\{EOL_CHAR}
GENERIC_ESC=\\[^\r\n]
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
SIGIL_START_INTERP=\~[a-z]
SIGIL_START_NO_INTERP=\~[A-Z]
SIGIL_MODIFIERS=[a-zA-Z]*
ESCAPED_SLASH=\\\/
SIGIL_SLASH_CONTENT=({ESCAPED_SLASH}|[^/])*

// Interpolation (simplified - deprecated patterns)

%%

<IN_INTERPOLATION> {
    "%{"                               { interpolationBalance++; return EX_PERCENT_LBRACE; }
    "}"                                {
                                          interpolationBalance--;
                                          if (interpolationBalance == 0) {
                                              int returnState = interpolationReturnState;
                                              if (interpolationStackTop > 0) {
                                                  interpolationStackTop--;
                                                  interpolationReturnState = interpolationStateStack[interpolationStackTop];
                                                  interpolationBalance = interpolationBalanceStack[interpolationStackTop];
                                              }
                                              yybegin(returnState);
                                              return EX_INTERPOLATION_END;
                                          }
                                          return EX_RBRACE;
                                      }
    "{"                                { interpolationBalance++; return EX_LBRACE; }
}

<YYINITIAL, IN_INTERPOLATION> {
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
    ":..//"                            { return EX_ATOM; }
    ":<<>>"                            { return EX_ATOM; }
    ":%{}"                             { return EX_ATOM; }
    ":<~>"                             { return EX_ATOM; }
    ":<<~"                             { return EX_ATOM; }
    ":~>>"                             { return EX_ATOM; }
    ":<<<"                             { return EX_ATOM; }
    ":>>>"                             { return EX_ATOM; }
    ":<|>"                             { return EX_ATOM; }
    ":==="                             { return EX_ATOM; }
    ":!=="                             { return EX_ATOM; }
    ":..."                             { return EX_ATOM; }
    ":+++"                             { return EX_ATOM; }
    ":---"                             { return EX_ATOM; }
    ":&&&"                             { return EX_ATOM; }
    ":|||"                             { return EX_ATOM; }
    ":.."                              { return EX_ATOM; }
    ":=="                              { return EX_ATOM; }
    ":!="                              { return EX_ATOM; }
    ":<="                              { return EX_ATOM; }
    ":>="                              { return EX_ATOM; }
    ":=~"                              { return EX_ATOM; }
    ":&&"                              { return EX_ATOM; }
    ":||"                              { return EX_ATOM; }
    ":->"                              { return EX_ATOM; }
    ":<-"                              { return EX_ATOM; }
    ":=>"                              { return EX_ATOM; }
    ":|>"                              { return EX_ATOM; }
    ":<~"                              { return EX_ATOM; }
    ":~>"                              { return EX_ATOM; }
    ":++"                              { return EX_ATOM; }
    ":--"                              { return EX_ATOM; }
    ":<>"                              { return EX_ATOM; }
    ":**"                              { return EX_ATOM; }
    ":::"                              { return EX_ATOM; }
    ":\\\\"                            { return EX_ATOM; }
    "://"                              { return EX_ATOM; }
    ":<<"                              { return EX_ATOM; }
    ":>>"                              { return EX_ATOM; }
    ":{}"                              { return EX_ATOM; }
    ":&"                               { return EX_ATOM; }
    ":@"                               { return EX_ATOM; }
    ":+"                               { return EX_ATOM; }
    ":-"                               { return EX_ATOM; }
    ":*"                               { return EX_ATOM; }
    ":/"                               { return EX_ATOM; }
    ":!"                               { return EX_ATOM; }
    ":^"                               { return EX_ATOM; }
    ":~"                               { return EX_ATOM; }
    ":="                               { return EX_ATOM; }
    ":<"                               { return EX_ATOM; }
    ":>"                               { return EX_ATOM; }
    ":|"                               { return EX_ATOM; }
    ":."                               { return EX_ATOM; }
    // Atoms - keyword atoms
    ":" {ATOM_HEAD}                    { return EX_ATOM; }
    // Atoms - quoted
    ":\"" [^\"]* "\""                  { return EX_ATOM_QUOTED; }
    ":\'" [^\']* "\'"                  { return EX_ATOM_QUOTED; }

    // Interpolated sigils (lowercase)
    {SIGIL_START_INTERP} "\"\"\""      { stringReturnState = yystate(); yybegin(IN_SIGIL_HEREDOC_DQUOTE); return EX_STRING_BEGIN; }
    {SIGIL_START_INTERP} "\'\'\'"      { stringReturnState = yystate(); yybegin(IN_SIGIL_HEREDOC_SQUOTE); return EX_STRING_BEGIN; }
    {SIGIL_START_INTERP} "\""          { stringReturnState = yystate(); yybegin(IN_SIGIL_DQUOTE); return EX_STRING_BEGIN; }
    {SIGIL_START_INTERP} "\'"          { stringReturnState = yystate(); yybegin(IN_SIGIL_SQUOTE); return EX_STRING_BEGIN; }
    {SIGIL_START_INTERP} "/"           { stringReturnState = yystate(); yybegin(IN_SIGIL_SLASH); return EX_STRING_BEGIN; }
    {SIGIL_START_INTERP} "|"           { stringReturnState = yystate(); yybegin(IN_SIGIL_PIPE); return EX_STRING_BEGIN; }
    {SIGIL_START_INTERP} "("           { stringReturnState = yystate(); yybegin(IN_SIGIL_PAREN); return EX_STRING_BEGIN; }
    {SIGIL_START_INTERP} "["           { stringReturnState = yystate(); yybegin(IN_SIGIL_BRACKET); return EX_STRING_BEGIN; }
    {SIGIL_START_INTERP} "{"           { stringReturnState = yystate(); yybegin(IN_SIGIL_BRACE); return EX_STRING_BEGIN; }
    {SIGIL_START_INTERP} "<"           { stringReturnState = yystate(); yybegin(IN_SIGIL_ANGLE); return EX_STRING_BEGIN; }

    // Sigils (non-interpolated, uppercase)
    {SIGIL_START_NO_INTERP} "\"\"\"" ~"\"\"\"" {SIGIL_MODIFIERS}   { return EX_SIGIL; }
    {SIGIL_START_NO_INTERP} "\'\'\'" ~"\'\'\'" {SIGIL_MODIFIERS}   { return EX_SIGIL; }
    {SIGIL_START_NO_INTERP} "\"" [^\"]* "\"" {SIGIL_MODIFIERS}     { return EX_SIGIL; }
    {SIGIL_START_NO_INTERP} "\'" [^\']* "\'" {SIGIL_MODIFIERS}     { return EX_SIGIL; }
    {SIGIL_START_NO_INTERP} "/" {SIGIL_SLASH_CONTENT} "/" {SIGIL_MODIFIERS} { return EX_SIGIL; }
    {SIGIL_START_NO_INTERP} "|" [^|]* "|" {SIGIL_MODIFIERS}        { return EX_SIGIL; }
    {SIGIL_START_NO_INTERP} "(" [^)]* ")" {SIGIL_MODIFIERS}        { return EX_SIGIL; }
    {SIGIL_START_NO_INTERP} "[" [^\]]* "]" {SIGIL_MODIFIERS}       { return EX_SIGIL; }
    {SIGIL_START_NO_INTERP} "{" [^}]* "}" {SIGIL_MODIFIERS}        { return EX_SIGIL; }
    {SIGIL_START_NO_INTERP} "<" [^>]* ">" {SIGIL_MODIFIERS}        { return EX_SIGIL; }

    // Strings - heredocs must be before regular strings
    "\"\"\""                           { stringReturnState = yystate(); yybegin(IN_HEREDOC); return EX_STRING_BEGIN; }
    "\'\'\'"                           { stringReturnState = yystate(); yybegin(IN_CHARLIST_HEREDOC); return EX_CHARLIST_BEGIN; }

    // Strings - regular
    "\""                               { stringReturnState = yystate(); yybegin(IN_STRING); return EX_STRING_BEGIN; }
    "\'"                               { stringReturnState = yystate(); yybegin(IN_CHARLIST); return EX_CHARLIST_BEGIN; }

    // Identifiers and aliases
    {IDENTIFIER} ":" / ({HORIZONTAL_SPACE}|{EOL_CHAR}) { return EX_KW_IDENTIFIER; }
    {ALIAS}                            { return EX_ALIAS; }
    {IDENTIFIER}                       { return EX_IDENTIFIER; }

    // Catch all bad characters
    [^]                                { return BAD_CHARACTER; }
}

<IN_STRING> {
    {ESCAPED_NEWLINE}                  { return StringEscapesTokenTypes.VALID_STRING_ESCAPE_TOKEN; }
    {UNICODE_BRACED}                   { return StringEscapesTokenTypes.VALID_STRING_ESCAPE_TOKEN; }
    {UNICODE_SHORT}                    { return StringEscapesTokenTypes.VALID_STRING_ESCAPE_TOKEN; }
    {HEX_ESC}                          { return StringEscapesTokenTypes.VALID_STRING_ESCAPE_TOKEN; }
    {SIMPLE_ESC}                       { return StringEscapesTokenTypes.VALID_STRING_ESCAPE_TOKEN; }
    \\u\{\}                            { return StringEscapesTokenTypes.INVALID_UNICODE_ESCAPE_TOKEN; }
    \\u\{{HEX_DIGIT}{7}{HEX_DIGIT}*\}  { return StringEscapesTokenTypes.INVALID_UNICODE_ESCAPE_TOKEN; }
    \\u\{[^}\r\n]*\}                   { return StringEscapesTokenTypes.INVALID_UNICODE_ESCAPE_TOKEN; }
    \\u[^0-9a-fA-F\{\r\n\"]            { return StringEscapesTokenTypes.INVALID_UNICODE_ESCAPE_TOKEN; }
    \\u{HEX_DIGIT}{0,3}                { return StringEscapesTokenTypes.INVALID_UNICODE_ESCAPE_TOKEN; }
    \\x[^0-9a-fA-F\r\n\"]              { return StringEscapesTokenTypes.INVALID_CHARACTER_ESCAPE_TOKEN; }
    \\x{HEX_DIGIT}?                    { return StringEscapesTokenTypes.INVALID_CHARACTER_ESCAPE_TOKEN; }
    {GENERIC_ESC}                      { return StringEscapesTokenTypes.VALID_STRING_ESCAPE_TOKEN; }
    "#{"                               { return beginInterpolation(IN_STRING); }
    "\""                               { yybegin(stringReturnState); return EX_STRING_END; }
    [^\"#\\]+                          { return EX_STRING_PART; }
    "#"                                { return EX_STRING_PART; }
    "\\"                               { return EX_STRING_PART; }
    .                                  { return EX_STRING_PART; }
}

<IN_HEREDOC> {
    "\"\"\""                           { yybegin(stringReturnState); return EX_STRING_END; }
    {ESCAPED_NEWLINE}                  { return StringEscapesTokenTypes.VALID_STRING_ESCAPE_TOKEN; }
    {UNICODE_BRACED}                   { return StringEscapesTokenTypes.VALID_STRING_ESCAPE_TOKEN; }
    {UNICODE_SHORT}                    { return StringEscapesTokenTypes.VALID_STRING_ESCAPE_TOKEN; }
    {HEX_ESC}                          { return StringEscapesTokenTypes.VALID_STRING_ESCAPE_TOKEN; }
    {SIMPLE_ESC}                       { return StringEscapesTokenTypes.VALID_STRING_ESCAPE_TOKEN; }
    \\u\{\}                            { return StringEscapesTokenTypes.INVALID_UNICODE_ESCAPE_TOKEN; }
    \\u\{{HEX_DIGIT}{7}{HEX_DIGIT}*\}  { return StringEscapesTokenTypes.INVALID_UNICODE_ESCAPE_TOKEN; }
    \\u\{[^}\r\n]*\}                   { return StringEscapesTokenTypes.INVALID_UNICODE_ESCAPE_TOKEN; }
    \\u[^0-9a-fA-F\{\r\n\"]            { return StringEscapesTokenTypes.INVALID_UNICODE_ESCAPE_TOKEN; }
    \\u{HEX_DIGIT}{0,3}                { return StringEscapesTokenTypes.INVALID_UNICODE_ESCAPE_TOKEN; }
    \\x[^0-9a-fA-F\r\n\"]              { return StringEscapesTokenTypes.INVALID_CHARACTER_ESCAPE_TOKEN; }
    \\x{HEX_DIGIT}?                    { return StringEscapesTokenTypes.INVALID_CHARACTER_ESCAPE_TOKEN; }
    {GENERIC_ESC}                      { return StringEscapesTokenTypes.VALID_STRING_ESCAPE_TOKEN; }
    "#{"                               { return beginInterpolation(IN_HEREDOC); }
    [^\"#]+                            { return EX_STRING_PART; }
    "\""                               { return EX_STRING_PART; }
    "#"                                { return EX_STRING_PART; }
    "\\"                               { return EX_STRING_PART; }
    .                                  { return EX_STRING_PART; }
}

<IN_CHARLIST> {
    \\\\#\{                            { return EX_CHARLIST_PART; }
    \\#\{                              { return EX_CHARLIST_PART; }
    "#{"                               { return beginInterpolation(IN_CHARLIST); }
    "\'"                               { yybegin(stringReturnState); return EX_CHARLIST_END; }
    [^\'#\\]+                          { return EX_CHARLIST_PART; }
    \\.                               { return EX_CHARLIST_PART; }
    "#"                                { return EX_CHARLIST_PART; }
    "\\"                               { return EX_CHARLIST_PART; }
    .                                  { return EX_CHARLIST_PART; }
}

<IN_CHARLIST_HEREDOC> {
    "\'\'\'"                           { yybegin(stringReturnState); return EX_CHARLIST_END; }
    \\\\#\{                            { return EX_CHARLIST_PART; }
    \\#\{                              { return EX_CHARLIST_PART; }
    "#{"                               { return beginInterpolation(IN_CHARLIST_HEREDOC); }
    [^\'#]+                            { return EX_CHARLIST_PART; }
    "\'"                               { return EX_CHARLIST_PART; }
    "#"                                { return EX_CHARLIST_PART; }
    "\\"                               { return EX_CHARLIST_PART; }
    .                                  { return EX_CHARLIST_PART; }
}

<IN_SIGIL_PAREN> {
    \\\\#\{                            { return EX_STRING_PART; }
    \\#\{                              { return EX_STRING_PART; }
    "#{"                               { return beginInterpolation(IN_SIGIL_PAREN); }
    ")" {SIGIL_MODIFIERS}              { yybegin(stringReturnState); return EX_STRING_END; }
    [^\\)#]+                           { return EX_STRING_PART; }
    \\.                               { return EX_STRING_PART; }
    "#"                                { return EX_STRING_PART; }
    "\\"                               { return EX_STRING_PART; }
    .                                  { return EX_STRING_PART; }
}

<IN_SIGIL_BRACKET> {
    \\\\#\{                            { return EX_STRING_PART; }
    \\#\{                              { return EX_STRING_PART; }
    "#{"                               { return beginInterpolation(IN_SIGIL_BRACKET); }
    "]" {SIGIL_MODIFIERS}              { yybegin(stringReturnState); return EX_STRING_END; }
    [^]#\\]+                           { return EX_STRING_PART; }
    \\.                               { return EX_STRING_PART; }
    "#"                                { return EX_STRING_PART; }
    "\\"                               { return EX_STRING_PART; }
    .                                  { return EX_STRING_PART; }
}

<IN_SIGIL_BRACE> {
    \\\\#\{                            { return EX_STRING_PART; }
    \\#\{                              { return EX_STRING_PART; }
    "#{"                               { return beginInterpolation(IN_SIGIL_BRACE); }
    "}" {SIGIL_MODIFIERS}              { yybegin(stringReturnState); return EX_STRING_END; }
    [^\\}#]+                           { return EX_STRING_PART; }
    \\.                               { return EX_STRING_PART; }
    "#"                                { return EX_STRING_PART; }
    "\\"                               { return EX_STRING_PART; }
    .                                  { return EX_STRING_PART; }
}

<IN_SIGIL_ANGLE> {
    \\\\#\{                            { return EX_STRING_PART; }
    \\#\{                              { return EX_STRING_PART; }
    "#{"                               { return beginInterpolation(IN_SIGIL_ANGLE); }
    ">" {SIGIL_MODIFIERS}              { yybegin(stringReturnState); return EX_STRING_END; }
    [^\\>#]+                           { return EX_STRING_PART; }
    \\.                               { return EX_STRING_PART; }
    "#"                                { return EX_STRING_PART; }
    "\\"                               { return EX_STRING_PART; }
    .                                  { return EX_STRING_PART; }
}

<IN_SIGIL_SLASH> {
    \\\\#\{                            { return EX_STRING_PART; }
    \\#\{                              { return EX_STRING_PART; }
    "#{"                               { return beginInterpolation(IN_SIGIL_SLASH); }
    "/" {SIGIL_MODIFIERS}              { yybegin(stringReturnState); return EX_STRING_END; }
    [^\\/#]+                           { return EX_STRING_PART; }
    \\.                               { return EX_STRING_PART; }
    "#"                                { return EX_STRING_PART; }
    "\\"                               { return EX_STRING_PART; }
    .                                  { return EX_STRING_PART; }
}

<IN_SIGIL_PIPE> {
    \\\\#\{                            { return EX_STRING_PART; }
    \\#\{                              { return EX_STRING_PART; }
    "#{"                               { return beginInterpolation(IN_SIGIL_PIPE); }
    "|" {SIGIL_MODIFIERS}              { yybegin(stringReturnState); return EX_STRING_END; }
    [^\\|#]+                           { return EX_STRING_PART; }
    \\.                               { return EX_STRING_PART; }
    "#"                                { return EX_STRING_PART; }
    "\\"                               { return EX_STRING_PART; }
    .                                  { return EX_STRING_PART; }
}

<IN_SIGIL_DQUOTE> {
    \\\\#\{                            { return EX_STRING_PART; }
    \\#\{                              { return EX_STRING_PART; }
    "#{"                               { return beginInterpolation(IN_SIGIL_DQUOTE); }
    "\"" {SIGIL_MODIFIERS}             { yybegin(stringReturnState); return EX_STRING_END; }
    [^\"#\\]+                          { return EX_STRING_PART; }
    \\.                               { return EX_STRING_PART; }
    "#"                                { return EX_STRING_PART; }
    "\\"                               { return EX_STRING_PART; }
    .                                  { return EX_STRING_PART; }
}

<IN_SIGIL_SQUOTE> {
    \\\\#\{                            { return EX_STRING_PART; }
    \\#\{                              { return EX_STRING_PART; }
    "#{"                               { return beginInterpolation(IN_SIGIL_SQUOTE); }
    "\'" {SIGIL_MODIFIERS}             { yybegin(stringReturnState); return EX_STRING_END; }
    [^\'#\\]+                          { return EX_STRING_PART; }
    \\.                               { return EX_STRING_PART; }
    "#"                                { return EX_STRING_PART; }
    "\\"                               { return EX_STRING_PART; }
    .                                  { return EX_STRING_PART; }
}

<IN_SIGIL_HEREDOC_DQUOTE> {
    "\"\"\"" {SIGIL_MODIFIERS}         { yybegin(stringReturnState); return EX_STRING_END; }
    \\\\#\{                            { return EX_STRING_PART; }
    \\#\{                              { return EX_STRING_PART; }
    "#{"                               { return beginInterpolation(IN_SIGIL_HEREDOC_DQUOTE); }
    [^\"#]+                            { return EX_STRING_PART; }
    "\""                               { return EX_STRING_PART; }
    "#"                                { return EX_STRING_PART; }
    "\\"                               { return EX_STRING_PART; }
    .                                  { return EX_STRING_PART; }
}

<IN_SIGIL_HEREDOC_SQUOTE> {
    "\'\'\'" {SIGIL_MODIFIERS}         { yybegin(stringReturnState); return EX_STRING_END; }
    \\\\#\{                            { return EX_STRING_PART; }
    \\#\{                              { return EX_STRING_PART; }
    "#{"                               { return beginInterpolation(IN_SIGIL_HEREDOC_SQUOTE); }
    [^\'#]+                            { return EX_STRING_PART; }
    "\'"                               { return EX_STRING_PART; }
    "#"                                { return EX_STRING_PART; }
    "\\"                               { return EX_STRING_PART; }
    .                                  { return EX_STRING_PART; }
}
