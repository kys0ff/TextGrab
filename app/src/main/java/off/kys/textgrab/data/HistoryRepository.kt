package off.kys.textgrab.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import off.kys.textgrab.core.model.ExtractionMode
import off.kys.textgrab.core.model.HistoryEntry
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject

private val Context.historyDataStore: DataStore<Preferences> by preferencesDataStore(name = "textgrab")

/**
 * Persists the copy history using Jetpack DataStore. Entries are serialised to a
 * compact JSON array under a single key — plenty for a capped, local-only log and
 * avoids pulling in Room for a flat list.
 */
class HistoryRepository(context: Context) {

    private val appContext = context.applicationContext
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    /** Cold flow of history, newest first. */
    val history: Flow<List<HistoryEntry>> =
        appContext.historyDataStore.data.map { prefs -> decode(prefs[KEY]) }

    /** Fire-and-forget append. Prepends the newest entry and caps the log at [MAX]. */
    fun add(text: String, source: ExtractionMode) {
        val trimmed = text.trim()
        if (trimmed.isEmpty()) return
        scope.launch {
            appContext.historyDataStore.edit { prefs ->
                val current = decode(prefs[KEY]).toMutableList()
                val now = System.currentTimeMillis()
                current.add(0, HistoryEntry(id = now, text = trimmed, timestamp = now, source = source))
                prefs[KEY] = encode(current.take(MAX))
            }
        }
    }

    fun clear() {
        scope.launch { appContext.historyDataStore.edit { it.remove(KEY) } }
    }

    private fun encode(entries: List<HistoryEntry>): String {
        val array = JSONArray()
        for (e in entries) {
            array.put(
                JSONObject()
                    .put("id", e.id)
                    .put("text", e.text)
                    .put("ts", e.timestamp)
                    .put("src", e.source.name),
            )
        }
        return array.toString()
    }

    private fun decode(raw: String?): List<HistoryEntry> {
        if (raw.isNullOrEmpty()) return emptyList()
        return runCatching {
            val array = JSONArray(raw)
            buildList {
                for (i in 0 until array.length()) {
                    val o = array.getJSONObject(i)
                    add(
                        HistoryEntry(
                            id = o.optLong("id", i.toLong()),
                            text = o.optString("text"),
                            timestamp = o.optLong("ts"),
                            source = runCatching { ExtractionMode.valueOf(o.optString("src")) }
                                .getOrDefault(ExtractionMode.ACCESSIBILITY),
                        ),
                    )
                }
            }
        }.getOrDefault(emptyList())
    }

    private companion object {
        val KEY = stringPreferencesKey("history_json")
        const val MAX = 100
    }
}
