package com.cosmonaut.app.util

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PromptLoader @Inject constructor(@ApplicationContext private val context: Context,) {
    private val prompts: List<String> by lazy {
        context.assets.open("story-prompts.txt")
            .bufferedReader()
            .readLines()
            .filter { it.isNotBlank() }
    }

    fun getRandomPrompt(): String = prompts.random()
}
