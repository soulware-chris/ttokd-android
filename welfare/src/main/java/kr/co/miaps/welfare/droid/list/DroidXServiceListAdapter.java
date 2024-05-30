/*
 * Copyrightⓒ 2018 NSHC.
 * All Rights Reserved.
 *
 * Droid-X에서 악성코드 검출시 검출된 앱을 List로 관리하기 위하여 연결된 Adapter 샘플
 *
 * 2018. 03. 08
 */

package kr.co.miaps.welfare.droid.list;

import android.app.Activity;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.RelativeLayout;
import android.widget.TextView;
import java.util.List;

import kr.co.miaps.welfare.R;


public class DroidXServiceListAdapter<T> extends ArrayAdapter<String> {

	private static final int TYPE_MALWARE	= 0x01;

	static class ResultHolder
	{
		int index;
		RelativeLayout rlRowScanresult;
		TextView imgTypeIcon;
		TextView tvCause;
		TextView tvInform;
		TextView tvFilepath;
		CheckBox imgHandle;
		int type = TYPE_MALWARE;
	}

	Context context;
	List<String> data = null;
	int layoutResourceId;
	boolean[] bChecked = null;
	int[] iHandleStatus = null;

	public DroidXServiceListAdapter(Context context, int resource, List<String> objects) {
		super(context, resource, objects);

		this.context = context;

		this.layoutResourceId = resource;
		this.data = objects;
		bChecked = new boolean[this.data.size()];
		iHandleStatus = new int[this.data.size()];
	}

	@Override
	public View getView(final int position, View convertView, ViewGroup parent) {
		View row = convertView;
		ResultHolder holder = null;

		if(row == null) {
			LayoutInflater inflater = ((Activity)context).getLayoutInflater();
			row = inflater.inflate(R.layout.activity_malware_row_list, parent, false);

			holder = new ResultHolder();
			holder.rlRowScanresult = (RelativeLayout)row.findViewById(R.id.rl_row_scanresult);
			holder.imgTypeIcon = (TextView)row.findViewById(R.id.img_scanresult_row_icon);
			holder.tvCause = (TextView)row.findViewById(R.id.tv_scanresult_row_cause);
			holder.tvInform = (TextView)row.findViewById(R.id.tv_scanresult_row_inform);
			holder.tvFilepath = (TextView)row.findViewById(R.id.tv_scanresult_row_filepath);
			holder.imgHandle = (CheckBox)row.findViewById(R.id.img_scanresult_row_handle);

			row.setTag(holder);
		} else {
			holder = (ResultHolder)row.getTag();
		}

		String strdata = data.get(position);
		if(strdata != null) {
			String[] tmpstrs = strdata.split("\n");
			for(String tmps : tmpstrs) {
				if(tmps.startsWith("Desc: ")) {
					holder.tvInform.setText(tmps.substring(6));
				} else if(tmps.startsWith("Path: ")) {
					holder.tvFilepath.setText(tmps.substring(6));
				} else if(tmps.startsWith("Package: ")) {
					holder.tvCause.setText(tmps.substring(9));
				} else if(tmps.startsWith("Type: ")) {
					holder.type = TYPE_MALWARE;
				}
			}
		}

		holder.imgHandle.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
			@Override
			public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
				setChecked(position, isChecked);
			}
		});

		if(bChecked != null)
			holder.imgHandle.setChecked(bChecked[position]);

		return row;
	}

	public void setAllChecked(boolean bChecked) {
		int tempsize = this.bChecked.length;

		for(int i=0;i<tempsize;i++) {
			this.bChecked[i] = bChecked;
		}
	}

	public void setChecked(int position, boolean bChecked) {
		this.bChecked[position] = bChecked;
	}

	public boolean getChecked(int position) {
		return this.bChecked[position];
	}
}
