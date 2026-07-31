package com.sirc.data.local

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

/** Migraciones de esquema de la base SIRC. */
object SircMigrations {
    /**
     * v1 → v2: `driver_config` incorpora perfil, vehículo, costos básicos,
     * plataformas y el objetivo de ganancia por km (`minProfitPerKm` sustituye
     * a `minProfit`). Se reconstruye la tabla para ser compatible con SQLite
     * antiguo (sin `RENAME COLUMN`).
     */
    val MIGRATION_1_2 =
        object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `driver_config_new` (
                        `id` INTEGER NOT NULL,
                        `costPerKm` REAL NOT NULL,
                        `costPerMinute` REAL NOT NULL,
                        `costPerTrip` REAL NOT NULL,
                        `currency` TEXT NOT NULL,
                        `name` TEXT,
                        `country` TEXT NOT NULL,
                        `city` TEXT NOT NULL,
                        `vehicleName` TEXT NOT NULL,
                        `brand` TEXT NOT NULL,
                        `model` TEXT NOT NULL,
                        `year` INTEGER NOT NULL,
                        `fuelType` TEXT NOT NULL,
                        `consumptionKmPerUnit` REAL NOT NULL,
                        `fuelPrice` REAL NOT NULL,
                        `maintenanceCostPerKm` REAL NOT NULL,
                        `additionalCosts` TEXT NOT NULL,
                        `platforms` TEXT NOT NULL,
                        `minProfitPerKm` REAL NOT NULL,
                        `minProfitPerHour` REAL NOT NULL,
                        PRIMARY KEY(`id`)
                    )
                    """.trimIndent(),
                )
                db.execSQL(
                    """
                    INSERT INTO `driver_config_new`
                        (id, costPerKm, costPerMinute, costPerTrip, currency, name,
                         country, city, vehicleName, brand, model, year, fuelType,
                         consumptionKmPerUnit, fuelPrice, maintenanceCostPerKm,
                         additionalCosts, platforms, minProfitPerKm, minProfitPerHour)
                    SELECT id, costPerKm, costPerMinute, costPerTrip, currency, NULL,
                           '', '', '', '', '', 2020, 'GASOLINE',
                           12.0, 24.0, 0.5,
                           '', '', minProfit, minProfitPerHour
                    FROM `driver_config`
                    """.trimIndent(),
                )
                db.execSQL("DROP TABLE `driver_config`")
                db.execSQL("ALTER TABLE `driver_config_new` RENAME TO `driver_config`")
            }
        }
}
