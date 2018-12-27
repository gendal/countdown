import java.util.*
import kotlin.coroutines.experimental.buildSequence
import kotlin.math.abs

fun main(args: Array<String>) {
    val (target, numbers)= determineNumbersAndTarget(args)

    print("The numbers are: ")
    for (number in numbers) print("${number} ")
    println()

    // Generate all orderings of the numbers
    // Note: this only generates lists of length 6
    // However, this turns out to be OK since we will halt as soon as we find a
    // winning answer, and the evaluations occur with smaller groupings too (I think...)

    // allNumberOrderings.size will be equal to the number of permutations of numbers (typically 6P6 = 720)
    val allNumberOrderings = generatePermutations(numbers)

    // So... at this point, we have a potential ordering of the six numbers
    // What we do now, for each of them, is generate a series of ASTs, representing a possible
    // insertion of +, -, * or / between each of these six numbers, where we model grouping (brackets)
    // by selecting one of the insertion points to be the root of the AST, working from left to right.
    //
    // Example:
    //
    // If the six numbers (if sorted) were [1, 2, 4, 6, 25, 100] and the current
    // permutation was, say, [2, 4, 6, 25, 100, 1]
    // then generateAllASTs will start with [2] and [4, 6, 25, 100, 1],
    // inserting, in turn, +, -, *, / as the root of the generated AST, with '2'
    // hanging off the left and an AST containing 4, 6, 25, 100 and 1 off the right.
    //
    // We then need to recurse, of course, since the AST on the right isn't evaluatable...
    // We have to generate all possible ASTs consisting of those five numbers, with the root of the
    // tree occurring in each of the four possible insertion points.
    //
    // In so doing, we generate all possible ASTs. And note, we don't need to reorder/permute any of the numbers
    // as we go..  the "written out" form of any ASTs generated will all contain the same numbers in the same order
    // but, of course, with lots of brackets to show operator affinity.  This is fine because the permutations
    // we calculate at the start of the program will generate every possible left-to-right ordering of
    // numbers... our job at this point is just to insert every possible combination of operators, with the
    // "try every insertion point as the root of the AST" trick enabling us to be sure of trying every
    // possible bracketing.
    //
    // The final trick is that whenever we have a fully-formed AST (of whatever size), we evaluate it and
    // test it against the target, halting if we find a match.  In this way, we can also find solutions that
    // use only a subset of the numbers.  To see why this must be so, consider a game that could be solved by,
    // say, just three numbers.  These are guaranteed, at some point to appear as the first three in the permuted
    // set.  Let's call them n1, n2, n3.  And we as also guaranteed that the following parse trees will be generated:
    // n1 OP1 (n2 OP2 n3), (n1 OP1 n2) OP2 n3.  AND... we are guaranteed that all permutations of n1, n2 and n3
    // will be evaluated at some point.

    var closest = 6
    allNumberOrderings.forEach {
        generateAllASTs(it).forEach { value ->
            //println("***** Sequence popped")
            val result = value.evaluate()
            if (result == target) {
                println("SUCCESS!!\n    ${value} = ${target}")
                System.exit(0)
            }
            if (abs(result-target) < closest) {
                println(".. Found closest answer so far:\n    ${value} = ${result}")
                closest = abs(result-target)
            }
        }
    }
    println("No solutions found")
}

private fun determineNumbersAndTarget(args: Array<String>): Pair<Int, List<Int>> {
    if (args.size < 2) {
        println("Usage: CountdownSolver target numbers (at least one)")
        System.exit(-1)
    }

    var target = args[0].toIntOrNull()

    if (target == null) {
        target = Random().nextInt(1000)
        println("Could not parse target (${args[0]}). Selecting random target instead")
    }
    println("Target: ${target}")

    var random = false
    val workingNumbers = ArrayList<Int>()
    for (i in 1 until args.size) {
        val temp = args[i].toIntOrNull()
        if (temp == null) {
            println("  Could not parse number (${args[i]}). Selecting six random numbers instead")
            random = true
            break
        }
        workingNumbers += temp
    }

    val numbers: List<Int>
    if (random) {
        numbers = generateNumbers()
    } else {
        numbers = workingNumbers.toList()
    }
    return Pair(target, numbers)
}

fun generateAllASTs(inputList:IntArray): Sequence<Value> = buildSequence<Value> {
    // println("generateALlASTs called with input list: ${inputList.contentToString()}")

    val returnValue = ArrayList<Value>()

    if (inputList.size == 1) {
        returnValue += Number(inputList.elementAt(0))
        yield (returnValue.elementAt(0))
    }

    // If we're here, there are at least two entries in the array. We now iterate across the list,
    // slicing it between each pair of members. The two slices so formed in each step will be the
    // left and right branches of an AST rooted at this point

    (1 until inputList.size).forEach { i ->
        // we're choosing the gaps between numbers in the list

        val leftList = inputList.sliceArray(0 until i)
        val rightList = inputList.sliceArray(i until inputList.size)

        val lefts: Sequence<Value> = generateAllASTs(leftList)
        val rights: Sequence<Value> = generateAllASTs(rightList)

        lefts.forEach { left ->
            rights.forEach { right ->
                val newTrees = ArrayList<Value>()
                var temp: Value
                temp = Plus(left, right); if (temp.evaluate() >= 0) newTrees += temp
                temp = Minus(left, right); if (temp.evaluate() >= 0) newTrees += temp
                temp = Multiply(left, right); if (temp.evaluate() >= 0) newTrees += temp
                temp = Divide(left, right); if (temp.evaluate() >= 0) newTrees += temp

                newTrees.forEach { item ->
                    //println ("Evaluating ${item}. Value: ${item.evaluate()}. Target: ${target}")
                    //val value = item.evaluate()
                    //when (value) {
                    //    target -> {
                    //        println("Evaluating ${item}. Value: ${value}. Target: ${target}")
                    //        println("  SUCCESS!!")
                    //        //System.exit(0)
                    //    }
                    //}
                    //println("Adding entry to list (input list size: ${inputList.size}")
                    yield (item)
                    //returnValue += item
                }
            }
        }
    }

    // return returnValue
}

fun generatePermutations(inputList:List<Int>): List<IntArray> {
    // Given a list of integers (typically the
    val (perms: List<IntArray>, _) = johnsonTrotter(inputList.size)

    val returnList = ArrayList<IntArray>()
    var currentList: IntArray

    perms.forEach { perm ->
        currentList = IntArray(inputList.size)
        for ((count, pos) in perm.withIndex()) currentList[count] = inputList.elementAt(pos)
        //println("Current permuation: ${currentList}")
        returnList += currentList
    }

    return returnList
}

interface Value {
    fun evaluate(): Int
}

class Number(val value: Int): Value {
    override fun evaluate(): Int {
        return value
    }
    override fun toString(): String {
        return "${value}"
    }
}

abstract class Operator(val left:Value, val right: Value): Value

class Plus(left: Value, right: Value): Operator(left, right) {
    override fun evaluate(): Int {
        return left.evaluate() + right.evaluate()
    }
    override fun toString(): String {
        return "${left} + ${right}"
    }
}

class Minus(left: Value, right: Value): Operator(left, right) {
    override fun evaluate(): Int {
        return left.evaluate() - right.evaluate()
    }
    override fun toString(): String {
        return "(${left}) - (${right})"
    }

}

class Multiply(left: Value, right: Value) : Operator(left, right) {
    override fun evaluate(): Int {
        return left.evaluate() * right.evaluate()
    }
    override fun toString(): String {
        return "(${left}) * (${right})"
    }

}

class Divide(left: Value, right: Value) : Operator(left, right) {
    override fun evaluate(): Int {
        if (right.evaluate() == 0) {
            //println("Trying to divide by zero!!")
            return -10000000
        }
        if (left.evaluate().rem(right.evaluate()) != 0 ){
            //println("Fraction!!!!")
            return -20000000
        }
        return left.evaluate() / right.evaluate()
    }
    override fun toString(): String {
        return "(${left}) / (${right})"
    }

}



fun generateNumbers(): List<Int> {
    val list = mutableListOf<Int>()
    val random = Random()
    val largeNumbers = random.nextInt(5)   // Rand returns 0 - 5
    val smallNumbers = 6 - largeNumbers
    (1..largeNumbers).forEach {
        list += 25 + random.nextInt(4) * 25  // Rand returns 0 - 3
    }
    // Rand returns 0 - 8
    (1..smallNumbers).forEach {
        list += 1 + random.nextInt(9)
    }
    return list
}



fun johnsonTrotter(n: Int): Pair<List<IntArray>, List<Int>> {
    val p = IntArray(n) { it }  // permutation
    val q = IntArray(n) { it }  // inverse permutation
    val d = IntArray(n) { -1 }  // direction = 1 or -1
    var sign = 1
    val perms = mutableListOf<IntArray>()
    val signs = mutableListOf<Int>()

    fun permute(k: Int) {
        if (k >= n) {
            perms.add(p.copyOf())
            signs.add(sign)
            sign *= -1
            return
        }
        permute(k + 1)
        for (i in 0 until k) {
            val z = p[q[k] + d[k]]
            p[q[k]] = z
            p[q[k] + d[k]] = k
            q[z] = q[k]
            q[k] += d[k]
            permute(k + 1)
        }
        d[k] *= -1
    }

    permute(0)
    return perms to signs
}

fun printPermsAndSigns(perms: List<IntArray>, signs: List<Int>) {
    for ((i, perm) in perms.withIndex()) {
        println("${perm.contentToString()} -> sign = ${signs[i]}")
    }
}

