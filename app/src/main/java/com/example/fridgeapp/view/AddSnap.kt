package com.example.fridgeapp.view

import android.app.AlertDialog
import android.content.Context
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.util.Log
import android.view.*
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.FileProvider
import androidx.core.view.MenuHost
import androidx.core.view.MenuProvider
import androidx.fragment.app.Fragment
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

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        sysImageSelector()
        val menuHost: MenuHost = requireActivity()
        menuHost.addMenuProvider(object : MenuProvider {
            override fun onCreateMenu(menu: Menu, menuInflater: MenuInflater) {
                menu.clear()
                menuInflater.inflate(R.menu.menu_add, menu)
            }

            override fun onMenuItemSelected(menuItem: MenuItem): Boolean {
                //здесь буквально нет ничего
                return false
            }
        })
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = FragmentAddSnapBinding.inflate(inflater, container, false)

        binding.imagePickerButton.setOnClickListener { selectImage() }
        binding.saveSnapButton.setOnClickListener { toSave() }

        return binding.root
    }


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
                        "ITEM_ADD",
                        "Item (name = ${binding.snapTitleInput.text}) has been updated"
                    )
                    findNavController().popBackStack()
                    Toast.makeText(
                        this@AddSnap.context,
                        getString(R.string.item_add_successful),
                        Toast.LENGTH_SHORT
                    ).show()
                }, { error ->
                    Log.d(
                        "ERROR",
                        "Cannot add item (name = ${binding.snapTitleInput.text}). Code: $error"
                    )
                    Toast.makeText(
                        this@AddSnap.context,
                        getString(R.string.item_add_failed),
                        Toast.LENGTH_SHORT
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
        val options = arrayOf<CharSequence>("Take Photo", "Pick from Gallery", "Cancel")
        val alertBuilder = AlertDialog.Builder(this.context)
        alertBuilder.setTitle("Choose a picture")

        alertBuilder.setItems(options) { dialogInterface, item ->

            when (options[item]) {
                "Take Photo" -> {
                    actualImage = this.context?.let { it1 ->
                        FileProvider.getUriForFile(
                            it1, BuildConfig.APPLICATION_ID + ".provider", createImageFile()
                        )
                    }
                    takenImage?.launch(actualImage)
                }
                "Pick from Gallery" -> pickedImage?.launch(
                    PickVisualMediaRequest(
                        ActivityResultContracts.PickVisualMedia.ImageOnly
                    )
                )
                "Cancel" -> dialogInterface.dismiss()
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


    // private fun multiIntentHanlder() {
    //     multiIntent = registerForActivityResult(
//            ActivityResultContracts.StartActivityForResult()
//        ) { result: ActivityResult ->
//            if (result.resultCode == Activity.RESULT_OK) {
//                Log.d("sda", result.data.toString())
//            }
//        }
    //   }

    //   private fun intentSelectImage(){
    // методы вызова камеры / выбора из галлереи
//            takenImage?.launch(actualImage)
//            pickedImage?.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
    // попытка реализации интентов, много косяков.
//            val fileSourceIntent = Intent(Intent.ACTION_GET_CONTENT).setType("image/*")
//            val imageSourceIntent = Intent(Intent.ACTION_PICK, android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI).setType("image/*")
//            val takePictureIntent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
//            val intentChooser = Intent.createChooser(takePictureIntent, "Pick image source")
//            intentChooser.putExtra(Intent.EXTRA_INITIAL_INTENTS, arrayOf(imageSourceIntent, fileSourceIntent))
//            multiIntent?.launch(intentChooser)
    //   }

}