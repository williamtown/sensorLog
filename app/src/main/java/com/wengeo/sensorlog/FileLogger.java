package com.wengeo.sensorlog;

import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.location.GnssClock;
import android.location.GnssMeasurement;
import android.location.GnssMeasurementsEvent;
import android.location.GnssStatus;
import android.location.Location;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.SystemClock;
import android.util.Log;
import android.widget.Toast;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileFilter;
import java.io.FileWriter;
import java.io.IOException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.TimeZone;

public class FileLogger implements MeasurementListener{
    private static final String TAG = "FileLogger";
    private static final String FILE_PREFIX = "gnss_log_wen";
    private static final String ERROR_WRITING_FILE = "Problem writing to file.";
    private static final String COMMENT_START = "# ";
    private static final char RECORD_DELIMITER = ',';
    private static final String VERSION_TAG = "Version: ";

    private static final int MAX_FILES_STORED = 100;
    private static final int MINIMUM_USABLE_FILE_SIZE_BYTES = 1000;

    private final Context mContext;
    private final Object mFileLock = new Object();

    private BufferedWriter mFileWriter;
    private File mFile;

    private BufferedWriter mFileWriterRinex;
    private File mFileRinex;
    private boolean firsttime;
    private double constFullBiasNanos = 0.0;
    final float TOLERANCE_MHZ = 1e8f;


    private BufferedWriter mFileWriterSensor;
    private File mFileSensor;
    private boolean sensorStart=false;
    private boolean gnssStart=false;
    private String gpsTime4Sensor="";

    private LoggerFragment.UIFragmentComponent mUiComponent;
    public synchronized LoggerFragment.UIFragmentComponent getUiComponent() {
        return mUiComponent;
    }
    public synchronized void setUiComponent(LoggerFragment.UIFragmentComponent value) {  mUiComponent = value;  }


    public FileLogger(Context mContext) {   this.mContext = mContext;  }

    @Override
    public void onProviderEnabled(String provider) {    }

    @Override
    public void onProviderDisabled(String provider) {    }

    @Override
    public void onLocationChanged(Location location) {
        synchronized (mFileLock) {
            if (mFileWriter == null) {
                return;
            }
            String locationStream = String.format(Locale.US,"Fix,%s,%f,%f,%f,%f,%f,%d",
                            location.getProvider(),
                            location.getLatitude(),
                            location.getLongitude(),
                            location.getAltitude(),
                            location.getSpeed(),
                            location.getAccuracy(),
                            location.getTime());
            try {
                mFileWriter.write(locationStream);
                mFileWriter.newLine();
            } catch (IOException e) {
                logException(ERROR_WRITING_FILE, e);
            }
        }
    } // public void onLocationChanged

    private void logException(String errorMessage, Exception e) {
        Log.e(MeasurementProvider.TAG + TAG, errorMessage, e);
        Toast.makeText(mContext, errorMessage, Toast.LENGTH_LONG).show();
    }

    private void logError(String errorMessage) {
        Log.e(MeasurementProvider.TAG + TAG, errorMessage);
        Toast.makeText(mContext, errorMessage, Toast.LENGTH_LONG).show();
    }

    @Override
    public void onLocationStatusChanged(String provider, int status, Bundle extras) {    }

    @Override
    public void onListenerRegistration(String listener, boolean result) {    }

    @Override
    public void onGnssMeasurementsReceived(GnssMeasurementsEvent event) {
        synchronized (mFileLock) {
            if (mFileWriter == null) { return; }
            GnssClock gnssClock = event.getClock();
            for (GnssMeasurement measurement : event.getMeasurements()) {
                try {
                    /** ******************************************************* */
                    if(firsttime == true && measurement.getConstellationType() == GnssStatus.CONSTELLATION_GPS) { // && measurement.getConstellationType() == GnssStatus.CONSTELLATION_GPS
                        gnssClock = event.getClock();
                        double weekNumber = Math.floor(-(gnssClock.getFullBiasNanos() * 1e-9 / 604800));
                        double weekNumberNanos = weekNumber * 604800 * 1e9;
                        double tRxNanos = gnssClock.getTimeNanos() - gnssClock.getFullBiasNanos() - weekNumberNanos;
                        if (gnssClock.hasBiasNanos()) {  tRxNanos = tRxNanos - gnssClock.getBiasNanos(); }
                        GPSWStoGPST gpswStoGPST = new GPSWStoGPST();
                        ReturnValue value = gpswStoGPST.method(weekNumber, tRxNanos * 1e-9);
                        if (measurement.getTimeOffsetNanos() != 0) { tRxNanos = tRxNanos - measurement.getTimeOffsetNanos();  }
                        double tRxSeconds = tRxNanos * 1e-9;
                        double tTxSeconds = measurement.getReceivedSvTimeNanos() * 1e-9;
                        double prSeconds = tRxSeconds - tTxSeconds;
                        boolean iRollover = prSeconds > 604800 / 2;
                        if (iRollover) {
                            double delS = Math.round(prSeconds / 604800) * 604800;
                            double prS = prSeconds - delS;
                            double maxBiasSeconds = 10;
                            if (prS > maxBiasSeconds) {
                                Log.e("RollOver", "Rollover Error");
                                iRollover = true;
                            } else {
                                tRxSeconds = tRxSeconds - delS;
                                prSeconds = tRxSeconds - tTxSeconds;
                                iRollover = false;
                            }
                        } // if (iRollover) {

                        double prm = prSeconds * 2.99792458e8;  //
                        int rinexVer = 2; // SettingsFragment.RINEX 2
                        if (iRollover == false && prm > 0 && prSeconds < 0.5) {
                            if (rinexVer == 2) {
                                String StartTimeOBS = String.format("%6d%6d%6d%6d%6d%13.7f     %3s         TIME OF FIRST OBS\n", value.Y, value.M, value.D, value.h, value.m, value.s, "GPS");
                                String ENDOFHEADER = String.format("%73s", "END OF HEADER");
                                mFileWriterRinex.write(StartTimeOBS + ENDOFHEADER);
                                mFileWriterRinex.newLine();
                            }
                            firsttime = false;
                        } // if (iRollover == false
                    } // if(firsttime == true ) { // && measurement.getConstellationType() == GnssStatus.CONSTELLATION_GPS
                    /** ******************************************************* */
                    writeGnssMeasurementToFile(gnssClock, measurement);
                } catch (IOException e) {
                    logException(ERROR_WRITING_FILE, e);
                }
            } // for (GnssMeasurement measurement : event.getMeasurements())

            /** *********************************************************************************** **/
            StringBuilder Time = new StringBuilder();
            StringBuilder Prn = new StringBuilder();
            StringBuilder Measurements = new StringBuilder();
            String SensorStream = "";
            boolean firstOBS = true;
            int satnumber = 0;
            int rinexVer = 2; // SettingsFragment.RINEX 2
            if (firsttime == false && rinexVer == 2) {
                for (GnssMeasurement measurement : event.getMeasurements()) {
                    if (measurement.getConstellationType() == GnssStatus.CONSTELLATION_GPS) {
                        double weekNumber = Math.floor(-(gnssClock.getFullBiasNanos() * 1e-9 / 604800));
                        double weekNumberNanos = weekNumber * 604800 * 1e9;
                        double tRxNanos = gnssClock.getTimeNanos() - gnssClock.getFullBiasNanos() - weekNumberNanos;
                        if (measurement.getTimeOffsetNanos() != 0) {
                            tRxNanos = tRxNanos - measurement.getTimeOffsetNanos();
                        }
                        double tRxSeconds = tRxNanos * 1e-9;
                        double tTxSeconds = measurement.getReceivedSvTimeNanos() * 1e-9;
                        double prSeconds = tRxSeconds - tTxSeconds;
                        boolean iRollover = prSeconds > 604800 / 2;

                        if (iRollover) {
                            double delS = Math.round(prSeconds / 604800) * 604800;
                            double prS = prSeconds - delS;
                            double maxBiasSeconds = 10;
                            if (prS > maxBiasSeconds) {
                                Log.e("RollOver", "Rollover Error");
                                iRollover = true;
                            } else {
                                tRxSeconds = tRxSeconds - delS;
                                prSeconds = tRxSeconds - tTxSeconds;
                                iRollover = false;
                            }
                        } // if (iRollover) {

                        GPSWStoGPST gpswStoGPST = new GPSWStoGPST();
                        // ReturnValue value = gpswStoGPST.method(weekNumber, tRxSeconds);
                        ReturnValue value = gpswStoGPST.method(weekNumber, tRxNanos * 1e-9);
                        String DeviceName = Build.DEVICE;
                        //Log.d("DEVICE",DeviceName);
                        double prm = prSeconds * 2.99792458e8;
                        if (iRollover == false && prm > 0 && prSeconds < 0.5) {
                            if (firstOBS == true) {
                                String OBSTime = String.format(" %2d %2d %2d %2d %2d%11.7f  0", value.Y - 2000, value.M, value.D, value.h, value.m, value.s);
                                // SensorStream = String.format("%6d,%6d,%6d,%6d,%6d,%13.7f", value.Y, value.M, value.D, value.h, value.m, value.s);
                                gpsTime4Sensor = String.format("%02d:%02d:%05.3f", value.h, value.m, value.s);
                                Time.append(OBSTime);
                                firstOBS = false;
                            }
                            String prn = String.format("G%02d", measurement.getSvid());
                            satnumber = satnumber + 1;
                            Prn.append(prn);
                            String DbHz = String.format("%14.3f%s%s", measurement.getCn0DbHz(), " ", " ");
                            String PrmStrings = String.format("%14.3f%s%s", prm, " ", " ");
                            Measurements.append(PrmStrings + DbHz + "\n");
                        } //  if (iRollover == false && prm > 0 && prSeconds < 0.5)
                    } // if (rinexVer == 2 && measurement.getConstellationType() == GnssStatus.CONSTELLATION_GPS)
                } // for (GnssMeasurement measurement : event.getMeasurements())
                try {
                    Prn.insert(0, String.format("%3d", satnumber));
                    mFileWriterRinex.write(Time.toString() + Prn.toString() + "\n");
                    mFileWriterRinex.write(Measurements.toString());
                    // Log.d("*****************************************************Rinex",Measurements.toString());
                } catch (IOException e) {
                    e.printStackTrace();
                }
                gnssStart=true;
            } // if (firsttime == false && rinexVer == 2) {
        } // synchronized (mFileLock) {
    } // public void onGnssMeasurementsReceived(GnssMeasurementsEvent event)

    private void writeGnssMeasurementToFile(GnssClock clock, GnssMeasurement measurement)
            throws IOException {
        String clockStream = String.format("Raw,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s",
                        SystemClock.elapsedRealtime(),
                        clock.getTimeNanos(),
                        clock.hasLeapSecond() ? clock.getLeapSecond() : "",
                        clock.hasTimeUncertaintyNanos() ? clock.getTimeUncertaintyNanos() : "",
                        clock.getFullBiasNanos(),
                        clock.hasBiasNanos() ? clock.getBiasNanos() : "",
                        clock.hasBiasUncertaintyNanos() ? clock.getBiasUncertaintyNanos() : "",
                        clock.hasDriftNanosPerSecond() ? clock.getDriftNanosPerSecond() : "",
                        clock.hasDriftUncertaintyNanosPerSecond()
                                ? clock.getDriftUncertaintyNanosPerSecond()
                                : "",
                        clock.getHardwareClockDiscontinuityCount() + ",");
        mFileWriter.write(clockStream);

        String measurementStream = String.format("%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s",
                        measurement.getSvid(),
                        measurement.getTimeOffsetNanos(),
                        measurement.getState(),
                        measurement.getReceivedSvTimeNanos(),
                        measurement.getReceivedSvTimeUncertaintyNanos(),
                        measurement.getCn0DbHz(),
                        measurement.getPseudorangeRateMetersPerSecond(),
                        measurement.getPseudorangeRateUncertaintyMetersPerSecond(),
                        measurement.getAccumulatedDeltaRangeState(),
                        measurement.getAccumulatedDeltaRangeMeters(),
                        measurement.getAccumulatedDeltaRangeUncertaintyMeters(),
                        measurement.hasCarrierFrequencyHz() ? measurement.getCarrierFrequencyHz() : "",
                        measurement.hasCarrierCycles() ? measurement.getCarrierCycles() : "",
                        measurement.hasCarrierPhase() ? measurement.getCarrierPhase() : "",
                        measurement.hasCarrierPhaseUncertainty()
                                ? measurement.getCarrierPhaseUncertainty()
                                : "",
                        measurement.getMultipathIndicator(),
                        measurement.hasSnrInDb() ? measurement.getSnrInDb() : "",
                        measurement.getConstellationType(),
                        Build.VERSION.SDK_INT >= Build.VERSION_CODES.O
                                && measurement.hasAutomaticGainControlLevelDb()
                                ? measurement.getAutomaticGainControlLevelDb()
                                : "");
        mFileWriter.write(measurementStream);
        mFileWriter.newLine();
    } // private void writeGnssMeasurementToFile

    @Override
    public void onGnssMeasurementsStatusChanged(int status) {    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        if(sensorStart==true) {
            String SensorAcc,SensorGyr,SensorOri;
            long time=System.currentTimeMillis();
            Date date = new Date(time);
            TimeZone tz=TimeZone.getTimeZone("UTC");
            DateFormat df=new SimpleDateFormat("HH:mm:ss.SSS,");
            df.setTimeZone(tz);
            String DATE = df.format(date);
            switch (event.sensor.getType()) {
                default:  return;
                case 1: // event.sensor.getType()==Sensor.TYPE_ACCELEROMETER
                    SensorAcc = String.format(Locale.US, "%f,%f,%f", event.values[0], event.values[1], event.values[2]);
                    // Log.d("********************Sensors", SensorAcc);
                    try {
                        mFileWriterSensor.write("acc:");
                        mFileWriterSensor.write(DATE);
                        mFileWriterSensor.write(gpsTime4Sensor+",");
                        mFileWriterSensor.write(SensorAcc);
                        mFileWriterSensor.newLine();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                    return;
                case 4: // event.sensor.getType()==TYPE_GYROSCOPE
                    SensorGyr = String.format(Locale.US, "%f,%f,%f", event.values[0], event.values[1], event.values[2]);
                    try {
                        mFileWriterSensor.write("gyr:");
                        mFileWriterSensor.write(DATE);
                        mFileWriterSensor.write(gpsTime4Sensor+",");
                        mFileWriterSensor.write(SensorGyr);
                        mFileWriterSensor.newLine();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                    return;
                case 3: // event.sensor.getType()==Sensor.TYPE_ORIENTATION
                    SensorOri= String.format(Locale.US, "%f,%f,%f", event.values[0], event.values[1], event.values[2]);
                    try {
                        mFileWriterSensor.write("ori:");
                        mFileWriterSensor.write(DATE);
                        mFileWriterSensor.write(gpsTime4Sensor+",");
                        mFileWriterSensor.write(SensorOri);
                        mFileWriterSensor.newLine();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                    return;
            } // switch (event.sensor.getType())
        } // if(sensorStart==true) {
    } // public void onSensorChanged

    @Override
    public void onAccuracyChanged(Sensor sensor, int i) {

    }

    /* Start a new file logging process. */
    public void startNewLog() {
        synchronized (mFileLock) {
            File baseDirectory;
            String state = Environment.getExternalStorageState();
            if (Environment.MEDIA_MOUNTED.equals(state)) {
                baseDirectory = new File(Environment.getExternalStorageDirectory(), FILE_PREFIX);
                baseDirectory.mkdirs();
            } else if (Environment.MEDIA_MOUNTED_READ_ONLY.equals(state)) {
                logError("Cannot write to external storage.");
                return;
            } else {
                logError("Cannot read external storage.");
                return;
            }

            SimpleDateFormat formatter = new SimpleDateFormat("yyy_MM_dd_HH_mm_ss");
            Date now = new Date();
            String fileName = String.format("%s_%s.txt", FILE_PREFIX, formatter.format(now));
            // String fileName = String.format("%s_%s.txt", formatter.format(now));
            File currentFile = new File(baseDirectory, fileName);
            String currentFilePath = currentFile.getAbsolutePath();
            BufferedWriter currentFileWriter;
            try {
                currentFileWriter = new BufferedWriter(new FileWriter(currentFile));
            } catch (IOException e) {
                logException("Could not open file: " + currentFilePath, e);
                return;
            }

            // initialize the contents of the file
            try {
                currentFileWriter.write(COMMENT_START);
                currentFileWriter.newLine();
                currentFileWriter.write(COMMENT_START);
                currentFileWriter.write("Header Description:");
                currentFileWriter.newLine();
                currentFileWriter.write(COMMENT_START);
                currentFileWriter.newLine();
                currentFileWriter.write(COMMENT_START);
                currentFileWriter.write(VERSION_TAG);
                String manufacturer = Build.MANUFACTURER;
                String model = Build.MODEL;
                String fileVersion = "v2.0.0.1"
                                + " Platform: "
                                + Build.VERSION.RELEASE
                                + " "
                                + "Manufacturer: "
                                + manufacturer
                                + " "
                                + "Model: "
                                + model;
                currentFileWriter.write(fileVersion);
                currentFileWriter.newLine();
                currentFileWriter.write(COMMENT_START);
                currentFileWriter.newLine();
                currentFileWriter.write(COMMENT_START);
                currentFileWriter.write(
                        "Raw,ElapsedRealtimeMillis,TimeNanos,LeapSecond,TimeUncertaintyNanos,FullBiasNanos,"
                                + "BiasNanos,BiasUncertaintyNanos,DriftNanosPerSecond,DriftUncertaintyNanosPerSecond,"
                                + "HardwareClockDiscontinuityCount,Svid,TimeOffsetNanos,State,ReceivedSvTimeNanos,"
                                + "ReceivedSvTimeUncertaintyNanos,Cn0DbHz,PseudorangeRateMetersPerSecond,"
                                + "PseudorangeRateUncertaintyMetersPerSecond,"
                                + "AccumulatedDeltaRangeState,AccumulatedDeltaRangeMeters,"
                                + "AccumulatedDeltaRangeUncertaintyMeters,CarrierFrequencyHz,CarrierCycles,"
                                + "CarrierPhase,CarrierPhaseUncertainty,MultipathIndicator,SnrInDb,"
                                + "ConstellationType,AgcDb");
                currentFileWriter.newLine();
                currentFileWriter.write(COMMENT_START);
                currentFileWriter.newLine();
                currentFileWriter.write(COMMENT_START);
                currentFileWriter.write("Fix,Provider,Latitude,Longitude,Altitude,Speed,Accuracy,(UTC)TimeInMs");
                currentFileWriter.newLine();
                currentFileWriter.write(COMMENT_START);
                currentFileWriter.newLine();
                currentFileWriter.write(COMMENT_START);
                currentFileWriter.write("Nav,Svid,Type,Status,MessageId,Sub-messageId,Data(Bytes)");
                currentFileWriter.newLine();
                currentFileWriter.write(COMMENT_START);
                currentFileWriter.newLine();
            } catch (IOException e) {
                logException("Count not initialize file: " + currentFilePath, e);
                return;
            }

            if (mFileWriter != null) {
                try {
                    mFileWriter.close();
                } catch (IOException e) {
                    logException("Unable to close all file streams.", e);
                    return;
                }
            }

            mFile = currentFile;
            mFileWriter = currentFileWriter;
            Toast.makeText(mContext, "File opened: " + currentFilePath, Toast.LENGTH_SHORT).show();

            // To make sure that files do not fill up the external storage:  // - Remove all empty files
            FileFilter filter = new FileToDeleteFilter(mFile);
            for (File existingFile : baseDirectory.listFiles(filter)) { existingFile.delete(); }
            // - Trim the number of files with data
            File[] existingFiles = baseDirectory.listFiles();
            int filesToDeleteCount = existingFiles.length - MAX_FILES_STORED;
            if (filesToDeleteCount > 0) {
                Arrays.sort(existingFiles);
                for (int i = 0; i < filesToDeleteCount; ++i) {
                    existingFiles[i].delete();
                }
            }

            // start Rinex File
            /** **************************************************************** **/
            String fileNameRinex = String.format("%s_%s.obs",FILE_PREFIX, formatter.format(now));
            File currentFileRinex = new File(baseDirectory, fileNameRinex);
            String currentFilePathRinex = currentFileRinex.getAbsolutePath();
            BufferedWriter currentFileWriterRinex;
            try {
                currentFileWriterRinex = new BufferedWriter(new FileWriter(currentFileRinex));
            } catch (IOException e) {
                logException("Could not open file: " + currentFilePathRinex, e);
                return;
            }

            // initialize the contents of the file
            int rinexVer=2; // SettingsFragment.RINEX 2
            try {
                if(rinexVer==2){
                    currentFileWriterRinex.write("     2.11           OBSERVATION DATA    G (GPS)             RINEX VERSION / TYPE");
                    currentFileWriterRinex.newLine();
                    String PGM = String.format("%-20s", "Gnsslog+sensor");
                    String RUNBY = String.format("%-20s", "Wenlin Yan");
                    long time=System.currentTimeMillis();
                    Date date = new Date(time);
                    TimeZone tz=TimeZone.getTimeZone("UTC");
                    DateFormat df=new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
                    df.setTimeZone(tz);
                    String DATE = df.format(date);
                    // String DATE = String.format("%-20s", now.getTime());
                    currentFileWriterRinex.write(PGM + RUNBY + DATE + " PGM / RUN BY / DATE");
                    currentFileWriterRinex.newLine();
                    //COMMENT
                    //String COMMENT = String.format("%-60s","Android Ver7.0 Nougat");
                    //currentFileWriter.write( COMMENT +  "COMMENT");
                    //currentFileWriter.newLine();
                    //MARKER NAME
                    String MARKERNAME = String.format("%-60s", Build.DEVICE);
                    currentFileWriterRinex.write(MARKERNAME + "MARKER NAME");
                    currentFileWriterRinex.newLine();
                    //MARKER NUMBER
                    //OBSERVER AGENCY
                    String OBSERVER = String.format("%-20s", "observer");
                    String AGENCY = String.format("%-40s", "agency");
                    currentFileWriterRinex.write(OBSERVER + AGENCY + "OBSERVER / AGENCY");
                    currentFileWriterRinex.newLine();
                    //REC TYPE VERS
                    String REC = String.format("%-20s", "0");
                    String TYPE = String.format("%-20s", "****");
                    String VERS = String.format("%-20s", Build.VERSION.BASE_OS);
                    currentFileWriterRinex.write(REC + TYPE + VERS + "REC # / TYPE / VERS");
                    currentFileWriterRinex.newLine();
                    //ANT TYPE
                    String ANT = String.format("%-20s", "0");
                    String ANTTYPE = String.format("%-40s", "****");
                    currentFileWriterRinex.write(ANT + ANTTYPE + "ANT # / TYPE");
                    currentFileWriterRinex.newLine();
                    //APPROX POSITION XYZ
                    String X = String.format("%14.4f", 0.0);
                    String Y = String.format("%14.4f", 0.0);
                    String Z = String.format("%14.4f", 0.0);
                    currentFileWriterRinex.write(X + Y + Z + "                  " + "APPROX POSITION XYZ");
                    currentFileWriterRinex.newLine();
                    //ANTENNA: DELTA H/E/N
                    String H = String.format("%14.4f", 0.0);
                    String E = String.format("%14.4f", 0.0);
                    String N = String.format("%14.4f", 0.0);
                    currentFileWriterRinex.write(H + E + N + "                  " + "ANTENNA: DELTA H/E/N");
                    currentFileWriterRinex.newLine();
                    //WAVELENGTH FACT L1/2
                    String WAVELENGTH = String.format("%-6d%-54d", 1, 0);
                    currentFileWriterRinex.write(WAVELENGTH + "WAVELENGTH FACT L1/2");
                    currentFileWriterRinex.newLine();
                    //# / TYPES OF OBSERV
                    // SettingsFragment.CarrierPhase
                    int CarrierPhase=1;
                    if (CarrierPhase==1) {
                        String NUMBEROFOBS = String.format("%-6d", 6);
                        String OBSERV = String.format("%-54s", "    L1    C1    S1    L5    C5    S5");
                        currentFileWriterRinex.write(NUMBEROFOBS + OBSERV + "# / TYPES OF OBSERV");
                        currentFileWriterRinex.newLine();
                    } else {
                        String NUMBEROFOBS = String.format("%-6d", 2);
                        String OBSERV = String.format("%-54s", "    C1    S1");
                        currentFileWriterRinex.write(NUMBEROFOBS + OBSERV + "# / TYPES OF OBSERV");
                        currentFileWriterRinex.newLine();
                    }
                    //INTERVAL
                    String INTERVAL = String.format("%-60.3f", 1.0);
                    currentFileWriterRinex.write(INTERVAL + "INTERVAL");
                    currentFileWriterRinex.newLine();
                    // currentFileWriterRinex.flush();  //***************************
                } //  if(rinexVer==2)  //**************************************************************
                firsttime = true;
//                localintervaltime = SettingsFragment.interval;
            } catch (IOException e) {
                Toast.makeText(mContext, "Count not initialize observation file", Toast.LENGTH_SHORT).show();
                logException("Count not initialize file: " + currentFilePathRinex, e);
                return;
            } // try

            if (mFileWriterRinex != null) {
                try {
                    mFileWriterRinex.close();
                } catch (IOException e) {
                    logException("Unable to close all file streams.", e);
                    return;
                }
            }

            mFileRinex = currentFileRinex;
            mFileWriterRinex = currentFileWriterRinex;
            Toast.makeText(mContext, "File opened: " + currentFilePathRinex, Toast.LENGTH_SHORT).show();
            /** **************************************************************** **/
            // Rinex File




            // Sensors File
            /** **************************************************************** **/
            String fileNameSensor = String.format("%s_%s.sns",FILE_PREFIX, formatter.format(now));
            File currentFileSensor = new File(baseDirectory, fileNameSensor);
            String currentFilePathSensor = currentFileSensor.getAbsolutePath();
            BufferedWriter currentFileWriterSensor;
            try {
                currentFileWriterSensor = new BufferedWriter(new FileWriter(currentFileSensor));
                currentFileWriterSensor.write("#time_phone(UTC), time_GPS, accx,accy,accz(m/s^2), gyrx,gyry,gyrz(deg/rad), orix,oriy,oriz(deg)");
                currentFileWriterSensor.newLine();
            } catch (IOException e) {
                logException("Could not open file: " + currentFilePathSensor, e);
                return;
            }

            mFileSensor = currentFileSensor;
            mFileWriterSensor = currentFileWriterSensor;
            Toast.makeText(mContext, "File opened: " + currentFilePathSensor, Toast.LENGTH_SHORT).show();
            sensorStart=true;
            /** **************************************************************** **/
            // Sensors File
        } // synchronized (mFileLock)
    } // public void startNewLog()


    /*Send the current log via email or other options selected from a pop menu shown to the user.
  A new log is started when calling this function.  */
    public void send() {
        if (mFile == null) { return; }
        if (mFileWriter != null) {
            try {
                mFileWriter.flush();
                mFileWriter.close();
                mFileWriter = null;
                mFileWriterRinex.flush();
                mFileWriterRinex.close();
                mFileWriterRinex = null;
                mFileWriterSensor.flush();
                mFileWriterSensor.close();
                mFileWriterSensor = null;
            } catch (IOException e) {
                logException("Unable to close all file streams.", e);
                return;
            }
        }
    }// public void send()


    /** * Implements a {@link FileFilter} to delete files that are not in the {@link FileToDeleteFilter#mRetainedFiles}. */
    private static class FileToDeleteFilter implements FileFilter {
        private final List<File> mRetainedFiles;

        public FileToDeleteFilter(File... retainedFiles) {
            this.mRetainedFiles = Arrays.asList(retainedFiles);
        }

        /* Returns {@code true} to delete the file, and {@code false} to keep the file.
         <p>Files are deleted if they are not in the {@link FileToDeleteFilter#mRetainedFiles} list.*/
        @Override
        public boolean accept(File pathname) {
            if (pathname == null || !pathname.exists()) {
                return false;
            }
            if (mRetainedFiles.contains(pathname)) {
                return false;
            }
            return pathname.length() < MINIMUM_USABLE_FILE_SIZE_BYTES;
        }
    } // private static class FileToDeleteFilter implements FileFilter


    public static class ReturnValue {
        public int Y;
        public int M;
        public int D;
        public int h;
        public int m;
        public double s;
    }

    public static class GPSWStoGPST {
        public ReturnValue method(double GPSW , double GPSWS) {
            ReturnValue value = new ReturnValue();
            //MJD
            double MD = (int)(GPSWS/86400);
            double MJD = 44244+GPSW*7+MD;
            //ymd
            double JD = MJD + 2400000.5;
            double N = JD + 0.5;
            int Z = (int)N;
            double F = N - Z;
            double A;
            if(Z >= 2299161){
                int X = (int)((Z-1867216.25)/36524.25);
                A = Z + 1 + X - X/4;
            }
            else {
                A = Z;
            }
            double B = A + 1524;
            int C = (int)((B-122.1)/365.25);
            int K = (int)(365.25*C);
            int E = (int)((B-K)/30.6001);
            double D = B-K-(int)(30.6001*E)+F;
            int M;
            int Y;
            if(E < 13.5){
                M = E - 1;
            }
            else {
                M = E - 13;
            }
            if(M > 2.5){
                Y = C - 4716;
            }
            else{
                Y = C - 4715;
            }
            value.Y = Y;
            value.M = M;
            value.D = (int)D;

            //GPSweektoGPStime
            double DS = GPSWS-MD*86400;
            int h = (int)(DS/3600);
            double hm = DS-h*3600;
            int m = (int)(hm/60);
            double s = hm - m * 60;

            value.h = h;
            value.m = m;
            value.s = s;
            return value;
        }
    } // public static class GPSWStoGPST

} // public class FileLogger implements MeasurementListener
