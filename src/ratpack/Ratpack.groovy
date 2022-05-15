import org.swalsh.image.ImageService
import ratpack.form.Form
import ratpack.form.UploadedFile
import ratpack.server.BaseDir
import ratpack.service.Service
import ratpack.service.StartEvent

import static ratpack.groovy.Groovy.byMethod
import static ratpack.groovy.Groovy.groovyTemplate
import static ratpack.groovy.Groovy.ratpack
import static ratpack.jackson.Jackson.json

def assetsPath = "public"
def imageDirName = "uploaded-files"
def imagePath = "$assetsPath/$imageDirName"
def thumbPath = "$imagePath/thumb"

ratpack {

     serverConfig {
         port(8080)
         maxContentLength(5000000)
        // baseDir BaseDir.find('ratpack')
     }

    bindings {
        bind(ImageService)

        add Service.startup('startup'){ StartEvent event ->
            // Create thumbnails directory if not exists
            if (!BaseDir.find("${thumbPath}").toFile().exists()){
                try {
                    BaseDir.find("${thumbPath}" ).toFile().mkdirs()
                } catch (Exception e) { println("Error: ${e.message}")}
            }
        }

    }

    handlers {

        files {
            dir( assetsPath )
            indexFiles('index.html')
        }

        path('image'){ ImageService imageService ->
            def baseDir = BaseDir.find("${assetsPath}")
            def imageDir = baseDir.resolve( imagePath ).toFile()
            def thumbDir = baseDir.resolve( thumbPath ).toFile()

            byMethod {

                get { def ctx ->
                    imageService.getUploadedImages( imageDir ).then {
                        render json(imagePath: imageDirName, images: it)
                    }
                }
                post /*('upload')*/ {
                    parse(Form).then { def form ->
                        form.files("fileUpload").each { def uploaded ->
                            if (imageService.isImageFile(uploaded)) {
                                imageService.process(uploaded, imageDir, thumbDir).then { File f ->
                                    render json(fileName: f.name)
                                }
                            } else {
                                response.status(400).send "Invalid file type. Images only!"
                            }
                        }
                    }
                }
            }
        } //path


    }
}
