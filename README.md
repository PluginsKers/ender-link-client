![Ender Link](assets/banner.png)

# Ender Link

Open your ender chest from a button beside the inventory's recipe book. No ender chest item is needed.

[Watch the Ender Link demo on YouTube](https://youtu.be/auczq3Sn7IQ).

![The ender chest button beside the recipe book, with its tooltip visible](assets/screenshot.png)

Requires the [Ender Link server plugin](https://github.com/PluginsKers/enderlink). The server owns the inventory and controls access. Without the plugin, the button stays disabled. Players without this mod can use `/enderlink`.

The button matches the vanilla recipe book. It is disabled while carrying an item and hidden when the recipe book covers the inventory in narrow windows.

## Installation

Supports Minecraft 1.21.11, 26.1, 26.1.1, 26.1.2, 26.2, and 26.3. Requires Fabric Loader 0.19.5 or newer and Fabric API.
Download the jar matching your Minecraft version from [Releases](https://github.com/PluginsKers/ender-link-client/releases) and place it in your client's `mods` folder. Use Java 21 for 1.21.11 and Java 25 for 26.x.

## Building

```sh
./gradlew build compileGametestJava
./gradlew :26.3:runClientGameTest
```

Use `gradlew.bat` on Windows. Run Gradle with JDK 25; the Java 21 toolchain is downloaded when needed. Game tests require a display. `build` builds all versions; `:1.21.11:build` builds one. Jars are written to `versions/<minecraft>/build/libs`.
GitHub Actions checks each supported version. Version tags publish the jars to GitHub Releases.

The 26.3 client test can also exercise a separate server running the ordinary Ender Link plugin:

```sh
./gradlew :26.3:runClientGameTest '-PenderlinkTestAddress=127.0.0.1:25763'
```

Only an explicit loopback port is accepted. Use a dedicated disposable server or an SSH forward to one, never a production server or an authenticated proxy backend. The test uses the `EnderLinkTest` player and does not supply credentials. Leave ender chest slot 0 empty or containing the matching fixture chestplate, with room in the player inventory. Once `ENDER_LINK_FIXTURE_READY` appears, use the server's `/give` command within 90 seconds to give that player exactly one enchanted diamond chestplate named `Ecosystem 26.3` with damage 7. An existing matching chestplate in the inventory or ender chest slot 0 is reused. The client deposits it in ender chest slot 0 through the real menu, then verifies withdrawal, reconnecting, and returning it to that slot. Every menu slot is compared with Minecraft's item codec, including counts and client-visible components; starter items stay unchanged. Server-private components require a separate server-side check. A failed test leaves the fixture for inspection; it does not fabricate replacement items. Existing tests remain the default when the address is omitted.

## License

[MIT](LICENSE).

The logo and banner use Minecraft artwork by Mojang Studios.
