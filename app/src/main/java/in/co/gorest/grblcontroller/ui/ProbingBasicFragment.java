package in.co.gorest.grblcontroller.ui;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.os.Bundle;
import android.os.Handler;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.RadioGroup;

import androidx.annotation.NonNull;
import androidx.appcompat.widget.SwitchCompat;
import androidx.databinding.DataBindingUtil;

import com.joanzapata.iconify.widget.IconButton;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import in.co.gorest.grblcontroller.R;
import in.co.gorest.grblcontroller.databinding.FragmentProbingBasicBinding;
import in.co.gorest.grblcontroller.events.GrblProbeEvent;
import in.co.gorest.grblcontroller.events.UiToastEvent;
import in.co.gorest.grblcontroller.helpers.EnhancedSharedPreferences;
import in.co.gorest.grblcontroller.listeners.MachineStatusListener;
import in.co.gorest.grblcontroller.model.Constants;
import in.co.gorest.grblcontroller.model.Position;
import in.co.gorest.grblcontroller.util.GrblUtils;

public class ProbingBasicFragment extends BaseFragment {
    private MachineStatusListener machineStatus;
    private EnhancedSharedPreferences sharedPref;

    private RadioGroup probeXLocation;
    private RadioGroup probeYLocation;
    private SwitchCompat probeZSwitch;
    private SwitchCompat autoZeroAfterProbe;

    private Position probeStartPosition = null;
    private String distanceMode;
    private String unitSelection;

    private boolean probeCycleRunning = false;
    private int probeX = 0; // probing direction: -1 = left, 0 = none, 1 = right
    private int probeY = 0; // probing direction: -1 = back, 0 = none, 1 = front
    private boolean probeZ = false;
    private int probeInProgress = 0; // 1 = X, 2 = Y, 3 = Z

    public ProbingBasicFragment() {
    }

    public static ProbingBasicFragment newInstance() {
        return new ProbingBasicFragment();
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        machineStatus = MachineStatusListener.getInstance();
        sharedPref = EnhancedSharedPreferences.getInstance(requireActivity().getApplicationContext(), getString(R.string.shared_preference_key));

        EventBus.getDefault().register(this);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        EventBus.getDefault().unregister(this);
    }

    @SuppressLint("SetTextI18n")
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

        FragmentProbingBasicBinding binding = DataBindingUtil.inflate(inflater, R.layout.fragment_probing_basic, container, false);
        binding.setMachineStatus(machineStatus);
        View view = binding.getRoot();

        probeXLocation = view.findViewById(R.id.probe_x_location);
        probeYLocation = view.findViewById(R.id.probe_y_location);
        probeZSwitch = view.findViewById(R.id.probe_z);
        autoZeroAfterProbe = view.findViewById(R.id.auto_zero_after_probe);

        IconButton startProbe = view.findViewById(R.id.start_probe);
        startProbe.setOnClickListener(view12 -> new AlertDialog.Builder(getActivity())
                .setTitle(getString(R.string.text_straight_probe))
                .setMessage(getString(R.string.text_straight_probe_desc))
                .setPositiveButton(getString(R.string.text_yes_confirm), (dialog, which) -> {
                    int xLocation = probeXLocation.getCheckedRadioButtonId();
                    if (xLocation == R.id.probe_x_left) {
                        probeX = -1;
                    } else if (xLocation == R.id.probe_x_right) {
                        probeX = 1;
                    } else {
                        probeX = 0;
                    }

                    int yLocation = probeYLocation.getCheckedRadioButtonId();
                    if (yLocation == R.id.probe_y_front) {
                        probeY = -1;
                    } else if (yLocation == R.id.probe_y_back) {
                        probeY = 1;
                    } else {
                        probeY = 0;
                    }

                    probeZ = probeZSwitch.isChecked();

                    doProbing();
                })
                .setNegativeButton(getString(R.string.text_cancel), null)
                .show());

        return view;
    }

    private void probeXYAxis(String axis, int direction, double probeTo) {
        double touchDistanceXY = Double.parseDouble(
                sharedPref.getString(getString(R.string.preference_probing_touch_distance_xy), "10.0")
        );
        double touchDepthXY = Double.parseDouble(
                sharedPref.getString(getString(R.string.preference_probing_touch_depth_xy), "10.0")
        );
        double touchFeedrate = Double.parseDouble(
                sharedPref.getString(getString(R.string.preference_probing_feed_rate), "50.0")
        );
        double relativeMove = (direction < 0) ? -touchDistanceXY : touchDistanceXY;

        fragmentInteractionListener.onGcodeCommandReceived("G91");
        fragmentInteractionListener.onGcodeCommandReceived("G53G0 " + axis + (probeTo + relativeMove));
        fragmentInteractionListener.onGcodeCommandReceived("G0 Z-" + touchDepthXY);
        fragmentInteractionListener.onGcodeCommandReceived("G38.3 " + axis + (-relativeMove) + " F" + touchFeedrate);
        fragmentInteractionListener.onGcodeCommandReceived("G90");
    }

    private void onProbeXYAxis(String axis, int direction, double axisOffset, double start, double probedPosition) {
        double toolDiameter = Double.parseDouble(
                sharedPref.getString(getString(R.string.preference_probing_tool_diameter), "6.0")
        );
        double offset = axisOffset + toolDiameter / 2.0;
        double finalOffset = (direction < 0) ? -offset : offset;

        fragmentInteractionListener.onGcodeCommandReceived("G53G0" + axis + probedPosition);
        fragmentInteractionListener.onGcodeCommandReceived("G10L20P0" + axis + finalOffset);
        fragmentInteractionListener.onGcodeCommandReceived("G53G0" + axis + (probedPosition + direction)); // back off before going up
        fragmentInteractionListener.onGcodeCommandReceived("G53G0Z" + probeStartPosition.getCordZ());
        fragmentInteractionListener.onGcodeCommandReceived("G53G0" + axis + start);
    }

    private void probeZAxis() {
        double touchDepthXY = Double.parseDouble(
                sharedPref.getString(getString(R.string.preference_probing_touch_depth_xy), "10.0")
        );
        double touchFeedrate = Double.parseDouble(
                sharedPref.getString(getString(R.string.preference_probing_feed_rate), "50.0")
        );

        fragmentInteractionListener.onGcodeCommandReceived("G91");
        fragmentInteractionListener.onGcodeCommandReceived("G38.3 Z" + (-touchDepthXY) + " F" + touchFeedrate);
        fragmentInteractionListener.onGcodeCommandReceived("G90");
    }

    private void onProbeZAxis(double probedPosition) {
        if (autoZeroAfterProbe.isChecked()) {
            double plateThickness = Double.parseDouble(
                    sharedPref.getString(getString(R.string.preference_probing_plate_thickness), "10.0")
            );
            fragmentInteractionListener.onGcodeCommandReceived("G53G0Z" + probedPosition);
            fragmentInteractionListener.onGcodeCommandReceived("G10L20P0Z" + plateThickness);
        }
        fragmentInteractionListener.onGcodeCommandReceived("G53G0Z" + probeStartPosition.getCordZ());
    }

    private void probeStateMachine(GrblProbeEvent event) {
        if (probeInProgress != 0) {
            double touchPlateXOffset = Double.parseDouble(
                    sharedPref.getString(getString(R.string.preference_probing_touch_plate_x_offset), "10.0")
            );
            double touchPlateYOffset = Double.parseDouble(
                    sharedPref.getString(getString(R.string.preference_probing_touch_plate_y_offset), "10.0")
            );

            if (probeInProgress == 1) {
                onProbeXYAxis("X", probeX, touchPlateXOffset, probeStartPosition.getCordX(), event.getProbeCordX());
                probeX = 0;
            } else if (probeInProgress == 2) {
                onProbeXYAxis("Y", probeY, touchPlateYOffset, probeStartPosition.getCordY(), event.getProbeCordY());
                probeY = 0;
            } else if (probeInProgress == 3) {
                onProbeZAxis(event.getProbeCordZ());
                probeZ = false;
            }
        }

        if (probeX != 0) {
            probeXYAxis("X", probeX, probeStartPosition.getCordX());
            probeInProgress = 1;
            return;
        }

        if (probeY != 0) {
            probeXYAxis("Y", probeY, probeStartPosition.getCordY());
            probeInProgress = 2;
            return;
        }

        if (probeZ) {
            probeZAxis();
            probeInProgress = 3;
            return;
        }

        fragmentInteractionListener.onGcodeCommandReceived(distanceMode + unitSelection);
        EventBus.getDefault().post(new UiToastEvent(getString(R.string.text_probe_success)));
        probeCycleRunning = false;
        probeInProgress = 0;
    }

    private void doProbing() {
        if (machineStatus.getState().equals(Constants.MACHINE_STATUS_IDLE)) {

            fragmentInteractionListener.onGcodeCommandReceived(GrblUtils.GRBL_VIEW_PARSER_STATE_COMMAND);

            probeStartPosition = new Position(
                    machineStatus.getMachinePosition().getCordX(),
                    machineStatus.getMachinePosition().getCordY(),
                    machineStatus.getMachinePosition().getCordZ()
            );

            probeCycleRunning = true;
            probeInProgress = 0;

            // Wait for few milliseconds, just to make sure we got the parser state
            new Handler().postDelayed(() -> {
                distanceMode = machineStatus.getParserState().distanceMode;
                unitSelection = machineStatus.getParserState().unitSelection;

                probeStateMachine(null);
            }, (Constants.GRBL_STATUS_UPDATE_INTERVAL + 100));


        } else {
            EventBus.getDefault().post(new UiToastEvent(getString(R.string.text_machine_not_idle), true, true));
        }
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onGrblProbeEvent(GrblProbeEvent event) {
        if (!probeCycleRunning) return;

        if (!event.getIsProbeSuccess()) {
            EventBus.getDefault().post(new UiToastEvent(getString(R.string.text_probe_failed), true, true));
            fragmentInteractionListener.onGcodeCommandReceived("G53G0Z" + probeStartPosition.getCordZ());
            probeCycleRunning = false;
            probeInProgress = 0;
            return;
        }

        probeStateMachine(event);

        machineStatus.setLastProbePosition(event.getProbePosition());
    }
}
