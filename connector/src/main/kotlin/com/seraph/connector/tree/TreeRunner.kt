package com.seraph.connector.tree

import com.seraph.connector.configuration.ConfigExecutor
import com.seraph.connector.configuration.ExecutionNote
import com.seraph.smarthome.util.Log
import script.definition.TreeBuilder

class TreeRunner(
    private val log: Log,
    private val runTreeBuilderFactory: (TreeHolder) -> TreeBuilder,
) {
    private var currentTreeRunner: TreeBuilder? = null
    private var currentTreeHolder: TreeHolder? = null

    suspend fun stopCurrent() {
        currentTreeHolder?.stop()
        currentTreeHolder = null
        currentTreeRunner = null
    }

    suspend fun runConfig(config: String): List<ExecutionNote> {
        val treeHolder = TreeHolder(log.copy("Holder"))
        currentTreeHolder = treeHolder
        val treeBuilder = runTreeBuilderFactory(treeHolder)
        currentTreeRunner = treeBuilder
        return ConfigExecutor(config).execute(treeBuilder)
    }
}