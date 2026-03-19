package eu.tosthedev.accessDenier;

import static net.kyori.adventure.text.event.HoverEvent.showText;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.tree.LiteralCommandNode;
import com.velocitypowered.api.command.BrigadierCommand;
import com.velocitypowered.api.command.CommandSource;
import com.velocitypowered.api.permission.Tristate;
import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.ProxyServer;
import com.velocitypowered.api.proxy.ServerConnection;
import com.velocitypowered.api.proxy.server.RegisteredServer;
import com.velocitypowered.api.proxy.server.ServerInfo;

import java.util.*;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.format.NamedTextColor;

public class ServerCommand {

    private static final String SERVER_ARG = "server";
    public static final int MAX_SERVERS_TO_LIST = 50;

    @SuppressWarnings("checkstyle:MissingJavadocMethod")
    public static BrigadierCommand create(final ProxyServer server) {
        final LiteralCommandNode<CommandSource> node = BrigadierCommand
            .literalArgumentBuilder("server")
            .requires(src -> src instanceof Player
                    && src.getPermissionValue("velocity.command.server") != Tristate.FALSE)
            .executes(ctx -> {
                final Player player = (Player) ctx.getSource();
                outputServerInformation(player, server);
                return Command.SINGLE_SUCCESS;
            })
            .then(BrigadierCommand.requiredArgumentBuilder(SERVER_ARG, StringArgumentType.word())
                .suggests((ctx, builder) -> {
                    final Player player = (Player) ctx.getSource();
                    final String input = builder.getRemaining().toLowerCase(Locale.ROOT);

                    for (final RegisteredServer sv : server.getAllServers()) {
                        final String serverName = sv.getServerInfo().getName();
                        final String normalized = serverName.toLowerCase(Locale.ROOT);

                        if (player.hasPermission("server.access." + normalized)
                                && normalized.startsWith(input)) {
                            builder.suggest(serverName);
                        }
                    }

                    return builder.buildFuture();
                })
                .executes(ctx -> {
                    final Player player = (Player) ctx.getSource();
                    // Trying to connect to a server.
                    final String serverName = StringArgumentType.getString(ctx, SERVER_ARG);
                    final String normalized = serverName.toLowerCase(Locale.ROOT);

                    if (!player.hasPermission("server.access." + normalized)) {
                        player.sendMessage(Component.text("You do not have access to that server."));
                        return 0;
                    }

                    final Optional<RegisteredServer> toConnect = server.getServer(serverName);
                    if (toConnect.isEmpty()) {
                        player.sendMessage(Component.text("The specified server " + serverName+ " does not exist"));
                        return -1;
                    }

                    player.createConnectionRequest(toConnect.get()).fireAndForget();
                    return Command.SINGLE_SUCCESS;
                })
            ).build();

        return new BrigadierCommand(node);
    }

    private static void outputServerInformation(final Player executor,
                                                final ProxyServer server) {
        final String currentServer = executor.getCurrentServer()
            .map(ServerConnection::getServerInfo)
            .map(ServerInfo::getName)
            .orElse("<unknown>");

        executor.sendMessage(Component.text("You are currently connected to " + currentServer,
            NamedTextColor.YELLOW));

        final List<RegisteredServer> servers = sortedServerList(server);
        if (servers.size() > MAX_SERVERS_TO_LIST) {
            executor.sendMessage(Component.text(
        "There are too many servers set up. Use tab completion to view all servers available.", NamedTextColor.RED));
            return;
        }

        List<RegisteredServer> allowedServers = servers.stream()
            .filter(rs -> {
                String name = rs.getServerInfo().getName().toLowerCase(Locale.ROOT);
                return executor.hasPermission("server.access." + name);
            })
            .toList();

        // Assemble the list of servers as components
        final TextComponent.Builder serverListBuilder = Component.text()
            .append(Component.text("Available servers:",
                NamedTextColor.YELLOW))
            .appendSpace();

        if (allowedServers.isEmpty()) {
            serverListBuilder.append(Component.text("You do not have access to any servers.", NamedTextColor.RED));
        }
        else {
            for (int i = 0; i < allowedServers.size(); i++) {
                final RegisteredServer rs = allowedServers.get(i);
                serverListBuilder.append(formatServerComponent(currentServer, rs));
                if (i != allowedServers.size() - 1) {
                    serverListBuilder.append(Component.text(", ", NamedTextColor.GRAY));
                }
            }
        }

        executor.sendMessage(serverListBuilder.build());
    }

    private static TextComponent formatServerComponent(final String currentPlayerServer,
                                                       final RegisteredServer server) {
        final ServerInfo serverInfo = server.getServerInfo();
        final TextComponent.Builder serverTextComponent = Component.text()
            .content(serverInfo.getName());

        final int connectedPlayers = server.getPlayersConnected().size();
        final TextComponent.Builder playersTextComponent = Component.text();
        if (connectedPlayers == 1) {
            playersTextComponent.content(connectedPlayers + " player online");
        } else {
            playersTextComponent.content(connectedPlayers + " players online");
        }

        if (serverInfo.getName().equals(currentPlayerServer)) {
            serverTextComponent.color(NamedTextColor.GREEN)
                .hoverEvent(
                    showText(
                        Component.text("Currently connected to this server")
                            .append(Component.newline())
                            .append(playersTextComponent))
                );
        } else {
            serverTextComponent.color(NamedTextColor.GRAY)
                .clickEvent(ClickEvent.runCommand("/server " + serverInfo.getName()))
                .hoverEvent(
                    showText(
                        Component.text("Click to connect to this server")
                            .append(Component.newline())
                            .append(playersTextComponent))
                );
        }
        return serverTextComponent.build();
    }

    private static List<RegisteredServer> sortedServerList(ProxyServer proxy) {
        List<RegisteredServer> servers = new ArrayList<>(proxy.getAllServers());
        servers.sort(Comparator.comparing(RegisteredServer::getServerInfo));
        return Collections.unmodifiableList(servers);
    }
}
