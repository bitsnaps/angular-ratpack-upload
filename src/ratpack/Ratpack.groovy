import org.slf4j.LoggerFactory
import org.swalsh.image.ImageService
import ratpack.form.Form
import ratpack.server.BaseDir
import ratpack.service.Service
import ratpack.service.StartEvent
import static ratpack.groovy.Groovy.ratpack
import static ratpack.jackson.Jackson.json

def log = LoggerFactory.getLogger("Ratpack")

def assetsPath = "public"
def imageDirName = "uploaded-files"
def imagePath = "$assetsPath/$imageDirName"
def thumbPath = "$imagePath/thumb"

ratpack {

     serverConfig {
         port(8080)
         maxContentLength(5000000)
         baseDir BaseDir.find('ratpack')
     }

    bindings {
        bind(ImageService)

        add Service.startup('startup'){ StartEvent event ->
            // Create thumbnails directory if it doesn't exist
            def thumbDir = new File("${thumbPath}")
            if (!thumbDir.exists()){
                log.info("thumbDir not found: ${thumbDir}, creating a new one...")
                try {
                    thumbDir.mkdirs()
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

            def imageDir = new File( imagePath )
            def thumbDir = new File( thumbPath )

            byMethod {

                get { def ctx ->
                    imageService.getUploadedImages( imageDir ).then { List imageList ->
                        render json(imagePath: imageDirName, images: imageList)
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

        get("${imageDirName}/thumb/:imageName") {
            response.sendFile( new File("${thumbPath}", pathTokens['imageName']).toPath())
        }
        get("${imageDirName}/:imageName") {
            response.sendFile( new File("${imagePath}", pathTokens['imageName']).toPath())
        }

    }
}
