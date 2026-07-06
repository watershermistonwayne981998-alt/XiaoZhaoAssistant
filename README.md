# 小赵手机事务助手

通知监听型本地 Agent —— 监听通知栏消息，自动识别待办，所有数据只保存在手机本地。

## 项目简介

本应用通过 Android `NotificationListenerService` 监听手机通知栏消息，收集微信、钉钉、企业微信、短信、邮件等 App 的通知，使用内置规则引擎自动识别待办事项，所有数据仅保存在手机本地。

**隐私承诺：**
- 不做服务器、不做网页、不做云端同步
- 不读取微信/钉钉数据库，不 root，不无障碍偷读聊天
- 不上传任何通知内容
- 无广告、无统计 SDK、无第三方埋点
- 不申请无关权限

## 技术栈

| 项目 | 版本 |
|------|------|
| Kotlin | 2.0.21 |
| AGP | 8.5.2 |
| Gradle | 8.9 |
| compileSdk | 34 |
| minSdk | 29 (Android 10) |
| targetSdk | 34 |
| UI | Jetpack Compose + Material3 |
| 数据库 | Room 2.6.1 |
| 异步 | Kotlin Coroutines + Flow |

## 项目结构

```
XiaoZhaoAssistant/
├── app/
│   ├── build.gradle.kts
│   ├── proguard-rules.pro
│   └── src/main/
│       ├── AndroidManifest.xml
│       ├── java/com/xiaozhao/assistant/
│       │   ├── App.kt                          # Application 入口
│       │   ├── data/
│       │   │   ├── entity/                     # Room 实体
│       │   │   │   ├── NotificationEntity.kt
│       │   │   │   ├── TaskEntity.kt
│       │   │   │   ├── AppWhitelistEntity.kt
│       │   │   │   └── KeywordRuleEntity.kt
│       │   │   ├── db/                         # DAO + Database
│       │   │   │   ├── AppDatabase.kt
│       │   │   │   ├── NotificationDao.kt
│       │   │   │   ├── TaskDao.kt
│       │   │   │   ├── AppWhitelistDao.kt
│       │   │   │   └── KeywordRuleDao.kt
│       │   │   └── repository/
│       │   │       └── AppRepository.kt
│       │   ├── engine/                         # 待办识别规则引擎
│       │   │   ├── KeywordSets.kt              # 默认词表
│       │   │   ├── TimeParser.kt               # 时间提取
│       │   │   └── TodoRuleEngine.kt           # 规则匹配
│       │   ├── service/
│       │   │   └── NotificationListener.kt     # 通知监听服务
│       │   ├── ui/
│       │   │   ├── MainActivity.kt             # 主界面 + 底部导航
│       │   │   ├── screens/                    # 四个页面
│       │   │   │   ├── HomeScreen.kt           # 首页
│       │   │   │   ├── MessageScreen.kt        # 消息页
│       │   │   │   ├── TodoScreen.kt           # 待办页
│       │   │   │   └── SettingsScreen.kt       # 设置页
│       │   │   └── theme/                      # Compose 主题
│       │   ├── viewmodel/
│       │   │   └── AppViewModel.kt
│       │   └── util/
│       │       └── NotificationUtils.kt        # 权限检测、文本提取
│       └── res/                                # 资源文件
│           ├── values/
│           │   ├── strings.xml
│           │   ├── colors.xml
│           │   └── themes.xml
│           ├── drawable/
│           └── mipmap-anydpi-v26/             # 自适应图标
├── build.gradle.kts
├── settings.gradle.kts
├── gradle.properties
├── gradle/
│   ├── libs.versions.toml                      # 版本目录
│   └── wrapper/
│       └── gradle-wrapper.properties
└── README.md
```

## 环境准备

### 1. 安装 Android Studio

下载 Android Studio Hedgehog (2023.1) 或更高版本：https://developer.android.com/studio

### 2. 安装 Android SDK

在 Android Studio 的 SDK Manager 中安装：
- Android SDK Platform 34 (Android 14)
- Android SDK Platform 29 (Android 10, minSdk)
- Android SDK Build-Tools 34.0.0
- Android SDK Platform-Tools

### 3. 配置 local.properties

在项目根目录创建 `local.properties` 文件，写入你的 SDK 路径：

```properties
# Windows 示例
sdk.dir=C\:\\Users\\你的用户名\\AppData\\Local\\Android\\Sdk
```

### 4. 生成 Gradle Wrapper

项目已包含 `gradle/wrapper/gradle-wrapper.properties`，但需要生成 `gradle-wrapper.jar`。

方法一（推荐）：用 Android Studio 打开项目，IDE 会自动下载。
方法二：如果本地已安装 Gradle 8.x，在项目根目录执行：
```bash
gradle wrapper --gradle-version 8.9
```

## 构建命令

### Debug APK

```bash
# Windows (项目根目录)
.\gradlew.bat assembleDebug

# Linux / macOS
./gradlew assembleDebug
```

生成的 APK 位于：
```
app/build/outputs/apk/debug/app-debug.apk
```

### Release APK

```bash
.\gradlew.bat assembleRelease
```

生成的 APK 位于：
```
app/build/outputs/apk/release/app-release.apk
```

> Release 构建未配置签名，如需正式签名请在 `app/build.gradle.kts` 的 `buildTypes` 中添加 `signingConfig`。

### 清理并重新构建

```bash
.\gradlew.bat clean assembleDebug
```

## 安装步骤

### 方法一：ADB 安装

1. 手机开启「开发者选项」和「USB 调试」
2. USB 连接电脑
3. 执行：
```bash
adb install app/build/outputs/apk/debug/app-debug.apk
```

### 方法二：直接安装

将 `app-debug.apk` 传到手机，点击安装。华为手机需在「设置 → 安全 → 更多设置」中允许「安装外部来源应用」。

## 通知权限开启步骤（关键）

应用安装后必须手动开启通知使用权，否则无法监听任何通知。

1. 打开「小赵手机事务助手」App
2. 点击底部「设置」标签
3. 查看「通知监听权限」状态
   - 如果显示「未开启」，点击「前往通知使用权设置」
4. 在系统设置页面的通知使用权列表中，找到「小赵手机事务助手」
5. 打开开关，在弹出的确认框中点击「允许」
6. 返回 App，点击「刷新权限状态」确认显示「已开启」

> HarmonyOS 4.2 路径：设置 → 通知和状态栏 → 通知使用权 → 小赵手机事务助手 → 开启

## 华为 / HarmonyOS 4.2 后台运行设置

华为/荣耀手机的省电策略较为激进，如果不做以下设置，通知监听服务可能被系统杀死导致停止工作：

### 1. 关闭电池优化

设置 → 应用 → 小赵手机事务助手 → 耗电详情 → 启动管理 → 关闭「自动管理」→ 手动开启：
- 允许自启动 ✅
- 允许关联启动 ✅
- 允许后台活动 ✅

### 2. 取消电池限制

设置 → 应用 → 小赵手机事务助手 → 耗电详情 → 电池 → 选择「无限制」

### 3. 任务加锁

在最近任务列表中找到「小赵手机事务助手」，下拉卡片（或点击锁图标），加锁防止被一键清理。

### 4. 检查通知使用权

部分系统更新后可能重置通知使用权。如果发现通知不再被采集，请到设置中检查通知使用权是否仍然开启。

> App 设置页中提供「后台运行设置引导」按钮，点击可查看完整步骤并直接跳转系统设置。

## 功能说明

### 首页
- 今日待办数量
- 未处理消息数量
- 重要消息数量
- 最近 5 条待办

### 消息页
- 通知流列表（按时间倒序）
- 按 App 筛选（顶部筛选条）
- 关键词搜索
- 每条通知可「一键转待办」

### 待办页
四个标签页：
- **待确认**：规则引擎自动生成的候选待办，需用户确认
- **今日待办**：用户确认后的正式待办
- **已逾期**：截止时间已过但未完成的待办
- **已完成**：已完成的待办

每条待办支持：
- 确认（待确认 → 今日待办）
- 完成（标记为已完成）
- 修改截止时间（日期 + 时间选择器）
- 删除

### 设置页
- 通知监听权限状态 + 跳转
- 采集开关（一键开启/关闭采集）
- App 白名单管理（添加/启用/禁用）
- 待办关键词管理（增删/启用禁用）
- 数据导出（JSON 格式，通过系统 SAF 选择保存位置）
- 数据清空
- 后台运行设置引导

## 待办识别规则

### 词表

| 类型 | 说明 | 示例 |
|------|------|------|
| 时间词 | 表示时间节点 | 今天、明天、后天、本周、下周、月底、上午、下午、晚上、下班前、10点、12点前、下午5点前 |
| 动作词 | 表示需要执行的动作 | 提交、发送、报送、审批、确认、修改、补充、整理、开会、提醒、处理、反馈、上传 |
| 工作词 | 表示工作对象 | 材料、文件、合同、报销、采购、项目、会议、请示、流程、付款、发票、比选、询价、清单 |
| 紧急词 | 表示紧迫程度 | 尽快、马上、抓紧、今天内、务必、别忘了、提醒一下 |

### 识别逻辑

1. 通知内容同时包含「动作词」+「工作词」→ 生成候选待办
2. 同时包含「时间词」→ 自动提取截止时间
3. 包含「紧急词」→ 标记为重要（priority=2）
4. 候选待办默认状态为「待确认」，不会直接变成正式待办
5. 用户在待办页确认后，进入「今日待办」

### 去重逻辑

去重字段：`packageName + notificationId + postTime + title + text`
通过 Room 数据库的 unique index 自动去重，重复通知不会重复入库。

## 数据表结构

### NotificationEntity（通知记录）
| 字段 | 类型 | 说明 |
|------|------|------|
| id | Long | 自增主键 |
| packageName | String | 来源包名 |
| appName | String | 应用名称 |
| title | String | 通知标题 |
| text | String | 通知正文 |
| bigText | String? | 大文本内容 |
| postTime | Long | 通知时间 |
| notificationKey | String | 通知 Key |
| notificationId | Int | 通知 ID |
| isRead | Boolean | 是否已读 |
| isConvertedToTask | Boolean | 是否已转待办 |
| isImportant | Boolean | 是否重要 |
| createdAt | Long | 入库时间 |

### TaskEntity（待办任务）
| 字段 | 类型 | 说明 |
|------|------|------|
| id | Long | 自增主键 |
| title | String | 待办标题 |
| sourceNotificationId | Long? | 来源通知 ID |
| sourceApp | String? | 来源应用 |
| originalText | String? | 原始文本 |
| dueTime | Long? | 截止时间 |
| priority | Int | 优先级 (1=普通, 2=重要) |
| status | String | 状态 (PENDING/TODAY/DONE) |
| createdAt | Long | 创建时间 |
| completedAt | Long? | 完成时间 |

### AppWhitelistEntity（App 白名单）
| 字段 | 类型 | 说明 |
|------|------|------|
| packageName | String | 包名（主键） |
| appName | String | 应用名称 |
| enabled | Boolean | 是否启用 |

### KeywordRuleEntity（关键词规则）
| 字段 | 类型 | 说明 |
|------|------|------|
| id | Long | 自增主键 |
| keyword | String | 关键词 |
| type | String | 类型 (TIME/ACTION/WORK/URGENT) |
| enabled | Boolean | 是否启用 |

## 第一版测试用例

### TC-01 通知权限检测
1. 首次安装打开 App，进入设置页
2. 预期：通知监听权限显示「未开启」
3. 点击「前往通知使用权设置」，跳转到系统通知使用权页面
4. 开启权限后返回 App，点击「刷新权限状态」
5. 预期：显示「已开启」

### TC-02 通知采集基础
1. 开启通知使用权和采集开关
2. 用另一台手机发送微信消息，或发送短信
3. 预期：消息页出现对应通知，显示来源 App、标题、正文、时间

### TC-03 通知去重
1. 同一条通知多次触发（如微信连续推送相同消息）
2. 预期：消息页只出现一条，不会重复

### TC-04 白名单过滤
1. 设置页添加「微信」(com.tencent.mm) 到白名单，勾选启用
2. 再添加「短信」(com.android.mms)，不勾选
3. 预期：只有微信通知被采集，短信通知不出现

### TC-05 采集开关
1. 在设置页关闭采集开关
2. 发送新通知
3. 预期：消息页不出现新通知
4. 重新打开采集开关
5. 发送新通知
6. 预期：消息页出现新通知

### TC-06 待办自动识别
1. 发送包含动作词+工作词的通知，如"请提交报销材料"
2. 预期：待办页「待确认」标签出现候选待办
3. 预期：待办标题为通知标题，来源显示微信/钉钉等

### TC-07 截止时间提取
1. 发送包含时间词的通知，如"明天下午5点前提交项目材料"
2. 预期：候选待办的截止时间被自动提取并显示

### TC-08 紧急标记
1. 发送包含紧急词的通知，如"尽快确认合同流程"
2. 预期：通知被标记为重要（消息页显示星标，首页重要消息数+1）
3. 预期：生成的候选待办 priority=2

### TC-09 一键转待办
1. 消息页找到一条未转待办的通知
2. 点击「转待办」按钮
3. 预期：通知标记为「已转待办」
4. 预期：待办页「待确认」出现新待办

### TC-10 待办确认
1. 待办页「待确认」中选择一条候选待办
2. 点击「确认」
3. 预期：待办从「待确认」移动到「今日待办」

### TC-11 待办完成
1. 在「今日待办」中选择一条
2. 点击完成按钮（✓）
3. 预期：待办移动到「已完成」标签

### TC-12 修改截止时间
1. 点击待办的编辑按钮（✎）
2. 选择新的日期和时间
3. 预期：待办的截止时间更新

### TC-13 删除待办
1. 点击待办的删除按钮
2. 预期：弹出确认对话框
3. 点击「删除」
4. 预期：待办从列表消失

### TC-14 数据导出
1. 设置页点击「导出数据（JSON）」
2. 系统弹出文件保存选择器
3. 选择保存位置
4. 预期：显示「数据已导出到所选位置」
5. 预期：导出的 JSON 文件包含 tasks 数组

### TC-15 清空数据
1. 设置页点击「清空所有数据」
2. 预期：弹出确认对话框
3. 点击「确定清空」
4. 预期：消息页和待办页均为空

### TC-16 关键词管理
1. 设置页添加一个新关键词"申报"，类型选择"动作词"
2. 发送包含"申报"的通知
3. 预期：如果同时包含工作词，则生成候选待办

### TC-17 首页统计
1. 首页检查四个统计卡片
2. 预期：今日待办数 = 今日待办标签中的数量
3. 预期：未处理消息数 = 消息页未读通知数
4. 预期：最近 5 条待办与待办页一致

### TC-18 后台存活
1. 开启通知使用权，按 Home 键回到桌面
2. 等待 5 分钟
3. 发送新通知
4. 预期：消息页仍然能采集到通知
5. 如果采集不到，按设置页的「后台运行设置引导」操作

## 常见问题

### Q: 通知监听服务自动断开怎么办？
A: 华为/荣耀手机可能因省电策略杀死后台服务。请按「后台运行设置引导」完成全部设置。如果仍然断开，重新打开 App 并检查通知使用权。

### Q: 微信通知读不到？
A: 确认：1) 通知使用权已开启；2) 微信在新消息时确实推送了通知栏通知；3) 如果配置了白名单，确认微信在白名单中且已启用。

### Q: 待办识别不准确？
A: 规则引擎基于关键词匹配，可在设置页增删关键词。候选待办默认为「待确认」状态，不会自动变成正式待办，用户确认前不会有副作用。

### Q: 数据会上传到服务器吗？
A: 不会。所有数据仅保存在手机本地 Room 数据库中。导出功能通过系统 SAF（Storage Access Framework）让用户自己选择保存位置，不经任何网络传输。

### Q: 支持哪些手机？
A: 最低支持 Android 10。已在华为 Mate 50 Pro / HarmonyOS 4.2 环境下设计适配。其他品牌 Android 10+ 手机也可使用，后台设置步骤可能略有不同。

## 许可

本项目为私人定制工具，不对外发布。
