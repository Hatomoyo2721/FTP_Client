package com.example.ftp_client.ui.connection;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.viewpager2.widget.ViewPager2;

import com.example.ftp_client.R;
import com.google.android.material.tabs.TabLayout;
import com.google.android.material.tabs.TabLayoutMediator;

public class ConnectionMainLayout extends Fragment {

    private ViewPager2 viewPager;
    private TabLayout tabLayout;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_connection_main, container, false);

        viewPager = view.findViewById(R.id.viewPager);
        tabLayout = view.findViewById(R.id.tabLayout);

        // Setup ViewPager adapter
        ViewPagerFragmentAdapter adapter = new ViewPagerFragmentAdapter(this);
        adapter.addFragment(new AddConnectionFragment());
        adapter.addFragment(new AddExistingConnectionFragment());
        viewPager.setAdapter(adapter);

        // Connect TabLayout with ViewPager
        new TabLayoutMediator(tabLayout, viewPager,
                (tab, position) -> {
                    switch (position) {
                        case 0:
                            tab.setText("Add Connection");
                            break;
                        case 1:
                            tab.setText("Add Existing");
                            break;
                    }
                }).attach();

        tabLayout.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                switch (tab.getPosition()) {
                    case 0:
                        view.findViewById(R.id.containerAddConnection).setVisibility(View.VISIBLE);
                        view.findViewById(R.id.containerAddExisting).setVisibility(View.GONE);
                        break;
                    case 1:
                        view.findViewById(R.id.containerAddConnection).setVisibility(View.GONE);
                        view.findViewById(R.id.containerAddExisting).setVisibility(View.VISIBLE);
                        break;
                }
            }

            @Override
            public void onTabUnselected(TabLayout.Tab tab) {
            }

            @Override
            public void onTabReselected(TabLayout.Tab tab) {
            }
        });

        return view;
    }
}
