package com.wengeo.sensorlog;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.text.Editable;
import android.text.SpannableStringBuilder;
import android.text.style.ForegroundColorSpan;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

public class LoggerFragment extends Fragment {
    TextView mLogView;
    ScrollView mScrollView;
    private UiLogger mUiLogger;
    private FileLogger mFileLogger;
    private Button mStartLog;
    private Button mSendFile;

    private final UIFragmentComponent mUiComponent = new UIFragmentComponent();

    private void enableOptions(boolean start) {
        // mTimer.setEnabled(start);
        mStartLog.setEnabled(start);
        mSendFile.setEnabled(!start);
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View newView=inflater.inflate(R.layout.fragment_log,container,false);
        mLogView = (TextView) newView.findViewById(R.id.log_view);
        mScrollView = (ScrollView) newView.findViewById(R.id.log_scroll);

        UiLogger currentUiLogger = mUiLogger; // display data at ScrollView
        if (currentUiLogger != null) { currentUiLogger.setUiFragmentComponent(mUiComponent); }

        FileLogger currentFileLogger = mFileLogger;
        if (currentFileLogger != null) { currentFileLogger.setUiComponent(mUiComponent);   }

        mStartLog = (Button) newView.findViewById(R.id.start_logs);
        mStartLog.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                enableOptions(false /* start */);
                Toast.makeText(getContext(), "Starting log...", Toast.LENGTH_LONG).show();
                mFileLogger.startNewLog();
            }
        });

        mSendFile = (Button) newView.findViewById(R.id.send_file);
        mSendFile.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                enableOptions(true /* start */);
                Toast.makeText(getContext(), "stop_message", Toast.LENGTH_LONG).show();
                mFileLogger.send(); // stopAndSend();
            }
        });

        return newView;
    }

    public void setUILogger(UiLogger value) {  mUiLogger = value;  }
    public void setFileLogger(FileLogger value) {
        mFileLogger = value;
    }

    /* A facade for UI and Activity related operations that are required for {@link MeasurementListener}s. */
    public class UIFragmentComponent {
        private static final int MAX_LENGTH = 42000;
        private static final int LOWER_THRESHOLD = (int) (MAX_LENGTH * 0.5);

        public synchronized void logTextFragment(final String tag, final String text, int color) {
            final SpannableStringBuilder builder = new SpannableStringBuilder();
            builder.append(tag).append(" | ").append(text).append("\n");
            builder.setSpan(
                    new ForegroundColorSpan(color),
                    0 /* start */,
                    builder.length(),
                    SpannableStringBuilder.SPAN_INCLUSIVE_EXCLUSIVE);

            Activity activity = getActivity();
            if (activity == null) {return; }

            activity.runOnUiThread(
                    new Runnable() {
                        @Override
                        public void run() {
                            mLogView.append(builder);
                            SharedPreferences sharedPreferences = PreferenceManager.
                                    getDefaultSharedPreferences(getActivity());
                            Editable editable = mLogView.getEditableText();
                            int length = editable.length();
                            if (length > MAX_LENGTH) {
                                editable.delete(0, length - LOWER_THRESHOLD);
                            }
                            if (sharedPreferences.getBoolean(SettingsFragment.PREFERENCE_KEY_AUTO_SCROLL,
                                    false /*default return value*/)){
                                mScrollView.post(new Runnable() {
                                    @Override
                                    public void run() {
                                        mScrollView.fullScroll(View.FOCUS_DOWN);
                                    }
                                });
                            }
                        }
                    });
        }

        public void startActivity(Intent intent) {
            getActivity().startActivity(intent);
        }
    } // public class UIFragmentComponent
}
