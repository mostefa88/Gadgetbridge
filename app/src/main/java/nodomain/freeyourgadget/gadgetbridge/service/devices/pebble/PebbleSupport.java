package nodomain.freeyourgadget.gadgetbridge.service.devices.pebble;

import android.net.Uri;
import android.util.Pair;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.UUID;

import nodomain.freeyourgadget.gadgetbridge.impl.GBDevice;
import nodomain.freeyourgadget.gadgetbridge.model.Alarm;
import nodomain.freeyourgadget.gadgetbridge.model.NotificationSpec;
import nodomain.freeyourgadget.gadgetbridge.model.ServiceCommand;
import nodomain.freeyourgadget.gadgetbridge.service.serial.AbstractSerialDeviceSupport;
import nodomain.freeyourgadget.gadgetbridge.service.serial.GBDeviceIoThread;
import nodomain.freeyourgadget.gadgetbridge.service.serial.GBDeviceProtocol;

public class PebbleSupport extends AbstractSerialDeviceSupport {

    @Override
    public boolean connect() {
        getDeviceIOThread().start();
        return true;
    }

    @Override
    protected GBDeviceProtocol createDeviceProtocol() {
        return new PebbleProtocol();
    }

    @Override
    protected GBDeviceIoThread createDeviceIOThread() {
        return new PebbleIoThread(PebbleSupport.this, getDevice(), getDeviceProtocol(), getBluetoothAdapter(), getContext());
    }

    @Override
    public boolean useAutoConnect() {
        return true;
    }

    @Override
    public void onInstallApp(Uri uri) {
        getDeviceIOThread().installApp(uri, 0);
    }

    @Override
    public void onAppConfiguration(UUID uuid, String config) {
        try {
            ArrayList<Pair<Integer, Object>> pairs = new ArrayList<>();

            JSONObject json = new JSONObject(config);
            Iterator<String> keysIterator = json.keys();
            while (keysIterator.hasNext()) {
                String keyStr = keysIterator.next();
                Object object = json.get(keyStr);
                pairs.add(new Pair<>(Integer.parseInt(keyStr), object));
            }
            getDeviceIOThread().write(((PebbleProtocol) getDeviceProtocol()).encodeApplicationMessagePush(PebbleProtocol.ENDPOINT_APPLICATIONMESSAGE, uuid, pairs));
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onHeartRateTest() {

    }

    @Override
    public synchronized PebbleIoThread getDeviceIOThread() {
        return (PebbleIoThread) super.getDeviceIOThread();
    }

    private boolean reconnect() {
        if (!isConnected() && useAutoConnect()) {
            if (getDevice().getState() == GBDevice.State.WAITING_FOR_RECONNECT) {
                gbDeviceIOThread.interrupt();
                gbDeviceIOThread = null;
                if (!connect()) {
                    return false;
                }
                try {
                    Thread.sleep(2000); // this is about the time the connect takes, so the notification can come though
                } catch (InterruptedException ignored) {
                }
            }
        }
        return true;
    }

    @Override
    public void onNotification(NotificationSpec notificationSpec) {
        if (reconnect()) {
            super.onNotification(notificationSpec);
        }
    }

    @Override
    public void onSetCallState(String number, String name, ServiceCommand command) {
        if (reconnect()) {
            super.onSetCallState(number, name, command);
        }
    }

    @Override
    public void onSetMusicInfo(String artist, String album, String track, int duration, int trackCount, int trackNr) {
        if (reconnect()) {
            super.onSetMusicInfo(artist, album, track, duration, trackCount, trackNr);
        }
    }


    @Override
    public void onSetAlarms(ArrayList<? extends Alarm> alarms) {
        //nothing to do ATM
    }
}
