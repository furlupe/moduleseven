package com.example.codeblocks.model

interface Command {

    var name: String

    fun execute(
        _variables: MutableMap<String, Double>,
        _arrays: MutableMap<String, MutableList<Double>>
    ) {
    }

}

class Variable(_name: String, _value: String = "0") : Command {
    override var name = _name
    private var value = _value

    fun assignName(_name: String) {
        name = _name
    }

    fun assignValue(_value: String) {
        value = _value
    }

    override fun execute(
        _variables: MutableMap<String, Double>,
        _arrays: MutableMap<String, MutableList<Double>>
    ){

        if (_variables.containsKey(name)) throw Exception("Variable already exists!")

        _variables[name] = Arifmetics.evaluateExpression(value, _variables, _arrays)
    }
}

class MyArray(_name: String, _size: String, _inside: String = "") : Command {
    override var name = _name
    private val nonProcessedSize = _size
    private val inside = _inside.split("\\s*,\\s*".toRegex())

    private var size = 0

    override fun execute(
        _variables: MutableMap<String, Double>,
        _arrays: MutableMap<String, MutableList<Double>>
    ){
        if (_arrays.containsKey(name)) throw Exception("Variable doesn't exist!")

        size = Arifmetics.evaluateExpression(nonProcessedSize, _variables, _arrays).toInt()

        _arrays[name] = mutableListOf()
        for (v in inside) {
            _arrays[name]!!.add(Arifmetics.evaluateExpression(v, _variables, _arrays))
        }
    }

    fun addValue(
        _value: String,
        _variables: MutableMap<String, Double>,
        _arrays: MutableMap<String, MutableList<Double>>
    ) {
        if (_arrays[name]!!.size >= size) {
            throw Exception("Out of size")
        }
        _arrays[name]!!.add(Arifmetics.evaluateExpression(_value, _variables, _arrays))
    }

}

class Assign(_name: String, _value: String) : Command {
    override var name: String = _name
    private var value: String = _value

    fun assignName(_name: String) {
        name = _name
    }

    fun assignValue(_value: String) {
        value = _value
    }

    private val variableRegex = "^[a-zA-Z][a-zA-Z0-9]*".toRegex()
    private val arrayRegex = "^($variableRegex)\\[(\\w+)]".toRegex()

    override fun execute(
        _variables: MutableMap<String, Double>,
        _arrays: MutableMap<String, MutableList<Double>>
    ) {

        if (name.matches(variableRegex)) {
            if (!_variables.containsKey(name)) throw Exception("Variable doesn't exist!")

            _variables[name] = Arifmetics.evaluateExpression(value, _variables, _arrays)
        }

        if (name.matches(arrayRegex)) {
            if (!_arrays.containsKey(name)) throw Exception("Variable doesn't exist!")

            val (name, index) = arrayRegex.find(name)!!.destructured
            if (!_arrays.containsKey(name)) {
                throw Exception("$name does not exist")
            }

            _arrays[name]!![Arifmetics.evaluateExpression(index, _variables, _arrays).toInt()] =
                Arifmetics.evaluateExpression(value, _variables, _arrays)
        }
    }
}

open class If(
    _condition: String,
    _commands: MutableList<Command> = mutableListOf(),
    _else: MutableList<Command> = mutableListOf()
) :
    Command {

    override var name = ""

    var elseExists = false

    val insideMainBlock = _commands
    val insideElseBlock = _else
    private var condition = _condition

    fun addCommandInsideMainBlock(_command: Command) {
        insideMainBlock.add(_command)
    }

    fun addCommandInsideElseBlock(_command: Command) {
        insideElseBlock.add(_command)
    }

    fun changeCondition(_condition: String) {
        condition = _condition
    }

    override fun execute(
        _variables: MutableMap<String, Double>,
        _arrays: MutableMap<String, MutableList<Double>>
    ){

        val toExecute = if (LogicalArifmetic.evalWhole(
                condition,
                _variables,
                _arrays
            )
        ) insideMainBlock else insideElseBlock

        for (command in toExecute)
            command.execute(_variables, _arrays)
    }
}

// для выполнения кода Print, в него необходимо передать лямбда-функцию из mainActivity,
// которая содержит в себе работу с textView из activity_main.xml
class Print(
    _showText: (toPrint: String, end: String) -> Unit,
    _toPrint: String,
    _end: String = " "
) : Command {

    override var name = ""

    private val toPrint = _toPrint.split("\\s*,\\s*".toRegex())
    private val end = _end
    private val showText: (toPrint: String, end: String) -> Unit = _showText

    override fun execute(
        _variables: MutableMap<String, Double>,
        _arrays: MutableMap<String, MutableList<Double>>
    ) {

        // проверяем, является ли переданное значени строкой ("что-то" или 'что-то')
        for (out in toPrint) {
            if (out.matches("^(?:\"(?=.*\")|\'(?=.*\')).*".toRegex())) {
                // ...то вывести ее без кавычек
                showText(out.substring(1, out.length - 1), end)
                // иначе нам передали либо переменную, либо ариф. выражение, либо неправильную строку
            } else {
                // если есть, то выводим ее значение
                showText(Arifmetics.evaluateExpression(out, _variables, _arrays).toString(), end)
            }
        }
        showText("", "\n")
    }
}

// по аналогии с Print, в Input нужно передать лямбда-функцию из mainActivity,
// которая вызывает окно с текстовым полем для ввода данных и возвращает введенные данные
class Input(_name: String, _inputText: () -> String) : Command {

    override var name = _name
    private val inputText: () -> String = _inputText

    private val variableRegex = "^[a-zA-Z][a-zA-Z0-9]*".toRegex()
    private val arrayRegex = "^($variableRegex)\\[(.+)]".toRegex()

    override fun execute(
        _variables: MutableMap<String, Double>,
        _arrays: MutableMap<String, MutableList<Double>>
    ){
        val value = inputText()
        if (name.matches(variableRegex)) {
            if (!_variables.containsKey(name)) throw Exception("Variable doesn't exist!")

            _variables[name] = Arifmetics.evaluateExpression(value, _variables, _arrays)
        }

        if (name.matches(arrayRegex)) {
            val (name, index) = arrayRegex.find(name)!!.destructured

            _arrays[name]!![Arifmetics.evaluateExpression(index, _variables, _arrays).toInt()] =
                Arifmetics.evaluateExpression(value, _variables, _arrays)
        }

    }
}

class While(_condition: String, _commands: MutableList<Command> = mutableListOf()) :
    Command {

    override var name: String = ""

    private var condition = _condition
    val inside = _commands

    fun addCommandInside(_command: Command) {
        inside.add(_command)
    }

    fun changeCondition(_condition: String) {
        condition = _condition
    }

    override fun execute(
        _variables: MutableMap<String, Double>,
        _arrays: MutableMap<String, MutableList<Double>>
    ){

        while (LogicalArifmetic.evalWhole(condition, _variables, _arrays)) {
            for (command in inside) {
                command.execute(_variables, _arrays)
            }
        }
    }
}

class For(
    _before: MutableList<Command> = mutableListOf(),
    _condition: String = "",
    _eachStep: MutableList<Command> = mutableListOf(),
    _inside: MutableList<Command> = mutableListOf()
) : Command {
    override var name = ""

    private var before = _before
    fun addCommandToDoBefore(command: Command) = before.add(command)

    private var condition = _condition
    fun changeCondition(cond: String) {
        condition = cond
    }

    private var eachStep = _eachStep
    fun addCommandToDoEachStep(command: Command) = eachStep.add(command)

    private var inside = _inside
    fun addCommandToDoInside(command: Command) = inside.add(command)

    override fun execute(
        _variables: MutableMap<String, Double>,
        _arrays: MutableMap<String, MutableList<Double>>
    ){
        val toDelete = mutableListOf<Command>()
        for (b in before) {
            b.execute(_variables, _arrays)
            if (b is Variable)
                toDelete.add(b)
        }

        while (LogicalArifmetic.evalWhole(condition, _variables, _arrays)) {
            for (i in inside + eachStep)
                i.execute(_variables, _arrays)
        }

        for (d in toDelete)
            _variables.remove(d.name)

    }

}