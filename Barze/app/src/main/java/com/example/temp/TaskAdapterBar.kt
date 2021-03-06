package com.example.temp

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*


// This class serves as the adapter for the list view of Bars in BarView.kt
class TaskAdapterBar(context: Context, taskList: MutableList<Bar>) : BaseAdapter() {

    private val _inflater: LayoutInflater = LayoutInflater.from(context)
    private var _taskList = taskList

    // The single view inflated for a single bar item in the list
    override fun getView(position: Int, convertView: View?, parent: ViewGroup?): View {

        val itemText: String = _taskList.get(position).name as String
        val itemImage: Int = _taskList.get(position).pic

        val view: View
        val listRowHolder: ListRowHolder
        if (convertView == null) {
            // This is the xml file that represents each individual bar in the list.
            view = _inflater.inflate(R.layout.bar_list_item, parent, false)
            listRowHolder = ListRowHolder(view)
            view.tag = listRowHolder
        } else {
            view = convertView
            listRowHolder = view.tag as ListRowHolder
        }

        listRowHolder.desc.text = itemText
        listRowHolder.pic.setImageResource(itemImage)
        return view
    }

    override fun getItem(index: Int): Any {
        return _taskList.get(index)
    }

    override fun getItemId(index: Int): Long {
        return index.toLong()
    }

    override fun getCount(): Int {
        return _taskList.size
    }

    private class ListRowHolder(row: View?) {
        val desc: TextView = row!!.findViewById(R.id.bar_text_view) as TextView
        var pic: ImageView = row!!.findViewById((R.id.bar_image_view)) as ImageView
    }
}