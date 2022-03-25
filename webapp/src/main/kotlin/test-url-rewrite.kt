import io.ktor.application.*
import io.ktor.html.*
import io.ktor.http.*
import io.ktor.http.content.*
import io.ktor.response.*
import io.ktor.utils.io.*
import kotlinx.html.*
import kotlinx.html.consumers.delayed
import kotlinx.html.stream.appendHTML
import java.io.ByteArrayOutputStream

fun main() {
    println(buildString {
        appendHTML().urlRewrite { it.substringBefore("?") + "?q={\"q\":2021}" }.html {
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
            }
        }
    })
}

fun <T> TagConsumer<T>.urlRewrite(rewriter: (String) -> String): TagConsumer<T> =
    UrlRewriteTagConsumer(this, rewriter).delayed()

private class UrlRewriteTagConsumer<T>(val downstream: TagConsumer<T>, val rewriter: (String) -> String) :
    TagConsumer<T> by downstream {
    override fun onTagStart(tag: Tag) {
        if (tag is A) {
            val copy = A(tag.attributes.transformValue("href"), this)
            downstream.onTagStart(copy)
        } else if (tag is FORM) {
            val copy = FORM(tag.attributes.transformValue("action"), this)
            //if method is get, add hidden element instead
            downstream.onTagStart(copy)
        } else {
            downstream.onTagStart(tag)
        }

//        if(tag.tagName == "a" && tag.attributes["href"] != null) {
//            tag.attributes["href"] = rewriter(tag.attributes["href"]!!)
//        }
//        if(tag.tagName == "form" && tag.attributes["action"] != null) {
//            tag.attributes["action"] = rewriter(tag.attributes["action"]!!)
//        }

    }

    fun Map<String, String>.transformValue(key: String): Map<String, String> {
        val copy = HashMap(this)
        copy[key] = rewriter(copy[key]!!)
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
