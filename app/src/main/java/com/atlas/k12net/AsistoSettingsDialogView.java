package com.atlas.k12net;

import android.content.Context;
import android.content.res.Configuration;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.SeekBar;

import com.atlas.k12net.utils.userSelection.AsistoUserReferences;

import java.util.Locale;

public class AsistoSettingsDialogView extends AsistoDailogView {

	public AsistoSettingsDialogView(Context context) {
		super(context);
	}

    public static final String ENGLISH = "en";
    public static final String ARABIC = "ar";
	
	protected int getToolbarIcon() {
		return R.drawable.asisto_icon;
	}

	@Override
	protected int getToolbarTitle() {
		return R.string.action_settings;
	}
	
	@Override
	public View getDialogView(Object objView) { 

        View view = inflater.inflate(R.layout.k12net_setting_layout, null);

        final EditText appAddress = (EditText) view.findViewById(R.id.txt_connection_address);
        final EditText fsAddress = (EditText) view.findViewById(R.id.txt_fs_address);

        appAddress.setText(AsistoUserReferences.getConnectionAddress());
        fsAddress.setText(AsistoUserReferences.getFileServerAddress());

        Button login_button = (Button) view.findViewById(R.id.btn_save);

        login_button.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View arg0) {
            AsistoUserReferences.setConnectionAddress(appAddress.getText().toString());
            AsistoUserReferences.setFileServerAddress(fsAddress.getText().toString());
            dismiss();
            }
        });

        SeekBar seekBar = (SeekBar) view.findViewById(R.id.seekBar_language);

        seekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int i, boolean b) {

                if(i == 0) {
                    AsistoUserReferences.setLanguage(ENGLISH);
                }
                else
                {
                    AsistoUserReferences.setLanguage(ARABIC);
                }

                changeLanguage();
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {

            }

        });

        if(AsistoUserReferences.getLanguageCode() == ENGLISH) {
            seekBar.setProgress(0);
        }
        else {
            seekBar.setProgress(1);
        }
		
		return view;
		
	}

    public void changeLanguage(){

        String lang = AsistoUserReferences.getLanguageCode();
        Locale locale = new Locale(lang);
        Locale.setDefault(locale);
        Configuration config = new Configuration();
        config.locale = locale;
        getContext().getResources().updateConfiguration(config,
                getContext().getResources().getDisplayMetrics());

    }

    @Override
    protected CharSequence getToolbarSubtitle() {
        return "";
    }
}
