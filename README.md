# HCCore Web API ![IMG](https://hackatime-badge.hackclub.com/U08RJ1PEM7X/HCCoreAPI)
A Minecraft (PaperMC) plugin that adds a powerful REST-based web API! \
This API allows programmatic access to certain data from [HCCore](https://github.com/hackclub/HCCore).

<a href="https://notbyai.fyi" target="_blank">
  <img src="not-by-ai.svg" alt="Developed by a human, not by AI!">
</a>

## Features
- Multiple REST API routes with proper headers
- Powerful API key system
- Rate limit system per API key
- Powerful in-game management command
- Detailed logging for all API requests made
- Full [documentation](https://api.mc.hackclub.com) for API routes

## Setup/Installation
The plugin works on any Minecraft: Java Edition server running PaperMC on Minecraft 1.21.11+ (with the instructions below). 
1. Download the latest [HCCore release](https://github.com/hackclub/HCCore/releases) and its dependencies ([ProtocolLib](https://www.spigotmc.org/resources/protocollib.1997/) and [UltimateAdvancementAPI](https://www.spigotmc.org/resources/ultimateadvancementapi-1-15-26-2.95585/)).
2. Download the latest [HCCoreAPI release](https://github.com/aelithron/HCCoreAPI/releases) (this plugin).
3. Put all four of these `.jar` files in your server's `plugins/` folder, then restart your server.
4. Run the following command: `/webapi keys add example`
5. Start making API calls with the key you get back! You can use it in calls on our [Swagger docs](https://api.mc.hackclub.com) or in your own code.

## Rules
There are rules to using the API, at least my official instance! See here:
- Try to stay within your rate limit, don't make more requests than you need to.
- Keep your API key confidential, and notify admins immediately if your key is leaked.
- Don't use this API key for anything other than what you said you would when you requested it.
- Stay within the Hack Club [Code of Conduct](https://hackclub.com/conduct) with your usage.

## Demo
Want to evaluate the API in production, on the [#minecraft](https://hackclub.enterprise.slack.com/archives/CD1JSG9UK) server? First, you need an API key. You can DM me on Slack for one if you don't have one.
Then, go to https://api.mc.hackclub.com in a web browser. Click Authorize and enter your key there, then save and close. \
Go to the `GET /player` route, and click "Try it out". You can then use **one** of the following parameters:
- UUID: `eb7ea62d-b7aa-4d6e-b68a-d7e948780f03`
- Slack ID: `U08RJ1PEM7X`
- HCCore Nickname: `Nova`

Note that this uses my information, you can feel free to use your own if you have played on the server before.
### In-game Command
If you want to test out the in-game command, I suggest you run your own server with the plugin using [these instructions](https://github.com/aelithron/HCCoreAPI/blob/main/README.md#setupinstallation). I have attached a video of it [here](https://user-cdn.hackclub-assets.com/019ffa01-336a-7558-88aa-b924c2473eae/Screencast_20260813_011625.webm) though! :3