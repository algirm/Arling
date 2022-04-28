package com.algirm.arling

import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Example local unit test, which will execute on the development machine (host).
 *
 * See [testing documentation](http://d.android.com/tools/testing).
 */
class ExampleUnitTest {

    private val WGS84_A = 6378137.0 // WGS 84 semi-major axis constant in meters

    private val WGS84_E2 = 0.00669437999014

    private val petugas1 = doubleArrayOf(-6.183039768588892, 106.67788586977257, 0.0)
    private val petugas2 = doubleArrayOf(-6.182117782777887, 106.67766887101084, 0.0)
    private val petugas3 = doubleArrayOf(39.0, -132.0, 0.0)
    private val petugas4 = doubleArrayOf(39.5, -132.0, 0.0)

    @Test
    fun addition_isCorrect() {
//        val agiEcef = LocationHelperTest.WSG84toECEF(petugas3[0], petugas3[1], petugas3[2])
//        println("ECEF Agi")
//        for (aa in agiEcef) {
//            println(aa)
//        }
//        val boyEcef = LocationHelperTest.WSG84toECEF(petugas4[0], petugas4[1], petugas4[2])
//        val agiEnuBoy = LocationHelperTest.ECEFtoENU(
//            petugas3[0],
//            petugas3[1],
//            petugas3[2],
//            agiEcef,
//            boyEcef
//        )
//        println("ENU AgiBoy")
//        for (aa in agiEnuBoy) {
//            println(aa)
//        }
//        println()

        val agiEnu2Boy = LocationHelperTest2.ECEFtoENU(
            petugas3[0],
            petugas3[1],
            petugas3[2],
            petugas4[0],
            petugas4[1],
            petugas4[2]
        )


        println("ENU2 AgiBoy")
        for (aa in agiEnu2Boy) {
            println(aa)
        }
        println()
        assertEquals(true, 1.toString())
    }
}