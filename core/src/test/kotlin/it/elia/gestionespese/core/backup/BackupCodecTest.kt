package it.elia.gestionespese.core.backup

import it.elia.gestionespese.core.BudgetMath
import it.elia.gestionespese.core.CategoryKind
import it.elia.gestionespese.core.FillMode
import it.elia.gestionespese.core.MovementKind
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test
import java.io.File
import java.time.LocalDate

class BackupCodecTest {

    private val realBackup: BackupData by lazy {
        val dir = System.getProperty("referenceDir") ?: "../reference"
        BackupCodec.decode(File(dir, "backup-2026-10-26.json").readText())
    }

    @Test
    fun `legge il backup reale`() {
        val data = realBackup
        assertEquals(10, data.settings.payday)
        assertEquals(4, data.categories.size)
        assertEquals(5, data.shortcuts.size)
        assertEquals(3, data.movements.size)

        val tasse = data.categories.single { it.id == "mvp2w7sof753at" }
        assertEquals(CategoryKind.OBIETTIVO, tasse.kind)
        assertEquals(FillMode.MANUALE, tasse.fill)
        assertEquals(80000L, tasse.target)
        assertEquals(LocalDate.of(2026, 12, 11), tasse.deadline)

        val stipendio = data.movements.single { it.kind == MovementKind.STIPENDIO }
        assertEquals(3, stipendio.allocations.size)
        assertEquals(1793010500127, stipendio.createdAt)
    }

    /** SPEC.md §9: saldi con i movimenti del backup reale. */
    @Test
    fun `saldi del backup reale`() {
        val data = realBackup
        val b = BudgetMath(data.movements, data.settings.payday, LocalDate.of(2026, 10, 26)).balances()
        val byName = data.categories.associate { it.name to b.of(it.id) }
        assertEquals(15000L, byName["Svago"])
        assertEquals(6000L, byName["Benzina"])
        assertEquals(5000L, byName["Colazione"])
        assertEquals(0L, byName["Tasse uni Dicembre"])
        assertEquals(139030, b.toAssign)
        assertEquals(165030, b.totalAccount)
    }

    @Test
    fun `esporta e reimporta senza perdite`() {
        val data = realBackup
        val text = BackupCodec.encode(data, "2026-10-26T12:00:00+01:00")
        assertEquals(data, BackupCodec.decode(text))
    }

    @Test
    fun `rifiuta file estranei`() {
        assertThrows(BackupFormatException::class.java) { BackupCodec.decode("{}") }
        assertThrows(BackupFormatException::class.java) { BackupCodec.decode("""{"format":"altro","version":1}""") }
        assertThrows(BackupFormatException::class.java) { BackupCodec.decode("non json") }
        assertThrows(BackupFormatException::class.java) {
            BackupCodec.decode(
                """{"format":"gestione-spese-backup","version":1,"movements":[{"id":"x","kind":"boh","date":"2026-01-01","amount":1}]}""",
            )
        }
    }

    @Test
    fun `nome del file`() {
        assertEquals("backup-gestione-spese-2026-10-26.json", BackupCodec.fileName(LocalDate.of(2026, 10, 26)))
    }
}
