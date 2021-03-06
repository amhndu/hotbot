package me.aberrantfox.hotbot.listeners

import com.google.common.eventbus.Subscribe
import me.aberrantfox.hotbot.database.markLastRecordAsBan
import net.dv8tion.jda.core.events.guild.GuildBanEvent
import java.util.Timer
import kotlin.concurrent.schedule

class BanListener() {
    @Subscribe
    fun onGuildBan(event: GuildBanEvent) {
        Timer().schedule(5 * 1000) {
            markLastRecordAsBan(event.user.id, event.guild.id)
        }
    }
}