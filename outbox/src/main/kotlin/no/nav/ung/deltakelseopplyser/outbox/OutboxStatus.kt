import com.fasterxml.jackson.annotation.JsonValue

enum class OutboxStatus(@JsonValue val value: String) {
    PENDING("PENDING"),
    PROCESSING("PROCESSING"),
    PROCESSED("PROCESSED"),
    FAILED("FAILED"),
    DEAD_LETTER("DEAD_LETTER")
}
