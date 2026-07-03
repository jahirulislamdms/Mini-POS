package com.minipos.feature.product

import com.minipos.data.entity.Product

/**
 * Shared Category/Subcategory multi-select filtering (Phases 24/28.1) — one implementation used
 * by the Products page and the Barcode Printing page. Union semantics: a product matches when
 * its category is among the selected categories OR its subcategory is among the selected
 * subcategories; no selection = all products.
 */
object ProductFilters {
    fun apply(list: List<Product>, categoryIds: Set<Long>, subCategoryIds: Set<Long>): List<Product> =
        if (categoryIds.isEmpty() && subCategoryIds.isEmpty()) {
            list
        } else {
            list.filter { p ->
                (p.categoryId?.let { it in categoryIds } == true) ||
                    (p.subCategoryId?.let { it in subCategoryIds } == true)
            }
        }
}
