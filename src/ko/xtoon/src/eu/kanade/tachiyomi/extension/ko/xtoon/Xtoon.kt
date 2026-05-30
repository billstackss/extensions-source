package eu.kanade.tachiyomi.extension.ko.xtoon

import eu.kanade.tachiyomi.network.GET
import eu.kanade.tachiyomi.source.model.FilterList
import eu.kanade.tachiyomi.source.model.MangasPage
import eu.kanade.tachiyomi.source.model.Page
import eu.kanade.tachiyomi.source.model.SChapter
import eu.kanade.tachiyomi.source.model.SManga
import eu.kanade.tachiyomi.source.online.ParsedHttpSource
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import org.jsoup.nodes.Document
import org.jsoup.nodes.Element
import rx.Observable

class Xtoon : ParsedHttpSource() {

    override val name = "Xtoon"

    override val baseUrl = "https://t3.xtoon365.com"

    override val lang = "ko"

    override val supportsLatest = false

    override val client: OkHttpClient = network.client

    // 검색창에 "https://t3.xtoon365.com/comic/숫자" 주소를 직접 넣으면 바로 이동하는 기능입니다.
    override fun fetchSearchManga(page: Int, query: String, filters: FilterList): Observable<MangasPage> {
        return if (query.startsWith("http")) {
            val urlPath = query.substringAfter(baseUrl)
            val manga = SManga.create().apply {
                this.url = urlPath
                this.title = "Xtoon 만화"
            }
            Observable.just(MangasPage(listOf(manga), false))
        } else {
            Observable.just(MangasPage(emptyList(), false))
        }
    }

    override fun searchMangaRequest(page: Int, query: String, filters: FilterList): Request = throw UnsupportedOperationException()
    override fun searchMangaSelector(): String = throw UnsupportedOperationException()
    override fun searchMangaFromElement(element: Element): SManga = throw UnsupportedOperationException()
    override fun searchMangaNextPageSelector(): String? = null

    override fun popularMangaRequest(page: Int): Request = GET(baseUrl)
    override fun popularMangaSelector(): String = "div"
    override fun popularMangaFromElement(element: Element): SManga = SManga.create()
    override fun popularMangaNextPageSelector(): String? = null

    override fun latestUpdatesRequest(page: Int): Request = throw UnsupportedOperationException()
    override fun latestUpdatesSelector(): String = throw UnsupportedOperationException()
    override fun latestUpdatesFromElement(element: Element): SManga = throw UnsupportedOperationException()
    override fun latestUpdatesNextPageSelector(): String? = null

    // 만화 상세 페이지 정보 파싱
    override fun mangaDetailsParse(document: Document): SManga = SManga.create().apply {
        val titleTag = document.select("h5.fw-bold, h1, h2").first()
        title = titleTag?.text()?.trim() ?: "Xtoon 무명 만화"
        status = SManga.UNKNOWN
    }

    // 회차 목록 파싱
    override fun chapterListSelector(): String = "a.chapter-list-item"

    override fun chapterFromElement(element: Element): SChapter = SChapter.create().apply {
        url = element.attr("href")
        
        val strongTag = element.select("strong").first()?.clone()
        if (strongTag != null) {
            strongTag.select("label, i").remove() // 불필요한 배지 제거
            name = strongTag.text().trim()
        } else {
            name = "만화 회차"
        }
        date_upload = 0L
    }
    
    override fun chapterListParse(response: Response): List<SChapter> {
        return super.chapterListParse(response).reversed() // 1화부터 순서대로 정렬
    }

    // 뷰어 이미지 목록 파싱
    override fun pageListParse(document: Document): List<Page> {
        return document.select("img.lazy-read").mapIndexed { index, element ->
            val imgUrl = element.attr("data-original").ifEmpty { element.attr("src") }
            Page(index, "", imgUrl)
        }
    }

    override fun imageUrlParse(document: Document): String = throw UnsupportedOperationException()
}
