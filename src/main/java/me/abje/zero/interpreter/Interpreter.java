package me.abje.zero.interpreter;

import me.abje.zero.Phase;
import me.abje.zero.interpreter.obj.Obj;
import me.abje.zero.lexer.Lexer;
import me.abje.zero.lexer.Morpher;
import me.abje.zero.parser.ParseException;
import me.abje.zero.parser.Parser;
import me.abje.zero.parser.expr.Expr;

import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.InputStreamReader;
import java.io.Reader;

public class Interpreter implements Phase<Expr, Obj> {
    private Environment env = new Environment();

    public Obj next(Expr expr) {
        return expr.evaluate(this);
    }

    public static void main(String[] args) throws FileNotFoundException {
        try {
            Interpreter interpreter = new Interpreter();
            new Intrinsics(interpreter.env.getGlobals()).register();
            interpreter.interpret(new InputStreamReader(Interpreter.class.getResourceAsStream("/core.zero")));
            interpreter.interpret(new FileReader("test.txt"));
        } catch (ParseException | InterpreterException e) {
            e.printStackTrace();
        }
    }

    public void interpret(Reader reader) {
        Parser parser = new Parser(new Morpher(new Lexer(reader)));
        Expr expr;
        while ((expr = parser.next()) != null) {
            next(expr);
        }
    }

    public Environment getEnv() {
        return env;
    }
}
