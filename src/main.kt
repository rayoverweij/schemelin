///////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
///////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
/////////////  Schemelin: a Scheme interpreter in Kotlin
/////////////  Written by Rayo Verweij
/////////////  Based on a similar interpreter for Python by Peter Norvig
///////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
///////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

import java.math.BigDecimal
import kotlin.math.sin
import kotlin.math.cos
import kotlin.math.sqrt
import kotlin.math.tan


///////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
/////////// TYPING
enum class Type {
    EXPRESSION,
    SYMBOL,
    NUMBER,
    STRING,
    LIST
}

class Procedure(private val parms: List<Pair<Any, Type>>,
                private val body: Pair<Any, Type>,
                private val env: MutableMap<String, Any>) {

    // The operator part here is the () after a function call - in other words, invoke() makes the class callable
    operator fun invoke(args: List<Any>): Any {
        checkArity(parms.size, args.size)

        val zip: MutableMap<String, Any> = mutableMapOf() // Kotlin's built-in zip doesn't work bc of the custom typing
        for (i in parms.indices) {
            zip[parms[i].first.toString()] = args[i]
        }

        // Combine the current environment with the parameter:argument map
        val procEnv = (env + zip).toMutableMap()
        return eval(body, procEnv)
    }
}



///////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
/////////// PARSING

fun parse(program: String) : Pair<Any, Type> {
    // Uncomment the following line to see how the parser works:
    //println(readFromTokens(tokenize(program)))
    return readFromTokens(tokenize(program))
}

// Lexer. The returned list needs to be mutable for the parser
fun tokenize(s: String) : MutableList<String> {
    return s
        .replace("("," ( ")
        .replace(")"," ) ")
        .replace("'", " ' ")
        .split(" ")
        .filter {
            it != ""
        }
        .toMutableList()
}

// Read an expression from a sequence of tokens
fun readFromTokens(tokens: MutableList<String>) : Pair<Any, Type> {
    require(tokens.size != 0) { "Empty list of tokens!" }

    val firstToken = tokens.removeAt(0)
    when {
        // Quotation gives a list
        firstToken == "'" -> {
            return readFromTokens(tokens) to Type.LIST
        }
        firstToken == "(" -> {
            // Check for quotation
            if(tokens[0] == "quote") {
                tokens.removeAt(0)
                return readFromTokens(tokens) to Type.LIST
            }

            // If it's not quoted, it is an expression
            val l : MutableList<Pair<Any, Type>> = mutableListOf()
            while(tokens[0] != ")") {
                l.add(readFromTokens(tokens))
            }
            tokens.removeAt(0)
            return l to Type.EXPRESSION
        }
        // Very simple implementation which 1) isn't able to handle whitespace and 2) doesn't actually require a closing "
        firstToken.startsWith("\"") -> {
            return firstToken to Type.STRING
        }
        else -> {
            // If it's not a number, it's a symbol
            return try {
                BigDecimal(firstToken) to Type.NUMBER
            } catch (e: Exception) {
                firstToken to Type.SYMBOL
            }
        }
    }
}



///////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
/////////// GLOBAL ENVIRONMENT

//// THE DEFAULT ENVIRONMENT
val standard_env: MutableMap<String, Any> = mutableMapOf(
    //// MATHEMATICAL OPERATIONS
    "+" to { args: List<Any> ->
        var total = args[0] as BigDecimal
        for(i in args.drop(1)) {
            total = total.add(i as BigDecimal)
        }
        total
    },
    "-" to { args: List<Any> ->
        var total = args[0] as BigDecimal
        for(i in args.drop(1)) {
            total = total.subtract(i as BigDecimal)
        }
        total
    },
    "*" to { args: List<Any> ->
        var total = args[0] as BigDecimal
        for(i in args.drop(1)) {
            total = total.multiply(i as BigDecimal)
        }
        total
    },
    "/" to { args: List<Any> ->
        var total = args[0] as BigDecimal
        for(i in args.drop(1)) {
            total = total.divide(i as BigDecimal)
        }
        total
    },
    "abs" to { args: List<Any> ->
        checkArity(1, args.size)
        (args[0] as BigDecimal).abs()
    },
    "sqrt" to { args: List<Any> ->
        checkArity(1, args.size)
        sqrt((args[0] as BigDecimal).toDouble()) // BigDecimal doesn't include methods for sqrt/sin/cos/tan
    },
    "sin" to { args: List<Any> ->
        checkArity(1, args.size)
        sin((args[0] as BigDecimal).toDouble())
    },
    "cos" to { args: List<Any> ->
        checkArity(1, args.size)
        cos((args[0] as BigDecimal).toDouble())
    },
    "tan" to { args: List<Any> ->
        checkArity(1, args.size)
        tan((args[0] as BigDecimal).toDouble())
    },
    "round" to { args: List<Any> ->
        checkArity(1, args.size)
        (args[0] as BigDecimal).toInt()
    },

    //// MATHEMATICAL PREDICATES
    // Should be relatively straightforward
    "number?" to { args: List<Any> ->
        checkArity(1, args.size)
        try {
            args[0] as BigDecimal
            true
        } catch (e: Exception) {
            false
        }
    },
    "=" to { args: List<Any> ->
        val comp = args[0] as BigDecimal
        var result = true
        for(i in args.drop(1)) {
            result = comp.compareTo(i as BigDecimal) == 0
        }
        result
    },
    "<" to { args: List<Any> ->
        val comp = args[0] as BigDecimal
        var result = true
        for(i in args.drop(1)) {
            result = comp.compareTo(i as BigDecimal) == -1
        }
        result
    },
    ">" to { args: List<Any> ->
        val comp = args[0] as BigDecimal
        var result = true
        for(i in args.drop(1)) {
            result = comp.compareTo(i as BigDecimal) == 1
        }
        result
    },
    "<=" to { args: List<Any> ->
        val comp = args[0] as BigDecimal
        var result = true
        for(i in args.drop(1)) {
            result = comp.compareTo(i as BigDecimal) == 0 || comp.compareTo(i as BigDecimal) == -1
        }
        result
    },
    ">=" to { args: List<Any> ->
        val comp = args[0] as BigDecimal
        var result = true
        for(i in args.drop(1)) {
            result = comp.compareTo(i as BigDecimal) == 0 || comp.compareTo(i as BigDecimal) == 1
        }
        result
    },

    //// GENERAL FUNCTIONS
    "display" to { args: List<Any> ->
        checkArity(1, args.size)
        args[0]
    },
    "not" to { args: List<Any> ->
        checkArity(1, args.size)
        args[0] == false
    },

    //// LIST OPERATIONS
    "list" to { args: List<Any> ->
        val l : MutableList<Any> = mutableListOf()
        for (i in args) {
            l.add(i)
        }
        l
    },
    "list?" to { args: List<Any> ->
        checkArity(1, args.size)
        args[0] is List<*>
    },
    "length" to { args: List<Any> ->
        checkArity(1, args.size)
        (args[0] as List<*>).size
    },
    "cons" to { args: List<Any> -> // Create a fake pair
        checkArity(2, args.size)
        listOf(args[0], args[1])
    },
    "car" to { args: List<Any> ->
        checkArity(1, args.size)
        (args[0] as List<*>)[0]
    },
    "cdr" to { args: List<Any> ->
        checkArity(1, args.size)
        (args[0] as List<*>).drop(1)
    },
    "append" to { args: List<Any> ->
        checkArity(2, args.size)
        val l : MutableList<Any> = mutableListOf()
        for(i in (args[0] as List<*>)) {
            l.add((i as Pair<*, *>).first ?: "")
        }
        l.add(args[1])
        l
    },

    //// STRING OPERATIONS
    "string?" to { args: List<Any> ->
        checkArity(1, args.size)
        args[0] is String
    },
    "string-length" to { args: List<Any> ->
        checkArity(1, args.size)
        (args[0] as String).length - 2 // minus 2 because we shouldn't count the ""
    },

    //// CONSTANTS
    "pi" to BigDecimal.valueOf(Math.PI)
)

// The global environment starts off as the standard environment
val global_env = standard_env

// Helper function to check for the correct arity; throws a Scheme-like error message if not
fun checkArity(arity: Int, argSize: Int) {
    if (arity != argSize) throw IllegalArgumentException("arity-mismatch: the expected number of arguments does not match the given number.\nExpected: $arity\nGiven: $argSize")
}



///////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
/////////// EVAL

// This function is perfectly type-safe. Kotlin isn't always smart enough to realize that, however,
// so we suppress warnings of unchecked casts.
@Suppress("UNCHECKED_CAST")
fun eval(x: Pair<Any, Type>, env: MutableMap<String, Any> = global_env): Any {
   when (x.second) {
       // Strings and numbers are simply returned
       Type.STRING -> return x.first
       Type.NUMBER -> return x.first
       // Because of the nature of the parser, lists are parsed as a Type.EXPRESSION inside a Type.LIST,
       // which is why we need to access the pair inside the pair
       Type.LIST -> return (x.first as Pair<Any, Type>).first

       // Symbols are looked up in the environment
       // If no such symbol exists, an error message similar to the Scheme one is thrown
       Type.SYMBOL -> {
           return when (x.first) {
               in env -> env[x.first] ?: "impossible" // Kotlin seems to forget that we literally condition on "in env", so it requires us to check for null
               else -> throw Exception(x.first.toString() + " is undefined; cannot reference an identifier before its definition.")
           }
       }

       Type.EXPRESSION -> {
            when (x.first) {
                is List<*> -> {
                    val exp = x.first as List<Pair<Any, Type>>

                    when(exp[0].first) {
                        "if" -> { // (if test conseq alt)
                            val (_, test, conseq, alt) = exp
                            val tbe = if (eval(test, env) as Boolean) conseq else alt
                            return eval(tbe, env)
                        }

                        "cond" -> { // (cond (test1 conseq1) ... (test_n conseq_n))
                            for(ex in exp.drop(1)) {
                                val (test, conseq) = ex as List<Any>
                                if(test == "else" || eval(test as Pair<Any, Type>, env) as Boolean) {
                                    return eval(conseq as Pair<Any, Type>, env)
                                }
                            }
                            return "#<void>"
                        }

                        "define" -> { // (define v exp)
                            val (_, v, body) = exp

                            when (v.second) {
                                // (define var exp)
                                Type.SYMBOL -> env[v.first as String] = eval(body, env)

                                // (define (name parms) (exp))
                                Type.EXPRESSION -> {
                                    // First, deconstructing the list
                                    val name = (v.first as List<Pair<Any, Type>>)[0].first as String
                                    val parms = (v.first as List<Pair<Any, Type>>).drop(1)
                                    // Then, when we have the parts, we simply store it as a Procedure
                                    env[name] = Procedure(parms, body, env)
                                }

                                // The same error message that Scheme gives
                                else -> throw Exception("define: bad syntax.")
                            }

                            return ""
                        }

                        "lambda" -> { // (lambda (var ...) body)
                            val (_, parms, body) = exp
                            if(parms.second !== Type.EXPRESSION) {
                                throw Exception("lambda: bad argument sequence.") // Again, similar to Scheme
                            }
                            return Procedure(parms.first as List<Pair<Any, Type>>, body, env) // Kotlin asks for explicit type checking here
                        }

                        "let" -> { // (let ((var1 exp1) ... (var_n exp_n)) (body))
                            val (_, decl, body) = exp
                            val parms: MutableList<Pair<Any, Type>> = mutableListOf()
                            val exps: MutableList<Pair<Any, Type>> = mutableListOf()

                            for (par in decl.first as List<Pair<Any, Type>>) {
                                parms.add((par.first as List<Pair<Any, Type>>)[0])
                                exps.add((par.first as List<Pair<Any, Type>>)[1])
                            }

                            // After getting all the elements from the let statement, rewrite it to the form ((lambda (parms ...) body) exps)
                            val lambexp: MutableList<Any> = mutableListOf(Pair(listOf(Pair("lambda", Type.SYMBOL), Pair(parms, Type.EXPRESSION), body), Type.EXPRESSION))
                            for (e in exps) {
                                lambexp.add(e)
                            }

                            // And then evaluate the statement
                            return eval(Pair(lambexp, Type.EXPRESSION), env)
                        }

                        "set!" -> { // (set! v value)
                            val (_, v, value) = exp
                            val toSet = v.first
                            // Can only set a variable that has already been defined
                            if (toSet in env) {
                                env[toSet.toString()] = eval(value, env)
                                return ""
                            } else {
                                // Otherwise, we throw the same error message Scheme does
                                throw Exception("set!: assignment disallowed; cannot set variable before its definition.\nVariable: $toSet")
                            }
                        }

                        "and" -> { // (and exp ...)
                            for(a in exp.drop(1)) {
                                if(eval(a, env) == false) {
                                    return false
                                }
                            }
                            return true
                        }

                        "or" -> { // (or exp ...)
                            for(a in exp.drop(1)) {
                                if(eval(a, env) == true) {
                                    return true
                                }
                            }
                            return false
                        }

                        in env -> {
                            val args = exp.drop(1).map {
                                eval(it, env)
                            }

                            // If it's user-defined, it's stored as a Procedure; if not, it is stored as a lambda
                            return if(env[exp[0].first.toString()] is Procedure) {
                                val proc = env[exp[0].first.toString()] as Procedure
                                proc(args)
                            } else {
                                val proc: (List<Any>) -> Any = env[exp[0].first] as (List<Any>) -> Any
                                proc(args)
                            }
                        }

                        // If an expression starts with an expression (e.g. ((lambda ...) something)), evaluate that first
                        is List<*> -> {
                            val proc = eval(exp[0], env) as Procedure
                            val args = exp.drop(1).map {
                                eval(it, env)
                            }
                            return proc(args)
                        }

                        // If it's none of the above, it must be an undefined symbol
                        else -> {
                            throw Exception(exp[0].first.toString() + " is undefined; cannot reference an identifier before its definition.")
                        }
                    }
                }

                // Because of the nature of the parser it is impossible for a Type.EXPRESSION to not be a list,
                // but Kotlin mandates us to check for it anyway
                else -> throw Exception("This is quite impossible. Well done.")
            }
       }

       // There are no other things x.second could be, but Kotlin mandates an else statement here
       else -> throw Exception("This is quite impossible. Well done.")
   }
}



///////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
/////////// REPL

// Convert the result of the evaluation back to a LISP-readable String.
fun lispify(exp: Any) : String {
    // If it's a list, lispify all its elements and put parentheses around it
    return if(exp is List<*>) {
        exp.map {
            if (it != null) {
                lispify(it)
            }
        }
        exp.joinToString(prefix = "(", postfix = ")")
    } else {
        // It not, just return a String
        exp.toString()
    }
}

fun repl() {
    // The loop is given a label so continue and break can be called form inside the when-statement
    replloop@ while (true) {
        print("scheme.kt> ")
        when(val input = readLine() ?: continue@replloop) { // If input is null, continue anyway
            "" -> continue@replloop
            "exit" -> break@replloop
            else -> {
                println(lispify(eval(parse(input))))
                println()
            }
        }
    }
}



///////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
/////////// MAIN FUNCTION
fun main() {
    // Exceptions are caught and displayed, after which the REPL continues, just like in DrRacket
    try {
        repl()
    } catch (e: Exception) {
        println(e)
        println()
        main()
    }
}
