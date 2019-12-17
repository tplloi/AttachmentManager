package com.mirza.attachmentmanager.managers

import android.app.Activity.RESULT_OK
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.net.Uri
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import com.mirza.attachmentmanager.R
import com.mirza.attachmentmanager.fragments.AttachmentBottomSheet
import com.mirza.attachmentmanager.fragments.AttachmentFragment
import com.mirza.attachmentmanager.utils.AttachmentUtil
import com.mirza.attachmentmanager.fragments.DialogAction
import com.mirza.attachmentmanager.models.AttachmentDetail
import com.mirza.attachmentmanager.utils.FileUtil
import java.io.File

enum class HideOption {
    GALLERY, CAMERA, DOCUMENT
}

class AttachmentManager private constructor(builder: AttachmentBuilder) {


    companion object {
        val instance = this
    }

    private var title: String? = ""
    private var activity: AppCompatActivity? = null
    private var fragment: Fragment? = null
    private var selection: DialogAction? = null
    private var isMultiple = false
    private var context: Context? = null
    private var cameraFile: File? = null
    private var isBottomSheet = false
    private var imagesColor: Int? = null
    private var optionsTextColor: Int? = null
    private var hideOptions: HideOption? = null


    init {
        activity = builder.activity
        fragment = builder.fragment
        title = builder.title
        isMultiple = builder.isMultiple
        context = builder.context
        isBottomSheet = builder.isBottomSheet
        imagesColor = builder.imagesColor
        optionsTextColor = builder.optionsTextColor
        hideOptions = builder.hideOption
    }

    /**
     * Call this method to open attachment selection
     */
    fun openSelection() {
        activity?.let {

            if (isBottomSheet) {
                val attachmentFragmentSheet = AttachmentBottomSheet(title, hideOptions) { action ->
                    handleSelectionResponse(action)
                }
                attachmentFragmentSheet.show(it.supportFragmentManager, "DIALOG_SELECTION")
            } else {
                val attachmentFragmentDialog = AttachmentFragment(title, optionsTextColor, imagesColor, hideOptions) { action ->
                    handleSelectionResponse(action)
                }
                attachmentFragmentDialog.show(it.supportFragmentManager, "DIALOG_SELECTION")
            }

        }
    }

    /**
     * @param action contains selection value selected by user using dialog or bottom sheet
     */

    private fun handleSelectionResponse(action: DialogAction) {
        selection = action
        when (action) {
            DialogAction.GALLERY -> {
                openGallery()
            }
            DialogAction.CAMERA -> {
                startCamera()
            }

            DialogAction.FILE -> {
                openFileSystem()
            }
        }
    }

    /**
     * @param activity container Activity and could be null if if user is interacting with AttachmentManager from fragment
     * @param fragment will hold the reference to fragment if user is interacting with AttachmentManager from fragment
     * @param permissionCode used in case of permission grant
     */

    private fun openCamera(activity: AppCompatActivity?, fragment: Fragment?, permissionCode: Int) {
        if (PermissionManager.checkForPermissions(activity, fragment, PermissionManager.cameraPermissionList, permissionCode)) {
            val tuple = AttachmentUtil.onCamera(activity!!)
            cameraFile = tuple.photoFile
            AttachmentUtil.openCamera(tuple, activity, fragment)
        }
    }

    /**
     * @param activity container Activity and could be null if if user is interacting with AttachmentManager from fragment
     * @param fragment will hold the reference to fragment if user is interacting with AttachmentManager from fragment
     * @param permissionCode used in case of permission grant
     */

    private fun openGallery(activity: AppCompatActivity?, fragment: Fragment?, permissionCode: Int) {
        if (PermissionManager.checkForPermissions(activity, fragment, PermissionManager.storagePermissionList, permissionCode)) {
            val intent = AttachmentUtil.onPhoto(activity!!, isMultiple)
            AttachmentUtil.openGallery(intent, activity, fragment)
        }
    }

    /**
     * @param activity container Activity and could be null if if user is interacting with AttachmentManager from fragment
     * @param fragment will hold the reference to fragment if user is interacting with AttachmentManager from fragment.
     * @param permissionCode used in case of permission grant
     */
    private fun openFileSystem(activity: AppCompatActivity?, fragment: Fragment?, permissionCode: Int) {
        if (PermissionManager.checkForPermissions(activity, fragment, PermissionManager.storagePermissionList, permissionCode)) {
            val intent = AttachmentUtil.onFile(activity, fragment, isMultiple)
        }
    }

    /**
     * Use below three methods to interact with AttachmentManager directly without any dialog or bottom sheet
     */
    fun startCamera() {
        openCamera(activity, fragment, AttachmentUtil.CAPTURE_IMAGE_ACTIVITY_REQUEST_CODE)
    }

    fun openGallery() {
        openGallery(activity, fragment, AttachmentUtil.PICK_PHOTO_CODE)
    }

    fun openFileSystem() {
        openFileSystem(activity, fragment, AttachmentUtil.FILE_CODE)
    }


    /**
     * Use this method from onActivityResult within your activity or fragment
     * @return List of AttachmentDetail objects
     */
    fun manipulateAttachments(requestCode: Int, resultCode: Int, data: Intent?): ArrayList<AttachmentDetail>? {
        val list = ArrayList<AttachmentDetail>()
        if (resultCode == RESULT_OK) {
            when (requestCode) {
                AttachmentUtil.FILE_CODE, AttachmentUtil.PICK_PHOTO_CODE -> {
                    if (data != null) {
                        if (isMultiple && data.clipData != null) {

                            data.clipData?.let {
                                // Toast.makeText(context!!, it.itemCount.toString(), Toast.LENGTH_SHORT).show()
                                for (x in 0 until it.itemCount) {
                                    it.getItemAt(x).uri?.let { uri ->
                                        list.add(prepareAttachment(uri, FileUtil.getFileDisplayName(uri, context!!, File(uri.toString())), FileUtil.getMimeType(uri, context!!), FileUtil.getFileSize(uri, context!!)))
                                    }

                                }
                            }

                        } else {

                            val fileUri = data.data
                            fileUri?.let {
                                list.add(prepareAttachment(it, FileUtil.getFileDisplayName(it, context!!, File(it.toString())), FileUtil.getMimeType(it, context!!), FileUtil.getFileSize(it, context!!)))
                            }
                        }
                    }
                }
                AttachmentUtil.CAPTURE_IMAGE_ACTIVITY_REQUEST_CODE -> {

                    val fileUri = Uri.fromFile(cameraFile)
                    val file = File(fileUri.toString())
                    val displayName = FileUtil.getFileDisplayName(fileUri, activity as AppCompatActivity, file)
                    list.add(prepareAttachment(fileUri, displayName, FileUtil.getMimeType(fileUri, context!!), FileUtil.getFileSize(fileUri, context!!)))


                }
            }
        }

        return list
    }


    /**
     * Use this method from onRequestPermissionsResult within your activity or fragment
     * It will handle permission results for you
     */
    fun handlePermissionResponse(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        when (requestCode) {
            AttachmentUtil.CAPTURE_IMAGE_ACTIVITY_REQUEST_CODE -> {
                if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    startCamera()
                }
            }
            AttachmentUtil.PICK_PHOTO_CODE -> {
                if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    openGallery()
                }
            }
            AttachmentUtil.FILE_CODE -> {
                if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    openFileSystem()
                }
            }
        }
    }

    private fun prepareAttachment(uri: Uri, name: String, mimeType: String?, size: Long): AttachmentDetail {

        val attachmentDetail = AttachmentDetail()
        attachmentDetail.uri = uri
        attachmentDetail.name = name
        attachmentDetail.path = uri.path
        attachmentDetail.mimeType = mimeType
        attachmentDetail.size = size
        return attachmentDetail
    }

    /**
     * Initiates AttachmentManager object for you
     */
    data class AttachmentBuilder(var context: Context) {

        var activity: AppCompatActivity? = null
        var fragment: Fragment? = null
        var title: String? = context.getString(R.string.m_choose)
        var isMultiple: Boolean = false
        var isBottomSheet: Boolean = false
        var imagesColor: Int? = null
        var optionsTextColor: Int? = null
        var hideOption: HideOption? = null

        fun activity(activity: AppCompatActivity?) = apply { this.activity = activity }
        fun fragment(fragment: Fragment?) = apply { this.fragment = fragment }
        /**
         * @param title of dialog or bottom sheet
         */
        fun setUiTitle(title: String?) = apply { this.title = title }

        fun allowMultiple(isMultiple: Boolean) = apply { this.isMultiple = isMultiple }
        fun asBottomSheet(isBottomSheet: Boolean) = apply { this.isBottomSheet = isBottomSheet }
        fun setImagesColor(imagesColor: Int) = apply { this.imagesColor = imagesColor }
        fun setOptionsTextColor(textColor: Int) = apply { this.optionsTextColor = textColor }
        fun hide(option: HideOption?) = apply { this.hideOption = option }

        fun build() = AttachmentManager(this)

    }


}