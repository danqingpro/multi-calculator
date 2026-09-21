package com.microsoft.calculator.engine

/**
 * 单位换算,对标 Windows 计算器的 Converter 模式。
 * 提供长度/面积/体积/质量/温度/时间/速度/数据/角度/压力/能量以及货币(mock)。
 * 每个类别内通过 "基准单位" 线性换算;温度为特例。
 *
 * 标签支持中英双语:[title]/[Unit.label] 为英文,[titleZh]/[Unit.labelZh] 为中文,
 * 通过 title(lang)/label(lang) 按语言码("zh"/"en")取值。
 */
object UnitConverter {

    enum class Category(val title: String, val titleZh: String) {
        LENGTH("Length", "长度"),
        AREA("Area", "面积"),
        VOLUME("Volume", "体积"),
        MASS("Mass and Weight", "质量与重量"),
        TEMPERATURE("Temperature", "温度"),
        TIME("Time", "时间"),
        SPEED("Speed", "速度"),
        DATA("Data", "数据"),
        ANGLE("Angle", "角度"),
        PRESSURE("Pressure", "压强"),
        ENERGY("Energy", "能量"),
        POWER("Power", "功率"),
        CURRENCY("Currency", "货币");

        fun title(lang: String): String = if (lang == "zh") titleZh else title
    }

    data class Unit(
        val name: String,
        val label: String,
        val toBase: Double,
        val fromBase: Double = 1.0 / toBase,
        val labelZh: String = label
    ) {
        fun label(lang: String): String = if (lang == "zh") labelZh else label
    }

    val units: Map<Category, List<Unit>> = mapOf(
        Category.LENGTH to listOf(
            Unit("nm", "Nanometer", 1e-9, labelZh = "纳米"),
            Unit("um", "Micrometer", 1e-6, labelZh = "微米"),
            Unit("mm", "Millimeter", 1e-3, labelZh = "毫米"),
            Unit("cm", "Centimeter", 1e-2, labelZh = "厘米"),
            Unit("m", "Meter", 1.0, labelZh = "米"),
            Unit("km", "Kilometer", 1e3, labelZh = "千米"),
            Unit("in", "Inch", 0.0254, labelZh = "英寸"),
            Unit("ft", "Foot", 0.3048, labelZh = "英尺"),
            Unit("yd", "Yard", 0.9144, labelZh = "码"),
            Unit("mi", "Mile", 1609.344, labelZh = "英里"),
            Unit("nmi", "Nautical Mile", 1852.0, labelZh = "海里")
        ),
        Category.AREA to listOf(
            Unit("mm2", "Square millimeter", 1e-6, labelZh = "平方毫米"),
            Unit("cm2", "Square centimeter", 1e-4, labelZh = "平方厘米"),
            Unit("m2", "Square meter", 1.0, labelZh = "平方米"),
            Unit("ha", "Hectare", 1e4, labelZh = "公顷"),
            Unit("km2", "Square kilometer", 1e6, labelZh = "平方千米"),
            Unit("in2", "Square inch", 0.00064516, labelZh = "平方英寸"),
            Unit("ft2", "Square foot", 0.09290304, labelZh = "平方英尺"),
            Unit("ac", "Acre", 4046.8564224, labelZh = "英亩"),
            Unit("mi2", "Square mile", 2589988.110336, labelZh = "平方英里")
        ),
        Category.VOLUME to listOf(
            Unit("ml", "Milliliter", 1e-3, labelZh = "毫升"),
            Unit("l", "Liter", 1.0, labelZh = "升"),
            Unit("m3", "Cubic meter", 1000.0, labelZh = "立方米"),
            Unit("tsp", "Teaspoon (US)", 0.00492892, labelZh = "茶匙(美)"),
            Unit("tbsp", "Tablespoon (US)", 0.01478676, labelZh = "汤匙(美)"),
            Unit("floz", "Fluid ounce (US)", 0.0295735, labelZh = "液量盎司(美)"),
            Unit("cup", "Cup (US)", 0.236588, labelZh = "杯(美)"),
            Unit("pt", "Pint (US)", 0.473176, labelZh = "品脱(美)"),
            Unit("qt", "Quart (US)", 0.946353, labelZh = "夸脱(美)"),
            Unit("gal", "Gallon (US)", 3.785411784, labelZh = "加仑(美)")
        ),
        Category.MASS to listOf(
            Unit("mcg", "Microgram", 1e-9, labelZh = "微克"),
            Unit("mg", "Milligram", 1e-6, labelZh = "毫克"),
            Unit("g", "Gram", 1e-3, labelZh = "克"),
            Unit("kg", "Kilogram", 1.0, labelZh = "千克"),
            Unit("t", "Metric ton", 1000.0, labelZh = "吨"),
            Unit("oz", "Ounce", 0.028349523125, labelZh = "盎司"),
            Unit("lb", "Pound", 0.45359237, labelZh = "磅"),
            Unit("st", "Stone", 6.35029318, labelZh = "英石")
        ),
        Category.TIME to listOf(
            Unit("ms", "Millisecond", 0.001, labelZh = "毫秒"),
            Unit("s", "Second", 1.0, labelZh = "秒"),
            Unit("min", "Minute", 60.0, labelZh = "分"),
            Unit("h", "Hour", 3600.0, labelZh = "小时"),
            Unit("day", "Day", 86400.0, labelZh = "天"),
            Unit("week", "Week", 604800.0, labelZh = "周"),
            Unit("month", "Month", 2629800.0, labelZh = "月"),
            Unit("year", "Year", 31557600.0, labelZh = "年")
        ),
        Category.SPEED to listOf(
            Unit("mps", "Meter/second", 1.0, labelZh = "米/秒"),
            Unit("kmh", "Kilometer/hour", 0.277777778, labelZh = "千米/小时"),
            Unit("mph", "Mile/hour", 0.44704, labelZh = "英里/小时"),
            Unit("fps", "Foot/second", 0.3048, labelZh = "英尺/秒"),
            Unit("knot", "Knot", 0.514444444, labelZh = "节"),
            Unit("mach", "Mach", 343.0, labelZh = "马赫")
        ),
        Category.DATA to listOf(
            Unit("bit", "Bit", 0.125, labelZh = "比特"),
            Unit("byte", "Byte", 1.0, labelZh = "字节"),
            Unit("kb", "Kilobyte", 1000.0, labelZh = "千字节"),
            Unit("mb", "Megabyte", 1e6, labelZh = "兆字节"),
            Unit("gb", "Gigabyte", 1e9, labelZh = "吉字节"),
            Unit("tb", "Terabyte", 1e12, labelZh = "太字节"),
            Unit("kib", "Kibibyte", 1024.0, labelZh = "千比字节"),
            Unit("mib", "Mebibyte", 1048576.0, labelZh = "兆比字节"),
            Unit("gib", "Gibibyte", 1.073741824e9, labelZh = "吉比字节"),
            Unit("tib", "Tebibyte", 1.099511627776e12, labelZh = "太比字节")
        ),
        Category.ANGLE to listOf(
            Unit("deg", "Degree", 1.0, labelZh = "度"),
            Unit("rad", "Radian", 57.29577951308232, labelZh = "弧度"),
            Unit("grad", "Gradian", 0.9, labelZh = "梯度"),
            Unit("arcmin", "Arcminute", 0.0166666667, labelZh = "角分"),
            Unit("arcsec", "Arcsecond", 0.000277777778, labelZh = "角秒"),
            Unit("rev", "Revolution", 360.0, labelZh = "转")
        ),
        Category.PRESSURE to listOf(
            Unit("pa", "Pascal", 1.0, labelZh = "帕斯卡"),
            Unit("kpa", "Kilopascal", 1000.0, labelZh = "千帕"),
            Unit("mpa", "Megapascal", 1e6, labelZh = "兆帕"),
            Unit("bar", "Bar", 100000.0, labelZh = "巴"),
            Unit("atm", "Atmosphere", 101325.0, labelZh = "标准大气压"),
            Unit("psi", "Pound/sq inch", 6894.757293168, labelZh = "磅/平方英寸"),
            Unit("mmhg", "mm of mercury", 133.322387415, labelZh = "毫米汞柱"),
            Unit("torr", "Torr", 133.322368421, labelZh = "托")
        ),
        Category.ENERGY to listOf(
            Unit("j", "Joule", 1.0, labelZh = "焦耳"),
            Unit("kj", "Kilojoule", 1000.0, labelZh = "千焦"),
            Unit("cal", "Calorie", 4.184, labelZh = "卡路里"),
            Unit("kcal", "Kilocalorie", 4184.0, labelZh = "千卡"),
            Unit("wh", "Watt-hour", 3600.0, labelZh = "瓦时"),
            Unit("kwh", "Kilowatt-hour", 3.6e6, labelZh = "千瓦时"),
            Unit("ev", "Electronvolt", 1.602176634e-19, labelZh = "电子伏特"),
            Unit("btu", "BTU", 1055.05585262, labelZh = "英热单位"),
            Unit("ftlb", "Foot-pound", 1.3558179483314, labelZh = "英尺-磅")
        ),
        Category.POWER to listOf(
            Unit("w", "Watt", 1.0, labelZh = "瓦"),
            Unit("kw", "Kilowatt", 1000.0, labelZh = "千瓦"),
            Unit("mw", "Megawatt", 1e6, labelZh = "兆瓦"),
            Unit("hp", "Horsepower (US)", 745.699872, labelZh = "马力(美制)"),
            Unit("hpm", "Horsepower (metric)", 735.49875, labelZh = "马力(公制)"),
            Unit("btumin", "BTU/minute", 17.5842667, labelZh = "BTU/分钟"),
            Unit("ftlbmin", "Foot-pound/minute", 0.02259697, labelZh = "磅英尺/分钟"),
            Unit("kgfm", "kgf·m/s", 9.80665, labelZh = "千克力·米/秒")
        ),
        // 真实货币(以 CNY 为基准,汇率通过 CurrencyApiService 动态获取)
        Category.CURRENCY to listOf(
            Unit("CNY", "Chinese Yuan", 1.0, labelZh = "人民币"),
            Unit("USD", "US Dollar", 0.15, labelZh = "美元"),
            Unit("EUR", "Euro", 0.13, labelZh = "欧元"),
            Unit("JPY", "Japanese Yen", 20.0, labelZh = "日元"),
            Unit("HKD", "Hong Kong Dollar", 1.17, labelZh = "港元"),
            Unit("GBP", "Pound Sterling", 0.11, labelZh = "英镑"),
            Unit("AUD", "Australian Dollar", 0.21, labelZh = "澳元"),
            Unit("CAD", "Canadian Dollar", 0.21, labelZh = "加元"),
            Unit("CHF", "Swiss Franc", 0.12, labelZh = "瑞士法郎"),
            Unit("SGD", "Singapore Dollar", 0.19, labelZh = "新加坡元"),
            Unit("KRW", "South Korean Won", 200.0, labelZh = "韩元"),
            Unit("RUB", "Russian Ruble", 12.5, labelZh = "俄罗斯卢布"),
            Unit("THB", "Thai Baht", 5.0, labelZh = "泰铢"),
            Unit("NZD", "New Zealand Dollar", 0.23, labelZh = "新西兰元"),
            Unit("TWD", "New Taiwan Dollar", 4.7, labelZh = "新台币"),
            Unit("INR", "Indian Rupee", 12.5, labelZh = "印度卢比"),
            Unit("MYR", "Malaysian Ringgit", 0.61, labelZh = "马来西亚林吉特"),
            Unit("VND", "Vietnamese Dong", 3400.0, labelZh = "越南盾")
        )
    )

    // 温度单位:(name, 英文, 中文, 符号)
    private val temperatureUnitDefs = listOf(
        Triple("Celsius", "摄氏度", "°C"),
        Triple("Fahrenheit", "华氏度", "°F"),
        Triple("Kelvin", "开尔文", "K")
    )

    /** 按语言返回温度单位 (符号, 标签) */
    fun temperatureUnits(lang: String): List<Pair<String, String>> =
        temperatureUnitDefs.map { it.third to (if (lang == "zh") it.second else it.first) }

    // 动态汇率: 货币代码 -> 1 CNY = X 目标货币
    private val currencyRates = mutableMapOf<String, Double>()

    /** 更新实时汇率(API 返回的 rates 已以 CNY 为基准) */
    fun updateCurrencyRates(rates: Map<String, Double>) {
        synchronized(currencyRates) {
            currencyRates.clear()
            currencyRates.putAll(rates)
        }
    }

    /** 货币是否已有实时汇率 */
    fun hasLiveRates(): Boolean = synchronized(currencyRates) { currencyRates.isNotEmpty() }

    fun convert(value: Double, category: Category, fromIdx: Int, toIdx: Int): Double {
        if (category == Category.TEMPERATURE) {
            return convertTemp(value, fromIdx, toIdx)
        }
        val list = units[category] ?: return value
        val from = list.getOrNull(fromIdx) ?: return value
        val to = list.getOrNull(toIdx) ?: return value
        if (category == Category.CURRENCY) {
            val rates = synchronized(currencyRates) { currencyRates.toMap() }
            if (rates.isNotEmpty()) {
                val fromRate = rates[from.name] ?: return value
                val toRate = rates[to.name] ?: return value
                return value * toRate / fromRate
            }
        }
        val base = value * from.toBase
        return base / to.toBase
    }

    private fun convertTemp(value: Double, fromIdx: Int, toIdx: Int): Double {
        val celsius = when (fromIdx) {
            0 -> value // C
            1 -> (value - 32) * 5.0 / 9.0 // F
            2 -> value - 273.15 // K
            else -> value
        }
        return when (toIdx) {
            0 -> celsius
            1 -> celsius * 9.0 / 5.0 + 32
            2 -> celsius + 273.15
            else -> celsius
        }
    }
}
