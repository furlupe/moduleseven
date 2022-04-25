package com.example.codeblocks.model

import androidx.core.text.isDigitsOnly
import java.lang.Exception
import com.example.codeblocks.model.ArifmeticOperators.*

object Arifmetics {
    fun createRPN(expression: String): MutableList<String> {
        if (expression.isEmpty()) {
            throw Exception("Присвойте переменной значение")
        }

        val output: MutableList<String> = mutableListOf() // выходная строка
        val stack = ArrayDeque<String>() // стек для операторов

        var i = 0
        while (i < expression.length) {
            var c: String = expression[i].toString()

            // если прочитанный символ число или буква
            if (c[0].isLetterOrDigit()) {
                // если число многоразрядное, или переменная имеет название длины > 1
                while (i + 1 < expression.length && expression[i + 1].isLetterOrDigit()) {
                    i++
                    c += expression[i]
                }
                // добавить в выходную строку операнд
                output.add(c)

                // проверка на операторы
            } else {
                // получить ArifmeticOperator по значению строки
                when (val op = getArifmeticOperator(c)) {
                    OPEN_BRACKET -> stack.addLast("(")
                    CLOSED_BRACKET -> {
                        while (stack.last() != "(") {
                            // если стек опустел раньше нахождения "(", значит ариф. запись была не верной
                            if (stack.count() == 0) {
                                throw Exception("Wrong expression")
                            }
                            // Добавляем все с вершины стека, пока не найдем "("
                            output.add(stack.removeLast())
                        }
                        stack.removeLast()
                    }
                    PLUS, MINUS, FRACTION, MULTIPLY -> {

                        if (stack.count() > 0) {
                            var stackOP = getArifmeticOperator(stack.last())
                            while (stackOP.priority >= op.priority && stack.count() > 0) {
                                output.add(stack.removeLast())
                                if (stack.count() > 0) stackOP = getArifmeticOperator(stack.last())
                            }
                        }
                        stack.addLast(c)
                    }
                    NOT_AN_OPERATION -> throw Exception("$c is not an operator")
                }
            }
            i++
        }
        // заносим оставшиеся операции из стека в выходную строку
        while (stack.count() > 0) {
            val l = stack.removeLast()
            if (getArifmeticOperator(l) == OPEN_BRACKET) throw Exception("Expression has inconsistent brackets")

            output.add(l)
        }

        return output
    }

    // обработать арифметическое выражение
    fun evaluateExpression(expression: String, variables: MutableMap<String, Int>): Int {
        val rpn = createRPN(expression)
        //println(rpn)
        val stack = ArrayDeque<Int>()

        for (operator in rpn) {
            if (operator.isDigitsOnly()) {
                stack.addLast(Integer.parseInt(operator))

                continue
            }
            if (operator.matches("^[a-zA-Z][a-zA-Z0-9]*".toRegex())) {
                if (!variables.containsKey(operator)) {
                    throw Exception("$operator does not exist")
                }
                val op = variables[operator] ?: throw Exception("$operator is null")
                stack.addLast(op)

                continue
            }

            val op = getArifmeticOperator(operator)

            val a = stack.removeLast()
            val b = stack.removeLast()

            //println("$b $op $a")

            when (op) {
                PLUS -> stack.addLast(b + a)
                MINUS -> stack.addLast(b - a)
                FRACTION -> stack.addLast(b / a)
                MULTIPLY -> stack.addLast(b * a)
                NOT_AN_OPERATION, OPEN_BRACKET, CLOSED_BRACKET -> throw Exception("$operator is not an operator")
            }
        }

        return stack.last()
    }
}
