package com.nicewuerfel.musicbot;

import android.annotation.SuppressLint;
import android.content.Context;
import android.os.Bundle;
import android.support.annotation.Nullable;
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

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.WeakHashMap;

/**
 * A fragment representing a list of Items.
 * <p>
 * Activities containing this fragment MUST implement the {@link OnListFragmentInteractionListener}
 * interface.
 */
public class SongFragment extends Fragment {

  private static final String ARG_SONG_LIST = "songs";

  private ArrayList<Song> songs;
  private ArrayAdapter<Song> songAdapter;
  private Map<View, Song> viewSongs;
  private Set<View> views;
  private boolean movable = true;
  private boolean removable = true;

  private OnListFragmentInteractionListener mListener;

  /**
   * Mandatory empty constructor for the fragment manager to instantiate the
   * fragment (e.g. upon screen orientation changes).
   */
  public SongFragment() {
  }

  public static SongFragment newInstance() {
    return newInstance(new ArrayList<Song>(64));
  }

  public static SongFragment newInstance(ArrayList<Song> songs) {
    SongFragment fragment = new SongFragment();
    Bundle args = new Bundle();
    args.putParcelableArrayList(ARG_SONG_LIST, songs);
    fragment.setArguments(args);
    return fragment;
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

  }

  @Override
  public void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);

    viewSongs = new WeakHashMap<>(128);
    views = Collections.newSetFromMap(new WeakHashMap<View, Boolean>(128));
    if (savedInstanceState != null) {
      songs = savedInstanceState.getParcelableArrayList(ARG_SONG_LIST);
    } else if (getArguments() != null) {
      songs = getArguments().getParcelableArrayList(ARG_SONG_LIST);
    } else {
      songs = new ArrayList<>(64);
    }
  }

  @Override
  public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
    View view = inflater.inflate(R.layout.fragment_song_list, container, false);

    // Set the adapter
    if (view instanceof DragSortListView) {
      DragSortListView listView = (DragSortListView) view;
      songAdapter = new QueueSongArrayAdapter(songs);
      songAdapter.setNotifyOnChange(false);
      listView.setAdapter(songAdapter);
      listView.setDropListener(new QueueSongDropListener());
    }
    return view;
  }

  private int findFirstQueued(List<Song> songs) {
    if (songs.isEmpty()) {
      return -1;
    }
    int halfSize = songs.size() / 2;
    if (songs.get(halfSize).isLastPlayed()) {
      if (halfSize == songs.size() - 1) {
        // There are no songs in the queue
        return halfSize;
      }
      return halfSize + findFirstQueued(songs.subList(halfSize + 1, songs.size())) + 1;
    } else if (halfSize == 0 || songs.get(halfSize - 1).isLastPlayed()) {
      return halfSize;
    } else {
      List<Song> subList = songs.subList(0, halfSize);
      return halfSize - (subList.size() - findFirstQueued(subList));
    }
  }

  private void selectFirstQueued() {
    if (songs.isEmpty()) {
      return;
    }
    int index = findFirstQueued(songs);
    DragSortListView listView = (DragSortListView) getView();
    listView.setSelection(index);
  }

  @Override
  public void onResume() {
    super.onResume();
    selectFirstQueued();
  }

  @Override
  public void onSaveInstanceState(Bundle outState) {
    outState.putParcelableArrayList(ARG_SONG_LIST, songs);
    super.onSaveInstanceState(outState);
  }

  @Override
  public void onDestroy() {
    songAdapter = null;
    songs = null;
    viewSongs = null;
    views = null;
    super.onDestroy();
  }

  @Override
  public void onDetach() {
    mListener = null;
    super.onDetach();
  }

  public void updateQueue(List<Song> songs) {
    if (songs != null && this.songs != null && songAdapter != null) {
      this.songs.clear();
      this.songs.addAll(songs);
      songAdapter.notifyDataSetChanged();
      songAdapter.setNotifyOnChange(false);
    }
    selectFirstQueued();
  }

  private void updateViews() {
    for (View view : views) {
      View moveView = view.findViewById(R.id.drag_handle);
      if (movable) {
        moveView.setVisibility(View.VISIBLE);
      } else {
        moveView.setVisibility(View.GONE);
      }
      View removeView = view.findViewById(R.id.remove_button);
      if (removable) {
        removeView.setVisibility(View.VISIBLE);
      } else {
        removeView.setVisibility(View.GONE);
      }
    }
  }

  public void setMovable(boolean movable) {
    if (movable != this.movable) {
      this.movable = movable;
      updateViews();
    }
  }

  public void setRemovable(boolean removable) {
    if (removable != this.removable) {
      this.removable = removable;
      updateViews();
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
    void onSongClick(Song song);

    void onSongRemoveClick(Song song);
  }

  private class QueueSongArrayAdapter extends ArrayAdapter<Song> {
    public QueueSongArrayAdapter(List<Song> songArrayList) {
      super(SongFragment.this.getContext(), R.layout.fragment_song, songArrayList);
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
          views.add(view);
        }
      } else {
        view = LayoutInflater.from(getContext()).inflate(R.layout.fragment_song, null);
        viewSongs.put(view, song);
        views.add(view);
      }

      boolean isLastPlayed = song.isLastPlayed();
      if (isLastPlayed) {
        view.setAlpha(0.5f);
      }

      View contentView = view.findViewById(R.id.non_drag_handle_content);
      contentView.setOnClickListener(new View.OnClickListener() {
        @Override
        public void onClick(View v) {
          if (null != mListener) {
            mListener.onSongClick(song);
          }
        }
      });

      final ImageView albumView = (ImageView) view.findViewById(R.id.album_art);
      albumView.setImageResource(android.R.drawable.ic_menu_close_clear_cancel);
      ApiConnector.displayAlbumArt(song, albumView, false);

      TextView titleText = (TextView) view.findViewById(R.id.song_title);
      titleText.setText(song.getTitle());
      titleText.setSelected(true);
      TextView descriptionText = (TextView) view.findViewById(R.id.song_description);
      descriptionText.setText(song.getDescription());
      descriptionText.setSelected(true);

      TextView durationText = (TextView) view.findViewById(R.id.song_duration);
      durationText.setVisibility(song.getDuration() == null ? View.GONE : View.VISIBLE);
      durationText.setText(song.getDuration());

      if (song.getUsername() != null) {
        ((TextView) view.findViewById(R.id.song_user)).setText(getString(R.string.queued_by, song.getUsername()));
      }

      View removeView = view.findViewById(R.id.remove_button);
      if (removable && !isLastPlayed) {
        removeView.setVisibility(View.VISIBLE);
      } else {
        removeView.setVisibility(View.GONE);
      }
      removeView.setOnClickListener(new View.OnClickListener() {
        @Override
        public void onClick(View v) {
          if (null != mListener) {
            // Notify the active callbacks interface (the activity, if the
            // fragment is attached to one) that an item has been selected.
            mListener.onSongRemoveClick(song);
          }
        }
      });

      View moveView = view.findViewById(R.id.drag_handle);
      if (movable && !isLastPlayed) {
        moveView.setVisibility(View.VISIBLE);
      } else {
        moveView.setVisibility(View.GONE);
      }

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
