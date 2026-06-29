package com.minipos.data.repo

import com.minipos.data.dao.MeasureUnitDao
import com.minipos.data.entity.MeasureUnit
import kotlinx.coroutines.flow.Flow

/** Custom measurement units (BUILD_PLAN §3, P4.1). */
class UnitRepository(private val dao: MeasureUnitDao) {

    fun observeByShop(shopId: Long): Flow<List<MeasureUnit>> = dao.observeByShop(shopId)
    suspend fun getById(id: Long): MeasureUnit? = dao.getById(id)

    suspend fun add(shopId: Long, name: String): Long =
        dao.insert(MeasureUnit(shopId = shopId, name = name, createdAt = System.currentTimeMillis()))

    suspend fun update(unit: MeasureUnit) = dao.update(unit)
    suspend fun delete(unit: MeasureUnit) = dao.delete(unit)
}
