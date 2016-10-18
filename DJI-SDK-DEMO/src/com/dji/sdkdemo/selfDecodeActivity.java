package com.dji.sdkdemo;

import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.Socket;
import java.nio.ByteBuffer;
import java.util.Timer;
import java.util.TimerTask;

import com.garrett.tcpip.RequestThread;
import com.garrett.tcpip.SocketUtil;
import com.garrett.ui.HKDialogLoading;
import com.garrett.ui.RectView;
import com.garrett.util.timer.MyClock;

import android.app.Dialog;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.SurfaceTexture;
import android.graphics.Paint.Style;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.view.Surface;
import android.view.TextureView;
import android.view.TextureView.SurfaceTextureListener;
import android.view.View;
import android.view.View.OnTouchListener;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import dji.sdk.api.DJIDrone;
import dji.sdk.api.Camera.DJICameraDecodeTypeDef.DecoderType;
import dji.sdk.api.Camera.DJICameraSettingsTypeDef.CameraVisionType;
import dji.sdk.api.Gimbal.DJIGimbalRotation;
import dji.sdk.api.GroundStation.DJIGroundStationTypeDef.GroundStationResult;
import dji.sdk.api.MainController.DJIMainControllerSystemState;
import dji.sdk.api.mediacodec.DJIVideoDecoder;
import dji.sdk.interfaces.DJIGroundStationExecuteCallBack;
import dji.sdk.interfaces.DJIMcuUpdateStateCallBack;
import dji.sdk.interfaces.DJIReceivedVideoDataCallBack;
import h264.com.VView;


public class selfDecodeActivity extends DemoBaseActivity{
	File file = new File("/sdcard/fortest" + ".h264");
    FileOutputStream fops;
	
    int bytenum = 0;
    int receiveTime = 0;
    InputStream fis = null;
    
		private String  ip = "172.20.10.9";
	    private static final String TAG = "selfDecodeActivity";
	    private DJIReceivedVideoDataCallBack mReceivedVideoDataCallBack = null;
	    RequestThread rt;
	    RequestThread rtControl;
	    byte[] data;
	    int di = 0;
	    static Object gimbalIsCmdReamainLock;
	    static protected Object gimbalMovingLock = new Object();
		boolean gimbalMoving = false;
		boolean gimbalIsCmdReamain = false;
	    boolean isFlying = false;
	    TextView tv;
	    private Timer mTimer;
	    private ImageView mAircraftImageView;
	    private ImageView mPCImageView;
	    private ImageView mPCControlView;
	    private DJIVideoDecoder mVideoDecoder = null; // 解码器实例
	    
	    private int x, y, w, h;
	    
	    private boolean aircraftLinkState = false;
	    private boolean pcImageLinkState = false;
	    private boolean pcControlLinkState = false;
	    ByteArrayOutputStream toDisk = new ByteArrayOutputStream();
	    private boolean isTransmitting = false;
	    private final static int MSG_INIT_DECODER = 1;

		protected static final int MSG_DSP = 2;
		protected static final int SHOWTOAST = 3;
		private RectView rv = null;
		private TextureView textv = null;
		class ControlTimerTask implements Runnable{
			//==============================================================================
			//指令格式	{0, 1, 3, 1/2/3/4, 0/1, -10,00~10,00, 0/1/2, -2400~2400}
//			        	 │	│  │    │       │        │        │        └─只有在控制云台时有用，范围为-2400到2400，占据两个字节。
//			        	 │	│  │    │       │        │        │
//			     		 │	│  │    │       │        │        └──────────只有在垂直移动时有效
//			  		     │	│  │ 	│   	│ 	     │				       0：垂直静止
//			     		 │	│  │	│	    │		 │				       1：垂直上升
//			     		 │	│  │	│	    │		 │				       2：垂直下降
//			        	 │  │  │    │       │        └───────────────────只有在前后移动/左右移动/水平旋转时有用，代表了移动的速度
//			        	 │  │  │    │       │							   因为是个大于255的整数，在这里要占据两个字节。
//			     		 │  │  │    │       └────────────────────────────第六七八个数的正负号
//			        	 │  │  │    └────────────────────────────────────指示了动作的类型
//			        	 │  │  │                                           1：垂直移动
//			        	 │  │  │                                           2：前后移动
//			        	 │  │  │                                           3：左右移动
//			        	 │  │  │                                           4：水平旋转
//						 │  │  │                                           5：云台上下转动
//			             └──┴──┴─────────────────────────────────────────帧头标志
			//============================================================================
						@Override
						public void run() {
							// TODO Auto-generated method stub
							//setResultToTv("开始定时器@"+MyClock.getClock());
							isFlying = true;
							while(isFlying){
							
							if(isFlying){
								//setResultToToast("checkpoint 1");
								byte[]  data = null;
								if (rtControl.isLongConnection() == true){
									//setResultToToast("checkpoint 2");
									
										data = null;
										
										try {
//											rtControl.heartBreakerRequest.getInputStream().read(data, 0, 3);
											data = SocketUtil.readByteFromStream(rtControl.heartBreakerRequest.getInputStream());
										} catch (Exception e) {
											// TODO Auto-generated catch block
											
											//e.printStackTrace();
											
										}
										
										
										
										if(data != null){
											if(data.length < 3){
												continue;
											}
											if((data[0] == 0) && (data[1] == 1) && (data[2] == 3)){
												switch (data[3]){
												case 1:
							                        DJIDrone.getDjiGroundStation().setAircraftThrottle(data[7], new DJIGroundStationExecuteCallBack(){
					
														@Override
														public void onResult(GroundStationResult arg0) {
															// TODO Auto-generated method stub
	//														setResultToToast("垂直移动");
															
														}
							                        	
							                        });
													
													break;
												case 2:
													
							                        DJIDrone.getDjiGroundStation().setAircraftPitchSpeed(data[4]*((data[5] & (int)0xff) + ((data[6] << 8)&(int)0xffff)), new DJIGroundStationExecuteCallBack(){
					
														@Override
														public void onResult(GroundStationResult arg0) {
															// TODO Auto-generated method stub
	//														setResultToToast("前后移动");
	
														}
							                        	
							                        });
													
													break;
												case 3:
							                        DJIDrone.getDjiGroundStation().setAircraftRollSpeed(data[4]*((data[5] & (int)0xff) + ((data[6] << 8)&(int)0xffff)), new DJIGroundStationExecuteCallBack(){
					
														@Override
														public void onResult(GroundStationResult arg0) {
															// TODO Auto-generated method stub
	//														setResultToToast("左右移动");        
														}
							                        	
							                        });
													
													break;
												case 4:
							                        DJIDrone.getDjiGroundStation().setAircraftYawSpeed(data[4]*((data[5] & (int)0xff) + ((data[6] << 8)&(int)0xffff)), new DJIGroundStationExecuteCallBack(){
					
														@Override
														public void onResult(GroundStationResult arg0) {
	//														setResultToToast("水平旋转");
														}
							                        	
							                        });
													
													break;
												case 5:
													
							                    	DJIGimbalRotation mPitch = null;
						                        	mPitch = new DJIGimbalRotation(true, false,false, data[4]*((data[8] & (int)0xff) + ((data[9] << 8)&(int)0xffff))); 
						                            DJIDrone.getDjiGimbal().updateGimbalAttitude(mPitch,null,null);
							                      
	//									            setResultToTv("执行云台指令，速度为："+data[4]*((data[8] & (int)0xff) + ((data[9] << 8)&(int)0xffff)));
										            break;       
												default:
													break;
												}
//												byte[] comfirm = new byte[10];
//												comfirm[0] = 0;
//												comfirm[1] = 1;
//												comfirm[2] = 4;
//												for (int i = 3; i <= 9; i++){
//													comfirm[i] = data[i];
//												}
	//											try {
	//												SocketUtil.wrightBytes2Stream(data, rtControl.heartBreakerRequest.getOutputStream());
	//											} catch (IOException e) {
	//												// TODO Auto-generated catch block
	//												e.printStackTrace();
	//											}
											}
											
										}else{
											try {
												Thread.sleep(40);
											} catch (InterruptedException e) {
												// TODO Auto-generated catch block
												//e.printStackTrace();
											}
										}
																			
								}else{
									try {
										Thread.sleep(40);
									} catch (InterruptedException e) {
										// TODO Auto-generated catch block
										//e.printStackTrace();
									}
									continue;
								}
									
							}else{
								return;
							}
							}
							//setResultToTv("	结束定时器@"+MyClock.getClock());
							return;
						}
			        };
		
			        
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
	    
	    public void writeTerminal(TextView _debugTextView,String data, int MAX_LINE) {
		    _debugTextView.append(data);
		    // Erase excessive lines
		    int excessLineNumber = _debugTextView.getLineCount() - MAX_LINE;
		    if (excessLineNumber > 0) {
		        int eolIndex = -1;
		        CharSequence charSequence = _debugTextView.getText();
		        for(int i=0; i<excessLineNumber; i++) {
		            do {
		                eolIndex++;
		            } while(eolIndex < charSequence.length() && charSequence.charAt(eolIndex) != '\n');             
		        }
		        if (eolIndex < charSequence.length()) {
		            _debugTextView.getEditableText().delete(0, eolIndex+1);
		        }
		        else {
		            _debugTextView.setText("");
		        }
		    }
		}
	    
	    public void setResultToTv(final String result){
	    	selfDecodeActivity.this.runOnUiThread(new Runnable(){

				@Override
				public void run() {
					// TODO Auto-generated method stub
					writeTerminal(tv, result + "\n", 10);
				}
			});
	    }

	    public void setResultToToast(final String result){
	    	selfDecodeActivity.this.runOnUiThread(new Runnable(){

				@Override
				public void run() {
					// TODO Auto-generated method stub
			        Toast.makeText(selfDecodeActivity.this, result, Toast.LENGTH_SHORT).show();
				}
			});
	    }
	    
		private ImageView mImageView;
		
	    
		class countingTask extends TimerTask{

			@Override
			public void run() {
				// TODO Auto-generated method stub
				synchronized(selfDecodeActivity.this){
					setResultToTv("字节数为："+bytenum);
					setResultToTv("接收次数为："+receiveTime);
					receiveTime = 0;
					bytenum = 0;
				}
			}
			
		}
		
	    class Task extends TimerTask {
	        //int times = 1;

	        @Override
	        public void run() 
	        {
				//setResultToTv("开始定时器@"+MyClock.getClock());

	            //Log.d(TAG ,"==========>Task Run In!");
	            checkConnectState(); 
	            if(rt != null){
	            	if(pcImageLinkState == false){
	                	if(rt.isThreadRunning == false){
		                    new Thread(rt).start();
//		                    try {
//		            			Thread.sleep(1000);
//		            		} catch (InterruptedException e) {
//		            			// TODO Auto-generated catch block
//		            			e.printStackTrace();
//		            		}
	                	}
	                }
                }
	            else{
	            	if (!rt.isThreadRunning){
	            		isTransmitting = false;
	            		try
	        	        {
	        	            DJIDrone.getDjiCamera().setReceivedVideoDataCallBack(null);
	        	        }
	        	        catch (Exception e)
	        	        {
	        	            // TODO Auto-generated catch block
	        	            e.printStackTrace();
	        	        }
	            	}
	            }
                if(rtControl != null){
	                if(pcControlLinkState == false){
	                	if(rtControl.isThreadRunning == false){
		                    new Thread(rtControl).start();
//		                    try {
//		            			Thread.sleep(1000);
//		            		} catch (InterruptedException e) {
//		            			// TODO Auto-generated catch block
//		            			e.printStackTrace();
//		            		}
	                	}
	                }
                }
				//setResultToTv("	结束定时器@"+MyClock.getClock());

	        }

	    };
	    
	    private void updateIconState(){
	    	
	    	//与飞行器的连接
	    	if(DJIDrone.getDjiCamera() != null){
                boolean bConnectState = DJIDrone.getDjiCamera().getCameraConnectIsOk();
                if(bConnectState){
                	if(aircraftLinkState == false){
                		aircraftLinkState = true;
                		mAircraftImageView.setImageResource(R.drawable.aircraft);
                		mAircraftImageView.postInvalidate();
                	}
                }
                else{
                	if(aircraftLinkState == true){
                    	aircraftLinkState = false;
                    	mAircraftImageView.setImageResource(R.drawable.aircraft_unconnected);
                		mAircraftImageView.postInvalidate();

                	}
                }
            }
            else{
            	if(aircraftLinkState == true){
                	aircraftLinkState = false;
                	mAircraftImageView.setImageResource(R.drawable.aircraft_unconnected);
            		mAircraftImageView.postInvalidate();

            	}	                
    		}
	    	
	    	 //当发现与电脑的图传连接不通时改变图标
            if(rt!=null){
                if(rt.heartBreakerRequest != null){
	                if(rt.heartBreakerRequest.isClosed()){
	                	if(pcImageLinkState == true){
	                		pcImageLinkState = false;
	                		mPCImageView.setImageResource(R.drawable.pc_unconnected);
	                		mPCImageView.postInvalidate();
	                	}
	                }
	                else{
	                	if(pcImageLinkState == false){
	                		pcImageLinkState = true;
	                		mPCImageView.setImageResource(R.drawable.pc);
	                		mPCImageView.postInvalidate();
	                	}	                
                	}
                }
                else{
                	if(pcImageLinkState == true){
                		pcImageLinkState = false;
                		mPCImageView.setImageResource(R.drawable.pc_unconnected);
                		mPCImageView.postInvalidate();
                	}
                }
            }
            
            //当发现与电脑的控制通道连接不通时改变图标
            if(rtControl!=null){
                if(rtControl.heartBreakerRequest != null){
	                if(rtControl.heartBreakerRequest.isClosed()){
	                	if(pcControlLinkState == true){
	                		pcControlLinkState = false;
	                		mPCControlView.setImageResource(R.drawable.pc_unconnected);
	                		mPCControlView.postInvalidate();
	                	}
	                }
	                else{
	                	if(pcControlLinkState == false){
	                		pcControlLinkState = true;
	                		mPCControlView.setImageResource(R.drawable.pc);
	                		mPCControlView.postInvalidate();
	                	}	                
                	}
                }
                else{
                	if(pcControlLinkState == true){
                		pcControlLinkState = false;
                		mPCControlView.setImageResource(R.drawable.pc_unconnected);
                		mPCControlView.postInvalidate();
                	}
                }
            }
            
	    }
	    
	    private void checkConnectState(){
	        
	    	selfDecodeActivity.this.runOnUiThread(new Runnable(){

	            @Override
	            public void run() 
	            {               
	            	updateIconState();
	                
	            }
	        });
	    }
	    
	    class MySurfaceTextureListener implements SurfaceTextureListener{

			@Override
			public void onSurfaceTextureAvailable(SurfaceTexture surface, int width, int height) {
				// TODO Auto-generated method stub
				
				if (mVideoDecoder == null) {
		            Surface mSurface  = new Surface(surface);
		            mHandler.sendMessageDelayed(Message.obtain(mHandler, MSG_INIT_DECODER, mSurface), 200);
		        } else {
		            mVideoDecoder.setSurface(new Surface(surface));
		        }
			}

			@Override
			public void onSurfaceTextureSizeChanged(SurfaceTexture surface, int width, int height) {
				// TODO Auto-generated method stub
				
			}

			@Override
			public boolean onSurfaceTextureDestroyed(SurfaceTexture surface) {
				// TODO Auto-generated method stub
				if (mVideoDecoder != null)
		            mVideoDecoder.setSurface(null);
		        return false;
			}

			@Override
			public void onSurfaceTextureUpdated(SurfaceTexture surface) {
				// TODO Auto-generated method stub
			}
	    	
	    }
	    
	    @Override
	    protected void onCreate(Bundle savedInstanceState)
	    {
//	    	try {
//				fops = new FileOutputStream(file);
//			} catch (FileNotFoundException e1) {
//				// TODO Auto-generated catch block
//				e1.printStackTrace();
//			}
	    	try {
				fis = new FileInputStream(file);
			} catch (FileNotFoundException e1) {
				// TODO Auto-generated catch block
				e1.printStackTrace();
			}
	    	
	        super.onCreate(savedInstanceState);
	        setContentView(R.layout.image_transmite_layout);
	        tv = (TextView)findViewById(R.id.textView1);
	        textv = (TextureView)findViewById(R.id.preview_in_phone);
	        textv.setSurfaceTextureListener(new MySurfaceTextureListener());
	        rv = (RectView)findViewById(R.id.rect_view);
	        rv.addOptionalRect(0.5, 0.5, 1, 1, "target", "1");
	        rv.setOnTouchListener(new OnTouchListener(){
	        	
	        	int x1 = -1;
	        	int y1 = -1;
	        	
				@Override
				public boolean onTouch(View v, MotionEvent event) {
					// TODO Auto-generated method stub
					int action = event.getAction();
					int xl;
	            	int xr;
	            	int yt;
	            	int yb;
		            switch (action) {
		            case MotionEvent.ACTION_DOWN:
		                x1=(int) event.getX();
		                y1=(int) event.getY();
		                break;
		            case MotionEvent.ACTION_MOVE:
		            	int mx=(int) event.getX();
		            	int my=(int) event.getY();
		            	xl = mx > x1?x1:mx;
		            	xr = mx < x1?x1:mx;
		            	yt = my > y1?y1:my;
		            	yb = my < y1?y1:my;
		                rv.setOptionalRect("1", xl, yt, xr, yb, "target");
		                break;
		            case MotionEvent.ACTION_UP:
		            	int x2=(int) event.getX();
		            	int y2=(int) event.getY();
		            	xl = x2 > x1?x1:x2;
		            	xr = x2 < x1?x1:x2;
		            	yt = y2 > y1?y1:y2;
		            	yb = y2 < y1?y1:y2;
		                rv.setOptionalRect("1", xl, yt, xr, yb, "target");
		                x1 = -1;
		                y1 = -1;
		                x = xl;
		                y = yt;
		                w = xr - xl;
		                h = yb - yt;
		          
		                byte[] data = {0, 2, 1,  (byte) x, (byte) (x >> 8)
		                						,(byte) y, (byte) (y >> 8)
		                						,(byte) w, (byte) (w >> 8)
		                						,(byte) h, (byte) (h >> 8)
		                						,(byte) rv.getWidth(), (byte) (rv.getWidth() >> 8)
		                						,(byte) rv.getHeight(), (byte) (rv.getHeight() >> 8)};
		                try {
							SocketUtil.wrightBytes2Stream(data, rtControl.heartBreakerRequest.getOutputStream());
						} catch (IOException e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
						} catch (Exception e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
						}
		                
		                break;
		            }
					return true;
				}
	        	
	        });
	        this.mAircraftImageView = (ImageView) findViewById(R.id.imageViewAircraft);
	        this.mPCImageView = (ImageView) findViewById(R.id.imageViewPC);
	        this.mPCControlView = (ImageView) findViewById(R.id.imageViewControlConnect);
	        mImageView = (ImageView) findViewById(R.id.imageView1);
	        this.rt = new RequestThread(ip, 2000, 30000, 30001) ;
	        rt.activity = this;
	       // rt.heartThreadPriory = Thread.MAX_PRIORITY - 1;
		    rt.heartThreadPriory = 2;
	        this.rtControl = new RequestThread(ip, 20, 30002, 30003) ;
	        rtControl.activity = this;
	        rtControl.heartThreadPriory = Thread.MIN_PRIORITY;
	        rtControl.sleepTime = 1000;
	        DJIMcuUpdateStateCallBack mMcuUpdateStateCallBack = new DJIMcuUpdateStateCallBack(){

	            @Override
	            public void onResult(DJIMainControllerSystemState state) {
	                // TODO Auto-generated method stub
//	            	if((isFlying == false)&(state.isFlying == true)){
//	            		setResultToToast("isFlying");
//	            	}
	            	//只要飞行器在空中，这里的isFlying就会被设为真
	                isFlying = state.isFlying;
	            }
	           
	        };        

	        DJIDrone.getDjiMainController().setMcuUpdateStateCallBack(mMcuUpdateStateCallBack);
//	        boolean startUpdateResult = false;
//	        for(int i = 0; (i < 10); i++){
//	        	startUpdateResult = !DJIDrone.getDjiMainController().startUpdateTimer(1000);
//	        	if (startUpdateResult == true) break;
//	        };
//	        if(startUpdateResult == false){
//	        	setResultToToast("START_UPDATING_FAIL");
//	        }
//	        
	    }
	    
	    @Override
	    protected void onResume()
	    {
	        // TODO Auto-generated method stub
	        mTimer = new Timer();
	        Task task = new Task();
			//TimerTask controlTimerTask = new ControlTimerTask();
			countingTask ct = new countingTask();
	        mTimer.schedule(task, 0, 500);
	        //mTimer.schedule(controlTimerTask, 0, 200);	 //TimerTask在被schedule了之后就不能再被schedule到其他Timer了！！
	        mTimer.schedule(ct, 0, 1000);
	        ControlTimerTask ctt = new ControlTimerTask();
	        Thread t = new Thread(ctt);
	        t.setPriority(3);
	        t.start();
	        super.onResume();
	    }
	    
	    public void oneKeyFly(View v){
	    	DJIDrone.getDjiGroundStation().openGroundStation(new DJIGroundStationExecuteCallBack(){

                @Override
                public void onResult(GroundStationResult result) {
                    // TODO Auto-generated method stub
                    String ResultsString = "opengs result =" + result.toString();
                    setResultToToast( ResultsString );
                    
                    if(result == GroundStationResult.GS_Result_Success){
                    	
                    	//one key fly
                        DJIDrone.getDjiGroundStation().oneKeyFly(new DJIGroundStationExecuteCallBack(){                            	
							@Override
							public void onResult(GroundStationResult result) {
								// TODO Auto-generated method stub
								
								String ResultsString = "one key fly result =" + result.toString();
								setResultToToast( ResultsString );		                        
								if(result == GroundStationResult.GS_Result_Success){
								    
								    
								}
								else{
								}
							}
                        	
                        });                         
                    }
                    else{
                    }
                    
                }
                
            });

	    }
	    
	    public void spin2Right(View v){
	    	DJIDrone.getDjiGroundStation().setAircraftYawSpeed(1000, new DJIGroundStationExecuteCallBack(){
	    		
				@Override
				public void onResult(GroundStationResult arg0) {
					setResultToToast(arg0.toString());
				}
            	
            });
	    }
	    
	    public void beginImageTransmite(View v){
	    	
	    	if(this.isTransmitting == false){
		    	if((DJIDrone.getDjiCamera().getCameraConnectIsOk())&&(rt.isLongConnection()) && (this.aircraftLinkState == true) && (this.pcImageLinkState == true)){
					//mHandler.sendMessageDelayed(Message.obtain(mHandler, MSG_INIT_DECODER), 200);
					this.isTransmitting = true;
					
					//调试用
					/*TimerTask t = new TimerTask(){

						@Override
						public void run() {
							selfDecodeActivity.this.runOnUiThread(new Runnable(){

								@Override
								public void run() {
									// TODO Auto-generated method stub
									// TODO Auto-generated method stub
									byte[] data = new byte[1000000];
									if (!rt.send2Master(data, 1000000)) return;
									data = null;								
								}
							});
							
						}
						
					};
					mTimer.schedule(t,0, 1);*/
		    	}
		    	else{
		    		Toast.makeText(this, "请确保与局域网和飞行控制器的连接正常", Toast.LENGTH_SHORT).show();
		    	}
	    	}
	    	else{
	    		//DJIDrone.getDjiCamera().stopDownloading();
	    		isTransmitting = false;
	    		Toast.makeText(this, "停止图传", Toast.LENGTH_SHORT).show();
	    	}
	    	
	    }
	    
	   
	    //onPause在退出UI前进行
	    @Override
	    protected void onPause()
	    {
	    	this.isTransmitting = false;
	    	if(mTimer!=null) {            
	            mTimer.cancel();
	            mTimer.purge();
	            mTimer = null;
	        }
	    	if((rt.isThreadRunning) && (rt.isLongConnection())){
		    	rt.setHeartThreadDead();
		    	while(rt.isThreadRunning);
	    	}
	    	if(rtControl.isThreadRunning && (rtControl.isLongConnection())){
		    	rtControl.setHeartThreadDead();
		    	while(rtControl.isThreadRunning);
	    	}
	    	try
	        {
	            DJIDrone.getDjiCamera().setReceivedVideoDataCallBack(null);
	        }
	        catch (Exception e)
	        {
	            // TODO Auto-generated catch block
	            e.printStackTrace();
	        }
	        // TODO Auto-generated method stub
	        
	        
	        super.onPause();
	    }
	     
	    //onStop在退出UI后进行
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
// 	        try {
////				fops.close();
//			} catch (IOException e) {
//				// TODO Auto-generated catch block
//				e.printStackTrace();
//			}

	        
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
    	 * 将图片写入到磁盘
    	 *
    	 * @param img      图片数据流
    	 * @param fileName 文件保存时的名称
    	 */
    	public void writeImageToDisk(byte[] img, String fileName) {
    	    try {
    	        File file = new File( fileName);
    	        FileOutputStream fops = new FileOutputStream(file);
    	        fops.write(img);
    	        fops.flush();
    	        fops.close();
    	    } catch (Exception e) {
    	        e.printStackTrace();
    	    }
    	}
    	public byte[] readFile2Byte(String pathStr) throws Exception {
    		byte[] outByte = null;
    		InputStream is = null;
    		ByteArrayOutputStream out = null;
    		try{
    			
    			out = new ByteArrayOutputStream();
    			
    		}
    		catch (Exception e){
    			Log.e("TimingMmsService.error" ,e.getMessage());
    		}
	        try {
	            is = new FileInputStream(pathStr);// pathStr 文件路径
	            byte[] b = new byte[1024];
	            int n;
	            while ((n = is.read(b)) != -1) {
	                out.write(b, 0, n);
	            }// end while
	        } catch (Exception e) {
	            Log.e("TimingMmsService.error" ,e.getMessage());
	            throw new Exception("System error,SendTimingMms.getBytesFromFile", e);
	        } finally {
	            if (is != null) {
	                try {
	                    is.close();
	                    if (out != null) out.close();
	                    outByte = out.toByteArray();
	                } catch (Exception e) {
	                    Log.e("error", e.toString());// TODO
	                }// end try
	            }// end if
	        }// end try
	  
	        return outByte;
    	}
	    /**
	     * Description : init decoder
	     */
	    private void initDecoder(Surface surface ) {
	        DJIDrone.getDjiCamera().setDecodeType(DecoderType.Hardware);
	        mVideoDecoder = new DJIVideoDecoder(this, surface);
	    	//TODO 改成 	setReceivedVideoFrameDataCallBack
	        mReceivedVideoDataCallBack = new DJIReceivedVideoDataCallBack(){
	            @Override
	            public void onResult(byte[] videoBuffer, int size)
	            {
//	            	setResultToTv("开始定时器@"+MyClock.getClock());
	            	final int finalSize = size;
	            	
//	            	try {
//	    					byte[] buffer = new byte[1000];
//	    					fis.read(buffer, 0, 1000);
//	    					DJIDrone.getDjiCamera().sendDataToDecoder(buffer,1000);
//	    			} catch (FileNotFoundException e) {
//	    				// TODO Auto-generated catch block
//	    				e.printStackTrace();
//	    			} catch (IOException e) {
//	    				// TODO Auto-generated catch block
//	    				e.printStackTrace();
//	    			}
	            	
	            	//这个函数不能被阻塞，否则图像会花掉
	            	if (isTransmitting) {
//	            		sendThread s = new sendThread(videoBuffer,size);
//	            		Thread t = new Thread(s);
//	            		t.start();
	            		rt.send2Master(videoBuffer, size);
	            	}
	            	else{
	            		DJIDrone.getDjiCamera().sendDataToDecoder(videoBuffer,size);
	            	}
							// TODO Auto-generated method stub
//	            	synchronized(selfDecodeActivity.this){
//	            		bytenum += finalSize;
//	            		receiveTime += 1;
//	            	}
//	            	try {
//						Thread.sleep(5);
//					} catch (InterruptedException e) {
//						// TODO Auto-generated catch block
//						e.printStackTrace();
//					}
                	
//                	 try {
//						fops.write(videoBuffer, 0,size);
//	         	        fops.flush();
//					} catch (IOException e1) {
//						// TODO Auto-generated catch block
//						e1.printStackTrace();
//					}
//	            	setResultToTv("结束定时器@"+MyClock.getClock());
                		            }
	        };
	        
	        DJIDrone.getDjiCamera().setReceivedVideoDataCallBack(mReceivedVideoDataCallBack);
	        
//	        byte[] buffer = new byte[1000];
//			try {
//				fis.read(buffer, 0, 1000);
//			} catch (IOException e) {
//				// TODO Auto-generated catch block
//				e.printStackTrace();
//			}
	    }
	
	    
	    class sendThread implements Runnable{
	    	byte[] videoBuffer;
	    	int size;
	    	
	    	sendThread(byte[] videoBuffer,int size){
	    		this.videoBuffer = videoBuffer;
	    		this.size = size;
	    	}
	    	
			@Override
			public void run() {
				// TODO Auto-generated method stub
        		rt.send2Master(videoBuffer, size);
			}
	    	
	    }
	    
}


