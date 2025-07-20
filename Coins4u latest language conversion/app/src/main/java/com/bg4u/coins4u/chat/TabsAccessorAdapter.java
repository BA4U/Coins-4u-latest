package com.bg4u.coins4u.chat;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.viewpager2.adapter.FragmentStateAdapter;

public class TabsAccessorAdapter extends FragmentStateAdapter {
    
    public TabsAccessorAdapter(@NonNull FragmentActivity fragmentActivity) {
        super(fragmentActivity);
    }
    
    @NonNull
    @Override
    public Fragment createFragment(int position) {
        switch (position) {
            //case 1:
            //    return new GroupsFragment();
            case 0:
                return new FindFriendsFragment();
            case 1:
                return new RequestsFragment();
            case 2:
                return new ChatsFragment();
            default:
                return null;
        }
    }
    
    @Override
    public int getItemCount() {
        return 3;
    }
}
