package com.whip.app.data

/** Read bounded, keyset-ordered pages inside the caller's snapshot transaction. */
internal suspend fun <T> readHistoryPages(loadPage: suspend (T?) -> List<T>): List<T> = buildList {
    while (true) {
        val page = loadPage(lastOrNull())
        if (page.isEmpty()) break
        addAll(page)
    }
}
