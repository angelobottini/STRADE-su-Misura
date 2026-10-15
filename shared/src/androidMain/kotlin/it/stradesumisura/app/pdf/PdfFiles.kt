package it.stradesumisura.app.pdf

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.pdf.PdfRenderer
import android.net.Uri
import android.os.ParcelFileDescriptor
import androidx.core.content.FileProvider
import java.io.File

/** Sharing, saving and previewing of generated PDF files. */
object PdfFiles {

    fun contentUri(context: Context, file: File): Uri =
        FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)

    fun share(context: Context, file: File, subject: String, chooserTitle: String) {
        val uri = contentUri(context, file)
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "application/pdf"
            putExtra(Intent.EXTRA_STREAM, uri)
            putExtra(Intent.EXTRA_SUBJECT, subject)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(Intent.createChooser(intent, chooserTitle))
    }

    fun open(context: Context, file: File) {
        val uri = contentUri(context, file)
        val intent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uri, "application/pdf")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(Intent.createChooser(intent, "Apri PDF"))
    }

    /** Copies [file] into the document chosen by the user through the Storage Access Framework. */
    fun copyTo(context: Context, file: File, target: Uri) {
        context.contentResolver.openOutputStream(target)?.use { out ->
            file.inputStream().use { input -> input.copyTo(out) }
        }
    }

    /** Renders every page to a bitmap of [widthPx] pixels (for the in-app preview). */
    fun renderPages(file: File, widthPx: Int): List<Bitmap> {
        val descriptor = ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY)
        val pages = mutableListOf<Bitmap>()
        PdfRenderer(descriptor).use { renderer ->
            for (index in 0 until renderer.pageCount) {
                renderer.openPage(index).use { page ->
                    val height = (widthPx.toFloat() * page.height / page.width).toInt().coerceAtLeast(1)
                    val bitmap = Bitmap.createBitmap(widthPx, height, Bitmap.Config.ARGB_8888)
                    bitmap.eraseColor(Color.WHITE)
                    page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                    pages += bitmap
                }
            }
        }
        return pages
    }
}
