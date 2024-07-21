package in.co.gorest.grblcontroller.adapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;

import androidx.databinding.DataBindingUtil;

import in.co.gorest.grblcontroller.R;
import in.co.gorest.grblcontroller.databinding.FragmentToolListEntryBinding;
import in.co.gorest.grblcontroller.listeners.FileSenderListener;
import in.co.gorest.grblcontroller.util.ToolLibrary;

public class ToolListAdapter extends BaseAdapter {
    private final Context context;
    private final FileSenderListener fileSenderListener;

    public ToolListAdapter(Context context) {
        this.context = context;
        fileSenderListener = FileSenderListener.getInstance();
    }

    @Override
    public int getCount() {
        return fileSenderListener.getToolsUsed().size();
    }

    @Override
    public Integer getItem(int position) {
        return fileSenderListener.getToolsUsed().get(position);
    }

    @Override
    public long getItemId(int position) {
        return fileSenderListener.getToolsUsed().get(position);
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        FragmentToolListEntryBinding binding;
        if (convertView == null) {
            binding = DataBindingUtil.inflate(LayoutInflater.from(context),
                    R.layout.fragment_tool_list_entry, parent, false);
            convertView = binding.getRoot();
        } else {
            binding = (FragmentToolListEntryBinding) convertView.getTag();
        }

        binding.setToolLibrary(ToolLibrary.INSTANCE);
        binding.setTool(fileSenderListener.getToolsUsed().get(position));

        convertView.setTag(binding);
        return convertView;
    }
}
