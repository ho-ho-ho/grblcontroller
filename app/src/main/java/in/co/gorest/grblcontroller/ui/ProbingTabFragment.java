/*
 *  /**
 *  * Copyright (C) 2017  Grbl Controller Contributors
 *  *
 *  * This program is free software; you can redistribute it and/or modify
 *  * it under the terms of the GNU General Public License as published by
 *  * the Free Software Foundation; either version 2 of the License, or
 *  * (at your option) any later version.
 *  *
 *  * This program is distributed in the hope that it will be useful,
 *  * but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  * GNU General Public License for more details.
 *  *
 *  * You should have received a copy of the GNU General Public License along
 *  * with this program; if not, write to the Free Software Foundation, Inc.,
 *  * 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301 USA.
 *  * <http://www.gnu.org/licenses/>
 *
 */

package in.co.gorest.grblcontroller.ui;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.databinding.DataBindingUtil;
import androidx.viewpager2.widget.ViewPager2;

import com.google.android.material.tabs.TabLayout;

import in.co.gorest.grblcontroller.R;
import in.co.gorest.grblcontroller.databinding.FragmentProbingTabBinding;
import in.co.gorest.grblcontroller.listeners.MachineStatusListener;

public class ProbingTabFragment extends BaseFragment {

    private MachineStatusListener machineStatus;

    public ProbingTabFragment() {
    }

    public static ProbingTabFragment newInstance() {
        return new ProbingTabFragment();
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        machineStatus = MachineStatusListener.getInstance();
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        FragmentProbingTabBinding binding = DataBindingUtil.inflate(inflater, R.layout.fragment_probing_tab, container, false);
        binding.setMachineStatus(machineStatus);
        View view = binding.getRoot();

        TabLayout probingTabLayout = view.findViewById(R.id.probing_tab_layout);

        probingTabLayout.addTab(probingTabLayout.newTab().setText(getString(R.string.text_probing_type_straight)));
        probingTabLayout.addTab(probingTabLayout.newTab().setText(getString(R.string.text_probing_type_tlo)));
        probingTabLayout.addTab(probingTabLayout.newTab().setText(getString(R.string.text_probing_type_center)));

        probingTabLayout.setTabGravity(TabLayout.GRAVITY_FILL);

        final ViewPager2 probingViewPager = view.findViewById(R.id.probing_pager);
        final ProbingAdapter probingAdapter = new ProbingAdapter(getActivity());
        probingViewPager.setAdapter(probingAdapter);
        probingViewPager.registerOnPageChangeCallback(new ViewPager2.OnPageChangeCallback() {
            @Override
            public void onPageSelected(int position) {
                probingTabLayout.selectTab(probingTabLayout.getTabAt(position));
            }
        });

        probingTabLayout.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                probingViewPager.setCurrentItem(tab.getPosition());
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
