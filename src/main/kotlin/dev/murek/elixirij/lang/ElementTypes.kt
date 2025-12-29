@file:JvmName("ElementTypes")

package dev.murek.elixirij.lang

import com.intellij.psi.TokenType
import com.intellij.psi.tree.IElementType
import com.intellij.psi.tree.TokenSet
import dev.murek.elixirij.ExLanguage

/**
 * Custom element type for Elixir tokens.
 */
class ExTokenType(debugName: String) : IElementType(debugName, ExLanguage)

// Re-export standard token types for use in lexer
@JvmField
val WHITE_SPACE: IElementType = TokenType.WHITE_SPACE

@JvmField
val BAD_CHARACTER: IElementType = TokenType.BAD_CHARACTER

// Comments
@JvmField
val EX_COMMENT = ExTokenType("COMMENT")

// Literals
@JvmField
val EX_INTEGER = ExTokenType("INTEGER")

@JvmField
val EX_FLOAT = ExTokenType("FLOAT")

@JvmField
val EX_CHAR = ExTokenType("CHAR")

// Strings
@JvmField
val EX_STRING = ExTokenType("STRING")

@JvmField
val EX_HEREDOC = ExTokenType("HEREDOC")

@JvmField
val EX_CHARLIST = ExTokenType("CHARLIST")

@JvmField
val EX_CHARLIST_HEREDOC = ExTokenType("CHARLIST_HEREDOC")

// Sigils
@JvmField
val EX_SIGIL = ExTokenType("SIGIL")

// Atoms
@JvmField
val EX_ATOM = ExTokenType("ATOM")

@JvmField
val EX_ATOM_QUOTED = ExTokenType("ATOM_QUOTED")

// Identifiers
@JvmField
val EX_IDENTIFIER = ExTokenType("IDENTIFIER")

@JvmField
val EX_KW_IDENTIFIER = ExTokenType("KW_IDENTIFIER")

@JvmField
val EX_ALIAS = ExTokenType("ALIAS")

// Keywords
@JvmField
val EX_TRUE = ExTokenType("true")

@JvmField
val EX_FALSE = ExTokenType("false")

@JvmField
val EX_NIL = ExTokenType("nil")

@JvmField
val EX_DO = ExTokenType("do")

@JvmField
val EX_END = ExTokenType("end")

@JvmField
val EX_FN = ExTokenType("fn")

@JvmField
val EX_CASE = ExTokenType("case")

@JvmField
val EX_COND = ExTokenType("cond")

@JvmField
val EX_WITH = ExTokenType("with")

@JvmField
val EX_TRY = ExTokenType("try")

@JvmField
val EX_RECEIVE = ExTokenType("receive")

@JvmField
val EX_DEFMODULE = ExTokenType("defmodule")

@JvmField
val EX_DEF = ExTokenType("def")

@JvmField
val EX_DEFP = ExTokenType("defp")

@JvmField
val EX_DEFMACRO = ExTokenType("defmacro")

@JvmField
val EX_DEFMACROP = ExTokenType("defmacrop")

@JvmField
val EX_AFTER = ExTokenType("after")

@JvmField
val EX_ELSE = ExTokenType("else")

@JvmField
val EX_CATCH = ExTokenType("catch")

@JvmField
val EX_RESCUE = ExTokenType("rescue")

@JvmField
val EX_WHEN = ExTokenType("when")

@JvmField
val EX_IN = ExTokenType("in")

@JvmField
val EX_NOT = ExTokenType("not")

@JvmField
val EX_AND = ExTokenType("and")

@JvmField
val EX_OR = ExTokenType("or")

// Operators - Unary
@JvmField
val EX_AT = ExTokenType("@")

@JvmField
val EX_EXCLAMATION = ExTokenType("!")

@JvmField
val EX_CARET = ExTokenType("^")

@JvmField
val EX_TILDE = ExTokenType("~")

@JvmField
val EX_AMPERSAND = ExTokenType("&")

// Operators - Arithmetic
@JvmField
val EX_PLUS = ExTokenType("+")

@JvmField
val EX_MINUS = ExTokenType("-")

@JvmField
val EX_STAR = ExTokenType("*")

@JvmField
val EX_SLASH = ExTokenType("/")

// Operators - Comparison
@JvmField
val EX_EQ = ExTokenType("=")

@JvmField
val EX_LT = ExTokenType("<")

@JvmField
val EX_GT = ExTokenType(">")

@JvmField
val EX_EQ_EQ = ExTokenType("==")

@JvmField
val EX_NOT_EQ = ExTokenType("!=")

@JvmField
val EX_EQ_EQ_EQ = ExTokenType("===")

@JvmField
val EX_NOT_EQ_EQ = ExTokenType("!==")

@JvmField
val EX_LT_EQ = ExTokenType("<=")

@JvmField
val EX_GT_EQ = ExTokenType(">=")

@JvmField
val EX_EQ_TILDE = ExTokenType("=~")

// Operators - Logical
@JvmField
val EX_AMP_AMP = ExTokenType("&&")

@JvmField
val EX_AMP_AMP_AMP = ExTokenType("&&&")

@JvmField
val EX_PIPE_PIPE = ExTokenType("||")

@JvmField
val EX_PIPE_PIPE_PIPE = ExTokenType("|||")

// Operators - Arrows
@JvmField
val EX_ARROW = ExTokenType("->")

@JvmField
val EX_LEFT_ARROW = ExTokenType("<-")

@JvmField
val EX_FAT_ARROW = ExTokenType("=>")

@JvmField
val EX_PIPE_GT = ExTokenType("|>")

@JvmField
val EX_LT_TILDE = ExTokenType("<~")

@JvmField
val EX_TILDE_GT = ExTokenType("~>")

@JvmField
val EX_LT_TILDE_GT = ExTokenType("<~>")

@JvmField
val EX_LT_LT_TILDE = ExTokenType("<<~")

@JvmField
val EX_TILDE_GT_GT = ExTokenType("~>>")

@JvmField
val EX_LT_LT_LT = ExTokenType("<<<")

@JvmField
val EX_GT_GT_GT = ExTokenType(">>>")

@JvmField
val EX_LT_PIPE_GT = ExTokenType("<|>")

// Operators - Range
@JvmField
val EX_DOT_DOT = ExTokenType("..")

@JvmField
val EX_DOT_DOT_DOT = ExTokenType("...")

@JvmField
val EX_DOT_DOT_SLASH_SLASH = ExTokenType("..//")

// Operators - Concatenation
@JvmField
val EX_PLUS_PLUS = ExTokenType("++")

@JvmField
val EX_MINUS_MINUS = ExTokenType("--")

@JvmField
val EX_PLUS_PLUS_PLUS = ExTokenType("+++")

@JvmField
val EX_MINUS_MINUS_MINUS = ExTokenType("---")

@JvmField
val EX_LT_GT = ExTokenType("<>")

// Operators - Power
@JvmField
val EX_STAR_STAR = ExTokenType("**")

// Operators - Type
@JvmField
val EX_COLON_COLON = ExTokenType("::")

// Operators - Match
@JvmField
val EX_BACK_SLASH_BACK_SLASH = ExTokenType("\\\\")

// Operators - Ternary
@JvmField
val EX_SLASH_SLASH = ExTokenType("//")

// Operators - Pipe
@JvmField
val EX_PIPE = ExTokenType("|")

// Delimiters
@JvmField
val EX_LPAREN = ExTokenType("(")

@JvmField
val EX_RPAREN = ExTokenType(")")

@JvmField
val EX_LBRACKET = ExTokenType("[")

@JvmField
val EX_RBRACKET = ExTokenType("]")

@JvmField
val EX_LBRACE = ExTokenType("{")

@JvmField
val EX_RBRACE = ExTokenType("}")

@JvmField
val EX_LT_LT = ExTokenType("<<")

@JvmField
val EX_GT_GT = ExTokenType(">>")

// Punctuation
@JvmField
val EX_DOT = ExTokenType(".")

@JvmField
val EX_COMMA = ExTokenType(",")

@JvmField
val EX_COLON = ExTokenType(":")

@JvmField
val EX_SEMICOLON = ExTokenType(";")

@JvmField
val EX_PERCENT = ExTokenType("%")

@JvmField
val EX_PERCENT_LBRACE = ExTokenType("%{")

// Special
@JvmField
val EX_EOL = ExTokenType("EOL")

// Token sets for syntax highlighting
val EX_COMMENTS = TokenSet.create(EX_COMMENT)

val EX_KEYWORDS = TokenSet.create(
    EX_TRUE,
    EX_FALSE,
    EX_NIL,
    EX_DO,
    EX_END,
    EX_FN,
    EX_CASE,
    EX_COND,
    EX_WITH,
    EX_TRY,
    EX_RECEIVE,
    EX_DEFMODULE,
    EX_DEF,
    EX_DEFP,
    EX_DEFMACRO,
    EX_DEFMACROP,
    EX_AFTER,
    EX_ELSE,
    EX_CATCH,
    EX_RESCUE,
    EX_WHEN,
    EX_IN,
    EX_NOT,
    EX_AND,
    EX_OR
)

val EX_STRINGS = TokenSet.create(EX_STRING, EX_HEREDOC, EX_CHARLIST, EX_CHARLIST_HEREDOC)

val EX_NUMBERS = TokenSet.create(EX_INTEGER, EX_FLOAT, EX_CHAR)

val EX_OPERATORS = TokenSet.create(
    EX_AT, EX_EXCLAMATION, EX_CARET, EX_TILDE, EX_AMPERSAND,
    EX_PLUS, EX_MINUS, EX_STAR, EX_SLASH,
    EX_EQ, EX_LT, EX_GT, EX_EQ_EQ, EX_NOT_EQ, EX_EQ_EQ_EQ, EX_NOT_EQ_EQ, EX_LT_EQ, EX_GT_EQ, EX_EQ_TILDE,
    EX_AMP_AMP, EX_AMP_AMP_AMP, EX_PIPE_PIPE, EX_PIPE_PIPE_PIPE,
    EX_ARROW, EX_LEFT_ARROW, EX_FAT_ARROW, EX_PIPE_GT, EX_LT_TILDE, EX_TILDE_GT, EX_LT_TILDE_GT,
    EX_LT_LT_TILDE, EX_TILDE_GT_GT, EX_LT_LT_LT, EX_GT_GT_GT, EX_LT_PIPE_GT,
    EX_DOT_DOT, EX_DOT_DOT_DOT, EX_DOT_DOT_SLASH_SLASH,
    EX_PLUS_PLUS, EX_MINUS_MINUS, EX_PLUS_PLUS_PLUS, EX_MINUS_MINUS_MINUS, EX_LT_GT,
    EX_STAR_STAR, EX_COLON_COLON, EX_BACK_SLASH_BACK_SLASH, EX_SLASH_SLASH, EX_PIPE
)

val EX_BINARY_DELIMITERS = TokenSet.create(EX_LT_LT, EX_GT_GT)
val EX_BRACES = TokenSet.create(EX_LBRACE, EX_RBRACE, EX_PERCENT_LBRACE)
val EX_BRACKETS = TokenSet.create(EX_LBRACKET, EX_RBRACKET)
val EX_PARENS = TokenSet.create(EX_LPAREN, EX_RPAREN)
