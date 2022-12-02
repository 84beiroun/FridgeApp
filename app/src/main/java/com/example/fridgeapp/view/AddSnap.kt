package com.example.fridgeapp.view

import android.Manifest
import android.annotation.SuppressLint
import android.app.AlertDialog
import android.content.Context
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.util.Log
import android.view.*
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.core.view.MenuHost
import androidx.core.view.MenuProvider
import androidx.fragment.app.Fragment
import androidx.lifecycle.Lifecycle
import androidx.navigation.fragment.findNavController
import com.example.fridgeapp.BuildConfig
import com.example.fridgeapp.R
import com.example.fridgeapp.data.FridgeSnap
import com.example.fridgeapp.databinding.FragmentAddSnapBinding
import com.example.fridgeapp.injector.repository.SnapsRepository
import com.example.fridgeapp.loaders.FridgeApp
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.schedulers.Schedulers
import java.io.File
import java.text.SimpleDateFormat
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.*
import javax.inject.Inject

//здесь ничего интересного, можно почитать комментарии из открытой записи (CardExpanded.kt), там всё то же самое
//экшуали здесь пытался реализовать загрузку изображения через интенды, но там мультиинтенд очень тяжело захэндлить
//он результатом выводит один из интендов в итоге это будет контракт в контракте (???), думаю красивые реализации
//оставить можно на новые версии андройда там и материал3 и джетпак компоуз
class AddSnap : Fragment() {

    @Inject
    lateinit var snapsRepository: SnapsRepository

    private var _binding: FragmentAddSnapBinding? = null

    private val binding get() = _binding!!

    private var pickedImage: ActivityResultLauncher<PickVisualMediaRequest>? = null

    private var takenImage: ActivityResultLauncher<Uri>? = null

    //  private var multiIntent: ActivityResultLauncher<Intent>? = null

    private var actualImage: Uri? = null

    private lateinit var currentPhotoPath: String

    override fun onAttach(context: Context) {
        super.onAttach(context)
        FridgeApp.dbInstance.inject(this)
    }


    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = FragmentAddSnapBinding.inflate(inflater, container, false)

        binding.imagePickerButton.setOnClickListener {
            if (Build.VERSION.SDK_INT < 29) {
                if ((ContextCompat.checkSelfPermission(
                        requireContext(), Manifest.permission.READ_EXTERNAL_STORAGE
                    ) == PackageManager.PERMISSION_GRANTED)
                ) selectImage()
                else requestPermissions(
                    arrayOf(
                        Manifest.permission.READ_EXTERNAL_STORAGE,
                        Manifest.permission.WRITE_EXTERNAL_STORAGE
                    ), 0
                )
            } else {
                if ((ContextCompat.checkSelfPermission(
                        requireContext(), Manifest.permission.READ_MEDIA_IMAGES
                    ) == PackageManager.PERMISSION_GRANTED)
                ) selectImage()
                else
                    Toast.makeText(
                    this@AddSnap.context, getString(R.string.permission_storage), Toast.LENGTH_LONG
                ).show()
            }
        }
        binding.saveSnapButton.setOnClickListener { toSave() }

        sysImageSelector()
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        menuSetup()
    }


    private fun menuSetup() {
        val menuHost: MenuHost = requireActivity()
        menuHost.addMenuProvider(
            object : MenuProvider {
                override fun onCreateMenu(menu: Menu, menuInflater: MenuInflater) {
                    menu.clear()
                    menuInflater.inflate(R.menu.menu_add, menu)
                }

                override fun onMenuItemSelected(menuItem: MenuItem): Boolean {
                    //здесь буквально нет ничего
                    return false
                }
            }, viewLifecycleOwner, Lifecycle.State.RESUMED
        )
    }

    @SuppressLint("CheckResult")
    private fun toSave() {
        if (binding.snapTitleInput.text!!.isNotEmpty()) {
            var commentText = binding.snapCommentInput.text.toString()
            if (commentText.isEmpty()) commentText = "default_comment_line"
            snapsRepository.insertSnap(
                FridgeSnap(
                    id = 0,
                    title = binding.snapTitleInput.text.toString(),
                    comment = commentText,
                    time = LocalDateTime.now().format(
                        DateTimeFormatter.ofPattern("hh:mm a")
                    ),
                    date = LocalDateTime.now().format(
                        DateTimeFormatter.ofPattern("dd MMM")
                    ),
                    image = actualImage.toString()
                )
            ).observeOn(AndroidSchedulers.mainThread()).subscribeOn(Schedulers.io()).subscribe({
                Log.d(
                    "ITEM_ADD", "Item (name = ${binding.snapTitleInput.text}) has been updated"
                )
                findNavController().popBackStack()
                Toast.makeText(
                    this@AddSnap.context,
                    getString(R.string.item_add_successful),
                    Toast.LENGTH_SHORT
                ).show()
            }, { error ->
                Log.d(
                    "ERROR", "Cannot add item (name = ${binding.snapTitleInput.text}). Code: $error"
                )
                Toast.makeText(
                    this@AddSnap.context, getString(R.string.item_add_failed), Toast.LENGTH_SHORT
                ).show()
            })


        }
    }

    private fun sysImageSelector() {
        pickedImage = registerForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
            if (uri != null) {
                actualImage = uri
                binding.snapImagePreview.setImageURI(actualImage)
            } else {
                Log.d("ADD_SNAP", "Can't handle Pick Media")
                Toast.makeText(
                    this@AddSnap.context, getString(R.string.pick_media_failed), Toast.LENGTH_SHORT
                ).show()
            }
        }

        takenImage = registerForActivityResult(
            ActivityResultContracts.TakePicture()
        ) { code ->
            if (code) {
                Log.d("path", actualImage.toString())
                binding.snapImagePreview.setImageURI(actualImage)
            } else {
                Log.d("ADD_SNAP", "Can't handle Take Photo")
                Toast.makeText(
                    this@AddSnap.context, getString(R.string.photo_take_failed), Toast.LENGTH_SHORT
                ).show()
            }
        }
    }


    private fun selectImage() {
        val options = arrayOf<CharSequence>(
            getString(R.string.alert_option_new_photo),
            getString(R.string.alert_option_pick_from_gallery),
            getString(R.string.alert_option_cancel)
        )
        val alertBuilder = AlertDialog.Builder(this.context)
        alertBuilder.setTitle(getString(R.string.alert_box_title))

        alertBuilder.setItems(options) { dialogInterface, item ->

            when (item) {
                0 -> {
                    actualImage = this.context?.let { it1 ->
                        FileProvider.getUriForFile(
                            it1, BuildConfig.APPLICATION_ID + ".provider", createImageFile()
                        )
                    }
                    takenImage?.launch(actualImage)
                }
                1 ->
                    pickedImage?.launch(
                    PickVisualMediaRequest(
                        ActivityResultContracts.PickVisualMedia.ImageOnly
                    )
                )
                2 -> dialogInterface.dismiss()
            }
        }
        alertBuilder.show()

    }

    private fun createImageFile(): File {
        // Create an image file name
        val timeStamp: String = SimpleDateFormat("yyyyMMdd_HHmmss").format(Date())
        val storageDir: File? = context?.getExternalFilesDir(Environment.DIRECTORY_PICTURES)
        return File.createTempFile(
            "IMG_${timeStamp}_", /* prefix */
            ".jpg", /* suffix */
            storageDir /* directory */
        ).apply {
            // Save a file: path for use with ACTION_VIEW intents
            currentPhotoPath = absolutePath
        }
    }


    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

}