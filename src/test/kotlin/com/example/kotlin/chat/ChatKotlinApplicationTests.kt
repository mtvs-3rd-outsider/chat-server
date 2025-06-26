package com.example.kotlin.chat

import app.cash.turbine.test
import com.example.kotlin.chat.repository.ContentType
import com.example.kotlin.chat.repository.Message
import com.example.kotlin.chat.repository.MessageRepository
import com.example.kotlin.chat.service.MessageVM
import com.example.kotlin.chat.service.UserVM
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.web.server.LocalServerPort
import org.springframework.messaging.rsocket.RSocketRequester
import org.springframework.messaging.rsocket.dataWithType
import org.springframework.messaging.rsocket.retrieveFlow
import java.net.URI
import java.time.Instant
import java.time.temporal.ChronoUnit.MILLIS
import kotlin.time.ExperimentalTime

@SpringBootTest(
    webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
    properties = [
        "spring.r2dbc.url=r2dbc:h2:mem:///testdb;USER=sa;PASSWORD=password",
        "spring.config.location=classpath:application.properties"
    ]
)
class ChatKotlinApplicationTests(
    @Autowired val rsocketBuilder: RSocketRequester.Builder,
    @Autowired val messageRepository: MessageRepository,
    @LocalServerPort val serverPort: Int
) {

    lateinit var lastMessageId: String

    val now: Instant = Instant.now()

    @BeforeEach
    fun setUp() {
        runBlocking {
            val secondBeforeNow = now.minusSeconds(1)
            val twoSecondBeforeNow = now.minusSeconds(2)
            val savedMessages = messageRepository.saveAll(
                listOf(
                    Message(
                        "*testMessage*",
                        ContentType.PLAIN.name.lowercase(),
                        twoSecondBeforeNow,
                        "test",
                        "http://test.com",
                        null,
                        null,
                        null,
                        null
                    ),
                    Message(
                        "**testMessage2**",
                        ContentType.MARKDOWN.name.lowercase(),
                        secondBeforeNow,
                        "test1",
                        "http://test.com",
                        null,
                        null,
                        null,
                        null
                    ),
                    Message(
                        "`testMessage3`",
                        ContentType.MARKDOWN.name.lowercase(),
                        now,
                        "test2",
                        "http://test.com",
                        null,
                        null,
                        null,
                        null
                    )
                )
            ).toList()
            lastMessageId = savedMessages.first().id?.toString() ?: ""
        }
    }

    @AfterEach
    fun tearDown() {
        runBlocking {
            messageRepository.deleteAll()
        }
    }

    @ExperimentalTime
    @ExperimentalCoroutinesApi
    @Test
    fun `test that messages API streams latest messages`() {
        runBlocking {
            val rSocketRequester = rsocketBuilder.websocket(URI("ws://localhost:${serverPort}/rsocket"))

            rSocketRequester
                .route("api.v1.messages.stream")
                .retrieveFlow<MessageVM>()
                .test {
                    assertThat(expectItem().prepareForTesting())
                        .isEqualTo(
                            MessageVM(
                                "*testMessage*",
                                UserVM("test", "test", "http://test.com", "test"),
                                now.minusSeconds(2).truncatedTo(MILLIS),
                                "",
                                null,
                                null,
                                null,
                                "",
                                null
                            )
                        )

                    assertThat(expectItem().prepareForTesting())
                        .isEqualTo(
                            MessageVM(
                                "<body><p><strong>testMessage2</strong></p></body>",
                                UserVM("test1", "test1", "http://test.com", "test1"),
                                now.minusSeconds(1).truncatedTo(MILLIS),
                                "",
                                null,
                                null,
                                null,
                                "",
                                null
                            )
                        )
                    assertThat(expectItem().prepareForTesting())
                        .isEqualTo(
                            MessageVM(
                                "<body><p><code>testMessage3</code></p></body>",
                                UserVM("test2", "test2", "http://test.com", "test2"),
                                now.truncatedTo(MILLIS),
                                "",
                                null,
                                null,
                                null,
                                "",
                                null
                            )
                        )

                    expectNoEvents()

                    launch {
                        rSocketRequester.route("api.v1.messages.stream")
                            .dataWithType(flow {
                                emit(
                                    MessageVM(
                                        "`HelloWorld`",
                                        UserVM("test", "test", "http://test.com", "test"),
                                        now.plusSeconds(1),
                                        "",
                                        null,
                                        null,
                                        null,
                                        "",
                                        null
                                    )
                                )
                            })
                            .retrieveFlow<Void>()
                            .collect()
                    }

                    assertThat(expectItem().prepareForTesting())
                        .isEqualTo(
                            MessageVM(
                                "<body><p><code>HelloWorld</code></p></body>",
                                UserVM("test", "test", "http://test.com", "test"),
                                now.plusSeconds(1).truncatedTo(MILLIS),
                                "",
                                null,
                                null,
                                null,
                                "",
                                null
                            )
                        )

                    cancelAndIgnoreRemainingEvents()
                }
        }
    }

    @ExperimentalTime
    @Test
    fun `test that messages streamed to the API is stored`() {
        runBlocking {
            launch {
                val rSocketRequester = rsocketBuilder.websocket(URI("ws://localhost:${serverPort}/rsocket"))

                rSocketRequester.route("api.v1.messages.stream")
                    .dataWithType(flow {
                        emit(
                            MessageVM(
                                "`HelloWorld`",
                                UserVM("test", "test", "http://test.com", "test"),
                                now.plusSeconds(1),
                                "",
                                null,
                                null,
                                null,
                                "",
                                null
                            )
                        )
                    })
                    .retrieveFlow<Void>()
                    .collect()
            }

            delay(2000)

            messageRepository.findAll()
                .first { it.content.contains("HelloWorld") }
                .apply {
                    assertThat(this.prepareForTesting())
                        .isEqualTo(
                            Message(
                                "`HelloWorld`",
                                ContentType.MARKDOWN.name.lowercase(),
                                now.plusSeconds(1).truncatedTo(MILLIS),
                                "test",
                                "http://test.com",
                                null,
                                null,
                                null,
                                null
                            )
                        )
                }
        }
    }
}
