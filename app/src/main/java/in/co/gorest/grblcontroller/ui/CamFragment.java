package in.co.gorest.grblcontroller.ui;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;

import androidx.databinding.DataBindingUtil;
import androidx.databinding.ObservableArrayList;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import java.io.FileNotFoundException;
import java.io.InputStreamReader;
import java.util.LinkedList;
import java.util.List;

import de.siegmar.fastcsv.reader.CsvReader;
import de.siegmar.fastcsv.reader.NamedCsvRecord;
import in.co.gorest.grblcontroller.R;
import in.co.gorest.grblcontroller.databinding.FragmentCamBinding;
import in.co.gorest.grblcontroller.events.ToolsImportedEvent;
import in.co.gorest.grblcontroller.listeners.MachineStatusListener;
import in.co.gorest.grblcontroller.model.Constants;
import in.co.gorest.grblcontroller.model.Tool;

public class CamFragment extends BaseFragment {
    private MachineStatusListener machineStatus;
    private final ObservableArrayList<Tool> tools = new ObservableArrayList<>();

    public CamFragment() {
    }

    public static CamFragment newInstance() {
        return new CamFragment();
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        machineStatus = MachineStatusListener.getInstance();
    }

    @Override
    public void onStart() {
        super.onStart();
        EventBus.getDefault().register(this);
    }

    @Override
    public void onStop() {
        super.onStop();
        EventBus.getDefault().unregister(this);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {
        FragmentCamBinding binding = DataBindingUtil.inflate(inflater, R.layout.fragment_cam,
                container, false);
        binding.setTools(tools);
        View view = binding.getRoot();

        Button fusionImportBtn = view.findViewById(R.id.cam_import_fusion_tools);
        fusionImportBtn.setOnClickListener(view1 -> getFilePicker());

        return view;
    }

    private void getFilePicker() {
        Intent fusionFilePicker = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        fusionFilePicker.addCategory(Intent.CATEGORY_OPENABLE);
        fusionFilePicker.setType("text/csv");
        startActivityForResult(fusionFilePicker, Constants.FILE_PICKER_CSV_REQUEST_CODE);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == Constants.FILE_PICKER_CSV_REQUEST_CODE
                && resultCode == Activity.RESULT_OK) {
            Uri uri = data.getData();

            new Thread(new Runnable() {
                @Override
                public void run() {
                    List<Tool> tools = new LinkedList<>();
                    try {
                        InputStreamReader isr = new InputStreamReader(
                                getContext().getContentResolver().openInputStream(uri));
                        CsvReader<NamedCsvRecord> reader = CsvReader.builder().ofNamedCsvRecord(
                                isr);
                        reader.forEach(record -> {
                            int number = Integer.parseInt(
                                    record.getField("Tool Index (tool_index)"));
                            String description = record.getField("Description (tool_description)");
                            int spindleSpeed = Integer.parseInt(
                                    record.getField("Spindle Speed (tool_spindleSpeed)"));
                            double diameter = Double.parseDouble(
                                    record.getField("Diameter (tool_diameter)"));
                            double cuttingFeedrate = Double.parseDouble(
                                    record.getField("Cutting Feedrate (tool_feedCutting)"));
                            double plungeFeedrate = Double.parseDouble(
                                    record.getField("Plunge Feedrate (tool_feedPlunge)"));
                            tools.add(new Tool(number, description, spindleSpeed, diameter,
                                    cuttingFeedrate, plungeFeedrate));
                        });

                        EventBus.getDefault().post(new ToolsImportedEvent(tools));
                    } catch (FileNotFoundException e) {
                        throw new RuntimeException(e);
                    }
                }
            }).start();
        }
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onToolsImportedEvent(ToolsImportedEvent event) {
        tools.clear();
        tools.addAll(event.getTools());
    }
}