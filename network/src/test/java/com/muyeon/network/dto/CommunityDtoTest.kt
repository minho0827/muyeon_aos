package com.muyeon.network.dto

import com.muyeon.network.dto.community.CommunityCommentDto
import com.muyeon.network.dto.community.CommunityPostDto
import com.squareup.moshi.Moshi
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class CommunityDtoTest {
    private val moshi = Moshi.Builder().build()

    @Test fun `글 — 내 글 판정·사진·익명`() {
        val json = """{"id":5,"category":"CHAT","title":"t","body":"b","images":["","/uploads/x.jpg"],"authorId":9,
            "authorName":"무연","authorImage":null,"authorIsTeacher":true,"likeCount":2,"commentCount":3,"viewCount":10,
            "likedByMe":true,"scrappedByMe":false,"createdAt":"2026-11-07T00:00:00.000Z"}"""
        val dto = moshi.adapter(CommunityPostDto::class.java).fromJson(json)!!
        val mine = dto.toPost(9L)!!
        assertTrue(mine.isMine)
        assertEquals("/uploads/x.jpg", mine.summary.image)
        assertEquals(listOf("/uploads/x.jpg"), mine.images)
        assertTrue(mine.summary.author.isTeacher)
        assertFalse(dto.toPost(1L)!!.isMine)
        assertFalse(dto.toPost(null)!!.isMine)
    }

    @Test fun `삭제된 댓글 — 작성자 없음`() {
        val json = """{"id":1,"parentId":null,"authorName":"","body":"삭제된 댓글입니다.","likeCount":0,"deleted":true,"replyCount":2}"""
        val c = moshi.adapter(CommunityCommentDto::class.java).fromJson(json)!!.toDomain(9L)!!
        assertTrue(c.deleted)
        assertNull(c.author.name)
        assertFalse(c.isMine)
        assertEquals(2, c.replyCount)
    }
}
