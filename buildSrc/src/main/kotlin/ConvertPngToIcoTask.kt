import org.gradle.api.DefaultTask
import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.tasks.InputDirectory
import org.gradle.api.tasks.InputFile
import org.gradle.api.tasks.InputFiles
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.OutputFiles
import org.gradle.api.tasks.TaskAction
import java.awt.Image
import java.awt.image.BufferedImage
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.OutputStream
import javax.imageio.ImageIO

abstract class ConvertPngToIcoTask : DefaultTask() {

    @get:InputFile
    abstract val inputFile: RegularFileProperty

    @get:OutputFile
    abstract val outputFile: RegularFileProperty

    @TaskAction
    fun run() {
        val input = inputFile.get().asFile
        val output = outputFile.get().asFile
        convertPngToIco(input, output)
    }



    /**
     * Converts a PNG file to a Windows ICO file.
     *
     * The resulting ICO contains multiple standard icon sizes (16, 32, 48, 64, 128, 256),
     * each stored as a PNG-encoded entry (supported by Windows Vista and later).
     *
     * @param pngFile The source PNG file
     * @param icoFile The destination ICO file
     */
    fun convertPngToIco(pngFile: File, icoFile: File) {
        val source = ImageIO.read(pngFile)
            ?: throw IllegalArgumentException("Could not read PNG file: ${pngFile.absolutePath}")

        val sizes = listOf(16, 32) +
                listOf(48, 64, 128, 256).filter { it <= maxOf(source.width, source.height) }

        val images = sizes.map { size -> scaleImageToSize(source, size, size) }
        writeIco(images, icoFile)
    }

    private fun scaleImageToSize(src: BufferedImage, width: Int, height: Int): BufferedImage {
        val scaled = src.getScaledInstance(width, height, Image.SCALE_SMOOTH)
        val out = BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB)
        val g = out.createGraphics()
        g.drawImage(scaled, 0, 0, null)
        g.dispose()
        return out
    }

    /** see https://en.wikipedia.org/wiki/ICO_(file_format)#ICONDIR_structure **/
    private fun writeIco(images: List<BufferedImage>, icoFile: File) {
        // Encode each image as PNG bytes
        val pngBlobs = images.map { img ->
            ByteArrayOutputStream().also {
                ImageIO.write(img, "png", it)
            }.toByteArray()
        }

        icoFile.outputStream().use { out ->
            // ICONDIR (first 6 bytes)
            out.writeLE16(0) // reserved, must be 0
            out.writeLE16(1) // ico type (1 for icon, 2 for cursor)
            out.writeLE16(images.size) // image cnt

            // ICONDIRENTRY is 16 bytes each; image data starts after directory.
            var offset = 6 + 16 * images.size
            for (i in images.indices) {
                val img = images[i]
                val data = pngBlobs[i]

                out.write(if (img.width >= 256) 0 else img.width)   // bWidth
                out.write(if (img.height >= 256) 0 else img.height) // bHeight
                out.write(0) // color palette count
                out.write(0) // reserved, must be 0
                out.writeLE16(1)          // color planes
                out.writeLE16(32)         // bits per pixel
                out.writeLE32(data.size)  // image data size in bytes
                out.writeLE32(offset)     // offset to image data
                offset += data.size
            }

            // the actual image data
            for (data in pngBlobs) {
                out.write(data)
            }
            out.flush()
        }
    }

    /** write 16 bit little endian */
    private fun OutputStream.writeLE16(value: Int) {
        write(value and 0xFF)
        write((value ushr 8) and 0xFF)
    }

    /** write 32 bit little endian */
    private fun OutputStream.writeLE32(value: Int) {
        write(value and 0xFF)
        write((value ushr 8) and 0xFF)
        write((value ushr 16) and 0xFF)
        write((value ushr 24) and 0xFF)
    }


}

