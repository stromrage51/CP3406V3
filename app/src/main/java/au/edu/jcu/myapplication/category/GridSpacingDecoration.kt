package au.edu.jcu.myapplication.category

import android.view.View
import androidx.recyclerview.widget.RecyclerView

class GridSpacingDecoration(
    private val space: Int
) : RecyclerView.ItemDecoration() {
    override fun getItemOffsets(outRect: android.graphics.Rect, v: View,
                                parent: RecyclerView, state: RecyclerView.State) {
        outRect.set(space, space, space, space)
    }
}