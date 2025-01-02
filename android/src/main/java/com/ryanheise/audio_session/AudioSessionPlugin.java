package com.ryanheise.audio_session;

import androidx.annotation.NonNull;
import io.flutter.embedding.engine.plugins.FlutterPlugin;
import io.flutter.plugin.common.BinaryMessenger;
import io.flutter.plugin.common.MethodCall;
import io.flutter.plugin.common.MethodChannel;
import io.flutter.plugin.common.MethodChannel.MethodCallHandler;
import io.flutter.plugin.common.MethodChannel.Result;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/** AudioSessionPlugin */
public class AudioSessionPlugin implements FlutterPlugin, MethodCallHandler {
    private static volatile Map<?, ?> configuration;
    private static final List<AudioSessionPlugin> instances = 
        Collections.synchronizedList(new ArrayList<>());
    
    private MethodChannel channel;
    private AndroidAudioManager androidAudioManager;

    @Override
    public void onAttachedToEngine(@NonNull FlutterPluginBinding flutterPluginBinding) {
        BinaryMessenger messenger = flutterPluginBinding.getBinaryMessenger();
        channel = new MethodChannel(messenger, "com.ryanheise.audio_session");
        channel.setMethodCallHandler(this);
        
        // Use factory method instead of constructor
        androidAudioManager = AndroidAudioManager.create(
            flutterPluginBinding.getApplicationContext(), 
            messenger
        );
        
        synchronized (instances) {
            instances.add(this);
        }
    }

    @Override
    public void onDetachedFromEngine(@NonNull FlutterPluginBinding binding) {
        if (channel != null) {
            channel.setMethodCallHandler(null);
            channel = null;
        }
        if (androidAudioManager != null) {
            androidAudioManager.dispose();
            androidAudioManager = null;
        }
        synchronized (instances) {
            instances.remove(this);
        }
    }

    @Override
    public void onMethodCall(@NonNull final MethodCall call, @NonNull final Result result) {
        List<?> args = (List<?>)call.arguments;
        switch (call.method) {
            case "setConfiguration": {
                configuration = (Map<?, ?>)args.get(0);
                result.success(null);
                invokeMethod("onConfigurationChanged", configuration);
                break;
            }
            case "getConfiguration": {
                result.success(configuration);
                break;
            }
            default:
                result.notImplemented();
                break;
        }
    }

    private void invokeMethod(String method, Object... args) {
        synchronized (instances) {
            for (AudioSessionPlugin instance : instances) {
                if (instance.channel != null) {
                    ArrayList<Object> list = new ArrayList<Object>(Arrays.asList(args));
                    instance.channel.invokeMethod(method, list);
                }
            }
        }
    }
}