package com.example.fridgeapp.view

import android.content.Context
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.fridgeapp.R
import com.example.fridgeapp.databinding.FragmentListBinding
import com.example.fridgeapp.loaders.FridgeApp
import com.example.fridgeapp.handlers.RecycleAdapter
import com.example.fridgeapp.data.FridgeSnap
import com.example.fridgeapp.injector.repository.SnapsRepository
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.schedulers.Schedulers
import kotlinx.coroutines.*
import javax.inject.Inject

//home фрагмент с recycleview
class ListFragment : Fragment() {

    //инъекция dagger с бд room, будет встречаться в фрагментах
    @Inject
    lateinit var snapsRepository: SnapsRepository

    private var _binding: FragmentListBinding? = null

    private val binding get() = _binding!!

    private var adapter: RecycleAdapter? = null

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
        getItemsList()
        return binding.root
    }


    // на дестрое, конечно же, скидываем бинды
    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }


    //выделил основной код в отдельную функцию, для удобного обновления (нотифай косячный)
    //тут по сути получаем инфу с бд и пихаем её в адаптер
    private fun getItemsList() {
            //получаем лист записей через дао (репозиторий)
            val fridgeSnaps = snapsRepository.getAll()
            fridgeSnaps
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe({response -> onResponse(response)},
                    {error -> onFailure(error)})
    }
    private fun onResponse(fridgeSnaps: List<FridgeSnap>){
        if (fridgeSnaps.isNotEmpty()) {
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
                Log.d("SD", fridgeSnap.toString())
            }
        } else {
            //на случай первого запуска
            binding.loadingSpinner.visibility = View.INVISIBLE
            binding.firstLaunchText.visibility = View.VISIBLE
        }
    }
    private fun onFailure(error: Throwable) {
        binding.loadingSpinner.visibility = View.INVISIBLE
        binding.firstLaunchText.visibility = View.VISIBLE
        binding.firstLaunchText.text = "Ошибка... А точнее: $error"
    }
}