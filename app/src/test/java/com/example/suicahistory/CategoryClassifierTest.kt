package com.example.suicahistory

import com.example.suicahistory.domain.CategoryClassifier
import org.junit.Assert.assertEquals
import org.junit.Test

class CategoryClassifierTest {

    @Test
    fun `コンビニ系店舗は正しく分類される`() {
        assertEquals("コンビニ", CategoryClassifier.classify("セブンイレブン新宿店"))
        assertEquals("コンビニ", CategoryClassifier.classify("ローソン渋谷"))
        assertEquals("コンビニ", CategoryClassifier.classify("ファミリーマート"))
    }

    @Test
    fun `交通系は正しく分類される`() {
        assertEquals("交通", CategoryClassifier.classify("入場 [渋谷]"))
        assertEquals("交通", CategoryClassifier.classify("バス乗車"))
        assertEquals("交通", CategoryClassifier.classify("東急東横線"))
    }

    @Test
    fun `飲食系は正しく分類される`() {
        assertEquals("飲食", CategoryClassifier.classify("スターバックス"))
        assertEquals("飲食", CategoryClassifier.classify("松屋"))
    }

    @Test
    fun `未知の店舗はその他になる`() {
        assertEquals("その他", CategoryClassifier.classify("謎のお店"))
    }

    @Test
    fun `チャージは正しく分類される`() {
        assertEquals("チャージ", CategoryClassifier.classify("チャージ"))
    }
}
