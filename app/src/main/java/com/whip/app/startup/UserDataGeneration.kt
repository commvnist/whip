package com.whip.app.startup

const val USER_DATA_GENERATION_KEY = "whip_user_data_generation"
const val MISSING_USER_DATA_GENERATION = Long.MIN_VALUE

internal fun generationMatches(current: Long, presented: Long): Boolean =
    presented == current || (current == 0L && presented == MISSING_USER_DATA_GENERATION)
