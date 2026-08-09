# HCCore Web API ![IMG](https://hackatime-badge.hackclub.com/U08RJ1PEM7X/HCCoreAPI)
A Minecraft (PaperMC) plugin that adds powerful a REST-based web API! \
This API allows programmatic access to certain data from [HCCore](https://github.com/hackclub/HCCore).

<a href="https://notbyai.fyi" target="_blank">
  <img src="not-by-ai.svg" alt="Developed by a human, not by AI!">
</a>

## Features
- Multiple REST API routes with proper headers
- Powerful API key system
- Rate limit system per API key
- Powerful in-game management command
- More coming soon!

## Setup/Installation
1. Download the latest [HCCore release](https://github.com/hackclub/HCCore/releases) and its dependencies ([ProtocolLib](https://www.spigotmc.org/resources/protocollib.1997/) and [UltimateAdvancementAPI](https://www.spigotmc.org/resources/ultimateadvancementapi-1-15-26-2.95585/)).
2. Download the latest [HCCoreAPI release](https://github.com/aelithron/HCCoreAPI/releases) (this plugin).
3. Put all four of these `.jar` files in your server's `plugins/` folder, then restart your server.
4. Run the following command (coming soon): `/webapi keys add example`
5. Start making API calls with the key you get back!