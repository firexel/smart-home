package configuration

import api.ConfigInstaller
import script.definition.TreeBuilder

class EvalConfigInstaller(
    private val checkerFactory: () -> CheckingTreeBuilder,
) : ConfigInstaller {

    override suspend fun installConfig(config: String): List<ExecutionNote> {
        TODO()
    }

    override suspend fun checkConfig(config: String): List<ExecutionNote> {
        val checker = checkerFactory()
        val errors = ConfigEvaluator(config).evaluate(checker)
        return errors + checker.results
    }

    interface CheckingTreeBuilder : TreeBuilder {
        val results: List<ExecutionNote>
    }
}