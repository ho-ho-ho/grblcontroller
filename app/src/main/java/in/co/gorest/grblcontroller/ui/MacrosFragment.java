package in.co.gorest.grblcontroller.ui;

import android.app.AlertDialog;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;

import androidx.annotation.NonNull;
import androidx.databinding.DataBindingUtil;

import com.joanzapata.iconify.widget.IconButton;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import java.util.LinkedList;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;

import in.co.gorest.grblcontroller.R;
import in.co.gorest.grblcontroller.databinding.FragmentMacrosBinding;
import in.co.gorest.grblcontroller.events.GrblOkEvent;
import in.co.gorest.grblcontroller.events.UiToastEvent;
import in.co.gorest.grblcontroller.helpers.EnhancedSharedPreferences;
import in.co.gorest.grblcontroller.listeners.MachineStatusListener;
import in.co.gorest.grblcontroller.model.Constants;
import in.co.gorest.grblcontroller.util.GrblUtils;

public class MacrosFragment extends BaseFragment implements View.OnClickListener,
        View.OnLongClickListener {

    private static final String TAG = MacrosFragment.class.getSimpleName();
    private MachineStatusListener machineStatus;
    private EnhancedSharedPreferences sharedPref;

    private CustomCommandsAsyncTask customCommandsAsyncTask;
    private BlockingQueue<Integer> completedCommands;

    public MacrosFragment() {
    }

    public static MacrosFragment newInstance() {
        return new MacrosFragment();
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        machineStatus = MachineStatusListener.getInstance();
        sharedPref = EnhancedSharedPreferences.getInstance(
                requireActivity().getApplicationContext(),
                getString(R.string.shared_preference_key));
        EventBus.getDefault().register(this);
    }

    public void onResume() {
        super.onResume();
        SetCustomButtons(requireView());
        requireView().requestLayout();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        EventBus.getDefault().unregister(this);
    }

    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {
        FragmentMacrosBinding binding = DataBindingUtil.inflate(inflater, R.layout.fragment_macros,
                container, false);
        binding.setMachineStatus(machineStatus);
        View view = binding.getRoot();

        IconButton alarmButton = view.findViewById(R.id.alarm_button);
        alarmButton.setOnClickListener(view1 -> {
            if (!machineStatus.getState().equals(Constants.MACHINE_STATUS_RUN)) {
                fragmentInteractionListener.onGcodeCommandReceived(
                        GrblUtils.GRBL_KILL_ALARM_LOCK_COMMAND);
            }
        });

        for (int resourceId : new Integer[]{R.id.goto_x_zero, R.id.goto_y_zero, R.id.goto_z_zero}) {
            IconButton btn = view.findViewById(resourceId);
            btn.setOnClickListener(view1 -> {
                final String tag = view1.getTag().toString();

                new AlertDialog.Builder(getActivity())
                        .setTitle(getString(R.string.text_zero_selected_axis))
                        .setMessage(
                                getString(R.string.text_set_axis_location_in_current_wpos) + tag)
                        .setPositiveButton(getString(R.string.text_yes_confirm),
                                (dialog, which) -> sendCommandIfIdle(tag))
                        .setNegativeButton(getString(R.string.text_no_confirm), null)
                        .show();
            });

            btn.setOnLongClickListener(this);
        }

        for (int resourceId : new Integer[]{R.id.wpos_g54, R.id.wpos_g55, R.id.wpos_g56,
                R.id.wpos_g57}) {
            IconButton btn = view.findViewById(resourceId);
            btn.setOnClickListener(view1 -> {
                if (machineStatus.getState().equals(Constants.MACHINE_STATUS_IDLE)) {
                    sendCommandIfIdle(view1.getTag().toString());
                    sendCommandIfIdle(GrblUtils.GRBL_VIEW_PARSER_STATE_COMMAND);
                    EventBus.getDefault().post(new UiToastEvent(
                            getString(R.string.text_selected_coordinate_system)
                                    + view1.getTag().toString()));
                } else {
                    EventBus.getDefault()
                            .post(new UiToastEvent(getString(R.string.text_machine_not_idle), true,
                                    true));
                }
            });

            btn.setOnLongClickListener(this);
        }

        return view;
    }

    private void sendCommandIfIdle(String command) {
        if (machineStatus.getState().equals(Constants.MACHINE_STATUS_IDLE)) {
            fragmentInteractionListener.onGcodeCommandReceived(command);
        } else {
            EventBus.getDefault()
                    .post(new UiToastEvent(getString(R.string.text_machine_not_idle), true, true));
        }
    }

    @Override
    public void onClick(View view) {
        int id = view.getId();

        if (id == R.id.custom_button_1 || id == R.id.custom_button_2 || id == R.id.custom_button_3
                || id == R.id.custom_button_4) {
            customButton(id, false);
        }
    }

    @Override
    public boolean onLongClick(View view) {
        int id = view.getId();

        if (id == R.id.wpos_g54 || id == R.id.wpos_g55 || id == R.id.wpos_g56
                || id == R.id.wpos_g57) {
            saveWPos((Button) view);
            return true;
        } else if (id == R.id.goto_x_zero) {
            gotoAxisZero("X");
            return true;
        } else if (id == R.id.goto_y_zero) {
            gotoAxisZero("Y");
            return true;
        } else if (id == R.id.goto_z_zero) {
            gotoAxisZero("Z");
            return true;
        } else if (id == R.id.custom_button_1 || id == R.id.custom_button_2
                || id == R.id.custom_button_3 || id == R.id.custom_button_4) {
            customButton(id, true);
            return true;
        }

        return false;
    }

    private void SetCustomButtons(View view) {
        if (sharedPref.getBoolean(getString(R.string.preference_enable_custom_buttons), false)) {
            for (int resourceId : new Integer[]{R.id.custom_button_1, R.id.custom_button_2,
                    R.id.custom_button_3, R.id.custom_button_4}) {
                IconButton iconButton = view.findViewById(resourceId);
                iconButton.setVisibility(View.VISIBLE);

                if (resourceId == R.id.custom_button_1) {
                    iconButton.setText(
                            sharedPref.getString(getString(R.string.preference_custom_button_one),
                                    getString(R.string.text_value_na)));
                }
                if (resourceId == R.id.custom_button_2) {
                    iconButton.setText(
                            sharedPref.getString(getString(R.string.preference_custom_button_two),
                                    getString(R.string.text_value_na)));
                }
                if (resourceId == R.id.custom_button_3) {
                    iconButton.setText(
                            sharedPref.getString(getString(R.string.preference_custom_button_three),
                                    getString(R.string.text_value_na)));
                }
                if (resourceId == R.id.custom_button_4) {
                    iconButton.setText(
                            sharedPref.getString(getString(R.string.preference_custom_button_four),
                                    getString(R.string.text_value_na)));
                }

                iconButton.setOnLongClickListener(this);
                iconButton.setOnClickListener(this);
            }
        } else {
            for (int resourceId : new Integer[]{R.id.custom_button_1, R.id.custom_button_2,
                    R.id.custom_button_3, R.id.custom_button_4}) {
                IconButton iconButton = view.findViewById(resourceId);
                iconButton.setVisibility(View.GONE);
            }
        }
    }

    private void customButton(int resourceId, boolean isLongClick) {
        if (!machineStatus.getState().equals(Constants.MACHINE_STATUS_IDLE)) {
            EventBus.getDefault()
                    .post(new UiToastEvent(getString(R.string.text_machine_not_idle), true, true));
            return;
        }

        String title = "";
        String commands = "";
        boolean confirmFirst = true;

        if (resourceId == R.id.custom_button_1) {
            title = sharedPref.getString(getString(R.string.preference_custom_button_one),
                    getString(R.string.text_value_na));
            commands = isLongClick ? sharedPref.getString(
                    getString(R.string.preference_custom_button_one_long_click), "")
                    : sharedPref.getString(
                            getString(R.string.preference_custom_button_one_short_click), "");
            confirmFirst = sharedPref.getBoolean(
                    getString(R.string.preference_custom_button_one_confirm),
                    true);
        }

        if (resourceId == R.id.custom_button_2) {
            title = sharedPref.getString(getString(R.string.preference_custom_button_two),
                    getString(R.string.text_value_na));
            commands = isLongClick ? sharedPref.getString(
                    getString(R.string.preference_custom_button_two_long_click), "")
                    : sharedPref.getString(
                            getString(R.string.preference_custom_button_two_short_click), "");
            confirmFirst = sharedPref.getBoolean(
                    getString(R.string.preference_custom_button_two_confirm),
                    true);
        }

        if (resourceId == R.id.custom_button_3) {
            title = sharedPref.getString(getString(R.string.preference_custom_button_three),
                    getString(R.string.text_value_na));
            commands = isLongClick ? sharedPref.getString(
                    getString(R.string.preference_custom_button_three_long_click), "")
                    : sharedPref.getString(
                            getString(R.string.preference_custom_button_three_short_click),
                            "");
            confirmFirst = sharedPref.getBoolean(
                    getString(R.string.preference_custom_button_three_confirm), true);
        }

        if (resourceId == R.id.custom_button_4) {
            title = sharedPref.getString(getString(R.string.preference_custom_button_four),
                    getString(R.string.text_value_na));
            commands = isLongClick ? sharedPref.getString(
                    getString(R.string.preference_custom_button_four_long_click), "")
                    : sharedPref.getString(
                            getString(R.string.preference_custom_button_four_short_click), "");
            confirmFirst = sharedPref.getBoolean(
                    getString(R.string.preference_custom_button_four_confirm), true);
        }

        if (commands.trim().length() <= 0) {
            EventBus.getDefault()
                    .post(new UiToastEvent(getString(R.string.text_empty_command), true, true));
            return;
        }

        final String finalCommands = commands;

        if (confirmFirst) {
            String alertSummary =
                    isLongClick ? getString(R.string.text_long_click) : getString(
                            R.string.text_short_click);
            new AlertDialog.Builder(getActivity())
                    .setTitle(getString(R.string.text_custom_action) + title)
                    .setMessage(
                            getString(R.string.text_send_custom_command) + alertSummary + getString(
                                    R.string.text_on_button) + title)
                    .setPositiveButton(getString(R.string.text_send), (dialog, which) -> {
                        customCommandsAsyncTask = new MacrosFragment.CustomCommandsAsyncTask();
                        customCommandsAsyncTask.execute(finalCommands);
                    })
                    .setNegativeButton(getString(R.string.text_cancel), null)
                    .show();
        } else {
            customCommandsAsyncTask = new MacrosFragment.CustomCommandsAsyncTask();
            customCommandsAsyncTask.execute(finalCommands);
        }
    }

    private void gotoAxisZero(final String axis) {
        new AlertDialog.Builder(getActivity())
                .setTitle(
                        getString(R.string.text_move) + axis + getString(
                                R.string.text_axis_to_zero_position))
                .setMessage(getString(R.string.text_go_to_zero_position) + axis + "0")
                .setPositiveButton(getString(R.string.text_yes_confirm),
                        (dialog, which) -> sendCommandIfIdle("G0 " + axis + "0"))
                .setNegativeButton(getString(R.string.text_no_confirm), null)
                .show();
    }

    private void saveWPos(Button button) {
        String wpos = button.getTag().toString();
        final String slot;

        switch (wpos) {
            case "G54":
                slot = "P1";
                break;
            case "G56":
                slot = "P3";
                break;
            case "G57":
                slot = "P4";
                break;
            default:
//              //G55
                slot = "P2";
        }

        new AlertDialog.Builder(getActivity())
                .setTitle(R.string.text_save_coordinate_system)
                .setMessage(getString(R.string.text_save_coordinate_system_desc) + " " + wpos + "?")
                .setPositiveButton(getString(R.string.text_yes_confirm),
                        (dialog, which) -> sendCommandIfIdle(
                                String.format("G10 L20 %s X0Y0Z0", slot)))
                .setNegativeButton(getString(R.string.text_no_confirm), null)
                .show();
    }

    @Subscribe(threadMode = ThreadMode.BACKGROUND)
    public void onGrblOkEvent(GrblOkEvent event) {
        if (customCommandsAsyncTask != null
                && customCommandsAsyncTask.getStatus() == AsyncTask.Status.RUNNING) {
            completedCommands.offer(1);
        }
    }

    private class CustomCommandsAsyncTask extends AsyncTask<String, Integer, Integer> {

        private int MAX_RX_SERIAL_BUFFER = Constants.DEFAULT_SERIAL_RX_BUFFER - 3;
        private int CURRENT_RX_SERIAL_BUFFER;
        private LinkedList<Integer> activeCommandSizes;

        protected void onPreExecute() {
            MachineStatusListener.CompileTimeOptions compileTimeOptions =
                    MachineStatusListener.getInstance()
                            .getCompileTimeOptions();
            if (compileTimeOptions.serialRxBuffer > 0) {
                MAX_RX_SERIAL_BUFFER = compileTimeOptions.serialRxBuffer - 3;
            }

            completedCommands = new ArrayBlockingQueue<>(Constants.DEFAULT_SERIAL_RX_BUFFER);
            activeCommandSizes = new LinkedList<>();
            CURRENT_RX_SERIAL_BUFFER = 0;
        }

        protected Integer doInBackground(String... commands) {

            String[] lines = commands[0].split("[\r\n]+");
            for (String command : lines) {
                if (isCancelled()) {
                    break;
                }
                streamLine(command);
            }

            return 1;
        }

        private void streamLine(String gcodeCommand) {
            int commandSize = gcodeCommand.length() + 1;

            // Wait until there is room, if necessary.
            while (MAX_RX_SERIAL_BUFFER < (CURRENT_RX_SERIAL_BUFFER + commandSize)) {
                try {
                    completedCommands.take();
                    if (activeCommandSizes.size() > 0) {
                        CURRENT_RX_SERIAL_BUFFER -= activeCommandSizes.removeFirst();
                    }
                } catch (InterruptedException e) {
                    Log.e(TAG, e.getMessage(), e);
                    return;
                }
            }

            activeCommandSizes.offer(commandSize);
            CURRENT_RX_SERIAL_BUFFER += commandSize;
            fragmentInteractionListener.onGcodeCommandReceived(gcodeCommand);
        }
    }
}
