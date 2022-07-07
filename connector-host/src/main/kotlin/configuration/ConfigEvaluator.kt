package configuration

import script.definition.ConfigScript
import script.definition.TreeBuilder
import kotlin.script.experimental.api.ScriptDiagnostic
import kotlin.script.experimental.api.ScriptEvaluationConfiguration
import kotlin.script.experimental.api.constructorArgs
import kotlin.script.experimental.api.with
import kotlin.script.experimental.host.toScriptSource
import kotlin.script.experimental.jvmhost.BasicJvmScriptingHost
import kotlin.script.experimental.jvmhost.createJvmCompilationConfigurationFromTemplate

class ConfigEvaluator(config: String) {

    private val source = config.toScriptSource()

    fun evaluate(treeBuilder: TreeBuilder): List<ExecutionNote> {
        val compilationConfiguration = createJvmCompilationConfigurationFromTemplate<ConfigScript>()
        val evaluationEnv = ScriptEvaluationConfiguration().with {
            constructorArgs(treeBuilder)
        }
        val res = BasicJvmScriptingHost().eval(
            source,
            compilationConfiguration,
            evaluationEnv
        )
        return res.reports.filter {
            it.severity > ScriptDiagnostic.Severity.DEBUG
        }.map {
            ExecutionNote(
                ExecutionNote.Severity.ERROR,
                "${it.severity} : ${it.location} :  ${it.message}" + if (it.exception == null) "" else ": ${it.exception}"
            )
        }
    }
}