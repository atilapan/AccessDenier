package eu.tosthedev.accessDenier;

import com.google.inject.Inject;
import com.velocitypowered.api.command.BrigadierCommand;
import com.velocitypowered.api.command.CommandManager;
import com.velocitypowered.api.command.CommandMeta;
import com.velocitypowered.api.event.player.ServerPreConnectEvent;
import com.velocitypowered.api.event.proxy.ProxyInitializeEvent;
import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.plugin.Plugin;
import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.ProxyServer;
import com.velocitypowered.api.proxy.server.RegisteredServer;
import net.kyori.adventure.text.Component;
import org.slf4j.Logger;

@Plugin(id = "accessdenier", name = "AccessDenier", version = "1.0.3", authors = {"ToS"})
public class AccessDenier {

    @Inject
    private Logger logger;

    @Inject
    private ProxyServer proxy;

    @Subscribe
    public void onProxyInitialization(ProxyInitializeEvent event) {
        logger.info("AccessDenier started!");

        CommandManager manager = proxy.getCommandManager();

        manager.unregister("server");

        BrigadierCommand newServerCommand = ServerCommand.create(proxy);
        CommandMeta newServerCommandMeta = manager.metaBuilder("server").build();
        manager.register(newServerCommandMeta, newServerCommand);
    }

    @Subscribe
    public void onServerPreConnectEvent(ServerPreConnectEvent event) {
        Player player = event.getPlayer();
        RegisteredServer server = event.getOriginalServer();
        String serverName = server.getServerInfo().getName();
        RegisteredServer oldServer = event.getPreviousServer();

        if (!player.hasPermission("server.access." + serverName)) {
            event.setResult(ServerPreConnectEvent.ServerResult.denied());

            if(oldServer == null)
            {
                player.disconnect(Component.text("You are not allowed to access server " + serverName));
            }
        }
        else {
            event.setResult(ServerPreConnectEvent.ServerResult.allowed(server));
        }
    }
}
