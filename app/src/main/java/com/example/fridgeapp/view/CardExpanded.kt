package com.example.fridgeapp.view

import android.Manifest
import android.app.Activity
import android.app.AlertDialog
import android.content.Context
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.util.Log
import android.view.*
import android.view.inputmethod.InputMethodManager
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.core.net.toUri
import androidx.core.view.MenuHost
import androidx.core.view.MenuProvider
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.lifecycle.Lifecycle
import androidx.navigation.fragment.findNavController
import com.example.fridgeapp.BuildConfig
import com.example.fridgeapp.R
import com.example.fridgeapp.data.FridgeSnap
import com.example.fridgeapp.databinding.FragmentCardExpandedBinding
import com.example.fridgeapp.injector.repository.SnapsRepository
import com.example.fridgeapp.loaders.FridgeApp
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.schedulers.Schedulers
import java.io.File
import java.text.SimpleDateFormat
import java.util.*
import javax.inject.Inject

//раскрытая запись
class CardExpanded : Fragment() {
    private var fridgeSnap: FridgeSnap? = null

    private var _binding: FragmentCardExpandedBinding? = null

    private var pickedImage: ActivityResultLauncher<PickVisualMediaRequest>? = null

    private var takenImage: ActivityResultLauncher<Uri>? = null

    private var actualImage: Uri? = null

    private lateinit var currentPhotoPath: String

    @Inject
    lateinit var snapsRepository: SnapsRepository

    private val binding get() = _binding!!


    //раскрытая запись очень похожа на создание новой, так как тут update, методы оттуда же перекочевали, делать интерфейс
    //ради 2 классов не стал, просто скопировал, особенно 1 в 1 скопировано было, конечно же, обновление изображения
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        //получаем инфу с навигатора (safeargs)
        fridgeSnap = CardExpandedArgs.fromBundle(requireArguments()).fridgeSnap

    }


    override fun onAttach(context: Context) {
        super.onAttach(context)
        FridgeApp.dbInstance.inject(this)
    }

    //биндим, тут же готовим элементы, тут же ставим онклики
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = FragmentCardExpandedBinding.inflate(inflater, container, false)

        //функция инита полей
        fieldsInit()

        //функция лока полей
        immutableHandler()

        actualImage = fridgeSnap?.image!!.toUri()


        binding.saveSnapEditButton.setOnClickListener {
            immutableHandler()
            //закрываем клавиатуру
            view?.let {
                val inputMethodManager =
                    activity?.getSystemService(Activity.INPUT_METHOD_SERVICE) as InputMethodManager
                inputMethodManager.hideSoftInputFromWindow(view?.windowToken, 0)
            }
            //функция обновления инфы в бд
            toUpdate()

        }

        //хэндлим кнопку смены картинки
        binding.changeImageTxt.setOnClickListener {
            if ((ContextCompat.checkSelfPermission(
                    context!!, Manifest.permission.READ_EXTERNAL_STORAGE
                ) == PackageManager.PERMISSION_GRANTED)
            ) selectImage()
            else requestPermissions(
                arrayOf(
                    Manifest.permission.READ_EXTERNAL_STORAGE,
                    Manifest.permission.WRITE_EXTERNAL_STORAGE
                ), 0
            )
        }

        //хэндлим обращение к андройду, загрузка картинок
        sysImageSelector()

        //инит меню
        menuSetup()

        return binding.root
    }


    private fun menuSetup() {
        val menuHost: MenuHost = requireActivity()
        menuHost.addMenuProvider(
            object : MenuProvider {
                override fun onCreateMenu(menu: Menu, menuInflater: MenuInflater) {
                    menu.clear()
                    menuInflater.inflate(R.menu.menu_snap, menu)
                }

                override fun onMenuItemSelected(menuItem: MenuItem): Boolean {
                    return when (menuItem.itemId) {
                        R.id.snap_edit -> {
                            //функция анлока полей
                            fieldsUnlockHandler()
                            true
                        }
                        R.id.delete_snap -> {
                            snapsRepository.deleteSnap(fridgeSnap?.id!!)
                                .observeOn(AndroidSchedulers.mainThread())
                                .subscribeOn(Schedulers.io()).subscribe({
                                    Log.d(
                                        "ITEM_DELETE",
                                        "Item (name = ${fridgeSnap?.title!!}) has been deleted"
                                    )
                                    findNavController().popBackStack()
                                    Toast.makeText(
                                        this@CardExpanded.context,
                                        getString(R.string.item_delete_successful),
                                        Toast.LENGTH_SHORT
                                    ).show()
                                }, { error ->
                                    Log.d(
                                        "ERROR",
                                        "Cannot delete item (name = ${fridgeSnap?.title!!}). Code: $error"
                                    )
                                    Toast.makeText(
                                        this@CardExpanded.context,
                                        getString(R.string.item_delete_failed),
                                        Toast.LENGTH_SHORT
                                    ).show()
                                })
                            true
                        }
                        else -> false
                    }
                }

            }, viewLifecycleOwner, Lifecycle.State.RESUMED
        )
    }

    private fun toUpdate() {
        if (binding.snapTitleOutput.text != null) {
            val comment: String =
                if (binding.snapCommentOutput.text != null) binding.snapCommentOutput.text.toString()
                else "null"
            snapsRepository.updateSnap(
                FridgeSnap(
                    id = fridgeSnap?.id!!,
                    date = fridgeSnap?.date,
                    time = fridgeSnap?.time,
                    title = binding.snapTitleOutput.text.toString(),
                    comment = comment,
                    image = actualImage.toString()
                )
            ).observeOn(AndroidSchedulers.mainThread()).subscribeOn(Schedulers.io()).subscribe({
                Log.d(
                    "ITEM_UPDATE", "Item (name = ${fridgeSnap?.title!!}) has been updated"
                )
                Toast.makeText(
                    this@CardExpanded.context,
                    getString(R.string.item_edit_successful),
                    Toast.LENGTH_SHORT
                ).show()
            }, { error ->
                Log.d(
                    "ERROR", "Cannot update item (name = ${fridgeSnap?.title!!}). Code: $error"
                )
                Toast.makeText(
                    this@CardExpanded.context,
                    getString(R.string.item_edit_failed),
                    Toast.LENGTH_SHORT
                ).show()
            })
        }
    }

    private fun fieldsInit() {
        binding.snapTitleOutput.setText(fridgeSnap?.title)
        if (fridgeSnap?.comment == "default_comment_line" || fridgeSnap?.comment?.length == 0) binding.snapCommentOutput.setText(
            "No commentary was provided..."
        )
        else binding.snapCommentOutput.setText(fridgeSnap?.comment)
        if (fridgeSnap?.image != "null") binding.snapImage.setImageURI(fridgeSnap?.image?.toUri())
        else binding.snapImage.setImageResource(R.drawable.fridge_preview)
    }

    private fun sysImageSelector() {
        //контракт на выбор фото из галереи
        pickedImage = registerForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
            if (uri != null) {
                actualImage = uri
                binding.snapImage.setImageURI(actualImage)
            } else {
                Log.d("ADD_SNAP", "Can't handle Pick Media")
                Toast.makeText(
                    this@CardExpanded.context,
                    getString(R.string.pick_media_failed),
                    Toast.LENGTH_SHORT
                ).show()
            }
        }

        //контракт нового фото
        takenImage = registerForActivityResult(
            ActivityResultContracts.TakePicture()
        ) { code ->
            if (code) {
                binding.snapImage.setImageURI(actualImage)
            } else {
                Log.d("ADD_SNAP", "Can't handle Take Photo")
                Toast.makeText(
                    this@CardExpanded.context,
                    getString(R.string.photo_take_failed),
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    //хэндлер выключения всех элементов (на вывод)
    private fun immutableHandler() {
        binding.snapTitleOutput.isFocusable = false
        binding.snapTitleOutput.isClickable = false
        binding.snapTitleOutput.isCursorVisible = false
        binding.snapTitleOutput.setTextIsSelectable(false)

        binding.snapCommentOutput.isFocusable = false
        binding.snapCommentOutput.isClickable = false
        binding.snapCommentOutput.isCursorVisible = false
        binding.snapCommentOutput.setTextIsSelectable(false)

        binding.saveSnapEditButton.isVisible = false
        binding.saveSnapEditButton.isEnabled = false

        binding.changeImageTxt.isVisible = false
        binding.changeImageTxt.isEnabled = false
    }

    //хэндлек включения всех элементов (ввод)
    private fun fieldsUnlockHandler() {
        binding.snapTitleOutput.isFocusable = true
        binding.snapTitleOutput.isClickable = true
        binding.snapTitleOutput.isCursorVisible = true
        binding.snapTitleOutput.setTextIsSelectable(true)

        binding.snapCommentOutput.isFocusable = true
        binding.snapCommentOutput.isClickable = true
        binding.snapCommentOutput.isCursorVisible = true
        binding.snapCommentOutput.setTextIsSelectable(true)

        binding.saveSnapEditButton.isEnabled = true
        binding.saveSnapEditButton.isVisible = true

        binding.changeImageTxt.isVisible = true
        binding.changeImageTxt.isEnabled = true

        if (fridgeSnap?.comment == "default_comment_line") binding.snapCommentOutput.text = null
    }

    //метод выбора действия
    private fun selectImage() {
        val options = arrayOf<CharSequence>(
            getString(R.string.alert_option_new_photo),
            getString(R.string.alert_option_pick_from_gallery),
            getString(R.string.alert_option_set_default),
            getString(R.string.alert_option_cancel)
        )
        val alertBuilder = AlertDialog.Builder(this.context)
        alertBuilder.setTitle(getString(R.string.alert_box_title))

        alertBuilder.setItems(options) { dialogInterface, item ->
            when (item) {
                0 -> {
                    takenImage?.launch(actualImage)
                    actualImage = this.context?.let { it1 ->
                        FileProvider.getUriForFile(
                            it1, BuildConfig.APPLICATION_ID + ".provider", createImageFile()
                        )
                    }
                }
                1 -> pickedImage?.launch(
                    PickVisualMediaRequest(
                        ActivityResultContracts.PickVisualMedia.ImageOnly
                    )
                )
                2 -> {
                    actualImage = null
                    binding.snapImage.setImageResource(R.drawable.fridge_preview)
                }
                3 -> dialogInterface.dismiss()
            }
        }
        alertBuilder.show()
    }

    //создания файла в который потом сохраняться будет изображение
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
}