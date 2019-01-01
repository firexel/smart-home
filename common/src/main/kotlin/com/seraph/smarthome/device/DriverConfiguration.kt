package com.seraph.smarthome.device


interface DriverConfiguration<out S> {
    val settings: S
    val connections: Connections

    fun validate(validator: DriverConfiguration<S>.() -> List<InvalidData>) {
        val invalidFields = try {
            validator()
        } catch (t: Throwable) {
            throw ValidationException("Error while validating $this", t)
        }
        if (invalidFields.isNotEmpty()) {
            val message = "Following errors found during validation: \n" +
                    invalidFields.joinToString("\n", "    ")
            throw ValidationException(message)
        }
    }

    class ValidationException(string: String, cause: Throwable? = null)
        : RuntimeException(string, cause)

    data class InvalidData(val key: String, val message: String) {
        override fun toString(): String = "'$key': $message"
    }

    data class Connections(
            private val map: Map<String, Alias>
    ) {
        val keys: Set<String>
            get() = map.keys
    }

    data class Alias(
            val names: List<String>
    ) {
        constructor(singleName: String) : this(listOf(singleName))
    }
}