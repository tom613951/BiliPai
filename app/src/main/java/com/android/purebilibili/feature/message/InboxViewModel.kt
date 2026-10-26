package com.android.purebilibili.feature.message

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.android.purebilibili.core.coroutines.AccountScopedRequests
import com.android.purebilibili.core.store.AccountSessionIdentity
import com.android.purebilibili.core.store.TokenManager
import com.android.purebilibili.data.model.response.MessageFeedUnreadData
import com.android.purebilibili.data.model.response.SessionItem
import com.android.purebilibili.data.repository.ImMenuItem
import com.android.purebilibili.data.repository.ImSession
import com.android.purebilibili.data.repository.MessageImRepository
import com.android.purebilibili.data.repository.MessageRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

/** 用户缓存属于当前账号，会话自带资料优先用于首帧。 */
data class UserBasicInfo(val mid: Long, val name: String, val face: String)

data class InboxUiState(
    val accountIdentity: AccountSessionIdentity = TokenManager.accountIdentity.value,
    val isLoading: Boolean = true,
    val isRefreshing: Boolean = false,
    val sessions: List<SessionItem> = emptyList(),
    val entries: Map<String, ImSession> = emptyMap(),
    val feedUnreadData: MessageFeedUnreadData? = null,
    val hasMore: Boolean = false,
    val isLoadingMore: Boolean = false,
    val isBatchOperating: Boolean = false,
    val error: String? = null,
    val loadMoreError: String? = null,
    val operationError: String? = null,
    val page: Int = 1,
    val endTs: Long = 0,
    val userInfoMap: Map<Long, UserBasicInfo> = emptyMap(),
    val pageType: Int = 1,
    val title: String = "消息",
    val menu: List<ImMenuItem> = emptyList(),
    val outside: List<ImMenuItem> = emptyList(),
)

class InboxViewModel : ViewModel() {
    private val _uiState = MutableStateFlow(InboxUiState())
    val uiState = _uiState.asStateFlow()
    private val requests = AccountScopedRequests(viewModelScope)
    private var generation = 0L
    private var listJob: Job? = null
    private var moreJob: Job? = null
    private var pagination = byteArrayOf()
    private val parents = mutableListOf<Pair<Int, String>>()

    init {
        requests.observe {
            generation++
            parents.clear()
            pagination = byteArrayOf()
            _uiState.value = InboxUiState(accountIdentity = requests.identity)
            loadSessions()
        }
        loadSessions()
    }

    fun entry(session: SessionItem): ImSession? = _uiState.value.entries[InboxSessionPaginationPolicy.resolveSessionKey(session)]

    fun openFolder(session: SessionItem): Boolean {
        val folder = entry(session)?.folderPage ?: return false
        parents += _uiState.value.pageType to _uiState.value.title
        navigatePage(folder, session.account_info?.name.orEmpty().ifBlank { "消息" })
        return true
    }

    fun back(): Boolean {
        val parent = parents.removeLastOrNull() ?: return false
        navigatePage(parent.first, parent.second)
        return true
    }

    private fun navigatePage(pageType: Int, title: String) {
        listJob?.cancel()
        moreJob?.cancel()
        generation++
        pagination = byteArrayOf()
        _uiState.update { InboxUiState(accountIdentity = it.accountIdentity, pageType = pageType, title = title, feedUnreadData = it.feedUnreadData) }
        loadSessions()
    }

    fun loadSessions() = load(refresh = false)
    fun refresh() = load(refresh = true)
    private fun load(refresh: Boolean) {
        listJob?.cancel()
        moreJob?.cancel()
        val version = ++generation
        val pageType = _uiState.value.pageType
        _uiState.update { it.copy(isLoading = it.sessions.isEmpty(), isRefreshing = refresh, isLoadingMore = false, error = null, loadMoreError = null) }
        listJob = requests.launch {
            coroutineScope {
                val feed = async { MessageRepository.getFeedUnread() }
                val sessions = async { MessageImRepository.sessions(pageType) }
                val result = sessions.await()
                val feedResult = feed.await()
                if (version != generation || !requests.isCurrent()) return@coroutineScope
                _uiState.update { it.copy(feedUnreadData = feedResult.getOrNull() ?: it.feedUnreadData) }
                result.fold(onSuccess = { data ->
                    pagination = data.pagination
                    val entries = data.sessions.distinctBy { it.key }
                    val items = entries.map { it.item }
                    _uiState.update { it.copy(
                        isLoading = false, isRefreshing = false, isBatchOperating = false,
                        sessions = items, entries = entries.associateBy { InboxSessionPaginationPolicy.resolveSessionKey(it.item) },
                        userInfoMap = items.associate { item -> item.talker_id to UserBasicInfo(item.talker_id, item.account_info?.name.orEmpty(), item.account_info?.avatarUrl.orEmpty()) },
                        menu = data.menu, outside = data.outside, hasMore = data.hasMore,
                        page = 1, endTs = it.endTs + 1,
                    ) }
                }, onFailure = { error ->
                    _uiState.update { it.copy(isLoading = false, isRefreshing = false, isBatchOperating = false, error = error.message ?: "加载失败") }
                })
            }
        }
    }

    fun loadMoreSessions() {
        val current = _uiState.value
        if (current.isLoading || current.isRefreshing || current.isLoadingMore || !current.hasMore) return
        val version = generation
        val cursor = pagination
        _uiState.update { it.copy(isLoadingMore = true, loadMoreError = null) }
        moreJob = requests.launch {
            MessageImRepository.sessions(current.pageType, cursor).fold(onSuccess = { data ->
                if (version != generation || !requests.isCurrent()) return@fold
                val advanced = !cursor.contentEquals(data.pagination)
                pagination = data.pagination
                val incoming = data.sessions.associateBy { InboxSessionPaginationPolicy.resolveSessionKey(it.item) }
                _uiState.update { state ->
                    val entries = state.entries + incoming
                    val orderedKeys = (state.sessions.map(InboxSessionPaginationPolicy::resolveSessionKey) + incoming.keys).distinct()
                    val items = orderedKeys.mapNotNull { entries[it]?.item }
                    state.copy(
                        isLoadingMore = false, sessions = items, entries = entries,
                        userInfoMap = items.associate { it.talker_id to UserBasicInfo(it.talker_id, it.account_info?.name.orEmpty(), it.account_info?.avatarUrl.orEmpty()) },
                        hasMore = data.hasMore && advanced, page = state.page + 1, endTs = state.endTs + 1,
                    )
                }
            }, onFailure = { error ->
                if (version == generation) _uiState.update { it.copy(isLoadingMore = false, loadMoreError = error.message ?: "加载更多失败") }
            })
        }
    }

    private fun operate(action: suspend () -> Result<Unit>) {
        if (_uiState.value.isBatchOperating) return
        val version = generation
        _uiState.update { it.copy(isBatchOperating = true, operationError = null) }
        requests.launch {
            action().fold(onSuccess = { if (version == generation) refresh() }, onFailure = { error ->
                if (version == generation) _uiState.update { it.copy(isBatchOperating = false, operationError = error.message ?: "操作失败") }
            })
        }
    }
    fun markRead(session: SessionItem) {
        val entry = entry(session) ?: return
        val page = _uiState.value.pageType
        operate { MessageImRepository.clearUnread(page, entry) }
    }
    fun markAllRead() { val page = _uiState.value.pageType; operate { MessageImRepository.clearUnread(page) } }
    fun clearSessions() { val page = _uiState.value.pageType; operate { MessageImRepository.clearList(page) } }
    fun removeSession(session: SessionItem) = operate { MessageRepository.removeSession(session.talker_id, session.session_type) }
    fun toggleTop(session: SessionItem) { val entry = entry(session) ?: return; operate { MessageImRepository.pin(entry) } }
    fun toggleDnd(session: SessionItem) = operate { MessageRepository.setSessionDnd(session.talker_id, session.session_type, session.is_dnd != 1) }
    fun toggleIntercept(session: SessionItem) = operate { MessageRepository.setSessionIntercept(session.talker_id, session.is_intercept != 1) }
    fun clearError() { _uiState.update { it.copy(error = null) } }
    fun clearOperationError() { _uiState.update { it.copy(operationError = null) } }
    fun onOpenSession(session: SessionItem) {
        val key = InboxSessionPaginationPolicy.resolveSessionKey(session)
        _uiState.update { state ->
            val current = state.entries[key] ?: return@update state
            val entry = current.copy(item = current.item.copy(unread_count = 0), unreadDot = false, unreadLabel = "")
            state.copy(entries = state.entries + (key to entry), sessions = state.sessions.map { if (InboxSessionPaginationPolicy.resolveSessionKey(it) == key) entry.item else it })
        }
    }
    fun clearFeedBadge(destination: MessageCenterDestination) {
        _uiState.update { state -> state.copy(feedUnreadData = state.feedUnreadData?.let {
            when (destination) {
                MessageCenterDestination.ReplyMe -> it.copy(reply = 0)
                MessageCenterDestination.AtMe -> it.copy(at = 0)
                MessageCenterDestination.LikeMe -> it.copy(like = 0)
                MessageCenterDestination.SystemNotice -> it.copy(sysMsg = 0)
            }
        }) }
    }
}
