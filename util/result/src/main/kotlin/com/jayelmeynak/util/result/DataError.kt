package com.jayelmeynak.util.result

public sealed interface DataError: Error {

    public enum class Remote: DataError {
        REQUEST_TIMEOUT,
        TOO_MANY_REQUESTS,
        NO_INTERNET,
        SERVER,
        SERIALIZATION,
        NOT_FOUND,
        UNKNOWN
    }

    public enum class Local: DataError {
        DISK_FULL,
        UNKNOWN
    }

}