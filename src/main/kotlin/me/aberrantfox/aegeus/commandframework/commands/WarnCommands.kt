package me.aberrantfox.aegeus.commandframework.commands

import me.aberrantfox.aegeus.commandframework.ArgumentType
import me.aberrantfox.aegeus.commandframework.Command
import me.aberrantfox.aegeus.commandframework.util.idToName
import me.aberrantfox.aegeus.commandframework.util.idToUser
import me.aberrantfox.aegeus.services.*
import net.dv8tion.jda.core.EmbedBuilder
import net.dv8tion.jda.core.entities.User
import net.dv8tion.jda.core.events.message.guild.GuildMessageReceivedEvent
import java.awt.Color


@Command(ArgumentType.UserID, ArgumentType.Joiner)
fun warn(event: GuildMessageReceivedEvent, args: List<Any>, config: Configuration) =
        strike(event, listOf(args[0], 0, args[1]), config)

@Command(ArgumentType.UserID, ArgumentType.Integer, ArgumentType.Joiner)
fun strike(event: GuildMessageReceivedEvent, args: List<Any>, config: Configuration) {
    val target = args[0] as String
    val strikeQuantity = args[1] as Int
    val reason = args[2] as String

    if(strikeQuantity < 0 || strikeQuantity > 3) {
        event.channel.sendMessage("Strike weight should be between 0 and 3").queue()
        return
    }

    if( !(event.guild.members.map { it.user.id }.contains(target)) ) {
        event.channel.sendMessage("Cannot find the member by the id: $target").queue()
        return
    }

    insertInfraction(target, event.author.id, strikeQuantity, reason)

    event.author.openPrivateChannel().queue {
        it.sendMessage("User ${target.idToUser(event.jda).asMention} has been infracted with weight: $strikeQuantity," +
                "with reason $reason.").queue()
    }

    administerPunishment(config, target.idToUser(event.jda), strikeQuantity, reason, event)
}

@Command(ArgumentType.UserID)
fun history(event: GuildMessageReceivedEvent, args: List<Any>) {
    val target = args[0] as String
    val records = getHistory(target)
    val builder = EmbedBuilder()
            .setTitle("${target.idToName(event.jda)}'s Record")
            .setColor(Color.MAGENTA)
            .setThumbnail(target.idToUser(event.jda).avatarUrl)

    records.forEach {
        builder.addField("Strike ID: ${it.id}",
                "**Acting moderator**: ${it.moderator.idToName(event.jda)}" +
                        "\n**Reason**: ${it.reason}" +
                        "\n**Weight**: ${it.strikes}",
                false)
    }

    event.channel.sendMessage(builder.build()).queue()
}

@Command(ArgumentType.Integer)
fun removeStrike(event: GuildMessageReceivedEvent, args: List<Any>) {
    val strikeID = args[0] as Int
    val amountRemoved = removeInfraction(strikeID)

    event.channel.sendMessage("Deleted $amountRemoved strike records.").queue()
}

private fun administerPunishment(config: Configuration, user: User, strikeQuantity: Int, reason: String,
                                 event: GuildMessageReceivedEvent) {
    user.openPrivateChannel().queue {
        it.sendMessage("${it.user.asMention}, you have been infracted. Infractions are formal warnings from staff members" +
                " on TPH. The infraction you just received was a $strikeQuantity strike infraction," +
                " and you received it for reason: **$reason**").queue()

        val punishmentAction = config.infractionActionMap[strikeQuantity]

        it.sendMessage("The assigned punishment for this infraction is: $punishmentAction").queue()

        when (config.infractionActionMap[strikeQuantity]) {
            InfractionAction.Warn -> {
                it.sendMessage("This is your warning - Do not break the rules again.").queue()
            }
            InfractionAction.Kick -> {
                it.sendMessage("You may return via this: https://discord.gg/BQN6BYE - please be mindful of the rules next time.")
                        .queue()
                event.guild.controller.kick(user.id, reason).queue()
            }
            InfractionAction.Mute -> {
                it.sendMessage("You have been muted. Please read the rules.").queue()

            }
            InfractionAction.Ban -> {
                it.sendMessage("Well... that happened. There may be an appeal system in the future. But for now, you're" +
                        "permanently banned. Sorry about that :) ").queue()
                event.guild.controller.ban(user.id, 0, reason).queue()
            }
        }
    }
}