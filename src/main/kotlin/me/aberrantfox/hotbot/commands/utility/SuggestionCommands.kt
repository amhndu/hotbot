package me.aberrantfox.hotbot.commands.utility

import me.aberrantfox.hotbot.database.*
import me.aberrantfox.hotbot.services.AddResponse
import me.aberrantfox.hotbot.services.Configuration
import me.aberrantfox.hotbot.services.PoolRecord
import me.aberrantfox.hotbot.services.UserElementPool
import me.aberrantfox.kjdautils.api.dsl.CommandSet
import me.aberrantfox.kjdautils.api.dsl.commands
import me.aberrantfox.kjdautils.api.dsl.embed
import me.aberrantfox.kjdautils.extensions.jda.sendPrivateMessage
import me.aberrantfox.kjdautils.internal.command.arguments.SentenceArg
import me.aberrantfox.kjdautils.internal.command.arguments.WordArg
import net.dv8tion.jda.core.EmbedBuilder
import net.dv8tion.jda.core.JDA
import net.dv8tion.jda.core.entities.Guild
import java.awt.Color


enum class SuggestionStatus(val colour: Color, val message: String) {
    Accepted(Color.green, "Accepted"),
    Review(Color.orange, "Under Review"),
    Denied(Color.red, "Denied")
}

object Suggestions {
    val pool: UserElementPool = UserElementPool(poolName = "Suggestions")
}

@CommandSet
fun suggestionCommands(config: Configuration) = commands {
    command("suggest") {
        expect(SentenceArg)
        execute {
            val author = it.author.id
            val message = it.args[0] as String

            val response = Suggestions.pool.addRecord(author, it.author.effectiveAvatarUrl, message)

            when (response) {
                AddResponse.PoolFull -> it.respond("You have enough suggestions in the pool for now...")
                AddResponse.UserFull -> it.respond("There are too many suggestions in the pool to handle your request currently... sorry about that.")
                AddResponse.Accepted -> it.respond("Your suggestion has been added to the review-pool. If it passes it'll be pushed out to the suggestions channel.")
            }
        }
    }

    command("poolinfo") {
        execute {
            it.respond(EmbedBuilder().setTitle("Suggestion Pool Info")
                .setColor(Color.cyan)
                .setDescription("There are currently ${Suggestions.pool.entries()} suggestions in the pool.")
                .build())
        }
    }

    command("pooltop") {
        execute {
            val suggestion = Suggestions.pool.peek()
            if (suggestion == null) {
                it.respond("The pool is empty.")
                return@execute
            }

            it.respond(suggestion.describe(it.jda, "Suggestion"))
        }
    }

    command("poolaccept") {
        execute {
            val suggestion = Suggestions.pool.top()

            if (suggestion == null) {
                it.respond("The suggestion pool is empty... :)")
                return@execute
            }

            val guild = it.jda.getGuildById(config.serverInformation.guildid)

            val channel = guild.textChannels.findLast { channel ->
                channel.id == config.messageChannels.suggestionChannel
            }


            channel?.sendMessage(buildSuggestionMessage(suggestion, it.jda, SuggestionStatus.Review).build())?.queue {
                trackSuggestion(SuggestionRecord(it.id, SuggestionStatus.Review, suggestion))

                it.addReaction("⬆").queue()
                it.addReaction("⬇").queue()
            }
        }
    }

    command("pooldeny") {
        execute {
            val rejected = Suggestions.pool.top()

            if (rejected == null) {
                it.respond("The suggestion pool is empty... :)")
                return@execute
            }

            it.respond(rejected.describe(it.jda, "Suggestion"))
        }
    }

    command("respond") {
        expect(WordArg, WordArg, SentenceArg)
        execute {
            val args = it.args

            val target = args[0] as String
            val response = args[1] as String
            val reason = args[2] as String
            val status = inputToStatus(response)

            if (status == null) {
                it.respond("Valid responses are 'accepted', 'denied' and 'review'... use accordingly.")
                return@execute
            }

            val guild = it.jda.getGuildById(config.serverInformation.guildid)

            val channel = fetchSuggestionChannel(guild, config)

            if (!(isTracked(target)) || channel.getMessageById(target) == null) {
                it.respond("That is not a valid message or a suggestion by the ID.")
                return@execute
            }

            channel.getMessageById(target).queue {
                val suggestion = obtainSuggestion(target)
                val message = buildSuggestionMessage(suggestion.poolInfo, it.jda, status)
                val reasonTitle = "Reason for Status"

                val suggestionUpdateMessage = buildSuggestionUpdateEmbed(suggestion, reason, status)
                it.jda.retrieveUserById(suggestion.member).complete()
                        .sendPrivateMessage(suggestionUpdateMessage)

                message.fields.removeIf { it.name == reasonTitle }

                message.addField(reasonTitle, reason, false)
                updateSuggestion(target, status)

                it.editMessage(message.build()).queue()
            }
        }
    }
}

private fun fetchSuggestionChannel(guild: Guild, config: Configuration) = guild.getTextChannelById(config.messageChannels.suggestionChannel)

private fun inputToStatus(input: String): SuggestionStatus? = SuggestionStatus.values().findLast { it.name.toLowerCase() == input.toLowerCase() }

private fun buildSuggestionUpdateEmbed(suggestion: SuggestionRecord, response: String, newStatus: SuggestionStatus) =
        embed {
            title("Suggestion Status Update")
            description("A suggestion that you submitted has changed status.")

            ifield {
                name = "ID"
                value = suggestion.messageID
            }

            ifield {
                name = "Old Status"
                value = suggestion.status.toString()
            }

            ifield {
                name = "New Status"
                value = newStatus.toString()
            }

            field {
                name = "Suggestion"
                value = suggestion.idea
                inline = false
            }

            field {
                name = "Response"
                value = response
                inline = false
            }

            setColor(newStatus.colour)
        }

private fun buildSuggestionMessage(suggestion: PoolRecord, jda: JDA, status: SuggestionStatus) =
    EmbedBuilder()
        .setTitle("${jda.retrieveUserById(suggestion.sender).complete().name}'s Suggestion")
        .setThumbnail(suggestion.avatarURL)
        .setColor(status.colour)
        .setDescription(suggestion.message)
        .addField("Suggestion Status", status.message, false)