# AccessDenier

A simple Velocity plugin that restricts access to proxy servers using permissions.

## Features

- Denies server connections when a player lacks permission
- Replaces Velocity’s `/server` command
- Only shows accessible servers in `/server`
- Supports tab completion for permitted servers
- Disconnects players if they try to join a server they cannot access on initial connect

## Requirements

- Java 17+
- Velocity `3.4.0-SNAPSHOT` or compatible
- A permissions plugin

## Permissions

Access to each server is controlled with:

`server.access.<server-name>`

### Examples
```txt
server.access.lobby
server.access.survival
server.access.minigames
```

## Installation

1. Download or build the plugin jar
2. Place it in your Velocity `plugins/` folder
3. Restart your proxy
4. Assign the appropriate permissions to players

## Building

Clone the repository and run:

`mvn clean package`

The compiled jar will be located in:

`target/`

## Usage
Players can run `/server` to see a list of servers they have access to.


To connect to a server:

`/server <server-name>`

If a player tries to join a server without permission, the connection will be denied.


Players also need access to the server command unless it is not explicitly denied:

`velocity.command.server`

## License
This project is licensed under the GNU General Public License v3.0.

Parts of this project are based on Velocity’s original server command code,
which is also licensed under the GNU General Public License v3.0.
