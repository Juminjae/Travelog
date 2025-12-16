package com.example.travelog

import android.app.Application
import android.net.Uri
import java.io.File
import java.util.UUID
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch

class ArchiveViewModel(app: Application) : AndroidViewModel(app) {

    private val dao = AppDatabase.get(app).archiveDao()

    private val selectedCityFlow = MutableStateFlow("빈")
    val selectedCity: StateFlow<String> =
        selectedCityFlow.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), "빈")

    fun setSelectedCity(city: String) {
        selectedCityFlow.value = city
    }

    private fun copyUriToInternalFile(uri: Uri): String {
        val ctx = getApplication<Application>()
        val dir = File(ctx.filesDir, "archive_photos").apply { mkdirs() }
        val outFile = File(dir, "${UUID.randomUUID()}.jpg")

        ctx.contentResolver.openInputStream(uri).use { input ->
            requireNotNull(input) { "Failed to open input stream for uri=$uri" }
            outFile.outputStream().use { output ->
                input.copyTo(output)
            }
        }

        return outFile.absolutePath
    }

    fun addUriPhoto(uriString: String) {
        // Backward compatible entry point.
        // If this is a temporary Photo Picker URI, copy it to internal storage first.
        val city = selectedCity.value

        viewModelScope.launch {
            val storedPathOrUri: String? = try {
                when {
                    // 이미 내부 저장소 absolute path로 들어온 경우
                    uriString.startsWith("/") -> uriString

                    // android.resource:// 는 drawable 리소스용. 이 경우에는 addDummyPhoto를 쓰는 게 맞지만,
                    // 혹시 들어오면 그대로 저장하지 않고 null 처리(placeholder 유지)
                    uriString.startsWith("android.resource://") -> null

                    // content:// 등은 Uri로 열어서 내부 파일로 복사
                    else -> {
                        val uri = Uri.parse(uriString)
                        copyUriToInternalFile(uri)
                    }
                }
            } catch (_: Throwable) {
                null
            }

            if (storedPathOrUri != null) {
                dao.insert(
                    ArchivePhotoEntity(
                        cityName = city,
                        sourceType = "internalFile",
                        uriString = storedPathOrUri,
                        localResName = null,
                        createdAt = System.currentTimeMillis()
                    )
                )
            }
        }
    }

    fun addUriPhoto(uri: Uri) {
        val city = selectedCity.value
        viewModelScope.launch {
            val path = try {
                copyUriToInternalFile(uri)
            } catch (_: Throwable) {
                null
            }

            if (path != null) {
                dao.insert(
                    ArchivePhotoEntity(
                        cityName = city,
                        sourceType = "internalFile",
                        uriString = path,
                        localResName = null,
                        createdAt = System.currentTimeMillis()
                    )
                )
            }
        }
    }

    //댓글 뷰모델
    private val selectedPhotoIdFlow = MutableStateFlow<Long?>(null)
    val selectedPhotoId: StateFlow<Long?> =
        selectedPhotoIdFlow.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    fun setSelectedPhoto(photoId: Long) {
        selectedPhotoIdFlow.value = photoId
    }

    val comments: StateFlow<List<ArchiveCommentEntity>> =
        selectedPhotoIdFlow
            .flatMapLatest { pid ->
                if (pid == null) flowOf(emptyList()) else dao.observeComments(pid)
            }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun addComment(authorName: String, text: String) {
        val pid = selectedPhotoIdFlow.value ?: return
        val t = text.trim()
        if (t.isEmpty()) return

        viewModelScope.launch {
            dao.insertComment(
                ArchiveCommentEntity(
                    photoId = pid,
                    authorName = authorName,
                    text = t,
                    createdAt = System.currentTimeMillis()
                )
            )
        }
    }

    val photos: StateFlow<List<ArchivePhotoEntity>> =
        selectedCityFlow
            .flatMapLatest { city -> dao.observeByCity(city) }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun addDummyPhoto(city: String, localResName: String) {
        viewModelScope.launch {
            dao.insert(
                ArchivePhotoEntity(
                    cityName = city,
                    sourceType = "localRes",
                    localResName = localResName
                )
            )
        }
    }
}