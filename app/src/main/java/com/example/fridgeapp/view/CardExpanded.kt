package com.example.fridgeapp.view

import android.Manifest
import android.app.Activity
import android.app.AlertDialog
import android.content.Context
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.*
import android.view.inputmethod.InputMethodManager
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.app.ActivityCompat
import androidx.core.net.toUri
import androidx.core.view.MenuHost
import androidx.core.view.MenuProvider
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.MutableLiveData
import androidx.navigation.fragment.findNavController
import com.example.fridgeapp.R
import com.example.fridgeapp.data.FridgeSnap
import com.example.fridgeapp.databinding.FragmentCardExpandedBinding
import com.example.fridgeapp.handlers.FileSystemInteractor
import com.example.fridgeapp.injector.repository.SnapsRepository
import com.example.fridgeapp.loaders.FridgeApp
import com.google.android.material.snackbar.Snackbar
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.schedulers.Schedulers
import javax.inject.Inject

//раскрытая запись
class CardExpanded : Fragment() {
    private var fridgeSnap: FridgeSnap? = null

    private var _binding: FragmentCardExpandedBinding? = null


    private var imageUriToSave: Uri? = null

    private lateinit var fileSystemInteractor: FileSystemInteractor

    private val uriLiveData = MutableLiveData<Uri?>()

    private var permissionRequestLauncher: ActivityResultLauncher<Array<String>>? = null

    @Inject
    lateinit var snapsRepository: SnapsRepository

    private val binding get() = _binding!!

    private val compositeDisposable = CompositeDisposable()


    //раскрытая запись очень похожа на создание новой, так как тут update, методы оттуда же перекочевали, делать интерфейс
    //ради 2 классов не стал, просто скопировал, особенно 1 в 1 скопировано было, конечно же, обновление изображения
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        //получаем инфу с навигатора (safeargs)
        fridgeSnap = CardExpandedArgs.fromBundle(requireArguments()).fridgeSnap

        fileSystemInteractor = FileSystemInteractor(
            requireActivity().activityResultRegistry, requireContext(), uriLiveData
        )
        lifecycle.addObserver(fileSystemInteractor)
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

        imageUriToSave = fridgeSnap?.image!!.toUri()


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
            if (fileSystemInteractor.permissionCheck()) sourceChooserAlert()
            else {
                if (Build.VERSION.SDK_INT < 33) {
                    permissionRequestLauncher!!.launch(
                        arrayOf(
                            Manifest.permission.READ_EXTERNAL_STORAGE,
                            Manifest.permission.WRITE_EXTERNAL_STORAGE
                        )
                    )
                } else {
                    ActivityCompat.shouldShowRequestPermissionRationale(
                        requireActivity(), Manifest.permission.READ_MEDIA_IMAGES
                    )
                    permissionRequestLauncher!!.launch(arrayOf(Manifest.permission.READ_MEDIA_IMAGES))
                }
            }
        }

        permissionRequestLauncher =
            registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { permissions ->
                run {
                    val granted = permissions.entries.all {
                        it.value
                    }
                    if (granted) {
                        sourceChooserAlert()
                    }
                }
            }


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
                                    Snackbar.make(
                                        requireView(),
                                        getString(R.string.item_delete_successful),
                                        Snackbar.LENGTH_LONG
                                    ).setAction(getString(R.string.undo)) { deleteUndo() }.show()
                                }, { error ->
                                    Log.d(
                                        "ERROR",
                                        "Cannot delete item (name = ${fridgeSnap?.title!!}). Code: $error"
                                    )
                                    Snackbar.make(
                                        requireView(),
                                        getString(R.string.item_delete_failed),
                                        Snackbar.LENGTH_SHORT
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

    private fun deleteUndo() {
        val disposable =
            snapsRepository.insertSnap(fridgeSnap!!).observeOn(AndroidSchedulers.mainThread())
                .subscribeOn(Schedulers.io()).subscribe {
                    Log.d("UNDO", "Complete Undo item deletion (id = ${fridgeSnap!!.id})")
                }
    }

    private fun toUpdate() {
        if (binding.snapTitleOutput.text != null) {
            val comment: String =
                if (binding.snapCommentOutput.text != null) binding.snapCommentOutput.text.toString()
                else "null"
            val disposable = snapsRepository.updateSnap(
                FridgeSnap(
                    id = fridgeSnap?.id!!,
                    date = fridgeSnap?.date,
                    time = fridgeSnap?.time,
                    title = binding.snapTitleOutput.text.toString(),
                    comment = comment,
                    image = imageUriToSave.toString()
                )
            ).observeOn(AndroidSchedulers.mainThread()).subscribeOn(Schedulers.io()).subscribe({
                Log.d(
                    "ITEM_UPDATE", "Item (name = ${fridgeSnap?.title!!}) has been updated"
                )
                Snackbar.make(
                    requireView(), getString(R.string.item_edit_successful), Snackbar.LENGTH_LONG
                ).show()
            }, { error ->
                Log.d(
                    "ERROR", "Cannot update item (name = ${fridgeSnap?.title!!}). Code: $error"
                )
                Snackbar.make(
                    requireView(), getString(R.string.item_edit_failed), Snackbar.LENGTH_SHORT
                ).show()
            })
            compositeDisposable.add(disposable)
        }
    }

    private fun fieldsInit() {
        binding.snapTitleOutput.setText(fridgeSnap?.title)
        if (fridgeSnap?.comment == "default_comment_line" || fridgeSnap?.comment?.length == 0) binding.snapCommentOutput.setText(
            getString(R.string.no_comments)
        )
        else binding.snapCommentOutput.setText(fridgeSnap?.comment)
        if (fridgeSnap?.image != "null") binding.snapImage.setImageURI(fridgeSnap?.image?.toUri())
        else binding.snapImage.setImageResource(R.drawable.fridge_preview)
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
    private fun sourceChooserAlert() {
        val options = arrayOf<CharSequence>(
            getString(R.string.alert_option_new_photo),
            getString(R.string.alert_option_pick_from_gallery),
            getString(R.string.alert_option_set_default),
            getString(R.string.alert_option_cancel)
        )
        val alertBuilder = AlertDialog.Builder(this.context)
        alertBuilder.setTitle(getString(R.string.alert_box_title))

        val observer = androidx.lifecycle.Observer<Uri?> { uri ->
            binding.snapImage.setImageURI(uri)
            imageUriToSave = uri
        }

        alertBuilder.setItems(options) { dialogInterface, item ->
            when (item) {
                0 -> {
                    uriLiveData.observe(viewLifecycleOwner, observer)
                    fileSystemInteractor.takePhoto()
                }
                1 -> {
                    uriLiveData.observe(viewLifecycleOwner, observer)
                    fileSystemInteractor.selectImage()
                }
                2 -> {
                    imageUriToSave = null
                    binding.snapImage.setImageResource(R.drawable.fridge_preview)
                }
                3 -> dialogInterface.dismiss()
            }
        }
        alertBuilder.show()
    }

    override fun onDestroy() {
        super.onDestroy()
        compositeDisposable.dispose()
    }

}