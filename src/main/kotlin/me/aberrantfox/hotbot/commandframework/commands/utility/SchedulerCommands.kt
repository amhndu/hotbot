package me.aberrantfox.hotbot.commandframework.commands.utility

import me.aberrantfox.hotbot.commandframework.parsing.ArgumentType
import me.aberrantfox.hotbot.database.deleteReminder
import me.aberrantfox.hotbot.database.insertReminder
import me.aberrantfox.hotbot.dsls.command.CommandSet
import me.aberrantfox.hotbot.dsls.command.commands
import me.aberrantfox.hotbot.extensions.jda.fullName
import me.aberrantfox.hotbot.extensions.jda.sendPrivateMessage
import me.aberrantfox.hotbot.extensions.stdlib.convertToTimeString
import me.aberrantfox.hotbot.logging.BotLogger
import me.aberrantfox.hotbot.utility.futureTime
import net.dv8tion.jda.core.entities.User
import java.util.*
import kotlin.concurrent.schedule
import kotlin.math.roundToLong

@CommandSet
fun schedulerCommands() = commands {
    command("remindme") {
        expect(ArgumentType.TimeString, ArgumentType.Sentence)
        execute {
            val timeMilliSecs = (it.args.component1() as Double).roundToLong() * 1000
            val title = it.args.component2()

            it.respond("Got it, I'll remind you about that in ${timeMilliSecs.convertToTimeString()}")

            insertReminder(it.author.id, title as String, futureTime(timeMilliSecs))
            scheduleReminder(it.author, title, timeMilliSecs, log)
        }
    }
}

fun scheduleReminder(user: User, message: String, timeMilli: Long, log: BotLogger) {
    fun remindTask () {
        deleteReminder(user.id, message)
        log.info("${user.fullName()} reminded themselves about: $message")
        user.sendPrivateMessage("Hi, you asked me to remind you about: $message")
    }

    if (timeMilli <= 0) {
        remindTask()
        return
    }

    Timer().schedule(timeMilli) { remindTask() }
}

