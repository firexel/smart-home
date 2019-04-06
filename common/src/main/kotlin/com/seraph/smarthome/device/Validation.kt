package com.seraph.smarthome.device


fun <S> S.validate(validator: S.() -> List<InvalidData>) {
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
