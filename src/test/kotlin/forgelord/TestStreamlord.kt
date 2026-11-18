package forgelord

import forgelord.configuration.StreamlordConfiguration
import io.github.markusaugust.streamlord.core.application.Streamlord
import tools.jackson.databind.json.JsonMapper
import tools.jackson.module.kotlin.KotlinModule

/**
 * The Streamlord the app declares, made by the app's own configuration, for tests that run
 * without Spring. Only the mapper differs: this one has the Kotlin module and nothing else, where
 * the app has the mapper Boot configures. The tests that start a server run on that one.
 */
fun testStreamlord(): Streamlord = StreamlordConfiguration().streamlord(JsonMapper.builder().addModule(KotlinModule.Builder().build()).build())
