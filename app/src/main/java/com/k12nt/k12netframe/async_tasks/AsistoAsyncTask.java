package com.k12nt.k12netframe.async_tasks;


import android.os.AsyncTask;

public abstract class AsistoAsyncTask extends AsyncTask <Void, Void, Void> {

	protected K12NetAsyncCompleteListener asyncListener = null;

    public void setOnLoadComplete(K12NetAsyncCompleteListener asyncListener){
		this.asyncListener = asyncListener;
	}
	
	@Override
	protected void onPostExecute(Void result) {
		
		onAsyncComplete();
		
		if(asyncListener != null) asyncListener.asyncTaskCompleted();
	}

	protected abstract void onAsyncComplete();


}
