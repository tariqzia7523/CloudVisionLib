package com.pet.betaanglecloudvision


import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import android.os.Handler
import android.os.Looper
import android.provider.MediaStore
import android.util.Log
import com.google.api.client.extensions.android.http.AndroidHttp
import com.google.api.client.json.gson.GsonFactory
import com.google.api.services.vision.v1.Vision
import com.google.api.services.vision.v1.VisionRequest
import com.google.api.services.vision.v1.VisionRequestInitializer
import com.google.api.services.vision.v1.model.*
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.util.*
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import kotlin.collections.ArrayList

class ApplyOCR(var context: Context, var CLOUD_VISION_API_KEY: String, var uri: Uri,val ocrListener : OnOcrResponseListener){
    val TAG = "***OCR"
    private val ANDROID_CERT_HEADER = "X-Android-Cert"
    private val ANDROID_PACKAGE_HEADER = "X-Android-Package"
    private val MAX_LABEL_RESULTS = 10
    private val MAX_DIMENSION = 1200

    lateinit var executor: ExecutorService
    fun start(){
//        ocrListener = OnOcrResponseListener()
        executor = Executors.newSingleThreadExecutor()
        val handler = Handler(Looper.myLooper()!!)
        executor.execute {
            val bitmap = scaleBitmapDown(
                MediaStore.Images.Media.getBitmap(
                    context.contentResolver,
                    uri
                ), MAX_DIMENSION
            )
            val mRequest = prepareAnnotationRequest(bitmap)
            try{
                val response = mRequest.execute()
                val respose = convertResponseToString(response)
                handler.post {
                    Log.e(TAG, "The Value is " + respose)
                    ocrListener.ocrSuccess(respose)
                }

            }catch (e: Exception){
                e.printStackTrace()
                ocrListener.ocrFailure()
            }

        }
    }
    private fun convertResponseToString(response: BatchAnnotateImagesResponse): String? {
        val message = java.lang.StringBuilder("\n")
        val labels = response.responses[0].textAnnotations
        if (labels != null) {
            message.append(
                java.lang.String.format(
                    Locale.US,
                    "%.3f: %s",
                    labels[0].score,
                    labels[0].description
                )
            )

        } else {
            message.append("nothing")
        }
        return message.toString()
    }

//    fun uploadImage(uri: Uri?) {
//        if (uri != null) {
//            try {
//                // scale the image to save on bandwidth
//                val bitmap = scaleBitmapDown(MediaStore.Images.Media.getBitmap(context.contentResolver, uri), MAX_DIMENSION)
//                callCloudVision(bitmap)
//                //                mMainImage.setImageBitmap(bitmap);
//            } catch (e: IOException) {
//                Log.d(TAG, "Image picking failed because " + e.message)
////                Toast.makeText(this, "image error", Toast.LENGTH_LONG).show()
//            }
//        } else {
//            Log.d(TAG, "Image picker gave us a null image.")
////            Toast.makeText(this, "Image erroir", Toast.LENGTH_LONG).show()
//        }
//    }

    @Throws(IOException::class)
    private fun prepareAnnotationRequest(bitmap: Bitmap): Vision.Images.Annotate {
        val httpTransport = AndroidHttp.newCompatibleTransport()
        val jsonFactory: GsonFactory? = GsonFactory.getDefaultInstance()
        val requestInitializer: VisionRequestInitializer =
            object : VisionRequestInitializer(CLOUD_VISION_API_KEY) {
                /**
                 * We override this so we can inject important identifying fields into the HTTP
                 * headers. This enables use of a restricted cloud platform API key.
                 */
                @Throws(IOException::class)
                override fun initializeVisionRequest(visionRequest: VisionRequest<*>) {
                    super.initializeVisionRequest(visionRequest)
                    val packageName = context.packageName
                    visionRequest.requestHeaders[ANDROID_PACKAGE_HEADER] = packageName
                    val sig: String = PackageManagerUtils.getSignature(
                        context.packageManager,
                        packageName
                    )!!
                    visionRequest.requestHeaders[ANDROID_CERT_HEADER] = sig
                }
            }
        val builder = Vision.Builder(httpTransport, jsonFactory, null)
        builder.setVisionRequestInitializer(requestInitializer)
        val vision = builder.build()
        val batchAnnotateImagesRequest = BatchAnnotateImagesRequest()
        batchAnnotateImagesRequest.requests = object : ArrayList<AnnotateImageRequest?>() {
            init {
                val annotateImageRequest = AnnotateImageRequest()

                // Add the image
                val base64EncodedImage = Image()

                // Convert the bitmap to a JPEG
                // Just in case it's a format that Android understands but Cloud Vision
                val byteArrayOutputStream = ByteArrayOutputStream()
                bitmap.compress(Bitmap.CompressFormat.JPEG, 90, byteArrayOutputStream)
                val imageBytes: ByteArray = byteArrayOutputStream.toByteArray()

                // Base64 encode the JPEG
                base64EncodedImage.encodeContent(imageBytes)
                annotateImageRequest.image = base64EncodedImage
//                val list: MutableList<String> = ArrayList()
//                list.add("am")
//                list.add("en")
//                list.add("zh")
//                val imageContext = ImageContext()
//                imageContext.languageHints = list
//                annotateImageRequest.imageContext = imageContext
                // add the features we want
                annotateImageRequest.features = object : ArrayList<Feature?>() {
                    init {
                        val labelDetection = Feature()
                        //                labelDetection.setType("LABEL_DETECTION");
                        labelDetection.setType("TEXT_DETECTION")
                        labelDetection.setMaxResults(MAX_LABEL_RESULTS)
                        add(labelDetection)
                    }
                }

                // Add the list of one thing to the request
                add(annotateImageRequest)
            }
        }
        val annotateRequest = vision.images().annotate(batchAnnotateImagesRequest)
        // Due to a bug: requests to Vision API containing large images fail when GZipped.
        annotateRequest.disableGZipContent = true
        Log.d(TAG, "created Cloud Vision request object, sending request")
        return annotateRequest
    }

//    private class LableDetectionTask internal constructor(annotate: Vision.Images.Annotate) :
//        AsyncTask<Any?, Void?, String>() {
//        private val mRequest: Vision.Images.Annotate
//        init {
//            mRequest = annotate
//        }
//        override fun doInBackground(vararg params: Any?): String? {
//            try {
//                Log.d("***TAG", "created Cloud Vision request object, sending request")
//                val response = mRequest.execute()
//                return convertResponseToString(response)
//            } catch (e: GoogleJsonResponseException) {
//                Log.d("***TAG", "failed to make API request because " + e.content)
//            } catch (e: IOException) {
//                Log.d(
//                    "***TAG",
//                    "failed to make API request because of other IOException " + e.message
//                )
//            }
//            return "Cloud Vision API request failed. Check logs for details."
//        }
//
//
//
//        override fun onPostExecute(result: String) {
//            Log.e("***TAG", "result is " + result)
//            ;
//            //furthercall
//        }
//        private fun convertResponseToString(response: BatchAnnotateImagesResponse): String? {
//            val message = java.lang.StringBuilder("\n")
//            val labels = response.responses[0].textAnnotations
//            if (labels != null) {
//                message.append(
//                    java.lang.String.format(
//                        Locale.US,
//                        "%.3f: %s",
//                        labels[0].score,
//                        labels[0].description
//                    )
//                )
//
////            for (EntityAnnotation label : labels) {
////                message.append(String.format(Locale.US, "%.3f: %s", label.getScore(), label.getDescription()));
////
//////                message.append("\n");
////            }
//            } else {
//                message.append("nothing")
//            }
//            return message.toString()
//        }
//
//    }
//
//    private fun callCloudVision(bitmap: Bitmap) {
//        // Switch text to loading
////        mImageDetails.setText(R.string.loading_message);
//
//        // Do the real work in an async task, because we need to use the network anyway
//        try {
//            val labelDetectionTask: LableDetectionTask = LableDetectionTask(prepareAnnotationRequest(bitmap))
//            labelDetectionTask.execute()
//        } catch (e: IOException) {
//            Log.d(TAG, "failed to make API request because of other IOException " + e.message)
//        }
//    }

    private fun scaleBitmapDown(bitmap: Bitmap, maxDimension: Int): Bitmap {
        val originalWidth = bitmap.width
        val originalHeight = bitmap.height
        var resizedWidth = maxDimension
        var resizedHeight = maxDimension
        if (originalHeight > originalWidth) {
            resizedHeight = maxDimension
            resizedWidth =
                (resizedHeight * originalWidth.toFloat() / originalHeight.toFloat()).toInt()
        } else if (originalWidth > originalHeight) {
            resizedWidth = maxDimension
            resizedHeight =
                (resizedWidth * originalHeight.toFloat() / originalWidth.toFloat()).toInt()
        } else if (originalHeight == originalWidth) {
            resizedHeight = maxDimension
            resizedWidth = maxDimension
        }
        return Bitmap.createScaledBitmap(bitmap, resizedWidth, resizedHeight, false)
    }



}