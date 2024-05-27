package com.kaznach.ordersapp
object ApiConstants {
    const val BASE_URL = "http://192.168.1.104:5000/api/v1/"
    const val CHECK_SESSION_URL = "${BASE_URL}sessions/check"
    const val CHECK_CONNECTION_URL = "${BASE_URL}connection/check"
    const val LOGOUT_URL = "${BASE_URL}users/logout"
    const val PROFILE_URL = "${BASE_URL}users/profile"
    const val LOGIN_URL = "${BASE_URL}users/log"
    const val REGISTRATION_URL = "${BASE_URL}users/reg"
    const val CREATE_ORDER_URL = "${BASE_URL}orders/create"
    const val ORDERS_URL = "${BASE_URL}orders/all"
    const val MY_ORDERS_URL = "${BASE_URL}orders/myorders"
    const val MY_ARCHIVE_ORDERS_URL = "${BASE_URL}orders/myarchiveorders"

}