package com.unava.dia.lightplay.data.remote

import com.google.firebase.firestore.FirebaseFirestore
import com.unava.dia.lightplay.data.entities.Song
import com.unava.dia.lightplay.other.AppConstants.SONG_LIST
import kotlinx.coroutines.tasks.await

class SongsDatabase {
    private val firestore = FirebaseFirestore.getInstance()
    private val songList = firestore.collection(SONG_LIST)

    suspend fun getSongs() : List<Song> {
        return try {
            songList.get().await().toObjects(Song::class.java)
        } catch (e: Exception) {
            emptyList()
        }
    }
}