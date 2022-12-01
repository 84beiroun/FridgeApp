package com.example.fridgeapp.helpers

import android.content.Context
import android.graphics.Rect
import android.view.View
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.RecyclerView

class ItemsDivider(context: Context?, orientation: Int) :
    DividerItemDecoration(context, orientation) {
    @Override
    override fun getItemOffsets(
        outRect: Rect,
        view: View,
        parent: RecyclerView,
        state: RecyclerView.State
    ) {
        super.getItemOffsets(outRect, view, parent, state)
            if (parent.getChildAdapterPosition(view) == state.itemCount - 1)
                outRect.top = outRect.bottom
    }
}