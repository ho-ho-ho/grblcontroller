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

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.RelativeLayout;
import android.widget.ToggleButton;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.SwitchCompat;
import androidx.databinding.DataBindingUtil;
import androidx.viewpager2.widget.ViewPager2;

import com.google.android.material.button.MaterialButtonToggleGroup;
import com.google.android.material.tabs.TabLayout;
import com.warkiz.widget.IndicatorSeekBar;
import com.warkiz.widget.OnSeekChangeListener;
import com.warkiz.widget.SeekParams;

import org.greenrobot.eventbus.EventBus;

import in.co.gorest.grblcontroller.R;
import in.co.gorest.grblcontroller.databinding.FragmentJoggingTabBinding;
import in.co.gorest.grblcontroller.events.JogCommandEvent;
import in.co.gorest.grblcontroller.events.UiToastEvent;
import in.co.gorest.grblcontroller.helpers.EnhancedSharedPreferences;
import in.co.gorest.grblcontroller.listeners.MachineStatusListener;
import in.co.gorest.grblcontroller.model.Constants;
import in.co.gorest.grblcontroller.util.GrblUtils;

public class JoggingTabFragment extends BaseFragment implements View.OnClickListener,
        View.OnLongClickListener, SharedPreferences.OnSharedPreferenceChangeListener {

    private MachineStatusListener machineStatus;
    private EnhancedSharedPreferences sharedPref;
    MaterialButtonToggleGroup stepSelector;
    ToggleButton continuousButton;
    Handler continuousJogHandler;
    String continuousTag;
    Runnable continuousJogging = new Runnable() {
        @Override
        public void run() {
            sendJogCommand(continuousTag);
            continuousJogHandler.postDelayed(this, Constants.CONTINUOUS_DELAY);
        }
    };

    public JoggingTabFragment() {
    }

    public static JoggingTabFragment newInstance() {
        return new JoggingTabFragment();
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        machineStatus = MachineStatusListener.getInstance();
        sharedPref = EnhancedSharedPreferences.getInstance(
                requireActivity().getApplicationContext(),
                getString(R.string.shared_preference_key));
        sharedPref.registerOnSharedPreferenceChangeListener(this);
        continuousJogHandler = new Handler();
    }

    @Override
    public void onResume() {
        super.onResume();

        String joggingPadRotateAngle = sharedPref.getString(
                getString(R.string.preference_xy_jog_pad_rotation), "0");
        String[] joggingPadTags = rotateJogPad(Integer.parseInt(joggingPadRotateAngle));
        int jogPadIndex = 0;
        for (int resourceId : new Integer[]{R.id.jog_xy_top_left, R.id.jog_y_positive,
                R.id.jog_xy_top_right, R.id.jog_x_negative, R.id.jog_x_positive,
                R.id.jog_xy_bottom_left,
                R.id.jog_y_negative, R.id.jog_xy_bottom_right}) {
            final Button jogButton = requireView().findViewById(resourceId);
            jogButton.setTag(joggingPadTags[jogPadIndex]);
            jogPadIndex++;
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        sharedPref.unregisterOnSharedPreferenceChangeListener(this);
    }

    private void updateSteps() {
        double stepXY = machineStatus.getJogging().stepXY;
        double stepZ = machineStatus.getJogging().stepZ;

        if (stepXY == Double.parseDouble(
                sharedPref.getString(getString(R.string.preference_jog_xy_step_small), "0")) &&
                stepZ == Double.parseDouble(
                        sharedPref.getString(getString(R.string.preference_jog_z_step_small),
                                "0"))) {
            stepSelector.check(R.id.jog_step_small_button);
        } else if (stepXY == Double.parseDouble(
                sharedPref.getString(getString(R.string.preference_jog_xy_step_medium), "0")) &&
                stepZ == Double.parseDouble(
                        sharedPref.getString(getString(R.string.preference_jog_z_step_medium),
                                "0"))) {
            stepSelector.check(R.id.jog_step_medium_button);
        } else if (stepXY == Double.parseDouble(
                sharedPref.getString(getString(R.string.preference_jog_xy_step_high), "0")) &&
                stepZ == Double.parseDouble(
                        sharedPref.getString(getString(R.string.preference_jog_z_step_high),
                                "0"))) {
            stepSelector.check(R.id.jog_step_high_button);
        } else {
            stepSelector.clearChecked();
        }
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences,
            @Nullable String key) {
        if (key.equals(getString(R.string.preference_jog_xy_step_small)) || key.equals(
                getString(R.string.preference_jog_z_step_small)) ||
                key.equals(getString(R.string.preference_jog_xy_step_medium)) || key.equals(
                getString(R.string.preference_jog_z_step_medium)) ||
                key.equals(getString(R.string.preference_jog_xy_step_high)) || key.equals(
                getString(R.string.preference_jog_z_step_high))) {
            updateSteps();
        }
    }

    @SuppressLint("ClickableViewAccessibility")
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {
        FragmentJoggingTabBinding binding = DataBindingUtil.inflate(inflater,
                R.layout.fragment_jogging_tab, container, false);
        binding.setMachineStatus(machineStatus);
        View view = binding.getRoot();

        continuousButton = view.findViewById(R.id.continuous_button);

        TabLayout probingTabLayout = view.findViewById(R.id.probing_tab_layout);

        probingTabLayout.addTab(probingTabLayout.newTab().setText("Macros"));
        probingTabLayout.addTab(
                probingTabLayout.newTab().setText(getString(R.string.text_probing_type_straight)));
        probingTabLayout.addTab(
                probingTabLayout.newTab().setText(getString(R.string.text_probing_type_tlo)));
        probingTabLayout.addTab(
                probingTabLayout.newTab().setText(getString(R.string.text_probing_type_center)));

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

        stepSelector = view.findViewById(R.id.jog_step_selector);
        updateSteps();
        stepSelector.addOnButtonCheckedListener((group, checkedId, checked) -> {
            if (!checked) {
                return;
            }

            if (checkedId == R.id.jog_step_small_button) {
                machineStatus.setJogging(
                        Double.parseDouble(
                                sharedPref.getString(
                                        getString(R.string.preference_jog_xy_step_small), "0")),
                        Double.parseDouble(
                                sharedPref.getString(
                                        getString(R.string.preference_jog_z_step_small), "0"))
                );
            } else if (checkedId == R.id.jog_step_medium_button) {
                machineStatus.setJogging(
                        Double.parseDouble(
                                sharedPref.getString(
                                        getString(R.string.preference_jog_xy_step_medium), "0")),
                        Double.parseDouble(
                                sharedPref.getString(
                                        getString(R.string.preference_jog_z_step_medium), "0"))
                );
            } else if (checkedId == R.id.jog_step_high_button) {
                machineStatus.setJogging(
                        Double.parseDouble(
                                sharedPref.getString(
                                        getString(R.string.preference_jog_xy_step_high), "0")),
                        Double.parseDouble(
                                sharedPref.getString(getString(R.string.preference_jog_z_step_high),
                                        "0"))
                );
            }
        });

        RelativeLayout joggingStepFeedView = view.findViewById(R.id.jogging_step_feed_view);
        joggingStepFeedView.setOnClickListener(this);

        for (int resourceId : new Integer[]{R.id.jog_y_positive, R.id.jog_x_positive,
                R.id.jog_z_positive,
                R.id.jog_xy_top_left, R.id.jog_xy_top_right, R.id.jog_xy_bottom_left,
                R.id.jog_xy_bottom_right,
                R.id.jog_y_negative, R.id.jog_x_negative, R.id.jog_z_negative}) {

            final Button iconButton = view.findViewById(resourceId);
            iconButton.setOnTouchListener((view1, event) -> {
                if (!continuousButton.isChecked()) {
                    return false;
                }

                switch (event.getAction()) {
                    case MotionEvent.ACTION_DOWN:
                        continuousTag = iconButton.getTag().toString();
                        continuousJogHandler.post(continuousJogging);
                        return true;
                    case MotionEvent.ACTION_UP:
                    case MotionEvent.ACTION_CANCEL:
                        continuousJogHandler.removeCallbacks(continuousJogging);
                        fragmentInteractionListener.onGrblRealTimeCommandReceived(
                                GrblUtils.GRBL_JOG_CANCEL_COMMAND);
                        return true;
                }
                return false;
            });

            iconButton.setOnClickListener(view1 -> {
                if (isAdded() && !continuousButton.isChecked()) {
                    sendJogCommand(iconButton.getTag().toString());
                }
            });

        }

        for (int resourceId : new Integer[]{R.id.jog_cancel, R.id.run_homing_cycle}) {
            Button iconButton = view.findViewById(resourceId);
            iconButton.setOnLongClickListener(this);
        }

        for (int resourceId : new Integer[]{R.id.run_homing_cycle, R.id.jog_cancel}) {
            Button iconButton = view.findViewById(resourceId);
            iconButton.setOnClickListener(this);
        }

        return view;
    }

    @SuppressLint("NonConstantResourceId")
    @Override
    public void onClick(View view) {
        int id = view.getId();

        if (id == R.id.jogging_step_feed_view) {
            this.setJoggingStepAndFeed();
        } else if (id == R.id.run_homing_cycle) {
            if (machineStatus.getState().equals(Constants.MACHINE_STATUS_IDLE)
                    || machineStatus.getState()
                    .equals(Constants.MACHINE_STATUS_ALARM)) {
                new AlertDialog.Builder(getActivity())
                        .setTitle(getString(R.string.text_homing_cycle))
                        .setMessage(getString(R.string.text_do_homing_cycle))
                        .setPositiveButton(getString(R.string.text_yes_confirm),
                                (dialog, which) -> fragmentInteractionListener.onGcodeCommandReceived(
                                        GrblUtils.GRBL_RUN_HOMING_CYCLE))
                        .setNegativeButton(getString(R.string.text_no_confirm), null)
                        .show();
            } else {
                EventBus.getDefault()
                        .post(new UiToastEvent(getString(R.string.text_machine_not_idle), true,
                                true));
            }
        } else if (id == R.id.jog_cancel) {
            if (machineStatus.getState().equals(Constants.MACHINE_STATUS_JOG)) {
                fragmentInteractionListener.onGrblRealTimeCommandReceived(
                        GrblUtils.GRBL_JOG_CANCEL_COMMAND);
            }

            /*TODO: implement a way to stop custom command
            if (customCommandsAsyncTask != null && customCommandsAsyncTask.getStatus() ==
            AsyncTask.Status.RUNNING) {
                customCommandsAsyncTask.cancel(true);
                fragmentInteractionListener.onGrblRealTimeCommandReceived(GrblUtils
                .GRBL_RESET_COMMAND);
            }*/
        }
    }


    @SuppressLint("NonConstantResourceId")
    @Override
    public boolean onLongClick(View view) {
        int id = view.getId();

        if (id == R.id.run_homing_cycle) {
            new AlertDialog.Builder(getActivity())
                    .setTitle(getString(R.string.text_set_coordinate_system))
                    .setMessage(getString(R.string.text_set_all_axes_zero))
                    .setPositiveButton(getString(R.string.text_yes_confirm),
                            (dialog, which) -> sendCommandIfIdle(
                                    GrblUtils.GCODE_RESET_COORDINATES_TO_ZERO))
                    .setNegativeButton(getString(R.string.text_no_confirm), null)
                    .show();
            return true;
        } else if (id == R.id.jog_cancel) {
            new AlertDialog.Builder(getActivity())
                    .setTitle(getString(R.string.text_return_to_zero_position))
                    .setMessage(getString(R.string.text_go_to_zero_position_in_current_wpos))
                    .setPositiveButton(getString(R.string.text_yes_confirm), (dialog, which) -> {
                        for (String gCommand : GrblUtils.getReturnToHomeCommands()) {
                            sendCommandIfIdle(gCommand);
                        }
                    })
                    .setNegativeButton(getString(R.string.text_no_confirm), null)
                    .show();
            return true;
        }

        return false;
    }

    private void sendJogCommand(String tag) {
        if (machineStatus.getState().equals(Constants.MACHINE_STATUS_IDLE)
                || machineStatus.getState()
                .equals(Constants.MACHINE_STATUS_JOG)) {

            String units = "G21";
            double jogFeed = machineStatus.getJogging().feed;

            if (machineStatus.getJogging().inches) {
                units = "G20";
                jogFeed = jogFeed / 25.4;
            }

            Double stepSize;
            if (continuousButton.isChecked()) {
                stepSize = Constants.CONTINUOUS_STEP_SIZE;
            } else if (tag.toUpperCase().contains("Z")) {
                stepSize = machineStatus.getJogging().stepZ;
            } else {
                stepSize = machineStatus.getJogging().stepXY;
            }

            String jog = String.format(tag, units, stepSize, jogFeed);
            EventBus.getDefault().post(new JogCommandEvent(jog));
        } else {
            EventBus.getDefault()
                    .post(new UiToastEvent(getString(R.string.text_machine_not_idle), true, true));
        }
    }

    @SuppressLint("NonConstantResourceId")
    private void setJoggingStepAndFeed() {
        LayoutInflater inflater = LayoutInflater.from(getActivity());
        View view = inflater.inflate(R.layout.dialog_step_and_feed, null, false);

        final IndicatorSeekBar jogStepSeekBarXY = view.findViewById(R.id.jog_xy_step_seek_bar);
        jogStepSeekBarXY.setProgress(machineStatus.getJogging().stepXY.floatValue());
        jogStepSeekBarXY.setMax(
                sharedPref.getInt(getString(R.string.preference_jogging_max_step_size), 10));
        jogStepSeekBarXY.setIndicatorTextFormat("XY: ${PROGRESS}");
        jogStepSeekBarXY.setDecimalScale(3);

        for (final int resourceId : new Integer[]{R.id.jog_xy_step_small, R.id.jog_xy_step_medium,
                R.id.jog_xy_step_high}) {
            final Button iconButton = view.findViewById(resourceId);

            iconButton.setOnLongClickListener(v -> {
                new AlertDialog.Builder(getActivity())
                        .setTitle("Save Quick Button Value")
                        .setMessage("do you want to save the quick button value as "
                                + jogStepSeekBarXY.getProgressFloat())
                        .setPositiveButton(getString(R.string.text_yes_confirm),
                                (dialog, which) -> {
                                    EnhancedSharedPreferences.Editor editor = sharedPref.edit();
                                    editor.putString(iconButton.getTag().toString(),
                                                    Float.toString(jogStepSeekBarXY.getProgressFloat()))
                                            .commit();
                                })
                        .setNegativeButton(getString(R.string.text_no_confirm), null)
                        .show();

                return true;
            });

            iconButton.setOnClickListener(v -> {
                if (isAdded()) {
                    String stepValue = sharedPref.getString(iconButton.getTag().toString(), "0");
                    if (stepValue.equals("0")) {
                        if (resourceId == R.id.jog_xy_step_small) {
                            stepValue = "0.1";
                        } else if (resourceId == R.id.jog_xy_step_medium) {
                            stepValue = "1";
                        } else if (resourceId == R.id.jog_xy_step_high) {
                            stepValue = "5";
                        }
                    }

                    if (stepValue.length() > 0) {
                        float step_value = Float.parseFloat(stepValue);
                        if (step_value > jogStepSeekBarXY.getMax()) {
                            EventBus.getDefault()
                                    .post(new UiToastEvent("Value is grater than the bar size",
                                            true, true));
                            return;
                        }

                        jogStepSeekBarXY.setProgress(step_value);
                        EventBus.getDefault()
                                .post(new UiToastEvent(
                                        "XY Axis step value is set to " + step_value));
                        EnhancedSharedPreferences.Editor editor = sharedPref.edit();
                        editor.putDouble(getString(R.string.preference_jogging_step_size),
                                Double.parseDouble(Float.toString(step_value))).commit();
                        updateSteps();
                    } else {
                        EventBus.getDefault().post(
                                new UiToastEvent("Invalid step size value, please check settings",
                                        true, true));
                    }
                }
            });
        }

        jogStepSeekBarXY.setOnSeekChangeListener(new OnSeekChangeListener() {
            @Override
            public void onSeeking(SeekParams seekParams) {
                machineStatus.setJogging(
                        Double.parseDouble(Float.toString(seekParams.progressFloat)),
                        machineStatus.getJogging().stepZ,
                        machineStatus.getJogging().feed,
                        sharedPref.getBoolean(getString(R.string.preference_jogging_in_inches),
                                false)
                );
            }

            @Override
            public void onStartTrackingTouch(IndicatorSeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(IndicatorSeekBar seekBar) {
                EnhancedSharedPreferences.Editor editor = sharedPref.edit();
                editor.putDouble(getString(R.string.preference_jogging_step_size),
                        Double.parseDouble(Float.toString(seekBar.getProgressFloat()))).commit();
            }
        });

        final IndicatorSeekBar jogStepSeekBarZ = view.findViewById(R.id.jog_z_step_seek_bar);
        jogStepSeekBarZ.setProgress(machineStatus.getJogging().stepZ.floatValue());
        jogStepSeekBarZ.setMax(
                sharedPref.getInt(getString(R.string.preference_jogging_max_step_size_z), 5));
        jogStepSeekBarZ.setIndicatorTextFormat("Z: ${PROGRESS}");
        jogStepSeekBarZ.setDecimalScale(3);

        for (final int resourceId : new Integer[]{R.id.jog_z_step_small, R.id.jog_z_step_medium,
                R.id.jog_z_step_high}) {
            final Button iconButton = view.findViewById(resourceId);

            iconButton.setOnLongClickListener(v -> {

                new AlertDialog.Builder(getActivity())
                        .setTitle("Save Quick Button Value")
                        .setMessage("do you want to save the quick button value as "
                                + jogStepSeekBarZ.getProgressFloat())
                        .setPositiveButton(getString(R.string.text_yes_confirm),
                                (dialog, which) -> {
                                    EnhancedSharedPreferences.Editor editor = sharedPref.edit();
                                    editor.putString(iconButton.getTag().toString(),
                                                    Float.toString(jogStepSeekBarZ.getProgressFloat()))
                                            .commit();
                                })
                        .setNegativeButton(getString(R.string.text_no_confirm), null)
                        .show();

                return true;
            });

            iconButton.setOnClickListener(v -> {
                if (isAdded()) {
                    String stepValue = sharedPref.getString(iconButton.getTag().toString(), "0");
                    if (stepValue.equals("0")) {
                        if (resourceId == R.id.jog_z_step_small) {
                            stepValue = "0.01";
                        } else if (resourceId == R.id.jog_z_step_medium) {
                            stepValue = "0.1";
                        } else if (resourceId == R.id.jog_z_step_high) {
                            stepValue = "1";
                        }
                    }

                    if (stepValue.length() > 0) {
                        float step_value = Float.parseFloat(stepValue);
                        if (step_value > jogStepSeekBarZ.getMax()) {
                            EventBus.getDefault()
                                    .post(new UiToastEvent("Value is grater than the bar size",
                                            true, true));
                            return;
                        }

                        jogStepSeekBarZ.setProgress(step_value);
                        EventBus.getDefault()
                                .post(new UiToastEvent(
                                        "Z Axis step value is set to " + step_value));
                        EnhancedSharedPreferences.Editor editor = sharedPref.edit();
                        editor.putDouble(getString(R.string.preference_jogging_step_size_z),
                                Double.parseDouble(Float.toString(step_value))).commit();
                    } else {
                        EventBus.getDefault().post(
                                new UiToastEvent("Invalid step size value, please check settings",
                                        true, true));
                    }
                }
            });
        }

        jogStepSeekBarZ.setOnSeekChangeListener(new OnSeekChangeListener() {
            @Override
            public void onSeeking(SeekParams seekParams) {
                machineStatus.setJogging(machineStatus.getJogging().stepXY,
                        Double.parseDouble(Float.toString(seekParams.progressFloat)),
                        machineStatus.getJogging().feed,
                        sharedPref.getBoolean(getString(R.string.preference_jogging_in_inches),
                                false)
                );
            }

            @Override
            public void onStartTrackingTouch(IndicatorSeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(IndicatorSeekBar seekBar) {
                EnhancedSharedPreferences.Editor editor = sharedPref.edit();
                editor.putDouble(getString(R.string.preference_jogging_step_size_z),
                        Double.parseDouble(Float.toString(seekBar.getProgressFloat()))).commit();
            }
        });

        IndicatorSeekBar jogFeedSeekBar = view.findViewById(R.id.jog_feed_seek_bar);
        jogFeedSeekBar.setProgress(machineStatus.getJogging().feed.floatValue());
        Double maxFeedRate = sharedPref.getDouble(
                getString(R.string.preference_jogging_max_feed_rate),
                2400.00);
        jogFeedSeekBar.setMax(Float.parseFloat(maxFeedRate.toString()));
        jogFeedSeekBar.setIndicatorTextFormat("Feed: ${PROGRESS}");

        jogFeedSeekBar.setOnSeekChangeListener(new OnSeekChangeListener() {
            @Override
            public void onSeeking(SeekParams seekParams) {
                machineStatus.setJogging(machineStatus.getJogging().stepXY,
                        machineStatus.getJogging().stepZ,
                        seekParams.progress,
                        sharedPref.getBoolean(getString(R.string.preference_jogging_in_inches),
                                false)
                );
            }

            @Override
            public void onStartTrackingTouch(IndicatorSeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(IndicatorSeekBar seekBar) {
                EnhancedSharedPreferences.Editor editor = sharedPref.edit();
                editor.putDouble(getString(R.string.preference_jogging_feed_rate),
                                seekBar.getProgress())
                        .commit();
            }
        });

        SwitchCompat jogInches = view.findViewById(R.id.jog_inches);
        jogInches.setChecked(
                sharedPref.getBoolean(getString(R.string.preference_jogging_in_inches), false));
        jogInches.setOnCheckedChangeListener((compoundButton, b) -> {
            machineStatus.setJogging(machineStatus.getJogging().stepXY,
                    machineStatus.getJogging().stepZ,
                    machineStatus.getJogging().feed, b);
            EnhancedSharedPreferences.Editor editor = sharedPref.edit();
            editor.putBoolean(getString(R.string.preference_jogging_in_inches), b).commit();
        });

        AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(getActivity());
        alertDialogBuilder.setView(view);
        //alertDialogBuilder.setTitle(getString(R.string.text_jogging_step_and_feed));
        alertDialogBuilder.setCancelable(true)
                .setPositiveButton(getString(R.string.text_ok), (dialog, id) -> {

                });

        AlertDialog dialog = alertDialogBuilder.create();
        dialog.setCancelable(false);
        dialog.show();
    }

    private String[] rotateJogPad(int angle) {

        switch (angle) {

            case 90:
                return new String[]{"$J=%1$sG91X-%2$sY-%2$sF%3$s", "$J=%sG91X-%sF%s",
                        "$J=%1$sG91X-%2$sY%2$sF%3$s", "$J=%sG91Y-%sF%s", "$J=%sG91Y%sF%s",
                        "$J=%1$sG91X%2$sY-%2$sF%3$s", "$J=%sG91X%sF%s",
                        "$J=%1$sG91X%2$sY%2$sF%3$s"};

            case 180:
                return new String[]{"$J=%1$sG91X%2$sY-%2$sF%3$s", "$J=%sG91Y-%sF%s",
                        "$J=%1$sG91X-%2$sY-%2$sF%3$s", "$J=%sG91X%sF%s", "$J=%sG91X-%sF%s",
                        "$J=%1$sG91X%2$sY%2$sF%3$s", "$J=%sG91Y%sF%s",
                        "$J=%1$sG91X-%2$sY%2$sF%3$s"};

            case 270:
                return new String[]{"$J=%1$sG91X%2$sY%2$sF%3$s", "$J=%sG91X%sF%s",
                        "$J=%1$sG91X%2$sY-%2$sF%3$s", "$J=%sG91Y%sF%s", "$J=%sG91Y-%sF%s",
                        "$J=%1$sG91X-%2$sY%2$sF%3$s", "$J=%sG91X-%sF%s",
                        "$J=%1$sG91X-%2$sY-%2$sF%3$s"};

            default:
                return new String[]{"$J=%1$sG91X-%2$sY%2$sF%3$s", "$J=%sG91Y%sF%s",
                        "$J=%1$sG91X%2$sY%2$sF%3$s", "$J=%sG91X-%sF%s", "$J=%sG91X%sF%s",
                        "$J=%1$sG91X-%2$sY-%2$sF%3$s", "$J=%sG91Y-%sF%s",
                        "$J=%1$sG91X%2$sY-%2$sF%3$s"};
        }
    }

    private void sendCommandIfIdle(String command) {
        if (machineStatus.getState().equals(Constants.MACHINE_STATUS_IDLE)) {
            fragmentInteractionListener.onGcodeCommandReceived(command);
        } else {
            EventBus.getDefault()
                    .post(new UiToastEvent(getString(R.string.text_machine_not_idle), true, true));
        }
    }
}
