package forgelord

import io.github.markusaugust.streamlord.core.application.Streamlord
import io.github.markusaugust.streamlord.json.jackson.JacksonSignalsCodec
import tools.jackson.databind.json.JsonMapper
import tools.jackson.module.kotlin.KotlinModule

/** The Streamlord the app declares, for tests that run without Spring. */
fun testStreamlord(): Streamlord =
    Streamlord(codec = JacksonSignalsCodec(JsonMapper.builder().addModule(KotlinModule.Builder().build()).build()))
