package com.nicewuerfel.musicbot.ui.fragments;

import android.content.res.Resources;
import android.support.v7.app.AlertDialog;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.TextView;

import com.nicewuerfel.musicbot.R;
import com.nicewuerfel.musicbot.api.ApiUser;

import java.util.List;

/**
 * {@link RecyclerView.Adapter} that can display a {@link ApiUser} and makes a call to the
 * specified {@link UserFragment.OnListFragmentInteractionListener}.
 */
class UserRecyclerViewAdapter extends RecyclerView.Adapter<UserRecyclerViewAdapter.ViewHolder> {

  private final List<ApiUser> mValues;
  private final UserFragment.OnListFragmentInteractionListener mListener;

  public UserRecyclerViewAdapter(List<ApiUser> items, UserFragment.OnListFragmentInteractionListener listener) {
    mValues = items;
    mListener = listener;
  }

  @Override
  public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
    View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.fragment_user, parent, false);
    return new ViewHolder(view);
  }

  @Override
  public void onBindViewHolder(final ViewHolder holder, int position) {
    final ApiUser user = holder.user = mValues.get(position);
    holder.mNameView.setText(user.getUsername());
    final Resources resources = holder.mView.getContext().getResources();
    holder.mPermissionsButtonView.setText(resources.getString(R.string.show_permissions, user.getPermissions().size()));
    holder.mPermissionsButtonView.setOnClickListener(new View.OnClickListener() {
      @Override
      public void onClick(View v) {
        new AlertDialog.Builder(holder.mView.getContext())
            .setCancelable(true)
            .setPositiveButton(android.R.string.ok, null)
            .setTitle(resources.getString(R.string.user_permissions, user.getUsername()))
            .setAdapter(new ArrayAdapter<>(holder.mPermissionsButtonView.getContext(), R.layout.permission_text, user.getPermissions()), null)
            .show();
      }
    });
    holder.mEditButton.setOnClickListener(new View.OnClickListener() {
      @Override
      public void onClick(View v) {
        if (null != mListener) {
          mListener.onEditClick(user);
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
    public final TextView mNameView;
    public final Button mPermissionsButtonView;
    public final Button mEditButton;
    public ApiUser user;

    public ViewHolder(View view) {
      super(view);
      mView = view;
      mNameView = (TextView) view.findViewById(R.id.username);
      mPermissionsButtonView = (Button) view.findViewById(R.id.permission_button);
      mEditButton = (Button) view.findViewById(R.id.edit_button);
    }

    @Override
    public String toString() {
      return super.toString() + " '" + mNameView.getText() + "'";
    }
  }
}
