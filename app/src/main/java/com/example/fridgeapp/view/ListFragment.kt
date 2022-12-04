package com.example.fridgeapp.view

import android.annotation.SuppressLint
import android.content.Context
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.fridgeapp.R
import com.example.fridgeapp.data.FridgeSnap
import com.example.fridgeapp.databinding.FragmentListBinding
import com.example.fridgeapp.helpers.ItemsDivider
import com.example.fridgeapp.handlers.RecycleAdapter
import com.example.fridgeapp.injector.repository.SnapsRepository
import com.example.fridgeapp.loaders.FridgeApp
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.schedulers.Schedulers
import javax.inject.Inject

//home фрагмент с recycleview
class ListFragment : Fragment() {

    //инъекция dagger с бд room, будет встречаться в фрагментах
    @Inject
    lateinit var snapsRepository: SnapsRepository

    private var _binding: FragmentListBinding? = null

    private val binding get() = _binding!!

    private var adapter: RecycleAdapter? = null

    private val compositeDisposable = CompositeDisposable()

    override fun onAttach(context: Context) {
        super.onAttach(context)
        //получаем инстанцию действующей бд
        FridgeApp.dbInstance.inject(this)
    }

    // на создании вью стандартный для каждого фрагмента, тут мы биндим элементы
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = FragmentListBinding.inflate(inflater, container, false)
        adapter = RecycleAdapter(listOf())
        val layoutManager = LinearLayoutManager(this@ListFragment.context)
        layoutManager.reverseLayout = true
        layoutManager.stackFromEnd = true
        binding.fridgeItemsList.layoutManager = layoutManager
        binding.fridgeItemsList.adapter = adapter

        val dividerItemDecoration = ItemsDivider(this@ListFragment.context, RecyclerView.VERTICAL)
        dividerItemDecoration.setDrawable(ContextCompat.getDrawable(this@ListFragment.requireContext(), R.drawable.item_divider)!!)
        binding.fridgeItemsList.addItemDecoration(dividerItemDecoration)

        return binding.root
    }

    override fun onResume() {
        super.onResume()
        subscribeOnList()
    }


    //подписываемся на обновление листа
    private fun subscribeOnList() {
        //получаем обзёрвабл лист записей через дао (репозиторий)
        val fridgeSnaps = snapsRepository.getAll()
        val disposable = fridgeSnaps
            .observeOn(AndroidSchedulers.mainThread())
            .subscribeOn(Schedulers.io())
            .subscribe({ response -> onListChange(response) }, { error ->
                Log.d(
                    "ERROR", "Cannot load a list. Code: $error"
                )
                if (_binding != null) {
                    binding.loadingSpinner.visibility = View.INVISIBLE
                    binding.firstLaunchText.visibility = View.VISIBLE
                    binding.firstLaunchText.text = getString(R.string.list_error_msg) + error
                }
            })
        compositeDisposable.add(disposable)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }


    private fun onListChange(fridgeSnaps: List<FridgeSnap>) {
        Log.d("LIST_LOADED", "List has been loaded")
        if (_binding != null)
            if (fridgeSnaps.isNotEmpty()) {
                Log.d(
                    "LIST_NOT_EMPTY",
                    "This list is not empty and has ${fridgeSnaps.size} items in it"
                )
                //спинер на случай отсутствия связи с бд
                binding.loadingSpinner.visibility = View.INVISIBLE
                binding.fridgeItemsList.visibility = View.VISIBLE
                //вот и адаптер
                adapter = RecycleAdapter(fridgeSnaps)
                binding.fridgeItemsList.adapter = adapter
                //по нажатию на элемент
                adapter?.onItemClick = { fridgeSnap ->
                    findNavController().navigate(
                        ListFragmentDirections.actionListFragmentToCardExpanded(fridgeSnap)
                    )
                }
            } else {
                //на случай первого запуска
                binding.loadingSpinner.visibility = View.INVISIBLE
                binding.firstLaunchText.visibility = View.VISIBLE
            }
    }

    override fun onDestroy() {
        super.onDestroy()
        compositeDisposable!!.dispose()
    }
}