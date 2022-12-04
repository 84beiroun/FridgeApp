package com.example.fridgeapp.view

import android.Manifest
import android.app.AlertDialog
import android.content.Context
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.*
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.app.ActivityCompat
import androidx.core.view.MenuHost
import androidx.core.view.MenuProvider
import androidx.fragment.app.Fragment
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.MutableLiveData
import androidx.navigation.fragment.findNavController
import com.example.fridgeapp.R
import com.example.fridgeapp.data.FridgeSnap
import com.example.fridgeapp.databinding.FragmentAddSnapBinding
import com.example.fridgeapp.handlers.FileSystemInteractor
import com.example.fridgeapp.injector.repository.SnapsRepository
import com.example.fridgeapp.loaders.FridgeApp
import com.google.android.material.snackbar.Snackbar
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.schedulers.Schedulers
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
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

    private var permissionRequestLauncher: ActivityResultLauncher<Array<String>>? = null

    private var imageUriToSave: Uri? = null

    private lateinit var fileSystemInteractor: FileSystemInteractor

    private val uriLiveData = MutableLiveData<Uri?>()

    private val compositeDisposable = CompositeDisposable()

    override fun onAttach(context: Context) {
        super.onAttach(context)
        FridgeApp.dbInstance.inject(this)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        fileSystemInteractor = FileSystemInteractor(
            requireActivity().activityResultRegistry, requireContext(), uriLiveData
        )
        lifecycle.addObserver(fileSystemInteractor)
    }


    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = FragmentAddSnapBinding.inflate(inflater, container, false)

        binding.imagePickerButton.setOnClickListener {
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

        binding.saveSnapButton.setOnClickListener { toSave() }

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

    private fun toSave() {
        if (binding.snapTitleInput.text!!.isNotEmpty()) {
            var commentText = binding.snapCommentInput.text.toString()
            if (commentText.isEmpty()) commentText = "default_comment_line"
            val disposable = snapsRepository.insertSnap(
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
                    image = imageUriToSave.toString()
                )
            ).observeOn(AndroidSchedulers.mainThread()).subscribeOn(Schedulers.io()).subscribe({
                Log.d(
                    "ITEM_ADD", "Item (name = ${binding.snapTitleInput.text}) has been added"
                )
                findNavController().popBackStack()
                Snackbar.make(
                    requireView(),
                    getString(R.string.item_add_successful),
                    Snackbar.LENGTH_LONG
                ).show()
            }, { error ->
                Log.d(
                    "ERROR", "Cannot add item (name = ${binding.snapTitleInput.text}). Code: $error"
                )
                Snackbar.make(
                    requireView(), getString(R.string.item_add_failed), Snackbar.LENGTH_SHORT
                ).show()
            })


            compositeDisposable.add(disposable)
        }
    }


    private fun sourceChooserAlert() {
        val options = arrayOf<CharSequence>(
            getString(R.string.alert_option_new_photo),
            getString(R.string.alert_option_pick_from_gallery),
            getString(R.string.alert_option_cancel)
        )
        val alertBuilder = AlertDialog.Builder(this.context)
        alertBuilder.setTitle(getString(R.string.alert_box_title))

        val observer = androidx.lifecycle.Observer<Uri?> { uri ->
            binding.snapImagePreview.setImageURI(uri)
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
                2 -> dialogInterface.dismiss()
            }
        }
        alertBuilder.show()
    }


    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
        compositeDisposable.dispose()
    }

}