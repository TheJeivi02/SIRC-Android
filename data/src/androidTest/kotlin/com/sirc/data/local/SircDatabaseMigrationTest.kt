package com.sirc.data.local

import androidx.room.testing.MigrationTestHelper
import androidx.sqlite.db.SupportSQLiteDatabase
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertEquals
import org.junit.Test

class SircDatabaseMigrationTest {
    private val helper =
        MigrationTestHelper(
            InstrumentationRegistry.getInstrumentation(),
            SircDatabase::class.java,
        )

    @Test
    fun migracionV1AV3ConservaDatosYAgregaColumnas() {
        val name = "migration-test"
        val db: SupportSQLiteDatabase = helper.createDatabase(name, 1)

        db.execSQL(
            """
            INSERT INTO driver_config
                (id, costPerKm, costPerMinute, costPerTrip, currency, minProfit, minProfitPerHour)
            VALUES (1, 8.5, 0.4, 5.0, 'MXN', 2.0, 150.0)
            """.trimIndent(),
        )
        db.execSQL(
            """
            INSERT INTO overlay_config
                (id, showDecision, showProfit, showProfitPerHour, showProfitPerKm, showTripSummary,
                 compactMode, opacityPercent, ttlSeconds, positionXPercent, positionYPercent)
            VALUES (1, 1, 1, 1, 1, 1, 0, 80, 15, 50, 80)
            """.trimIndent(),
        )
        db.execSQL(
            """
            INSERT INTO offer_history
                (platform, timestampMillis, estimatedTotal, distanceKm, durationMin, estimatedProfit, decision, summary)
            VALUES ('UBER', 1700000000000, 125.0, 8.5, 22.0, 100.0, 'PROFITABLE', 'Viaje aceptado')
            """.trimIndent(),
        )
        db.close()

        val migrated =
            helper.runMigrationsAndValidate(
                name,
                3,
                true,
                SircMigrations.MIGRATION_1_2,
                SircMigrations.MIGRATION_2_3,
            )

        migrated.query("SELECT * FROM driver_config").use { cursor ->
            cursor.moveToFirst()
            assertEquals(1, cursor.getInt(cursor.getColumnIndexOrThrow("id")))
            assertEquals(8.5, cursor.getDouble(cursor.getColumnIndexOrThrow("costPerKm")), 0.001)
            assertEquals(2.0, cursor.getDouble(cursor.getColumnIndexOrThrow("minProfitPerKm")), 0.001)
            assertEquals(150.0, cursor.getDouble(cursor.getColumnIndexOrThrow("minProfitPerHour")), 0.001)
        }

        migrated.query("SELECT * FROM overlay_config").use { cursor ->
            cursor.moveToFirst()
            assertEquals(500, cursor.getInt(cursor.getColumnIndexOrThrow("historyLimit")))
            assertEquals(80, cursor.getInt(cursor.getColumnIndexOrThrow("opacityPercent")))
        }

        migrated.query("SELECT * FROM offer_history").use { cursor ->
            cursor.moveToFirst()
            assertEquals("UBER", cursor.getString(cursor.getColumnIndexOrThrow("platform")))
            assertEquals("PROFITABLE", cursor.getString(cursor.getColumnIndexOrThrow("decision")))
            assertEquals(100.0, cursor.getDouble(cursor.getColumnIndexOrThrow("estimatedProfit")), 0.001)
            assertEquals(null, cursor.getString(cursor.getColumnIndexOrThrow("offerType")))
            assertEquals(null, cursor.getString(cursor.getColumnIndexOrThrow("confidenceLevel")))
        }

        migrated.close()
    }
}
