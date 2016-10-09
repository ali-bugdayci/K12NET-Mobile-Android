package com.k12nt.k12netframe;

import android.app.Activity;
import android.app.Dialog;
import android.os.AsyncTask.Status;
import android.os.Bundle;
import android.util.DisplayMetrics;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.Window;
import android.widget.GridView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;

import com.k12nt.k12netframe.async_tasks.K12NetAsyncCompleteListener;
import com.k12nt.k12netframe.async_tasks.AsistoAsyncTask;

public abstract class K12NetActivity extends Activity implements K12NetAsyncCompleteListener {
	
	Dialog openDialog = null;
	protected AsistoAsyncTask atask = null;
	
	GridView list = null;
	protected LinearLayout mainLayout = null;
	protected LayoutInflater inflater = null;
	protected RelativeLayout loadView = null;

    static Boolean isMobile = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
    	
        super.onCreate(savedInstanceState);
        
      //Remove title bar
        this.requestWindowFeature(Window.FEATURE_NO_TITLE);

        //setStatusBarColor(K12NetStaticDefinition.TORQUESE);

        if(isMobile == null) {
            DisplayMetrics metrics = new DisplayMetrics();
            getWindowManager().getDefaultDisplay().getMetrics(metrics);
            int density = metrics.densityDpi;

            isMobile = metrics.widthPixels < getApplicationContext().getResources().getDimension(R.dimen.screen_size_width_limit);
        }

//        //Remove notification bar
//        this.getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
//
//       //set content view AFTER ABOVE sequence (to avoid crash)
//        this.setContentView(R.layout.your_layout_name_here); 
        
       // requestWindowFeature(Window.FEATURE_ACTION_BAR);
        
        setContentView(R.layout.k12net_activity_layout);
        
        inflater = LayoutInflater.from(getApplicationContext());
        
     /*   ActionBar actBar = getActionBar();
        actBar.setDisplayHomeAsUpEnabled(true);
        actBar.setBackgroundDrawable(getApplicationContext().getResources().getDrawable(R.drawable.action_bar_color));
        actBar.setTitle(getToolbarTitle());
        actBar.setSubtitle(getToolbarSubtitle());
        actBar.setIcon(getToolbarIcon());*/
        
     /*   Typeface type = Typeface.createFromAsset(getAssets(),"fonts/ProximaNova/ProximaNova-Light.otf"); 
        int titleId = getResources().getIdentifier("action_bar_title", "id", "android");
        TextView yourTextView = (TextView) findViewById(titleId);
        yourTextView.setTextColor(getResources().getColor(android.R.color.white));
        yourTextView.setTextSize(R.dimen.titleTextSize);
        yourTextView.setTypeface(type);
       */
		
		mainLayout = (LinearLayout)findViewById(R.id.lyt_activity);
		
		LinearLayout back_button = (LinearLayout) findViewById(R.id.lyt_back);
		back_button.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				 onBackPressed();
				 finish();
			}
		});
		
		initMainData();		
		
		overridePendingTransition(android.R.anim.slide_in_left, android.R.anim.slide_in_left);
	}

	protected void initMainData() {
		loadView = (RelativeLayout) inflater.inflate(R.layout.modal_loading_layout, null, false);
		mainLayout.addView(loadView);
		
		buildCustomView();
		
		atask = getAsyncTask();
		if(atask != null && atask.getStatus() != Status.RUNNING) {
			atask.setOnLoadComplete(this);
			atask.execute();
		}
	}
	
	public abstract void buildCustomView();

	protected abstract AsistoAsyncTask getAsyncTask();

	protected CharSequence getToolbarSubtitle() {
        String toolbarSubtitle = "";
       /* if(MainActivity.getSelectedEnrollment() != null) {
            toolbarSubtitle += MainActivity.getSelectedPersonelInfo().getName().getFullName() + " - " + MainActivity.getSelectedEnrollment().getSchoolYearID();
        }*/
        return toolbarSubtitle;
    }

	protected abstract int getToolbarIcon();

	protected abstract int getToolbarTitle();
	

	@Override
	public void onBackPressed() {
		if(atask != null) {
			atask.cancel(true);
		}
		super.onBackPressed();
	}
	
	protected void openDialog(Dialog dialog) {
		openDialog = dialog;
		openDialog.show();
	}
	
	@Override
	protected void onPause() {
		if(openDialog != null) openDialog.dismiss();
		
		super.onPause();
	}
	
	public abstract void asyncTaskCompleted();
	
	protected Activity getActivity(){
		return this;
	}	

	public void refreshView(){
		initMainData();
	}

    //
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
        case android.R.id.home: 
            // API 5+ solution
            onBackPressed();
            return true;

        default:
            return super.onOptionsItemSelected(item);
        }
    }
}
