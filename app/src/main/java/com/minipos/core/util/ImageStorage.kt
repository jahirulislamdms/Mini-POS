package com.minipos.core.util

import android.content.Context
import android.net.Uri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.util.UUID

/**
 * Copies picked images into app storage and resolves their relative paths (CONVENTIONS §11).
 * We store only the RELATIVE path in the DB (never a gallery content:// uri) and load via Coil
 * from the resolved File.
 */
object ImageStorage {

    private fun shopDir(context: Context, shopId: Long): File =
        File(context.filesDir, "shop_$shopId")

    private fun productsDir(context: Context, shopId: Long): File =
        File(shopDir(context, shopId), "products")

    /** Resolve a stored relative path (e.g. "shop_3/logo.jpg") to an absolute File. */
    fun fileFor(context: Context, relativePath: String): File =
        File(context.filesDir, relativePath)

    /** Copy a picked logo into shop_<id>/logo.jpg; returns the relative path. */
    suspend fun copyShopLogo(context: Context, shopId: Long, source: Uri): String =
        withContext(Dispatchers.IO) {
            val dir = shopDir(context, shopId).apply { mkdirs() }
            val dest = File(dir, "logo.jpg")
            copy(context, source, dest)
            "shop_$shopId/logo.jpg"
        }

    /** Copy a picked product photo into shop_<id>/products/<uuid>.jpg; returns the relative path. */
    suspend fun copyProductPhoto(context: Context, shopId: Long, source: Uri): String =
        withContext(Dispatchers.IO) {
            val dir = productsDir(context, shopId).apply { mkdirs() }
            val name = "${UUID.randomUUID()}.jpg"
            val dest = File(dir, name)
            copy(context, source, dest)
            "shop_$shopId/products/$name"
        }

    /** Remove all files for a shop (used when a shop is deleted). */
    suspend fun deleteShopFiles(context: Context, shopId: Long) =
        withContext(Dispatchers.IO) {
            shopDir(context, shopId).deleteRecursively()
            Unit
        }

    /** Remove a single stored file by its relative path (e.g. a deleted product's photo). */
    suspend fun deleteFile(context: Context, relativePath: String) =
        withContext(Dispatchers.IO) {
            fileFor(context, relativePath).delete()
            Unit
        }

    private fun copy(context: Context, source: Uri, dest: File) {
        context.contentResolver.openInputStream(source)?.use { input ->
            dest.outputStream().use { output -> input.copyTo(output) }
        }
    }
}
