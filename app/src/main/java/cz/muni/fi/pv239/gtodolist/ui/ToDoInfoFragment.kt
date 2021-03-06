package cz.muni.fi.pv239.gtodolist.ui

import android.graphics.Color
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.findNavController
import cz.muni.fi.pv239.gtodolist.R
import cz.muni.fi.pv239.gtodolist.api.CalendarExporter
import cz.muni.fi.pv239.gtodolist.api.CategoryViewModel
import cz.muni.fi.pv239.gtodolist.api.ToDoViewModel
import cz.muni.fi.pv239.gtodolist.model.Category
import cz.muni.fi.pv239.gtodolist.model.ToDo
import kotlinx.android.synthetic.main.activity_main.view.*
import kotlinx.android.synthetic.main.create_todo.*
import kotlinx.android.synthetic.main.fragment_todo_info.*
import kotlinx.android.synthetic.main.welcome_screen.view.*
import java.util.*

/**
 * Fragment that contains information of specific TODO task.
 */
class ToDoInfoFragment : Fragment() {

    private val TAG = ToDoInfoFragment::class.java.simpleName
    private lateinit var todoViewModel: ToDoViewModel
    private lateinit var activeCategory: String
    private lateinit var categoryViewModel: CategoryViewModel
    private lateinit var adapter: CategorySpinnerAdapter
    private lateinit var todo: ToDo
    private var categoriesLoaded: Boolean = false
    private var  todosLoaded: Boolean = false

    override fun onCreateView(
            inflater: LayoutInflater, container: ViewGroup?,
            savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.update_todo, container, false)
    }

    override fun onResume() {
        super.onResume()

        if(view != null){
            view!!.rootView.search_bar.visibility = View.GONE
            view!!.rootView.sort_button.visibility = View.GONE
            view!!.rootView.add_todo_fab.hide()
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        Log.d(TAG, "CREATIGN VIEW MODEL")

        todoViewModel = ViewModelProvider(this).get(ToDoViewModel::class.java)

        todoViewModel.allTodos.observe(viewLifecycleOwner, Observer {todos ->
            Log.d(TAG, "GOT ALL TODOS")
            Log.d(TAG, todos.toString())
            val id = arguments?.getLong("id")!!
            todo = todoViewModel.getTodoWithId(id)
            activeCategory = todo.category.name
            todosLoaded = true
            Log.d(TAG, "FOUND TODO $todo")

            todo_name_content.setText(todo.name)
            todo_description_content.setText(todo.description)

            seekBar.progress = todo.importance.toInt()

            button_done.setOnClickListener {
                todo.done = true
                todoViewModel.update(todo)
                findNavController().navigate(R.id.action_to_todo_list)
            }

            button_export.setOnClickListener {
                startActivity(CalendarExporter.createIntent(todo))
            }

            button_back.setOnClickListener {
                view.rootView.add_todo_fab.show()
                view.rootView.search_bar.visibility = View.VISIBLE
                view.rootView.sort_button.visibility = View.VISIBLE
                findNavController().navigate(R.id.action_to_todo_list)
            }

            add_todo_button.setOnClickListener{
                todo.name = todo_name_content.text.toString()
                todo.description = todo_description_content.text.toString()
                todo.category = category_spinner.selectedItem as Category
                todo.importance = seekBar.progress.toLong()
                todoViewModel.update(todo)
                findNavController().navigate(R.id.action_to_todo_list)
            }
            if(categoriesLoaded){
                var position = categoryViewModel.getAllCategories().indexOf(todo.category)
                category_spinner.setSelection(position)
            }
        })

        add_category_button.setOnClickListener{
            val builder =  AlertDialog.Builder(context!!)
            builder.setView(R.layout.create_category)

            val dialog = builder.create()
            dialog.show()
            dialog.findViewById<Button>(R.id.cancel_category_create)!!.setOnClickListener{
                Log.d(TAG, "CLICKED CANCEL")
                dialog.dismiss()
            }
            dialog.findViewById<Button>(R.id.create_category)!!.setOnClickListener{
                Log.d(TAG, "CLICKED CREATE")
                var categoryName = dialog.findViewById<EditText>(R.id.category_name)!!.text
                if(categoryName.length == 0){
                    // make toast that category was not added
                    Toast.makeText(
                        context,
                        "Fill category name",
                        Toast.LENGTH_SHORT
                    ).show()
                }else{
                    // add category
                    var categoryName = dialog.findViewById<EditText>(R.id.category_name)!!.text.toString()
                    var rnd = Random()
                    var color = Color.HSVToColor(floatArrayOf(rnd.nextInt(361).toFloat(), 0.6f, 0.8f)).toString(16).replace('-', '#')
                    categoryViewModel.insert(Category(categoryName, color))
                    Toast.makeText(
                        context,
                        "Category added",
                        Toast.LENGTH_SHORT
                    ).show()
                    dialog.dismiss()
                }

            }
        }

        categoryViewModel = ViewModelProvider(this).get(CategoryViewModel::class.java)
        categoryViewModel.userCategories.observe(viewLifecycleOwner, Observer {categories ->
            var items = categoryViewModel.getAllCategories()
            adapter.setCategories(items)
            if(todosLoaded){
                category_spinner.setSelection(items.indexOf(todo.category))
            }
            categoriesLoaded= true
        })

        adapter  = CategorySpinnerAdapter(this.context!!)
        category_spinner.adapter =  adapter
    }
}
