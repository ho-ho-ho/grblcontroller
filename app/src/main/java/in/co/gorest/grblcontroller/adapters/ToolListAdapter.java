package in.co.gorest.grblcontroller.adapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;

import androidx.databinding.DataBindingUtil;

import java.util.List;

import in.co.gorest.grblcontroller.R;
import in.co.gorest.grblcontroller.databinding.FragmentToolListEntryBinding;
import in.co.gorest.grblcontroller.util.ToolLibrary;

// shows a list of tools (from a List<Integer>)
public class ToolListAdapter extends BaseAdapter {
    private final Context context;
    private final List<Integer> list;

    public ToolListAdapter(Context context, List<Integer> list) {
        this.context = context;
        this.list = list;
    }

    @Override
    public int getCount() {
        return list.size();
    }

    @Override
    public Integer getItem(int position) {
        return list.get(position);
    }

    @Override
    public long getItemId(int position) {
        return list.get(position);
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
        binding.setTool(list.get(position));

        convertView.setTag(binding);
        return convertView;
    }
}
