package in.co.gorest.grblcontroller.ui;

import android.app.AlertDialog;
import android.os.Bundle;
import android.os.Handler;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.RadioGroup;
import androidx.annotation.NonNull;
import androidx.appcompat.widget.SwitchCompat;
import androidx.databinding.DataBindingUtil;
import in.co.gorest.grblcontroller.GrblActivity;
import in.co.gorest.grblcontroller.R;
import in.co.gorest.grblcontroller.databinding.FragmentProbingBasicBinding;
import in.co.gorest.grblcontroller.events.UiToastEvent;
import in.co.gorest.grblcontroller.listeners.MachineStatusListener;
import in.co.gorest.grblcontroller.model.Constants;
import in.co.gorest.grblcontroller.util.GrblProbe;
import in.co.gorest.grblcontroller.util.GrblUtils;
import org.greenrobot.eventbus.EventBus;

public class ProbingBasicFragment extends BaseFragment {

  private MachineStatusListener machineStatus;
  private GrblProbe grblProbe;

  private RadioGroup probeXLocation;
  private RadioGroup probeYLocation;
  private SwitchCompat probeZSwitch;
  private SwitchCompat autoZeroAfterProbe;

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

    probeXLocation = view.findViewById(R.id.probe_x_location);
    probeYLocation = view.findViewById(R.id.probe_y_location);
    probeZSwitch = view.findViewById(R.id.probe_z);
    autoZeroAfterProbe = view.findViewById(R.id.auto_zero_after_probe);

    Button startProbe = view.findViewById(R.id.start_probe);
    startProbe.setOnClickListener(view12 -> new AlertDialog.Builder(getActivity())
        .setTitle(getString(R.string.text_straight_probe))
        .setMessage(getString(R.string.text_straight_probe_desc))
        .setPositiveButton(getString(R.string.text_yes_confirm), (dialog, which) -> startProbing())
        .setNegativeButton(getString(R.string.text_cancel), null)
        .show());

    return view;
  }

  private void startProbing() {
    if (!machineStatus.getState().equals(Constants.MACHINE_STATUS_IDLE)) {
      EventBus.getDefault()
          .post(new UiToastEvent(getString(R.string.text_machine_not_idle), true, true));
      return;
    }

    fragmentInteractionListener.onGcodeCommandReceived(GrblUtils.GRBL_VIEW_PARSER_STATE_COMMAND);

    GrblProbe.Direction xAxisDir = null;
    GrblProbe.Direction yAxisDir = null;

    int xLocation = probeXLocation.getCheckedRadioButtonId();
    if (xLocation == R.id.probe_x_left) {
      xAxisDir = GrblProbe.Direction.Minus;
    } else if (xLocation == R.id.probe_x_right) {
      xAxisDir = GrblProbe.Direction.Plus;
    }

    int yLocation = probeYLocation.getCheckedRadioButtonId();
    if (yLocation == R.id.probe_y_front) {
      yAxisDir = GrblProbe.Direction.Minus;
    } else if (yLocation == R.id.probe_y_back) {
      yAxisDir = GrblProbe.Direction.Plus;
    }

    GrblProbe.Configuration.Builder builder = new GrblProbe.Configuration.Builder()
        .type(GrblProbe.ProbeType.Straight)
        .zeroZ(autoZeroAfterProbe.isChecked());

    if (xAxisDir != null) {
      builder.addAxisDir(GrblProbe.Axis.X, xAxisDir);
    }
    if (yAxisDir != null) {
      builder.addAxisDir(GrblProbe.Axis.Y, yAxisDir);
    }
    if (probeZSwitch.isChecked()) {
      builder.addAxisDir(GrblProbe.Axis.Z, GrblProbe.Direction.Minus);
    }

    // Wait for few milliseconds, just to make sure we got the parser state
    new Handler().postDelayed(() -> grblProbe.probe(builder.build()),
        (Constants.GRBL_STATUS_UPDATE_INTERVAL + 100));
  }
}
