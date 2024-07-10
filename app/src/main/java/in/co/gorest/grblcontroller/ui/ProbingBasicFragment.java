package in.co.gorest.grblcontroller.ui;

import android.app.AlertDialog;
import android.os.Bundle;
import android.util.Pair;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;

import androidx.annotation.NonNull;
import androidx.databinding.DataBindingUtil;

import org.greenrobot.eventbus.EventBus;

import java.util.LinkedList;
import java.util.List;

import in.co.gorest.grblcontroller.GrblActivity;
import in.co.gorest.grblcontroller.R;
import in.co.gorest.grblcontroller.databinding.FragmentProbingBasicBinding;
import in.co.gorest.grblcontroller.events.UiToastEvent;
import in.co.gorest.grblcontroller.listeners.MachineStatusListener;
import in.co.gorest.grblcontroller.model.Constants;
import in.co.gorest.grblcontroller.util.GrblProbe;
import in.co.gorest.grblcontroller.util.GrblUtils;

public class ProbingBasicFragment extends BaseFragment {

    private MachineStatusListener machineStatus;
    private GrblProbe grblProbe;

    public ProbingBasicFragment() {
    }

    public static ProbingBasicFragment newInstance() {
        return new ProbingBasicFragment();
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        machineStatus = MachineStatusListener.getInstance();
        grblProbe = GrblProbe.getInstance(getContext(), (GrblActivity) getActivity());
    }

    @Override
    public void onResume() {
        super.onResume();
        requireView().requestLayout();
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {
        FragmentProbingBasicBinding binding = DataBindingUtil.inflate(inflater,
                R.layout.fragment_probing_basic, container, false);
        binding.setMachineStatus(machineStatus);
        View view = binding.getRoot();

        for (int resourceId : new Integer[]{R.id.probing_basic_back_left_button,
                R.id.probing_basic_back_button, R.id.probing_basic_back_right_button,
                R.id.probing_basic_left_button, R.id.probing_basic_z_button,
                R.id.probing_basic_right_button, R.id.probing_basic_front_left_button,
                R.id.probing_basic_front_button, R.id.probing_basic_front_right_button}) {
            Button btn = view.findViewById(resourceId);

            LinkedList<Pair<GrblProbe.Axis, GrblProbe.Direction>> axisDirs = new LinkedList<>();
            if (resourceId == R.id.probing_basic_z_button) {
                axisDirs.add(new Pair<>(GrblProbe.Axis.Z, GrblProbe.Direction.Minus));
            } else {
                if (resourceId == R.id.probing_basic_back_left_button
                        || resourceId == R.id.probing_basic_left_button
                        || resourceId == R.id.probing_basic_front_left_button) {
                    axisDirs.add(new Pair<>(GrblProbe.Axis.X, GrblProbe.Direction.Minus));
                } else if (resourceId == R.id.probing_basic_back_right_button
                        || resourceId == R.id.probing_basic_right_button
                        || resourceId == R.id.probing_basic_front_right_button) {
                    axisDirs.add(new Pair<>(GrblProbe.Axis.X, GrblProbe.Direction.Plus));
                }

                if (resourceId == R.id.probing_basic_front_left_button
                        || resourceId == R.id.probing_basic_front_button
                        || resourceId == R.id.probing_basic_front_right_button) {
                    axisDirs.add(new Pair<>(GrblProbe.Axis.Y, GrblProbe.Direction.Minus));
                } else if (resourceId == R.id.probing_basic_back_left_button
                        || resourceId == R.id.probing_basic_back_button
                        || resourceId == R.id.probing_basic_back_right_button) {
                    axisDirs.add(new Pair<>(GrblProbe.Axis.Y, GrblProbe.Direction.Plus));
                }
            }

            btn.setOnClickListener(view12 -> startProbing(axisDirs));
        }

        return view;
    }

    private void startProbing(List<Pair<GrblProbe.Axis, GrblProbe.Direction>> axisDirs) {
        if (!machineStatus.getState().equals(Constants.MACHINE_STATUS_IDLE)) {
            EventBus.getDefault()
                    .post(new UiToastEvent(getString(R.string.text_machine_not_idle), true, true));
            return;
        }

        fragmentInteractionListener.onGcodeCommandReceived(
                GrblUtils.GRBL_VIEW_PARSER_STATE_COMMAND);

        GrblProbe.Configuration.Builder builder = new GrblProbe.Configuration.Builder()
                .type(GrblProbe.ProbeType.Straight)
                .addAxisDirs(axisDirs)
                .zeroZ(true);

        new AlertDialog.Builder(getActivity())
                .setTitle(getString(R.string.text_straight_probe))
                .setMessage(getString(R.string.text_straight_probe_desc))
                .setPositiveButton(getString(R.string.text_yes_confirm),
                        (dialog, which) -> grblProbe.probe(builder.build()))
                .setNegativeButton(getString(R.string.text_cancel), null)
                .show();
    }
}
