# ECO 高速规划器 (ECO Fast Planner)

一个面向 **Minecraft 1.20.1 / Forge** 的 [Applied Energistics 2](https://github.com/AppliedEnergistics/Applied-Energistics-2) 附属模组，
在 ME 网络中提供两个方块：

- **高速规划器**：接管 AE2 原版合成计划计算，提供快速规划与环计算（自增殖配方求解 + 环图）。
- **CPU 增强调度器**：把指定的合成 CPU 接入增强执行器（自环配方产出回用、自环计划可执行）。

灵感来自 [NeoECOAE](https://github.com/DancingSnow0517/NeoECOAEExtension) 的规划器与环计算设计。

> 目标版本：Forge 47.4.16（1.20.1）· AE2 15.4.10+（15.4.10 / 15.4.11 均兼容）· Java 17
>
> 注意：Forge **47.2.0** 自带的 Mixin 版本过旧，AE2 15.4.x（其 Mixin 配置为
> `JAVA_11` 兼容级别）在开发环境（official 映射）下会加载失败；请使用 **47.4.x**。

## 功能

### 高速规划器方块

- 合成后放入 **ME 网络**（任意方向贴线缆即可），右键打开配置界面。
- 界面中有两个开关：
  - **快速规划**：开启后，本网络上的 AE2 合成计划计算不再受 AE2 每 tick 5 微秒的
    `TickHandler` 预算限制，直接在 AE2 的合成线程池内**全速计算完成**；计划的
    `patternTimes / usedItems / emittedItems / missingItems / bytes` 与官方计算完全一致，
    只是不再被拉长到很多 tick。
  - **环计算**：开启后，在每次计算计划前分析目标物品的**配方依赖图**，用 Tarjan 强连通
    分量（SCC）找出循环依赖，并用有界迭代求解器估算「环需要的外部种子材料」，结果通过
    界面下半部分的**环图**展示（节点=资源，边=配方依赖，颜色区分 SCC）。

### CPU 增强调度器方块

- 放入 ME 网络后右键打开界面，会**列出网络中的所有合成 CPU**（名称 / 运行状态 / 协处理器数）。
- 把某个 CPU「添加」后，该 CPU 由本模组的**增强执行器**接管（其余 CPU 保持原版行为）：
  - **自环配方产出回用**：CPU 产出与输入相同的自增殖配方（如锻造模板复制）时，
    执行器会把 1 份产出回收到 CPU 内部库存，供下一次执行使用，剩下的照常输出到网络；
  - 配合高速规划器的「环计算」，模板复制这类循环配方可以持续执行，而不再因为
    「输入一次性就位」的要求卡死。
- 「移除」即恢复该 CPU 的原版行为。

### 行为安全性

- 两个开关都关闭、或方块不在网络中、或网络未加载时，行为与安装前**完全一致**。
- 环计算分析失败不会影响 AE2 原版规划（catch 掉并记日志后回退）。
- 快速规划只改变**计算节奏**，不改变计划内容，因此不会打破材料守恒或任务一致性。
- 与 NeoECOAE 无硬依赖；当 NeoECOAE 的合成系统接管计划时，本模组方块不在其时序路径上，
  不会冲突（软兼容）。

## 构建

```bash
# 需要 JDK 17 与网络（首次会下载 ForgeGradle / Minecraft 依赖）
./gradlew build
```

产物：`build/libs/ecocalcer-0.2.0.jar`。

构建要求：

- **JDK 17**（`java.toolchain.languageVersion = 17`；Gradle 会在已安装的 JDK 中寻找，
  或允许它自动下载）
- Gradle Wrapper 8.1.1（仓库自带，与 ForgeGradle 6 `[6.0,6.2)` 匹配）
- 首次构建需要访问 `maven.minecraftforge.net` 与 Mojang 的库服务器

### 关于中文路径 / 受限环境

ForgeGradle 与 Gradle 的构建过程对工作区路径较敏感。在少数环境（例如受限的
Windows 沙箱、某些带安全策略的专用机器）下，若项目路径包含非 ASCII 字符，可能遇到：

- 访问转换器报 `ReadOnlyFileSystemException`：这是 JVM 的 `Files.isWritable()` 在受限
  令牌下误报导致 ForgeGradle 把 `jar:` 文件系统当成只读；
- Gradle Worker 报 `ClassNotFoundException: ...GradleWorkerMain`：这是 worker 参数文件
  在非 ASCII 路径下的编码问题。

这两类问题**不属于本模组代码**，正常桌面环境不会出现。若遇到，把项目放在纯英文路径下
（例如 `D:\projects\ecocalcer`）再构建即可。仓库内也提供了本机验证用的
`scripts/build.sh`（JDK 21 运行 Gradle 8.5 + JDK 17 toolchain）。

### 验证记录

- `./gradlew build` 通过（Forge 47.4.10，AE2 **15.4.10** + GuideME 20.1.14）。
- 产物 `ecocalcer-0.1.0.jar` 已在 **正式 Forge 47.4.10 / 47.4.16 专用服务器**（非开发环境，SRG 映射）
  上实测启动：模组正常初始化、方块/物品/菜单注册无异常、两个 Mixin 注入点全部匹配
  （`require = 1`，不匹配会直接崩溃）。
- **GTL十周目 同款环境**（Forge 47.4.16 + AE2 15.4.10 + GuideME 20.1.14）隔离验证通过：
  服务器 `Done`，模组正常初始化。
- 注：开发环境的 `runServer` 受 ForgeGradle/Mixin 的角色与映射影响，AE2 的 Mixin 在
  `official` 映射下需要 refmap 重映射才能运行，这属于开发环境工具链问题，不影响正式环境。

### 安装到现有整合包（例如 GTL十周目）

1. 把 `build/libs/ecocalcer-0.1.0.jar` 复制到整合包的 `mods` 目录；
2. 确认整合包中 AE2 **≥ 15.4.10**（GTL十周目 当前为 15.4.10，满足；GuideME 20.1.14 满足）；
3. 卸载时删除该 jar 即可。

本模组只新增方块与物品，**不修改存档数据**。已在隔离环境验证，未改动 GTL 整合包本身。

### GTLCore 兼容（重要）

GTL 整合包中的 **GTLCore** 用 `@Overwrite` 重写了 `CraftingCalculation.run()` 与
`simulateFor()`。因此本模组**不再注入 `run()`**（早期版本注入 `run()` 会导致该 Mixin
整类 FATAL 失败，进而让 `CraftingCalculation` 类加载失败、所有合成计划卡死）。
本模组只注入 `handlePausing()` 作为**没有 GTLCore 环境**下的兜底；GTLCore 自身在非自适应
模式下已移除每 tick 节流，计划计算速度由 GTLCore 保证。

### 本地验证脚本

- `scripts/build.sh`：使用本机 JDK 21 + Gradle 8.5（JDK 17 作为 toolchain）直接构建；
- `scripts/sync-and-build.sh`：把源码同步到纯英文路径后再构建，用于绕开本沙箱
  中文路径下的 JVM/ForgeGradle 限制。

### 离线 / 内网构建

仓库自带：

- `libs/appliedenergistics2-forge-15.4.11.jar`（AE2 15.4.11，Forge 1.20.1）
- `maven-repo/appeng/appliedenergistics2/15.4.11/`（本地 Maven 镜像，`build.gradle` 已引用）

因此构建时**不需要**访问 CurseForge / Modrinth 获取 AE2。ForgeGradle 本身仍需访问
`maven.minecraftforge.net` 与 Mojang 的库服务器。

### 运行时依赖

需要同时安装：

- **AE2 15.4.11+ / 15.4.x**（`mods.toml` 中声明的硬依赖）
- **GuideME 20.1.7 ~ 20.1.x**（AE2 15.4.11 的硬依赖，Modrinth 可下载）
- 客户端与服务端都需要安装本模组

## 用法

1. 合成「高速规划器」：

   ```
   石英玻璃   计算处理器   石英玻璃
   石英玻璃   能量元件     石英玻璃
   石英玻璃   计算处理器   石英玻璃
   ```

2. 放入 ME 网络，右键打开界面并打开所需开关。
3. 正常在 AE2 终端里发起合成：计划计算将被快速规划接管；开启环计算后，界面下方会出现
   最近一次规划对应的依赖环图。

## 实现说明

| 部分 | 位置 | 说明 |
| --- | --- | --- |
| 方块 / 方块实体 | `com.ecocalcer.block`, `com.ecocalcer.blockentity` | 基于 AE2 `AEBaseEntityBlock` / `AENetworkBlockEntity`，节点不占用频道 |
| 配置界面 | `com.ecocalcer.client`, `com.ecocalcer.menu` | 原版 `Screen` + `AbstractContainerMenu`，`ContainerData` 同步开关与统计 |
| 计划接管 | `com.ecocalcer.mixin.CraftingServiceMixin` | `@Inject` 到 `CraftingService#beginCraftingCalculation`，开启快速规划时提交 `FastCraftingCalculation` |
| 取消节流 | `appeng.crafting.FastCraftingCalculation` | 重写包私有的 `handlePausing()`，跳过 TickHandler 的每 tick 预算 |
| 防止主线程阻塞 | `com.ecocalcer.mixin.CraftingCalculationMixin` | 快速计算**不注册**到 `TickHandler`，避免 `simulateFor()` 等待永不让出的计算线程 |
| 环计算 | `com.ecocalcer.planner.cycle` | 依赖图 BFS（有界：384 节点 / 8 层）、Kosaraju SCC、有界种子求解 |
| 环图同步 | `com.ecocalcer.net` | `SimpleChannel` 把环图快照发给打开界面的玩家 |

## 已知限制

- 快速规划会占用 AE2 合成线程池中的一条线程直到算完；超大计划（数十万次合成）时该线程
  可能持续较长时间。这只是线程池占用，不会阻塞服务器主线程。
- 环计算目前用于**分析、展示与种子估算**；真正的计划仍由 AE2 的计划器产出（快速规划加速）。
- 环图最多绘制 140 个节点 / 320 条边，超出部分不绘制（分析本身也截断在 384 节点以内）。
- 环计算依赖 AE2 的 `ICraftingService` 与网络库存缓存，不感知 `CraftingUnit` 的并行限制。

## 许可证

GPL-3.0-only（与 NeoECOAE 生态保持一致；本项目未复制其代码，仅参考其功能设计）。

AE2 本身为 LGPL-3.0，`libs/` 中的 AE2 jar 仅作为编译与本地构建依赖，不随本模组 jar 分发。
