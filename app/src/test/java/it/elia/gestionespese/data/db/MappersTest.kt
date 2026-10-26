package it.elia.gestionespese.data.db

import it.elia.gestionespese.core.Category
import it.elia.gestionespese.core.CategoryKind
import it.elia.gestionespese.core.FillMode
import it.elia.gestionespese.core.Movement
import it.elia.gestionespese.core.MovementKind
import it.elia.gestionespese.core.Shortcut
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDate
import java.time.YearMonth

class MappersTest {

    @Test
    fun `budget e spese rapide andata e ritorno`() {
        val c = Category("g", "Tasse", CategoryKind.OBIETTIVO, FillMode.MANUALE, 80000, 80000, LocalDate.of(2026, 12, 11), 3)
        assertEquals(c, c.toEntity().toDomain())
        val s = Shortcut("s", "Caffe", 130, "c", 2)
        assertEquals(s, s.toEntity().toDomain())
    }

    @Test
    fun `movimento con quote andata e ritorno`() {
        val m = Movement(
            id = "m", kind = MovementKind.STIPENDIO, date = LocalDate.of(2026, 10, 27), amount = 165030,
            createdAt = 1793010500127, allocations = mapOf("a" to 15000, "b" to 6000), refMonth = YearMonth.of(2026, 11),
        )
        val back = MovementWithAllocations(m.toEntity(), m.allocationEntities()).toDomain()
        assertEquals(m, back)
    }

    @Test
    fun `le quote a zero non vengono salvate`() {
        val m = Movement("m", MovementKind.STIPENDIO, LocalDate.of(2026, 10, 27), 1000, allocations = mapOf("a" to 0, "b" to 500))
        assertEquals(listOf(MovementAllocationEntity("m", "b", 500)), m.allocationEntities())
    }
}
