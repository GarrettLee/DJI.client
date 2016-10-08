package com.dji.sdkdemo;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Timer;
import java.util.TimerTask;

import com.garrett.tcpip.RequestThread;

import android.R.integer;
import android.annotation.TargetApi;
import android.app.Activity;
import android.content.Context;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.SurfaceTexture;
import android.media.MediaCodec;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.Surface;
import android.view.SurfaceView;
import android.view.TextureView;
import android.view.View;
import android.view.TextureView.SurfaceTextureListener;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import dji.sdk.api.mediacodec.DJIVideoDecoder;
import dji.sdk.api.DJIDrone;
import dji.sdk.interfaces.DJIReceivedVideoDataCallBack;
import dji.sdk.interfaces.DJIReceivedVideoFrameCallBack;
import dji.sdk.api.Camera.DJICameraDecodeTypeDef.DecoderType;

@TargetApi(Build.VERSION_CODES.ICE_CREAM_SANDWICH)
public class PreviewDemoHwDecodeActivity extends DemoBaseActivity implements SurfaceTextureListener
{

    private static final String TAG = "PreviewDemoHwDecodeActivity";
    boolean state = false; 
    private TextureView mVideoSurface;
    private DJIReceivedVideoDataCallBack mReceivedVideoDataCallBack = null;
    
    private TextView mConnectStateTextView;
    private Timer mTimer;
    RequestThread rt;
    private DJIVideoDecoder mVideoDecoder = null; // 解码器实例
    
    //private boolean recvData = false;
    
    private final static int MSG_INIT_DECODER = 1;

	protected static final int MSG_DSP = 2;
    
    private Handler mHandler = new Handler(new Handler.Callback() {
        
        @Override
        public boolean handleMessage(Message msg)
        {
            // TODO Auto-generated method stub
            switch (msg.what)
            {
                case MSG_INIT_DECODER:
                    Surface mSurface = (Surface)msg.obj;
                    initDecoder(mSurface);
                    break;
                case MSG_DSP:
                	mImageView.setImageBitmap((Bitmap) msg.obj);
                	mImageView.invalidate();
                	break;
                default:
                    break;
            }
            
            return false;
        }
    });

	private ImageView mImageView;
    
    class Task extends TimerTask {
        //int times = 1;

        @Override
        public void run() 
        {
            //Log.d(TAG ,"==========>Task Run In!");
            checkConnectState(); 
        }

    };
    private void checkConnectState(){

        PreviewDemoHwDecodeActivity.this.runOnUiThread(new Runnable(){

            @Override
            public void run() 
            {
                if(DJIDrone.getDjiCamera() != null){
                    boolean bConnectState = DJIDrone.getDjiCamera().getCameraConnectIsOk();
                    if(bConnectState){
                        mConnectStateTextView.setText(R.string.camera_connection_ok);
                    }
                    else{
                        mConnectStateTextView.setText(R.string.camera_connection_break);
                    }
                    
//                    if(recvData){
//                        mConnectStateTextView.setTextColor(PreviewDemoActivity.this.getResources().getColor(R.color.blue));
//                    }
//                    else{
//                        mConnectStateTextView.setTextColor(PreviewDemoActivity.this.getResources().getColor(R.color.red));
//                    }
                }
            }
        });
        
    }
    @TargetApi(Build.VERSION_CODES.ICE_CREAM_SANDWICH)
	@Override
    protected void onCreate(Bundle savedInstanceState)
    {
		Toast.makeText(PreviewDemoHwDecodeActivity.this, "开始", Toast.LENGTH_SHORT).show();

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_preview_hw_demo);
        
        mVideoSurface = (TextureView)findViewById(R.id.video_previewer_surface);
        
        mVideoSurface.setSurfaceTextureListener(this);
        
        mConnectStateTextView = (TextView)findViewById(R.id.ConnectStatePreviewTextView);
        mImageView = (ImageView) findViewById(R.id.imageView1);

        rt = new RequestThread("192.168.0.126") ;
        new Thread(rt).start();
        try {
			Thread.sleep(1000);
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
        
//        Resources res=PreviewDemoHwDecodeActivity.this.getResources();
//		
//    	final Bitmap bm=BitmapFactory.decodeResource(res, R.drawable.asiaface);
//    	while(true){
//			new Thread(){
//				@Override
//				public void run(){
//					if (state == false){
//						state = true;
//						ByteArrayOutputStream baos = new ByteArrayOutputStream();  
//				        bm.compress(Bitmap.CompressFormat.PNG, 20, baos);
//				        byte[] data = baos.toByteArray();
//				        rt.send2Master(data);
//				        state = false;
//					}
//				}
//			}.start();
//			try {
//				Thread.sleep(500);
//			} catch (InterruptedException e) {
//				// TODO Auto-generated catch block
//				e.printStackTrace();
//			}
//    	}
//        
        
        
    }
    
    @Override
    protected void onResume()
    {
        // TODO Auto-generated method stub
        mTimer = new Timer();
        Task task = new Task();
        mTimer.schedule(task, 0, 500);
        
        super.onResume();
    }
    
    @Override
    protected void onPause()
    {
        // TODO Auto-generated method stub
        if(mTimer!=null) {            
            mTimer.cancel();
            mTimer.purge();
            mTimer = null;
        }
        
        super.onPause();
    }
    
    @Override
    protected void onStop()
    {
        // TODO Auto-generated method stub
        super.onStop();
    }

    @Override
    protected void onDestroy()
    {
        // TODO Auto-generated method stub
        try
        {
            DJIDrone.getDjiCamera().setReceivedVideoDataCallBack(null);
        }
        catch (Exception e)
        {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        
        if (mVideoDecoder != null) {
            mVideoDecoder.stopVideoDecoder();
            mVideoDecoder = null;
        }
        
        super.onDestroy();
    }

    /** 
     * @Description : RETURN BTN RESPONSE FUNCTION
     */
    public void onReturn(View view){
        Log.d(TAG ,"onReturn");  
        this.finish();
    }
    
    /**
     * @param surface
     * @param width
     * @param height
     * @see android.view.TextureView.SurfaceTextureListener#onSurfaceTextureAvailable(android.graphics.SurfaceTexture,
     *      int, int)
     */
    @Override
    public void onSurfaceTextureAvailable(SurfaceTexture surface, int width, int height) {
        if (mVideoDecoder == null) {
            Surface mSurface  = new Surface(surface);
            
            mHandler.sendMessageDelayed(Message.obtain(mHandler, MSG_INIT_DECODER, mSurface), 200);
        } else {
            mVideoDecoder.setSurface(new Surface(surface));
        }
    }

    /**
     * @param surface
     * @param width
     * @param height
     * @see android.view.TextureView.SurfaceTextureListener#onSurfaceTextureSizeChanged(android.graphics.SurfaceTexture,
     *      int, int)
     */
    @Override
    public void onSurfaceTextureSizeChanged(SurfaceTexture surface, int width, int height) {

    }

    /**
     * @param surface
     * @return
     * @see android.view.TextureView.SurfaceTextureListener#onSurfaceTextureDestroyed(android.graphics.SurfaceTexture)
     */
    @Override
    public boolean onSurfaceTextureDestroyed(SurfaceTexture surface) {
        if (mVideoDecoder != null)
            mVideoDecoder.setSurface(null);
        return false;
    }

    class updateClass implements Runnable{
    	boolean state = false;
		@Override
		public void run() {
			if (state == false){
				state = true;
				// TODO Auto-generated method stub
				Bitmap bm = mVideoSurface.getBitmap();
		        ByteArrayOutputStream baos = new ByteArrayOutputStream();  
		        bm.compress(Bitmap.CompressFormat.PNG, 100, baos);
		        state = false;
			}
		}
    	
    }
    
    /**
     * @param surface
     * @see android.view.TextureView.SurfaceTextureListener#onSurfaceTextureUpdated(android.graphics.SurfaceTexture)
     */
    @Override
    public void onSurfaceTextureUpdated(SurfaceTexture surface) {
    	
    	
	    	
			// TODO Auto-generated method stub
			final Bitmap bm = mVideoSurface.getBitmap();
//			Resources res=PreviewDemoHwDecodeActivity.this.getResources();
//			
//	    	final Bitmap bm=BitmapFactory.decodeResource(res, R.drawable.asiaface);
//			new Thread(){
//				@Override
//				public void run(){
//					if (state == false){
//						state = true;
//						ByteArrayOutputStream baos = new ByteArrayOutputStream();  
//				        bm.compress(Bitmap.CompressFormat.PNG, 20, baos);
//				        byte[] data = baos.toByteArray();
//				        rt.send2Master(data);
//				        state = false;
//					}
//				}
//			}.start();
//			Toast.makeText(this, "更新", Toast.LENGTH_SHORT).show();
	        
//	        byte[] data = baos.toByteArray();
////	        rt.send2Master(data, (Context)PreviewDemoHwDecodeActivity.this);
//	        try {
//				FileOutputStream write = new FileOutputStream(new File(Environment.getExternalStorageDirectory().getAbsolutePath() + "/tmp.png"));
//				write.write(data);
//	            write.close();
//	        } catch (FileNotFoundException e) {
//				// TODO Auto-generated catch block
//				e.printStackTrace();
//			} catch (IOException e) {
//				// TODO Auto-generated catch block
//				e.printStackTrace();
//			}
//	        byte[] buf = new byte[1024 * 1024];// 1M 
//	        int len;
//			try {
//		        FileInputStream fis = new FileInputStream(Environment.getExternalStorageDirectory().getAbsolutePath() + "/tmp.png");  
//				len = fis.read(buf, 0, buf.length);
//		        Bitmap bitmap = BitmapFactory.decodeByteArray(buf, 0, len); 
//	            mHandler.sendMessageDelayed(Message.obtain(mHandler, MSG_DSP, bitmap), 200);
//
//			} catch (IOException e) {
//				// TODO Auto-generated catch block
//				e.printStackTrace();
//			}  
//	        mImageView.invalidate();
	        
    }

    /**
     * Description : init decoder
     */
    private void initDecoder(Surface surface) {
        DJIDrone.getDjiCamera().setDecodeType(DecoderType.Hardware);
        mVideoDecoder = new DJIVideoDecoder(this, surface);
        //mVideoDecoder.setRecvDataCallBack(null);
        
        
        //TODO 改成 	setReceivedVideoFrameDataCallBack
        mReceivedVideoDataCallBack = new DJIReceivedVideoDataCallBack(){

            @Override
            public void onResult(byte[] videoBuffer, int size)
            {                
            	//MediaCodec mc =  MediaCodec.createDecoderByType("video/avc");
            	//rt.send2Master(videoBuffer, (Context)PreviewDemoHwDecodeActivity.this);
				//Toast.makeText((Context)PreviewDemoHwDecodeActivity.this, "开始了数据发送", Toast.LENGTH_SHORT).show();
                //recvData = true;
//            	byte[] data = new byte[size];
//            	System.arraycopy(videoBuffer, 0, data, 0, size);
//            	rt.send2Master(data);
                DJIDrone.getDjiCamera().sendDataToDecoder(videoBuffer,size);
            }
        };
        
        DJIDrone.getDjiCamera().setReceivedVideoDataCallBack(mReceivedVideoDataCallBack);
    }
    
}
