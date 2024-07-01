package in.co.gorest.grblcontroller.ui;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.os.Bundle;
import android.os.Handler;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.databinding.DataBindingUtil;
import androidx.databinding.ObservableBoolean;
import androidx.databinding.ObservableDouble;

import com.joanzapata.iconify.widget.IconButton;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import in.co.gorest.grblcontroller.R;
import in.co.gorest.grblcontroller.databinding.FragmentProbingTloBinding;
import in.co.gorest.grblcontroller.events.GrblProbeEvent;
import in.co.gorest.grblcontroller.events.UiToastEvent;
import in.co.gorest.grblcontroller.helpers.EnhancedSharedPreferences;
import in.co.gorest.grblcontroller.listeners.MachineStatusListener;
import in.co.gorest.grblcontroller.model.Constants;
import in.co.gorest.grblcontroller.util.GrblUtils;

public class ProbingTloFragment extends BaseFragment {
    private MachineStatusListener machineStatus;
    private EnhancedSharedPreferences sharedPref;

    private Double startZ = null;
    private ObservableDouble referenceZ = new ObservableDouble();
    private ObservableBoolean referenceZValid = new ObservableBoolean(false);
    private String distanceMode;
    private String unitSelection;

    private boolean probeCycleRunning = false;
    private boolean probeReference = false;

    public ProbingTloFragment() {
    }

    public static ProbingTloFragment newInstance() {
        return new ProbingTloFragment();
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        machineStatus = MachineStatusListener.getInstance();
        sharedPref = EnhancedSharedPreferences.getInstance(requireActivity().getApplicationContext(), getString(R.string.shared_preference_key));
        if (savedInstanceState != null && savedInstanceState.containsKey("referenceZ")) {
            referenceZ.set(savedInstanceState.getDouble("referenceZ"));
            referenceZValid.set(true);
        }

        EventBus.getDefault().register(this);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        EventBus.getDefault().unregister(this);
    }

    @Override
    public void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        if (referenceZValid.get()) {
            outState.putDouble("referenceZ", referenceZ.get());
        }
    }

    @SuppressLint("SetTextI18n")
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

        FragmentProbingTloBinding binding = DataBindingUtil.inflate(inflater, R.layout.fragment_probing_tlo, container, false);
        binding.setMachineStatus(machineStatus);
        binding.setReferenceZValid(referenceZValid);
        binding.setReferenceZ(referenceZ);
        View view = binding.getRoot();

        IconButton probeReferenceBtn = view.findViewById(R.id.start_tlo_reference);
        probeReferenceBtn.setOnClickListener(view1 -> new AlertDialog.Builder(getActivity())
                .setTitle(getString(R.string.text_probing_tlo_probe_reference))
                .setMessage(getString(R.string.text_probing_tlo_probe_reference_desc))
                .setPositiveButton(getString(R.string.text_ok), (dialog, which) -> {
                    probeReference = true;
                    doProbing();
                })
                .setNegativeButton(getString(R.string.text_cancel), null)
                .show());

        IconButton startToolOffset = view.findViewById(R.id.start_tool_length_offset);
        startToolOffset.setOnClickListener(view1 -> {
            if (machineStatus.getLastProbePosition() == null) {
                EventBus.getDefault().post(new UiToastEvent(getString(R.string.text_last_probe_location_unknown), true, true));
                return;
            }

            new AlertDialog.Builder(getActivity())
                    .setTitle(getString(R.string.text_dynamic_tool_length_offset))
                    .setMessage(getString(R.string.text_dynamic_tlo_desc))
                    .setPositiveButton(getString(R.string.text_ok), (dialog, which) -> {
                        probeReference = false;
                        doProbing();
                    })
                    .setNegativeButton(getString(R.string.text_cancel), null)
                    .show();
        });

        IconButton cancelToolOffset = view.findViewById(R.id.cancel_tool_offset);
        cancelToolOffset.setOnClickListener(view16 -> {
            if (machineStatus.getState().equals(Constants.MACHINE_STATUS_IDLE)) {
                new AlertDialog.Builder(getActivity())
                        .setTitle(getString(R.string.text_cancel_tlo))
                        .setMessage(getString(R.string.text_cancel_tlo_desc))
                        .setPositiveButton(getString(R.string.text_yes_confirm), (dialog, which) -> {
                            fragmentInteractionListener.onGcodeCommandReceived(GrblUtils.GCODE_CANCEL_TOOL_OFFSETS);
                            fragmentInteractionListener.onGcodeCommandReceived(GrblUtils.GRBL_VIEW_GCODE_PARAMETERS_COMMAND);
                        })
                        .setNegativeButton(getString(R.string.text_no_confirm), null)
                        .show();
            } else {
                EventBus.getDefault().post(new UiToastEvent(getString(R.string.text_machine_not_idle), true, true));
            }
        });

        return view;
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

    private void onProbeZTLO(GrblProbeEvent event) {
        fragmentInteractionListener.onGcodeCommandReceived("G53G0Z" + startZ);

        if (probeReference) {
            referenceZ.set(event.getProbeCordZ());
            referenceZValid.set(true);
            EventBus.getDefault().post(new UiToastEvent(getString(R.string.text_probing_tlo_probe_reference_success)));
        } else {
            double toolOffset = referenceZ.get() - event.getProbeCordZ();
            fragmentInteractionListener.onGcodeCommandReceived("G43.1Z" + toolOffset);
            fragmentInteractionListener.onGcodeCommandReceived(GrblUtils.GRBL_VIEW_GCODE_PARAMETERS_COMMAND);
            EventBus.getDefault().post(new UiToastEvent(getString(R.string.text_probe_success_with_tlo)));
        }
    }

    private void doProbing() {
        if (!machineStatus.getState().equals(Constants.MACHINE_STATUS_IDLE)) {
            EventBus.getDefault().post(new UiToastEvent(getString(R.string.text_machine_not_idle), true, true));
            return;
        }

        fragmentInteractionListener.onGcodeCommandReceived(GrblUtils.GRBL_VIEW_PARSER_STATE_COMMAND);
        startZ = machineStatus.getMachinePosition().getCordZ();
        probeCycleRunning = true;

        // Wait for few milliseconds, just to make sure we got the parser state
        new Handler().postDelayed(() -> {
            distanceMode = machineStatus.getParserState().distanceMode;
            unitSelection = machineStatus.getParserState().unitSelection;

            probeZAxis();
        }, (Constants.GRBL_STATUS_UPDATE_INTERVAL + 100));
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onGrblProbeEvent(GrblProbeEvent event) {
        if (!probeCycleRunning) return;

        probeCycleRunning = false;

        if (!event.getIsProbeSuccess()) {
            EventBus.getDefault().post(new UiToastEvent(getString(R.string.text_probe_failed), true, true));
            fragmentInteractionListener.onGcodeCommandReceived("G53G0Z" + startZ);

            return;
        }

        onProbeZTLO(event);

        fragmentInteractionListener.onGcodeCommandReceived(distanceMode + unitSelection);

        machineStatus.setLastProbePosition(event.getProbePosition());
    }
}
