package tipz.browservio.utils;

import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Message;

import com.loopj.android.http.AsyncHttpClient;
import com.loopj.android.http.AsyncHttpResponseHandler;

import cz.msebera.android.httpclient.Header;

public class DownloaderThread extends HandlerThread {
    public static final int TYPE_SUCCESS = 2;
    public static final int TYPE_FAILED = 3;

    public final static String MSG_RESPONSE = "response";

    private Handler mCallerHandler;

    public DownloaderThread(String name) {
        super(name);
    }

    public void setCallerHandler(Handler callerHandler) {
        mCallerHandler = callerHandler;
    }

    public void startDownload(String url) {
        Message message = mCallerHandler.obtainMessage();
        Bundle bundle = new Bundle();

        AsyncHttpClient client = new AsyncHttpClient();
        client.get(url, new AsyncHttpResponseHandler(getLooper()) {
            @Override
            public void onSuccess(int statusCode, Header[] headers, byte[] response) {
                if (response == null) {
                    bundle.putString(MSG_RESPONSE, CommonUtils.EMPTY_STRING);
                    message.what = TYPE_FAILED;
                } else {
                    bundle.putString(MSG_RESPONSE, new String(response));
                    message.what = TYPE_SUCCESS;
                }
            }

            @Override
            public void onFailure(int statusCode, Header[] headers, byte[] errorResponse, Throwable e) {
                if (errorResponse == null) {
                    bundle.putString(MSG_RESPONSE, CommonUtils.EMPTY_STRING);
                } else {
                    bundle.putString(MSG_RESPONSE, new String(errorResponse));
                }
                message.what = TYPE_FAILED;
            }

            @Override
            public void onFinish() {
                message.setData(bundle);
                mCallerHandler.sendMessage(message);
            }
        });
    }
}
