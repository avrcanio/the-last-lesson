package com.qubit.thelastlesson.navigation

import androidx.annotation.StringRes
import com.qubit.thelastlesson.R

enum class AppScreen(
    @StringRes val titleRes: Int,
    @StringRes val descriptionRes: Int,
    @StringRes val primaryActionRes: Int,
) {
    MainMenu(
        titleRes = R.string.main_menu_title,
        descriptionRes = R.string.main_menu_description,
        primaryActionRes = R.string.main_menu_action,
    ),
    OutsideSchool(
        titleRes = R.string.outside_scene_title,
        descriptionRes = R.string.outside_scene_description,
        primaryActionRes = R.string.outside_scene_action,
    ),
}
