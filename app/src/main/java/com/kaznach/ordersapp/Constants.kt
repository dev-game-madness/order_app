package com.kaznach.ordersapp
object ApiConstants {
    private const val SERVER_ADDRESS = "http://192.168.1.104:"
    private const val API_VERSION = "api/v1/"

//    private val PORTS = mapOf(
//        "sessions/check" to 5000,
//        "connection/check" to 5001,
//        "users/logout" to 5002,
//        "users/profile" to 5003,
//        "users/log" to 5004,
//        "users/reg" to 5005,
//        "orders/create" to 5006,
//        "orders/all" to 5007,
//        "orders/myorders" to 5008,
//        "orders/myarchiveorders" to 5009
//    )

    private val PORTS = mapOf(
        "sessions/check" to 5000,
        "connection/check" to 5000,
        "users/logout" to 5000,
        "users/profile" to 5000,
        "users/log" to 5000,
        "users/reg" to 5000,
        "orders/create" to 5000,
        "orders/all" to 5000,
        "orders/myorders" to 5000,
        "orders/myarchiveorders" to 5000
    )

    val URLS = PORTS.map { (endpoint, port) ->
        endpoint to "$SERVER_ADDRESS${port}/$API_VERSION$endpoint"
    }.toMap()
}