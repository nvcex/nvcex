package io.github.nvcex.android;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import android.os.RemoteException;
import android.util.Log;

import androidx.annotation.Nullable;

public class SynthesizeService extends Service {
    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        Log.i(this.getClass().toString(), "onBind: " + intent);

        ISynthesizeService.Stub binder = new ISynthesizeService.Stub() {
            @Override
            public String message() throws RemoteException {
                return "hello, world!";
            }
        };

        return binder;
    }
}
