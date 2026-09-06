package com.muyeon.app.ui.survey

import android.content.Context
import android.content.Intent
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import android.text.Layout
import android.text.StaticLayout
import android.text.TextPaint
import androidx.core.content.FileProvider
import java.io.File
import java.io.FileOutputStream

/** PDF 한 줄 — 문항과 응답. iOS `SurveyPDFItem`. */
data class SurveyPdfItem(val question: String, val answer: String)

/**
 * 레슨 전 설문 응답 → PDF + 공유 — iOS `SurveyPDF.swift` 1:1.
 *  A4(595×842pt) · 여백 44 · 넘치면 페이지 분할. iOS 와 같은 좌표·글자 크기를 쓴다.
 *
 * ⚠️ iOS 는 UIGraphicsPDFRenderer, 안드로이드는 android.graphics.pdf.PdfDocument 로
 *   같은 문서를 그린다. 줄바꿈은 StaticLayout 이 맡는다(iOS boundingRect 대응).
 */
object SurveyPdf {

    private const val PAGE_W = 595
    private const val PAGE_H = 842
    private const val MARGIN = 44f
    private const val CONTENT_W = PAGE_W - MARGIN * 2

    private fun paint(size: Float, bold: Boolean, color: Int) = TextPaint().apply {
        isAntiAlias = true
        textSize = size
        typeface = Typeface.create(Typeface.SANS_SERIF, if (bold) Typeface.BOLD else Typeface.NORMAL)
        this.color = color
    }

    /**
     * 문서를 만들어 캐시에 저장하고 파일을 돌려준다. 실패하면 null.
     *  @param info 제목 아래 정보 라인(회원 이름·수업 일시·장소 등).
     */
    fun make(
        ctx: Context,
        title: String,
        subtitle: String?,
        info: List<String>,
        items: List<SurveyPdfItem>,
    ): File? = runCatching {
        val titleP = paint(20f, true, 0xFF101116.toInt())
        val subP = paint(12f, false, 0xFF6D6E71.toInt())
        val infoP = paint(13f, true, 0xFF101116.toInt())
        val qP = paint(14f, true, 0xFF101116.toInt())
        val aP = paint(13f, false, 0xFF333338.toInt())
        val lineP = Paint().apply { color = 0xFFC7C7CC.toInt(); strokeWidth = 0.6f }

        fun layout(text: String, p: TextPaint): StaticLayout =
            StaticLayout.Builder.obtain(text, 0, text.length, p, CONTENT_W.toInt())
                .setAlignment(Layout.Alignment.ALIGN_NORMAL)
                .setIncludePad(false)
                .build()

        val doc = PdfDocument()
        var pageNo = 1
        var page = doc.startPage(PdfDocument.PageInfo.Builder(PAGE_W, PAGE_H, pageNo).create())
        var canvas = page.canvas
        var y = MARGIN

        /** 한 덩어리를 그린다. 남은 높이가 모자라면 페이지를 넘긴다. */
        fun draw(text: String, p: TextPaint, gapAfter: Float) {
            val l = layout(text, p)
            if (y + l.height > PAGE_H - MARGIN) {
                doc.finishPage(page)
                pageNo += 1
                page = doc.startPage(PdfDocument.PageInfo.Builder(PAGE_W, PAGE_H, pageNo).create())
                canvas = page.canvas
                y = MARGIN
            }
            canvas.save()
            canvas.translate(MARGIN, y)
            l.draw(canvas)
            canvas.restore()
            y += l.height + gapAfter
        }

        draw(title, titleP, 6f)
        subtitle?.takeIf { it.isNotEmpty() }?.let { draw(it, subP, 6f) }
        info.filter { it.isNotEmpty() }.forEach { draw(it, infoP, 3f) }

        y += 6f
        canvas.drawLine(MARGIN, y, PAGE_W - MARGIN, y, lineP)
        y += 14f

        items.forEachIndexed { idx, item ->
            draw("${idx + 1}. ${item.question}", qP, 4f)
            draw(item.answer, aP, 16f)
        }

        doc.finishPage(page)

        // 공유 대상 디렉터리는 file_paths.xml 의 <cache-path name="shared"/> 와 맞춰야 한다.
        val dir = File(ctx.cacheDir, "shared").apply { mkdirs() }
        val file = File(dir, "레슨전설문_${System.currentTimeMillis() / 1000}.pdf")
        FileOutputStream(file).use { doc.writeTo(it) }
        doc.close()
        file
    }.getOrNull()

    /** 공유 시트 — iOS UIActivityViewController 대응. */
    fun share(ctx: Context, file: File) {
        val uri = FileProvider.getUriForFile(ctx, "${ctx.packageName}.fileprovider", file)
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "application/pdf"
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        ctx.startActivity(Intent.createChooser(intent, "설문 PDF 공유"))
    }
}
