package com.emha.mynotesapp

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.View
import androidx.activity.result.ActivityResult
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.emha.mynotesapp.adapter.NoteAdapter
import com.emha.mynotesapp.databinding.ActivityMainBinding
import com.emha.mynotesapp.db.NoteHelper
import com.emha.mynotesapp.entity.Note
import com.emha.mynotesapp.helper.MappingHelper
import com.google.android.material.snackbar.Snackbar
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var noteAdapter: NoteAdapter

    val resultLauncher: ActivityResultLauncher<Intent> = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        // Akan dipanggil jika request codenya ADD
        when (result.resultCode) {
            NoteAddUpdateActivity.RESULT_ADD -> {
                val note =
                    result.data?.getParcelableExtra<Note>(NoteAddUpdateActivity.EXTRA_NOTE) as Note
                noteAdapter.addItem(note)
                binding.rvNotes.smoothScrollToPosition(noteAdapter.itemCount - 1)
                showSnackbarMessage("Satu item berhasil ditambahkan")
            }

            NoteAddUpdateActivity.RESULT_UPDATE -> {
                val note =
                    result.data?.getParcelableExtra<Note>(NoteAddUpdateActivity.EXTRA_NOTE) as Note
                val position =
                    result.data?.getIntExtra(NoteAddUpdateActivity.EXTRA_POSITION, 0) as Int
                noteAdapter.updateItem(position, note)
                binding.rvNotes.smoothScrollToPosition(position)
                showSnackbarMessage("Satu item berhasil diubah")
            }

            NoteAddUpdateActivity.RESULT_DELETE -> {
                val position =
                    result.data?.getIntExtra(NoteAddUpdateActivity.EXTRA_POSITION, 0) as Int
                noteAdapter.removeItem(position)
                showSnackbarMessage("Satu item berhasil dihapus")
            }
        }
    }

    companion object {
        private const val EXTRA_STATE = "extra_state"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        supportActionBar?.title = "Notes"

        binding.rvNotes.layoutManager = LinearLayoutManager(this)
        binding.rvNotes.setHasFixedSize(true)

        noteAdapter = NoteAdapter(object : NoteAdapter.OnItemClickCallback {
            override fun onItemClicked(selectedNote: Note?, position: Int?) {
                val intent = Intent(this@MainActivity, NoteAddUpdateActivity::class.java)
                intent.putExtra(NoteAddUpdateActivity.EXTRA_NOTE, selectedNote)
                intent.putExtra(NoteAddUpdateActivity.EXTRA_POSITION, position)
                resultLauncher.launch(intent)
            }
        })
        binding.rvNotes.adapter = noteAdapter

        binding.fabAdd.setOnClickListener {
            val intent = Intent(this@MainActivity, NoteAddUpdateActivity::class.java)
            resultLauncher.launch(intent)
        }

        if (savedInstanceState == null) {
            // proses ambil data
            loadNotesAsync()
        } else {
            val list = savedInstanceState.getParcelableArrayList<Note>(EXTRA_STATE)
            if (list != null) {
                noteAdapter.listNotes = list
            }
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putParcelableArrayList(EXTRA_STATE, noteAdapter.listNotes)
    }

    private fun loadNotesAsync() {
        lifecycleScope.launch {

            binding.progressbar.visibility = View.VISIBLE
            val noteHelper = NoteHelper.getInstance(applicationContext)
            noteHelper.open()

            val deferredNotes = async(Dispatchers.IO) {
                val cursor = noteHelper.queryAll()
                MappingHelper.mapCursorToArrayList(cursor)
            }

            binding.progressbar.visibility = View.INVISIBLE
            val notes = deferredNotes.await()

            if (notes.size > 0) {
                noteAdapter.listNotes = notes
            } else {
                noteAdapter.listNotes = ArrayList()
                showSnackbarMessage("Tidak ada data saat ini")
            }
            noteHelper.close()
        }
    }

    private fun showSnackbarMessage(message: String) {
        Snackbar.make(binding.rvNotes, message, Snackbar.LENGTH_SHORT).show()
    }
}