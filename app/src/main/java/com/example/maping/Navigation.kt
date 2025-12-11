package com.example.maping

sealed class AppScreen(val route: String) {
    object Login : AppScreen("login")
    object Map : AppScreen("map")
    object Upload : AppScreen("upload")
    object Profile : AppScreen("profile")
    object FindFriends : AppScreen("find_friends")


    object Detail : AppScreen("detail/{postId}") {
        fun createRoute(postId: String) = "detail/$postId"
    }

    object NfcDetail : AppScreen("nfc_detail/{tagData}") {
        fun createRoute(tagData: String) = "nfc_detail/$tagData"
    }
}