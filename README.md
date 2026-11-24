# 模块化魔法 · Modular Magic

**Minecraft 1.21.1 · NeoForge · Java 21 · dev2**

两桶水为什么能生出无尽的水？隔着末影珍珠，你第一次看见以太沿着某种纹样流动。理解这些纹样，将它们刻成符文，再把符文组合成自己的法术。

模块化魔法是一款以自然研究、以太工程和法术装配为核心的模组。进入游戏后，可以查阅帕秋莉手册《装配指南》，从观察自然开始。

## 主要玩法

- **观察与研究**。用虚空透镜观察世界，在符文研究台解读笔记，再用符文刻印台制作节点宝石。探索遗迹也能找到知识碎片和成型法术核心。知识碎片可以随机解锁一项未知知识，或合成为晶化以太碎片。
- **以太生产与供能**。使用以太发生器从水流、岩石、作物等来源回收以太，用储槽缓存，通过定向射流或紫水晶共振网络传输。个人供能锚可给随身容器补充以太。
- **自定义法术**。在法术编写台组合触发条件、范围、动作和弹体等节点，编辑参数并校验结构。法术核心支持瞬发、蓄力和持续施放。生存施法会消耗储存的以太与可恢复的专注。
- **以太样板与工具**。用实物制作基础样板，在手持以太合成台衍生新样板。简易施法单元可调用样板执行放置、进食、发射、燃烧或工具操作。以太凝形器可将样板实体化。
- **示例法术**。「模块化魔法：示例法术」创造栏提供 70 枚可编辑的成型核心。从中挑选的 15 种法术分布在三档野外宝藏中，也可以从零装配自己的法术。

## 安装

使用 **Minecraft 1.21.1** 和 **Java 21**。本版使用 **NeoForge 21.1.78** 构建和验证。客户端和服务端各自在 `mods` 目录安装下列两个版本之一。

| 文件 | 安装方式 |
| --- | --- |
| `modularmagic-0.1.0-dev2.jar` | 普通版。另行安装 L2Magic 及其运行库、Patchouli 和 Jade。 |
| `modularmagic-0.1.0-dev2-bundled.jar` | Jar-in-Jar 版。内含必需模组前置和运行库，NeoForge 会自动加载。 |

<details>
<summary>普通版的已验证前置版本</summary>

| 前置 | 版本 |
| --- | --- |
| [L2Magic](https://github.com/Minecraft-LightLand/L2Magic) | `3.0.6` |
| [Patchouli](https://github.com/VazkiiMods/Patchouli) | `1.21.1-92-NEOFORGE` |
| [Jade](https://github.com/Snownee/Jade) | `15.10.6+neoforge` |
| [Registrate](https://github.com/tterrag1098/Registrate) | `MC1.21-1.3.0+50` |

L2Magic 的配套运行库为 `l2core 3.0.8+8`、`l2serial 3.0.9`、`l2modularblocks 3.0.0+4`、`l2tabs 3.0.5+1`、`l2menustacker 3.0.9`、`l2itemselector 3.0.8`、`l2library 3.0.4`、`l2damagetracker 3.0.6` 和 `fast_projectile_api 3.1.0`。已由所选 L2Magic 发行包提供的运行库只需安装一次。

</details>

JEI 和 Curios 可按需安装。微光纪事的示例法术已移植，游玩时无需安装微光纪事。

## 第一个法术

1. 在法术编写台放入「法术核心：瞬发」。
2. 装入「自身位置」与「声音」节点。也可以先打开任意示例核心，查看它的结构。
3. 选择节点查看参数，点击「应用参数」，然后取出成型核心。
4. 潜行右键打开初学者法杖的核心槽。装入核心后，右键施放。

## 从源码构建

使用 JDK 21 和仓库附带的 Gradle Wrapper。产物位于 `build/libs/`。

```powershell
.\gradlew.bat jar jarJar
.\gradlew.bat test runGameTestServer
```

在 Linux 或 macOS 上使用 `./gradlew`。本地引擎库和版本记录见 [libs/README.md](libs/README.md)。

## 许可证与致谢

本模组以 [LGPL-2.1](LICENSE) 开源。示例法术使用了来自 [L2Magic](https://github.com/Minecraft-LightLand/L2Magic) 和 [微光纪事](https://github.com/Minecraft-LightLand/GlimmeringTales/tree/18b24281d507bdd6a1d40efebbddf8fa0d23994c) 的法术，感谢 lcy0x1 和两项目的贡献者。

Jar-in-Jar 版内的第三方 JAR 按各自的许可证分发。Patchouli 由 VazkiiMods 开发，采用 [CC BY-NC-SA 3.0](https://creativecommons.org/licenses/by-nc-sa/3.0/)。Jade 由 Snownee 开发，采用 [CC BY-NC-SA 4.0](https://creativecommons.org/licenses/by-nc-sa/4.0/)。
