///////////// Schemelin: Scheme interpreter in Kotlin


////////////////// PARSING

fun parse(program: String) : List<Any> {
    return readFromTokens(tokenize(program))
}

// Lexer
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
fun readFromTokens(tokens: MutableList<String>) : List<Any> {
    require(tokens.size != 0) { "Empty list of tokens!" }

    val firstToken = tokens.removeAt(0)
    val l : MutableList<Any> = mutableListOf()

    when(firstToken) {
        "'" -> {
            l.add("quote")
            l.add(readFromTokens(tokens))
        }
        "(" -> {
            while(tokens[0] != ")") {
                if(tokens[0] == "'" || tokens[0] == "(") {
                    l.add(readFromTokens(tokens))
                }
                try {
                    l.add(tokens[0] as Int) // TODO: not working
                } catch (e: Exception) {
                    try {
                        l.add(tokens[0] as Float)
                    } catch (e: Exception) {
                        l.add(tokens[0])
                    }
                }
                tokens.removeAt(0)
            }
        }
        else -> throw IllegalArgumentException("Unexpected token: $firstToken")
    }
    return l.filter {// TODO: more elegant implementation
        it != ")"
    }
}


////////////////// REPL
fun repl() {
    println("Welcome to the Scheme interpreter. Enter 'exit' to exit the REPL.")
    replloop@ while (true) {
        print("scheme.kt> ")
        when(val input = readLine() ?: continue@replloop) {
            "" -> continue@replloop
            "exit" -> break@replloop
            else -> println(parse(input))
        }
    }
}


////////////////// MAIN FUNCTION
fun main() {
    repl()
}
