package forgelord.lesson2notice

import forgelord.page.stylesheet

/** The classes in NoticePage.css. There are none yet: a class you write there gets a constant here. */
object NoticePageClasses

/** NoticePage.css, for the page's `<style>`. Read once. */
val noticePageCss: String by lazy { stylesheet("forgelord/lesson2notice/NoticePage.css") }
