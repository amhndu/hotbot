package me.aberrantfox.hotbot.listeners

import com.google.common.eventbus.Subscribe
import me.aberrantfox.hotbot.commands.permissions.RankContainer
import me.aberrantfox.hotbot.services.Configuration
import net.dv8tion.jda.core.events.role.update.RoleUpdateNameEvent

class RoleListener(val configuration: Configuration) {
    @Subscribe
    fun onRoleUpdateName(event: RoleUpdateNameEvent) {
        val oldName = event.oldName
        val newName = event.role.name

        if (RankContainer.canUse(oldName)) {
            RankContainer.remove(oldName)
            RankContainer.add(newName)
        }
    }
}