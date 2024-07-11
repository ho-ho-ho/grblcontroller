package in.co.gorest.grblcontroller.ui;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ToggleButton;

import androidx.annotation.NonNull;
import androidx.databinding.DataBindingUtil;

import org.greenrobot.eventbus.EventBus;

import in.co.gorest.grblcontroller.GrblActivity;
import in.co.gorest.grblcontroller.R;
import in.co.gorest.grblcontroller.databinding.FragmentProbingCenterBinding;
import in.co.gorest.grblcontroller.events.UiToastEvent;
import in.co.gorest.grblcontroller.listeners.MachineStatusListener;
import in.co.gorest.grblcontroller.model.Constants;
import in.co.gorest.grblcontroller.util.GrblProbe;
import in.co.gorest.grblcontroller.util.GrblUtils;

public class ProbingCenterFragment extends BaseFragment {

    private MachineStatusListener machineStatus;
    private GrblProbe grblProbe;

    public ProbingCenterFragment() {
    }

    public static ProbingCenterFragment newInstance() {
        return new ProbingCenterFragment();
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

    @SuppressLint("SetTextI18n")
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {
        FragmentProbingCenterBinding binding = DataBindingUtil.inflate(inflater,
                R.layout.fragment_probing_center, container, false);
        binding.setMachineStatus(machineStatus);
        binding.setPosLeft(grblProbe.getCenterPosLeft());
        binding.setPosRight(grblProbe.getCenterPosRight());
        binding.setPosBack(grblProbe.getCenterPosBack());
        binding.setPosFront(grblProbe.getCenterPosFront());

        View view = binding.getRoot();

        ToggleButton outsideBtn = view.findViewById(R.id.probing_center_outside_btn);
        outsideBtn.setOnClickListener(view1 -> grblProbe.clearCenterPositions());

        Button leftBtn = view.findViewById(R.id.probe_center_x_left_btn);
        leftBtn.setOnClickListener(
                view1 -> startProbing(GrblProbe.Axis.X, GrblProbe.Direction.Minus,
                        outsideBtn.isChecked()));

        Button rightBtn = view.findViewById(R.id.probe_center_x_right_btn);
        rightBtn.setOnClickListener(
                view1 -> startProbing(GrblProbe.Axis.X, GrblProbe.Direction.Plus,
                        outsideBtn.isChecked()));

        Button backBtn = view.findViewById(R.id.probe_center_y_back_btn);
        backBtn.setOnClickListener(
                view1 -> startProbing(GrblProbe.Axis.Y, GrblProbe.Direction.Plus,
                        outsideBtn.isChecked()));

        Button frontBtn = view.findViewById(R.id.probe_center_y_front_btn);
        frontBtn.setOnClickListener(
                view1 -> startProbing(GrblProbe.Axis.Y, GrblProbe.Direction.Minus,
                        outsideBtn.isChecked()));

        Button applyBtn = view.findViewById(R.id.probe_center_apply_btn);
        applyBtn.setOnClickListener(view1 -> applyCenters());

        return view;
    }

    private void startProbing(GrblProbe.Axis axis, GrblProbe.Direction dir, boolean isOutside) {
        if (!machineStatus.getState().equals(Constants.MACHINE_STATUS_IDLE)) {
            EventBus.getDefault()
                    .post(new UiToastEvent(getString(R.string.text_machine_not_idle), true, true));
            return;
        }

        fragmentInteractionListener.onGcodeCommandReceived(
                GrblUtils.GRBL_VIEW_PARSER_STATE_COMMAND);

        GrblProbe.Configuration.Builder builder = new GrblProbe.Configuration.Builder()
                .type(GrblProbe.ProbeType.Center)
                .centerDirection(isOutside ? GrblProbe.CenterDirection.Outside
                        : GrblProbe.CenterDirection.Inside)
                .addAxisDir(axis, dir);

        new AlertDialog.Builder(getActivity())
                .setTitle(getString(R.string.text_probing_center))
                .setMessage(getString(R.string.text_straight_probe_desc))
                .setPositiveButton(getString(R.string.text_yes_confirm),
                        (dialog, which) -> grblProbe.probe(builder.build()))
                .setNegativeButton(getString(R.string.text_cancel), null)
                .show();
    }

    private void applyCenters() {
        if (!machineStatus.getState().equals(Constants.MACHINE_STATUS_IDLE)) {
            EventBus.getDefault().post(
                    new UiToastEvent(getString(R.string.text_machine_not_idle), true, true));
            return;
        }

        grblProbe.applyCenters();
    }
}
