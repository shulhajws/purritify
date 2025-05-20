package com.example.purrytify.ui.library

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.PopupMenu
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.purrytify.R
import com.example.purrytify.databinding.ItemLibrarySongBinding
import com.example.purrytify.model.Song
import com.example.purrytify.ui.playback.PlayerViewModel

class LibrarySongAdapter(
    private val onSongClick: (Song) -> Unit,
    private val playerViewModel: PlayerViewModel
) : ListAdapter<Song, LibrarySongAdapter.SongViewHolder>(SongDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SongViewHolder {
        val binding = ItemLibrarySongBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return SongViewHolder(binding)
    }

    override fun onBindViewHolder(holder: SongViewHolder, position: Int) {
        val song = getItem(position)
        holder.bind(song)
    }

    inner class SongViewHolder(private val binding: ItemLibrarySongBinding) : RecyclerView.ViewHolder(binding.root) {
        init {
            binding.root.setOnClickListener {
                val position = bindingAdapterPosition
                if (position != RecyclerView.NO_POSITION) {
                    val song = getItem(position)
                    onSongClick(song)
                }
            }

            binding.root.setOnLongClickListener {
                val position = bindingAdapterPosition
                if (position != RecyclerView.NO_POSITION) {
                    val song = getItem(position)
                    showPopupMenu(it, song)
                    return@setOnLongClickListener true
                }
                return@setOnLongClickListener false
            }

            binding.btnMore.setOnClickListener {
                val position = bindingAdapterPosition
                if (position != RecyclerView.NO_POSITION) {
                    val song = getItem(position)
                    showPopupMenu(it, song)
                }
            }
        }

        private fun showPopupMenu(view: View, song: Song) {
            val popup = PopupMenu(view.context, view)
            popup.inflate(R.menu.menu_song_item)

            popup.setOnMenuItemClickListener { menuItem ->
                when (menuItem.itemId) {
                    R.id.action_add_to_queue -> {
                        playerViewModel.addToQueue(song)
                        true
                    }
                    else -> false
                }
            }

            popup.show()
        }

        fun bind(song: Song) {
            binding.txtSongTitle.text = song.title
            binding.txtArtistName.text = song.artist

            Glide.with(binding.imgAlbumArt)
                .load(song.albumArt)
                .placeholder(R.drawable.placeholder_album_art)
                .error(R.drawable.placeholder_album_art)
                .centerCrop()
                .into(binding.imgAlbumArt)
        }
    }

    private class SongDiffCallback : DiffUtil.ItemCallback<Song>() {
        override fun areItemsTheSame(oldItem: Song, newItem: Song): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: Song, newItem: Song): Boolean {
            return oldItem == newItem
        }
    }
}