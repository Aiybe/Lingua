/*
 * Copyright (c) 2015 Abe Jellinek
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */

package me.abje.lingua.interpreter.obj;

import me.abje.lingua.interpreter.Environment;
import me.abje.lingua.interpreter.Interpreter;
import me.abje.lingua.interpreter.InterpreterException;
import me.abje.lingua.parser.expr.Expr;

import java.util.Deque;
import java.util.List;

/**
 * A Lingua function. Functions can be called, have a body and take arguments.
 */
public class FunctionObj extends Obj {
    public static final ClassObj SYNTHETIC = ClassObj.builder("Function").build();

    /**
     * This function's name.
     */
    private String name;

    /**
     * This function's formal argument list.
     */
    private List<Expr> argNames;

    /**
     * This function's body expression.
     */
    private Expr body;

    /**
     * The "self" implicit argument passed to this function.
     */
    private Obj self;

    private Deque<Environment.Frame> captured;

    /**
     * Creates a new function.
     *
     * @param name     The function's name.
     * @param argNames The function's formal argument list.
     * @param body     The function's body expression.
     * @param captured The function's captured frames.
     */
    public FunctionObj(String name, List<Expr> argNames, Expr body, Deque<Environment.Frame> captured) {
        this(name, argNames, body, null, null, captured);
    }

    /**
     * Creates a new function.
     *
     * @param name      The function's name.
     * @param argNames  The function's formal argument list.
     * @param body      The function's body expression.
     * @param self      The function's "self" implicit argument.
     * @param superInst The function's "super" implicit argument.
     * @param captured  The function's captured frames.
     */
    public FunctionObj(String name, List<Expr> argNames, Expr body, Obj self, Obj superInst, Deque<Environment.Frame> captured) {
        super(SYNTHETIC);
        this.name = name;
        this.argNames = argNames;
        this.body = body;
        this.self = self;
        this.captured = captured;

        setSuperInst(superInst);
    }

    /**
     * Returns this function's name.
     */
    public String getName() {
        return name;
    }

    /**
     * Returns this function's argument names.
     */
    public List<Expr> getArgNames() {
        return argNames;
    }

    /**
     * Returns this function's body.
     */
    public Expr getBody() {
        return body;
    }

    @Override
    public Obj call(Interpreter interpreter, List<Obj> args) {
        if (args.size() != argNames.size())
            throw new InterpreterException("CallException", "invalid number of arguments for function " + name, interpreter);

        Environment env = interpreter.getEnv();
        if (self != null)
            env.pushFrame(self.getType().getName() + "." + name);
        else
            env.pushFrame(name);

        Environment.Frame frame = env.getStack().peek();
        for (int i = 0; i < args.size(); i++)
            if (argNames.get(i).match(interpreter, frame, args.get(i)) == null)
                throw new InterpreterException("CallException", "invalid argument for function " + name, interpreter);

        Deque<Environment.Frame> oldStack = env.getStack();
        env.setStack(captured);
        captured.push(frame);

        if (self != null)
            env.define("self", self);
        if (getSuperInst() != null)
            env.define("super", getSuperInst());
        Obj obj = interpreter.next(body);
        env.popFrame();
        env.popFrame();
        env.setStack(oldStack);

        return obj;
    }

    @Override
    public String toString() {
        return "<function " + name + ">";
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        FunctionObj that = (FunctionObj) o;

        return argNames.equals(that.argNames) && body.equals(that.body) && name.equals(that.name);
    }

    @Override
    public int hashCode() {
        int result = name.hashCode();
        result = 31 * result + argNames.hashCode();
        result = 31 * result + body.hashCode();
        return result;
    }

    /**
     * Returns a copy of this function with an updated {@link #self}.
     *
     * @param self The new value of <code>self</code>.
     */
    public FunctionObj withSelf(Obj self) {
        return new FunctionObj(name, argNames, body, self, getSuperInst(), captured);
    }

    /**
     * Returns a copy of this function with an updated {@link #superInst}.
     *
     * @param superInst The new value of <code>super</code>.
     */
    public FunctionObj withSuper(Obj superInst) {
        return new FunctionObj(name, argNames, body, self, superInst, captured);
    }

    public boolean isApplicable(Interpreter interpreter, List<Obj> args) {
        if (args.size() != argNames.size())
            return false;

        Environment.Frame frame = interpreter.getEnv().getStack().peek();
        for (int i = 0; i < args.size(); i++)
            if (argNames.get(i).match(interpreter, frame, args.get(i)) == null)
                return false;

        return true;
    }
}
