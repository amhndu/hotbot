package me.aberrantfox.aegeus.commandframework.commands

import me.aberrantfox.aegeus.services.Configuration
import me.aberrantfox.aegeus.services.saveConfigurationFile
import me.aberrantfox.aegeus.commandframework.ArgumentType
import me.aberrantfox.aegeus.commandframework.Command
import net.dv8tion.jda.core.events.message.MessageReceivedEvent
import java.util.*

val startTime = Date()

@Command fun ping(event: MessageReceivedEvent) = event.channel.sendMessage("Pong!").queue()

@Command fun help(event: MessageReceivedEvent) = event.channel.sendMessage("url-to-help-to-be-added").queue()

@Command fun uptime(event: MessageReceivedEvent) {
    val minutes = Date().time - startTime.time / 1000 / 60
    val currentDate = startTime.toString()

    event.channel.sendMessage("I've been awake since ${currentDate}, so like... ${minutes} minutes").queue()
}

@Command(ArgumentType.INTEGER, ArgumentType.INTEGER)
fun add(event: MessageReceivedEvent, args: List<Any>) {
    val left = args[0] as Int
    val right = args[1] as Int

    event.channel.sendMessage("Result: ${left + right}").queue()
}

@Command
fun exit(event: MessageReceivedEvent, args: List<Any>, config: Configuration) {
    saveConfigurationFile(config)
    event.channel.sendMessage("Exiting").queue()
    System.exit(0)
}

@Command
fun kill(event: MessageReceivedEvent) {
    event.channel.sendMessage("Killing process, configurations will not be saved.").queue()
    System.exit(0)
}