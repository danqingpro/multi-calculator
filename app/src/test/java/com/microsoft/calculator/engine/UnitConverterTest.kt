package com.microsoft.calculator.engine

import org.junit.Assert.assertEquals
import org.junit.Test

class UnitConverterTest {

    @Test
    fun lengthKmToMile() {
        // 1 km = 0.621371 mile
        val r = UnitConverter.convert(1.0, UnitConverter.Category.LENGTH, 5, 9) // km idx5, mi idx9
        assertEquals(0.621371, r, 1e-3)
    }

    @Test
    fun lengthMeterToFeet() {
        // 1 m = 3.28084 ft
        val r = UnitConverter.convert(1.0, UnitConverter.Category.LENGTH, 4, 7) // m idx4, ft idx7
        assertEquals(3.28084, r, 1e-3)
    }

    @Test
    fun temperatureCF() {
        // 0 C = 32 F
        val r = UnitConverter.convert(0.0, UnitConverter.Category.TEMPERATURE, 0, 1)
        assertEquals(32.0, r, 1e-6)
        // 100 C = 212 F
        val r2 = UnitConverter.convert(100.0, UnitConverter.Category.TEMPERATURE, 0, 1)
        assertEquals(212.0, r2, 1e-6)
    }

    @Test
    fun temperatureKelvin() {
        // 0 C = 273.15 K
        val r = UnitConverter.convert(0.0, UnitConverter.Category.TEMPERATURE, 0, 2)
        assertEquals(273.15, r, 1e-3)
    }

    @Test
    fun dataByteToBit() {
        // 1 byte = 8 bit
        val r = UnitConverter.convert(1.0, UnitConverter.Category.DATA, 1, 0)
        assertEquals(8.0, r, 1e-6)
    }

    @Test
    fun massKgToLb() {
        // 1 kg = 2.20462 lb
        val r = UnitConverter.convert(1.0, UnitConverter.Category.MASS, 3, 6) // kg idx3, lb idx6
        assertEquals(2.20462, r, 1e-3)
    }

    @Test
    fun speedKmhToMph() {
        // 1 kmh = 0.621371 mph
        val r = UnitConverter.convert(1.0, UnitConverter.Category.SPEED, 1, 2)
        assertEquals(0.621371, r, 1e-3)
    }

    @Test
    fun currencyMock() {
        // 1 Mars = 1.7 Earth (Mars toBase=1.7 -> 1 Mars = 1.7 base, Earth toBase=1.0)
        val r = UnitConverter.convert(1.0, UnitConverter.Category.CURRENCY, 1, 0)
        assertEquals(1.7, r, 1e-6)
    }
}
