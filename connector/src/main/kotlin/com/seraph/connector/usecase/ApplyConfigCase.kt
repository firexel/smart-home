package com.seraph.connector.usecase

import com.seraph.connector.configuration.*
import com.seraph.connector.tree.TreeRunner
import com.seraph.smarthome.util.Log

class ApplyConfigCase(
    private val checker: ConfigChecker,
    private val runner: TreeRunner,
    private val storage: ConfigStorage,
    private val log: Log
) {

    /**
     * Load latest config or use default
     */
    suspend fun applyLatestOrDefault(): ApplicationResult {
        return try {
            val list = storage.list()
            if (list.isNotEmpty()) {
                val header = list.maxByOrNull { it.dateCreated }!!
                apply(header.id)
            } else {
                log.w("No configs are stored. Using default empty config")
                apply("")
            }
        } catch (ex: Exception) {
            error("Failed loading latest config: ${ex.cause}")
        }
    }

    /**
     * Check, save and apply new config
     */
    suspend fun apply(config: String): ApplicationResult {
        // check new config
        val notes = ConfigExecutor(config).execute(checker)
        val errors = (notes + checker.results).errors()
        if (errors.isNotEmpty()) {
            log.w("Config not applied due to errors $errors")
            return ApplicationResult(notes + checker.results)
        }

        // apply config
        val runNotes = applyChecked(config)
        val runErrors = runNotes.errors()
        if (runErrors.isNotEmpty()) {
            log.w("Config applied but not started due to errors $runErrors")
            return ApplicationResult(checker.results + runNotes)
        }

        return try {
            val configData = storage.save(config)
            ApplicationResult(checker.results + runNotes, configData)
        } catch (ex: Throwable) {
            val err = ExecutionNote(
                ExecutionNote.Severity.ERROR,
                "Config not applied: cannot save. Exception ${ex.cause}"
            )
            log.w(err.message)
            ApplicationResult(checker.results + runNotes + err)
        }
    }

    /**
     * Load existing config, check and apply it
     */
    suspend fun apply(id: ConfigStorage.ConfigHeader.Id): ApplicationResult {

        // load config
        val config = try {
            storage.load(id)
        } catch (ex: Exception) {
            return error("Cannot load config with id $id. Exception " + ex.message)
        }

        // check new config
        val notes = ConfigExecutor(config.content).execute(checker)
        val errors = (notes + checker.results).errors()
        if (errors.isNotEmpty()) {
            log.w("Config not applied due to errors $errors")
            return ApplicationResult(notes + checker.results)
        }

        // apply config
        val runNotes = applyChecked(config.content)
        val runErrors = runNotes.errors()
        if (runErrors.isNotEmpty()) {
            log.w("Config applied but not started due to errors $runErrors")
        }

        return ApplicationResult(checker.results + runNotes, config.header)
    }

    private suspend fun applyChecked(config: String): List<ExecutionNote> {
        // stop current runner
        runner.stopCurrent()

        // run new config
        return runner.runConfig(config)
    }

    data class ApplicationResult(
        val notes: List<ExecutionNote>,
        val configHeader: ConfigStorage.ConfigHeader? = null
    )

    fun error(message: String): ApplicationResult {
        val note = ExecutionNote(
            ExecutionNote.Severity.ERROR,
            message
        )
        log.w(note.message)
        return ApplicationResult(listOf(note))
    }
}