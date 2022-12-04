package com.example.fridgeapp.handlers

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.util.Log
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.ActivityResultRegistry
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.MutableLiveData
import com.example.fridgeapp.BuildConfig
import com.example.fridgeapp.R
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

class FileSystemInteractor(
    private val registry: ActivityResultRegistry,
    private val context: Context,
    private val uriLiveData: MutableLiveData<Uri?>
) : DefaultLifecycleObserver {
    private lateinit var pickImageLauncher: ActivityResultLauncher<PickVisualMediaRequest>
    private lateinit var takePictureLauncher: ActivityResultLauncher<Uri>
    private lateinit var currentPhotoPath: String
    private var imageUriToSave: Uri? = null

    override fun onCreate(owner: LifecycleOwner) {
        super.onCreate(owner)
        pickImageLauncher =
            registry.register("gallery", owner, ActivityResultContracts.PickVisualMedia()) { uri ->
                if (uri != null) {
                    context.contentResolver.takePersistableUriPermission(
                        uri, Intent.FLAG_GRANT_READ_URI_PERMISSION
                    )
                    uriLiveData.value = uri
                } else {
                    Log.d("FILESYSTEM_ITERATION", "Image pick disrupted")
                }
            }

        takePictureLauncher = registry.register(
            "camera", owner, ActivityResultContracts.TakePicture()
        ) { code ->
            if (code) {
                uriLiveData.value = imageUriToSave
            } else {
                Log.d("FILESYSTEM_ITERATION", "Photo take disrupted")
            }

        }
    }

    fun selectImage() {
        pickImageLauncher.launch(
            PickVisualMediaRequest(
                ActivityResultContracts.PickVisualMedia.ImageOnly
            )
        )
    }

    fun takePhoto() {
        imageUriToSave =
            FileProvider.getUriForFile(
                context, BuildConfig.APPLICATION_ID + ".provider", createImageFile()
            )
        takePictureLauncher.launch(imageUriToSave)
    }

    private fun createImageFile(): File {
        // Create an image file name
        val timeStamp: String = SimpleDateFormat("yyyyMMdd_HHmmss").format(Date())
        val storageDir: File? = context.getExternalFilesDir(Environment.DIRECTORY_PICTURES)
        return File.createTempFile(
            "IMG_${timeStamp}_", /* prefix */
            ".jpg", /* suffix */
            storageDir /* directory */
        ).apply {
            // Save a file: path for use with ACTION_VIEW intents
            currentPhotoPath = absolutePath
        }
    }

    fun permissionCheck(): Boolean {
        return if (Build.VERSION.SDK_INT < 33) {
            (ContextCompat.checkSelfPermission(
                context, Manifest.permission.READ_EXTERNAL_STORAGE
            ) == PackageManager.PERMISSION_GRANTED)
        } else (ContextCompat.checkSelfPermission(
            context, Manifest.permission.READ_MEDIA_IMAGES
        ) == PackageManager.PERMISSION_GRANTED)
    }
}