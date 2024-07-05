package in.co.gorest.grblcontroller.ui;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.os.Bundle;
import android.os.Handler;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;

import androidx.annotation.NonNull;
import androidx.databinding.DataBindingUtil;

import org.greenrobot.eventbus.EventBus;

import in.co.gorest.grblcontroller.GrblActivity;
import in.co.gorest.grblcontroller.R;
import in.co.gorest.grblcontroller.databinding.FragmentProbingTloBinding;
import in.co.gorest.grblcontroller.events.UiToastEvent;
import in.co.gorest.grblcontroller.listeners.MachineStatusListener;
import in.co.gorest.grblcontroller.model.Constants;
import in.co.gorest.grblcontroller.util.GrblProbe;
import in.co.gorest.grblcontroller.util.GrblUtils;

public class ProbingTloFragment extends BaseFragment {
    private MachineStatusListener machineStatus;
    private GrblProbe grblProbe;

    public ProbingTloFragment() {
    }

    public static ProbingTloFragment newInstance() {
        return new ProbingTloFragment();
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        machineStatus = MachineStatusListener.getInstance();
        grblProbe = GrblProbe.getInstance(getContext(), (GrblActivity) getActivity());
    }

    @SuppressLint("SetTextI18n")
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

        FragmentProbingTloBinding binding = DataBindingUtil.inflate(inflater, R.layout.fragment_probing_tlo, container, false);
        binding.setMachineStatus(machineStatus);
        binding.setReferenceZ(grblProbe.getReferenceZValue());
        View view = binding.getRoot();

        Button probeReferenceBtn = view.findViewById(R.id.start_tlo_reference);
        probeReferenceBtn.setOnClickListener(view1 -> new AlertDialog.Builder(getActivity())
                .setTitle(getString(R.string.text_probing_tlo_probe_reference))
                .setMessage(getString(R.string.text_probing_tlo_probe_reference_desc))
                .setPositiveButton(getString(R.string.text_ok), (dialog, which) -> doProbe(true))
                .setNegativeButton(getString(R.string.text_cancel), null)
                .show());

        Button startToolOffset = view.findViewById(R.id.start_tool_length_offset);
        startToolOffset.setOnClickListener(view1 -> {
            if (machineStatus.getLastProbePosition() == null) {
                EventBus.getDefault().post(new UiToastEvent(getString(R.string.text_last_probe_location_unknown), true, true));
                return;
            }

            new AlertDialog.Builder(getActivity())
                    .setTitle(getString(R.string.text_dynamic_tool_length_offset))
                    .setMessage(getString(R.string.text_dynamic_tlo_desc))
                    .setPositiveButton(getString(R.string.text_ok), (dialog, which) -> doProbe(false))
                    .setNegativeButton(getString(R.string.text_cancel), null)
                    .show();
        });

        Button cancelToolOffset = view.findViewById(R.id.cancel_tool_offset);
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

    private void doProbe(boolean reference) {
        if (!machineStatus.getState().equals(Constants.MACHINE_STATUS_IDLE)) {
            EventBus.getDefault().post(new UiToastEvent(getString(R.string.text_machine_not_idle), true, true));
            return;
        }

        fragmentInteractionListener.onGcodeCommandReceived(GrblUtils.GRBL_VIEW_PARSER_STATE_COMMAND);

        GrblProbe.Configuration.Builder builder = new GrblProbe.Configuration.Builder()
                .type(GrblProbe.ProbeType.ToolLengthOffset)
                .addAxisDir(GrblProbe.Axis.Z, GrblProbe.Direction.Minus)
                .referenceZ(reference)
                .zeroZ(false);

        new Handler().postDelayed(() -> grblProbe.probe(builder.build()), (Constants.GRBL_STATUS_UPDATE_INTERVAL + 100));
    }
}
