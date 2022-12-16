package de.uni_muenster.imi.oegd.baseX

import model.IBaseXClient
import model.LocalBasexInfo
import org.basex.core.Context
import org.basex.core.cmd.*
import java.io.File
import java.nio.file.Files

/**
 * read a directory and create an internal BaseX database instance to execute XQueries
 */
class LocalBaseXClient(val directory: File) : IBaseXClient {

    private val context: Context = Context()

    init {
        CreateDB("LocalDB").execute(context)
        processDirectory(directory, context)
        Optimize().execute(context)
    }

    override suspend fun executeXQuery(xquery: String): String {
        return XQuery(xquery).execute(context)
    }

    override fun close() {
        //Drop local DB
        DropDB("LocalDB").execute(context)
        context.close()
    }


    private fun processDirectory(directory: File, context: Context) {
        directory.walk()
            .filter { item -> Files.isRegularFile(item.toPath()) }
            .forEach { Add("", it.absolutePath).execute(context) }
    }

    override fun getInfo() = LocalBasexInfo(directory.toString())
}
