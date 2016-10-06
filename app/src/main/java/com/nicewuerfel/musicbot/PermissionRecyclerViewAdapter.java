package com.nicewuerfel.musicbot;

import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CompoundButton;
import android.widget.TextView;
import android.widget.ToggleButton;

import com.nicewuerfel.musicbot.PermissionFragment.OnListFragmentInteractionListener;
import com.nicewuerfel.musicbot.api.ApiConnector;
import com.nicewuerfel.musicbot.api.ApiUser;
import com.nicewuerfel.musicbot.api.Permission;

import java.util.List;

/**
 * {@link RecyclerView.Adapter} that can display a {@link Permission} and makes a call to the
 * specified {@link OnListFragmentInteractionListener}.
 */
public class PermissionRecyclerViewAdapter extends RecyclerView.Adapter<PermissionRecyclerViewAdapter.ViewHolder> {

  private final ApiUser mUser;
  private final List<Permission> mValues;
  private final OnListFragmentInteractionListener mListener;

  public PermissionRecyclerViewAdapter(ApiUser user, List<Permission> items, OnListFragmentInteractionListener listener) {
    mUser = user;
    mValues = items;
    mListener = listener;
  }

  @Override
  public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
    View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.fragment_permission, parent, false);
    return new ViewHolder(view);
  }

  @Override
  public void onBindViewHolder(final ViewHolder holder, int position) {
    final Permission permission = holder.permission = mValues.get(position);
    holder.mNameView.setText(permission.getName());
    holder.mDescriptionView.setText(permission.getDescription());

    holder.mToggleView.setChecked(mUser.getPermissions().contains(permission.getName()));
    holder.mToggleView.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
      @Override
      public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
        if (mListener != null) {
          mListener.onPermissionToggle(mUser, permission, isChecked);
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
    public final TextView mDescriptionView;
    public final ToggleButton mToggleView;
    public Permission permission;

    public ViewHolder(View view) {
      super(view);
      mView = view;
      mNameView = (TextView) view.findViewById(R.id.permission_name);
      mDescriptionView = (TextView) view.findViewById(R.id.permission_description);
      mToggleView = (ToggleButton) view.findViewById(R.id.permission_toggle);
    }

    @Override
    public String toString() {
      return super.toString() + " '" + mNameView.getText() + "'";
    }
  }
}
