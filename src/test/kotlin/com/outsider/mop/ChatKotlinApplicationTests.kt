package com.outsider.mop

import com.outsider.mop.chat.ChatKotlinApplication
import com.outsider.mop.chat.repository.ContentType
import com.outsider.mop.chat.repository.Message
import com.outsider.mop.chat.repository.MessageRepository
import com.outsider.mop.chat.service.MessageVM
import com.outsider.mop.chat.service.UserVM
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
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
import java.net.URL
import java.time.Instant
import java.time.temporal.ChronoUnit
import java.time.temporal.ChronoUnit.MINUTES
import kotlin.properties.Delegates
import kotlin.time.Duration.Companion.seconds
import kotlin.time.ExperimentalTime

@SpringBootTest(
    classes = [ChatKotlinApplication::class],
    webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
    properties = [
        "spring.r2dbc.url=r2dbc:mysql://root:1234@localhost:9876/forcasthub"
    ]
)
class ChatKotlinApplicationTests(
    @Autowired val rsocketBuilder: RSocketRequester.Builder,
    @Autowired val messageRepository: MessageRepository,
    @LocalServerPort val serverPort: Int
) {

    var lastMessageId by Delegates.notNull<Long>()

    val now: Instant = Instant.now().truncatedTo(ChronoUnit.MINUTES)

    @BeforeEach
    fun setUp() = runBlocking {
        val secondBeforeNow = now
        val twoSecondBeforeNow = now
        val savedMessages = messageRepository.saveAll(
            listOf(
                Message(
                    "*testMessage*",
                    ContentType.PLAIN.name,
                    twoSecondBeforeNow,
                    "test",
                    "http://test.com",
                    2,
                    null
                ),
                Message(
                    "*testMessage*",
                    ContentType.PLAIN.name,
                    secondBeforeNow,
                    "test1",
                    "http://test.com",
                    2,
                    null
                ),
                Message(
                    "*testMessage*",
                    ContentType.PLAIN.name,
                    now,
                    "test2",
                    "http://test.com",
                    2,
                    null
                )
            )
        ).toList()
        lastMessageId = savedMessages.first().id ?: 0
    }

    @AfterEach
    fun tearDown() = runBlocking {
        messageRepository.deleteAll()
    }

    @ExperimentalTime
    @ExperimentalCoroutinesApi
    @Test
    fun test_that_messages_API_streams_latest_messages(): Unit = runBlocking {
        val rSocketRequester = rsocketBuilder.websocket(URI("ws://localhost:${serverPort}/rsocket"))

        val messages = rSocketRequester
            .route("api.v1.messages.stream/{roomId}", 2)
            .retrieveFlow<MessageVM>()
            .take(3)
            .toList()

        assertThat(messages[0].prepareForTesting())
            .isEqualTo(
                MessageVM(
                    "*testMessage*",
                    UserVM("test", URL("http://test.com").toURI()),
                    now.truncatedTo(MINUTES), 2
                )
            )

        assertThat(messages[1].prepareForTesting())
            .isEqualTo(
                MessageVM(
                    "*testMessage*",
                    UserVM("test1", URL("http://test.com").toURI()),
                    now.truncatedTo(MINUTES), 2
                )
            )

        assertThat(messages[2].prepareForTesting())
            .isEqualTo(
                MessageVM(
                    "*testMessage*",
                    UserVM("test2", URL("http://test.com").toURI()),
                    now.truncatedTo(MINUTES), 2
                )
            )

        assertThat(messages.size).isEqualTo(3)

        rSocketRequester.route("api.v1.messages.stream/{roomId}", 2)
            .dataWithType(flow {
                emit(
                    MessageVM(
                        "*testMessage*",
                        UserVM("test", URL("http://test.com").toURI()),
                        now, 2
                    )
                )
            })
            .retrieveFlow<Void>()
            .collect()

        val newMessages = rSocketRequester
            .route("api.v1.messages.stream/{roomId}", 2)
            .retrieveFlow<MessageVM>()
            .take(1) // 새로운 메시지 하나만 확인
            .toList()

        assertThat(newMessages[0].prepareForTesting())
            .isEqualTo(
                MessageVM(
                    "*testMessage*",
                    UserVM("test", URL("http://test.com").toURI()),
                    now,
                    2
                )
            )
    }

    @ExperimentalTime
    @Test
    fun test_that_messages_streamed_to_the_API_is_stored(): Unit = runBlocking {
        val rSocketRequester = rsocketBuilder.websocket(URI("ws://localhost:${serverPort}/rsocket"))

        rSocketRequester.route("api.v1.messages.stream/{roomId}", 2)
            .dataWithType(flow {
                emit(
                    MessageVM(
                        "HelloWorld",
                        UserVM("test", URL("http://test.com").toURI()),
                        now, 2
                    )
                )
            })
            .retrieveFlow<Void>()
            .collect()


        val storedMessage = messageRepository.findAll()
            .first { it.content.contains("HelloWorld") }

        assertThat(storedMessage.prepareForTesting())
            .isEqualTo(
                Message(
                    "HelloWorld",
                    ContentType.MARKDOWN.name,
                    now,
                    "test",
                    "http://test.com",
                    2,
                    null
                )
            )
    }
}
