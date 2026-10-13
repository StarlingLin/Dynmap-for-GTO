# Dynmap for GTO

给 GTO 用的 Dynmap，其他 Forge 1.20.1 大概也能用。
自带 BlockScan，能渲染模组方块。

已为部分动态模型增加服务端适配，多方块成型与连接纹理尚未完成，具体范围见下方。

## 构建

需要 JDK 17，首次构建需要联网下载 Gradle、Forge 和依赖。

```powershell
.\build.ps1
```

输出：`build/libs/Dynmap-for-GTO-0.1.1-forge-1.20.1.jar`

## 使用

1. 停止服务器。
2. 将 jar 放入 mods 文件夹。
3. 启动并等待扫描完成，第一次可能需要数分钟。
4. 和原模组一样可以修改配置文件并重启服务器，见[原模组的百科](https://github.com/webbukkit/dynmap/wiki)。比方说可以设置默认世界默认缩放和默认起始坐标。
5. 在想扫描的世界执行 `/dynmap fullrender`，可能要几个小时。

## 修改

- 修复 Dynmap 面片 UV 校验将 U 边界误用为 V 边界。
- 修复 Dynmap 与 BlockScan 读取 Forge Jar-in-Jar 模组资源时把虚拟 Path 转成普通 File 的问题。
- GTCEu：框架适配，机器适配，输入/输出仓适配。
- GTMThings：无线能源监视器适配。
- GTOCore：大型蒸汽太阳能锅炉适配。
- ExtendedAE：扩展驱动器适配。
- AE2 与 GT 线缆适配

原项目与许可证见 `NOTICE`、`LICENSE`。
