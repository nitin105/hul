package com.hul.curriculam.ui.schoolCode

import android.content.Context
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.ImageView
import android.widget.TextView
import com.google.gson.Gson
import com.hul.R
import com.hul.data.ProjectInfo
import com.hul.data.SchoolCode
import java.util.Locale

class SchoolCodeAdapter(
    context: Context,
    private val resource: Int,
    private var items: List<SchoolCode>
) :
    ArrayAdapter<SchoolCode>(context, resource, items) {

    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
        val view: View =
            convertView ?: LayoutInflater.from(context).inflate(resource, parent, false)

        val item = items[position]

        val itemImage: ImageView = view.findViewById(R.id.itemImage)
        val itemText: TextView = view.findViewById(R.id.itemText)

        if (item.external_id1 != null) {
            itemText.text = item.external_id1
        } else {
            itemText.text = item.external_id2
        }

        return view
    }

    fun updateVisits(newVisits: List<SchoolCode>) {
        items = newVisits
        notifyDataSetChanged()
    }

    override fun getFilter(): android.widget.Filter {
        return object : android.widget.Filter() {
            override fun performFiltering(constraint: CharSequence?): FilterResults {
                var filteredList = if (constraint.isNullOrEmpty()) {
                    items
                } else {
                    val filterPattern = constraint.toString().lowercase(Locale.getDefault()).trim()
                    items.filter {
                        (it.external_id1 != null && it.external_id1?.lowercase(Locale.getDefault())
                            ?.contains(filterPattern) == true)
                                || (it.external_id2 != null && it.external_id2?.lowercase(Locale.getDefault())
                            ?.contains(filterPattern) == true)
                    }
                }

                return FilterResults().apply {
                    values = filteredList
                    count = filteredList.size
                }
            }

            override fun publishResults(constraint: CharSequence?, results: FilterResults?) {
                clear()
                val schoolCodes = results?.values as List<SchoolCode>
                addAll(schoolCodes)
                notifyDataSetChanged()
            }
        }
    }
}