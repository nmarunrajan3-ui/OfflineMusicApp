package com.auramusic.offline.model

import java.util.UUID

data class CustomAlbum(
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val description: String = "",
    val customPhotoUri: String? = null,
    val trackIds: List<Long> = emptyList(),
    val createdAt: Long = System.currentTimeMillis()
)
