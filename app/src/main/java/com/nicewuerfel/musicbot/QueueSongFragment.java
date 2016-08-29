package com.nicewuerfel.musicbot;

import android.content.Context;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;

import com.mobeta.android.dslv.DragSortListView;
import com.nicewuerfel.musicbot.api.ApiConnector;
import com.nicewuerfel.musicbot.api.DummyCallback;
import com.nicewuerfel.musicbot.api.MoveRequestBody;
import com.nicewuerfel.musicbot.api.PlayerState;
import com.nicewuerfel.musicbot.api.Song;
import com.nostra13.universalimageloader.core.ImageLoader;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;

/**
 * A fragment representing a list of Items.
 * <p>
 * Activities containing this fragment MUST implement the {@link OnListFragmentInteractionListener}
 * interface.
 */
public class QueueSongFragment extends Fragment {

  private static final String ARG_SONG_LIST = "songs";
  private List<Song> songs;
  private ArrayAdapter<Song> songAdapter;
  private Map<View, Song> viewSongs;

  private OnListFragmentInteractionListener mListener;

  /**
   * Mandatory empty constructor for the fragment manager to instantiate the
   * fragment (e.g. upon screen orientation changes).
   */
  public QueueSongFragment() {
  }

  @SuppressWarnings("unused")
  public static QueueSongFragment newInstance(ArrayList<Song> songs) {
    QueueSongFragment fragment = new QueueSongFragment();
    Bundle args = new Bundle();
    args.putParcelableArrayList(ARG_SONG_LIST, songs);
    fragment.setArguments(args);
    return fragment;
  }

  @Override
  public void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);

    if (getArguments() != null) {
      songs = getArguments().getParcelableArrayList(ARG_SONG_LIST);
    }

    viewSongs = new WeakHashMap<>(128);
  }

  @Override
  public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
    View view = inflater.inflate(R.layout.fragment_queue_song_list, container, false);


    // Set the adapter
    if (view instanceof DragSortListView) {
      DragSortListView listView = (DragSortListView) view;
      songAdapter = new QueueSongArrayAdapter(songs);
      listView.setAdapter(songAdapter);
      listView.setDropListener(new QueueSongDropListener());
    }
    return view;
  }

  @Override
  public void onDestroyView() {
    super.onDestroyView();
  }

  @Override
  public void onAttach(Context context) {
    super.onAttach(context);
    if (context instanceof OnListFragmentInteractionListener) {
      mListener = (OnListFragmentInteractionListener) context;
    } else {
      throw new RuntimeException(context.toString()
          + " must implement OnListFragmentInteractionListener");
    }
    viewSongs = new HashMap<>(128);
  }

  @Override
  public void onDetach() {
    super.onDetach();
    mListener = null;
    viewSongs = null;
    songs = null;
    songAdapter = null;
  }

  public void updateQueue(List<Song> songs) {
    if (this.songs != null && songAdapter != null) {
      this.songs.clear();
      this.songs.addAll(songs);
      songAdapter.notifyDataSetChanged();
    }
  }

  /**
   * This interface must be implemented by activities that contain this
   * fragment to allow an interaction in this fragment to be communicated
   * to the activity and potentially other fragments contained in that
   * activity.
   * <p>
   * See the Android Training lesson <a href=
   * "http://developer.android.com/training/basics/fragments/communicating.html"
   * >Communicating with Other Fragments</a> for more information.
   */
  public interface OnListFragmentInteractionListener {
    void onRemoveSongClick(Song song);
  }

  private class QueueSongArrayAdapter extends ArrayAdapter<Song> {
    public QueueSongArrayAdapter(List<Song> songArrayList) {
      super(QueueSongFragment.this.getContext(), R.layout.fragment_queue_song, songArrayList);
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
      View view;
      final Song song = getItem(position);
      if (convertView instanceof FrameLayout) {
        view = convertView;
        if (song.equals(viewSongs.get(view))) {
          return view;
        } else {
          viewSongs.put(view, song);
        }
      } else {
        view = LayoutInflater.from(getContext()).inflate(R.layout.fragment_queue_song, null);
        viewSongs.put(view, song);
      }

      ImageView albumView = (ImageView) view.findViewById(R.id.album_art);
      albumView.setImageResource(android.R.drawable.ic_menu_close_clear_cancel);
      String albumArtUrl = song.getAlbumArtUrl();
      if (albumArtUrl != null) {
        ImageLoader.getInstance().displayImage(albumArtUrl, albumView);
      }

      TextView titleText = (TextView) view.findViewById(R.id.song_title);
      titleText.setText(song.getTitle());
      titleText.setSelected(true);
      TextView descriptionText = (TextView) view.findViewById(R.id.song_description);
      descriptionText.setText(song.getDescription());
      descriptionText.setSelected(true);
      ((TextView) view.findViewById(R.id.song_duration)).setText(song.getDuration());

      view.findViewById(R.id.remove_button).setOnClickListener(new View.OnClickListener() {
        @Override
        public void onClick(View v) {
          if (null != mListener) {
            // Notify the active callbacks interface (the activity, if the
            // fragment is attached to one) that an item has been selected.
            mListener.onRemoveSongClick(song);
          }
        }
      });
      return view;
    }
  }

  private class QueueSongDropListener implements DragSortListView.DropListener {
    @Override
    public void drop(int from, int to) {
      if (from == to) {
        return;
      }
      Song movedSong = songs.get(from);
      Song otherSong = songs.get(to);
      songs.remove(from);
      songs.add(to, movedSong);
      songAdapter.notifyDataSetChanged();
      Object after = from < to ? "true" : null;
      ApiConnector.getService().moveSong(new MoveRequestBody(movedSong, otherSong), after).enqueue(new DummyCallback<PlayerState>());
    }
  }
}
