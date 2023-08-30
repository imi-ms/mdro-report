package model

import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import mu.KotlinLogging
import net.harawata.appdirs.AppDirsFactory
import java.io.File
import java.time.LocalDateTime


class CachingUtility(private val baseXClient: IBaseXClient) {
    private val log = KotlinLogging.logger { }
    private val basexInfo: BasexInfo = baseXClient.getInfo()
    val cacheProvider = CacheProvider(basexInfo)

    //Loading the data is timeconsuming. Use a mutex to triggering duplicate report creation
    private val mutexMap = mutableMapOf<Pair<XQueryParams, GermType?>, Mutex>().apply {
        for (year in 2000..2030) {
            val xQueryParams = XQueryParams(year)
            for (germ in GermType.values()) {
                put(xQueryParams to germ, Mutex())
            }
            put(xQueryParams to null, Mutex())
        }
    }

    suspend fun getOrLoadGermInfo(xQueryParams: XQueryParams, germ: GermType): GermInfo =
        mutexMap[xQueryParams to germ]!!.withLock {
            if (getGermForGermtype(xQueryParams, germ)?.created == null) {
                log.info { "Loading $germ-GermInfo from BaseX for $xQueryParams" }
                val germInfo = DataProvider.getGermInfo(baseXClient, germ, xQueryParams)
                cache(xQueryParams, germInfo)
                log.info { "Done with ${germInfo.type} for $xQueryParams" }
            } else {
                log.info { "Loading $germ-GermInfo for $xQueryParams from Cache" }
            }
            return getGermForGermtype(xQueryParams, germ)!!
        }


    suspend fun getOrLoadGlobalInfo(xQueryParams: XQueryParams): GlobalInfo =
        mutexMap[xQueryParams to null]!!.withLock {
            if (getGlobalInfo(xQueryParams)?.created == null) {
                log.info { "Loading GlobalInfo from BaseX for $xQueryParams" }
                val overviewContent = DataProvider.getGlobalStatistics(baseXClient, xQueryParams)
                cache(xQueryParams, overviewContent)
                log.info("Done with GlobalInfo request for $xQueryParams")
            } else {
                log.info { "Loading GlobalInfo from Cache" }
            }
            return getGlobalInfo(xQueryParams)!!
        }

    suspend fun cacheAllData(xQueryParams: XQueryParams) {
        getOrLoadGlobalInfo(xQueryParams)
        for (germType in GermType.values()) {
            getOrLoadGermInfo(xQueryParams, germType)
        }
    }

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

    private fun getOrCreateCache(xQueryParams: XQueryParams) =
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
    fun getCachedParameters(): List<XQueryParams> {
        val cached = File(cacheDirectory).listFiles()!!.filter { it.name.startsWith(getBaseXPrefix()) }
            .map { it.name.substringAfter("--").removeSuffix(".mrereport") }
        return cached.map { XQueryParams(it.toInt()) }
    }

    fun clearCache(xQueryParams: XQueryParams) {
        try {
            getCacheFile(xQueryParams).delete()
        } catch (_: Exception) {
        }
    }

    private fun cacheExists(xQueryParams: XQueryParams): Boolean {
        return getCacheFile(xQueryParams).exists()
    }

    private fun getCacheFile(xQueryParams: XQueryParams) = File(cacheDirectory, getCacheFileName(xQueryParams))

    private val cacheDirectory: String by lazy {
        val userCacheDir = System.getenv("mrereport.cachedir") ?: AppDirsFactory.getInstance()
            .getUserCacheDir("mrereport", "1.0", "IMI")!!

        log.info { "Using '$userCacheDir' as cache directory!" }
        //TODO: Add caching path as property
        userCacheDir
    }

    fun getCacheFileName(xQueryParams: XQueryParams): String {
        return "${getBaseXPrefix()}--${xQueryParams.year}.mrereport"
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

