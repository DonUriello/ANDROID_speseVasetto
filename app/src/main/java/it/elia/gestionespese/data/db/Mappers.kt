package it.elia.gestionespese.data.db

import it.elia.gestionespese.core.Category
import it.elia.gestionespese.core.Movement
import it.elia.gestionespese.core.Shortcut

// Conversioni tra tabelle Room e modello di dominio di :core.

fun CategoryEntity.toDomain() = Category(id, name, kind, fill, quota, target, deadline, order)

fun Category.toEntity() = CategoryEntity(id, name, kind, fill, quota, target, deadline, order)

fun ShortcutEntity.toDomain() = Shortcut(id, name, amount, categoryId, order)

fun Shortcut.toEntity() = ShortcutEntity(id, name, amount, categoryId, order)

fun MovementWithAllocations.toDomain() = Movement(
    id = movement.id,
    kind = movement.kind,
    date = movement.date,
    amount = movement.amount,
    categoryId = movement.categoryId,
    note = movement.note,
    total = movement.total,
    createdAt = movement.createdAt,
    allocations = allocations.associate { it.categoryId to it.amount },
    refMonth = movement.refMonth,
)

fun Movement.toEntity() = MovementEntity(id, kind, date, amount, categoryId, note, total, createdAt, refMonth)

fun Movement.allocationEntities() = allocations
    .filterValues { it != 0L }
    .map { (categoryId, amount) -> MovementAllocationEntity(id, categoryId, amount) }
