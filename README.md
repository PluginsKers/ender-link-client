![Ender Link](artwork/banner.png)

# Ender Link

Open your ender chest from a button beside the inventory's recipe book. No ender chest item is needed.

Requires the [Ender Link server plugin](https://github.com/PluginsKers/enderlink). The server owns the inventory and controls access. Without the plugin, the button stays disabled. Players without this mod can use `/enderlink`.

The button matches the vanilla recipe book. It is disabled while carrying an item and hidden when the recipe book covers the inventory in narrow windows.

## Installation

Requires Minecraft 26.3, Fabric Loader 0.19.5 or newer, Fabric API, and Java 25.
Place the mod jar in your client's `mods` folder.

## Building

```sh
./gradlew build compileGametestJava
./gradlew runClientGameTest
```

Use `gradlew.bat` on Windows. Builds require JDK 25; game tests also require a display. Jars are written to `build/libs`.
GitHub Actions builds pull requests and `main`; jars are available as workflow artifacts.

## License

[MIT](LICENSE).
