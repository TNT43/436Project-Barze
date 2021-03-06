package com.example.temp

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import android.widget.CheckBox
import android.widget.ImageButton
import android.widget.TextView

// This serves as the List adapter for the Reviews listview.
class TaskAdapterReview(context: Context, taskList: MutableList<Review>) : BaseAdapter() {

    private val _inflater: LayoutInflater = LayoutInflater.from(context)
    private var _taskList = taskList

    // The single view inflated for a single review item in the list
    override fun getView(position: Int, convertView: View?, parent: ViewGroup?): View {

        val itemText: String = _taskList.get(position).name +"   "+ _taskList.get(position).review

        val view: View
        val listRowHolder: ListRowHolder
        if (convertView == null) {
            // This is the xml file that represents each individual bar in the list.
            view = _inflater.inflate(R.layout.review_single_item, parent, false)
            listRowHolder = ListRowHolder(view)
            view.tag = listRowHolder
        } else {
            view = convertView
            listRowHolder = view.tag as ListRowHolder
        }

        listRowHolder.desc.text = itemText

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
        val desc: TextView = row!!.findViewById(R.id.review_text_view) as TextView
    }
}