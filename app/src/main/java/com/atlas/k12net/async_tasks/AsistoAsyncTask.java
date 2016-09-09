package com.atlas.k12net.async_tasks;


import android.os.AsyncTask;

public abstract class AsistoAsyncTask extends AsyncTask <Void, Void, Void> {

	protected AsistoAsyncCompleteListener asyncListener = null;

    public void setOnLoadComplete(AsistoAsyncCompleteListener asyncListener){
		this.asyncListener = asyncListener;
	}
	
	@Override
	protected void onPostExecute(Void result) {
		
		onAsyncComplete();
		
		if(asyncListener != null) asyncListener.asyncTaskCompleted();
	}

	protected abstract void onAsyncComplete();


}
