package com.k12nt.k12netframe;

import android.content.Context;
import android.content.res.Configuration;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.SeekBar;
import android.widget.ToggleButton;

import com.k12nt.k12netframe.utils.userSelection.K12NetUserReferences;

import java.util.Locale;

public class K12NetSettingsDialogView extends K12NetDailogView {

    public static String TURKISH = "tr";
    public static String ENGLISH = "en";
    public static String ARABIC = "ar";


	public K12NetSettingsDialogView(Context context) {
		super(context);
	}

    ToggleButton[] language_btn_list = new ToggleButton[3];
	
	protected int getToolbarIcon() {
		return R.drawable.k12net_logo;
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

        appAddress.setText(K12NetUserReferences.getConnectionAddress());
        fsAddress.setText(K12NetUserReferences.getFileServerAddress());

        Button login_button = (Button) view.findViewById(R.id.btn_save);

        login_button.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View arg0) {
            K12NetUserReferences.setConnectionAddress(appAddress.getText().toString());
            K12NetUserReferences.setFileServerAddress(fsAddress.getText().toString());
            dismiss();
            }
        });


        ToggleButton btn_tr = (ToggleButton) view.findViewById(R.id.btn_tr);
        ToggleButton btn_en = (ToggleButton) view.findViewById(R.id.btn_en);
        ToggleButton btn_ar = (ToggleButton) view.findViewById(R.id.btn_ar);

        language_btn_list[0] = btn_tr;
        language_btn_list[1] = btn_en;
        language_btn_list[2] = btn_ar;

        btn_tr.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View arg0) {
                updateLanguage(0, TURKISH);
            }
        });

        btn_en.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View arg0) {
                updateLanguage(1, ENGLISH);
            }
        });

        btn_ar.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View arg0) {
                updateLanguage(2, ARABIC);
            }
        });

        if(K12NetUserReferences.getLanguageCode().equals(TURKISH))  {
            updateLanguage(0, TURKISH);
        }
        else if(K12NetUserReferences.getLanguageCode().equals(ENGLISH)) {
            updateLanguage(1, ENGLISH);
        }
        else if(K12NetUserReferences.getLanguageCode().equals(ARABIC)) {
            updateLanguage(2, ARABIC);
        }

		return view;
		
	}

    private void updateLanguage(int languageIndex, String languageCode) {
        K12NetUserReferences.setLanguage(languageCode);

        for(int i = 0; i < language_btn_list.length;i++) {
            language_btn_list[i].setChecked(i == languageIndex);
        }

    }

    public void changeLanguage(){

        String lang = K12NetUserReferences.getLanguageCode();
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
