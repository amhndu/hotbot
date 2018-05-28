package me.aberrantfox.hotbot.commands.utility

import com.github.ricksbrown.cowsay.Cowsay
import me.aberrantfox.kjdautils.api.dsl.CommandSet
import me.aberrantfox.kjdautils.api.dsl.arg
import me.aberrantfox.kjdautils.api.dsl.commands
import me.aberrantfox.kjdautils.internal.command.arguments.SentenceArg
import me.aberrantfox.kjdautils.internal.command.arguments.SplitterArg
import org.jsoup.Jsoup
import java.io.File
import java.net.URLEncoder
import java.util.*
import khttp.get as kget

@CommandSet
fun funCommands() =
    commands {
        command("cat") {
            execute {
                val json = kget("http://aws.random.cat/meow").jsonObject
                it.respond(json.getString("file"))
            }
        }

        command("bird") {
            execute {
                val json = kget("https://birdsare.cool/bird.json?exclude=webm,mp4").jsonObject
                it.respond(json.getString("url"))
            }
        }

        command("flip") {
            expect(arg(SplitterArg, true, listOf("Heads", "Tails")))
            execute {
                val options = it.args[0] as List<String>
                var choice = options[Random().nextInt(options.size)]
                if (options.size == 1)
                    choice += "\n... were you expecting something else ? :thinking: Did you forget the `|` separator ?"
                it.safeRespond(choice)
            }
        }

        command("dog") {
            execute {
                val json = kget("https://dog.ceo/api/breeds/image/random").jsonObject
                it.respond(json.getString("message"))
            }
        }

        command("google") {
            expect(SentenceArg)
            execute {
                val google = "http://www.google.com/search?q="
                val search = it.args[0] as String
                val charset = "UTF-8"
                val userAgent = "Mozilla/5.0"

                val links = Jsoup.connect(google + URLEncoder.encode(search, charset)).userAgent(userAgent).get().select(".g>.r>a")

                it.respond(links.first().absUrl("href"))
            }
        }

        command("cowsay") {
            expect(SentenceArg)
            execute {
                val sentence = it.args[0] as String

                val response = parseCowsayArgs(sentence.split(" "))
                if(!response.isBlank()){
                    it.safeRespond(response)
                    return@execute
                }

                val specialChars = Regex("(`+|@|<@[0-9]+>)")
                val args = sentence
                        .replace("\n", " ")
                        .replace(specialChars, "")
                        .split(" ")
                        .toTypedArray()

                val result = Cowsay.say(args)

                if(!result.isBlank()){
                    val response = Cowsay.say(args)
                    if (response.length > 1994){
                        it.safeRespond("that message was too long, moo!")
                        return@execute
                    }

                    it.safeRespond("```" + Cowsay.say(args) + "```")
                }
            }
        }
    }


private fun parseCowsayArgs(arguments: List<String>): String {
    val flagsWithArgs = Regex("-T|-W|-f|-e|--alt|--lang")
    val flagsWithNoArgs = Regex("-b|-d|-g|-l|-n|-p|-s|-t|-w|-y")

    if(!arguments.mapIndexedNotNull{index, s -> if (flagsWithArgs.matches(s)) index + 1 else null }
            .all { it < arguments.size && !arguments[it].startsWith("-") && !arguments[it].contains(Regex("/|\\\\")) }){
        return "one of your flags is missing an argument, or the supplied argument is invalid"
    }

    var skipNextArg = false
    arguments.forEach{
        if (!skipNextArg) {
            if(flagsWithArgs.matches(it)){
                skipNextArg = true
            }
            if(it == "-h"){
                return "```" + Scanner(File("cowsayhelp.txt")).useDelimiter("\\Z").next() + "```"
            }
            if(!flagsWithArgs.matches(it) && !flagsWithNoArgs.matches(it)){
                return ""
            }
        }
        else {
            skipNextArg = false
        }
    }
    return ""
}