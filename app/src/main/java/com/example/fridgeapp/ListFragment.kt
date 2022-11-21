package com.example.fridgeapp

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.fridgeapp.databinding.FragmentListBinding
import com.example.fridgeapp.helpers.RecycleAdapter
import com.example.fridgeapp.helpers.data.FridgeSnap
import com.example.fridgeapp.helpers.repository.SnapsRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
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
        adapter = RecycleAdapter(listOf<FridgeSnap>())
        binding.fridgeItemsList.layoutManager = LinearLayoutManager(this@ListFragment.context)
        binding.fridgeItemsList.adapter = adapter
        return binding.root
    }

    //вызываем для обновления
    override fun onResume() {
        super.onResume()
        listHandler()
    }


    // на дестрое, конечно же, скидываем бинды
    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }


    //выделил основной код в отдельную функцию, для удобного обновления (нотифай косячный)
    //тут по сути получаем инфу с бд и пихаем её в адаптер
    private fun listHandler() {
        //корутина
        GlobalScope.launch(Dispatchers.IO) {
            //получаем лист записей через дао (репозиторий)
            val fridgeSnaps: List<FridgeSnap> = snapsRepository.getAll()
            withContext(Dispatchers.Main) {
                if (fridgeSnaps.isNotEmpty()) {
                    //спинер на случай отсутствия связи с бд
                    binding.loadingSpinner.visibility = View.INVISIBLE
                    binding.fridgeItemsList.visibility = View.VISIBLE
                    //вот и адаптер
                    adapter = RecycleAdapter(fridgeSnaps.sortedByDescending { it.id })
                    binding.fridgeItemsList.adapter = adapter
                    //по нажатию на элемент
                    adapter?.onItemClick = { fridgeSnap ->
                        findNavController().navigate(
                            R.id.action_listFragment_to_cardExpanded,
                            bundleOf("snapBundlePointer" to fridgeSnap)
                        )
                    }
                } else {
                    //на случай первого запуска
                    binding.loadingSpinner.visibility = View.INVISIBLE
                    binding.firstLaunchText.visibility = View.VISIBLE
                }
            }
        }
    }
}