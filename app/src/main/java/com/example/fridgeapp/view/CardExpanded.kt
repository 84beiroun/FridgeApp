package com.example.fridgeapp.view

import android.app.Activity
import android.app.AlertDialog
import android.content.Context
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
import androidx.core.content.FileProvider
import androidx.core.net.toUri
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
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

    lateinit var currentPhotoPath: String

    @Inject
    lateinit var snapsRepository: SnapsRepository

    private val binding get() = _binding!!

    //раскрытая запись очень похожа на создание новой, так как тут update, методы оттуда же перекочевали, делать интерфейс
    //ради 2 классов не стал, просто скопировал, особенно 1 в 1 скопировано было, конечно же, обновление изображения
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        //получаем инфу с навигатора (safeargs)
        fridgeSnap = CardExpandedArgs.fromBundle(requireArguments()).fridgeSnap
        setHasOptionsMenu(true)

        //хэндлим обращение к андройду, загрузка картинок
        sysImageSelector()

    }

    //добавляем меню
    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        menu.clear()
        inflater.inflate(R.menu.menu_snap, menu)
        super.onCreateOptionsMenu(menu, inflater)
    }

    //хэнндлим кнопки меню
    override fun onOptionsItemSelected(item: MenuItem): Boolean = when (item.itemId) {
        R.id.snap_edit -> {
            //функция анлока полей
            fieldsUnlockHandler()
            true
        }
        R.id.delete_snap -> {
            snapsRepository.deleteSnap(fridgeSnap?.id!!)
                .observeOn(AndroidSchedulers.mainThread())
                .subscribeOn(Schedulers.io())
                .subscribe({
                    Log.d(
                        "ITEM_DELETE",
                        "Item (name = ${fridgeSnap?.title!!}) has been deleted"
                    )
                    findNavController().popBackStack()
                    Toast.makeText(
                        this@CardExpanded.context,
                        "Item has been deleted!",
                        Toast.LENGTH_SHORT
                    ).show()
                },
                    { error ->
                        Log.d(
                            "ERROR",
                            "Cannot delete item (name = ${fridgeSnap?.title!!}). Code: $error"
                        )
                        Toast.makeText(
                            this@CardExpanded.context,
                            "Failed to delete item!",
                            Toast.LENGTH_SHORT
                        ).show()
                    })

            true
        }
        else -> super.onOptionsItemSelected(item)
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
        binding.changeImageTxt.setOnClickListener { selectImage() }

        return binding.root
    }


    private fun toUpdate() {
        if (binding.snapTitleOutput.text != null) {
            val comment: String
            if (binding.snapCommentOutput.text != null) comment =
                binding.snapCommentOutput.text.toString()
            else comment = "null"
            snapsRepository.updateSnap(
                FridgeSnap(
                    id = fridgeSnap?.id!!,
                    date = fridgeSnap?.date,
                    time = fridgeSnap?.time,
                    title = binding.snapTitleOutput.text.toString(),
                    comment = comment,
                    image = actualImage.toString()
                )
            )
                .observeOn(AndroidSchedulers.mainThread())
                .subscribeOn(Schedulers.io())
                .subscribe({
                    Log.d(
                        "ITEM_UPDATE",
                        "Item (name = ${fridgeSnap?.title!!}) has been updated"
                    )
                    Toast.makeText(
                        this@CardExpanded.context,
                        "Item has been updated!",
                        Toast.LENGTH_SHORT
                    ).show()
                },
                    { error ->
                        Log.d(
                            "ERROR",
                            "Cannot update item (name = ${fridgeSnap?.title!!}). Code: $error"
                        )
                        Toast.makeText(
                            this@CardExpanded.context,
                            "Failed to update item!",
                            Toast.LENGTH_SHORT
                        ).show()
                    })
        }
    }

    private fun fieldsInit() {
        binding.snapTitleOutput.setText(fridgeSnap?.title)
        if (fridgeSnap?.comment == "default_comment_line" || fridgeSnap?.comment?.length == 0)
            binding.snapCommentOutput.setText("No commentary was provided...")
        else
            binding.snapCommentOutput.setText(fridgeSnap?.comment)
        if (fridgeSnap?.image != "null") binding.snapImage.setImageURI(fridgeSnap?.image?.toUri())
        else binding.snapImage.setImageResource(R.drawable.fridge_preview)
    }

    private fun sysImageSelector() {
        //контракт на выбор фото из галереи
        pickedImage = registerForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
            if (uri != null) {
                actualImage = uri
                binding.snapImage.setImageURI(actualImage)
            } else Log.d("EXPANDED_CARD", "Can't handle Pick Media")
        }

        //контракт нового фото
        takenImage = registerForActivityResult(
            ActivityResultContracts.TakePicture()
        ) { code ->
            if (code) {
                binding.snapImage.setImageURI(actualImage)
            } else Log.d("EXPANDED_CARD", "Can't handle Take Photo")
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
        binding.snapTitleOutput.background = null
        binding.snapTitleOutput.setTextIsSelectable(false)

        binding.snapCommentOutput.isFocusable = false
        binding.snapCommentOutput.isClickable = false
        binding.snapCommentOutput.isCursorVisible = false
        binding.snapCommentOutput.background = null
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
        binding.snapTitleOutput.setBackgroundResource(androidx.appcompat.R.drawable.abc_edit_text_material)

        binding.snapCommentOutput.isFocusable = true
        binding.snapCommentOutput.isClickable = true
        binding.snapCommentOutput.isCursorVisible = true
        binding.snapCommentOutput.setTextIsSelectable(true)
        binding.snapCommentOutput.setBackgroundResource(androidx.appcompat.R.drawable.abc_edit_text_material)

        binding.saveSnapEditButton.isEnabled = true
        binding.saveSnapEditButton.isVisible = true

        binding.changeImageTxt.isVisible = true
        binding.changeImageTxt.isEnabled = true

        Log.d("sdsd", fridgeSnap.toString())
        if (fridgeSnap?.comment == "default_comment_line")
            binding.snapCommentOutput.text = null
    }

    //метод выбора действия
    private fun selectImage() {
        val options =
            arrayOf<CharSequence>("Take Photo", "Pick from Gallery", "Set Default", "Cancel")
        val alertBuilder = AlertDialog.Builder(this.context)
        alertBuilder.setTitle("Choose a picture")

        alertBuilder.setItems(options) { dialogInterface, item ->
            when (options[item]) {
                "Take Photo" -> {
                    takenImage?.launch(actualImage)
                    actualImage = this.context?.let { it1 ->
                        FileProvider.getUriForFile(
                            it1, BuildConfig.APPLICATION_ID + ".provider", createImageFile()
                        )
                    }
                }
                "Pick from Gallery" -> pickedImage?.launch(
                    PickVisualMediaRequest(
                        ActivityResultContracts.PickVisualMedia.ImageOnly
                    )
                )
                "Set Default" -> {
                    actualImage = null
                    binding.snapImage.setImageResource(R.drawable.fridge_preview)
                }
                "Cancel" -> dialogInterface.dismiss()
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