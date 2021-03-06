package br.com.ferreiraz.fullcamera;

import android.annotation.TargetApi;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.hardware.Camera;
import android.media.CamcorderProfile;
import android.media.MediaPlayer;
import android.media.MediaRecorder;
import android.net.Uri;
import android.os.*;
import android.support.v4.app.FragmentTabHost;
import android.util.Log;
import android.view.*;
import android.widget.*;

import java.io.*;
import java.nio.channels.FileChannel;
import java.text.SimpleDateFormat;
import java.util.*;

import com.coremedia.iso.boxes.Container;
import com.googlecode.mp4parser.authoring.Movie;
import com.googlecode.mp4parser.authoring.Track;
import com.googlecode.mp4parser.authoring.builder.DefaultMp4Builder;
import com.googlecode.mp4parser.authoring.container.mp4.MovieCreator;
import com.googlecode.mp4parser.authoring.tracks.AppendTrack;
import com.learnncode.mediachooser.activity.HomeFragmentActivity;
import com.learnncode.mediachooser.fragment.ImageFragment;
import com.learnncode.mediachooser.fragment.VideoFragment;

import us.feras.ecogallery.EcoGallery;
import us.feras.ecogallery.EcoGalleryAdapterView;

@SuppressWarnings("deprecation")
public class FullCameraActivity extends HomeFragmentActivity {

    public static final String BUTTON_SOURCE_GALLERY = "BUTTON_SOURCE_GALLERY";
    public static final String BUTTON_SOURCE_PHOTO = "BUTTON_SOURCE_PHOTO";
    public static final String BUTTON_SOURCE_VIDEO = "BUTTON_SOURCE_VIDEO";
    private static final String TAG = "FULL_CAM";


    @SuppressWarnings("deprecation")
    private Camera mCamera;
    private SurfaceView mPreview;
    private MediaRecorder mMediaRecorder;
    private boolean isRecording = false;
    static private int cameraId = 0;

    private SurfaceHolder               surfaceHolder;

    protected Activity                  self;
    private View.OnClickListener        onClickListener;
    private int                         currentFlashMode = -1;
    private EcoGallery                  pagerSources;
    private SourcesAdapter              pagerSourcesAdapter;
    private EcoGallery                  pagerPhotos;
    private PhotosAdapter               pagerPhotosAdapter;
    private ArrayList<ResultClass>      pagerPhotosItems;

    private File                        tempVideoFile;
    private ArrayList<ResultFile>       videosItems = new ArrayList<ResultFile>();

    private File                        galleryVideo;

    private ProgressBarAnimation        progressAnimation;
    private ProgressBar                 progressBar;
    private ProgressDialog              progressDialog;

    ImageButton buttonSwitchCamera;
    ImageButton buttonBack;
    ImageButton buttonNext;
    ImageButton buttonCancel;

    ImageButton buttonFlashOn;
    ImageButton buttonFlashAuto;
    ImageButton buttonFlashOff;

    ImageButton buttonSourceGallery;
    ImageButton buttonSourcePhoto;
    ImageButton buttonSourceVideo;

    //Parameters

    private boolean                     shouldSaveOnGallery     = true;
    private int                         imageBox                = 720;
    private int                         imageCompression        = 100;

    private static int                  maxVideoDuration        = 30; //in seconds
    private int                         maxPhotoCount           = 5;
    private int                         totalVideoDuration      = 0;

    private String                      stringOk                = "OK";
    private String                      stringCancel            = "Cancel";
    private String                      stringMaxPhotos         = "You are limited to X photos.";
    private String                      stringDeletePhoto       = "Are you sure that you want to delete this photo?";
    private String                      stringDeleteAllPhotos   = "This will cause all photos to be removed. Are you sure?";
    private String                      stringDeleteVideo       = "Are you sure that you want to delete this video?";
    private String                      stringDeleteAllVideos   = "This will cause your video to be removed. Are you sure?";
    private String                      stringProcessingVideos  = "Processing video";
    private String                      stringAppFolder         = "fullcam";

    private String getString(Bundle bundle, String key, String defaultValue){
        if(bundle != null)
            if(bundle.containsKey(key))
                return bundle.getString(key);
        return defaultValue;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        if(savedInstanceState == null)
            savedInstanceState = new Bundle();
        super.onCreate(savedInstanceState);

        //TODO Fix rotation issues
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_NOSENSOR);
        self = this;

        setupOptionsFC(savedInstanceState);

        setupReferencesFC();

        initPagerSources();

        initPagerPhotos();

//        startCamera();


    }

    private void setupReferencesFC() {
        buttonSwitchCamera  = (ImageButton) findViewById(R.id.switchCamera);
        buttonBack          = (ImageButton) findViewById(R.id.back);
        buttonNext          = (ImageButton) findViewById(R.id.next);
        buttonCancel        = (ImageButton) findViewById(R.id.cancel);

        buttonFlashOn       = (ImageButton) findViewById(R.id.flashOn);
        buttonFlashAuto     = (ImageButton) findViewById(R.id.flashAuto);
        buttonFlashOff      = (ImageButton) findViewById(R.id.flashOff);

        progressBar         = (ProgressBar) findViewById(R.id.progressBar);

        if(buttonSourceGallery == null) {
            buttonSourceGallery = new ImageButton(self);
            buttonSourcePhoto = new ImageButton(self);
            buttonSourceVideo = new ImageButton(self);

            initOnClickListener();
        }
    }

    private void setupOptionsFC(Bundle savedInstanceState) {
        stringOk                = getString(savedInstanceState, "stringOk" , stringOk);
        stringCancel            = getString(savedInstanceState, "stringCancel" , stringCancel);
        stringMaxPhotos         = getString(savedInstanceState, "stringMaxPhotos" , stringMaxPhotos);
        stringDeletePhoto       = getString(savedInstanceState, "stringDeletePhoto" , stringDeletePhoto);
        stringDeleteAllPhotos   = getString(savedInstanceState, "stringDeleteAllPhotos" , stringDeleteAllPhotos);
        stringDeleteVideo       = getString(savedInstanceState, "stringDeleteVideo" , stringDeleteVideo);
        stringDeleteAllVideos   = getString(savedInstanceState, "stringDeleteAllVideos" , stringDeleteAllVideos );
        stringProcessingVideos  = getString(savedInstanceState, "stringProcessingVideos" , stringProcessingVideos);
        stringAppFolder         = getString(savedInstanceState, "stringAppFolder" , stringAppFolder);

        maxVideoDuration        = savedInstanceState.getInt("maxVideoDuration", maxVideoDuration);
        maxPhotoCount           = savedInstanceState.getInt("maxPhotoCount", maxPhotoCount);
        imageBox                = savedInstanceState.getInt("imageBox", 1200);
        imageCompression        = savedInstanceState.getInt("imageCompression", 100);

        shouldSaveOnGallery     = savedInstanceState.getBoolean("shouldSaveOnGallery", shouldSaveOnGallery);
    }

    private void initOnClickListener() {
        if(onClickListener == null) {
            onClickListener = new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    onClickProxy((ImageButton) v);
                }
            };
            buttonSwitchCamera.setOnClickListener(onClickListener);
            buttonBack.setOnClickListener(onClickListener);
            buttonNext.setOnClickListener(onClickListener);
            buttonCancel.setOnClickListener(onClickListener);

            buttonSourceGallery.setOnClickListener(onClickListener);
            buttonSourcePhoto.setOnClickListener(onClickListener);
            buttonSourceVideo.setOnClickListener(onClickListener);

            buttonFlashOn.setOnClickListener(onClickListener);
            buttonFlashAuto.setOnClickListener(onClickListener);
            buttonFlashOff.setOnClickListener(onClickListener);
        }
    }

    private void initPagerPhotos() {
        pagerPhotos = (EcoGallery) findViewById(R.id.pagerPhotos);
        if(pagerPhotosItems == null) {
            pagerPhotosItems = new ArrayList<ResultClass>();
            pagerPhotosAdapter = new PhotosAdapter(this, pagerPhotosItems);
            pagerPhotos.setAdapter(pagerPhotosAdapter);
            pagerPhotos.setSpacing(pagerPhotos.getHeight() / 10);
            pagerPhotos.setOnItemClickListener(new EcoGalleryAdapterView.OnItemClickListener() {
                @Override
                public void onItemClick(EcoGalleryAdapterView<?> parent, View view, final int position, long id) {
                    if(pagerPhotos.getSelectedItemPosition() == position) {
                        AlertDialog dialog = new AlertDialog.Builder(self).create();
                        dialog.setMessage(stringDeletePhoto);
                        dialog.setCancelable(false);
                        dialog.setButton(DialogInterface.BUTTON_POSITIVE, stringOk, new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int buttonId) {
                                dialog.dismiss();
                                ResultClass result = pagerPhotosItems.remove(position);
                                if(!shouldSaveOnGallery) {
                                    if(!result.getFile().delete())
                                        result.getFile().deleteOnExit();
                                }
                                pagerPhotosAdapter.notifyDataSetChanged();
                                pagerPhotos.setSelection(pagerPhotos.getCount() / 2, true);
                            }
                        });

                        dialog.setButton(DialogInterface.BUTTON_NEGATIVE, stringCancel, new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int buttonId) {
                                dialog.dismiss();
                            }
                        });
                        dialog.setIcon(android.R.drawable.ic_dialog_alert);
                        dialog.show();
                    }
                }
            });
        }
    }

    private void initPagerSources() {
        pagerSources = (EcoGallery) findViewById(R.id.pagerSources);
        if(pagerSourcesAdapter == null) {
            ArrayList<ImageButton> items = new ArrayList<ImageButton>();
            setBackgroundOn(buttonSourceGallery, R.drawable.fullcamgallerystyle);
            buttonSourceGallery.setId(R.id.source_gallery);
            buttonSourceGallery.setTag(BUTTON_SOURCE_GALLERY);
            items.add(buttonSourceGallery);

            setBackgroundOn(buttonSourcePhoto, R.drawable.fullcamphotostyle);
            buttonSourcePhoto.setId(R.id.source_photo);
            buttonSourcePhoto.setTag(BUTTON_SOURCE_PHOTO);
            items.add(buttonSourcePhoto);

            setBackgroundOn(buttonSourceVideo, R.drawable.fullcamvideostyle);
            buttonSourceVideo.setId(R.id.source_video);
            buttonSourceVideo.setTag(BUTTON_SOURCE_VIDEO);
            items.add(buttonSourceVideo);

            pagerSourcesAdapter = new SourcesAdapter(this, items);
            pagerSources.setSpacing(30);
            pagerSources.setOnItemSelectedListener(new EcoGalleryAdapterView.OnItemSelectedListener() {
                @Override
                public void onItemSelected(EcoGalleryAdapterView<?> parent, View view, int position, long id) {
                    if(isRecording) {
                        pagerSources.setSelection(pagerPhotos.getOldPosition());
                        return;
                    }
                    if(pagerPhotosItems.size() > 0 && view.getTag() != BUTTON_SOURCE_PHOTO) {
                        AlertDialog dialog = new AlertDialog.Builder(self).create();
                        dialog.setMessage(stringDeleteAllPhotos);
                        dialog.setCancelable(false);
                        dialog.setButton(DialogInterface.BUTTON_POSITIVE, stringOk, new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int buttonId) {
                                dialog.dismiss();
                                pagerPhotosItems.clear();
                                pagerPhotosAdapter.notifyDataSetChanged();
                                pagerPhotos.setVisibility(View.GONE);
                            }
                        });
                        dialog.setButton(DialogInterface.BUTTON_NEGATIVE, stringCancel, new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int buttonId) {
                                dialog.dismiss();
                                pagerSources.setSelection(pagerPhotos.getOldPosition());
                            }
                        });
                        dialog.setIcon(android.R.drawable.ic_dialog_alert);
                        dialog.show();
                    }

                    if(videosItems.size() > 0 && view.getTag() != BUTTON_SOURCE_VIDEO) {
                        AlertDialog dialog = new AlertDialog.Builder(self).create();
                        dialog.setMessage(stringDeleteAllVideos);
                        dialog.setCancelable(false);
                        dialog.setButton(DialogInterface.BUTTON_POSITIVE, stringOk, new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int buttonId) {
                                dialog.dismiss();
                                dropVideos(true);
                            }
                        });
                        dialog.setButton(DialogInterface.BUTTON_NEGATIVE, stringCancel, new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int buttonId) {
                                dialog.dismiss();
                                pagerSources.setSelection(pagerPhotos.getOldPosition());
                            }
                        });
                        dialog.setIcon(android.R.drawable.ic_dialog_alert);
                        dialog.show();
                    }

                    if (view == null)
                        return;

                    if (view.getTag() == BUTTON_SOURCE_GALLERY) {
                        buttonSourceGallery.setClickable(true);
                        buttonSourceGallery.setEnabled(true);

                        buttonSourcePhoto.setClickable(false);
                        buttonSourcePhoto.setEnabled(false);

                        buttonSourceVideo.setClickable(false);
                        buttonSourceVideo.setEnabled(false);
                    } else if (view.getTag() == BUTTON_SOURCE_PHOTO) {
                        buttonSourceGallery.setClickable(false);
                        buttonSourceGallery.setEnabled(false);

                        buttonSourcePhoto.setClickable(true);
                        buttonSourcePhoto.setEnabled(true);

                        buttonSourceVideo.setClickable(false);
                        buttonSourceVideo.setEnabled(false);
                    } else if (view.getTag() == BUTTON_SOURCE_VIDEO) {
                        buttonSourceGallery.setClickable(false);
                        buttonSourceGallery.setEnabled(false);

                        buttonSourcePhoto.setClickable(false);
                        buttonSourcePhoto.setEnabled(false);

                        buttonSourceVideo.setClickable(true);
                        buttonSourceVideo.setEnabled(true);
                    }
                }

                @Override
                public void onNothingSelected(EcoGalleryAdapterView<?> parent) {

                }
            });
            pagerSources.setAdapter(pagerSourcesAdapter);
            pagerSources.setSelection(1);
        }
    }

    protected void stopCamera() {
        if(mCamera != null) {
            mCamera.stopPreview();
            mCamera.setPreviewCallback(null);
            mCamera.release();
            mCamera = null;
        }
        if(mPreview != null) {
            surfaceHolder = mPreview.getHolder();
            if(surfaceHolder != null) {
                surfaceHolder = null;
            }
            mPreview = null;
        }
    }

    protected void switchCameras() {
        cameraId++;
        if(cameraId >= Camera.getNumberOfCameras()) {
            cameraId = 0;
        }
        stopCamera();
        startCamera();

/*        if(camera != null) {
            camera.stopPreview();
            camera.setPreviewCallback(null);
            camera.release();
            camera = null;
        }
        if(surfaceHolder != null) {
            surfaceHolder.removeCallback(surfaceHolderCallback);
            surfaceHolder = null;
        }

        if(mPreview != null) {
            baseLayout.removeView(surfaceView);
        }

        currentCameraId++;
        if(currentCameraId >= Camera.getNumberOfCameras()) {
            currentCameraId = 0;
        }
        cameraId = currentCameraId;

        surfaceView = new SurfaceView(this);
        RelativeLayout.LayoutParams layoutParams = new RelativeLayout.LayoutParams(RelativeLayout.LayoutParams.MATCH_PARENT, RelativeLayout.LayoutParams.MATCH_PARENT);
        baseLayout.addView(surfaceView, 0, layoutParams);


        surfaceHolder = surfaceView.getHolder();
//        surfaceHolderCallback = createSurfaceHolderCallback();
        surfaceHolder.addCallback(surfaceHolderCallback);
        buttonSwitchCamera.setVisibility(View.VISIBLE);
        currentFlashMode = 0;
        flashSwitch();*/
    }

    protected void flashHide() {
        buttonFlashOn.setVisibility( View.GONE );
        buttonFlashAuto.setVisibility( View.GONE );
        buttonFlashOff.setVisibility( View.GONE );
    }

    private void flashSwitch() {
        currentFlashMode++;
        if(currentFlashMode == 3)
            currentFlashMode = 0;
        flashSet();
    }

    private void flashSet() {
        if(mCamera != null) {
            Camera.Parameters cameraParameters = mCamera.getParameters();
            List<String> flashModes = cameraParameters.getSupportedFlashModes();
            if(flashModes == null) {
                flashHide();
                return;
            }
            if(flashModes.size() == 1 && flashModes.get(0).equals(Camera.Parameters.FLASH_MODE_OFF)) {
                flashHide();
                return;
            }

            try {
                if(currentFlashMode == 0) {
                    cameraParameters.setFlashMode(Camera.Parameters.FLASH_MODE_ON);
                } else if(currentFlashMode == 1) {
                    cameraParameters.setFlashMode(Camera.Parameters.FLASH_MODE_AUTO);
                } else if(currentFlashMode == 2) {
                    cameraParameters.setFlashMode(Camera.Parameters.FLASH_MODE_OFF);
                }
                mCamera.setParameters(cameraParameters);

                buttonFlashOn.setVisibility( (currentFlashMode == 0) ? View.VISIBLE : View.GONE );
                buttonFlashAuto.setVisibility( (currentFlashMode == 1) ? View.VISIBLE : View.GONE );
                buttonFlashOff.setVisibility( (currentFlashMode == 2) ? View.VISIBLE : View.GONE );
            } catch (Exception e) {
                flashSwitch();
            }
        }
    }

    protected void onClickProxy(ImageButton v) {
//        Log.d("Camera", "Touch");
        int id = v.getId();

        if(id == R.id.next)
            finishWithItems();
        else if(id == R.id.back)
            finishWithoutItems();
        else if(id == R.id.switchCamera)
            switchCameras();
        else if(id == R.id.flashOn || id == R.id.flashOff || id == R.id.flashAuto)
            flashSwitch();
        else if(id == R.id.source_photo)
            capturePicture();
        else if(id == R.id.source_video)
            captureVideo();
        else if(id == R.id.cancel)
            dropVideosDialog();
    }


    protected void finishWithItems() {
        ArrayList<String> items = new ArrayList<String>();
        this.setResult(Activity.RESULT_OK, this.getIntent());
        int source_id = pagerSources.getSelectedView().getId();

        if(source_id == R.id.source_gallery) {
            this.getIntent().putExtra("source", "gallery");
            for(ResultClass result : pagerPhotosItems) {
                items.add(result.getFile().getAbsolutePath());
            }
        } else if(pagerSources.getSelectedView().getId() == R.id.source_photo) {
            this.getIntent().putExtra("source", "photo");
            for(ResultClass result : pagerPhotosItems) {
                items.add(result.getFile().getAbsolutePath());
            }
        } else if(pagerSources.getSelectedView().getId() == R.id.source_video) {
            this.getIntent().putExtra("source", "video");
            if(videosItems.size() > 1) {
                processVideo();
                return;
            } else if(videosItems.size() == 1) {
                items.add(videosItems.get(0).getFile().getAbsolutePath());
            }
        }
        this.getIntent().putStringArrayListExtra("items", items);
        this.finish();
    }

    protected void processVideo() {
        progressDialog = ProgressDialog.show(this, "", stringProcessingVideos, true);
        final Handler handler = new Handler();
        Runnable runnable = new Runnable() {
            @Override
            public void run() {

                List<Track> videoTracks = new LinkedList<Track>();
                List<Track> audioTracks = new LinkedList<Track>();

                Movie[] inMovies = new Movie[videosItems.size()];
                for (int i = 0; i < videosItems.size(); i++) {
                    try {
                        inMovies[i] = MovieCreator.build(videosItems.get(i).getFile().getAbsolutePath());
                        for (Track t : inMovies[i].getTracks()) {
                            if (t.getHandler().equals("soun")) {
                                audioTracks.add(t);
                            }
                            if (t.getHandler().equals("vide")) {
                                videoTracks.add(t);
                            }
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }

                try {
                    Movie result = new Movie();

                    if (audioTracks.size() > 0) {
                        result.addTrack(new AppendTrack(audioTracks.toArray(new Track[audioTracks.size()])));
                    }
                    if (videoTracks.size() > 0) {
                        result.addTrack(new AppendTrack(videoTracks.toArray(new Track[videoTracks.size()])));
                    }

                    Container out = new DefaultMp4Builder().build(result);

                    File ret = getOutputMediaFile(MEDIA_TYPE_MERGED_VIDEO);

                    FileChannel fc = new RandomAccessFile(ret.getAbsolutePath(), "rw").getChannel();
                    out.writeContainer(fc);
                    fc.close();
                    Log.d("ProcessVideo RET", ret.getAbsolutePath() + " >>> "  + ret.length());
                    if(shouldSaveOnGallery) {
                        Intent mediaScanIntent = new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE);
                        Uri contentUri = Uri.fromFile(ret);
                        mediaScanIntent.setData(contentUri);
                        self.sendBroadcast(mediaScanIntent);
                    }
                    dropVideos(true);
                    videosItems.add(new ResultFile(ret));
                } catch (Exception e) {
                    e.printStackTrace();
                }

                handler.post(new Runnable() {
                    @Override
                    public void run() {
                        progressDialog.dismiss();
                        finishWithItems();
                    }
                });
            }
        };

        Thread thread = new Thread(runnable);
        thread.start();
    }

    protected void finishWithoutItems() {
        this.setResult(Activity.RESULT_CANCELED, this.getIntent());
        this.finish();
    }

    protected void dropVideosDialog() {
        AlertDialog dialog = new AlertDialog.Builder(self).create();
        dialog.setMessage(stringDeleteVideo);
        dialog.setCancelable(false);
        dialog.setButton(DialogInterface.BUTTON_POSITIVE, stringOk, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int buttonId) {
                dialog.dismiss();
                if(pagerSources.getSelectedItemId() == R.id.source_video) {
                    dropVideos(true);
                } else {
                    dropVideos(false);
                }
            }
        });
        dialog.setButton(DialogInterface.BUTTON_NEGATIVE, stringCancel, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int buttonId) {
                dialog.dismiss();
            }
        });
        dialog.setIcon(android.R.drawable.ic_dialog_alert);
        dialog.show();
    }

    public void dropVideos(boolean shouldDelete) {
        if(!shouldSaveOnGallery && shouldDelete) {
            for(ResultFile resultFile : videosItems) {
                if(!resultFile.getFile().delete())
                    resultFile.getFile().deleteOnExit();
            }
        }
        videosItems.clear();
        buttonCancel.setVisibility(View.GONE);
        progressBar.setProgress(0);
        totalVideoDuration = 0;
    }

    //region Camera Implementation based on Google Code
    //Reference:
    // http://developer.android.com/guide/topics/media/camera.html#preview-layout


    public void startCamera() {
        int numberOfCameras = Camera.getNumberOfCameras();
        if(numberOfCameras > 0) {
            if(numberOfCameras > 1) {
                buttonSwitchCamera.setVisibility(View.VISIBLE);
            }
            if(mCamera == null)
                mCamera = getCameraInstance();

            mPreview = new CameraPreview(this, mCamera);
            FrameLayout preview = (FrameLayout) findViewById(R.id.cameraPreview);
            if(preview.getChildCount() > 0) {
                preview.removeViewAt(0);
            }
            preview.addView(mPreview);
            currentFlashMode = 1; //AUTO
            flashSet();
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        releaseMediaRecorder();       // if you are using MediaRecorder, release it first
        releaseCamera();              // release the camera immediately on pause event
    }

    private void releaseMediaRecorder(){
        if (mMediaRecorder != null) {
            mMediaRecorder.reset();   // clear recorder configuration
            mMediaRecorder.release(); // release the recorder object
            mMediaRecorder = null;
            mCamera.lock();           // lock camera for later use
        }
    }

    private void releaseCamera(){
        if (mCamera != null){
            mCamera.release();        // release the camera for other applications
            mCamera = null;
        }
    }

    public static final int MEDIA_TYPE_IMAGE = 1;
    public static final int MEDIA_TYPE_VIDEO = 2;
    public static final int MEDIA_TYPE_MERGED_VIDEO = 3;

    /** Create a file Uri for saving an image or video *
    private static Uri getOutputMediaFileUri(int type){
        return Uri.fromFile(getOutputMediaFile(type));
    }*/

    /** Create a File for saving an image or video */
    private File getOutputMediaFile(int type){
        // To be safe, you should check that the SDCard is mounted
        // using Environment.getExternalStorageState() before doing this.

        File mediaStorageDir = new File(Environment.getExternalStoragePublicDirectory(
                Environment.DIRECTORY_PICTURES), stringAppFolder);
        // This location works best if you want the created images to be shared
        // between applications and persist after your app has been uninstalled.

        // Create the storage directory if it does not exist
        if (! mediaStorageDir.exists()){
            if (! mediaStorageDir.mkdirs()){
                Log.d("MyCameraApp", "failed to create directory");
                return null;
            }
        }

        // Create a media file name
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
        File mediaFile;
        if (type == MEDIA_TYPE_IMAGE){
            mediaFile = new File(mediaStorageDir.getPath() + File.separator +
                    "IMG_"+ timeStamp + ".jpg");
        } else if(type == MEDIA_TYPE_VIDEO) {
            mediaFile = new File(mediaStorageDir.getPath() + File.separator +
                    "VID_"+ timeStamp + ".mp4");
        } else if(type == MEDIA_TYPE_MERGED_VIDEO) {
            mediaFile = new File(mediaStorageDir.getPath() + File.separator +
                    "MERGED_"+ timeStamp + ".mp4");
        } else {
            return null;
        }

        return mediaFile;
    }

    public static Camera getCameraInstance(){
        Camera c = null;
        try {
            Log.d("Camera", "Camera to open: " + cameraId);
            c = Camera.open(cameraId); // attempt to get a Camera instance
        }
        catch (Exception e){
            // Camera is not available (in use or does not exist)
        }
        return c; // returns null if camera is unavailable
    }

    private boolean prepareVideoRecorder(){

        if(mCamera == null)
            mCamera = getCameraInstance();
        mMediaRecorder = new MediaRecorder();

        // Step 1: Unlock and set camera to MediaRecorder
        mCamera.unlock();
        mMediaRecorder.setOrientationHint(90);
        mMediaRecorder.setCamera(mCamera);
        mMediaRecorder.reset();

        // Step 2: Set sources

        mMediaRecorder.setVideoSource(MediaRecorder.VideoSource.CAMERA);
        mMediaRecorder.setAudioSource(MediaRecorder.AudioSource.CAMCORDER);

        // Step 3: Set a CamcorderProfile (requires API Level 8 or higher)
//        mMediaRecorder.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4);
//        mMediaRecorder.setVideoEncoder(MediaRecorder.VideoEncoder.H264);
        mMediaRecorder.setProfile(CamcorderProfile.get(CamcorderProfile.QUALITY_HIGH));

        // Step 4: Set output file
        tempVideoFile = getOutputMediaFile(MEDIA_TYPE_VIDEO);
        mMediaRecorder.setOutputFile(tempVideoFile.toString());

        // Step 5: Set the preview output
        mMediaRecorder.setPreviewDisplay(mPreview.getHolder().getSurface());
        mMediaRecorder.setMaxDuration(maxVideoDuration * 1000);
        mMediaRecorder.setOnInfoListener(new MediaRecorder.OnInfoListener() {
            @Override
            public void onInfo(MediaRecorder mr, int what, int extra) {
                if(what == MediaRecorder.MEDIA_RECORDER_INFO_MAX_DURATION_REACHED) {
                    Log.d("MEDIA_RECORDER_INFO_MAX_DURATION_REACHED", "UHUL!!!!");
                    captureVideo();
                }
            }
        });

        // Step 6: Prepare configured MediaRecorder
        try {
            mMediaRecorder.prepare();
        } catch (IllegalStateException e) {
            Log.d(TAG, "IllegalStateException preparing MediaRecorder: " + e.getMessage());
            releaseMediaRecorder();
            return false;
        } catch (IOException e) {
            Log.d(TAG, "IOException preparing MediaRecorder: " + e.getMessage());
            releaseMediaRecorder();
            return false;
        }
        return true;
    }

    private Camera.PictureCallback mPicture = new Camera.PictureCallback() {

        @Override
        public void onPictureTaken(final byte[] data, Camera camera) {

            final File pictureFile = getOutputMediaFile(MEDIA_TYPE_IMAGE);
            if (pictureFile == null){
                Log.d(TAG, "Error creating media file...");
                return;
            }

            camera.startPreview();
            final Handler handler = new Handler();
            Runnable runnable = new Runnable() {
                @Override
                public void run() {
                    try {
                        Bitmap bitmap;
                        Bitmap originalBitmap = BitmapFactory.decodeByteArray(data, 0, data.length);

                        pagerPhotosItems.add(new ResultClass(originalBitmap, pictureFile, self));

                        handler.post(new Runnable() {
                            @Override
                            public void run() {
                                pagerPhotosAdapter.notifyDataSetChanged();
                                pagerPhotos.setSelection(pagerPhotos.getCount() / 2, true);
                                pagerPhotos.setVisibility(View.VISIBLE);
                            }
                        });

                        if(imageBox > 0) {
                            int dstW = (originalBitmap.getHeight() < originalBitmap.getWidth()) ? imageBox : originalBitmap.getWidth() * imageBox / originalBitmap.getHeight();
                            int dstH = (originalBitmap.getHeight() > originalBitmap.getWidth()) ? imageBox : originalBitmap.getHeight() * imageBox / originalBitmap.getWidth();
                            originalBitmap = Bitmap.createScaledBitmap(originalBitmap, dstW, dstH, true);
                        }

                        //Rotates the bitmap inside the ResultClass and here. The goal is to place the thumb as soon as possible
                        bitmap = rotatedBitmap(originalBitmap);
                        originalBitmap.recycle();

                        FileOutputStream fos = new FileOutputStream(pictureFile);
                        bitmap.compress(Bitmap.CompressFormat.JPEG, imageCompression, fos);
                        fos.flush();
                        fos.close();
                        bitmap.recycle();

                        if(shouldSaveOnGallery) {
                            Intent mediaScanIntent = new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE);
                            Uri contentUri = Uri.fromFile(pictureFile);
                            mediaScanIntent.setData(contentUri);
                            self.sendBroadcast(mediaScanIntent);
                        }
                        Log.d(TAG, "Photo Done");
                    } catch (FileNotFoundException e) {
                        Log.d(TAG, "File not found: " + e.getMessage());
                    } catch (IOException e) {
                        Log.d(TAG, "Error accessing file: " + e.getMessage());
                    }
                }
            };
            Thread thread = new Thread(runnable);
            thread.start();
        }
    };


    public void capturePicture() {
        // get an image from the camera
        if(maxPhotoCount > 0 && pagerPhotosItems.size() == maxPhotoCount) {
            AlertDialog dialog = new AlertDialog.Builder(self).create();
            dialog.setMessage(stringMaxPhotos.replace("X", "" + maxPhotoCount));
            dialog.setCancelable(false);
            dialog.setButton(DialogInterface.BUTTON_NEGATIVE, stringCancel, new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int buttonId) {
                    dialog.dismiss();
                }
            });
            dialog.setIcon(android.R.drawable.ic_dialog_alert);
            dialog.show();
        } else {
            mCamera.takePicture(null, null, mPicture);
        }
    }

    public void captureVideo() {
        if (isRecording) {
            // stop recording and release camera
            try {
                mMediaRecorder.stop();  // stop the recording
            } catch (Exception e) {
                Log.d("CaptureVideo", e.getMessage());
            }
            releaseMediaRecorder(); // release the MediaRecorder object
            mCamera.lock();         // take camera access back from MediaRecorder

            progressBar.clearAnimation();
            if(shouldSaveOnGallery) {
                Intent mediaScanIntent = new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE);
                Uri contentUri = Uri.fromFile(tempVideoFile);
                mediaScanIntent.setData(contentUri);
                self.sendBroadcast(mediaScanIntent);
            }

            addVideo(tempVideoFile);

            // inform the user that recording has stopped
            isRecording = false;
        } else {
            // initialize video camera
            if (prepareVideoRecorder()) {
                // Camera is available and unlocked, MediaRecorder is prepared,
                // now you can start recording
                mMediaRecorder.start();

                // inform the user that recording has started
                //TODO
                isRecording = true;
                int availableDuration = (maxVideoDuration - totalVideoDuration) * 1000;
                if(videosItems.size() == 0) {
                    progressBar.setProgress(0);
                    totalVideoDuration = 0;
                }
                progressAnimation = new ProgressBarAnimation(progressBar, progressBar.getProgress(), 100);
                progressAnimation.setDuration(availableDuration);
                progressBar.startAnimation(progressAnimation);
            } else {
                releaseMediaRecorder();
            }
        }
    }

    protected void addVideo(File file) {
        try {
            MediaPlayer mp = new MediaPlayer();
            FileInputStream stream;
            stream = new FileInputStream(file);
            mp.setDataSource(stream.getFD());
            stream.close();
            mp.prepare();
            long duration = mp.getDuration();
            mp.release();
            totalVideoDuration += (duration / 1000);

            videosItems.add(new ResultFile(file));
            tempVideoFile = null;
            pagerPhotos.setVisibility(View.GONE);
            if(videosItems.size() > 0)
                buttonCancel.setVisibility(View.VISIBLE);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    //endregion


    @Override
    public void onRestoreInstanceState(Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);

        pagerPhotosItems = savedInstanceState.getParcelableArrayList("pagerPhotosItems");
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putParcelableArrayList("pagerPhotosItems", pagerPhotosItems);
    }

    private Bitmap rotatedBitmap(Bitmap bitmap) {
        return ResultClass.rotatedBitmap(bitmap, self);
    }

    @TargetApi(Build.VERSION_CODES.JELLY_BEAN)
    private void setBackgroundOn(View v, int d) {
        int sdk = android.os.Build.VERSION.SDK_INT;
        if(sdk < android.os.Build.VERSION_CODES.JELLY_BEAN) {
            v.setBackgroundDrawable( getResources().getDrawable(d) );
        } else {
            v.setBackground( getResources().getDrawable(d));
        }
    }

    private class PhotosAdapter extends BaseAdapter {
        private Context context;
        private List<ResultClass> items;

        PhotosAdapter(Context context, List<ResultClass> items) {
            this.context = context;
            this.items = items;
        }

        public int getCount() {
            return (items != null) ? items.size() : 0;
        }

        public Object getItem(int position) {
            return items.get(position).scaled;
        }

        public long getItemId(int position) {
            return position;
        }

        @TargetApi(Build.VERSION_CODES.JELLY_BEAN)
        public View getView(int position, View convertView, ViewGroup parent) {
            ImageView imageView;
            if(convertView == null) {
                imageView = new ImageView(context);
                imageView.setScaleType(ImageView.ScaleType.FIT_CENTER);
                imageView.setAdjustViewBounds(true);
                EcoGallery.LayoutParams layoutParams = new EcoGallery.LayoutParams(EcoGallery.LayoutParams.WRAP_CONTENT, EcoGallery.LayoutParams.MATCH_PARENT);
                imageView.setLayoutParams(layoutParams);
            } else {
                imageView = (ImageView) convertView;
            }
            imageView.setImageBitmap(items.get(position).getScaled());

            return imageView;
            //return convertView;
        }

    }

    private class SourcesAdapter extends BaseAdapter {
        private Context context;
        private List<ImageButton> items;

        SourcesAdapter(Context context, List<ImageButton> items) {
            this.context = context;
            this.items = items;
            Log.d("Context", this.context.getPackageName()); //Avoid "unused" message on IDE
        }

        public int getCount() {
            return (items != null) ? items.size() : 0;
        }

        public Object getItem(int position) {
            return items.get(position);
        }

        public long getItemId(int position) {
            return position;
        }

        @TargetApi(Build.VERSION_CODES.JELLY_BEAN)
        public View getView(int position, View convertView, ViewGroup parent) {
            return items.get(position);
        }
    }

    public static int getMaxVideoDuration() {
        return maxVideoDuration;
    }

    //region Media Chooser


    @Override
    protected void setupHeaderUI() { }

    @Override
    protected void setHeaderTitle(int id_text, int id_image) { }

    @Override
    protected void setupContentView() {
        setContentView(R.layout.activity_full_cam);
    }

    @Override
    protected Class<? extends ImageFragment> getImageFragmentClass() {
        return ImageFragmentFC.class;
    }

    @Override
    protected Class<? extends VideoFragment> getVideoFragmentClass() {
        return VideoFragmentFC.class;
    }

    @Override
    protected void setupTabTitle(int i) {
        TabWidget tabWidget = mTabHost.getTabWidget();
        TextView textView = (TextView) tabWidget.getChildAt(i).findViewById(android.R.id.title);
        if(textView.getLayoutParams() instanceof RelativeLayout.LayoutParams){

            RelativeLayout.LayoutParams params = (RelativeLayout.LayoutParams) textView.getLayoutParams();
            params.addRule(RelativeLayout.CENTER_HORIZONTAL);
            params.addRule(RelativeLayout.CENTER_VERTICAL);
            params.height = RelativeLayout.LayoutParams.MATCH_PARENT;
            params.width  = RelativeLayout.LayoutParams.MATCH_PARENT;
            params.alignWithParent = true;
            params.setMargins(0,0,0,0);
            textView.setGravity(Gravity.CENTER);
            textView.setPadding(0,0,0,0);
            textView.setLayoutParams(params);
            textView.setBackgroundColor(Color.BLACK);

        }else if(textView.getLayoutParams() instanceof LinearLayout.LayoutParams){
            LinearLayout.LayoutParams params = (LinearLayout.LayoutParams) textView.getLayoutParams();
            params.gravity = Gravity.CENTER;
            textView.setLayoutParams(params);
        }
//        textView.setTextColor(getResources().getColor(com.learnncode.mediachooser.R.color.tabs_title_color));
        textView.setTextSize(convertDipToPixels(10));
    }

    @Override
    protected void changeTabTitleUnselected(int tabNumber) {
        //((TextView)(mTabHost.getTabWidget().getChildAt(tabNumber).findViewById(android.R.id.title))).setTextColor(Color.WHITE);
    }

    @Override
    protected void changeTabTitleSelected(int tabNumber) {
        //((TextView)(mTabHost.getTabWidget().getChildAt(tabNumber).findViewById(android.R.id.title))).setTextColor(getResources().getColor(com.learnncode.mediachooser.R.color.headerbar_selected_tab_color));
    }


    @Override
    public void onVideoSelected(ArrayList<String> items){
        if(items.size() > 0){
            galleryVideo = new File(items.get(0));
            showNext();
        }else{
            galleryVideo = null;
            hideNext();
        }
    }
    //endregion

    void showNext() {
        buttonNext.setVisibility(View.VISIBLE);
    }

    void hideNext() {
        buttonNext.setVisibility(View.GONE);
    }
}
