package com.example.travelog

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.io.File
import java.util.UUID

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

    // DB 접근용 DAO
    private val dao = AppDatabase.get(app).archiveDao()

    // 기본으로 보여줄 도시(원하면 화면에서 첫 도시로 바꿔도 됨)
    private val defaultCity = "빈"

    // 내부 저장 폴더명 (filesDir/archive_photos)저장
    private val internalDirName = "archive_photos"

    // ----------------------------
    // 1) 도시 선택 상태
    // ----------------------------

    // 현재 선택된 도시 (드롭다운에서 바뀜)
    private val _selectedCity = MutableStateFlow(defaultCity)
    val selectedCity: StateFlow<String> = _selectedCity.asStateFlow()

    // 드롭다운에서 도시를 선택하면 호출
    fun setSelectedCity(city: String) {
        _selectedCity.value = city
    }

    // 2. 사진 목록 (도시별)

    //선택된 도시가 바뀌면, 그 도시의 사진 목록 Flow로 자동 갱신
    //DAO가 Flow를 내보내고
    //Compose에서 collectAsState()로 받으면 화면이 자동으로 업데이트됨

    val photos: StateFlow<List<ArchivePhotoEntity>> =
        _selectedCity
            .flatMapLatest { city -> dao.observeByCity(city) }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    // 3. 사진 추가 (포토피커 → 내부 저장소 복사 → DB 저장)

    //포토피커로 받은 Uri를 내부 저장소(filesDir)로 복사하고,
    //그 결과 파일의 경로(String)를 반환.
    //앱 재실행시 날라가는 문제 해결 위해서 내부 파일로 복사해두는 방식 선택
    private fun copyUriToInternalFile(uri: Uri): String {
        val ctx = getApplication<Application>()

        // filesDir/archive_photos/ 폴더 생성
        val dir = File(ctx.filesDir, internalDirName).apply { mkdirs() }

        // 파일명은 충돌 방지용 UUID 사용
        val outFile = File(dir, "${UUID.randomUUID()}.jpg")

        ctx.contentResolver.openInputStream(uri).use { input ->
            requireNotNull(input) { "Failed to open input stream for uri=$uri" }
            outFile.outputStream().use { output ->
                input.copyTo(output)
            }
        }

        return outFile.absolutePath
    }

    //+ 버튼에서 포토피커로 사진을 고른 뒤 호출.
    //내부저장소에 복사
    //현재 선택 도시(cityName)에 속한 사진으로 DB에 insert
    fun addPickedPhoto(uri: Uri) {
        val city = selectedCity.value
        viewModelScope.launch {
            val internalPath = copyUriToInternalFile(uri)
            dao.insert(
                ArchivePhotoEntity(
                    cityName = city,
                    internalPath = internalPath
                )
            )
        }
    }

    // 4. 댓글 (선택된 사진 → 댓글 목록/추가)

    // 현재 오버레이로 열려있는 사진 id
    private val _selectedPhotoId = MutableStateFlow<Long?>(null)
    val selectedPhotoId: StateFlow<Long?> = _selectedPhotoId.asStateFlow()

    // 그리드에서 사진을 클릭하면 호출: 오버레이에 띄울 photoId 지정
    fun setSelectedPhoto(photoId: Long) {
        _selectedPhotoId.value = photoId
    }

    //선택된 photoId가 바뀌면 그 사진의 댓글 목록이 자동 갱신됨
    //photoId가 null이면(아직 선택 안함) 빈 리스트
    val comments: StateFlow<List<ArchiveCommentEntity>> =
        _selectedPhotoId
            .flatMapLatest { pid ->
                if (pid == null) flowOf(emptyList()) else dao.observeComments(pid)
            }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    //오버레이에서 댓글 전송 버튼 누르면 호출
    fun addComment(authorName: String, text: String) {
        val pid = _selectedPhotoId.value ?: return
        val t = text.trim()
        if (t.isEmpty()) return

        viewModelScope.launch {
            dao.insertComment(
                ArchiveCommentEntity(
                    photoId = pid,
                    authorName = authorName,
                    text = t
                )
            )
        }
    }
}