# Ender Link Client

Minecraft 26.3 的 Fabric 末影箱按钮客户端。需要 Fabric Loader 0.19.5、Fabric API 和 Java 25。

## 使用

将 `ender-link-client-<版本>.jar` 放入客户端 `mods` 目录。

末影箱按钮位于背包的配方书按钮左侧，使用原版配方书的边框、底色和悬停高亮。点击打开自己的末影箱，无需携带末影箱物品。
鼠标拿着物品时按钮禁用；窄窗口中配方书遮住背包时隐藏按钮。
服务器未提供开箱功能时，按钮禁用，悬停提示服务器尚未启用 Ender Link。

服务端插件与通信协议由 [Ender Link](https://gitea.i5res.com/eyreland/enderlink) 维护，末影箱内容和访问权限由服务端管理。

## 构建与测试

```sh
./gradlew build compileGametestJava
./gradlew runClientGameTest
```

版本在 `gradle.properties` 中维护，可用 `-Pversion=0.1.2+mc26.3` 覆盖。成品位于 `build/libs/`，运行游戏使用不带 `-sources` 的 jar。
PR 与 `main` 推送自动构建，jar 可从 GitHub Actions 附件下载。

游戏测试需要图形环境，覆盖服务器能力识别、按钮位置与悬停、配方书切换、窄窗口、光标持物保护、无物品开箱和切服状态清理。
截图位于 `build/run/clientGameTest/screenshots/`。
