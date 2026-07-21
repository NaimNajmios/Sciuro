package com.sciuro.core.parsing.engine

import io.ktor.client.HttpClient

expect fun createHttpClient(): HttpClient
