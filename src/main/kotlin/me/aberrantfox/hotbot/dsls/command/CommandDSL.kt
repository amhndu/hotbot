package me.aberrantfox.hotbot.dsls.command

import me.aberrantfox.hotbot.commandframework.parsing.ArgumentType
import me.aberrantfox.hotbot.extensions.stdlib.sanitiseMentions
import me.aberrantfox.hotbot.logging.BotLogger
import me.aberrantfox.hotbot.logging.DefaultLogger
import me.aberrantfox.hotbot.permissions.PermissionManager
import me.aberrantfox.hotbot.services.Configuration
import me.aberrantfox.hotbot.services.MService
import net.dv8tion.jda.core.JDA
import net.dv8tion.jda.core.entities.*
import org.reflections.Reflections
import org.reflections.scanners.MethodAnnotationsScanner

annotation class CommandSet

data class CommandEvent(val config: Configuration, val jda: JDA, val channel: MessageChannel,
                        val author: User, val message: Message, val guild: Guild, val manager: PermissionManager,
                        val container: CommandsContainer, val mService: MService,
                        var args: List<Any> = listOf()) {

    fun respond(msg: String) =
        if(msg.length > 2000) {
            val toSend = msg.chunked(2000)
            toSend.forEach { channel.sendMessage(it).queue() }
        } else {
            this.channel.sendMessage(msg).queue()
        }

    fun respond(embed: MessageEmbed) = this.channel.sendMessage(embed).queue()

    fun safeRespond(msg: String) = respond(msg.sanitiseMentions())
}

@CommandTagMarker
open class Command(open val name: String,  var expectedArgs: Array<out CommandArgument> = arrayOf(),
              var execute: (CommandEvent) -> Unit = {}, var requiresGuild: Boolean = false) {

    operator fun invoke(args: Command.() -> Unit) {}

    val parameterCount: Int
        get() = this.expectedArgs.size

    fun requiresGuild(requiresGuild: Boolean) {
        this.requiresGuild = requiresGuild
    }

    fun execute(execute: (CommandEvent) -> Unit) {
        this.execute = execute
    }

    fun expect(vararg args: CommandArgument) {
        this.expectedArgs = args
    }

    fun expect(vararg args: ArgumentType) {
        val clone = Array(args.size) { arg(ArgumentType.Word) }

        for (x in args.indices) {
            clone[x] = arg(args[x])
        }

        this.expectedArgs = clone
    }

    fun expect(args: Command.() -> Array<out CommandArgument>) {
        this.expectedArgs = args()
    }
}

data class CommandArgument(val type: ArgumentType, val optional: Boolean = false, val defaultValue: Any = "") {
    override fun equals(other: Any?): Boolean {
        if(other == null) return false

        if(other !is CommandArgument) return false

        return other.type == this.type
    }
}

@CommandTagMarker
open class CommandsContainer(var log: BotLogger, open var commands: HashMap<String, Command> = HashMap()) {
    operator fun invoke(args: CommandsContainer.() -> Unit) {}

    fun listCommands() = this.commands.keys.toList()

    fun command(name: String, construct: Command.() -> Unit = {}): Command? {
        val command = Command(name)
        command.construct()
        this.commands.put(name, command)
        return command
    }

    fun join(vararg cmds: CommandsContainer): CommandsContainer {
        cmds.forEach {
            this.commands.putAll(it.commands)
        }

        return this
    }

    fun has(name: String) = this.commands.containsKey(name)

    operator fun get(name: String) = this.commands.get(name)

    fun newLogger(log: BotLogger) {
        this.log = log
    }
}

fun produceContainer(logger: BotLogger): CommandsContainer {
    val pack = "me.aberrantfox.hotbot.commandframework.commands"
    val cmds = Reflections(pack, MethodAnnotationsScanner()).getMethodsAnnotatedWith(CommandSet::class.java)

    val container = cmds.map { it.invoke(null) }
            .map { it as CommandsContainer }
            .reduce { a, b -> a.join(b) }

    val lowMap = HashMap<String, Command>()

    container.commands.keys.forEach {
        lowMap.put(it.toLowerCase(), container.commands[it]!!)
    }

    container.commands = lowMap

    container.log = logger

    return container
}
@DslMarker
annotation class CommandTagMarker

fun commands(construct: CommandsContainer.() -> Unit): CommandsContainer {
    val commands = CommandsContainer(DefaultLogger())
    commands.construct()
    return commands
}

fun arg(type: ArgumentType, optional: Boolean = false, default: Any = "") = CommandArgument(type, optional, default)

fun arg(type: ArgumentType, optional: Boolean = false, default: (CommandEvent) -> Any) = CommandArgument(type, optional, default)
