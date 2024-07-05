package in.co.gorest.grblcontroller.ui;

import android.annotation.SuppressLint;
import android.os.Bundle;
import android.os.Handler;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ToggleButton;

import androidx.annotation.NonNull;
import androidx.databinding.DataBindingUtil;
import androidx.databinding.ObservableDouble;

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

    private ToggleButton outsideBtn;

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

    @SuppressLint("SetTextI18n")
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        FragmentProbingCenterBinding binding = DataBindingUtil.inflate(inflater, R.layout.fragment_probing_center, container, false);
        binding.setMachineStatus(machineStatus);
        binding.setPosLeft(grblProbe.getCenterPosLeft());
        binding.setPosRight(grblProbe.getCenterPosRight());
        binding.setPosBack(grblProbe.getCenterPosBack());
        binding.setPosFront(grblProbe.getCenterPosFront());
        View view = binding.getRoot();

        outsideBtn = view.findViewById(R.id.probing_center_outside_btn);
        Button leftBtn = view.findViewById(R.id.probe_center_x_left_btn);
        leftBtn.setOnClickListener(view1 -> startProbing(GrblProbe.Axis.X, GrblProbe.Direction.Minus));

        Button rightBtn = view.findViewById(R.id.probe_center_x_right_btn);
        rightBtn.setOnClickListener(view1 -> startProbing(GrblProbe.Axis.X, GrblProbe.Direction.Plus));

        Button backBtn = view.findViewById(R.id.probe_center_y_back_btn);
        backBtn.setOnClickListener(view1 -> startProbing(GrblProbe.Axis.Y, GrblProbe.Direction.Plus));

        Button frontBtn = view.findViewById(R.id.probe_center_y_front_btn);
        frontBtn.setOnClickListener(view1 -> startProbing(GrblProbe.Axis.Y, GrblProbe.Direction.Minus));

        Button applyBtn = view.findViewById(R.id.probe_center_apply_btn);
        applyBtn.setOnClickListener(view1 -> {
            if (!machineStatus.getState().equals(Constants.MACHINE_STATUS_IDLE)) {
                EventBus.getDefault().post(new UiToastEvent(getString(R.string.text_machine_not_idle), true, true));
                return;
            }

            ObservableDouble left = grblProbe.getCenterPosLeft();
            ObservableDouble right = grblProbe.getCenterPosRight();
            ObservableDouble back = grblProbe.getCenterPosBack();
            ObservableDouble front = grblProbe.getCenterPosFront();
            if (left.get() != Double.MAX_VALUE && right.get() != Double.MAX_VALUE) {
                double center = (left.get() + right.get()) / 2;
                applyCenter(GrblProbe.Axis.X, center);
                left.set(Double.MAX_VALUE);
                right.set(Double.MAX_VALUE);
            }

            if (back.get() != Double.MAX_VALUE && front.get() != Double.MAX_VALUE) {
                double center = (back.get() + front.get()) / 2;
                applyCenter(GrblProbe.Axis.Y, center);
                back.set(Double.MAX_VALUE);
                front.set(Double.MAX_VALUE);
            }

            EventBus.getDefault().post(new UiToastEvent("Center applied successfully."));
        });

        return view;
    }

    private void applyCenter(GrblProbe.Axis axis, double center) {
        fragmentInteractionListener.onGcodeCommandReceived("G53G0" + axis.name() + center);
        fragmentInteractionListener.onGcodeCommandReceived("G90G10L20P0" + axis.name() + "0");
    }

    private void startProbing(GrblProbe.Axis axis, GrblProbe.Direction dir) {
        if (!machineStatus.getState().equals(Constants.MACHINE_STATUS_IDLE)) {
            EventBus.getDefault().post(new UiToastEvent(getString(R.string.text_machine_not_idle), true, true));
            return;
        }

        fragmentInteractionListener.onGcodeCommandReceived(GrblUtils.GRBL_VIEW_PARSER_STATE_COMMAND);

        GrblProbe.Configuration.Builder builder = new GrblProbe.Configuration.Builder()
                .type(GrblProbe.ProbeType.Center)
                .centerDirection(outsideBtn.isChecked() ? GrblProbe.CenterDirection.Outside : GrblProbe.CenterDirection.Inside)
                .addAxisDir(axis, dir);

        // Wait for few milliseconds, just to make sure we got the parser state
        new Handler().postDelayed(() -> grblProbe.probe(builder.build()), (Constants.GRBL_STATUS_UPDATE_INTERVAL + 100));
    }
}
