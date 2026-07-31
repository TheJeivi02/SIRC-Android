# SIRC (Sistema Inteligente de Rentabilidad para Conductores)
# Reglas de ofuscación adicionales para el release.

# Room
-keep class * extends androidx.room.RoomDatabase
-dontwarn androidx.room.paging.**

# Hilt / Dagger
-keep class dagger.hilt.** { *; }
-dontwarn dagger.hilt.**

# Modelos usados vía reflection (Room/Flow)
-keep class com.sirc.domain.model.** { *; }
