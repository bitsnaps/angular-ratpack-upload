package org.swalsh.image

import org.imgscalr.Scalr
import ratpack.exec.Blocking
import ratpack.exec.Promise
import ratpack.form.UploadedFile

import javax.imageio.ImageIO
import java.awt.image.BufferedImage

import static java.util.UUID.randomUUID

class ImageService {


    Promise<List<String>> getUploadedImages(File imageDirectory ) {
        Blocking.get {
            imageDirectory.listFiles( { it.isFile() } as FileFilter ).sort { it.lastModified() }*.name
        }
    }


    Boolean isImageFile(UploadedFile file ) {
        file.contentType.type.contains( "image" )
    }

    Promise<File> process( UploadedFile file, File imageDirectory, File thumbDirectory ) {

        String fileName = getUniqueFileName( "png" )
        BufferedImage image = readImage( file )

        Blocking.get {
            saveThumb( image, fileName, thumbDirectory )
            saveImage( image, fileName, imageDirectory )
        }

    }

    String getUniqueFileName( String extension ) {
        "${randomUUID()}.$extension"
    }

    BufferedImage readImage( UploadedFile file ) {
        ImageIO.read( file.inputStream )
    }

    File saveImage( BufferedImage image, String fileName, File directory ) {
        File file = new File( directory, fileName )
        ImageIO.write( image, "png", file )
        file
    }

    File saveThumb( BufferedImage image, String fileName, File directory ) {
        BufferedImage thumb = Scalr.resize( image, Scalr.Mode.FIT_TO_HEIGHT, 100 )
        saveImage( thumb, fileName, directory )
    }
}
