package com.nicewuerfel.musicbot;

import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.nicewuerfel.musicbot.SearchSongFragment.OnListFragmentInteractionListener;
import com.nicewuerfel.musicbot.api.Song;
import com.nostra13.universalimageloader.core.ImageLoader;

import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;

/**
 * {@link RecyclerView.Adapter} that can display a {@link Song} and makes a call to the
 * specified {@link OnListFragmentInteractionListener}.
 */
public class MySearchSongRecyclerViewAdapter extends RecyclerView.Adapter<MySearchSongRecyclerViewAdapter.ViewHolder> {

  private final List<Song> mValues;
  private final OnListFragmentInteractionListener mListener;
  private final Map<View, Song> viewSongs;

  public MySearchSongRecyclerViewAdapter(List<Song> items, OnListFragmentInteractionListener listener) {
    mValues = items;
    mListener = listener;
    viewSongs = new WeakHashMap<>(128);
  }

  @Override
  public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
    View view = LayoutInflater.from(parent.getContext())
        .inflate(R.layout.fragment_search_song, parent, false);
    return new ViewHolder(view);
  }

  @Override
  public void onBindViewHolder(final ViewHolder holder, int position) {
    Song song = mValues.get(position);

    if (song.equals(holder.song)) {
      return;
    }

    holder.song = song;
    holder.mTitleView.setText(song.getTitle());
    holder.mTitleView.setSelected(true);
    holder.mDescriptionView.setText(song.getDescription());
    holder.mDescriptionView.setSelected(true);
    holder.mDurationView.setText(song.getDuration());
    holder.mAlbumView.setImageResource(android.R.drawable.ic_menu_close_clear_cancel);
    String albumArtUrl = song.getAlbumArtUrl();
    if (albumArtUrl != null) {
      ImageLoader.getInstance().displayImage(albumArtUrl, holder.mAlbumView);
    }

    holder.mView.setOnClickListener(new View.OnClickListener() {
      @Override
      public void onClick(View v) {
        if (null != mListener) {
          // Notify the active callbacks interface (the activity, if the
          // fragment is attached to one) that an item has been selected.
          mListener.onSearchResultClick(holder.song);
        }
      }
    });
  }

  @Override
  public int getItemCount() {
    return mValues.size();
  }

  public class ViewHolder extends RecyclerView.ViewHolder {
    public final View mView;
    public final TextView mTitleView;
    public final TextView mDescriptionView;
    public final TextView mDurationView;
    public final ImageView mAlbumView;
    public Song song;

    public ViewHolder(View view) {
      super(view);
      mView = view;
      mTitleView = (TextView) view.findViewById(R.id.song_title);
      mDescriptionView = (TextView) view.findViewById(R.id.song_description);
      mDurationView = (TextView) view.findViewById(R.id.song_duration);
      mAlbumView = (ImageView) view.findViewById(R.id.album_art);
    }

    @Override
    public String toString() {
      return super.toString() + " '" + mTitleView.getText() + "'";
    }
  }
}
