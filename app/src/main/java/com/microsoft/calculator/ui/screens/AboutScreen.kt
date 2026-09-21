package com.microsoft.calculator.ui.screens

import android.content.Intent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Divider
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.FileProvider
import com.microsoft.calculator.BuildConfig
import com.microsoft.calculator.ui.i18n.LocalStrings
import com.microsoft.calculator.ui.theme.*
import java.io.File

/**
 * 更新日志条目:(版本号, 日期, 各语言对应变更列表)
 * 新增版本时在此追加一条即可。
 */
private data class ChangelogEntry(
    val version: String,
    val date: String,
    val zh: List<String>,
    val en: List<String>
)

private val CHANGELOG = listOf(
    ChangelogEntry(
        version = "2.11.0",
        date = "2026-09-21",
        zh = listOf(
            "货币转换接入免费实时汇率 API（open.er-api.com，无需注册），支持 18 种主要货币实时换算，含自动加载、手动刷新、离线备用数据",
            "新增 README.md 文档，详细说明环境安装（JDK 17、Android SDK 34）和编译步骤，任何人可据此自行编译 APK",
            "项目已开源至 GitHub: github.com/danqingpro/multi-calculator"
        ),
        en = listOf(
            "Currency converter integrated with free real-time exchange rate API (open.er-api.com, no registration needed), supports 18 major currencies with auto-load, manual refresh, and offline fallback rates",
            "Added README.md with detailed build instructions (JDK 17, Android SDK 34) so anyone can compile their own APK",
            "Project is now open source on GitHub: github.com/danqingpro/multi-calculator"
        )
    ),
    ChangelogEntry(
        version = "2.10.1",
        date = "2026-09-07",
        zh = listOf(
            "修复日期计算界面点击日历图标后闪退的问题:移除导致无限测量循环的 horizontalScroll 包装,改用自定义宽度 Dialog(占屏幕 94% 宽),确保日历完整显示且不再崩溃",
            "修复 QQ 授权登录报错 \"param client_id is wrong\":QQ 互联 OAuth 流程需要在 connect.qq.com 注册真实 App,当前版本暂时移除 WebView 授权登录按钮,反馈改为仅支持「通过 QQ 邮箱 SMTP 直接发送」(填写 QQ 邮箱 + 16 位授权码),无需注册任何开发者账号"
        ),
        en = listOf(
            "Fix crash when tapping calendar icon in date picker: removed the horizontalScroll wrapper that caused an infinite measurement loop, replaced with a custom-width Dialog (94% of screen) so the calendar displays fully without crashing",
            "Fix QQ OAuth error 'param client_id is wrong': QQ Connect OAuth requires a real app registered on connect.qq.com. This version temporarily removes the WebView OAuth login button; feedback now supports only 'Send via QQ Mail directly' (fill in QQ email + 16-digit auth code), no developer account required"
        )
    ),
    ChangelogEntry(
        version = "2.10.0",
        date = "2026-09-07",
        zh = listOf(
            "修复日期计算界面日历选择器只显示周一到周六、缺少周日的问题:用横向滚动包裹 DatePicker,确保 7 列完整可见",
            "反馈功能新增「QQ 邮箱直接发送」方式:无需手机预装邮件 App,通过 QQ 邮箱 SMTP(smtp.qq.com:465 SSL)直接把反馈发送到开发者邮箱",
            "反馈功能新增「QQ 授权登录」:在应用内 WebView 中完成 QQ 互联 OAuth 授权,获取 QQ 昵称作为发送人身份",
            "QQ 直接发送方式需填写 QQ 邮箱地址和邮箱授权码(在 QQ 邮箱设置 → 账户 → 开启 SMTP 服务生成),支持附件",
            "反馈界面增加发送方式单选:「通过邮件应用发送」与「通过 QQ 邮箱直接发送」二选一"
        ),
        en = listOf(
            "Fix date picker only showing Monday-Saturday (Sunday column clipped): wrapped DatePicker in horizontal scroll so all 7 columns are visible",
            "Feedback adds 'Send via QQ Mail directly' mode: no pre-installed email app needed, sends feedback directly to developer's mailbox via QQ Mail SMTP (smtp.qq.com:465 SSL)",
            "Feedback adds 'QQ authorized login': completes QQ Connect OAuth in an in-app WebView to get the QQ nickname as sender identity",
            "QQ direct mode requires QQ email address and email authorization code (generate in QQ Mail Settings → Account → enable SMTP), supports attachments",
            "Feedback screen adds send-mode radio: 'Send via email app' or 'Send via QQ Mail directly'"
        )
    ),
    ChangelogEntry(
        version = "2.9.0",
        date = "2026-09-06",
        zh = listOf(
            "「关于」页面新增「分享给好友」按钮:一键把当前版本的安装包(APK)通过系统分享面板发送给好友",
            "好友收到 .apk 文件后下载即可安装使用,无需应用商店",
            "分享文件以 calculator-v<版本号>.apk 命名,复用应用已配置的 FileProvider"
        ),
        en = listOf(
            "Added 'Share app' button on the About screen: one-tap share of the current version's APK via the system share sheet",
            "Friends receive the .apk file and can install it directly without an app store",
            "Shared file is named calculator-v<version>.apk, reuses the app's existing FileProvider"
        )
    ),
    ChangelogEntry(
        version = "2.8.0",
        date = "2026-09-06",
        zh = listOf(
            "新增「意见反馈」功能:在导航抽屉设置分组中新增入口,可向开发者邮箱 2290943281@qq.com 发送反馈",
            "反馈界面支持填写联系方式(电话或邮箱)和反馈内容文本",
            "支持添加图片与任意文件作为附件,附件列表显示文件名与大小,可单独移除",
            "附件大小限制:单个文件及总附件均不超过 50 MB(QQ 邮箱单封邮件附件上限)",
            "提交时自动调用系统邮件应用,收件人/主题/正文/附件均已预填,用户点击发送即可"
        ),
        en = listOf(
            "New 'Feedback' feature: added entry in navigation drawer Settings group, sends feedback to developer email 2290943281@qq.com",
            "Feedback screen supports contact info (phone or email) and feedback message text",
            "Supports attaching images and arbitrary files; attachment list shows file name and size, removable individually",
            "Attachment size limit: each file and total attachments must not exceed 50 MB (QQ email per-message attachment cap)",
            "On submit, the system email app is launched with recipient/subject/body/attachments pre-filled; user just taps send"
        )
    ),
    ChangelogEntry(
        version = "2.7.0",
        date = "2026-09-07",
        zh = listOf(
            "转换器拆分为独立界面:导航栏点击每个类别(长度/面积/体积/质量/温度/时间/速度/数据/角度/压强/能量/功率/货币)直接进入对应换算界面,不再在界面内集中显示所有类别",
            "转换器界面按参考图重构:大号数值显示 + 单位下拉按钮,底部数字键盘(CE/退格/7-9/4-6/1-3/正负/0/小数点)",
            "新增「功率」类别:瓦/千瓦/兆瓦/马力(美制)/马力(公制)/BTU每分钟/磅英尺每分钟/千克力·米每秒",
            "角度换算新增「约等于 X 梯度」补充行;功率换算新增「约等于 X BTU/分钟  Y 磅英尺/分钟  Z 瓦」补充行",
            "修复绘图界面 Y 轴刻度标签不显示的问题(坐标映射符号错误)"
        ),
        en = listOf(
            "Converter split into independent screens: tapping each category (Length/Area/Volume/Mass/Temperature/Time/Speed/Data/Angle/Pressure/Energy/Power/Currency) in the navigation opens its own dedicated converter, all categories are no longer shown together",
            "Converter screen redesigned per reference: large value display + unit dropdown button, bottom numeric keypad (CE/backspace/7-9/4-6/1-3/plus-minus/0/decimal)",
            "New 'Power' category: Watt/Kilowatt/Megawatt/Horsepower (US)/Horsepower (metric)/BTU per minute/Foot-pound per minute/kgf·m/s",
            "Angle converter adds '≈ X gradians' supplementary line; Power converter adds '≈ X BTU/min  Y ft-lb/min  Z W' supplementary line",
            "Fix plot screen Y-axis tick labels not showing (incorrect sign in coordinate mapping)"
        )
    ),
    ChangelogEntry(
        version = "2.6.0",
        date = "2026-09-06",
        zh = listOf(
            "函数分析大幅扩展:除 sin/cos/tan 外,新增对 sin(x)+cos(x)、多项式(xⁿ、一次、二次)、反比例(1/x)、根式(sqrt)、对数(ln/log)、指数(eˣ/aˣ)、绝对值(abs)、csc/sec/cot 等函数类型的符号化分析",
            "下标统一改为 nᵢ(n 下标 i),不再使用 n₁",
            "sin(x)+cos(x) 型函数自动合并为 R·sin(x+φ) 形式输出精确结果(如值域 [-√2, √2])"
        ),
        en = listOf(
            "Function analysis greatly expanded: beyond sin/cos/tan, now supports symbolic analysis for sin(x)+cos(x), polynomials (xⁿ, linear, quadratic), reciprocal (1/x), sqrt, logarithm (ln/log), exponential (eˣ/aˣ), absolute value (abs), and csc/sec/cot",
            "Subscript unified to nᵢ (n subscript i) instead of n₁",
            "sin(x)+cos(x) type expressions are automatically merged into R·sin(x+φ) form for exact results (e.g. range [-√2, √2])"
        )
    ),
    ChangelogEntry(
        version = "2.5.0",
        date = "2026-09-06",
        zh = listOf(
            "函数分析改为符号化精确输出:sin/cos/tan 等三角函数现在显示精确数学表达式(如 x = πn₁, n₁ ∈ Z),而非数值采样的小数近似",
            "函数分析新增 5 个字段:水平渐近线、斜渐近线、奇偶性、周期、单调性",
            "未匹配到符号模式的函数仍回退到数值采样分析"
        ),
        en = listOf(
            "Function analysis now outputs exact symbolic expressions for sin/cos/tan (e.g. x = πn₁, n₁ ∈ Z) instead of decimal sampling approximations",
            "Added 5 new analysis fields: horizontal asymptotes, oblique asymptotes, parity, period, monotonicity",
            "Functions not matching a symbolic pattern still fall back to numerical sampling analysis"
        )
    ),
    ChangelogEntry(
        version = "2.4.0",
        date = "2026-09-06",
        zh = listOf(
            "新增「函数分析」界面:点击分析图标进入可滚动的分析面板,显示定义域、值域、X/Y 轴截距、极小值、极大值、拐点、垂直渐近线",
            "分析界面的 f₁ 图标变为返回按钮(< f₁),点击返回函数列表",
            "新增「隐藏公式」功能:点击函数行左侧 f₁ 图标可切换该函数曲线的显示/隐藏,隐藏时图标变灰并加划线",
            "函数分析通过数值采样自动计算,支持任意表达式"
        ),
        en = listOf(
            "New 'Function Analysis' screen: tap the analyze icon to open a scrollable panel showing domain, range, X/Y intercepts, minima, maxima, inflection points, and vertical asymptotes",
            "In analysis view, the f₁ icon morphs into a back button (< f₁); tap it to return to the function list",
            "New 'Hide formula' feature: tap the f₁ icon on a function row to toggle that curve's visibility; hidden functions show a grayed, struck-through icon",
            "Analysis is computed automatically via numerical sampling and supports arbitrary expressions"
        )
    ),
    ChangelogEntry(
        version = "2.3.0",
        date = "2026-09-05",
        zh = listOf(
            "术语更正:「三角学」→「三角函数」",
            "每个函数项常驻 3 个图标按钮(分析函数 / 更改样式 / 删除),输入未完成时按钮自动置灰禁用",
            "分享按钮在无函数时自动隐藏(空白图不提供分享)",
            "坐标轴 X/Y 轴增加箭头,并沿轴线绘制刻度(放大缩小依然可见)",
            "画布任意位置点击可显示该点坐标(带坐标气泡和圆点),平移/缩放时自动消失"
        ),
        en = listOf(
            "Terminology fix: '三角学' → '三角函数' (Trigonometry)",
            "Each function row has 3 always-visible icon buttons (analyze / style / delete); buttons are disabled (grayed out) while the expression is incomplete",
            "Share button auto-hides when no functions are plotted (blank graph can't be shared)",
            "X/Y axes now have arrowheads, and tick marks are drawn along each axis (persists through zoom/pan)",
            "Tap anywhere on the canvas to show a coordinate bubble + dot (auto-hides when panning/zooming)"
        )
    ),
    ChangelogEntry(
        version = "2.2.0",
        date = "2026-09-05",
        zh = listOf(
            "绘图界面:新增分享按钮(截图当前图形,支持微信/微信收藏/隔空传图/豆包/文件管理/第三方 APP/复制图片)",
            "绘图界面:切换到画布视图时自动提交已完成的表达式,无需每次回车",
            "绘图界面:每个函数项新增常驻 3 个图标按钮 — 分析函数 / 更改公式样式(颜色选择)/ 删除",
            "绘图界面:画布工具栏改用 Material 图标(拖动/分享/设置、右下角 +/−/归零)"
        ),
        en = listOf(
            "Plot screen: new Share button (captures current graph, supports WeChat / WeChat Favorites / nearby share / Doubao / Files / third-party apps / copy image)",
            "Plot screen: auto-commit a complete expression when switching to canvas view, no need to press Enter every time",
            "Plot screen: each function row now has 3 always-visible icon buttons — analyze function / change formula style (color picker) / delete",
            "Plot screen: canvas toolbar uses Material icons (drag / share / settings, +/−/reset at bottom-right)"
        )
    ),
    ChangelogEntry(
        version = "2.1.0",
        date = "2026-09-05",
        zh = listOf(
            "绘图界面:坐标轴新增刻度标签,自动计算美观步长显示数值,并标注原点与 x/y 轴",
            "绘图界面:支持双指捏合手势缩放(以手势中心为焦点)及单指拖动平移,不再依赖加/减按钮",
            "绘图界面:新增「图形选项」设置弹窗 — 窗口重置、X/Y 轴范围、角度单位(弧度/度/百分度)、线条粗细(细/中/粗/虚线)、图形主题(始终亮/匹配应用主题)",
            "绘图界面:键盘区上方新增三角学/不等式/函数标题栏,函数列表与键盘布局重新调整"
        ),
        en = listOf(
            "Plot screen: axis tick labels with auto-calculated nice steps, origin and x/y axis markers",
            "Plot screen: pinch-to-zoom (centered on gesture focus) and one-finger drag to pan, no longer limited to +/- buttons",
            "Plot screen: new 'Graph Options' dialog — window reset, X/Y axis range, angle unit (radians/degrees/gradians), line style (thin/medium/thick/dashed), graph theme (always light/match app theme)",
            "Plot screen: Trig/Inequality/Function title bar above keypad, restructured function-list and keyboard layout"
        )
    ),
    ChangelogEntry(
        version = "2.0.0",
        date = "2026-09-05",
        zh = listOf(
            "绘图界面:函数列表视图移除画布预览,画布仅在全屏画布视图中显示",
            "绘图界面:全屏画布支持鼠标拖动平移(拖动工具激活时可拖拽图表)",
            "绘图界面:右下角缩放控制完善 — 加号放大、减号缩小、归零按钮恢复默认视图",
            "绘图界面:三角学/不等式/函数下拉面板改为 DropdownMenu,点击同一按钮可正常收起"
        ),
        en = listOf(
            "Plot screen: removed canvas preview from function-list view; canvas only shown in fullscreen canvas mode",
            "Plot screen: fullscreen canvas supports mouse drag to pan (when the drag tool is active)",
            "Plot screen: improved zoom controls — + to zoom in, − to zoom out, reset button to restore default view",
            "Plot screen: Trig/Inequality/Function dropdowns now use DropdownMenu; clicking the same button properly toggles the panel closed"
        )
    ),
    ChangelogEntry(
        version = "1.9.0",
        date = "2026-09-05",
        zh = listOf(
            "绘图界面:修复函数输入光标定位,插入带括号的函数(如 sin()时光标自动定位到括号内部",
            "绘图界面:新增画布/函数列表视图切换,顶部栏图表按钮切换到全屏画布,fₓ 按钮返回函数列表",
            "绘图界面:全屏画布视图支持缩放控制(放大/缩小/重置)",
            "绘图界面:当前激活的视图按钮高亮显示(蓝色),便于辨识"
        ),
        en = listOf(
            "Plot screen: fix cursor positioning — when inserting a function with parentheses (e.g. sin(), cursor is placed inside the parentheses",
            "Plot screen: add Canvas/Function-list view toggle — graph icon switches to fullscreen canvas, fₓ button returns to function list",
            "Plot screen: fullscreen canvas view supports zoom controls (zoom in/out/reset)",
            "Plot screen: the active view button is highlighted in blue for clarity"
        )
    ),
    ChangelogEntry(
        version = "1.8.0",
        date = "2026-09-05",
        zh = listOf(
            "绘图界面:三角学/不等式/函数浮动面板定位修正,分别显示在对应按钮正下方",
            "绘图界面:顶部栏新增图形按钮与函数(fₓ)按钮"
        ),
        en = listOf(
            "Plot screen: fix floating panel positioning — Trig/Inequality/Function panels now appear directly below their respective buttons",
            "Plot screen: add Graph button and Function (fₓ) button in the top app bar"
        )
    ),
    ChangelogEntry(
        version = "1.7.0",
        date = "2026-09-05",
        zh = listOf(
            "绘图界面优化:三角学/不等式/函数三个下拉面板改为浮动弹窗,悬浮于按键面板上方",
            "绘图界面优化:顶部为函数列表,输入表达式按回车添加新函数到列表(每按一次回车新增一个)",
            "函数列表支持删除单个函数,每个函数用不同颜色标识(f₁蓝/f₂绿/f₃橙...)"
        ),
        en = listOf(
            "Plot screen: Trig/Inequality/Function dropdowns changed to floating popup panels above the keypad",
            "Plot screen: top area is a function list — pressing Enter adds a new function to the list (one per Enter press)",
            "Function list supports deleting individual functions, each with a distinct color (f₁ blue/f₂ green/f₃ orange...)"
        )
    ),
    ChangelogEntry(
        version = "1.6.0",
        date = "2026-09-05",
        zh = listOf(
            "导航抽屉重构为整体可滚动大列表,分为「计算器」「转换器」「设置」三个分组",
            "计算器分组:标准 / 科学 / 绘图 / 程序员 / 日期计算",
            "转换器分组:12 种类别(货币/体积/长度/重量/温度/能量/面积/速度/时间/功率/数据/压强/角度)直接列出",
            "设置分组:历史记录 / 内存 / 语言 / 主题 / 关于",
            "新增主题切换:浅色 / 深色 / 跟随系统,选择持久化保存",
            "绘图界面按参考图重构:多函数输入(f₁/f₂...)、分类下拉工具栏(三角学/不等式/函数)、完整数学按键面板"
        ),
        en = listOf(
            "Redesign navigation drawer as a single scrollable list with three groups: Calculator / Converter / Settings",
            "Calculator group: Standard / Scientific / Plot / Programmer / Date Calculation",
            "Converter group: all 12 categories listed directly (Currency/Volume/Length/Mass/Temperature/Energy/Area/Speed/Time/Power/Data/Pressure/Angle)",
            "Settings group: History / Memory / Language / Theme / About",
            "Add theme switcher: Light / Dark / Follow system, persisted across restarts",
            "Redesign Plot screen per reference: multi-function input (f₁/f₂...), category dropdown toolbar (Trig/Inequality/Function), full math keypad"
        )
    ),
    ChangelogEntry(
        version = "1.5.0",
        date = "2026-09-05",
        zh = listOf(
            "转换器从抽屉展开列表改为弹窗选择:点击「转换器」弹出完整类别列表(12 种),避免抽屉滚动区域过小"
        ),
        en = listOf(
            "Converter: replace drawer expansion list with popup picker — tap 'Converter' to open full category list (12 types), avoiding tiny scroll area"
        )
    ),
    ChangelogEntry(
        version = "1.4.1",
        date = "2026-09-05",
        zh = listOf(
            "修复导航抽屉无法滚动:中间转换器分组区域改为可滚动,底部功能项(历史/内存/语言/关于)固定显示"
        ),
        en = listOf(
            "Fix drawer not scrollable: converter group area now scrollable, bottom items (History/Memory/Language/About) stay fixed"
        )
    ),
    ChangelogEntry(
        version = "1.4.0",
        date = "2026-09-05",
        zh = listOf(
            "导航抽屉按参考图重构:计算器分组 + 转换器分组 + 底部功能项",
            "新增「绘图」模式:输入 sin(x) 或 x^2 等表达式,在 Canvas 上绘制 2D 函数图像",
            "转换器从独立模式改为抽屉分组,每个 Category 直接在抽屉里选择",
            "Category.entries 遍历所有 12 种转换器类别(货币/体积/长度/重量/温度/能量/面积/速度/时间/功率/数据/压强/角度)"
        ),
        en = listOf(
            "Redesign navigation drawer: Calculator group + Converter group + bottom items",
            "Add 'Plot' mode: enter sin(x) or x^2 to draw 2D function graphs on Canvas",
            "Converter moved from top-level mode to drawer group, select each Category directly",
            "All 12 converter categories (Currency/Volume/Length/Mass/Temperature/Energy/Area/Speed/Time/Power/Data/Pressure/Angle)"
        )
    ),
    ChangelogEntry(
        version = "1.3.0",
        date = "2026-09-05",
        zh = listOf(
            "日期计算界面按 Windows 参考图重构",
            "模式切换改为顶部下拉菜单(替换原来的两个按钮)",
            "加/减模式添加添加/减去 RadioButton 方向选择",
            "年/月/天改为数字下拉列表(替换原来的 +/- 按钮)",
            "日期格式本地化:中文显示 2026年9月5日,英文显示 Sep 5, 2026",
            "日期选择右侧加日历图标按钮",
            "日期差值相同时间显示「相同日期」"
        ),
        en = listOf(
            "Redesign Date screen per Windows reference",
            "Mode switch: dropdown menu at top (replaces two buttons)",
            "Add/Subtract mode: add Add/Subtract RadioButton direction",
            "Years/Months/Days: number dropdowns (replace +/- buttons)",
            "Localized date format: zh→2026年9月5日, en→Sep 5, 2026",
            "Add calendar icon button next to each date",
            "Show 'Same day' when both dates are equal in Difference mode"
        )
    ),
    ChangelogEntry(
        version = "1.2.0",
        date = "2026-09-05",
        zh = listOf(
            "修复转换器模式切换中文后仍显示英文:类别与单位名全部本地化",
            "修复单位选择下拉框/日期选择器白色背景:改为深色主题、字体改白色"
        ),
        en = listOf(
            "Fix Converter mode showing English after switching to Chinese: localize all category & unit names",
            "Fix white background of unit dropdown / date picker: switch to dark theme with white text"
        )
    ),
    ChangelogEntry(
        version = "1.1.0",
        date = "2026-09-05",
        zh = listOf(
            "新增中英文国际化:默认按系统语言自动识别",
            "导航栏新增「语言」切换项,可手动切换中/英文",
            "语言选择持久化:重启 App 后保留上次设置",
            "全界面文本(标准/科学/程序员/转换器/日期)完成本地化",
            "新增「关于」页面,展示版本号与更新日志"
        ),
        en = listOf(
            "Add Chinese/English i18n: auto-detect by system language",
            "Add 'Language' toggle in navigation drawer",
            "Persist language choice across app restarts",
            "Localize all UI text (Standard/Scientific/Programmer/Converter/Date)",
            "Add 'About' page with version and changelog"
        )
    ),
    ChangelogEntry(
        version = "1.0.0",
        date = "2026-08-01",
        zh = listOf(
            "首个版本:标准 / 科学 / 程序员 / 转换器 / 日期计算 五种模式",
            "支持角度模式 DEG/RAD/GRAD",
            "历史记录与内存功能"
        ),
        en = listOf(
            "Initial release: Standard / Scientific / Programmer / Converter / Date modes",
            "Support angle modes DEG/RAD/GRAD",
            "History and memory features"
        )
    )
)

@Composable
fun AboutScreen() {
    val s = LocalStrings.current
    val context = LocalContext.current
    val versionName = BuildConfig.VERSION_NAME
    val versionCode = BuildConfig.VERSION_CODE

    Column(
        Modifier
            .fillMaxSize()
            .background(DarkBg)
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // ---- App 名称 + 版本 ----
        Surface(
            color = PanelBg,
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                Modifier.padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Text(
                    s.appName,
                    color = TextPrimary,
                    fontSize = 26.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    s.aboutDesc,
                    color = TextSecondary,
                    fontSize = 13.sp,
                    modifier = Modifier.padding(top = 4.dp)
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    "$s.version: v$versionName ($versionCode)",
                    color = AccentBlue,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(Modifier.height(12.dp))
                Button(
                    onClick = { shareAppPackage(context, versionName) },
                    colors = ButtonDefaults.buttonColors(containerColor = AccentBlue),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.height(44.dp)
                ) {
                    Icon(Icons.Default.Share, contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text(s.shareApp, color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
                }
                Text(
                    s.shareAppHint,
                    color = TextMuted,
                    fontSize = 11.sp,
                    modifier = Modifier.padding(top = 2.dp)
                )
            }
        }

        // ---- 更新日志 ----
        Text(
            s.changelog,
            color = TextPrimary,
            fontSize = 18.sp,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(start = 4.dp)
        )

        CHANGELOG.forEach { entry ->
            Surface(
                color = PanelBg,
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(
                        Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            "v${entry.version}",
                            color = AccentBlue,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            entry.date,
                            color = TextMuted,
                            fontSize = 12.sp
                        )
                    }
                    Divider(color = Divider)
                    val items = if (currentLangCode(context) == "zh") entry.zh else entry.en
                    items.forEach { line ->
                        Row(Modifier.fillMaxWidth()) {
                            Text("•  ", color = AccentBlue, fontSize = 13.sp)
                            Text(line, color = TextSecondary, fontSize = 13.sp)
                        }
                    }
                }
            }
        }
    }
}

/** 读取持久化的语言码(若未设置则用系统检测值) */
private fun currentLangCode(context: android.content.Context): String {
    val prefs = context.getSharedPreferences("calculator_prefs", android.content.Context.MODE_PRIVATE)
    return prefs.getString("lang_code", null)
        ?: if (java.util.Locale.getDefault().language.startsWith("zh")) "zh" else "en"
}

/**
 * 把当前应用的安装包(APK)复制到缓存目录,并通过系统分享面板分享给好友。
 * 好友收到 .apk 文件后下载即可安装使用。
 */
private fun shareAppPackage(context: android.content.Context, versionName: String) {
    try {
        // 已安装 APK 的源路径
        val sourceApk = File(context.applicationInfo.sourceDir)
        if (!sourceApk.exists()) return

        val cacheDir = File(context.cacheDir, "share")
        if (!cacheDir.exists()) cacheDir.mkdirs()
        val outFile = File(cacheDir, "calculator-v$versionName.apk")

        // 复制到可分享的缓存位置(若已存在且大小一致则跳过)
        if (!outFile.exists() || outFile.length() != sourceApk.length()) {
            sourceApk.inputStream().use { input ->
                outFile.outputStream().use { output ->
                    input.copyTo(output)
                }
            }
        }

        val uri = FileProvider.getUriForFile(
            context,
            "com.microsoft.calculator.fileprovider",
            outFile
        )
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "application/vnd.android.package-archive"
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            putExtra(Intent.EXTRA_SUBJECT, context.getString(android.R.string.copy) ?: "Share App")
        }
        context.startActivity(Intent.createChooser(intent, "分享应用"))
    } catch (_: Exception) {
        // 分享失败时静默忽略
    }
}
