package com.bosch.diag.expression;

public class ExpressionParser {
    private Lexem curLexem;
    private String expression;
    private int nextTokenPointer;
    private Lexem prevLexem;

    private class Lexem {
        public TLexem lexem;
        public double val;

        Lexem() {
            this.lexem = TLexem._None;
            this.val = 0.0d;
        }

        Lexem(TLexem lex, int v) {
            this.lexem = lex;
            this.val = (double) v;
        }
    }

    private enum TLexem {
        _None,
        _Num,
        _UMinus,
        _UPlus,
        _Plus,
        _Minus,
        _Mul,
        _Div,
        _Open,
        _Close,
        _End
    }

    public ExpressionParser() {
        this.curLexem = null;
        this.prevLexem = null;
        this.nextTokenPointer = 0;
        this.nextTokenPointer = 0;
        this.curLexem = null;
        this.prevLexem = null;
    }

    private double parse(String s) throws ParserException {
        this.curLexem = null;
        this.prevLexem = null;
        this.nextTokenPointer = 0;
        this.expression = s;
        if (this.expression.length() == 0) {
            this.expression += '=';
        } else if (this.expression.charAt(this.expression.length() - 1) != '=') {
            this.expression += '=';
        }
        this.curLexem = nextLexem();
        double rv = expr();
        if (this.curLexem.lexem == TLexem._End) {
            return rv;
        }
        throw new ParserException("End lexem expected");
    }

    public double calculate(String s) throws ParserException {
        return parse(s);
    }

    private void skipSpaces() {
        while (this.nextTokenPointer < this.expression.length()) {
            if (this.expression.charAt(this.nextTokenPointer) == ' ' || this.expression.charAt(this.nextTokenPointer) == '\t') {
                this.nextTokenPointer++;
            } else {
                return;
            }
        }
    }

    private Lexem nextLexem() throws ParserException {
        skipSpaces();
        this.prevLexem = this.curLexem;
        Lexem rv = new Lexem();
        char letter = this.expression.charAt(this.nextTokenPointer);
        if (letter == '(') {
            rv.lexem = TLexem._Open;
        } else if (letter == ')') {
            rv.lexem = TLexem._Close;
        } else if (letter == '+') {
            if (this.prevLexem == null || !(this.prevLexem.lexem == TLexem._Close || this.prevLexem.lexem == TLexem._Num)) {
                rv.lexem = TLexem._UPlus;
            } else {
                rv.lexem = TLexem._Plus;
            }
        } else if (letter == '-') {
            if (this.prevLexem == null || !(this.prevLexem.lexem == TLexem._Close || this.prevLexem.lexem == TLexem._Num)) {
                rv.lexem = TLexem._UMinus;
            } else {
                rv.lexem = TLexem._Minus;
            }
        } else if (letter == '*') {
            rv.lexem = TLexem._Mul;
        } else if (letter == '/') {
            rv.lexem = TLexem._Div;
        } else if (letter == '=') {
            rv.lexem = TLexem._End;
        } else if ((letter >= '0' && letter <= '9') || letter == '.') {
            String number = "" + letter;
            int i = this.nextTokenPointer + 1;
            while (true) {
                if ((this.expression.charAt(i) < '0' || this.expression.charAt(i) > '9') && this.expression.charAt(i) != '.') {
                    break;
                }
                number = new StringBuilder(String.valueOf(number)).append(this.expression.charAt(i)).toString();
                i++;
            }
            rv.lexem = TLexem._Num;
            try {
                rv.val = Double.parseDouble(number);
                this.nextTokenPointer = i - 1;
            } catch (IllegalArgumentException e) {
                throw new ParserException("Double parsing error");
            }
        }
        this.nextTokenPointer++;
        return rv;
    }

    private double expr() throws ParserException {
        double rv = item();
        while (true) {
            if (this.curLexem.lexem != TLexem._Plus && this.curLexem.lexem != TLexem._Minus) {
                return rv;
            }
            Lexem action = this.curLexem;
            this.curLexem = nextLexem();
            if (action.lexem == TLexem._Plus) {
                rv += item();
            } else if (action.lexem == TLexem._Minus) {
                rv -= item();
            }
        }
    }

    private double item() throws ParserException {
        double rv = mult();
        while (true) {
            if (this.curLexem.lexem != TLexem._Mul && this.curLexem.lexem != TLexem._Div) {
                return rv;
            }
            Lexem action = this.curLexem;
            this.curLexem = nextLexem();
            if (action.lexem == TLexem._Mul) {
                rv *= mult();
            } else if (action.lexem == TLexem._Div) {
                rv /= mult();
            }
        }
    }

    private double mult() throws ParserException {
        if (this.curLexem.lexem == TLexem._UMinus) {
            this.curLexem = nextLexem();
            return -1.0d * mult();
        } else if (this.curLexem.lexem == TLexem._UPlus) {
            this.curLexem = nextLexem();
            return mult();
        } else {
            double rv;
            if (this.curLexem.lexem == TLexem._Num) {
                rv = this.curLexem.val;
                this.curLexem = nextLexem();
            } else if (this.curLexem.lexem == TLexem._Open) {
                this.curLexem = nextLexem();
                rv = expr();
                if (this.curLexem.lexem == TLexem._Close) {
                    this.curLexem = nextLexem();
                } else {
                    throw new ParserException("Close bracket expected");
                }
            } else {
                throw new ParserException("Unexpected lexem: " + this.curLexem.lexem.name());
            }
            return rv;
        }
    }
}
