package it.elia.gestionespese.data.db

import androidx.room.TypeConverter
import java.time.LocalDate
import java.time.YearMonth

class Converters {
    @TypeConverter
    fun dateToText(date: LocalDate?): String? = date?.toString()

    @TypeConverter
    fun textToDate(text: String?): LocalDate? = text?.let { LocalDate.parse(it) }

    @TypeConverter
    fun monthToText(month: YearMonth?): String? = month?.toString()

    @TypeConverter
    fun textToMonth(text: String?): YearMonth? = text?.let { YearMonth.parse(it) }
}
