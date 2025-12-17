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

// 1."선택된 도시" 상태를 관리한다.
// 2. 선택된 도시의 "사진 목록"을 DB에서 Flow로 관찰해서 UI에 제공한다.
// 3. "선택된 사진" 상태를 관리한다.
// 4. 선택된 사진의 "댓글 목록"을 DB에서 Flow로 관찰해서 UI에 제공한다.
// 5. 포토피커로 고른 이미지는 앱 내부 저장소(filesDir)에 복사한 뒤, DB에 경로를 저장한다.

/*
DB에 Dao 함수쓰면 Flow가 자동으로 새로운 값을 내보내고, 화면 최신화 위해서
room이랑 flow 사용
*/
class ArchiveViewModel(app: Application) : AndroidViewModel(app) {

    private val dao = AppDatabase.get(app).archiveDao()

    private val DEFAULT_CITY = "빈"
    private val INTERNAL_DIR_NAME = "archive_photos"

    //1. 도시 선택 상태
    private val selectedCityFlow = MutableStateFlow(DEFAULT_CITY)
    val selectedCity: StateFlow<String> =
        selectedCityFlow.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), DEFAULT_CITY)

    // 드롭다운에서 도시를 선택하면 호출
    fun setSelectedCity(city: String) {
        selectedCityFlow.value = city
    }

    // 2. 사진 추가 (포토피커 → 내부저장소 복사 → DB 저장)
    //포토피커로 받은 Uri를 앱 내부 저장소(filesDir)로 복사하고,
    //그 결과 파일의 경로(String)를 반환
    //앱 재실행하면 업로드한 사진이 자꾸 끊기는 현상 해결
    private fun copyUriToInternalFile(uri: Uri): String {
        val ctx = getApplication<Application>()
        val dir = File(ctx.filesDir, INTERNAL_DIR_NAME).apply { mkdirs() }
        val outFile = File(dir, "${UUID.randomUUID()}.jpg")

        ctx.contentResolver.openInputStream(uri).use { input ->
            requireNotNull(input) { "Failed to open input stream for uri=$uri" }
            outFile.outputStream().use { output ->
                input.copyTo(output)
            }
        }

        return outFile.absolutePath
    }

    //에뮬에서 고른 사진 DB에 추가
    fun addPickedPhoto(uri: Uri) {
        val city = selectedCity.value
        viewModelScope.launch {
            val internalPath = copyUriToInternalFile(uri)
            dao.insert(
                ArchivePhotoEntity(
                    cityName = city,
                    internalPath = internalPath,
                    createdAt = System.currentTimeMillis()
                )
            )
        }
    }


    // 3. 댓글 (선택된 사진 → 댓글 목록 조회 + 댓글 추가)
    private val selectedPhotoIdFlow = MutableStateFlow<Long?>(null)
    val selectedPhotoId: StateFlow<Long?> =
        selectedPhotoIdFlow.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    //사진 클릭하면 오버레이에 띄울 photoId 설정
    fun setSelectedPhoto(photoId: Long) {
        selectedPhotoIdFlow.value = photoId
    }

    val comments: StateFlow<List<ArchiveCommentEntity>> =
        selectedPhotoIdFlow
            .flatMapLatest { pid ->
                if (pid == null) flowOf(emptyList()) else dao.observeComments(pid)
            }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    //오버레이에서 댓글 추가 버튼 누르면 호출
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

    // 4. 도시별 사진 목록 (DB Flow → StateFlow)
    val photos: StateFlow<List<ArchivePhotoEntity>> =
        selectedCityFlow
            .flatMapLatest { city -> dao.observeByCity(city) }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
}