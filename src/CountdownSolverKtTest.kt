import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

class CountdownSolverKtTest {

    @Test
    fun generateAllASTs() {
        // TODO!!!
    }

    lateinit var inputList: List<Int>
    lateinit var results: List<IntArray>

    @Before
    fun setUp() {
        inputList = listOf(10,20,30,40,50,60)
        results = generatePermutations(inputList)
    }

    @Test
    fun `correct number of permutations generated`() {
        assert(results.size== factorial(inputList.size))

    }

    fun factorial(n: Int): Int {
        if (n == 1) return 1 else return n*factorial(n - 1)
    }

    @Test
    fun `all entries in permutation set are different`() {
        assert(results.toSet().size==results.size)
    }

    @Test
    fun `all entries in permutation set have the right length`() {
        for(result in results) {
            assert (result.size == inputList.size)
        }
    }

    @Test
    fun `valid solutions are accepted`() {
        val trivialAST: Value = Number(1)
        assert(validateCountdownSolution(trivialAST, 1))
    }

    @Test
    fun `invalid solutions are rejected`() {
        val trivialAST: Value = Number(1)
        assert(!validateCountdownSolution(trivialAST, 2))
    }

    @Test
    fun `new permutation creator`() {
        val testList = listOf(1, 2, 3, 4)
        val perms = generatePerms(testList)
        val permsList = mutableListOf<IntArray>()
        perms.forEach {
            permsList.add(it.toIntArray())
        }
        assert(permsList.size == factorial(testList.size))

    }
}