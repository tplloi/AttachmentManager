package com.mirza.attachmentmanager

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Environment
import android.provider.MediaStore
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.FileProvider
import androidx.fragment.app.Fragment
import java.io.File

object AttachmentUtil {
    const val APP_TAG = "Jawdah"
    const val CAPTURE_IMAGE_ACTIVITY_REQUEST_CODE = 1000
    const val PICK_PHOTO_CODE = 1001
    const val FILE_CODE = 125
    // Returns the File for a photo stored on disk given the fileName
    private fun getPhotoFileUri(fileName: String, context: Context): File {
        // Get safe storage directory for photos
        // Use `getExternalFilesDir` on Context to access package-specific directories.
        // This way, we don't need to request external read/write runtime permissions.
        val mediaStorageDir = File(context.getExternalFilesDir(Environment.DIRECTORY_PICTURES), APP_TAG)

        // Create the storage directory if it does not exist
        if (!mediaStorageDir.exists() && !mediaStorageDir.mkdirs()) {
            Log.d(APP_TAG, "failed to create directory")
        }

        // Return the file target for the photo based on filename

        return File(mediaStorageDir.path + File.separator + fileName)
    }

    fun onCamera(context: Context): Tuple {
        // create Intent to take a picture and return control to the caller

        val intent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
        // Create a File reference to access to future access
        val photoFileName = "IMG_" + System.currentTimeMillis() + ".jpg"
        val photoFile = getPhotoFileUri(photoFileName, context)

        val fileProvider = FileProvider.getUriForFile(context, context.packageName + ".fileprovider", photoFile)
        intent.putExtra(MediaStore.EXTRA_OUTPUT, fileProvider)

        val tuple = Tuple()
        tuple.intent = intent
        tuple.photoFile = photoFile
        return tuple
    }

    // Trigger gallery selection for a photo
    fun onPhoto(context: Context): Intent {
        // Create intent for picking a photo from the gallery
        return Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
    }

    fun onFile(activity: AppCompatActivity?, fragmentContext: Fragment?): Intent {
        val intent = Intent()
        intent.action = Intent.ACTION_OPEN_DOCUMENT
        intent.addCategory(Intent.CATEGORY_OPENABLE)
        //intent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true)
        intent.type = if (FileUtil.mimeTypes.size == 1) FileUtil.mimeTypes[0] else "*/*"
        if (FileUtil.mimeTypes.isNotEmpty()) {
            intent.putExtra(Intent.EXTRA_MIME_TYPES, FileUtil.mimeTypes)
        }


        val list = activity?.packageManager?.queryIntentActivities(intent, PackageManager.MATCH_DEFAULT_ONLY)
        if (list?.size!! > 0) {
            if (fragmentContext == null) {
                activity.startActivityForResult(
                    Intent.createChooser(
                        intent,
                        activity.getString(R.string.selectFile_txt)
                    ), FILE_CODE
                )
            } else {
                fragmentContext.startActivityForResult(
                    Intent.createChooser(
                        intent,
                        activity.getString(R.string.selectFile_txt)
                    ), FILE_CODE
                )
            }
        }
        return intent
    }

    fun openCamera(tuple: Tuple, activity: AppCompatActivity?, fragmentContext: Fragment?) {

        if (tuple.intent?.resolveActivity(activity?.packageManager!!) != null) {
            if (fragmentContext == null) {
                activity?.startActivityForResult(tuple.intent, CAPTURE_IMAGE_ACTIVITY_REQUEST_CODE)
            } else {
                fragmentContext.startActivityForResult(tuple.intent, CAPTURE_IMAGE_ACTIVITY_REQUEST_CODE)
            }
        }
    }

    fun openGallery(intent: Intent, activity: AppCompatActivity?, fragmentContext: Fragment?) {
        if (intent.resolveActivity(activity?.packageManager!!) != null) {
            // Bring up gallery to select a photo
            if (fragmentContext == null) {
                activity.startActivityForResult(intent, PICK_PHOTO_CODE)
            } else {
                fragmentContext.startActivityForResult(intent, PICK_PHOTO_CODE)
            }
        }
    }


}