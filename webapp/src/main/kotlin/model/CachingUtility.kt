package model

import de.uni_muenster.imi.oegd.common.GermType
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import mu.KotlinLogging
import net.harawata.appdirs.AppDirsFactory
import java.io.File
import java.time.LocalDateTime



class CachingUtility(private val basexInfo: BasexInfo) {
    //TODO: Use ContextReceiver to get BasexInfo
    object RequestState { //TODO: Make this also depending on XQueryParams
        private val stateMap: HashMap<String, Boolean> = hashMapOf(
            Pair("mrsaState", false),
            Pair("mrgnState", false),
            Pair("vreState", false),
            Pair("globalState", false)
        )

        fun isRequestActive(germ: GermType?): Boolean {
            return when (germ) {
                GermType.MRSA -> stateMap["mrsaState"]!!
                GermType.MRGN -> stateMap["mrgnState"]!!
                GermType.VRE -> stateMap["vreState"]!!
                else -> stateMap["globalState"]!!
            }
        }

        fun markRequestActive(germ: GermType?) {
            when (germ) {
                GermType.MRSA -> stateMap["mrsaState"] = true
                GermType.MRGN -> stateMap["mrgnState"] = true
                GermType.VRE -> stateMap["vreState"] = true
                else -> stateMap["globalState"] = true
            }
        }

        fun markRequestInactive(germ: GermType?) {
            when (germ) {
                GermType.MRSA -> stateMap["mrsaState"] = false
                GermType.MRGN -> stateMap["mrgnState"] = false
                GermType.VRE -> stateMap["vreState"] = false
                else -> stateMap["globalState"] = false
            }
        }
    }

    private val log = KotlinLogging.logger { }
    val cacheProvider = CacheProvider(basexInfo)


    fun cache(xQueryParams: XQueryParams, germ: GermInfo) {
        val cache = getOrCreateCache(xQueryParams)
        cache.metadata.timeUpdated = LocalDateTime.now().toString()
        cache.germCache.removeIf { it.type == germ.type }
        cache.germCache.add(germ)
        cacheProvider.writeCache(cache)
    }

    fun cache(xQueryParams: XQueryParams, data: GlobalInfo) {
        val cache = getOrCreateCache(xQueryParams)
        cache.metadata.timeUpdated = LocalDateTime.now().toString()
        cache.globalCache = data
        cacheProvider.writeCache(cache)
    }


    fun clearGlobalInfoCache(xQueryParams: XQueryParams) {
        val cache = getOrCreateCache(xQueryParams)

        cache.metadata.timeUpdated = LocalDateTime.now().toString()
        cache.globalCache.clear()

        cacheProvider.writeCache(cache)
    }

    fun clearGermInfo(xQueryParams: XQueryParams, germ: GermType) {
        val cache = getOrCreateCache(xQueryParams)

        cache.metadata.timeUpdated = LocalDateTime.now().toString()
        cache.germCache.removeIf { it.type == germ.germtype }

        cacheProvider.writeCache(cache)
    }


    fun uploadExistingCache(cache: String) {
        cacheProvider.writeCache(Json.decodeFromString(cache))
    }


    fun getGermForGermtype(xQueryParams: XQueryParams, germ: GermType): GermInfo? {
        return cacheProvider.getCache(xQueryParams)?.germCache?.find { it.type == germ.germtype }
    }

    fun getGlobalInfo(xQueryParams: XQueryParams): GlobalInfo? {
        return cacheProvider.getCache(xQueryParams)?.globalCache
    }

    fun getOrCreateCache(xQueryParams: XQueryParams) =
        cacheProvider.getCache(xQueryParams) ?: createCache(xQueryParams)

    private fun createCache(xQueryParams: XQueryParams): CacheData {
        return CacheData(
            metadata = CacheMetadata(
                timeCreated = LocalDateTime.now().toString(),
                timeUpdated = LocalDateTime.now().toString(),
                basex = basexInfo,
                xQueryParams = xQueryParams,
            ),
            germCache = mutableListOf(),
            globalCache = GlobalInfo(),
        )
    }


}


class CacheProvider(val basexInfo: BasexInfo) {

    private val log = KotlinLogging.logger { }
    fun clearCache(xQueryParams: XQueryParams) {
        try {
            getCacheFile(xQueryParams).delete()
        } catch (_: Exception) {
        }
    }

    private fun cacheExists(xQueryParams: XQueryParams): Boolean {
        return getCacheFile(xQueryParams).exists()
    }

    fun getCacheFileName(xQueryParams: XQueryParams): String {
        return "${getBaseXPrefix()}--${xQueryParams.year}.mrereport"
    }

    private fun getCacheFile(xQueryParams: XQueryParams) =
        File(cacheDirectory, getCacheFileName(xQueryParams))

    private val cacheDirectory: String by lazy {
        val userCacheDir = AppDirsFactory.getInstance().getUserCacheDir("mrereport", "1.0", "IMI")!!
        log.info { "Using '$userCacheDir' as cache directory!" }
        //TODO: Add caching path as property
        userCacheDir
    }

    fun getCachedParameters(): List<XQueryParams> {
        val cached = File(cacheDirectory).listFiles().filter { it.name.startsWith(getBaseXPrefix()) }
            .map { it.name.substringAfter("--").removeSuffix(".mrereport") }
        return cached.map { XQueryParams(it.toInt()) }
    }

    fun writeCache(cache: CacheData) {
        val json = Json.encodeToString(cache)
        File(cacheDirectory).mkdirs()
        getCacheFile(cache.metadata.xQueryParams).writeText(json)
    }

    fun getCache(xQueryParams: XQueryParams): CacheData? {
        if (cacheExists(xQueryParams)) {
            return try {
                val json = getCacheFile(xQueryParams).readText()
                Json.decodeFromString(json)
            } catch (e: Exception) {
                null
            }
        }
        return null
    }


    private fun getBaseXPrefix() = if (basexInfo is RestConnectionInfo) {
        "${sanitizeFilename(basexInfo.serverUrl)}-${sanitizeFilename(basexInfo.databaseId)}"
    } else {
        basexInfo as LocalBasexInfo
        "local-${sanitizeFilename(basexInfo.directory)}"
    }


    private fun sanitizeFilename(inputName: String) = inputName.replace(Regex("[^a-zA-Z0-9_]"), "_")

}

