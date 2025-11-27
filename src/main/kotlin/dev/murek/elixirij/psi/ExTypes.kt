package dev.murek.elixirij.psi

import com.intellij.psi.tree.IElementType
import com.intellij.psi.tree.TokenSet
import dev.murek.elixirij.ExLanguage

/**
 * Custom element type for Elixir tokens.
 */
class ExTokenType(debugName: String) : IElementType(debugName, ExLanguage)

/**
 * Token types for the Elixir lexer.
 * Based on the Elixir tokenizer from Expert LS.
 */
object ExTypes {
    // Comments
    @JvmField val COMMENT = ExTokenType("COMMENT")
    
    // Literals
    @JvmField val INTEGER = ExTokenType("INTEGER")
    @JvmField val FLOAT = ExTokenType("FLOAT")
    @JvmField val CHAR = ExTokenType("CHAR")
    
    // Strings
    @JvmField val STRING = ExTokenType("STRING")
    @JvmField val HEREDOC = ExTokenType("HEREDOC")
    @JvmField val CHARLIST = ExTokenType("CHARLIST")
    @JvmField val CHARLIST_HEREDOC = ExTokenType("CHARLIST_HEREDOC")
    
    // Sigils
    @JvmField val SIGIL = ExTokenType("SIGIL")
    
    // Atoms
    @JvmField val ATOM = ExTokenType("ATOM")
    @JvmField val ATOM_QUOTED = ExTokenType("ATOM_QUOTED")
    
    // Identifiers
    @JvmField val IDENTIFIER = ExTokenType("IDENTIFIER")
    @JvmField val ALIAS = ExTokenType("ALIAS")
    
    // Keywords
    @JvmField val TRUE = ExTokenType("true")
    @JvmField val FALSE = ExTokenType("false")
    @JvmField val NIL = ExTokenType("nil")
    @JvmField val DO = ExTokenType("do")
    @JvmField val END = ExTokenType("end")
    @JvmField val FN = ExTokenType("fn")
    @JvmField val AFTER = ExTokenType("after")
    @JvmField val ELSE = ExTokenType("else")
    @JvmField val CATCH = ExTokenType("catch")
    @JvmField val RESCUE = ExTokenType("rescue")
    @JvmField val WHEN = ExTokenType("when")
    @JvmField val IN = ExTokenType("in")
    @JvmField val NOT = ExTokenType("not")
    @JvmField val AND = ExTokenType("and")
    @JvmField val OR = ExTokenType("or")
    
    // Operators - Unary
    @JvmField val AT = ExTokenType("@")
    @JvmField val EXCLAMATION = ExTokenType("!")
    @JvmField val CARET = ExTokenType("^")
    @JvmField val TILDE = ExTokenType("~")
    @JvmField val AMPERSAND = ExTokenType("&")
    
    // Operators - Arithmetic
    @JvmField val PLUS = ExTokenType("+")
    @JvmField val MINUS = ExTokenType("-")
    @JvmField val STAR = ExTokenType("*")
    @JvmField val SLASH = ExTokenType("/")
    
    // Operators - Comparison
    @JvmField val EQ = ExTokenType("=")
    @JvmField val LT = ExTokenType("<")
    @JvmField val GT = ExTokenType(">")
    @JvmField val EQ_EQ = ExTokenType("==")
    @JvmField val NOT_EQ = ExTokenType("!=")
    @JvmField val EQ_EQ_EQ = ExTokenType("===")
    @JvmField val NOT_EQ_EQ = ExTokenType("!==")
    @JvmField val LT_EQ = ExTokenType("<=")
    @JvmField val GT_EQ = ExTokenType(">=")
    @JvmField val EQ_TILDE = ExTokenType("=~")
    
    // Operators - Logical
    @JvmField val AMP_AMP = ExTokenType("&&")
    @JvmField val AMP_AMP_AMP = ExTokenType("&&&")
    @JvmField val PIPE_PIPE = ExTokenType("||")
    @JvmField val PIPE_PIPE_PIPE = ExTokenType("|||")
    
    // Operators - Arrows
    @JvmField val ARROW = ExTokenType("->")
    @JvmField val LEFT_ARROW = ExTokenType("<-")
    @JvmField val FAT_ARROW = ExTokenType("=>")
    @JvmField val PIPE_GT = ExTokenType("|>")
    @JvmField val LT_TILDE = ExTokenType("<~")
    @JvmField val TILDE_GT = ExTokenType("~>")
    @JvmField val LT_TILDE_GT = ExTokenType("<~>")
    @JvmField val LT_LT_TILDE = ExTokenType("<<~")
    @JvmField val TILDE_GT_GT = ExTokenType("~>>")
    @JvmField val LT_LT_LT = ExTokenType("<<<")
    @JvmField val GT_GT_GT = ExTokenType(">>>")
    @JvmField val LT_PIPE_GT = ExTokenType("<|>")
    
    // Operators - Range
    @JvmField val DOT_DOT = ExTokenType("..")
    @JvmField val DOT_DOT_DOT = ExTokenType("...")
    @JvmField val DOT_DOT_SLASH_SLASH = ExTokenType("..//")
    
    // Operators - Concatenation
    @JvmField val PLUS_PLUS = ExTokenType("++")
    @JvmField val MINUS_MINUS = ExTokenType("--")
    @JvmField val PLUS_PLUS_PLUS = ExTokenType("+++")
    @JvmField val MINUS_MINUS_MINUS = ExTokenType("---")
    @JvmField val LT_GT = ExTokenType("<>")
    
    // Operators - Power
    @JvmField val STAR_STAR = ExTokenType("**")
    
    // Operators - Type
    @JvmField val COLON_COLON = ExTokenType("::")
    
    // Operators - Match
    @JvmField val BACK_SLASH_BACK_SLASH = ExTokenType("\\\\")
    
    // Operators - Ternary
    @JvmField val SLASH_SLASH = ExTokenType("//")
    
    // Operators - Pipe
    @JvmField val PIPE = ExTokenType("|")
    
    // Delimiters
    @JvmField val LPAREN = ExTokenType("(")
    @JvmField val RPAREN = ExTokenType(")")
    @JvmField val LBRACKET = ExTokenType("[")
    @JvmField val RBRACKET = ExTokenType("]")
    @JvmField val LBRACE = ExTokenType("{")
    @JvmField val RBRACE = ExTokenType("}")
    @JvmField val LT_LT = ExTokenType("<<")
    @JvmField val GT_GT = ExTokenType(">>")
    
    // Punctuation
    @JvmField val DOT = ExTokenType(".")
    @JvmField val COMMA = ExTokenType(",")
    @JvmField val COLON = ExTokenType(":")
    @JvmField val SEMICOLON = ExTokenType(";")
    @JvmField val PERCENT = ExTokenType("%")
    @JvmField val PERCENT_LBRACE = ExTokenType("%{")
    
    // Special
    @JvmField val EOL = ExTokenType("EOL")
    
    // Token sets for syntax highlighting
    @JvmField val COMMENTS = TokenSet.create(COMMENT)
    
    @JvmField val KEYWORDS = TokenSet.create(
        TRUE, FALSE, NIL, DO, END, FN, AFTER, ELSE, CATCH, RESCUE, WHEN, IN, NOT, AND, OR
    )
    
    @JvmField val STRINGS = TokenSet.create(STRING, HEREDOC, CHARLIST, CHARLIST_HEREDOC)
    
    @JvmField val NUMBERS = TokenSet.create(INTEGER, FLOAT, CHAR)
    
    @JvmField val OPERATORS = TokenSet.create(
        AT, EXCLAMATION, CARET, TILDE, AMPERSAND,
        PLUS, MINUS, STAR, SLASH,
        EQ, LT, GT, EQ_EQ, NOT_EQ, EQ_EQ_EQ, NOT_EQ_EQ, LT_EQ, GT_EQ, EQ_TILDE,
        AMP_AMP, AMP_AMP_AMP, PIPE_PIPE, PIPE_PIPE_PIPE,
        ARROW, LEFT_ARROW, FAT_ARROW, PIPE_GT, LT_TILDE, TILDE_GT, LT_TILDE_GT,
        LT_LT_TILDE, TILDE_GT_GT, LT_LT_LT, GT_GT_GT, LT_PIPE_GT,
        DOT_DOT, DOT_DOT_DOT, DOT_DOT_SLASH_SLASH,
        PLUS_PLUS, MINUS_MINUS, PLUS_PLUS_PLUS, MINUS_MINUS_MINUS, LT_GT,
        STAR_STAR, COLON_COLON, BACK_SLASH_BACK_SLASH, SLASH_SLASH, PIPE
    )
    
    @JvmField val BRACES = TokenSet.create(
        LPAREN, RPAREN, LBRACKET, RBRACKET, LBRACE, RBRACE, LT_LT, GT_GT
    )
}
