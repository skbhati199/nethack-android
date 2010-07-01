package com.nethackff;

import android.app.Activity;
//import android.app.ActivityManager;
//import android.app.AlertDialog;
//import android.app.AlertDialog.Builder;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.res.AssetManager;
import android.content.res.Configuration;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.Rect;
import android.graphics.Typeface;
import android.inputmethodservice.Keyboard;
import android.inputmethodservice.KeyboardView;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.Display;
import android.view.GestureDetector;
import android.view.GestureDetector.OnGestureListener;
import android.view.inputmethod.InputMethodManager;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnKeyListener;
import android.view.ViewGroup;
import android.view.ViewGroup.LayoutParams;
import android.view.Window;
import android.view.WindowManager;
import android.widget.EditText;
import android.widget.LinearLayout;
//import android.widget.ScrollView;

import java.io.BufferedReader;
import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileDescriptor;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.lang.Thread;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

public class NetHackApp extends Activity implements Runnable, OnGestureListener
{
	NetHackTerminalView mainView;
	NetHackTerminalView messageView;
	NetHackTerminalView statusView;
	NetHackTerminalView menuView;

	NetHackKeyboard virtualKeyboard;
	
	/* For debugging only. */
	NetHackTerminalView dbgTerminalTranscriptView;
	static NetHackTerminalState dbgTerminalTranscriptState;

	static UIMode uiModeActual = UIMode.Invalid;
	
	NetHackTerminalView currentDbgTerminalView;
	
	class ModifierKey
	{
		public boolean active = false;
		public boolean down = false;
		public boolean used = false;
		public boolean sticky = false;

		public void resetState()
		{
			active = down = used = false;
		}
		
		public void keyUp()
		{
			down = false;
			if(!sticky || used)
			{
				active = false;
				used = false;
			}
		}

		public void keyDown()
		{
			if(active && sticky)
			{
				used = true;
			}
			down = true;
			active = true;
		}

		public void usedIfActive()
		{
			if(active)
			{
				used = true;
			}
			if(sticky && active && !down)
			{
				active = false;
				used = false;
			}
		}
	}

	ModifierKey altKey;
	ModifierKey ctrlKey;
	ModifierKey shiftKey;

	enum Orientation
	{
		Invalid,
		Sensor,
		Portrait,
		Landscape
	}
	
	enum KeyAction
	{
		None,
		VirtualKeyboard,
		AltKey,
		CtrlKey,
		ShiftKey,
		EscKey
	}
	
	enum ColorMode
	{
		Invalid,
		WhiteOnBlack,
		BlackOnWhite
	}
	
	enum UIMode
	{
		Invalid,
		PureTTY,
		AndroidTTY
	}

	enum FontSize
	{
		FontSize10,
		FontSize11,
		FontSize12,
		FontSize13,
		FontSize14,
		FontSize15
	}

	enum CharacterSet
	{
		Invalid,
		ANSI128,
		IBM,
		Amiga
	}
	
	boolean optAllowTextReformat = true;
	boolean optFullscreen = true;
	ColorMode optColorMode = ColorMode.Invalid;
	UIMode optUIModeNew = UIMode.Invalid;
	CharacterSet optCharacterSet = CharacterSet.Invalid;
	NetHackTerminalView.ColorSet optCharacterColorSet = NetHackTerminalView.ColorSet.Amiga;
	FontSize optFontSize = FontSize.FontSize10;
	Orientation optOrientation = Orientation.Invalid;
	boolean optMoveWithTrackball = true;
	KeyAction optKeyBindAltLeft = KeyAction.AltKey;
	KeyAction optKeyBindAltRight = KeyAction.AltKey;
	KeyAction optKeyBindBack = KeyAction.None;
	KeyAction optKeyBindCamera = KeyAction.VirtualKeyboard;
	KeyAction optKeyBindMenu = KeyAction.None;
	KeyAction optKeyBindSearch = KeyAction.CtrlKey;
	KeyAction optKeyBindShiftLeft = KeyAction.ShiftKey;
	KeyAction optKeyBindShiftRight = KeyAction.ShiftKey;

	public KeyAction getKeyActionFromKeyCode(int keyCode)
	{
		KeyAction keyAction = KeyAction.None;
		switch(keyCode)
		{
			case KeyEvent.KEYCODE_ALT_LEFT:
				keyAction = optKeyBindAltLeft; 	
				break;
			case KeyEvent.KEYCODE_ALT_RIGHT:
				keyAction = optKeyBindAltRight; 	
				break;
			case KeyEvent.KEYCODE_CAMERA:
				keyAction = optKeyBindCamera; 	
				break;
			case KeyEvent.KEYCODE_BACK:
				keyAction = optKeyBindBack; 	
				break;
			case KeyEvent.KEYCODE_MENU:
				keyAction = optKeyBindMenu;
				break;
			case KeyEvent.KEYCODE_SEARCH:
				keyAction = optKeyBindSearch; 	
				break;
			case KeyEvent.KEYCODE_SHIFT_LEFT:
				keyAction = optKeyBindShiftLeft; 	
				break;
			case KeyEvent.KEYCODE_SHIFT_RIGHT:
				keyAction = optKeyBindShiftRight; 	
				break;
			default:
				break;
		}
		return keyAction;		
	}
	
	public boolean onKeyDown(int keyCode, KeyEvent event)
	{
		KeyAction keyAction = getKeyActionFromKeyCode(keyCode);

		if(keyAction == KeyAction.VirtualKeyboard)
		{
//			InputMethodManager inputManager = (InputMethodManager)this.getSystemService(Context.INPUT_METHOD_SERVICE);
//			inputManager.showSoftInput(mainView.getRootView(), InputMethodManager.SHOW_FORCED);
			keyboardShownInConfig[screenConfig.ordinal()] = !keyboardShownInConfig[screenConfig.ordinal()]; 
			updateLayout();
			return true;
		}

		if(keyCode == KeyEvent.KEYCODE_BACK || keyCode == KeyEvent.KEYCODE_MENU)
		{
			return super.onKeyDown(keyCode, event);
		}

		if(keyAction == KeyAction.AltKey)
		{
			altKey.keyDown();
			return true;
		}
		if(keyAction == KeyAction.CtrlKey)
		{
			ctrlKey.keyDown();
			return true;
		}
		if(keyAction == KeyAction.ShiftKey)
		{
			shiftKey.keyDown();
			return true;
		}
		char c = 0;
		if(keyAction == KeyAction.EscKey)
		{
			c = 27;
		}
		if(optMoveWithTrackball)
		{
			switch(keyCode)
			{
				case KeyEvent.KEYCODE_DPAD_DOWN:
					c = 'j';
					break;
				case KeyEvent.KEYCODE_DPAD_UP:
					c = 'k';
					break;
				case KeyEvent.KEYCODE_DPAD_LEFT:
					c = 'h';
					break;
				case KeyEvent.KEYCODE_DPAD_RIGHT:
					c = 'l';
					break;
				case KeyEvent.KEYCODE_DPAD_CENTER:
					c = ',';
					break;
			}
		}
		
		String s = "";

		if(c == 0)
		{
			c = (char)event.getUnicodeChar((shiftKey.active ? KeyEvent.META_SHIFT_ON : 0)
						| (altKey.active ? KeyEvent.META_ALT_ON : 0));
			if(ctrlKey.active)
			{
				// This appears to be how the ASCII numbers would have been
				// represented if we had a Ctrl key, so now we apply that
				// for the search key instead. This is for commands like kick
				// (^D).
				c = (char)(((int)c) & 0x1f);
			}
		}

		// Map the delete button to backspace.
		if(keyCode == KeyEvent.KEYCODE_DEL)
		{
			c = 8;
		}
		
		if(c != 0)
		{
			ctrlKey.usedIfActive();
			shiftKey.usedIfActive();
			altKey.usedIfActive();

			s += c;
			NetHackTerminalSend(s);
		}

		return true;
	}

	public boolean onKeyUp(int keyCode, KeyEvent event)
	{
		KeyAction keyAction = getKeyActionFromKeyCode(keyCode);

		if(keyAction == KeyAction.CtrlKey)
		{
			ctrlKey.keyUp();
		}
		if(keyAction == KeyAction.AltKey)
		{
			altKey.keyUp();
		}
		if(keyAction == KeyAction.ShiftKey)
		{
			shiftKey.keyUp();
		}

		if(keyAction == KeyAction.None)
		{
			if(keyCode == KeyEvent.KEYCODE_BACK || keyCode == KeyEvent.KEYCODE_MENU)
			{
				return super.onKeyUp(keyCode, event);
			}
		}

		return true;
	}

	private boolean finishRequested = false;

	private synchronized void quit()
	{
		if(!finishRequested)
		{
			finishRequested = true;

			// This could be used to just shut down the Activity, but that's probably
			// not good enough for us - the NetHack native code library would often
			// stay resident, and appears to sadly not be reentrant:
			//	this.finish();

			// This could supposedly be used to kill ourselves - but, I only
			// got some sort of exception from it.
			//	ActivityManager am = (ActivityManager)getSystemService(ACTIVITY_SERVICE); 
			//	am.restartPackage("com.nethackff");

			// This seems to work. For sure, this is not encouraged for Android
			// applications, but the only alternative I could find would be to dig
			// through the NetHack native source code and find any places that may not
			// get reinitialized if reusing the library without reloading it
			// (and I have found no way to force it to reload). Obviously, it could
			// be a lot of work and a lot of risk involved with that approach.
			// It's not just a theoretical problem either - I often got characters
			// that had tons (literally) of items when the game started, so something
			// definitely appeared to be corrupted. Given this, System.exit() is the
			// best option I can think of.
			System.exit(0);
		}
	}

	private boolean clearScreen = false;

	public int quitCount = 0;

	NetHackTerminalView currentView;
	NetHackTerminalView preLogView;
	boolean escSeq = false;
	boolean escSeqAndroid = false;
	String currentString = "";

	public void writeTranscript(String s)
	{
		String transcript = "";
		for(int i = 0; i < s.length(); i++)
		{
			char c = s.charAt(i);
			if(c < 32)
			{
				transcript += '^';
				int a = c/10;
				int b = c - a*10;
				transcript += (char)('0' + a);
				transcript += (char)('0' + b);
			}
			else
			{
				transcript += c;
			}
		}
		Log.i("NetHackDbg", transcript);
		dbgTerminalTranscriptView.write(transcript);
	}

	boolean refreshDisplay = false;

	public void onConfigurationChanged(Configuration newConfig)
	{
		super.onConfigurationChanged(newConfig);

		Configuration config = getResources().getConfiguration();		
		if(config.orientation == Configuration.ORIENTATION_PORTRAIT)
		{
			screenConfig = ScreenConfig.Portrait;
		}
		else
		{
			screenConfig = ScreenConfig.Landscape;
		}

		rebuildViews();
	}

	final int menuViewWidth = 80;
	final int statusViewWidth = 80;

	public void initViewsCommon(boolean initial)
	{
		Display display = ((WindowManager)getSystemService(Context.WINDOW_SERVICE)).getDefaultDisplay();
		int sizeX = display.getWidth();
		int sizeY = display.getHeight();

		messageView.setSizeXFromPixels(sizeX);
		messageView.setSizeY(messageRows);

		messageView.extraSizeY = 1;

		messageView.computeSizePixels();
		messageView.initStateFromView();

		menuView.setSizeX(menuViewWidth);
		menuView.setSizeY(24);
		menuView.computeSizePixels();

		menuView.reformatText = optAllowTextReformat;

		if(initial)
		{
			menuView.initStateFromView();
		}
		if(menuView.reformatText)
		{
			menuView.setSizeXFromPixels(sizeX);
		}
		else
		{
			menuView.setSizeX(menuViewWidth);
		}

		// Compute how many characters would fit on screen in the status line. This gets passed
		// on to the native code so it knows if it should shorten the status or not.
		int statuswidthonscreen = sizeX/statusView.charWidth;

		// Regardless of how much we can actually fit, we still keep the width of the actual
		// view constant. This is done so that in case the text on the first line still doesn't fit
		// on screen after being shortened, it doesn't wrap around and kill the whole second line.
		// We may need to add the ability to scroll this view.
		statusView.setSizeX(statusViewWidth);

		statusView.setSizeY(statusRows);
		statusView.computeSizePixels();
		statusView.initStateFromView();

		NetHackSetScreenDim(messageView.getSizeX(), messageRows, statuswidthonscreen);

		mainView.colorSet = optCharacterColorSet;
	}
	
	public void rebuildViews()
	{
		screenLayout.removeAllViews();

		initViewsCommon(false);	

		Display display = ((WindowManager)getSystemService(Context.WINDOW_SERVICE)).getDefaultDisplay();
		int sizeX = display.getWidth();
		int sizeY = display.getHeight();

		screenLayout.removeAllViews();
		screenLayout = new LinearLayout(this);

		virtualKeyboard = new NetHackKeyboard(this);

		updateLayout();

		screenLayout.setOrientation(LinearLayout.VERTICAL);
		setContentView(screenLayout);

		NetHackRefreshDisplay();
	}

	private Handler handler = new Handler()
	{
		public void handleMessage(Message msg)
		{
			if(NetHackHasQuit() != 0)
			{
				gameInitialized = false;
				terminalInitialized = false;

				quit();
				return;
			}
			if(clearScreen)
			{
				clearScreen = false;
				mainView.terminal.clearScreen();
				mainView.invalidate();
				return;
			}

			if(NetHackGetPlayerPosShouldRecenter() != 0)
			{
				// This doesn't seem to work very well in pure TTY mode. 
				if(uiModeActual == UIMode.AndroidTTY)
				{
					centerOnPlayer();
				}
			}

			String s = NetHackTerminalReceive();
			if(s.length() != 0)
			{
				for(int i = 0; i < s.length(); i++)
				{
					char c = s.charAt(i);
					if(!escSeq)
					{
						if(c == 27)
						{
							escSeq = true;
							escSeqAndroid = false;
						}
						else
						{
							currentString += c;
						}
					}
					else if(!escSeqAndroid)
					{
						if(c == 'A')
						{
							if(currentView == currentDbgTerminalView && currentView != null)
							{
								writeTranscript(currentString);
							}
							if(currentView == null)
							{
								Log.i("NetHackDbg", currentString);
							}
							else
							{
								currentView.write(currentString);
							}
							currentString = "";
							escSeqAndroid = true;
						}
						else
						{
							// Not the droids we were looking for.
							currentString += (char)27;
							currentString += c;
							escSeq = escSeqAndroid = false;
						}
					}
					else
					{
						if(c == '0')
						{
							if((currentView == null) && (preLogView != null))
							{
								currentView = preLogView;
								preLogView = null;
							}
							else
							{
								currentView = mainView;
							}
						}
						else if(c == '1')
						{
							currentView = messageView;
						}
						else if(c == '2')
						{
							currentView = statusView;
						}
						else if(c == '4')
						{
							currentView = menuView;
						}
						else if(c == 'S')
						{
							if(currentView == menuView)
							{
								menuShown = true;
								menuView.scrollTo(0, 0);
								updateLayout();
							}
						}
						else if(c == 'H')
						{
							if(currentView == menuView)
							{
								menuShown = false;
								updateLayout();
							}
						}
						else if(c == '3')
						{
							// TEMP
							if(currentView != null)
							{
								preLogView = currentView;
							}
							currentView = null;							
						}
						else if(c == 'C')
						{
							if(currentView != null)
							{
								currentView.setDrawCursor(!currentView.getDrawCursor());
								//currentView.invalidate();
							}
						}
						escSeq = escSeqAndroid = false;	
					}
				}
				if(!escSeq)
				{
					if(currentView == currentDbgTerminalView && currentView != null)
					{
						writeTranscript(currentString);
					}
					if(currentView == null)
					{
						Log.i("NetHackDbg", currentString);
					}
					else
					{
						currentView.write(currentString);
					}
					currentString = "";
				}
			}

			if(refreshDisplay)
			{
/*
				String tmp = "" + (char)(((int)'r') & 0x1f);
// Is this safe??
				NetHackTerminalSend(tmp);
*/
				NetHackRefreshDisplay();
				refreshDisplay = false;
			}
		}
	};

	public synchronized boolean checkQuitCommThread()
	{
		if(shouldStopCommThread)
		{
			commThreadRunning = false;
			shouldStopCommThread = false;
			notify();
			return true;
		}
		return false;
	}

	public synchronized boolean isCommThreadRunning()
	{
		return commThreadRunning;
	}

	public void chmod(String filename, int permissions)
	{
		// This was a bit problematic - there is an android.os.FileUtils.setPermissions()
		// function, but apparently that is not a part of the supported interface.
		// I found some other options:
		// - java.io.setReadOnly() exists, but seems limited.
		// - java.io.File.setWritable() is a part of Java 1.6, but doesn't seem to exist in Android.
		// - java.nio.file.attribute.PosixFilePermission also doesn't seem to exist under Android.
		// - doCommand("/system/bin/chmod", permissions, filename) was what I used to do, but it was crashing for some.
		// I don't think these permissions are actually critical for anything in the application,
		// so for now, we will try to use the undocumented function and just be careful to catch any exceptions
		// and print some output spew. /FF
		
		try
		{
		    Class<?> fileUtils = Class.forName("android.os.FileUtils");
		    Method setPermissions =
		        fileUtils.getMethod("setPermissions", String.class, int.class, int.class, int.class);
		    int a = (Integer) setPermissions.invoke(null, filename, permissions, -1, -1);
		    if(a != 0)
		    {
				Log.i("NetHackDbg", "android.os.FileUtils.setPermissions() returned " + a + " for '" + filename + "', probably didn't work.");
		    }
		}
		catch(ClassNotFoundException e)
		{
			Log.i("NetHackDbg", "android.os.FileUtils.setPermissions() failed - ClassNotFoundException.");
		}
		catch(IllegalAccessException e)
		{
			Log.i("NetHackDbg", "android.os.FileUtils.setPermissions() failed - IllegalAccessException.");
		}
		catch(InvocationTargetException e)
		{
			Log.i("NetHackDbg", "android.os.FileUtils.setPermissions() failed - InvocationTargetException.");
		}
		catch(NoSuchMethodException e)
		{
			Log.i("NetHackDbg", "android.os.FileUtils.setPermissions() failed - NoSuchMethodException.");
		}
	}
	
	public void mkdir(String dirname)
	{
		// This is how it used to be done, but it's probably not a good idea
		// to rely on some external command in a hardcoded path... /FF
		//	doCommand("/system/bin/mkdir", dirname, "");

		boolean status = new File(dirname).mkdir();

		// Probably good to keep the debug spew here for now. /FF
		if(status)
		{
			Log.i("NetHackDbg", "Created dir '" + dirname + "'");

			// Probably best to keep stuff accessible, for now.
			chmod(dirname, 0777);
		}
		else
		{
			Log.i("NetHackDbg", "Failed to create dir '" + dirname + "', may already exist");
		}
	}

	public String getAppDir()
	{
		return appDir;
	}

	public String getNetHackDir()
	{
		return getAppDir() + "/nethackdir"; 
	}
	public void run()
	{
		if(!gameInitialized)
		{
			// Up until version 1.2.1, the application hardcoded the path.
			// Not sure if this caused a problem in practice, but it's possible
			// that for example people running the application from the SD card
			// with a mod could run into trouble, and it's much more proper to
			// use getFilesDir(). But, unfortunately, that may not actually return
			// the same value for people with existing installations (in my case,
			// it returns "/data/data/com.nethackff/files", so we have to be really
			// careful to not lose saved data. For that reason, we check for the
			// presence of the "version.txt" file at the old hardcoded location,
			// and if it's there, we continue to use the old location.
			String obsoletePath = "/data/data/com.nethackff";
			if(new File(obsoletePath + "/version.txt").exists())
			{
				appDir = obsoletePath;
			}
			else
			{
				appDir = getFilesDir().getAbsolutePath();	
			}
			Log.i("NetHackDbg", "Using directory '" + appDir + "' for application files.");

			String nethackdir = getNetHackDir();

			if(!compareAsset("version.txt"))
			{
				mkdir(nethackdir);
				mkdir(nethackdir + "/save");

				copyNetHackData();

				copyAsset("version.txt");
				copyAsset("NetHack.cnf", nethackdir + "/.nethackrc");
			}

			uiModeActual = optUIModeNew;
			boolean pureTTY = (uiModeActual == UIMode.PureTTY);
			if(NetHackInit(pureTTY ? 1 : 0, nethackdir) == 0)
			{
				// TODO
				return;
			}

			messageView.terminal.clearScreen();	// Remove the "Please wait..." stuff.

			//	copyFile("/data/data/com.nethackff/dat/save/10035foo.gz", "/sdcard/10035foo.gz");

			gameInitialized = true;
			clearScreen = true;
		}

		while(true)
		{
			if(checkQuitCommThread())
			{
				return;
			}
			try
			{
				handler.sendEmptyMessage(0);
				Thread.sleep(10);
			}
			catch(InterruptedException e)
			{
				throw new RuntimeException(e.getMessage());
			}
		}
	}

	boolean shouldStopCommThread = false;
	boolean commThreadRunning = false;

	public synchronized void stopCommThread()
	{
		if(!commThreadRunning)
		{
			return;
		}
		shouldStopCommThread = true;
		try
		{
			wait();
		}
		catch(InterruptedException e)
		{
			throw new RuntimeException(e.getMessage());
		}
	}

	public void onDestroy()
	{
		if(NetHackHasQuit() == 0)
		{
			Log.i("NetHack", "Auto-saving");
			if(NetHackSave() != 0)
			{
				Log.i("NetHack", "Auto-save succeeded");
			}
			else
			{
				Log.w("NetHack", "Auto-save failed");
			}
		}

		stopCommThread();
		super.onDestroy();
		//TestShutdown();
	}

	// This should work, but relying on external commands is generally undesirable,
	// and all current use of it has been eliminated. /FF
	/*
	public void doCommand(String command, String arg0, String arg1)
	{
		try
		{
				String fullCmd = command + " " + arg0 + " " + arg1;
				Process p = Runtime.getRuntime().exec(fullCmd);
				p.waitFor();
		}
		catch (IOException e)
		{
			throw new RuntimeException(e.getMessage());
		}
		catch (InterruptedException e)
		{
			throw new RuntimeException(e.getMessage());
		}
	}
	*/

	public boolean compareAsset(String assetname)
	{
		boolean match = false;

		String destname = getAppDir() + "/" + assetname;
		File newasset = new File(destname);
		try
		{
			BufferedInputStream out = new BufferedInputStream(new FileInputStream(newasset));
			BufferedInputStream in = new BufferedInputStream(this.getAssets().open(assetname));
			match = true;
			while(true)
			{
				int b = in.read();
				int c = out.read();
				if(b != c)
				{
					match = false;
					break;
				}
				if(b == -1)
				{
					break;
				}
			}
			out.close();
			in.close();
		}
		catch (IOException ex)
		{
			match = false;
		}
		return match;
	}
	
	public void copyAsset(String assetname)
	{
		copyAsset(assetname, getAppDir() + "/" + assetname);
	}
	
	public void copyAsset(String srcname, String destname)
	{
		File newasset = new File(destname);
		try
		{
			newasset.createNewFile();
			BufferedOutputStream out = new BufferedOutputStream(new FileOutputStream(newasset));
			BufferedInputStream in = new BufferedInputStream(this.getAssets().open(srcname));
			int b;
			while((b = in.read()) != -1)
			{
				out.write(b);
			}
			out.flush();
			out.close();
			in.close();
		}
		catch (IOException ex)
		{
			mainView.terminal.write("Failed to copy file '" + srcname + "'.\n");
		}
	}

	public void copyFileRaw(String srcname, String destname) throws IOException
	{
		File newasset = new File(destname);
		File srcfile = new File(srcname);
		try
		{
			newasset.createNewFile();
			BufferedOutputStream out = new BufferedOutputStream(new FileOutputStream(newasset));
			BufferedInputStream in = new BufferedInputStream(new FileInputStream(srcfile));
			int b;
			while((b = in.read()) != -1)
			{
				out.write(b);
			}
			out.flush();
			out.close();
			in.close();
		}
		catch(IOException ex)
		{
			throw ex;
		}
	}
	public void copyFile(String srcname, String destname)
	{
		try
		{
			copyFileRaw(srcname, destname);
		}
		catch(IOException ex)
		{
			mainView.terminal.write("Failed to copy file '" + srcname + "' to '" + destname + "'.\n");
		}
	}

	public void copyNetHackData()
	{
		AssetManager am = getResources().getAssets();
		String assets[] = null;
		try
		{
			assets = am.list("nethackdir");

			for(int i = 0; i < assets.length; i++)
			{
				String destname = getNetHackDir() + "/" + assets[i]; 
				copyAsset("nethackdir/" + assets[i], destname);
				chmod(destname, 0666);
			}
		}
		catch (IOException e)
		{
			throw new RuntimeException(e.getMessage());
		}
	}

	Thread commThread;

	GestureDetector gestureScanner;

	public boolean onTouchEvent(MotionEvent me)
	{
		return gestureScanner.onTouchEvent(me);
	}

	public boolean onScroll(MotionEvent e1, MotionEvent e2, float distanceX, float distanceY) 
	{
		View scrollView = mainView;
		int termx, termy;
		if(uiModeActual != UIMode.PureTTY && menuShown)
		{
			scrollView = menuView;
			termx = menuView.charWidth*menuView.sizeX;
//			termy = menuView.charHeight*menuView.sizeY;
			termy = menuView.charHeight*menuView.getNumDisplayedLines();

		}
		else
		{
			termx = mainView.charWidth*mainView.sizeX;
			termy = mainView.charHeight*mainView.sizeY;
		}
	
		int newscrollx = scrollView.getScrollX() + (int)distanceX;
		int newscrolly = scrollView.getScrollY() + (int)distanceY;
		if(newscrollx < 0)
		{
			newscrollx = 0;
		}
		if(newscrolly < 0)
		{
			newscrolly = 0;
		}

		int maxx = termx - scrollView.getWidth();
		int maxy = termy - scrollView.getHeight();
		if(maxx < 0)
		{
			maxx = 0;
		}
		if(maxy < 0)
		{
			maxy = 0;
		}
		if(newscrollx >= maxx)
		{
			newscrollx = maxx - 1;
		}
		if(newscrolly >= maxy)
		{
			newscrolly = maxy - 1;
		}

		scrollView.scrollTo(newscrollx, newscrolly);
		return true;
	}

	public boolean onDown(MotionEvent e)
	{
		return true;
	}
	public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY)
	{
		return true;
	}
	public void onLongPress(MotionEvent e)
	{
		centerOnPlayer();
	}
	public void onShowPress(MotionEvent e)
	{
	}
	public boolean onSingleTapUp(MotionEvent e)
	{
		return true;
	}

	public void centerOnPlayer()
	{
		// Probably would be better to get these two with one function call, but
		// seems a bit messy to return two values at once through JNI.
		int posx = NetHackGetPlayerPosX();
		int posy = NetHackGetPlayerPosY();

		// This is a bit funky. I think the difference of two between posx and posy
		// comes from two different things:
		// - NetHack subtracts one from the column as stored in u.ux, but not from the row? 
		//   (see wintty.c:    cw->curx = --x;	/* column 0 is never used */
 		// - Y offset of 1 in tty_create_nhwindow():
		//	 (see wintty.c:    newwin->offy = 1;
		posx--;
		posy++;
		posx -= mainView.offsetX;
		posy -= mainView.offsetY;

		mainView.scrollToCenterAtPos(posx, posy);
	}
	
	public synchronized void startCommThread()
	{
		if(!commThreadRunning)
		{
			commThreadRunning = true;
			commThread = new Thread(this);
			commThread.start();
		}
	}
	public void onResume()
	{
		super.onResume();

		while(isCommThreadRunning())
		{
			try
			{
				Thread.sleep(100);
			}
			catch(InterruptedException e)
			{
				throw new RuntimeException(e.getMessage());
			}
		}

		startCommThread();
	}
	
	public void onPause()
	{
		stopCommThread();

		super.onPause();
	}

	public void onStart()
	{
		super.onStart();

		UIMode uiModeBefore = optUIModeNew;

		int textsizebefore = getOptFontSize();
		boolean allowreformatbefore = optAllowTextReformat;

		CharacterSet characterSetBefore = optCharacterSet;
		NetHackTerminalView.ColorSet colorSetBefore = optCharacterColorSet;
		Orientation orientationBefore = optOrientation;

		getPrefs();

		// Probably makes sense to do this, in case the user held down some key
		// from before, or messed with the stickiness.
		ctrlKey.resetState();
		altKey.resetState();
		shiftKey.resetState();

		if(optFullscreen)
		{
			this.getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
					WindowManager.LayoutParams.FLAG_FULLSCREEN);
		}
		else
		{
			this.getWindow().setFlags(0, WindowManager.LayoutParams.FLAG_FULLSCREEN);
		}

		if(optUIModeNew != uiModeBefore)
		{
			Dialog dialog = new Dialog(this);
			dialog.setContentView(R.layout.uimodechanged);
			dialog.setTitle(getString(R.string.uimodechanged_title));
			dialog.show();
		}

// TEMP
		Log.i("NetHackDbg", "Before: " + characterSetBefore.toString() + " After: " + optCharacterSet);
		if(optCharacterSet != characterSetBefore)
		{
			int index = -1;
			switch(optCharacterSet)
			{
				case ANSI128:
					index = 0;
					break;
				case IBM:
					index = 1;
					break;
				case Amiga:
					index = 2;
					break;
					
			}
			if(index >= 0)
			{
				// TEMP
				Log.i("NetHackDbg", "Switching to mode " + index);

				NetHackSwitchCharSet(index);
			}
		}
		
		boolean blackonwhite = (optColorMode == ColorMode.BlackOnWhite);
		mainView.setWhiteBackgroundMode(blackonwhite);
		menuView.setWhiteBackgroundMode(blackonwhite);
		messageView.setWhiteBackgroundMode(blackonwhite);
		statusView.setWhiteBackgroundMode(blackonwhite);

		int textsize = getOptFontSize();
		mainView.setTextSize(textsize);
		messageView.setTextSize(textsize);
		statusView.setTextSize(textsize);
		menuView.setTextSize(textsize);

		if(orientationBefore != optOrientation)
		{
			switch(optOrientation)
			{
				case Sensor:
					setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_SENSOR);
					break;
				case Portrait:
					setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
					break;
				case Landscape:
					setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
					break;
			}
		}
		
		if(textsizebefore != textsize || optAllowTextReformat != allowreformatbefore || optCharacterColorSet != colorSetBefore)
		{
			rebuildViews();
		}
	}	

	LinearLayout screenLayout;

	enum ScreenConfig
	{
		Landscape,
		Portrait
	}
	ScreenConfig screenConfig;
	boolean menuShown = false;

	boolean []keyboardShownInConfig;

	void updateLayout()
	{
		mainView.setLayoutParams(
				new LinearLayout.LayoutParams(LayoutParams.FILL_PARENT,
				LayoutParams.WRAP_CONTENT, 1.0f));
		menuView.setLayoutParams(
				new LinearLayout.LayoutParams(LayoutParams.FILL_PARENT,
				LayoutParams.WRAP_CONTENT, 1.0f));
		messageView.setLayoutParams(
				new LinearLayout.LayoutParams(LayoutParams.FILL_PARENT,
				LayoutParams.WRAP_CONTENT, 0.0f));
		statusView.setLayoutParams(
				new LinearLayout.LayoutParams(LayoutParams.FILL_PARENT,
				LayoutParams.WRAP_CONTENT, 0.0f));

		screenLayout.removeAllViews();
		if(!menuShown)
		{
			boolean pureTTY = (uiModeActual == UIMode.PureTTY);

			//layout.addView(dbgTerminalTranscript);
			if(!pureTTY)
			{
				screenLayout.addView(messageView);
			}
			screenLayout.addView(mainView);
			if(!pureTTY)
			{
				screenLayout.addView(statusView);
			}
		}
		else
		{
			screenLayout.addView(menuView);
		}
		if(currentDbgTerminalView != null)
		{
			screenLayout.addView(dbgTerminalTranscriptView);
		}
		if(keyboardShownInConfig[screenConfig.ordinal()])
		{
			screenLayout.addView(virtualKeyboard.virtualKeyboardView);
		}

		mainView.invalidate();
	}

	
	public void initDisplay()
	{
		virtualKeyboard = new NetHackKeyboard(this);

		initViewsCommon(true);

		messageView.setDrawCursor(false);
		statusView.setDrawCursor(false);

		currentView = mainView;

		//currentDbgTerminalView = messageView;
		if(currentDbgTerminalView != null)
		{
			Display display = ((WindowManager)getSystemService(Context.WINDOW_SERVICE)).getDefaultDisplay();
			int sizeX = display.getWidth();

			dbgTerminalTranscriptState = new NetHackTerminalState();
			dbgTerminalTranscriptState.colorForeground = NetHackTerminalState.kColGreen;
			dbgTerminalTranscriptView = new NetHackTerminalView(this, dbgTerminalTranscriptState);
			dbgTerminalTranscriptView.setSizeXFromPixels(sizeX);
			dbgTerminalTranscriptView.setSizeY(5);
			dbgTerminalTranscriptView.initStateFromView();
		}

		screenLayout = new LinearLayout(this);
		updateLayout();

		screenLayout.setOrientation(LinearLayout.VERTICAL);
		setContentView(screenLayout);
//		setContentView(mainView);

		refreshDisplay = true;
	}
	
	int messageRows = 2;
	int statusRows = 2;

	public int getOptFontSize()
	{
		int sz = 10;
		switch(optFontSize)
		{
			case FontSize10:
				sz = 10;
				break;
			case FontSize11:
				sz = 11;
				break;
			case FontSize12:
				sz = 12;
				break;
			case FontSize13:
				sz = 13;
				break;
			case FontSize14:
				sz = 14;
				break;
			case FontSize15:
				sz = 15;
				break;
			default:
				break;
		}
		return sz;
	}

	Bitmap fontBitmap;

	public void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);

		altKey = new ModifierKey();
		ctrlKey = new ModifierKey();
		shiftKey = new ModifierKey();

		requestWindowFeature(Window.FEATURE_NO_TITLE);
//        this.getWindow().setFlags(WindowManager.LayoutParams.FLAG_NO_STATUS_BAR,
//      		WindowManager.LayoutParams.FLAG_NO_STATUS_BAR);

		int width = 80;
		int height = 24;		// 26

		if(!terminalInitialized)
		{
//			mainTerminalState = new NetHackTerminalState(width, height - messageRows - statusRows);
			mainTerminalState = new NetHackTerminalState(width, height);
//			statusTerminalState = new NetHackTerminalState(width, statusRows);

			terminalInitialized = true;
		}

//		messageTerminalState = new NetHackTerminalState(width, messageRows);
		messageTerminalState = new NetHackTerminalState();
//		statusTerminalState = new NetHackTerminalState(53, statusRows);
//		dbgTerminalTranscriptState = new NetHackTerminalState(53, 5);
		statusTerminalState = new NetHackTerminalState();
		menuTerminalState = new NetHackTerminalState();

		getPrefs();
		optCharacterSet = CharacterSet.Invalid;

		boolean pureTTY;
		if(!gameInitialized)
		{
			uiModeActual = optUIModeNew;
		}
		pureTTY = (uiModeActual == UIMode.PureTTY);

		int textsize = getOptFontSize();
		mainView = new NetHackTerminalView(this, mainTerminalState);
		mainView.setTextSize(textsize);
		//		mainView.offsetY = messageRows;
		if(!pureTTY)
		{
			mainView.sizeY -= messageRows + statusRows;
			mainView.offsetY = 1;

			/* TEMP */
//			mainView.sizeY -= 10;
		}
//		mainView.sizeY -= 3;
		mainView.computeSizePixels();
//		mainView.sizePixelsY -= 40;	// TEMP
//		mainView.sizePixelsY -= 120;	// TEMP
		mainView.sizePixelsY = 32;	// Hopefully not really relevant - will grow as needed.

		fontBitmap = BitmapFactory.decodeResource(getResources(), R.drawable.dungeonfont);
		mainView.fontBitmap = fontBitmap;

		keyboardShownInConfig = new boolean[ScreenConfig.values().length];
		keyboardShownInConfig[ScreenConfig.Portrait.ordinal()] = true;
		keyboardShownInConfig[ScreenConfig.Landscape.ordinal()] = false;

		Configuration config = getResources().getConfiguration();		
		if(config.orientation == Configuration.ORIENTATION_PORTRAIT)
		{
			screenConfig = ScreenConfig.Portrait;
		}
		else
		{
			screenConfig = ScreenConfig.Landscape;
		}
		
		messageView = new NetHackTerminalView(this, messageTerminalState);
		statusView = new NetHackTerminalView(this, statusTerminalState);
		menuView = new NetHackTerminalView(this, menuTerminalState); 
		messageView.setTextSize(textsize);
		statusView.setTextSize(textsize);
		menuView.setTextSize(textsize);

		initDisplay();
		
		if(!gameInitialized)
		{
//			mainView.terminal.write("Please wait, initializing...\n");
			messageView.terminal.write("Please wait, initializing...\n");
		}

		gestureScanner = new GestureDetector(this);
	}

	public boolean onCreateOptionsMenu(Menu menu)
	{
		super.onCreateOptionsMenu(menu);
		getMenuInflater().inflate(R.layout.menu, menu);
		return true;
	}

	public void configExport(String outname)
	{
		try
		{
			copyFileRaw(getNetHackDir() + "/.nethackrc", outname);

			AlertDialog.Builder alert = new AlertDialog.Builder(this);  
			alert.setTitle(getString(R.string.dialog_Success));
			alert.setMessage(getString(R.string.configexport_success) + " '" + outname + "'.");
			alert.show();
		}
		catch(IOException e)
		{
			AlertDialog.Builder alert = new AlertDialog.Builder(this);  
			alert.setTitle(getString(R.string.dialog_Error));
			alert.setMessage(getString(R.string.configexport_failed) + " '" + outname + "'.");
			alert.show();
		}
	}

	public void configImport(String inname)
	{
		try
		{
			copyFileRaw(inname, getNetHackDir() + "/.nethackrc"); 

			AlertDialog.Builder alert = new AlertDialog.Builder(this);  
			alert.setTitle(getString(R.string.dialog_Success));
			alert.setMessage(getString(R.string.configimport_success) + " '" + inname + "'. " + getString(R.string.configimport_success2));
			alert.show();
		}
		catch(IOException e)
		{
			AlertDialog.Builder alert = new AlertDialog.Builder(this);  
			alert.setTitle(getString(R.string.dialog_Error));
			alert.setMessage(getString(R.string.configimport_failed) + " '" + inname + "'. " + getString(R.string.configimport_failed2));
			alert.show();
		}
	}

	public void configImportExport(String filename, boolean cfgimport)
	{
		if(cfgimport)
		{
			configImport(filename);
		}
		else
		{
			configExport(filename);
		}
	}
	
	public void configImportExportDialog(final boolean cfgimport)
	{
		final AlertDialog.Builder dialog = new AlertDialog.Builder(this);
		if(cfgimport)
		{
			dialog.setTitle(getString(R.string.configimport_title));
			dialog.setMessage(getString(R.string.configimport_msg));
		}
		else
		{
			dialog.setTitle(getString(R.string.configexport_title));
			dialog.setMessage(getString(R.string.configexport_msg));
		}
		final EditText input = new EditText(this);
		input.getText().append(getString(R.string.config_defaultfile));

		dialog.setView(input);
		dialog.setPositiveButton(getString(R.string.dialog_OK), new DialogInterface.OnClickListener()
		{
			public void onClick(DialogInterface d, int whichbutton)
			{
				String value = input.getText().toString();
				configImportExport(value, cfgimport);
			}
		});
		dialog.setNegativeButton(getString(R.string.dialog_Cancel), new DialogInterface.OnClickListener()
		{
			public void onClick(DialogInterface d, int whichbutton) {}
		});
		
		input.setOnKeyListener(new OnKeyListener()
		{
			public boolean onKey(View v, int keyCode, KeyEvent event)
			{
				if(keyCode == KeyEvent.KEYCODE_ENTER)
				{
					return true;
				}
				return false;
			}
		});

		dialog.show();

	}
	public boolean onOptionsItemSelected(MenuItem item)
	{  
		switch(item.getItemId())
		{
			case R.id.about:
			{
				Dialog dialog = new Dialog(this);
				dialog.setContentView(R.layout.about);
				dialog.setTitle(getString(R.string.about_title));
				dialog.show();
				return true;
			}
			case R.id.preferences:
			{
				startActivity(new Intent(this, NetHackPreferences.class));
				return true;
			}
			case R.id.importconfig:
			{
				configImportExportDialog(true);
				return true;
			}
			case R.id.exportconfig:
			{
				configImportExportDialog(false);
				return true;
			}
		}
		return false;  
	}

	private KeyAction getKeyActionEnumFromString(String s)
	{
		return KeyAction.valueOf(s);
	}
	
	private void getPrefs()
	{
		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getBaseContext());
		optKeyBindCamera = getKeyActionEnumFromString(prefs.getString("CameraButtonFunc", "VirtualKeyboard"));
		optKeyBindSearch = getKeyActionEnumFromString(prefs.getString("SearchButtonFunc", "CtrlKey"));
		optKeyBindAltLeft = getKeyActionEnumFromString(prefs.getString("LeftAltKeyFunc", "AltKey"));
		optKeyBindAltRight = getKeyActionEnumFromString(prefs.getString("RightAltKeyFunc", "AltKey"));
		optKeyBindShiftLeft = getKeyActionEnumFromString(prefs.getString("LeftShiftKeyFunc", "ShiftKey"));
		optKeyBindShiftRight = getKeyActionEnumFromString(prefs.getString("RightShiftKeyFunc", "ShiftKey"));
		altKey.sticky = prefs.getBoolean("StickyAlt", false);
		ctrlKey.sticky = prefs.getBoolean("StickyCtrl", false);
		shiftKey.sticky = prefs.getBoolean("StickyShift", false);
		optFullscreen = prefs.getBoolean("Fullscreen", true);
		optAllowTextReformat = prefs.getBoolean("AllowTextReformat", true);
		optMoveWithTrackball = prefs.getBoolean("MoveWithTrackball", true);
		optColorMode = ColorMode.valueOf(prefs.getString("ColorMode", "WhiteOnBlack"));
		optUIModeNew = UIMode.valueOf(prefs.getString("UIMode", "AndroidTTY"));
		optCharacterSet = CharacterSet.valueOf(prefs.getString("CharacterSet", "Amiga"));
		optCharacterColorSet = NetHackTerminalView.ColorSet.valueOf(prefs.getString("CharacterColorSet", "Amiga"));
		optFontSize = FontSize.valueOf(prefs.getString("FontSize", "FontSize10"));
		optOrientation = Orientation.valueOf(prefs.getString("Orientation", "Sensor"));
	}

	public static String appDir;
	public static boolean terminalInitialized = false;
	public static boolean gameInitialized = false;
	public static NetHackTerminalState mainTerminalState;
	public /*static*/ NetHackTerminalState messageTerminalState;
	public /*static*/ NetHackTerminalState statusTerminalState;
	public NetHackTerminalState menuTerminalState;

	public native int NetHackInit(int puretty, String nethackdir);
	public native void NetHackShutdown();
	public native String NetHackTerminalReceive();
	public native void NetHackTerminalSend(String str);
	public native int NetHackHasQuit();
	public native int NetHackSave();
	public native void NetHackSetScreenDim(int msgwidth, int nummsglines, int statuswidth);
	public native void NetHackRefreshDisplay();
	public native void NetHackSwitchCharSet(int charsetindex);
	
	public native int NetHackGetPlayerPosX();
	public native int NetHackGetPlayerPosY();
	public native int NetHackGetPlayerPosShouldRecenter();
	
	static
	{
		System.loadLibrary("nethack");
	}
}
