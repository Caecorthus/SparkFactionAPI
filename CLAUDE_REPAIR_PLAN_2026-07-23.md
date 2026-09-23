# Spark Traits / FactionAPI / Witch / Assist / Strength 修复计划

日期：2026-07-23
执行者：Claude
计划编写者：Codex 主 agent

> 这是一份执行指令，不是实现记录。Codex 本轮只完成远端同步审计、源码诊断和计划编写，没有实施下列玩法修复，也没有 commit 或 push。

## 一、Claude 的总目标

在最新远端基线上修复以下九项问题，同时证明未影响范围外的角色、阵营和词条：

1. `sparkwitch:curser`（诅咒者/源码名 Curser）没有可见的主动技能。
2. `sparkwitch:curser` 使用了类似杀手的回退本能，而不是魔女阵营本能。
3. `sparkwitch:saboteur`（破坏者）没有原生杀手阵营本能。
4. `sparktraits:niko` 与 `sparktraits:heavy_artillery` 不能一起触发。
5. 部分玩家随到大魔女后看不见魔力。
6. 终局亡命徒与其他杀手共存时，倒计时到 `0s` 后不结束。
7. 部分玩家随到 `sparkwitch:pig_god` 后看不见技能。
8. 因 `sparktraits:conscience`（善良杀手）补偿而从好人替换成杀手的玩家没有按最终杀手身份抽词条。
9. `sparktraits:depression`（抑郁）不会自然出现。

执行优先级必须是：

1. 证明实际加载的 jar；
2. 在当前版本上复现；
3. 写失败测试；
4. 做最小修复；
5. Java 21 自动化验证；
6. 双客户端多人验收；
7. 最后才讨论版本号、提交、发布和替换实际 mods 目录。

不要把“源码存在”“Gradle 构建成功”“jar 已复制”当作多人验收通过。

## 二、已同步的远端基线

五个仓库均已执行 `git fetch --prune origin`。没有 reset、rebase、stash、clean、worktree prune、源码编辑、commit 或 push。

| 仓库 | 最新远端/本地主检出 | 版本 | 结论 |
|---|---|---:|---|
| SparkTraits | `origin/main=286d51b1a4ac38351edf6ebad8584c3ea5e9bc99`；当前主目录仍在 `ec6b03b8` 的功能分支 | 远端 `0.1.9.11` | 远端引用已更新；当前目录有 539 个未跟踪项，且比 `origin/main` 落后 14，禁止强行切分支或快进 |
| SparkFactionAPI | 本地 `main=f0087699`；`origin/main=727350ad` | `0.1.6.0` | 本地历史领先 4，但二者 tree hash 同为 `59d4adca`，源码内容与远端一致，禁止 reset |
| SparkWitch | `main=origin/main=3c44012ded25938828b6c1ad457bbce32c1ad503` | `0.1.6.2` | 主目录 clean，已是远端最新 |
| SparkAssist | `main=origin/main=2189c5c2bd9ae3f3fb5b060198847aaa88946ad4` | `0.1.3.8` | 主目录 clean，已是远端最新 |
| SparkStrength | `main=origin/main=6edbf02a92f88a989ca88443d668b41346a61e4a` | `1.0.8` | 主目录 clean，已是远端最新 |

### 工作树保护

- SparkTraits 当前主目录以及两个 Wraith 工作树都含大量未提交/未跟踪内容。缺失的 `/private/tmp/sparktraits-integrate-all-20260718` 已标记 prunable，但不要清理或复用。
- SparkWitch 的 `.codex-worktrees/wraith/SparkWitch` 大量 dirty；另有 6 个失效的 `/private/tmp` worktree。全部不碰。
- SparkAssist 的 Wraith 工作树有 7 个脏路径，另有一个失效 `/private/tmp` worktree。全部不碰。
- SparkFactionAPI 的 Wraith 工作树 clean 但分叉；SparkStrength 另有 detached worktree。不要移动它们。

Claude 实施 SparkTraits 时必须从 `origin/main` 创建新的附着分支工作树。创建前先确认分支和目标路径不存在，例如：

```bash
git -C /Users/kricy/Documents/Codex-Projects/SparkTraits fetch --prune origin
git -C /Users/kricy/Documents/Codex-Projects/SparkTraits worktree add \
  -b claude/trait-faction-fixes \
  /Users/kricy/Documents/Codex-Projects/.codex-worktrees/claude-trait-faction-fixes/SparkTraits \
  origin/main
```

SparkWitch 也必须从 `origin/main` 建独立附着分支工作树，例如 `claude/witch-faction-fixes`；不要直接把共享 clean `main` 留脏，更不要在现有 Wraith 工作树上实现。SparkAssist 等 Witch runtime 多人验收通过后，再从其 `origin/main` 建独立 `claude/witch-guidebook-sync` 工作树做资源同步。

SparkTraits 的 `AGENTS.md`、`CONTEXT.md` 和 `DOWNSTREAM_MIGRATION_NOTES.md` 是本地治理文件，已从远端删除并被 ignore，所以新 worktree 不会自带。Claude 动手前必须从原检出只读：

- `/Users/kricy/Documents/Codex-Projects/SparkTraits/AGENTS.md`
- `/Users/kricy/Documents/Codex-Projects/SparkTraits/CONTEXT.md`
- `/Users/kricy/Documents/Codex-Projects/SparkTraits/DOWNSTREAM_MIGRATION_NOTES.md`

将其中规则应用到新 worktree，但不要复制或提交这些文件。新增 non-obvious public API、mixin、Wathe seam 或跨模组语义时，按仓库规则写简短英中双语注释。

## 三、先做实际加载物审计

### 已发现的确定证据

- 当前 SparkWitch 构建物：
  - `build/libs/sparkwitch-0.1.6.2.jar`
  - SHA-256：`d063f05e96d10c693d3bfbe655a1e2dd9450ca736d8ef52632b58c77cb48ef65`
- Downloads 里仍只有旧包：
  - `/Users/kricy/Downloads/sparkwitch-0.1.6.1.jar`
  - SHA-256：`1971660712467bbfd79c11420d5f7953474151620b859876bae97f9d8fdd1f07`
- `0.1.6.1` 只有 `CurserRole.class`，没有 `CurserFeatureService`、`CurserPlayerComponent`、`CurserHudMixin`、`CurserHudRenderer` 或 `UseCurserAbilityC2SPacket`。如果服务器或客户端实际加载此包，诅咒者没有主动技能是旧包的确定结果。
- SparkTraits 当前主目录只有旧的 `0.1.9.9` 构建物，没有远端当前 `0.1.9.11` jar。
- SparkTraits 远端基线使用：
  - Wathe `1.5.6-spark-1.21.1`，SHA-256 `a4e0355c61def0b482c197a7ccd1f86ee91752b7af1b5bdafae8716c652f207f`
  - NoellesRoles `1.7.6-h1.5.6-spark`，SHA-256 `fcb0da6995197afff8637dd9236f96d9d07cfc0e26484ad3777e5cf3de37d8b7`
- 旧 SparkTraits 脏检出中仍有 Wathe `1.5.7` / NoellesRoles `1.7.7`。不得从该检出复制依赖，否则 Niko mixin ordinal 和胜负 listener 的诊断基线不再成立。

### Claude 必须收集

对服务器、正常客户端、每一个异常客户端分别记录 Spark 五库以及 Wathe、NoellesRoles：

- 实际 `mods` 目录的绝对路径；
- SparkTraits、SparkFactionAPI、SparkWitch、SparkAssist、SparkStrength 的文件名；
- jar 内 `fabric.mod.json` 的版本；
- SHA-256；
- `latest.log` 中实际加载版本；
- SparkWitch 日志中的 login query、play confirmation 和 mismatch 信息。

重点搜索：

```text
Confirmed SparkWitch server through login channel
Confirmed SparkWitch server through play channel
play confirmation channel ... is not available
Rejecting SparkWitch version mismatch
Sending SparkFactionAPI login version query
SparkFactionAPI ... was not understood
Allowing ... because proxies can drop Fabric login-query responses
Received SparkFactionAPI login version response
Rejecting SparkFactionAPI version mismatch
```

Curser/Saboteur 本能还依赖客户端 SparkFactionAPI listener，因此异常客户端与服务器的 FactionAPI jar/hash 也必须一致，不能只核对 SparkWitch。只有相关各端都加载相同当前 jar/hash，问题仍能复现时，才进入对应源码修复。版本字符串相同但 SHA 不同，也按错包处理。

## 四、SparkWitch 修改

### 4.1 诅咒者主动技能 HUD

**确定根因**

当前 `0.1.6.2` 已经有完整主动技能链：

- `src/main/java/dev/caecorthus/sparkwitch/roles/witch/curser/CurserFeatureService.java`
- `src/main/java/dev/caecorthus/sparkwitch/roles/witch/curser/CurserPlayerComponent.java`
- `src/main/java/dev/caecorthus/sparkwitch/net/SparkWitchPackets.java`
- `src/client/java/dev/caecorthus/sparkwitch/client/SparkWitchClient.java`
- `src/client/java/dev/caecorthus/sparkwitch/client/mixin/CurserHudMixin.java`

但 `src/client/java/dev/caecorthus/sparkwitch/client/curser/CurserHudRenderer.java` 在 `render` 开头用 `GameFunctions.isPlayerPlayingAndAlive(player)` 直接返回。晋升冤魂保持 Wathe dead 标记并处于 Adventure，因此专属右下 HUD 永远被该门禁隐藏。

**如何修改**

- 移除 renderer 内的通用 alive 门禁，或抽出一个纯展示规则。
- 展示条件只允许：
  - 已确认 SparkWitch 服务端；
  - exact `sparkwitch:curser`；
  - `WraithClientState.isActive(player)` 和 `isPromoted(player)` 同时为 true；
  - 当前本地玩家。
- 在 `CurserHudMixin`/纯规则补上 explicit active 检查；`promoted` 本身不包含 `active`。不要在 renderer 里再次套用 Wathe alive。
- 保持当前技能语义：共享角色能力键、8 格范围、10 秒混乱、晋升初始冷却、成功命中后冷却；附近无有效目标时不消耗冷却。

**测试**

- 为 HUD 条件新增纯规则测试：active+promoted dead/adventure Curser 可见；普通死者、未晋升 Wraith、`promoted=true/active=false` 和其他角色均不可见。
- 扩展 `src/test/java/dev/caecorthus/sparkwitch/client/curser/CurserClientPresentationSourceTest.java`，锁定 mixin 和 renderer 不再调用通用 alive 门禁。
- 新增 Curser packet payload ID/codec/receiver wiring 测试。
- 新增 `CurserFeatureService` 空目标契约测试/source test，证明零目标在 `startCooldown()` 之前返回；保留现有 `CurserRulesTest`、`CurserStateTest`。

**不该碰**

- 不把 Curser 加入 `WitchSkillRegistry` 或 `WitchSkillAssignmentService`。
- 不把 Curser 放进左上 `gui.sparkwitch.skills` inventory panel。该面板仍只属于 Grand Witch、Apprentice Witch、Murderous Witch。
- 不新建第二套按键，不改 NoellesRoles 的共享角色能力键。

### 4.2 诅咒者必须使用魔女阵营本能

**确定根因**

`CurserRole.DEFINITION` 已属于 `SparkWitchFactions.WITCH`，`WitchFactionRules.instinctColor` 也已有 Curser 配色。真正断点在：

- `src/main/java/dev/caecorthus/sparkwitch/roles/witch/WitchInstinctPolicy.java`

该 policy 以 Wathe `viewerAlive` 为前置条件。promoted Curser 带 dead 标记，policy 返回 `null`，随后 SparkFactionAPI 走通用 effective-faction fallback，因此呈现为错误的回退本能。

**如何修改**

- 在 `WitchInstinctPolicy.instinctHighlight` 中引入窄参与条件：
  - `viewerAlive || exact active promoted Curser`
  - 仍排除 spectator/creative。
- exact promoted Curser 可以通过 `CurserFeatureService.isActivePromotedCurser(viewer)` 或等价纯规则判断。
- 为本能亮度另加一个窄 predicate，让 exact active promoted Curser 在按住本能键时获得魔女本能的亮度过渡。
- 客户端判断必须同时使用已同步的 `WraithClientState.isActive`、`isPromoted`、exact role 和 confirmed server；不要把 server-only 状态类直接塞进客户端路径。

**目标文件**

- `src/main/java/dev/caecorthus/sparkwitch/roles/witch/WitchInstinctPolicy.java`
- `src/main/java/dev/caecorthus/sparkwitch/roles/witch/WitchFactionRules.java`，仅在需要纯 predicate 时修改
- `src/client/java/dev/caecorthus/sparkwitch/client/hooks/WitchInstinctClientHooks.java`
- `src/test/java/dev/caecorthus/sparkwitch/roles/witch/CurserWitchBoundaryTest.java`
- 新增/扩展 Witch instinct policy 与客户端规则测试

**不该碰**

- 保持 `WitchFactionRules.usesKillerStyleInstinctLight(Role)` 的旧语义，不要直接把 Curser 加进去。它还控制掉落物金色透视和 invisible Phantom hard-skip。
- Curser 仍不看金色掉落物，不继承 Grand Witch/Accomplice 的 invisible Phantom 特例，不获得 Wathe native killer capability、被动金币或直接击杀金币。
- 不扩大到其他 Wraith、普通死者或 spectator。
- 客户端测试单独锁定 `promoted=true/active=false` 仍为 false。
- 不修改 SparkFactionAPI 的通用 viewer predicate。

### 4.3 破坏者必须获得原生杀手本能

**确定根因**

`sparkwitch:saboteur` 已是 `FactionIds.KILLER`，并声明 `nativeWatheFaction(Faction.KILLER)`。缺口仍是 promoted Wraith 的 Wathe dead/adventure 状态，使 `WatheClient.isInstinctEnabledAndIsKiller` 的 alive/spectator 门禁失败。

**如何修改**

- 仿照 Wind Spirit 的生命周期门禁，新建：
  - `src/client/java/dev/caecorthus/sparkwitch/client/saboteur/SaboteurInstinctClientRules.java`
  - `src/client/java/dev/caecorthus/sparkwitch/client/mixin/saboteur/SaboteurInstinctMixin.java`
- 仅在下列条件同时满足时，于 `WatheClient.isInstinctEnabledAndIsKiller` 的 HEAD 返回 `true`：
  - confirmed SparkWitch server；
  - exact `sparkwitch:saboteur`；
  - `WraithClientState.isActive` 和 `isPromoted` 同时为 true；
  - 本能键正在按下。
- 在 `src/client/resources/sparkwitch.client.mixins.json` 注册新 mixin。
- Saboteur 已是 native killer，保留 Wathe 原生玩家、物品颜色和 lightmap，不复制 Wind Spirit 的角色色改写。
- 新 mixin 使用默认或显式 `priority = 1000`，必须低于 `CurserInstinctGateMixin` 的 1600 和 `WatheClientFearInstinctMixin` 的 1500，让 confusion/fear 等现有 veto 先返回 false。

**测试**

- 新增 Saboteur instinct 纯规则测试，覆盖所有布尔门禁和 exact role ID。
- 新增 mixin source/descriptor/config 注册测试。
- 覆盖 `promoted=true/active=false`、wrong role、confirmed=false、fear/confusion active；原始 false 只能在 exact active promoted Saboteur 条件下被扩展。
- 多人验收原生杀手玩家轮廓、物品轮廓和亮度。

**不该碰**

- 不改变 `SaboteurRole.DEFINITION`、FactionAPI capability 或全局 `WatheClient.isKiller()`。
- 不影响普通杀手、Curser、Wind Spirit、Vendetta、Guardian Angel。
- 不把 Saboteur 接入魔女自定义颜色。

### 4.4 “部分玩家”看不见大魔女魔力 / Pig God 技能

**当前源码事实**

服务端赋值链已经存在：

- `src/main/java/dev/caecorthus/sparkwitch/impl/SparkWitchEvents.java`
- `src/main/java/dev/caecorthus/sparkwitch/mana/WitchManaService.java`
- `src/main/java/dev/caecorthus/sparkwitch/skill/WitchSkillAssignmentService.java`
- `src/main/java/dev/caecorthus/sparkwitch/skill/SparkWitchBuiltInSkills.java`
- `src/main/java/dev/caecorthus/sparkwitch/component/WitchPlayerComponent.java`
- `src/main/java/dev/caecorthus/sparkwitch/component/WitchPlayerSyncCodec.java`

Grand Witch 会初始化并同步 mana；Pig God 的 exact skill 是 `sparkwitch:pig_chase`。不要在没有状态证据时重写 assignment。

共同风险是本问题相关的 `WitchManaHudMixin`、`WitchSkillHudMixin`、`CurserHudMixin` 和 shared-ability client tick 都受 `SparkWitchServerConnection.isConfirmedServer()` 门禁。login query 可因代理未回应而被容忍；play confirmation 目前只在 JOIN 时尝试一次。只有当异常客户端既没有 login confirmation，JOIN 时 play channel 又不可用，并且之后也没有收到 play confirmation 时，该客户端才会保持 unconfirmed。

**如何修改**

- 硬性分叉：仅当相同当前 jar/hash 下，异常客户端 `isConfirmedServer()==false`，且日志同时证明 login 未确认、play confirmation 未送达时，才修改握手。若已有 `Confirmed ... login/play channel`，跳过本节，直接比较 server/client component 状态。
- 首选目标：`src/main/java/dev/caecorthus/sparkwitch/net/SparkWitchPackets.java`
- 保留 JOIN 时 `canSend` 已为 true 的立即 confirmation。
- 使用 Fabric API `S2CPlayChannelEvents.REGISTER`：当该玩家在 play 阶段后续声明可接收的 channel 集合包含 `SparkWitchServerConfirmS2CPacket.PAYLOAD_ID` 时，再发送兼容版本 confirmation。
- 这是事件驱动的最小修复；不要先引入 UUID pending map、server tick 轮询、超时状态机或失效 handler/sender 缓存。
- 保留客户端对版本 mismatch 的断开行为。
- 不得因为“玩家已加入”或服务端“看到了 channel”就直接改客户端 confirmed 状态；客户端确认仍必须来自兼容版本的 confirmation packet。
- 如果没有发出 play-stage reconfirmation，只能报告该事实；客户端可能已经在 login 阶段确认，不能由服务端日志反推其一定 unconfirmed。
- 只有实测代理不触发 late REGISTER 事件时，才考虑有界轮询或小型 C2S ready/ack。

**测试**

- login understood：直接确认。
- unanswered login + delayed `S2CPlayChannelEvents.REGISTER`：late confirmation 后确认。
- wrong channel REGISTER：不发送；目标 channel REGISTER：发送兼容版本。
- channel 永远不可用：不崩溃、不误确认。
- mismatch：断开。
- disconnect/reconnect：不能泄漏上次连接的 confirmed 状态。
- 新增明确的 handshake wiring/source test，锁定 JOIN immediate path 与 late REGISTER path；若抽纯 helper，列为新的 `net` 内部规则类，不增加 production-only test hook。

如果相同当前 jar/hash 且客户端已经 confirmed，直接做状态分叉：

- Grand Witch：服务器与客户端分别检查 `manaEnabled`、`mana`。
- Pig God：服务器与客户端分别检查 `activeSkillId == sparkwitch:pig_chase`。
- server 正确/client 错误才查 owner sync；server 错误才查 RoleAssigned/assignment。
- 不增加周期性全量同步，不增加 production-only test hook。

### 4.5 SparkAssist Guidebook 同步

SparkAssist 不拥有上述运行时。Witch 语义完成后，只同步说明：

- `src/client/resources/assets/sparkassist/guidebook/roles/sparkwitch/curser.json`
- `src/client/resources/assets/sparkassist/guidebook/roles/sparkwitch/saboteur.json`
- `src/client/resources/assets/sparkassist/lang/zh_cn.json`
- `src/client/resources/assets/sparkassist/lang/en_us.json`
- `src/test/java/dev/caecorthus/sparkassist/guidebook/GuidebookWraithResourcesTest.java`
- `src/test/java/dev/caecorthus/sparkassist/guidebook/GuidebookLocalizationResourcesTest.java`

只在相同当前 jar 的多人 runtime 验收通过后同步文案。Curser 要精确说明：专属右下主动技能、玩家轮廓使用 Witch matrix，但没有金色掉落物透视或 invisible Phantom 特例。Saboteur 说明 Wathe 原生 killer 玩家/物品轮廓与亮度。追加新 key，不重排既有 key。

`GuidebookLocalizationResourcesTest.wraithTextKeys` 当前有精确计数；按实际新增引用更新该计数，并同时为 `zh_cn`、`en_us` 加语义断言。不要删除或放宽精确计数；只改 `GuidebookWraithResourcesTest` 不足以证明英文同步。

不要修改 `GuidebookRuntimeCatalog.java`，不要改 SparkAssist 的 `KeyBindingMixin`/`InstinctKeyRules`，也不要用 Guidebook 文案冒充 HUD 已修复。

## 五、SparkTraits 修改

### 5.1 终局亡命徒在 0s 后不结算

**确定根因**

1. Wathe 先把 `0s` 解析为 `TIME`。
2. `LastStandFinalMomentService` 对“活亡命徒 + 活杀手 + TIME”返回 `null`，允许后续 listener。
3. `EffectiveTraitService.checkWin` 没有保留 `TIME`，重新计算为 `KILLERS`。
4. `MurderGameModeMixin` 在写入胜者前看到活亡命徒，又取消该 `KILLERS`。
5. 时间保持 0，下一 tick 重复，形成死循环。

**目标文件**

- `src/main/java/dev/caecorthus/sparktraits/impl/effective/EffectiveTraitService.java`
- `src/test/java/dev/caecorthus/sparktraits/impl/effective/EffectiveTraitWinPrecedenceTest.java`，新增
- `src/test/java/dev/caecorthus/sparktraits/impl/traits/civilian/laststand/LastStandWitchLifecycleTest.java`

**如何修改**

- 在 `EffectiveTraitService.checkWin` 开头对已经解析的 `TIME` 和 `NEUTRAL` 返回 `null`，不要再重算 PASSENGERS/KILLERS。
- 必须返回 `null`，不要返回 `allow(TIME)`：`null` 才能保留后续中立 listener 的既有优先级。
- 抽出一个 package-private 纯 predicate，例如 `preservesResolvedWinStatus`，并让生产 `checkWin` 实际调用它。不要为测试暴露 public API，也不要增加 test-only production hook。

**测试**

- `EffectiveTraitWinPrecedenceTest` 测纯 predicate 对 TIME/NEUTRAL 保留、对 NONE/PASSENGERS/KILLERS 不误短路。
- `LastStandWitchLifecycleTest` 测 active Final Moment + 活亡命徒 + 活杀手 + TIME 时 final-moment listener 返回 `null`，且 finalization 不取消。
- KILLERS/PASSENGERS 在活亡命徒期间仍被阻止。
- 仅亡命徒存活仍保持现有 survivor/PASSENGERS 契约。
- NEUTRAL 不被重写。
- 非 Final Moment 的普通团队胜负不变。

**不该碰**

- 不修改 FactionAPI `BLOCK` 全局优先级。
- 不修改 Wathe/NoellesRoles。
- 不改变普通回合 TIME、中立胜利、Last Stand pending 假死或终局三分钟时长。

### 5.2 善良杀手补偿玩家按最终杀手身份重抽词条

**确定根因**

`TraitAssignmentService.assignForMurderGameBeforeWelcome` 先按原角色抽词条，再执行 Conscience 补杀手。`addExtraKillersForConscience` 会清掉候选好人的 random traits、替换 role、触发 `RoleAssigned`，但没有按最终杀手 role 再调用 `TraitSelector`。

**目标文件**

- `src/main/java/dev/caecorthus/sparktraits/impl/assignment/TraitAssignmentService.java`
- `src/main/java/dev/caecorthus/sparktraits/impl/selection/TraitSelector.java`
- 新增 `src/test/java/dev/caecorthus/sparktraits/impl/assignment/ConscienceCompensationTraitPlanTest.java`

**如何修改**

- 保持整体 assignment 顺序不变。
- 补偿目标必须继续满足现有 `!hasLocks()` 契约；带 trait lock 或 role lock 的计划都不能成为补偿玩家。
- 给补杀手流程传入同一 `TraitWorldComponent`、starting player count 和同一 RNG。
- forced Conscience 完成后，从当前 `PlayerPlan` 重新构建/校准 random unique reservations；不要盲用最初累计的 set，因为强制 Conscience 可能已经移除不兼容或占满槽位的 random trait。
- 清掉旧 random traits 前释放该计划占用的 unique reservation。
- 替换为最终 killer role 并触发既有 `RoleAssigned` 后，使用标准 selector 重抽 random slots。
- 选择器需要一个窄 overload，能够：
  - 尊重 slot chance、权重、enabled、audience、predicate、incompatibility、`MAX_TRAITS` 和 unique reservations；
  - 接收 exclusion set，并排除 `ConscienceTrait.ID`，避免补偿杀手再次成为善良杀手。
- 把新 random traits 写回同一个 `PlayerPlan`，再重新占用 unique reservations。

**重要语义**

修的是“从未按最终角色抽取”，不是“保证至少获得一个杀手词条”。标准 slot chance 合法地可能抽空；除非 owner 另行修改契约，不得强制至少一个。

**测试**

- 补偿玩家从 killer 候选池重抽，旧好人 random traits 被移除。
- 永不再抽 `sparktraits:conscience`。
- disabled、unique、incompatibility、最多 3 个均生效。
- 多个补偿杀手不重复 unique。
- trait-locked 与 role-locked 玩家都不进入补偿候选。
- 不重抽其他玩家，不改变 role history、初始物品清理和 public killer count。

### 5.3 抑郁自然出现上限

**确定根因**

`DepressionTraitService.randomDepressionCap` 当前公式为：

```text
(startingPlayerCount - 24) / 8
```

因此 24 至 31 人上限仍是 0，`enforceRandomDepressionCap` 会删除已经随机抽到的 Depression；32 人才首次允许 1 个。

**目标文件**

- `src/main/java/dev/caecorthus/sparktraits/impl/traits/civilian/depression/DepressionTraitService.java`
- `src/main/java/dev/caecorthus/sparktraits/impl/assignment/TraitAssignmentService.java`，原则上只验证，不应重写
- 新增 `src/test/java/dev/caecorthus/sparktraits/impl/traits/civilian/depression/DepressionRandomSelectionTest.java`
- 新增 `src/test/java/dev/caecorthus/sparktraits/impl/assignment/DepressionAssignmentCapTest.java`

**如何修改**

如果契约是“24 人起允许 1 个，之后每 8 人增加 1 个”，公式改为：

```text
1 + (startingPlayerCount - MIN_RANDOM_PLAYERS) / RANDOM_CAP_STEP_PLAYERS
```

边界应为：23→0，24..31→1，32..39→2，40..47→3。

如果实际复现局少于 24 人，先向 owner 确认是否要降低 `MIN_RANDOM_PLAYERS`；不要静默改变最低人数契约。

**测试与非目标**

- `DepressionRandomSelectionTest` 覆盖 23、24、31、32、39、40 和真实 selector 阈值。
- `DepressionAssignmentCapTest` 与 `TraitAssignmentService` 同包，使用 package-private `PlayerPlan` 验证 cap 只删除超额 random Depression，pending/admin lock 继续绕过随机预算。
- 不要为了测试扩大 `PlayerPlan` 或 `enforceRandomDepressionCap` 的生产可见性。
- 不改 stamina、mood、psycho、audio、render、fake death、death reason。

### 5.4 Niko 与重炮手组合

**当前远端源码结论**

当前 `origin/main` 没有静态互斥：

- `PoliceTraits` 同时注册 `sparktraits:niko` 和 `sparktraits:heavy_artillery`。
- Final Moment Outlaw loadout 明确同时包含二者。
- 第一发 Wathe 枪击会进入 `killPlayerWithPoliceGunTraits`。
- Niko 的两次延迟射击最终也调用 `killPlayerWithHeavyArtillery`。

因此不要先改逻辑。先用当前 `0.1.9.11` 构建并加载后复现。

**先加的测试**

- 新增 `src/test/java/dev/caecorthus/sparktraits/impl/traits/civilian/police/NikoHeavyArtilleryCompositionTest.java`。
- 覆盖：
  - exact Vigilante 与标记的 Final Moment Outlaw；
  - Veteran、普通 Loose End、未标记 Loose End 均为 false；
  - 同时有/只有一个 trait；
  - crouching / standing；
  - 4.9 格 / 5.1 格；
  - 普通目标、盾、Jester transition、Last Stand handoff；
  - 第一发和两次 synthetic repeat 均进入同一个 Heavy resolver。

**只有当前 jar 仍复现时才继续**

- 记录 `GunShootPayloadMixin` 是否应用。
- 记录首发 redirect 和 `resolveNikoBurstShot` 三次调用的 shooter、target、distance、trait set。
- 当前 repeat 会在 +2/+4 tick 重新射线取准星目标。只有回放证明问题来自重新取目标，并且 owner 明确要求“三连发锁定首个目标”时，才把原始 target UUID 传入 scheduler。
- 即使 owner 批准锁定，也必须在每次延迟 tick 重新验证同 world、同 round、非自己、playing/alive、距离和视线/遮挡；移动或超出 `NIKO_GUN_RANGE` 后的语义必须有明确测试，不能形成穿墙或无限距离追踪。
- 不得把阵营变成目标资格门禁；每发仍按当时的 effective-civilian 状态重新计算 punishment。
- 若改变 scheduler 签名，除 composition test 外还要扩展现有 `NikoBurstSchedulerSafetyTest` 和 `NikoRepeatShotPunishmentTest`，覆盖断线重连、world/round 切换、枪被移除、取消蹲伏、目标消失和延迟 punishment。
- 未得到该语义批准，不要锁目标；现有文案只承诺“三连发”，不承诺固定目标。

**不该碰**

- 不扩大到非 exact Vigilante、非蹲伏、Derringer 或 5 格外。唯一现有例外是 active Final Moment 标记的 `wathe:loose_end` 经 runtime 映射为 Vigilante；Veteran、普通 Loose End 和未标记 Loose End 都必须为 false。
- 不绕过 Jester fake-death、Last Stand、punishment/backfire。
- 不把任何逻辑迁入 SparkStrength。

## 六、明确不需要运行时代码修改的仓库

### SparkFactionAPI

九项均不应先改 API。当前 API 已正确区分：

- Spark faction id；
- Wathe native faction bucket；
- effective faction；
- registered instinct policy 与 fallback。

不得把 Curser/Saboteur role ID 硬编码进 API，不得扩大通用 viewer predicate，不得改变 `FactionWinRules` 的全局 BLOCK 优先级。

### SparkStrength

九项全部 verification-only。仓库没有 Niko/Heavy Artillery owner 逻辑。保留 exact `sparktraits:impostor` 的公共反射桥，不新增 SparkTraits/SparkWitch/FactionAPI 依赖，不动 Corrupt Cop instinct。

### SparkAssist

Java runtime verification-only；只做 4.5 节所述 Guidebook 资源同步。

## 七、验证命令与验收

本机默认 Java 是 23，所有 Gradle 命令必须显式使用 Java 21：

```bash
export JAVA_HOME=$(/usr/libexec/java_home -v 21)
export PATH="$JAVA_HOME/bin:$PATH"
java -version
```

在各自干净工作树中先跑 focused tests。SparkTraits `origin/main` 已跟踪仓库根目录的三个基线依赖；不要从其他仓库覆盖它们，先直接核验新 worktree 中的文件。SparkAssist 的 Wathe jar 被 ignore，才需要复制到其 `libs/`：

```bash
TRAITS_WORKTREE=/absolute/path/to/SparkTraits-worktree
ASSIST_WORKTREE=/absolute/path/to/SparkAssist-worktree

shasum -a 256 "$TRAITS_WORKTREE/wathe-1.5.6-spark-1.21.1.jar"
shasum -a 256 "$TRAITS_WORKTREE/noellesroles-1.7.6-h1.5.6-spark.jar"
shasum -a 256 "$TRAITS_WORKTREE/sparkfactionapi-0.1.5.9.jar"

shasum -a 256 /Users/kricy/Documents/Codex-Projects/SparkFactionAPI/libs/wathe-1.5.6-spark-1.21.1.jar
cp /Users/kricy/Documents/Codex-Projects/SparkFactionAPI/libs/wathe-1.5.6-spark-1.21.1.jar \
  "$ASSIST_WORKTREE/libs/wathe-1.5.6-spark-1.21.1.jar"
```

期望 SHA-256：

```text
Wathe       a4e0355c61def0b482c197a7ccd1f86ee91752b7af1b5bdafae8716c652f207f
NoellesRoles fcb0da6995197afff8637dd9236f96d9d07cfc0e26484ad3777e5cf3de37d8b7
SparkFactionAPI 0.1.5.9 e86fc1ea32b0bf858b9b80f46c2bb68d091fd800c741c29331d70b734567911f
```

SparkTraits `origin/main` 的 `sparkfactionapi_version` 仍是 `0.1.5.9`；这里是编译基线，不得擅自用当前运行时 `0.1.6.0` 替换，也不要顺手升级依赖契约。不要使用旧 SparkTraits 或 dirty Wraith 工作树中的 Wathe `1.5.7` / NoellesRoles `1.7.7` jar。

然后做 repo-specific clean verification：

```bash
# SparkTraits / SparkWitch
./gradlew clean test verifyArchitecture build \
  --no-daemon --no-watch-fs --console=plain

# SparkFactionAPI
./gradlew verifyArchitecture --no-daemon --no-watch-fs --console=plain
./gradlew clean build --no-daemon --no-watch-fs --console=plain

# SparkAssist
./gradlew verifyWatheDependency verifyArchitecture --no-daemon --no-watch-fs --console=plain
./gradlew clean build --no-daemon --no-watch-fs --console=plain

# SparkStrength
./gradlew verifyArchitecture --no-daemon --no-watch-fs --console=plain
./gradlew clean build --no-daemon --no-watch-fs --console=plain \
  --no-parallel --max-workers=1
```

每个仓库最后都运行：

```bash
git diff --check
```

注意：本轮审计曾尝试 SparkWitch Java 21 focused tests，但 `:compileJava` 超过两分钟未完成后被终止，不能把它写成已通过。

### 多人验收矩阵

至少使用两个客户端，一个直连、一个走实际代理路径：

1. promoted Curser：右下技能可见，G 可用，空目标不耗冷却。
2. promoted Curser：玩家轮廓使用 Witch matrix；不看金色掉落物；confusion 仍封锁本能。
3. promoted Saboteur：原生 killer 玩家/物品轮廓与亮度正常。
4. Niko + Heavy：用能承受首发的受控目标/护盾场景，证明三连发存在，且每个真正命中活目标的近距离枪击进入 Heavy double-resolution；范围外不走。普通目标首发死亡后，后两发无法再次命中是合法结果。
5. Grand Witch：每个客户端均显示相同 mana，任务/击杀/自然恢复后同步。
6. Final Moment：Outlaw + killer 到 0s 正常按 TIME 结束。
7. Pig God：每个客户端均显示 `sparkwitch:pig_chase`，输入可用。
8. Conscience 补偿 killer：最终 trait plan 来自 killer 候选池且不含 Conscience。
9. Depression：在可实际组织的代表人数局中证明能自然 assignment；23/24/31/32/39/40 和锁定绕过属于自动化测试，不为凑人数增加 production hook。

最后记录每个产物的：

- commit SHA；
- `mod_version`；
- jar 文件名；
- SHA-256；
- 实际服务器/客户端 mods 目标；
- 复制后 `cmp` 或重新计算的 SHA；
- 服务端与客户端日志中的实际加载版本；
- 自动化测试结果；
- 多人验收结果。

## 八、Claude 的最终汇报格式

Claude 完成后必须分别报告：

1. 每个仓库的基线 SHA、分支、工作树路径；
2. 每项问题的根因、修改文件、测试；
3. 未修改的仓库及原因；
4. Java 21 命令和结果；
5. 新 jar 的版本与 SHA；
6. 实际加载 jar 的版本与 SHA；
7. 多人验收是否完成；
8. 尚未证明的部分，不得用“应该已经修复”替代。

未经 owner 另行要求，不要 bump version、commit、push、清理旧工作树或替换生产 mods。
