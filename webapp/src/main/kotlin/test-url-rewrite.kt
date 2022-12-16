import io.ktor.http.*
import io.ktor.http.content.*
import io.ktor.server.application.*
import io.ktor.server.html.*
import io.ktor.server.response.*
import io.ktor.utils.io.*
import kotlinx.html.*
import kotlinx.html.consumers.delayed
import kotlinx.html.stream.appendHTML
import java.io.ByteArrayOutputStream

/** This is just a test project made by me to test if one could modify hrefs with session data during URL build process **/
fun main() {
    println(buildString {
        appendHTML().urlRewrite { it.substringBefore("?") + "?q=" }.html {
            head { }
            body {
                a(href = "bar") {
                    href = "baz"
                }
                form(action = "list/invalidate-cache", method = FormMethod.post) {
                    button(type = ButtonType.submit, classes = "btn btn-light btn-sm") {
                        +"Neu erstellen"
                    }
                }
                form(action = "list/invalidate-cache", method = FormMethod.get) {
                    button(type = ButtonType.submit, classes = "btn btn-light btn-sm") {
                        +"Neu erstellen"
                    }
                }
            }
        }
    })
}

fun <T> TagConsumer<T>.urlRewrite(rewriter: (String) -> String): TagConsumer<T> =
    UrlRewriteTagConsumer(this, "{\"q\":2021}", "q").delayed()

private class UrlRewriteTagConsumer<T>(
    val downstream: TagConsumer<T>,
    val sessionData: String,
    val designator: String
) :
    TagConsumer<T> by downstream {
    override fun onTagStart(tag: Tag) {
        if (tag is A) {
            val copy = A(tag.attributes.transformValue("href"), this)
            downstream.onTagStart(copy)
        } else if (tag is FORM) {
            if (tag.method == FormMethod.get) {
                downstream.onTagStart(tag)
                tag.hiddenInput {
                    value = sessionData
                    name = designator
                }
            } else {
                val copy = FORM(tag.attributes.transformValue("action"), this)
                downstream.onTagStart(copy)
            }
        } else {
            downstream.onTagStart(tag)
        }
    }

    fun Map<String, String>.transformValue(key: String): Map<String, String> {
        //TODO: Only transform if required (different domain, etc.)
        val copy = HashMap(this)
        val value = copy[key] ?: return copy
        copy[key] = if (value.contains("?")) "$value&$designator=$sessionData" else "$value?$designator=$sessionData"
        return copy
    }


}

public suspend fun <TTemplate : Template<HTML>> ApplicationCall.respondHtmlTemplate2(
    template: TTemplate,
    status: HttpStatusCode = HttpStatusCode.OK,
    body: TTemplate.() -> Unit
) {
    template.body()
    respond(HtmlContent2(status, fun HTML.() {
        with(template) { apply() }
    }))
}

public class HtmlContent2(
    override val status: HttpStatusCode? = null,
    private val builder: HTML.() -> Unit
) : OutgoingContent.WriteChannelContent() {

    override val contentType: ContentType
        get() = ContentType.Text.Html.withCharset(Charsets.UTF_8)

    override suspend fun writeTo(channel: ByteWriteChannel) {
        val content = ByteArrayOutputStream()

        try {
            content.bufferedWriter().use {
                it.append("<!DOCTYPE html>\n")
                it.appendHTML().urlRewrite { it.substringBefore("?") + "?q={\"year\":2021}" }.html(block = builder)
            }
        } catch (cause: Throwable) {
            channel.close(cause)
            throw cause
        }

        channel.writeFully(content.toByteArray())
    }
}
